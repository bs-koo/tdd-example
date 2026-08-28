# reservation 구현 추적

> PRD 요구사항별 구현 상태를 Phase 단위로 추적합니다. Phase 정의는 [`requirements/mvp.md` 7.3](../../requirements/mvp.md#73-구현-phase) 참조.
> 상세 설계·수용 기준은 `.dev/feat-room-reservation/` 의 `prd.md`(AC 39건) / `design.md` / `state.md` 참조.

## 범례

- ✅ 반영됨 — 코드에 구현 완료 + 테스트로 검증됨
- ⬜ 미반영 — 정책/설계만 확정, 코드 미구현

## 전체 상태

**구현 완료 (T1~T23).** TDD RED-GREEN-REFACTOR 사이클로 진행했으며, 프로덕션 코드는 실패 테스트가 선행하지 않으면 작성하지 않았다.

| 항목 | 값 |
|------|-----|
| 백엔드 테스트 | **167** (0 failures / 0 errors / 0 skipped) |
| 프론트 테스트 | **98** (13 파일) |
| 합계 | **265** |
| 백엔드 프로덕션 파일 | 28 |
| 프론트 프로덕션 파일 | 11 |
| RGR 사이클 | 15회 |

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

## Phase 0 · 기반 준비

| 항목 | 상태 | 위치 |
|------|------|------|
| `Room`/`Reservation` 엔티티 | ✅ | `backend/.../domain/Room.java`, `Reservation.java` |
| H2 + JPA 설정 | ✅ | `backend/src/main/resources/application.yml`, `application-test.yml` |
| 회의실 시드 데이터 (4개) | ✅ | `backend/src/main/resources/data.sql` |

> `Room` 은 시드 id를 고정하므로 `@GeneratedValue` 를 쓰지 않는다. `Reservation` 은 `IDENTITY` 채번이다.

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

| 항목 | 내용 |
|------|------|
| 그리드 접근성 | 셀이 `<div onClick>` 이라 키보드로 조작할 수 없다 |
| 취소 대화상자 정보 | 무엇을 취소하는지 표시하지 않는다 (설계 DOM 계약표에 표시 내용 명세가 없었다) |
| N+1 조회 | 위 Phase 5 참조 |
| 날짜 입력 타입 | jsdom 테스트 결정성을 위해 `<input type="text">` 를 썼다. 실제 브라우저에서 네이티브 날짜 피커가 없다 |
| `spring.jpa.open-in-view` | 기본 활성 상태이며 기동 시 경고가 뜬다. 명시적으로 끄는 것이 권장된다 |
