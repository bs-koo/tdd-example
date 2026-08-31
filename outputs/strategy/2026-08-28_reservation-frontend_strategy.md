# 개선 전략 — 회의실 예약 프론트엔드

- 작성일: 2026-08-28 · 단계 3(개선 전략) · 핵심 모드
- 입력: 브리프(승인) §3·§5·§5.5·§5.6·§11 / 진단 39건(Critical 13 · Major 19 · Minor 7) / 테스트 13파일 108건 실측
- 성격: 주력 1안 + 반대 강도 미니 대안. 제작 명세가 아니라 **결정과 근거**를 담는다.

---

## 1. 주력안 — 리뉴얼급 "단일 보드, 하나의 과업"

### 1.1 한 문장

방·시간 격자를 **하나의 CSS Grid로 통일하고**, 방 이름 문자열을 한 번만 렌더한 채 데스크톱은 가로축=시간 / 모바일은 세로축=시간으로 **축을 전환**한다. 그 위에 무채색 기반 + 포인트 1색 토큰 체계를 얹고, 실패해도 입력이 남는 모달 흐름으로 바꾼다.

### 1.2 해결하는 진단 문제 — 36/39

| 심각도 | 완전 해결 | ID |
|---|---|---|
| Critical | **13 / 13** | A-1 A-2 A-3 A-4 A-5 B-1 B-2 B-5 B-10 B-11 B-12 B-23 B-24 |
| Major | **16 / 19** | A-6 A-8 A-9 A-10 A-11 A-12 B-3 B-4 B-6 B-7 B-13 B-14 B-15 B-16 B-18 B-22 |
| Minor | **7 / 7** | B-8 B-9 B-19 B-20 B-21 B-26 B-27 |

### 1.3 해결하지 않고 남기는 것 — 3건, 이유 포함

| ID | 처리 | 이유 |
|---|---|---|
| **A-7** | **부분** — 날짜 축만 해결(전/다음 날 버튼이 14일 범위 밖으로 못 가게 막고, 과거 날짜에 안내 문구). **슬롯 단위 "지난 시각" 음영은 미해결.** | `TimeSlotGrid`는 `date`도 현재 시각도 모른다. 판정하려면 `now`/`date` prop을 추가해야 하는데 브리프 §5-2가 prop 타입을 불가침으로 뒀다. 선택 게이트에서 "optional prop 1개 추가"를 승인하면 즉시 해결 가능(기존 108건은 이 prop을 넘기지 않으므로 전부 통과). **결정 요청 항목**으로 올린다. |
| **B-17** | **부분** — `<main>`/`<header>`/`<h1>` 랜드마크와 "격자 건너뛰기" 링크(`<a>`, role=link라 72버튼 단언 무관)로 우회 경로를 만든다. **roving tabindex는 채택하지 않는다.** | `TimeSlotGrid.test.tsx:135-175`의 키보드 3건이 `focus()` + Enter/Space에 의존한다. `tabIndex=-1`과 화살표 키 핸들러를 넣어도 통과할 가능성이 높지만, 성공 기준 6을 걸고 검증할 만한 이득이 아니다. 72개 탭 스톱은 스킵 링크로 실질 완화한다. |
| **B-25** | **완화만** — 버튼 접근 이름 `확인`·`닫기`를 그대로 두고, 취소창 본문에 "이 예약을 취소합니다. 되돌릴 수 없습니다"를 텍스트로 명시한다. | 5개 파일 9곳이 이름을 고정한다. 가시 텍스트만 "예약 취소"로 바꾸면 WCAG 2.5.3(Label in Name)과 충돌하므로 **바꾸지 않는 쪽**을 택한다. 손실은 본문 카피로 메운다. |

### 1.4 화면 정보 구조 (위→아래, 단일 페이지)

```
<main>
 ├ <header>  h1 "회의실 예약"  ·  부제: 선택 날짜 + 요일                    ← A-12, B-1
 │            정책 요약 1줄: 09:00-18:00 · 30분 단위 · 최대 4시간 · 14일 이내  ← A-6 (role 없음 = B-26 준수)
 ├ 날짜 바   [이전]  [날짜 input(기존 그대로)]  [다음]  [오늘]              ← A-11 (그리드 밖이라 버튼 자유)
 │            우측: 범례 3종(가용 / 예약됨 / 선택) + "hh:mm 기준"           ← B-20
 ├ 알림 영역 (min-height 예약 — 등장해도 레이아웃이 밀리지 않음)             ← B-19
 │            ErrorBanner(role=alert, 무변경) + 형제 버튼 "오류 알림 닫기" · "다시 시도"  ← A-9, B-16
 ├ 보드 카드 (단일 표면 1장 — 카드 인 카드 금지)                            ← B-9
 │   ├ 시간 축 헤더 행: [시간] 09:00 09:30 … 17:30 (전부 비-버튼)            ← A-1, B-23 준수
 │   └ 방 4행: [이름 / 정원 / 층] + 슬롯 18개                               ← A-10, B-24(b)
 └ 다이얼로그 2종 (오버레이 + 중앙 패널 + 포커스 트랩 + Escape)              ← B-11
```

- **위계 3단**: 페이지 제목(28px/700) > 방 이름(16px/600) > 슬롯 내용(14px/500 + 13px/400 보조). 그 외 모든 텍스트는 secondary 색.
- **한 화면 하나의 과업**: 보드가 유일한 주 과업. 다이얼로그가 열리면 배경은 오버레이로 비활성.
- **예약 슬롯 내부**: 이름과 목적을 **블록 2줄 + gap 2px**로 분리한다(B-3). 목적은 1줄 말줄임 — CSS 말줄임은 텍스트 노드를 지우지 않으므로 `getByText` 단언이 안전하다.

### 1.5 레이아웃 개념 — 축 전환 그리드

**핵심 제약**: B-24가 방 이름 이중 렌더를 금지하므로, 흔한 "데스크톱용 행 + 모바일용 카드" 반응형 패턴을 쓸 수 없다. → **DOM 하나를 그대로 두고 그리드 축만 회전시킨다.**

DOM 재구성(마크업 **추가만**, 요소 제거·교체 없음):

```
.board                       display:grid
 ├ .row.row--head            display:contents   → 19개 항목
 │    .corner "시간" + .tick × 18 (span, 비-버튼)
 └ .row (방마다)             display:contents   → 19개 항목
      .room-head( span 이름 + span "20명" + span "3층" )   ← 3개를 1셀로 묶어 19개 리듬 유지
      button × 18 (기존 seam 그대로)
```

`display: contents`로 행 래퍼의 박스를 없애면 헤더 행과 4개 방 행의 셀이 **같은 그리드에 놓여 열이 절대 어긋나지 않는다**(A-5 근본 해결). 정원·층을 `.room-head`로 묶는 이유는, 묶지 않으면 그 행의 그리드 항목이 21개가 되어 열 리듬이 깨지기 때문이다.

| 폭 | 축 | 그리드 선언 |
|---|---|---|
| ≥ 1024 | 가로 = 시간 | `grid-auto-flow: row` · `grid-template-columns: var(--room-col) repeat(18, minmax(44px,1fr))` · `--room-col: 180px`(1440) / `148px`(1024) |
| ≤ 720 | **세로 = 시간** | `grid-auto-flow: column` · `grid-template-rows: 40px repeat(18, 44px)` · `grid-auto-columns: minmax(84px,1fr)` |

모바일에서 `grid-auto-flow: column`은 DOM 순서대로 **1열 = 시간 축 헤더(19칸), 2열 = 대회의실, 3열 = 중회의실 …**을 채운다. 390px에서 시간열 56px + 방 4열 × 84px ≈ 392px. **이중 렌더 0건, 미디어 쿼리 1개로 축이 뒤집힌다.**

- 슬롯 최소 타깃 **44 × 44px**(A-2). 1024에서도 44px 유지, 그 아래는 축 전환으로 넘긴다.
- **다중 슬롯 예약의 연결감**(B-4): 칸 병합은 B-27이 막으므로, 연속 점유 칸에 `data-cont` 표식을 붙여 **가운데 칸의 좌측 보더와 라운드를 제거**해 하나의 면으로 잇는다. 반복되는 이름·목적 텍스트는 `visibility: hidden`으로 **시각적으로만** 숨긴다 — 노드는 DOM에 남으므로 `TimeSlotGrid.test.tsx:63-68`의 09:30 칸 단언이 통과한다(`getByText`는 CSS 가시성을 보지 않는다). `display:none`은 쓰지 않는다.
- 1024 이하에서 `:30` 눈금 라벨만 `visibility:hidden`으로 숨겨 축을 정리한다(DOM 유지).

---

## 2. 디자인 토큰 체계 초안

원칙만 참조한다 — 넓은 여백 / 명확한 타이포 위계 / 무채색 + 포인트 1색 / 큰 라운드 / 그림자 최소 / 상태는 색이 아니라 **면과 대비** / 한 화면 하나의 과업. 로고·전용 서체·고유 일러스트·브랜드 hex는 쓰지 않는다.

### 2.1 색 — 무채색 기반

```css
:root{
  color-scheme: light;                 /* B-8 — index.html에 <meta name="color-scheme" content="light">도 함께 */
  --c-bg:#F4F5F7;  --c-surface:#FFFFFF;  --c-surface-sunken:#EDEFF2;
  --c-border:#E4E7EC;  --c-border-strong:#D2D6DD;
  --c-text:#16181D;  --c-text-2:#5A6070;  --c-text-3:#8C93A1;  --c-text-off:#B4B9C4;
  /* 포인트 1색 — 상호작용(호버·선택·포커스·주 버튼) 전용 */
  --c-primary:#2B5CE6;  --c-primary-hover:#2450CC;  --c-primary-active:#1E44B0;
  --c-primary-weak:#E8EEFD;  --c-on-primary:#FFFFFF;
  --c-focus-ring:0 0 0 3px rgba(43,92,230,.28);
}
```

### 2.2 의미색 — 포인트와 **별개 체계**

가용/점유/지난시간은 **채도를 쓰지 않고 면의 밝기와 보더 대비로만** 구분한다. 색약 사용자에게도 성립하고, 포인트 컬러(파랑)는 오직 "지금 상호작용 중"에만 남는다.

```css
:root{
  --c-free-bg:#FFFFFF;  --c-free-bd:#E4E7EC;  --c-free-text:#8C93A1;   /* 가용 */
  --c-busy-bg:#EBEDF1;  --c-busy-bd:#DADEE5;  --c-busy-text:#2E323B;   /* 점유 — 채워진 면 */
  --c-past-bg:#F7F8FA;  --c-past-bd:#EDEFF2;  --c-past-text:#B4B9C4;   /* 지난 시간 */
  --c-danger:#DC2E2E;  --c-danger-weak:#FDECEC;  --c-danger-bd:#F3C7C7;  --c-danger-text:#A32222;
  --c-warn:#C77A0A;    --c-warn-weak:#FDF3E3;    --c-warn-bd:#F0DBB4;    --c-warn-text:#8A5405;
  --c-ok:#17886B;      --c-ok-weak:#E6F4F0;                              /* 성공 — 최소 사용 */
}
```

### 2.3 타이포

```css
--ff-sans:'Pretendard Variable',Pretendard,-apple-system,BlinkMacSystemFont,'Apple SD Gothic Neo',
          'Malgun Gothic','Noto Sans KR','Segoe UI',sans-serif;
--fs-title:28px;   --lh-title:1.35;   --ls-title:-0.02em;  /* h1 · 700 */
--fs-section:20px; --lh-section:1.4;                       /* 섹션 · 600 */
--fs-body:16px;    --lh-body:1.6;                          /* 본문 · 400 — 기계 바닥 충족(B-6) */
--fs-sm:14px;      --lh-sm:1.5;                            /* 슬롯 이름 · 보조 · 500 */
--fs-caption:13px; --lh-caption:1.45;                      /* 시간 눈금 · 범례 · 정원층 (비본문 한정) */
--fw-r:400; --fw-m:500; --fw-sb:600; --fw-b:700;
```

- 서체: **Pretendard Variable**(OFL, 공개 CDN `cdn.jsdelivr.net/gh/orioncactus/pretendard`). 한글 자소 균형과 숫자 폭이 이 화면(시간 축 + 격자)에 맞는다. 사내망에서 CDN이 막혀도 폴백 스택의 `Malgun Gothic`(Windows) / `Apple SD Gothic Neo`(macOS)로 한글이 깨지지 않는다. **Plan B**: Google Fonts만 허용될 경우 `Noto Sans KR` 400/500/700으로 교체하고 나머지 토큰은 그대로 둔다.
- `button, input { font: inherit }` 강제 — UA 기본 13.33px 부산물 제거(B-6·B-7).
- 시간 축과 슬롯 시각에 `font-variant-numeric: tabular-nums`로 자릿수 정렬.
- `--fs-caption:13px`는 **본문이 아닌 눈금·범례·메타 라벨에만** 쓴다. 본문·슬롯 내용은 16/14px 이상을 유지한다.

### 2.4 간격 · 라운드 · 그림자 · 모션

```css
--sp-1:4px; --sp-2:8px; --sp-3:12px; --sp-4:16px; --sp-5:24px; --sp-6:32px; --sp-7:48px; --sp-8:64px;
--r-sm:6px;   /* 슬롯 칸 */    --r-md:10px;  /* 버튼 · 입력 */
--r-lg:16px;  /* 보드 카드 */  --r-xl:20px;  /* 다이얼로그 */   --r-full:999px; /* 범례 점 */
--sh-card:0 1px 2px rgba(16,24,40,.04);                                /* 그림자 최소 — 경계는 보더로 */
--sh-dialog:0 12px 32px rgba(16,24,40,.14), 0 2px 6px rgba(16,24,40,.06);
--c-overlay:rgba(16,24,40,.44);
--dur-fast:120ms; --dur-base:180ms; --ease:cubic-bezier(.2,.7,.3,1);
```

간격은 **위계에 따라 리듬을 준다**(B-9): 섹션 사이 `--sp-7`, 카드 내부 패딩 `--sp-5`, 셀 간 `--sp-1`. 전부 같은 값으로 깔지 않는다. `@media (prefers-reduced-motion: reduce)`에서 트랜지션 `0s`.

---

## 3. hooks 긴장 (브리프 §5.6) — 실증과 결론

### 3.1 읽은 것

`frontend/src/hooks/useReservationBoard.test.ts:156-168` — 테스트명 "createReservation이 실패해도 훅의 async 메서드는 reject하지 않고 정상 resolve한다":

```
:166   await expect(result.current.createReservation(CREATE_REQUEST)).resolves.toBeUndefined();
```

이 한 줄이 **두 가지를 동시에 고정**한다 — (i) rethrow 금지, (ii) 반환값이 `undefined`. 즉 `Promise<boolean>`이나 결과 객체 반환도 이 단언을 깬다. 추가로 `:138-154`가 실패 시 `fetchReservations` 호출 횟수를 2로 고정해 훅 내부 재시도 로직도 막는다. **브리프 §5.6의 서술은 정확했다.**

### 3.2 (a) 화면 계층만으로 우회 — 깨지는 테스트 **0건**

`ReservationPage`가 받은 `api` prop을 `useMemo`로 감싸 훅에 넘긴다. 훅은 손대지 않는다.

```
const outcome = useRef<'ok'|'fail'>('ok');
const api = useMemo(() => ({ ...raw,
  createReservation: async r => { try { const v = await raw.createReservation(r); outcome.current='ok'; return v; }
                                  catch (e) { outcome.current='fail'; throw e; } },
  // cancelReservation / fetchRooms / fetchReservations 도 동일 위임 + pending 카운터
}), [raw]);
// handleFormSubmit: await board.createReservation(req); if (outcome.current === 'ok') setFormTarget(null);
```

- 예외는 래퍼가 **되던지고 훅이 그대로 잡는다** → `useReservationBoard.ts:70-72`의 `setErrorCode` 경로와 resolve 동작이 무변경이다. 훅 테스트 11건은 자기 fake api를 직접 주입하므로 전부 무영향.
- `ReservationPage.test.tsx`의 api 단언은 전부 **인자·호출 횟수** 기반이고 래퍼는 1:1 위임이라 값이 보존된다 — `:133` `toHaveBeenCalledWith(CREATE_REQUEST)`, `:162` `toHaveBeenCalledWith(11,'김본승')`, `:187` `not.toHaveBeenCalled()`, `:238-241` `fetchReservations` 델타 2 / `fetchRooms` 델타 1.
- **실패 시 폼을 남겨도 안전한 근거**: `ReservationPage.test.tsx:97-113`은 `findByRole('alert')`와 문구만 검증하고 **다이얼로그 생존 여부를 검증하지 않는다.** 성공 시 닫힘은 `:133-135`가 요구하며 `outcome === 'ok'` 경로가 그대로 만족시킨다.
- 같은 래퍼의 pending 카운터로 **B-16 로딩**이, `board.changeDate(board.date)` 재호출로 **재시도**가 성립한다(훅이 이미 노출한 메서드).
- 주의 1: 로딩·인라인 오류에 `role="alert"`를 쓰면 `:111`의 단수 `findByRole('alert')`와 `:199`의 0개 단언이 깨진다 → `role="status"` 또는 무-role만 쓴다.
- 주의 2: 재시도·오류닫기 버튼은 `ErrorBanner` 내부가 아니라 **`ReservationPage`에 형제로** 둔다. `ErrorBanner.test.tsx`가 `code` 단일 prop으로 렌더하므로 prop 추가를 피한다.

### 3.3 (b) §5-3 개정으로 hooks 변경 허용 — 깨지는 테스트 **최소 1건**

- 깨지는 것: `hooks/useReservationBoard.test.ts:156-168`의 단언 `:166`. rethrow든 반환값 변경이든 동일하게 걸린다. → 108 → **107 통과**, 성공 기준 6 즉시 미달.
- 되살리려면 브리프 **§5-3(hooks 불가침)과 §4(테스트 파일 범위 밖) 두 곳을 동시에 개정**해야 하고, "108건 그대로 통과"라는 성공 기준 6의 정의 자체를 바꿔야 한다.
- 얻는 것은 (a)와 **동일한 사용자 가치**(A-3·B-16)뿐이다. 코드가 더 정직해지는 이득은 있으나, 이번 과업의 산출물(시각 리디자인)과 무관한 계약 변경이다.

### 3.4 소신 추천 — **(a) 화면 계층 우회**

훅 계약을 지키면서 A-3·B-16을 **둘 다** 해결하고 108건을 건드리지 않는 경로가 실재한다. (b)는 같은 결과를 얻으려고 승인 2건과 성공 기준 재정의를 요구한다. 훅이 예외를 삼키는 설계 자체는 별도 리팩터링 과제로 분리해 이 리디자인과 묶지 않는다.

---

## 4. 미니 대안 — 최소 개입 (10줄)

1. CSS 단일 파일 1개(~180줄), 토큰 12개(배경·표면·보더·텍스트 3단·포인트 1색·라운드 2단)만 선언한다.
2. `TimeSlotGrid`에 시간 축 헤더 행 + `display:contents` 그리드만 넣어 A-1·A-5를 세운다.
3. 슬롯 44×44px, 점유 칸 회색 면, 이름/목적 2줄 분리로 A-2·B-3를 고친다.
4. 다이얼로그에 오버레이·중앙 정렬·Escape·`aria-modal`을 붙여 B-11을 닫는다.
5. `formTarget`에 `key`를 부여해 B-12(시작 14:00 / 종료 10:00) 상태를 제거한다.
6. 취소창에 방·시각·예약자·목적을 텍스트로 표시해 A-4·B-10을 처리한다.
7. api 래퍼로 A-3(입력 소실)만 막는다. 로딩·재시도(B-16)는 넣지 않는다.
8. h1·`<main>`·웹폰트·`color-scheme`으로 B-1·B-5·B-7·B-8을 최소선까지만 올린다.
9. **포기하는 것**: 정책 사전 노출(A-6), 정원·층(A-10), 날짜 이동 버튼(A-11), 오류 카피·닫기(A-8·A-9), 종료 시각 검증(B-13), 필수 입력 검증(B-14), 데이터 신선도(B-20), 모바일 축 전환(B-18 — 가로 스크롤로 대체), 다중 슬롯 연결감(B-4).
10. 결과: **Critical 13건 해결, Major 19건 중 4건, Minor 7건 중 3건.** 토큰은 남지만 톤 기준(SSOT)으로 쓰기엔 얇다.

---

## 5. 비교표

| | **주력안 (리뉴얼급)** | **미니 대안 (최소 개입)** |
|---|---|---|
| **해결 범위** | **36 / 39** — Critical 13, Major 16(+부분 3), Minor 7 | **20 / 39** — Critical 13, Major 4, Minor 3 |
| **리스크** | 중. 위험 3곳이 특정돼 있다 — ① 축 전환(`display:contents` + `grid-auto-flow:column`)의 실물 렌더 검증 필요 ② 연속 칸 텍스트 `visibility:hidden`이 `TimeSlotGrid.test.tsx:63-68`을 통과하는지 최초 1회 확인 필수 ③ 신규 UI에 `role="alert"` 오사용 시 `ReservationPage.test.tsx:111·199` 파손. 셋 다 `vitest run` 한 번으로 검출되고 롤백 단위가 CSS·JSX 국소라 회복이 싸다. hooks·domain·api 무변경이라 108건의 대부분이 구조적으로 안전. | 낮음. 변경 표면이 작다. 다만 A-6·B-13·B-14를 남기므로 **사용자가 규칙을 어겨야 규칙을 알게 되는 구조**가 유지되고, 서버 왕복 실패가 상시 발생하는 운영 리스크가 그대로 남는다. |
| **작업량** | CSS 신규 ~420줄(토큰 60 + 레이아웃 140 + 컴포넌트 180 + 반응형 40), JSX 수정 6파일(추가 마크업 위주), `index.html` 2줄. 렌더 품질 루프 2~3회(B-22). | CSS ~180줄, JSX 수정 4파일, 렌더 루프 1회. 주력안 대비 약 45%. |

---

## 6. 소신 추천 — **주력안**

미니 대안은 Critical 13건을 닫고도 **성공 기준 2·3이 여전히 미충족**으로 남는다(정책이 사전에 안 보이고, 오류가 다음 행동을 말하지 않는다). 즉 "화면은 멀쩡해졌으나 사내 실사용 목표는 절반만 달성"이라는, 재작업이 예정된 상태다. 반면 주력안의 추가 작업량은 대부분 **CSS 선언과 텍스트 추가**로 회귀 위험이 낮은 종류이고, 실재하는 위험 3곳은 위 표에 특정돼 검증 명령 한 번으로 잡힌다. 보존할 시각 부채가 0인 지금이 토큰 체계를 SSOT로 세울 수 있는 가장 싼 시점이다. **주력안을 채택하고, A-7의 optional prop 1건만 별도 결정으로 올린다.**

---

## 7. 제작 단계 인계 체크리스트

**하드 제약 — 위반 시 성공 기준 6 파탄**

- [ ] `TimeSlotGrid` 렌더 트리 안의 `role=button` 요소는 **정확히 72개**. 시간 축·범례·캡션은 `<span>`/`<div>`로만.
- [ ] 방 이름 문자열은 **화면 전체에서 1회**. 정원·층은 **별도 형제 요소**(exact 매칭이라 한 노드에 합칠 수 없음). 반응형 이중 렌더 금지 — 축 전환으로 처리.
- [ ] 점유 칸 병합(`colspan` / `grid-column: span`) 금지. 연속 칸의 반복 텍스트는 **`visibility:hidden`만**(`display:none` 금지, 노드 제거 금지).
- [ ] 날짜 입력 `type="date"` 금지 · 종료 시각 `<select>` 금지(`<input list>` + `<datalist>`는 가능) · 취소창에 텍스트 입력 추가 금지.
- [ ] 다이얼로그 버튼 접근 이름 `확인`·`닫기` 유지. 페이지의 다른 버튼은 이 두 이름을 쓰지 않는다(오류 닫기는 `aria-label="오류 알림 닫기"`).
- [ ] 상시 노출 요소·로딩·인라인 검증 문구에 **`role="alert"` 금지**. `role="status"` 또는 무-role.
- [ ] `hooks/` · `domain/` · `api/` · `today.ts` · 테스트 파일 **무변경**. `errorMessages.ts`의 13개 문자열도 건드리지 않는다(다음 행동 문구는 `ErrorBanner` 내부 상수로 추가).
- [ ] `ErrorBanner`·`DateSelector`의 prop 타입에 새 prop을 추가하지 않는다. 부가 버튼은 `ReservationPage`에 형제로 배치.
- [ ] 빈 **목적** 제출은 계속 허용해야 한다(`ReservationFormDialog.test.tsx:92-107`). 클라이언트 검증은 **예약자명·종료 시각**까지만.

**토큰 — §2 값을 그대로 쓴다**

- [ ] 배경 `#F4F5F7` / 표면 `#FFFFFF` / 보더 `#E4E7EC` / 본문 `#16181D` / 포인트 `#2B5CE6`
- [ ] 의미색은 포인트와 분리 — 가용 `#FFFFFF`, 점유 `#EBEDF1`, 지난시간 `#F7F8FA`, 오류 `#DC2E2E`, 경고 `#C77A0A`
- [ ] 타이포 28 / 20 / 16 / 14 / 13px, 본문 16px·`line-height:1.6`, `button,input{font:inherit}`
- [ ] 간격 4·8·12·16·24·32·48·64 / 라운드 6·10·16·20 / 그림자는 카드 1단 + 다이얼로그 1단만
- [ ] `:root{ color-scheme: light }` + `<meta name="color-scheme" content="light">`
- [ ] 폰트 `<link>` 1개(Pretendard Variable) + 폴백 스택 전체 기입. CDN 차단 대비 Plan B(Noto Sans KR)를 주석으로 남긴다.
- [ ] 슬롯 최소 타깃 44×44px, 포커스 링 `0 0 0 3px rgba(43,92,230,.28)`을 모든 인터랙티브 요소에 적용

**안티-텔 (회피 대상이 아니라 지켜야 할 제약 — B-9)**

- [ ] 그래디언트 텍스트 · 색 글로우 · 카드 인 카드 · eyebrow 칩 · 동일 카드 3개 그리드 · 과대 h1 · 펄싱 점 **금지**
- [ ] 간격은 위계에 따라 리듬을 갖는다(전부 같은 값 금지)

**검증 — 회귀 판정 기준선 13파일 / 108건**

- [ ] `cd frontend && npx vitest run` → `Test Files 13 passed / Tests 108 passed`
- [ ] `cd frontend && npm run build` → `tsc --noEmit` 포함 성공
- [ ] 1440 / 1024 / 390 세 폭 실물 렌더 캡처 후 시각 비평 → 수정 루프 **최소 1회 · 최대 3회** 기록(B-22)
- [ ] 실물 확인 필수 상태 4종: 초기 로딩 · 예약 폼 열림 · 취소 확인창 · 오류 배너 표시(진단 부록 A.3이 미확보로 남긴 것)
