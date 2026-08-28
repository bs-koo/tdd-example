# reservation 용어 사전

| 용어 | 설명 |
|------|------|
| 겹침(overlap) | 두 예약 구간이 같은 회의실에서 시간상 교차하는 상태. `A.start < B.end && B.start < A.end`로 판정 |
| 반개구간(half-open interval) | `[start, end)` 형태의 구간 표현. 끝시각과 시작시각이 같은 두 예약은 겹치지 않는다고 정의 |
| TimeSlot | 시작·종료 시각을 감싼 값 객체. `overlaps()` 판정 로직을 가진다 |
| ReservationPolicy | BR-02~BR-06(30분 단위·영업시간·길이·과거 금지·14일 제한)을 검증하는 도메인 서비스. `Clock`을 주입받는다 |
| ACTIVE / CANCELLED | 예약 상태. 취소는 삭제가 아니라 상태 변경으로 처리한다 |
| Clock 주입 | `LocalDateTime.now()`를 직접 호출하지 않고 `java.time.Clock`을 주입받아, 테스트에서 `Clock.fixed()`로 시각을 고정하는 방식 |
| 예약자명(reserverName) | 인증 없이 문자열로 예약자를 식별하는 필드. 취소 시 본인 검증(BR-07)에 사용 |
