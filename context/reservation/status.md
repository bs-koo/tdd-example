# reservation 구현 추적

> PRD 요구사항별 구현 상태를 Phase 단위로 추적합니다. Phase 정의는 [`requirements/mvp.md` 7.3](../../requirements/mvp.md#73-구현-phase) 참조.
> 상세 설계·수용 기준은 `.dev/feat-room-reservation/` 의 `prd.md`(AC 39건) / `design.md` / `state.md` 참조.

## 범례

- ✅ 반영됨 — 코드에 구현 완료 + 테스트로 검증됨
- ⬜ 미반영 — 정책/설계만 확정, 코드 미구현

## 전체 상태

**MVP(Phase 0~7) 구현 완료 (T1~T23) + 로그인·그룹(Phase 8) + 회의실 수정·삭제(W10, 백엔드) 구현 완료.** TDD RED-GREEN-REFACTOR 사이클로 진행했으며, 프로덕션 코드는 실패 테스트가 선행하지 않으면 작성하지 않았다.

| 항목 | 값 | 실측일 |
|------|-----|--------|
| 백엔드 테스트 | **545** (0 failures / 0 errors / 0 skipped) | 2026-11-02 |
| 프론트 테스트 | **251** (22 파일, 0 failures) | 2026-11-02 |
| 합계 | **796** | |
| 백엔드 `src/main` java 파일 | 83 | |
| 프론트 `src` 비테스트 파일 | 39 (CSS 2 포함, `setupTests.ts` 제외) | |
| RGR 사이클 | MVP 15회. Phase 8 은 `.dev/` 에 사이클 기록이 없다. W10 은 7 태스크 (`.dev/feat-room-edit-delete/`, 로컬 산출물) | |

> Phase 8 과 W10 은 PR [#1](https://github.com/bs-koo/tdd-example/pull/1) (`feat/room-edit-delete`) 로 올라가 있다. 백엔드 / 프론트 / 문서 3개 커밋이며, 머지 전까지 `main` 에는 없다.
>
> 프론트 테스트를 `./gradlew test` 와 **동시에** 돌리면 vitest 워커가 기동 타임아웃(`Failed to start forks worker`)으로 7건 오류를 낸다. 테스트는 통과하지만 종료 코드가 1이 된다. 순서대로 돌리면 깨끗하다.

**스택 실측값** (추측 아님 — 실제 해석 결과):
Spring Boot **4.1.1** / Gradle 9.7.1 / JUnit **6.0.3** / Jackson **3** (`tools.jackson`) / H2 2.4.240
React **19.2.8** / TypeScript **7.0.2** / Vite **8.2.2** / Vitest **4.1.11**

## Phase 진행 상태

| Phase | 이름 | 유형 | 상태 |
|-------|------|------|------|
| 0 | 기반 준비 | 스캐폴딩 | ✅ |
| 1 | 겹침 판정 | RGR | ✅ |
| 2 | 예약 정책 경계값 | RGR | ✅ |
| 3 | 예약 생성 통합 | RGR | ✅ |
| 4 | 예약 취소 | RGR | ✅ |
| 5 | 조회 API | 통합 | ✅ |
| 6 | API 오류 매핑 | 통합 | ✅ |
| 7 | 프론트 화면 | 통합 | ✅ |
| 8 | 로그인 · 그룹 · 그룹 전용 회의실 | RGR + 통합 | ✅ (PR #1) |
| 9 (W10) | 회의실 수정 · 삭제 (백엔드) | RGR 7 태스크 | ✅ (PR #1) |

## Phase 0 · 기반 준비

| 항목 | 상태 | 위치 |
|------|------|------|
| `Room`/`Reservation` 엔티티 | ✅ | `backend/.../domain/Room.java`, `Reservation.java` |
| H2 + JPA 설정 | ✅ | `backend/src/main/resources/application.yml`, `application-test.yml` |
| 회의실 시드 데이터 (4개) | ⚠️ 대체됨 | `data.sql` 삭제 → Phase 8 의 `DemoDataInitializer` |

> MVP 당시 `Room` 은 시드 id를 고정해 `@GeneratedValue` 를 쓰지 않았다. Phase 8 에서 `IDENTITY` 채번으로 바뀌었다(`requirements/auth-groups.md` §3.5).

## Phase 1 · 겹침 판정

| ID | 규칙 | 상태 | 검증 |
|----|------|------|------|
| BR-01 | 같은 회의실 내 시간 겹침 예약 금지 | ✅ | `TimeSlotTest` 15건 |

**반개구간 `[start, end)`** 로 구현했다 — `A.start < B.end && B.start < A.end`.
2.1 판정표 9케이스를 `@CsvSource` 로 **양방향 대칭까지** 검증한다(`overlaps(a,b) == overlaps(b,a)`).
경계 확인: `09:00~10:00` 과 `10:00~11:00` 은 **겹치지 않는다**(직후 접함 허용).

## Phase 2 · 예약 정책 경계값

| ID | 규칙 | 상태 | 검증 |
|----|------|------|------|
| BR-02 | 예약 시각 30분 단위 | ✅ | `ReservationPolicyTest` 21건 |
| BR-03 | 예약 가능 시간대 09:00~18:00 | ✅ | 〃 |
| BR-04 | 예약 길이 30분~4시간 | ✅ | 〃 |
| BR-05 | 과거 시각 예약 금지 | ✅ | 〃 |
| BR-06 | 14일 이내만 예약 가능 | ✅ | 〃 |

**검증 순서가 계약이다** (`ReservationPolicy.validate()`):
`INVALID_RESERVER_NAME` → `INVALID_PURPOSE_LENGTH` → `INVALID_TIME_UNIT` → `PAST_DATETIME` → `TOO_FAR_IN_FUTURE` → `OUTSIDE_BUSINESS_HOURS` → `INVALID_DURATION`

> ⚠️ `PAST_DATETIME` 이 `OUTSIDE_BUSINESS_HOURS` 보다 **먼저** 와야 한다. 순서를 바꾸면 "오늘 08:30 요청"이 영업시간 오류로 잡혀 과거 시각 검증이 도달 불가가 된다(설계 §1 A-1).

**시각 의존성 제거(QE-1)**: `Clock` 을 주입하며, 프로덕션 전역에 무인자 `now()` 가 **0건**이다. 테스트는 `Clock.fixed(2026-08-25T09:00 KST)` 를 쓴다.

## Phase 3 · 예약 생성 통합

| ID | 기능 | 상태 | 검증 |
|----|------|------|------|
| FR-03 | 예약 생성 (겹침 + 정책 조합) | ✅ | `ReservationServiceTest` 9건, `ReservationApiTest` 13건 |

`reserve()` 흐름(순서 고정 — 오류 코드를 결정한다):
`new TimeSlot` → `policy.validate` → `roomRepository.existsById` → 겹침 검사 → `Reservation.create` → **`return repository.save(r)`**

## Phase 4 · 예약 취소

| ID | 규칙/기능 | 상태 | 검증 |
|----|-----------|------|------|
| BR-07 | 취소는 예약자 본인만 | ✅ | `ReservationCancelTest` 10건 |
| BR-08 | 이미 취소된 예약 재취소 금지 | ✅ | 〃 |
| FR-04 | 예약 취소 | ✅ | `ReservationServiceCancelTest` 8건 |

**동시 위반 시 판정 순서**: 타인이 이미 취소된 예약을 취소하려 하면 `NOT_RESERVER` 가 우선한다(설계 D-4). 거절 시 상태는 `ACTIVE` 로 유지된다.

## Phase 5 · 조회 API

| ID | 기능 | 상태 | 검증 |
|----|------|------|------|
| FR-01 | 회의실 목록 조회 | ✅ | `RoomApiTest` (id 오름차순 + 시드 4건 정확 일치) |
| FR-02 | 날짜별 예약 현황 조회 | ✅ | `RoomApiTest`, `JpaReservationRepositoryTest` 8건 |

- 예약이 없는 날짜는 **빈 배열 200** 이다 (404가 아니다).
- 존재하지 않는 회의실 조회는 **404 `ROOM_NOT_FOUND`** 다.
- 날짜 형식 오류와 회의실 부재가 **동시** 발생하면 **`INVALID_DATE_FORMAT` 이 우선**한다 — 컨트롤러가 `date` 를 먼저 파싱하기 때문이다(설계 D-3).

> ⚠️ **알려진 부채**: 전체 회의실의 예약을 한 번에 가져오는 엔드포인트가 없다. 화면 하나를 채우려면 `1 + 회의실수` 호출이 필요하다(`Promise.all` 병렬). 회의실이 4개로 고정인 MVP 범위에서 수용한 결정이다(설계 §9.13.1).

## Phase 6 · API 오류 매핑

**오류 코드 13종** — `ErrorCodeHttpStatus` 가 `EnumMap` 으로 매핑하며, 누락 시 `IllegalStateException` 을 던진다(`getOrDefault` 로 조용히 넘기지 않는다).

| code | HTTP |
|------|------|
| `OVERLAPPING_RESERVATION` | 409 |
| `ALREADY_CANCELLED` | 409 |
| `NOT_RESERVER` | 403 |
| `RESERVATION_NOT_FOUND` | 404 |
| `ROOM_NOT_FOUND` | 404 |
| `INVALID_TIME_UNIT` · `OUTSIDE_BUSINESS_HOURS` · `INVALID_DURATION` · `PAST_DATETIME` · `TOO_FAR_IN_FUTURE` · `INVALID_RESERVER_NAME` · `INVALID_PURPOSE_LENGTH` · `INVALID_DATE_FORMAT` | 400 |

검증: `ErrorCodeHttpStatusTest` 16건(매핑 단위) + `ApiErrorMappingTest` 13건(**실제 HTTP 요청으로 13종 전부 도달 가능함을 E2E 확인**) + `ReservationErrorCodeMessageTest` 16건(문구).

> `@ExceptionHandler` 는 **`ReservationException` 하나뿐**이다. `MethodArgumentTypeMismatchException` 핸들러를 두지 않으므로, `date` 는 `@RequestParam String` 으로 받아 컨트롤러 안에서 파싱한다.

## Phase 7 · 프론트 화면

| ID | 기능 | 상태 | 검증 |
|----|------|------|------|
| FR-05 | 예약 화면 (타임슬롯 그리드) | ✅ | `TimeSlotGrid` 8건, `ReservationPage` 9건 외 |

- **그리드 점유 = 구간 포함 매칭**: `09:00~10:00` 예약은 `09:00`·`09:30` **2칸**을 점유한다. `10:00` 칸은 점유하지 않는다.
- 슬롯은 `generateTimeSlots()` 가 만드는 **18개**(`09:00`~`17:30`)다.
- 오류 코드 → 한국어 문구 변환은 `toKoreanMessage()` 한 곳에 있고, 알 수 없는 코드는 `'요청을 처리하지 못했습니다.'` 로 떨어진다(서버가 신규 코드를 보내도 화면이 깨지지 않는다).
- **취소 확인 대화상자에는 입력 필드가 없다** — 인증이 없는 상태에서 이름을 재입력받으면 타인이 이름을 추측해 넣는 경로가 열린다. 본인 확인은 서버(BR-07)가 하고 UI는 예약 자신의 `reserverName` 을 그대로 되돌려보낸다(설계 §9.8). 이 **부재를 테스트로 고정**해두었다.
- 시각 문자열은 `Date` 객체 없이 정수 분 산술로 다룬다. `new Date()` 는 `src/today.ts` **한 곳**에만 있으며, `Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Seoul' })` 로 실행 머신 TZ와 무관하게 동작한다.

## 실서버 E2E 확인 (2026-08-27)

MockMvc가 아닌 **실제 톰캣 + H2 + Hibernate** 기동 후 확인한 항목:

시드 4건 조회 / 빈 배열 200 / 없는 회의실 404 / 잘못된 날짜 400 / **동시 위반 시 400 우선** / 예약 생성 201(한글 UTF-8 왕복) / 겹침 409 / **직후 접함 201** / 타인 취소 403 / 본인 취소 204 / 연속 취소 409 / 없는 예약 취소 404 / CANCELLED 조회 제외 / **취소한 시간대 재예약 201**

## 미해결 / 이월 항목

review 단계에서 다룰 항목이다. 전부 "테스트가 요구하지 않아 구현 단계에서 손대지 않은" 것들이다.

| 항목 | 내용 | 현재 |
|------|------|------|
| 그리드 접근성 | 셀이 `<div onClick>` 이라 키보드로 조작할 수 없다 | **해소** — AC-37에서 `<button type="button">` 으로 전환. Enter/Space 테스트 3건이 고정 중 |
| 취소 대화상자 정보 | 무엇을 취소하는지 표시하지 않는다 | **해소** — 리디자인에서 회의실·일시·예약자·목적을 텍스트로 표시하고 "되돌릴 수 없습니다" 를 명시 |
| `spring.jpa.open-in-view` | 기본 활성 상태이며 기동 시 경고가 뜬다 | **해소** — `application.yml` 에 `open-in-view: false` |
| N+1 조회 | 위 Phase 5 참조 | 유효 |
| 날짜 입력 타입 | jsdom 테스트 결정성을 위해 `<input type="text">` 를 썼다. 네이티브 날짜 피커가 없다 | 유효 — 다만 리디자인이 `이전 날`·`다음 날`·`오늘` 버튼을 더해 타이핑 없이 이동할 수 있게 했다. `type="date"` 전환은 AC-38(부분 입력 노출)이 막는다 |
| 지난 시각 슬롯 클릭 | 오전 10시에 09:00 슬롯이 클릭되고, 제출 후에야 `PAST_DATETIME` 을 받는다 | 부분 — 리디자인이 음영·점선·접근 이름(`지난 시간`)으로 표시하나 **클릭은 계속 동작**한다. `ReservationPage.test.tsx` 5건이 과거 날짜 슬롯 클릭으로 폼이 열리기를 요구하기 때문. 완전 해결은 테스트가 고정 시각을 주입하도록 바뀌어야 가능 |
| 슬롯 3상태 대비 | — | 인수 — WCAG 1.4.11(3:1) 미달(1.16~1.39:1). 팔레트 방향을 지키고 점선 등 밝기 외 채널로 보완했다. [`design-tokens.md` §5](design-tokens.md) 참조 |

## 프론트 리디자인 (2026-08-28)

스타일시트가 0줄이던 화면을 사내 실사용 가능한 상태로 재구성했다. gx-redesign 파이프라인으로 진단 39건(Critical 13 / Major 19 / Minor 7)을 도출해 **35건 해소 / 4건 부분 / 0건 미해소**로 닫았고, 검수 2회를 거쳐 승인됐다.

| | |
|------|------|
| 테스트 | 108건 **무수정 통과** — 셀렉터 seam 을 불가침으로 두고 그 위에 시각 계층만 얹었다 |
| 무변경 | `hooks/` · `domain/` · `api/` · `today.ts` · 테스트 13파일 · `package.json` (의존성 0 추가) |
| 톤 기준 | [`design-tokens.md`](design-tokens.md) — 값의 근거와 **바꾸면 안 되는 것** |
| 전 과정 | `outputs/` — 브리프 / 진단 / 전략 / 제작 매핑 / 검수 2회 / 실물 13장 |

주요 해결: 시간 축 표시 · 격자 정렬(`display:contents`) · 모바일 축 전환(`grid-auto-flow:column`, DOM 재렌더 없음) · 실패해도 입력이 남는 폼 · 취소 대상 표시 · 모달 오버레이·포커스 트랩 · 정책 사전 노출 · 정원·층 표시 · 오류 문구에 다음 행동 추가

## Phase 8 · 로그인 · 그룹 · 그룹 전용 회의실 (2026-09-01)

요구사항: [`requirements/auth-groups.md`](../../requirements/auth-groups.md). MVP의 "인증 없음"(NFR-05)을 폐기하고, 회의실을 전역 자원에서 **그룹 소유 자원**으로 바꿨다.

| ID | 규칙 | 상태 | 검증 |
|----|------|------|------|
| AR-01~03 | 이메일 정규화 · 비밀번호 길이 · 평문 미저장 | ✅ | `MemberTest` · `MemberPolicyTest` · `Pbkdf2PasswordHasherTest` |
| AR-04 | 미가입과 비밀번호 불일치를 같은 코드로 거절 | ✅ | `AuthServiceTest` · `AuthApiTest` |
| AR-05~06 | 세션 12시간 만료 · 토큰별 로그아웃 | ✅ | `SessionTest` · `AuthServiceTest` |
| GR-01~09 | 그룹 생성 · 초대 · 승인 · 재초대 · 자기 초대 금지 | ✅ | `GroupTest` · `GroupMembershipTest` · `GroupServiceTest` · `GroupApiTest` |
| RR-01~04 | 회의실 그룹 귀속 · 승인 그룹원만 추가·조회·예약 | ✅ | `RoomTest` · `RoomServiceTest` · `RoomApiTest` · `ReservationApiTest` |
| BR-07(개정) | 취소 권한을 **예약자명이 아니라 memberId** 로 판정 | ✅ | `ReservationCancelTest` · `ReservationApiTest` 의 동명이인 케이스 |

### 설계상 짚어둘 점

- **초대장 테이블이 따로 없다.** 초대는 `PENDING` 상태의 `GroupMembership` 이다. 덕분에 "초대 → 수락 → 그룹원" 이 상태 전이 하나로 끝나고, 수락 시 레코드를 옮겨 담을 일이 없다.
- **거절한 사람의 재초대는 새 행을 만들지 않는다**(GR-07). 같은 사람이 초대·거절을 반복해도 `(groupId, memberId)` 는 항상 한 행이다.
- **권한 검사가 입력 검증보다 앞선다**(회의실 추가). 남의 그룹에 대고 이름 길이 규칙을 캐낼 수 없다. 반대로 예약 생성은 시간 규칙이 앞서는데, 이는 MVP의 검증 순서를 깨지 않기 위한 의도된 예외다.
- **인증이 바디 파싱보다 앞선다.** `@LoginMember` 를 컨트롤러 메서드의 첫 파라미터에 두어, 토큰 없는 요청은 JSON 문법 오류가 있어도 `401` 로 끝난다.
- **`data.sql` 을 버렸다.** 비밀번호 해시를 SQL에 하드코딩하지 않기 위해 `DemoDataInitializer`(`@Profile("!test")`)가 실제 가입 경로로 데모 데이터를 만든다.
- **Spring Security를 도입하지 않았다.** 필요한 것이 "헤더의 토큰으로 회원을 찾아 넘기기" 하나뿐이라 `HandlerMethodArgumentResolver` 로 충분하다. 의존성 0 추가.

### 프론트 (Phase 8)

앱 쉘이 **로그인 → (예약 | 그룹) 탭** 으로 바뀌었다. `App.tsx` 가 `useAuth` 상태(`loading`/`anonymous`/로그인)로 분기하고, 로그인한 회원이 바뀌면 `key={member.id}` 로 `SignedInShell` 을 갈아 끼워 앞 사용자의 그룹이 한 프레임이라도 비치지 않게 한다. 그룹이 하나도 없으면 빈 격자 대신 "그룹을 만들거나 초대를 수락하세요" 안내를 보인다.

| 파일 | 역할 | 테스트 |
|------|------|--------|
| `auth/tokenStorage.ts` | 토큰 보관 | 6 |
| `api/auth.ts` · `api/groups.ts` | fetch 래퍼 (Bearer 헤더) | 9 · 10 |
| `hooks/useAuth.ts` · `useGroups.ts` | 인증·그룹 상태 | 14 · 13 |
| `pages/LoginPage.tsx` · `GroupsPage.tsx` | 가입/로그인 · 그룹/초대/명단 | 10 · 15 |
| `components/RoomFormDialog.tsx` | 그룹에 회의실 추가 | 10 |
| `App.tsx` | 쉘·탭·로그인 게이트 | 10 |

### 데모 계정 (`DemoDataInitializer`, `!test` 프로파일)

`owner@sqisoft.com` / `member@sqisoft.com`, 비밀번호 `password123`. 그룹 "SQI 개발팀" 에 회의실 4개(대회의실·중회의실·소회의실1·2). 인메모리 DB라 기동할 때마다 다시 만든다.

### 이 증분에서 새로 생긴 이월 항목

| 항목 | 내용 |
|------|------|
| 만료 세션 정리 | 만료된 `auth_session` 행이 쌓인다. 인메모리 DB라 재기동으로 비워지지만, 영속 DB로 가면 정리 작업이 필요하다 |
| 로그인 시도 제한 | 횟수 제한·계정 잠금이 없다. 사내망 전제의 MVP 범위 밖 |
| 그룹 탈퇴·강퇴 | `APPROVED` 에서 나가는 전이가 없다. 그룹장 위임·그룹 삭제도 범위 밖 |
| 회의실 수정·삭제 — 화면 | 백엔드(W10)만 있다. 수정 폼·삭제 확인 대화상자·거절 문구·권한별 버튼 노출·삭제된 방 조작 안내가 없다 |
| 삭제·예약 생성 경합 | 확인과 삭제 사이에 트랜잭션·잠금이 없어 고아 예약이 생길 수 있다(PRD 가정 A-4 로 수용). 계획서 W13 에서 다룬다 |
| 초대 메일 발송 | 이미 가입한 이메일만 초대할 수 있다(GR-09). 미가입자 초대는 메일 발송이 전제라 범위 밖 |
