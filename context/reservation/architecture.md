# reservation 아키텍처

> 전체 구조 요약과 주제별 상세 문서 링크를 관리합니다.

## 스택 (실측값 — 추측 아님)

| 영역 | 버전 |
|------|------|
| Spring Boot | **4.1.1** (Initializr 기본. 3.x는 더 이상 제공되지 않는다) |
| Spring Framework | 7.0.9 / Gradle 9.7.1 / Java 21 |
| JUnit | **6.0.3** (5.x 아님) |
| Jackson | **3** — `tools.jackson.core:jackson-databind` (`com.fasterxml` 경로는 없다) |
| H2 | 2.4.240 |
| React | **19.2.8** |
| TypeScript | **7.0.2** (5.x 아님) |
| Vite / Vitest | **8.2.2** / **4.1.11** |

> Boot 4에서 `@DataJpaTest`·`@AutoConfigureTestDatabase`·`@AutoConfigureMockMvc` 가 **모듈별로 재배치**됐다. 정확한 경로는 `.dev/feat-room-reservation/design.md §0.9.7` 참조 — 공식 레퍼런스 문서의 예제는 구 경로라 그대로 쓰면 컴파일 에러다.

## 모노레포 구조

Gradle 멀티프로젝트(`include(":backend")`) + npm workspaces(`"workspaces": ["frontend"]`).
루트에 두 빌드 파일이 공존하므로 verify 게이트가 **`java-spring` + `node` 두 축**으로 돌아간다 — `./gradlew test`, `./gradlew build`, `npm test`, `npm run build`.

## 백엔드 — 4계층

도메인·애플리케이션 계층은 **Spring/JPA 애노테이션을 갖지 않는다**(엔티티의 `jakarta.persistence` 제외). 빈 등록은 `config/` 가 전담한다.

```
domain/          Spring 무의존. new 로 만들어 즉시 단위 테스트 가능
  TimeSlot            반개구간 [start, end) 겹침 판정
  Reservation         cancel(memberId) 가 스스로 규칙을 지킴
  Room                create() 가 이름·인원·위치를 검증. groupId 보유
  ReservationPolicy   BR-02~06 + 목적 길이 검증. Clock 주입
  Member / MemberPolicy / Session          인증 도메인
  Group / GroupMembership / GroupRole / MembershipStatus
  ErrorCode (interface)                    오류 코드의 공통 계약
  AppException                             코드를 실어 나르는 예외의 뿌리
  ReservationErrorCode(15) / AuthErrorCode(7) / GroupErrorCode(9)   총 31종
application/     포트 인터페이스에만 의존
  AuthService        가입·로그인·인증·로그아웃
  GroupService       그룹 생성·초대·승인·명단
  GroupAccess        그룹 권한 확인 한 곳 (세 서비스가 공유)
  RoomService        회의실 추가·조회 + requireAccessibleRoom
  ReservationService RoomService 를 통해서만 회의실에 접근한다
  *Repository / PasswordHasher / TokenGenerator (interface)
api/
  AuthController / GroupController / InvitationController
  ReservationController / RoomController
  LoginMember + LoginMemberArgumentResolver   Bearer 토큰 → Member 주입
  BearerToken                                 헤더 파싱 한 곳
  GlobalExceptionHandler    AppException 하나만 처리
  ErrorCodeHttpStatus       Map<ErrorCode,HttpStatus> 31종. 누락 시 예외
  dto/                      날짜는 String + DateTimeFormatter 직접 포맷
infra/
  Jpa*Repository            (@Repository 는 여기에만)
  Pbkdf2PasswordHasher      PBKDF2-HMAC-SHA256, 별도 의존성 없이 JDK 암호 API만
  SecureRandomTokenGenerator
config/
  ClockConfig / DomainConfig                  빈 등록 전담
  WebConfig                                   ArgumentResolver 등록
  DemoDataInitializer                         @Profile("!test") 데모 데이터
```

`TimeSlot.overlaps()` 와 `ReservationPolicy` 가 이 구조의 핵심이다. Spring도 DB도 모르는 순수 객체라 RGR 사이클이 가장 빠르게 도는 지점이다.

**시각 의존성**: `Clock` 을 주입하며 프로덕션 전역에 무인자 `now()` 가 0건이다. 테스트는 고정 시계를 쓴다.

**Jackson 회피**: 응답 DTO의 날짜·상태를 전부 `String` 으로 두고 `DateTimeFormatter` 로 직접 포맷한다. Jackson 3 환경이라 `com.fasterxml` 경로가 없고, 기본 직렬화는 초가 0일 때 생략해버린다.

저장소는 H2 인메모리(NFR-01)이며 재기동 시 비워진다. 로그인할 계정조차 없으면 실행이 불가능하므로 `DemoDataInitializer` 가 데모 계정·그룹·회의실을 심는다. `data.sql` 을 쓰지 않는 이유는 **비밀번호 해시를 SQL에 하드코딩하지 않기 위해서**다 — 서비스를 거쳐 실제 해시 경로로 만든다. 테스트 프로파일에서는 각 테스트가 자기 데이터를 만들므로 실행하지 않는다.

**인증과 권한**: MVP의 "인증 없음"(NFR-05)은 폐기됐다. 로그인하면 불투명 토큰을 받아 `Authorization: Bearer` 로 보낸다. Spring Security를 도입하지 않은 이유는 필요한 것이 "헤더의 토큰으로 회원을 찾아 컨트롤러에 넘기기" 하나뿐이라 `HandlerMethodArgumentResolver` 로 충분하기 때문이다. 토큰에 권한을 싣지 않으므로(JWT 아님) 로그아웃과 권한 변경이 즉시 반영된다.

**권한 판정 순서는 고정**이다. 회의실 추가는 `인증 → 그룹 존재 → 그룹 권한 → 입력 검증` 으로 **권한이 입력 검증보다 앞선다** — 남의 그룹에 대고 이름 길이 규칙을 캐낼 수 없어야 한다. 반대로 예약 생성은 `인증 → 날짜·시간 규칙 → 회의실 존재 → 그룹 권한 → 겹침` 으로 시간 규칙이 앞서는데, 이는 MVP의 검증 순서를 그대로 유지하기 위한 선택이다. 근거는 [`requirements/auth-groups.md` §2.1](../../requirements/auth-groups.md).

## 프론트 — 계층

```
domain/       순수 함수. Date 객체 미사용 (문자열·정수 분 산술)
  timeSlots / reservationLookup / errorMessages
api/          fetch 래퍼. 상대 경로만 사용 (dev proxy 가 백엔드로 넘긴다)
  client(ApiError) / reservations / auth / groups / types(ReservationApi·AuthApi·GroupApi)
auth/
  tokenStorage           토큰 보관 한 곳
hooks/
  useReservationBoard    오류를 errorCode 로 흡수하고 reject 하지 않는다
  useAuth                loading / anonymous / 로그인 상태. 가입·로그인·로그아웃
  useGroups              그룹 목록·선택·초대·수락/거절·회의실 추가
components/
  TimeSlotGrid / ReservationFormDialog / CancelConfirmDialog / ErrorBanner / DateSelector
  RoomFormDialog         그룹에 회의실 추가
  errorCopy              오류 코드 → 다음 행동 문구 (errorMessages 를 건드리지 않는다)
  useDialogFocus         포커스 트랩 · 초기 포커스 · Escape · 초점 복원
pages/
  ReservationPage        api 를 prop 으로 주입받는다 (테스트 격리 seam). groupId·groups·member 도 받는다
  LoginPage / GroupsPage
App.tsx                  로그인 게이트 + (예약|그룹) 탭. 회원이 바뀌면 key 로 쉘을 갈아 끼운다
                         authApi·groupApi·reservationApi 를 prop 으로 주입받는다
styles/
  tokens.css             디자인 토큰 (색·타이포·간격·라운드) — 근거는 design-tokens.md
  app.css                리셋·레이아웃·컴포넌트·반응형. 리터럴 hex 0건
today.ts      new Date() 를 쓰는 유일한 곳
```

**훅 계약 우회**: `useReservationBoard.createReservation` 은 예외를 삼키고 `undefined` 로 정상 resolve 한다(테스트가 고정). 그래서 `ReservationPage` 가 `api` prop 을 `useMemo` 래퍼로 감싸 성패와 pending 수를 기록하고, **성공했을 때만** 다이얼로그를 닫는다. 훅은 무변경이다 — 이 우회로 "실패 시 입력이 사라지는" 결함과 로딩·재시도 부재를 훅 계약을 깨지 않고 해결했다.

**격자 구조**: `.board` 단일 CSS Grid + 행 래퍼 `display:contents`. 머리 행과 방 4행의 셀이 같은 그리드에 놓여 열이 구조적으로 어긋날 수 없다. 모바일(≤720px)은 `grid-auto-flow:column` 하나로 축이 90° 회전하며 **DOM 재렌더가 없다** — 방 이름 문자열이 화면 전체에서 1회만 등장해야 한다는 테스트 제약 때문에 반응형 이중 렌더를 쓸 수 없다.

**테스트 격리 전략**: 모듈 모킹(`vi.mock`)을 쓰지 않는다 — 호이스팅과 경로 문자열에 결합돼 import 경로가 바뀌면 조용히 깨진다. 대신 `api` 를 prop으로 주입하고, 네트워크 계층은 `globalThis.fetch` 를 스텁한다.

## 주제 문서

| 주제 | 설명 |
|------|------|
| 구현 추적 | [`status.md`](status.md) — BR/FR별 상태 + 이월 항목 |
| **테스트 시나리오** | [`test-scenarios.md`](test-scenarios.md) — 658건이 보장하는 동작을 시나리오 단위로 정리 |
| **디자인 톤 기준** | [`design-tokens.md`](design-tokens.md) — 색·타이포·간격의 근거와 **바꾸면 안 되는 것** |
| 설계 결정 | `.dev/feat-room-reservation/design.md` — 검증 순서(A-1), 겹침 매칭(B-5), 확정 계약(§9.13·§9.14) |
| 수용 기준 | `.dev/feat-room-reservation/prd.md` — AC 44건 (리뷰 반영분 AC-36~40 포함) |
| 원 요구사항 | [`requirements/mvp.md`](../../requirements/mvp.md) |
| **로그인·그룹 요구사항** | [`requirements/auth-groups.md`](../../requirements/auth-groups.md) — MVP를 확장한다. BR-07·NFR-05 변경분 포함 |
