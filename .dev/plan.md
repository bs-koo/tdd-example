# 작업 계획

> 회의실 예약(reservation) 도메인의 구현 이력과 남은 작업. `/gx-tdd --work W07` 처럼 행 하나씩 가져간다.
> 요구사항 원문: [`requirements/mvp.md`](../requirements/mvp.md) · [`requirements/auth-groups.md`](../requirements/auth-groups.md). 진행 추적: [`context/reservation/status.md`](../context/reservation/status.md).

| ID | 작업 | 도메인 | 요구사항 | 의존 | 작업 위치 | 상태 |
|----|------|--------|----------|------|-----------|------|
| W00 | 예약 판정 도메인 (겹침·정책·생성·취소) | reservation | BR-01~08, FR-03~04 | - | feat/room-reservation | 완료 |
| W01 | 조회 API · 오류 코드 매핑 | reservation | FR-01~02, 오류 코드 | W00 | feat/room-reservation | 완료 |
| W02 | 예약 화면 (타임슬롯 그리드) · 리디자인 | reservation | FR-05 | W01 | feat/room-reservation | 완료 |
| W03 | 가입 · 로그인 · 세션 | reservation | AR-01~06 | W00 | feat/room-reservation | 완료 |
| W04 | 그룹 · 초대 · 승인 | reservation | GR-01~09 | W03 | feat/room-reservation | 완료 |
| W05 | 그룹 소유 회의실 · 예약 권한 · 취소 권한 개정 | reservation | RR-01~04, BR-07 개정 | W04 | feat/room-reservation | 완료 |
| W06 | 프론트 로그인 · 그룹 · 회의실 추가 화면 | reservation | AR-01~06, GR-01~09, RR-02 | W05 | feat/room-reservation | 완료 |
| W07 | 만료 세션 정리 | reservation | AR-05 후속 (status.md 이월) | W03 | - | 대기 |
| W08 | 로그인 실패 횟수 제한 · 임시 잠금 | reservation | AR-04 후속 (auth-groups §6) | W03 | - | 대기 |
| W09 | 그룹 탈퇴 | reservation | GR 후속 (auth-groups §2.2·§6) | W04 | - | 대기 |
| W10 | 회의실 수정 · 삭제 | reservation | RR 후속 (auth-groups §6) | W05 | feat/room-edit-delete | 진행 |
| W11 | 그룹 전체 회의실 예약 일괄 조회 (N+1 해소) | reservation | FR-02 후속 (status.md 이월) | W05 | - | 대기 |
| W12 | 지난 시각 슬롯 클릭 차단 | reservation | FR-05 후속 (status.md 이월) | W06 | - | 대기 |
| W13 | 같은 슬롯 동시 요청 제어 | reservation | mvp §8-1 (NFR-04) | W05 | - | 대기 |
| W14 | 반복 예약 (매주 같은 요일) | reservation | mvp §8-3 | W05 | - | 대기 |

## 의존 근거

- W03 ← W00: 세션 만료 판정이 `Clock` 주입 패턴(W00의 QE-1)을 재사용한다.
- W04 ← W03, W05 ← W04: 그룹은 회원 id를, 회의실 귀속은 승인된 멤버십을 전제한다 (auth-groups §2).
- W06 ← W05: 화면이 `GET /api/groups/{id}/rooms` 와 권한 오류 코드에 기대고 있다.
- W07·W08 ← W03: `auth_session`·`app_member` 테이블이 있어야 한다.
- W09 ← W04: 탈퇴는 `APPROVED` 멤버십에서 나가는 전이를 새로 만든다 (현재는 없음, §2.2).
- W10·W11·W13·W14 ← W05: 회의실이 그룹 소유 자원이 된 뒤의 모델을 전제한다.
- W12 ← W06: 화면이 로그인 쉘 안에서 동작하는 현재 구조를 기준으로 한다.
- **대기 W07~W14 는 서로 독립이다.** 파일 경계가 겹치는 곳(W10·W11 모두 `RoomController`/`RoomService`)만 동시에 하지 않는 것이 안전하다.

## 요구사항 열에 관하여

W07~W14 의 요구사항 ID는 **아직 없다.** 원문(`auth-groups.md` §6 범위 밖, `mvp.md` §8 MVP 이후, `status.md` 이월 항목)에 항목으로만 적혀 있고 AR/GR/RR 번호가 매겨지지 않았다. `/gx-tdd` 의 requirements 단계가 PRD와 AC를 만들 때 번호를 붙이고, 완료 후 `status.md` 에 반영한다. 계획에서 새 ID를 지어내지 않았다.

## 시연 전 준비 (반드시 먼저)

1. **Phase 8(W03~W06)이 커밋되지 않았다.** 워킹트리에 수정 64건 + 신규 파일이 있고 마지막 커밋(`8f9ee42`)은 MVP + 리디자인까지다. `/gx-tdd` 는 베이스 브랜치(`main`)에서 새 브랜치를 따므로, **Phase 8이 main에 들어가기 전에 W07을 시작하면 로그인·그룹 코드가 없는 상태에서 출발한다.** 순서: 커밋(`/gx-commit`) → PR(`/gx-pull-request`) → main 머지 → 시연.
2. **`.dev/` 가 `.gitignore` 에 있다** (`.gitignore:24`). gx-tdd 가 plan.md 를 `docs: [plan] W0N 착수` 로 커밋하려면 추적 대상이어야 한다. 시연에서 착수·완료 기록 커밋을 보여 주려면 `.gitignore` 를 `.dev/*` + `!.dev/plan.md` 로 바꾸거나 최초 1회 `git add -f .dev/plan.md` 가 필요하다. (바꾸지 않으면 커밋 단계에서 막히거나 기록이 남지 않는다.)
3. **테스트를 동시에 돌리지 않는다.** `./gradlew test` 와 `npm test` 를 병렬로 돌리면 vitest 워커 기동 타임아웃이 난다. verify 게이트가 두 명령을 순서대로 돌리므로 시연 중에는 문제없지만, 시연 직전에 손으로 확인할 때 주의.
4. 현재 기준선: 백엔드 407 · 프론트 251 · 합계 **658** (0 failures, 2026-11-02 실측). 시연 후 이 숫자가 늘어난 만큼이 새로 추가된 테스트다.

## 시연 추천

TDD 가 왜 필요한지가 드러나려면 **판정 로직이 있고 경계값이 있는** 작업이어야 한다 (`context/reservation/README.md` "배경").

| 시간 | 작업 | 이유 |
|------|------|------|
| 10~15분 | **W07 만료 세션 정리** | AC 3~4개. "정각은 이미 만료" 경계가 `SessionTest` 에 이미 있어 RED 가 선명하다. `Clock.fixed()` 패턴을 그대로 쓴다 |
| 20~30분 | **W08 로그인 실패 제한** | N회째 잠금 · 잠금 해제 시각 정각 · 성공 시 카운터 초기화 · 미가입 이메일에도 같은 응답(AR-04) — 경계가 4개 이상 |
| 30분+ | **W09 그룹 탈퇴** | 상태 전이 하나가 늘며 OWNER 탈퇴 금지·탈퇴 후 재초대 여부·미래 예약 처리 결정이 필요해 design-critic 이 할 일이 있다 |
| 프론트 | **W12 지난 시각 슬롯 차단** | 현재 `ReservationPage.test.tsx` 5건이 "과거 날짜 슬롯 클릭으로 폼이 열린다"를 요구한다 — 시각 주입 seam 을 먼저 만들어야 하는, 테스트가 설계를 이끄는 사례 |

W13(동시성)·W14(반복 예약)는 크기가 커서 시연 한 번에 끝나지 않는다. `phase-implement` 의 "작업 계획으로 분할"을 보여 주는 소재로는 좋다.

## 보류 (계획에 올리지 않음)

| 항목 | 이유 |
|------|------|
| 초대 메일 발송 · 예약 알림 | 메일/메신저 인프라가 전제다. 미가입자 초대(GR-09)도 같은 이유로 묶여 있다 |
| 날짜 입력 `type="date"` 전환 | AC-38(부분 입력 노출)이 막고 있다. 이 AC를 바꾸는 결정이 먼저다 |
| 슬롯 3상태 명도 대비(WCAG 1.4.11) | 디자인 팔레트 결정 사항 (`design-tokens.md` §5). 코드 작업이 아니다 |
| 비밀번호 변경 · 계정 삭제 · 그룹 간 회의실 공유 | `auth-groups.md` §6 범위 밖 |
