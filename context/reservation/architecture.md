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
  Reservation         cancel(name) 이 스스로 규칙을 지킴
  Room
  ReservationPolicy   BR-02~06 검증. Clock 주입
  ReservationErrorCode / ReservationException   오류 코드 13종
application/     포트 인터페이스에만 의존
  ReservationService / RoomService
  ReservationRepository / RoomRepository (interface)
api/
  ReservationController / RoomController
  GlobalExceptionHandler    ReservationException 하나만 처리
  ErrorCodeHttpStatus       EnumMap 13종. 누락 시 예외
  dto/                      날짜는 String + DateTimeFormatter 직접 포맷
infra/
  JpaReservationRepository / JpaRoomRepository   (@Repository 는 여기에만)
config/
  ClockConfig / DomainConfig                     빈 등록 전담
```

`TimeSlot.overlaps()` 와 `ReservationPolicy` 가 이 구조의 핵심이다. Spring도 DB도 모르는 순수 객체라 RGR 사이클이 가장 빠르게 도는 지점이다.

**시각 의존성**: `Clock` 을 주입하며 프로덕션 전역에 무인자 `now()` 가 0건이다. 테스트는 고정 시계를 쓴다.

**Jackson 회피**: 응답 DTO의 날짜·상태를 전부 `String` 으로 두고 `DateTimeFormatter` 로 직접 포맷한다. Jackson 3 환경이라 `com.fasterxml` 경로가 없고, 기본 직렬화는 초가 0일 때 생략해버린다.

저장소는 H2 인메모리(NFR-01)이며 재기동 시 `data.sql` 시드로 초기화된다. 인증은 없고(NFR-05) 예약자명 문자열로만 식별한다.

## 프론트 — 계층

```
domain/       순수 함수. Date 객체 미사용 (문자열·정수 분 산술)
  timeSlots / reservationLookup / errorMessages
api/          fetch 래퍼. 상대 경로만 사용 (dev proxy 가 백엔드로 넘긴다)
  client(ApiError) / reservations / types(ReservationApi)
hooks/
  useReservationBoard    오류를 errorCode 로 흡수하고 reject 하지 않는다
components/
  TimeSlotGrid / ReservationFormDialog / CancelConfirmDialog / ErrorBanner / DateSelector
pages/
  ReservationPage        api 를 prop 으로 주입받는다 (테스트 격리 seam)
today.ts      new Date() 를 쓰는 유일한 곳
```

**테스트 격리 전략**: 모듈 모킹(`vi.mock`)을 쓰지 않는다 — 호이스팅과 경로 문자열에 결합돼 import 경로가 바뀌면 조용히 깨진다. 대신 `api` 를 prop으로 주입하고, 네트워크 계층은 `globalThis.fetch` 를 스텁한다.

## 주제 문서

| 주제 | 설명 |
|------|------|
| 구현 추적 | [`status.md`](status.md) — BR/FR별 상태 + 이월 항목 |
| **테스트 시나리오** | [`test-scenarios.md`](test-scenarios.md) — 287건이 보장하는 동작을 시나리오 단위로 정리 |
| 설계 결정 | `.dev/feat-room-reservation/design.md` — 검증 순서(A-1), 겹침 매칭(B-5), 확정 계약(§9.13·§9.14) |
| 수용 기준 | `.dev/feat-room-reservation/prd.md` — AC 44건 (리뷰 반영분 AC-36~40 포함) |
| 원 요구사항 | [`requirements/mvp.md`](../../requirements/mvp.md) |
