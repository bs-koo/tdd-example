# reservation 테스트 시나리오

> 이 시스템이 **보장하는 동작**을 시나리오 단위로 정리한 문서입니다.
> 클래스 목록이 아니라 "무엇을 보장하는가" 기준으로 묶었습니다. 각 항목 끝의 `클래스명`이 실제 검증 위치입니다.
> 수용 기준 원문은 `.dev/feat-room-reservation/prd.md` (AC 44건), 설계 결정은 `design.md` 참조.

## 총계

| 구분 | 건수 |
|------|------|
| 백엔드 | **179** |
| 프론트 | **108** |
| **합계** | **287** |

실행:
```bash
./gradlew test    # 백엔드 179
npm test          # 프론트 108
```

> ⚠️ 두 명령을 **동시에 돌리지 마십시오.** Gradle 데몬과 vitest 워커가 자원 경합을 일으켜 워커 기동 타임아웃이 발생한 사례가 있습니다. `./gradlew --stop` 후 `npm test` 를 권합니다.

---

# 1. 겹침 판정 — 이 시스템의 심장

**규칙**: 겹침은 반개구간 `[start, end)` 으로 판정한다. `A.start < B.end && B.start < A.end`

## 1.1 판정표 9케이스 (양방향 대칭 포함)

기존 예약 `09:00~10:00` 기준. `TimeSlotTest` 가 `@CsvSource` 로 검증하며, **`overlaps(a,b) == overlaps(b,a)` 대칭까지** 단언합니다.

| # | 요청 구간 | 겹침 | 의미 |
|---|-----------|------|------|
| 1 | 08:00~09:00 | **false** | 직전에 접함 → **예약 허용** |
| 2 | 08:30~09:30 | true | 뒤쪽 부분 겹침 |
| 3 | 09:00~10:00 | true | 완전 동일 |
| 4 | 09:15~09:45 | true | 기존에 포함됨 |
| 5 | 09:30~10:00 | true | 기존에 포함됨 |
| 6 | 08:30~10:30 | true | 기존을 포함함 |
| 7 | 09:30~10:30 | true | 앞쪽 부분 겹침 |
| 8 | 10:00~11:00 | **false** | 직후에 접함 → **예약 허용** |
| 9 | 11:00~12:00 | false | 완전히 분리 |

**1번과 8번이 이 규칙의 존재 이유입니다.** 회의가 끝나는 시각에 다음 회의를 잡을 수 있어야 하는데, 폐구간으로 판정하면 거절됩니다.

## 1.2 겹침 판정의 부수 조건

| 시나리오 | 기대 | 검증 |
|----------|------|------|
| 다른 회의실이면 동일 시간대도 허용 | 201 | `ReservationServiceTest` |
| **CANCELLED 예약은 겹침 검사에서 제외** | 완전히 동일한 시간대도 `false` | `ReservationTest`, `ReservationServiceTest` |
| 취소 후 동일 시간대 재예약 | 성공 | `ReservationServiceCancelTest` |

## 1.3 `TimeSlot` 자체의 계약

| 시나리오 | 기대 |
|----------|------|
| `duration()` | 시작~종료 `Duration` |
| `date()` | 시작 시각의 날짜 |
| 시작 또는 종료가 `null` | `NullPointerException` |
| **시작 == 종료** | **생성자가 거부하지 않는다** |

마지막 항목이 의도적입니다. `09:00~09:00` 을 값 객체가 거부해버리면 **AC-10(0분 예약 → `INVALID_DURATION`)을 판정할 주체가 사라집니다.** 순서·길이 검증은 `ReservationPolicy` 의 책임입니다.

---

# 2. 예약 정책 경계값

**검증 순서가 계약입니다.** `ReservationPolicy.validate()` 는 아래 순서로 판정하며, **먼저 걸리는 것이 이깁니다.**

```
1. reserverName  null·공백·20자 초과   → INVALID_RESERVER_NAME
2. purpose       50자 초과              → INVALID_PURPOSE_LENGTH
3. start/end     30분 단위 아님          → INVALID_TIME_UNIT
4. start         < 현재 시각             → PAST_DATETIME
5. start 날짜     > today + 14일         → TOO_FAR_IN_FUTURE
6. 영업시간 벗어남 / 날짜 불일치          → OUTSIDE_BUSINESS_HOURS
7. duration      < 30분 또는 > 4시간     → INVALID_DURATION
```

> **4번이 6번보다 앞서야 합니다.** 순서를 바꾸면 "오늘 08:30 요청"이 영업시간 오류로 잡혀 **과거 시각 검증이 영영 도달 불가**가 됩니다. 설계 검토에서 이 문제를 코드 작성 전에 잡았습니다(§1 A-1).

## 2.1 허용 경계 (전부 "정확히 그 값")

| 시나리오 | 입력 | 검증 |
|----------|------|------|
| 최소 길이 30분 | `09:00~09:30` | `ReservationPolicyTest`, `ReservationApiTest`(201) |
| 종료가 **정확히 18:00** | `17:30~18:00` | 〃 |
| **정확히 4시간** | `09:00~13:00` | 〃 |
| 시작 == **현재 시각** | `2026-08-25T09:00` (고정 시계) | 〃 |
| **정확히 14일 후** | `2026-09-08` | 〃 |
| 예약자명 **정확히 20자** | | `ReservationPolicyTest` |
| 목적 **정확히 50자** | | 〃 |
| 목적이 빈 문자열 | | 〃 (선택 입력) |

## 2.2 거절 경계 (한 칸 넘은 값)

| 시나리오 | 입력 | code |
|----------|------|------|
| 30분 단위 아님 | `09:15` | `INVALID_TIME_UNIT` |
| 초·나노가 0이 아님 | | `INVALID_TIME_UNIT` |
| 영업 시작 전 (미래일) | `08:30~09:00` | `OUTSIDE_BUSINESS_HOURS` |
| 18:00 초과 | `18:00~18:30` | `OUTSIDE_BUSINESS_HOURS` |
| 0분 | `09:00~09:00` | `INVALID_DURATION` |
| 4시간 초과 | `09:00~13:30` | `INVALID_DURATION` |
| **과거 (당일)** | `08:30~09:00`, 날짜=오늘 | `PAST_DATETIME` |
| 15일 후 | `2026-09-09` | `TOO_FAR_IN_FUTURE` |
| 예약자명 빈 문자열·공백만 | `""`, `"   "` | `INVALID_RESERVER_NAME` |
| 예약자명 **21자** | | `INVALID_RESERVER_NAME` |
| 목적 **51자** | | `INVALID_PURPOSE_LENGTH` |

## 2.3 순서를 고정하는 핵심 대비쌍

**AC-7과 AC-13은 시·분이 `08:30~09:00` 으로 동일합니다. 날짜만 다릅니다.**

| AC | 날짜 | 결과 | 이유 |
|----|------|------|------|
| AC-7 | 내일 | `OUTSIDE_BUSINESS_HOURS` | 미래라 4번을 통과 → 6번에 도달 |
| AC-13 | **오늘** | `PAST_DATETIME` | `08:30 < now(09:00)` 이라 **4번에서 잡힘** |

이 두 건이 검증 순서를 고정합니다. `ReservationPolicyTest` 가 별도 테스트로 분리해 검증합니다.

## 2.4 시각 의존성 제거

`Clock` 을 주입하며 **프로덕션 전역에 무인자 `now()` 가 0건**입니다. 테스트는 `Clock.fixed(2026-08-25T09:00 KST)` 를 씁니다. `TimeSlotTest` 가 고정 시계 자체를 기준선으로 단언합니다.

---

# 3. 예약 생성

| 시나리오 | 기대 | 검증 |
|----------|------|------|
| 모든 규칙 통과 | 201 + **7필드**(id·roomId·reserverName·purpose·startAt·endAt·status) | `ReservationApiTest` |
| 생성된 예약을 저장소에서 재조회 가능 | id 채워짐 | `ReservationServiceTest` |
| 커맨드 필드가 그대로 반영 | | 〃 |
| 존재하지 않는 회의실 | 404 `ROOM_NOT_FOUND` | `ReservationServiceTest`, `ApiErrorMappingTest` |
| 겹침 | 409 `OVERLAPPING_RESERVATION` | 〃 |

**`reserve()` 흐름은 순서가 고정입니다** — 오류 코드를 결정하기 때문입니다:

```
new TimeSlot → policy.validate → roomId 존재 검사 → 겹침 검사 → create → return save()
```

`roomId` 존재 검사가 **정책 검증 이후**에 옵니다. 예약자명이 함께 비어 있으면 `INVALID_RESERVER_NAME` 이 먼저 나옵니다.

---

# 4. 예약 취소

## 4.1 권한 판정

| 시나리오 | 기대 | 검증 |
|----------|------|------|
| 본인 이름으로 취소 | 204, status → `CANCELLED` | `ReservationCancelTest`, `ReservationApiTest` |
| **타인 이름으로 취소** | 403 `NOT_RESERVER`, **status는 `ACTIVE` 유지** | 〃 |
| 이름이 `null` | `NOT_RESERVER` | `ReservationCancelTest` |
| 공백이 포함된 이름 | trim 후 비교하여 성공 | 〃 |
| 이미 취소된 예약 재취소 | 409 `ALREADY_CANCELLED` | 〃 |
| 존재하지 않는 예약 | 404 `RESERVATION_NOT_FOUND` | 〃 |

## 4.2 동시 위반 우선순위

**이미 CANCELLED인 예약을 타인이 취소하려 하면 `NOT_RESERVER` 가 이깁니다.**

권한 확인이 상태 검사보다 앞섭니다 — 권한 없는 사람에게 "그 예약은 이미 취소됐습니다"라는 정보를 주지 않기 위해서입니다. `ReservationCancelTest` 가 전용 테스트로 고정합니다.

## 4.3 영속화

| 시나리오 | 검증 |
|----------|------|
| 취소가 저장소에 반영된다 | `ReservationServiceCancelTest` |
| **준영속 엔티티를 cancel 후 save 하면 재조회 시 CANCELLED** | `JpaReservationRepositoryTest` (AC-39) |

**AC-39는 리뷰에서 추가됐습니다.** `ReservationService.cancel()` 의 `save()` 호출을 **어느 테스트도 검증하지 못하던 상태**였습니다 — 그 줄을 지워도 167건이 전부 통과했습니다. 원인은 두 가지였습니다:

- `ApiTestSupport` 의 `@Transactional` 이 **관리 상태 엔티티**를 반환해 더티체킹이 `save()` 없이도 UPDATE를 발행
- `InMemoryReservationRepository` 가 **같은 참조**를 보관해 맵 안 객체가 이미 변이돼 있음

Fake가 준영속 복사본을 반환하도록 고친 뒤, `save()` 를 주석 처리하면 실제로 테스트가 깨지는 것을 확인했습니다.

---

# 5. 조회

| 시나리오 | 기대 | 검증 |
|----------|------|------|
| 회의실 전체 목록 | 200, **시드 4건 값 전수 일치**, id 오름차순 | `RoomApiTest` |
| 지정 날짜의 예약만 | 다른 날짜 제외 | 〃 |
| **CANCELLED 제외** | ACTIVE만 | 〃 |
| **예약 없는 날짜** | **200 + 빈 배열** (404가 아니다) | 〃 |
| 존재하지 않는 회의실 | 404 `ROOM_NOT_FOUND` | 〃 |
| 잘못된 날짜 형식 | 400 `INVALID_DATE_FORMAT` | 〃 |
| **없는 방 + 잘못된 날짜 동시** | **400이 404를 이긴다** | 〃 |
| 응답 시각이 **초까지** 포함 | `2026-08-25T09:00:00` | 〃 |

마지막 두 건이 설계 결정입니다:

- **우선순위**: 컨트롤러가 `date` 를 먼저 파싱하고 서비스를 호출하므로 형식 오류가 앞섭니다
- **초 표기**: DTO의 날짜를 `String` 으로 두고 `DateTimeFormatter` 로 직접 포맷합니다. Jackson 기본 직렬화는 초가 0일 때 생략해버립니다

---

# 6. 오류 응답 계약

**오류 코드는 정확히 13종이며, 모든 거절은 이 중 하나로 응답합니다.**

## 6.1 코드 ↔ HTTP 상태 매핑

`ErrorCodeHttpStatusTest` 가 13종을 전수 검증하고, **"모든 코드가 매핑되어 있다"** 는 누락 방지 테스트를 함께 둡니다. 매핑 누락 시 `getOrDefault` 로 조용히 넘기지 않고 `IllegalStateException` 을 던집니다.

| code | HTTP |
|------|------|
| `OVERLAPPING_RESERVATION` | 409 |
| `ALREADY_CANCELLED` | 409 |
| `NOT_RESERVER` | 403 |
| `RESERVATION_NOT_FOUND` | 404 |
| `ROOM_NOT_FOUND` | 404 |
| `INVALID_TIME_UNIT` · `OUTSIDE_BUSINESS_HOURS` · `INVALID_DURATION` · `PAST_DATETIME` · `TOO_FAR_IN_FUTURE` · `INVALID_RESERVER_NAME` · `INVALID_PURPOSE_LENGTH` · `INVALID_DATE_FORMAT` | 400 |

## 6.2 코드 ↔ 한국어 문구

`ReservationErrorCodeMessageTest`(백엔드) 와 `errorMessages.test.ts`(프론트) 가 **각각 13종을 하드코딩 리터럴로** 검증합니다. 양쪽이 독립적으로 문구를 고정하므로 한쪽만 바뀌면 드러납니다. 둘 다 "정확히 13종" 개수 단언을 포함합니다.

## 6.3 13종에 **도달하는** 경로 — `ApiErrorMappingTest` (AC-30)

13종 전부를 **실제 HTTP 요청으로 재현**합니다. 각 케이스는 해당 코드에만 걸리도록 입력이 계산돼 있습니다.

예: `PAST_DATETIME` 은 **어제 09:00~10:00** 으로 유발합니다. 영업시간·단위·기간이 전부 유효해서 과거 검증에만 걸립니다. 당일 08:30을 쓰면 순서가 바뀌어도 통과해버립니다.

## 6.4 13종 **밖으로 새는** 경로 — AC-36 / AC-40

**이 영역이 리뷰에서 발견됐습니다.** 원 AC들은 "정상 입력"과 "정의된 거절 사유"만 기술했고, **계약 밖으로 새는 경로는 어느 AC도 묻지 않았습니다.**

### AC-36: 요청 필드 누락 (리뷰 1회차 발견)

수정 전에는 **HTTP 500** 이 나갔습니다. `LocalDateTime.parse(null)` 의 `NullPointerException` 이 `catch (DateTimeParseException)` 에 걸리지 않아 미처리 예외로 새어나간 것입니다.

| 누락 필드 | 기대 |
|-----------|------|
| `startAt` / `endAt` | 400 `INVALID_DATE_FORMAT` |
| `roomId` | 404 `ROOM_NOT_FOUND` |
| 빈 바디 `{}` | 400 `INVALID_DATE_FORMAT` |

### AC-40: 요청 바디 파싱 실패 (리뷰 2회차 발견)

`HttpMessageNotReadableException` 이 미처리라 `code` 필드 없는 Spring 기본 400이 나갔습니다. **설계 C-7이 이 핸들러를 요구했는데 구현에서 누락돼 있었습니다.**

| 요청 | 기대 |
|------|------|
| JSON 문법 오류 (`{"roomId":1,`) | 400 `INVALID_DATE_FORMAT` |
| `"roomId":"abc"` (문자열 → Long) | 〃 |
| `"roomId":[1,2]` (배열 → Long) | 〃 |
| `"startAt":123` | 〃 (Jackson이 `"123"` 으로 변환 후 날짜 파싱에서 걸림 — 다른 경로) |

### AC-40 회귀 가드 — "하지 말아야 할 것"의 고정

`GET /api/rooms/abc/reservations` (경로 변수 타입 불일치)는 **C-7대로 Spring 기본 400으로 흘려야 합니다.**

```java
jsonPath("$.code").doesNotExist()
```

`MethodArgumentTypeMismatchException` 핸들러를 추가하면 이 테스트가 깨집니다. **과잉 핸들링을 막는 가드**입니다.

> `@ExceptionHandler(Exception.class)` catch-all 도 두지 않습니다. 미처리 예외가 500으로 드러나는 편이 결함 은폐보다 낫습니다 — 실제로 그 덕에 AC-36을 발견했습니다.

---

# 7. 저장소 — Fake ↔ JPA 동치성

**같은 4시나리오를 Fake와 실제 JPA 양쪽에 넣어 같은 결과를 확인합니다.**

| 시나리오 | Fake | JPA |
|----------|------|-----|
| 같은 방, 다른 날짜 → 제외 | `InMemoryReservationRepositoryTest` | `JpaReservationRepositoryTest` |
| 같은 날짜, 다른 방 → 제외 | 〃 | 〃 |
| CANCELLED → 제외 | 〃 | 〃 |
| `startAt` 오름차순 정렬 | 〃 | 〃 |
| 날짜 경계 (당일 00시 포함 / 익일 00시 제외) | — | 〃 |

## 7.1 Fake 자체의 계약

| 시나리오 | 검증 |
|----------|------|
| `save(id=null)` → 1부터 증가하는 id 부여 | `InMemoryReservationRepositoryTest` |
| `save(id≠null)` → 그 id 유지 | 〃 |
| `seed(id=5)` 후 `save` → 새 id는 **6** | 〃 |
| `seed` 에 id가 `null` 이면 예외 | 〃 |
| **`findById` 가 준영속 복사본을 반환** | 〃 |
| **`findActiveByRoomIdAndDate` 도 복사본을 반환** | 〃 |

마지막 두 건이 중요합니다. **Fake가 내부 참조를 그대로 반환하면 실제 JPA보다 관대해집니다** — `findById → 변이` 만으로 저장소가 바뀌어, 뒤따르는 `save()` 가 없어도 테스트가 통과합니다. 실제 DB는 새로 읽으면 새 인스턴스를 주고 `merge` 없이는 반영되지 않습니다.

`seed` 시퀀스 전진(`Math.max`)도 같은 부류입니다. 이게 없으면 `seed(id=1)` 후 `save` 가 id 1을 다시 발급해 **시드를 조용히 덮어씁니다.**

---

# 8. 프론트엔드 (108건)

## 8.1 순수 로직 — `Date` 객체를 쓰지 않는다

### `timeSlots.test.ts` (7)
- `generateTimeSlots()` → **18개**, `09:00`~`17:30`. **전체 배열을 리터럴로 대조**합니다(길이·첫·끝만 맞고 중간이 틀리는 것을 방지)
- `nextSlot`: `09:00→09:30` / `09:30→10:00`(정시 넘김) / `17:30→18:00`(마지막 경계)
- `toServerDateTime("2026-08-26","09:00")` → `"2026-08-26T09:00:00"`

### `reservationLookup.test.ts` (8) — 구간 포함 매칭
`09:00~10:00` 예약 기준:

| 조회 슬롯 | 찾힘 |
|-----------|------|
| `09:00` | ✅ 시작 경계 포함 |
| `09:30` | ✅ **구간 포함** |
| `10:00` | ❌ 종료 경계 제외 (반개구간) |
| `08:30` | ❌ 시작 이전 |

그 외: 다른 `roomId` 제외, `CANCELLED` 제외, 빈 배열 → `undefined`, 여러 예약 중 정확히 골라냄.

### `today.test.ts` (4) — 시간대가 유일한 쟁점
`Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Seoul' })` 로 **실행 머신 TZ와 무관하게** 동작합니다.

| 고정 시각 (UTC) | KST | 기대 |
|-----------------|-----|------|
| `2026-08-26T14:59:59Z` | 08-26 23:59:59 | `"2026-08-26"` |
| `2026-08-26T15:00:00Z` | **08-27 00:00:00** | `"2026-08-27"` |

**이 두 경계가 시간대 처리의 유일한 증거입니다.** 하나만 있으면 UTC로 구현해도 통과합니다.

### `errorMessages.test.ts` (17)
13종 문구 + "정확히 13종" + 알 수 없는 코드 → `"요청을 처리하지 못했습니다."` + **기본 문구 자체를 리터럴로 고정**.

> 마지막 항목이 없으면 `expect(toKoreanMessage(unknown)).toBe(DEFAULT_ERROR_MESSAGE)` 가 **양변이 같은 상수를 참조해 상수값이 무엇이든 통과**합니다.

## 8.2 API 클라이언트

### `client.test.ts` (4)
`ApiError` 가 `code`·`status` 를 보관하고 **`Error` 의 인스턴스**입니다(호출부가 `catch(e)` 표준 처리 가능).

### `reservations.test.ts` (7)
`globalThis.fetch` 를 스텁합니다. **`vi.mock` 모듈 모킹은 설계가 배제**했습니다 — 호이스팅과 경로 문자열에 결합돼 import 경로가 바뀌면 조용히 깨집니다.

| 시나리오 | 검증 |
|----------|------|
| `fetchRooms` → `/api/rooms` | **URL을 리터럴로 단언** |
| `fetchReservations(1, date)` → 쿼리스트링 포함 URL | 〃 |
| `createReservation` → POST + JSON 바디 | 〃 |
| **`cancelReservation` → 204 본문 없음에서도 예외 없음** | 스텁의 `json()` 이 일부러 throw |
| 409 / 404 → `ApiError` 로 reject, code·status 보존 | |
| **500 + JSON 파싱 실패 → `code = 'UNKNOWN'`** | 프론트가 기본 문구로 폴백하는 합류점 |

## 8.3 컴포넌트

### `TimeSlotGrid.test.tsx` (14)
- **72개 셀**(4×18), 회의실 이름 4개 표시
- 예약된 셀에 예약자명·목적이 **셀 안에** 표시 (`within(cell)` 로 검증 — 화면 전체 검색은 다른 셀과 구분되지 않음)
- **구간 포함**: `09:30` 셀에 표시 / `10:00` 셀에 미표시 (같은 렌더에서 긍정·부정 짝)
- **회의실 격리**: `slot-1-09:00` 에 표시 / `slot-2-09:00` 에 미표시
- 빈 슬롯 클릭 → `onEmptySlotClick(roomId, slot)` / 예약 슬롯 클릭 → `onReservedSlotClick(reservation)`
- **CANCELLED 예약이 덮는 슬롯은 빈 슬롯으로 동작**
- **접근성 (AC-37)**: 72개 셀이 전부 **버튼 역할**로 노출, 접근 가능한 이름 규약(`"{회의실} {슬롯} 빈 슬롯"` / `"{회의실} {슬롯} {예약자} {목적}"`), **Enter·Space 키보드 활성화**

> 접근성 테스트는 리뷰에서 추가됐습니다. 셀이 `<div onClick>` 이라 키보드 사용자는 예약도 취소도 할 수 없었는데, **그리드 테스트가 전부 `data-testid` 로 셀을 잡았기 때문에** RED에서 걸리지 않았습니다.

### `ReservationFormDialog.test.tsx` (9)
- `open=false` → 렌더링 안 함
- **시작 시각이 클릭한 슬롯으로 채워짐** (AC-33), **readOnly 실동작 검증**(타이핑해도 안 바뀜)
- 종료 시각 기본값 = 다음 슬롯
- 제출 시 `onSubmit` 이 **정확한 요청 객체**로 1회 호출 (키 5개)
- **목적을 비워도 제출됨** (`purpose: ''`)
- `initialStartTime="14:00"` 케이스 별도 — `09:30` 에만 맞는 하드코딩 방지

### `CancelConfirmDialog.test.tsx` (6)
- `reservation` 이 `null` 이면 열리지 않음
- 확인 → `onConfirm(reservation)` **객체 그대로** (참조 동등성)
- **대화상자 안에 텍스트 입력이 존재하지 않는다**

> 마지막 항목이 **보안 결정을 테스트로 고정한 것**입니다(§9.8). 인증이 없는 상태에서 취소 시 이름을 재입력받게 하면 **타인이 이름을 추측해 넣는 경로가 UI에 열립니다.** 본인 확인은 서버(BR-07)가 하고, UI는 예약 자신의 `reserverName` 을 그대로 되돌려보냅니다. 나중에 "확인 절차니까 이름 입력란을 넣자"는 선의의 변경을 이 테스트가 막습니다.

### `ErrorBanner.test.tsx` (5) / `DateSelector.test.tsx` (6)
- `code=null` → 렌더링 안 함, 코드가 바뀌면 문구도 갱신(하드코딩 방지)
- **`DateSelector`**: 중간 입력도 화면에 보이되(로컬 draft), **완성 형식일 때만 `onChange`**, `date` prop 변경 시 표시값 갱신

## 8.4 훅 — `useReservationBoard.test.ts` (11)

| 시나리오 | 검증 방법 |
|----------|-----------|
| 최초 로드 **N+1 조회** | `fetchRooms` 1회 + `fetchReservations` 회의실 수만큼 |
| 실패 시 `errorCode` 설정 | `ApiError` → `e.code` / 그 외 → `'UNKNOWN'` |
| **성공 후 재조회** | `fetchReservations` 누적 호출이 **2 → 4** |
| **실패 시 재조회 안 함** | **2 그대로** + 스냅샷 유지 |
| **reject 하지 않는다** | `resolves.toBeUndefined()` |
| `cancelReservation` 인자 분해 | `(11, '김본승')` |
| **성공하면 `errorCode` 가 `null` 로 복귀** | 실패 → 성공 순서로 확인 |

**재조회 여부는 상태만 봐서는 알 수 없어 호출 횟수를 직접 단언합니다.** 실패 시 재조회하면 서버 스냅샷이 화면을 덮어써 사용자가 방금 입력한 맥락이 사라집니다.

**`errorCode` 복귀 테스트가 없으면** "배너를 한 번 띄운 뒤 영영 지우지 않는" 구현도 통과합니다.

## 8.5 페이지 통합 — `ReservationPage.test.tsx` (10)

`api` 를 prop으로 주입해 격리합니다(설계 A-3).

| 시나리오 | 검증 |
|----------|------|
| 초기 로드 | 회의실명·예약자명 표시 |
| 빈 슬롯 클릭 → 폼 (AC-33) | 시작 시각 채워짐 |
| **다른 슬롯 연달아 열기 (§9.13.4)** | 이전 슬롯 값이 남지 않음 |
| **AC-34** | 서버 409 → 한국어 배너 |
| 예약 성공 | 폼 닫힘 + 그리드 반영 |
| **AC-35** | 취소 → 재조회 → 슬롯에서 사라짐 + 대화상자 닫힘 |
| 취소 대화상자 닫기 | 취소 요청 미전송 |
| 날짜 변경 | 새 날짜로 재조회 |
| **AC-38** | 타이핑 중 폭주 없음 — 증가분이 **회의실 수만큼 1회분** |

### §9.13.4 재마운트 함정

`ReservationFormDialog` 에는 `initialStartTime` 변경 시 상태를 동기화하는 `useEffect` 가 **없습니다**(YAGNI로 의도적 배제). 따라서 `ReservationPage` 가 `open` prop만 토글하면 **두 번째로 연 슬롯의 시작 시각이 첫 값으로 남습니다.** 조건부 렌더링으로 언마운트해야 합니다.

**단계별 최소 구현이 다음 단계에서 버그를 만든 사례**이며, 설계 검토에서 미리 예측해 기록해뒀기 때문에 테스트에 포함할 수 있었습니다.

### AC-38 폭주 방지

`"2026-08-27"` 10타 입력 시:

| | 수정 전 | 수정 후 |
|---|---------|---------|
| `fetchRooms` | 10회 | **1회** |
| `fetchReservations` | 40회 | **회의실 수만큼 1회분** |
| 중간값(`"2"`, `"2026"`)으로 조회 | 발생 | **없음** |

중간값은 서버가 400으로 거절해 **타이핑 내내 오류 배너가 점멸**했습니다.

---

# 9. 이 테스트 스위트가 실제로 잡은 것

구현 완료 후 리뷰에서 발견된 결함들입니다. **전부 "테스트가 통과한다"와 "검증됐다"가 다르다는 것을 보여줍니다.**

| 결함 | 그때 통과하고 있던 것 | 발견 방법 |
|------|----------------------|-----------|
| 필드 누락 시 **500** | AC 39/39, E2E 14경로 | 실서버에 필드 뺀 요청 |
| **`cancel()` 의 `save()` 미검증** | 265건 전원 통과 | `save()` 삭제 후 재실행 (뮤테이션) |
| 설계 C-7 핸들러 누락 | AC 43/43 | 실서버에 깨진 JSON 6종 |
| 그리드 키보드 조작 불가 | 컴포넌트 테스트 통과 | 코드 리뷰 (`data-testid` 가 비시맨틱 구조를 통과시킴) |
| 날짜 입력 재조회 폭주 | 통합 테스트 통과 | 코드 리뷰 (`toHaveBeenCalledWith` 는 횟수를 안 봄) |

## 검증 기법 메모

- **뮤테이션**: 프로덕션 코드를 일부러 훼손하고 테스트가 깨지는지 확인합니다. 처음부터 통과하는 테스트(회귀 방지용)에 특히 필요합니다
- **실서버 발사**: MockMvc는 프레임워크 계층 일부를 건너뜁니다. 실제 톰캣에서만 드러나는 경로가 있습니다
- **공허한 통과 감시**: 부정 단언("~이 없다")은 대상이 아예 없어도 참입니다. 같은 렌더/응답에서 긍정 단언과 짝지어야 합니다
- **자기참조 단언 감시**: `expect(f(x)).toBe(CONST)` 는 양변이 같은 상수를 참조하면 상수값이 무엇이든 통과합니다
