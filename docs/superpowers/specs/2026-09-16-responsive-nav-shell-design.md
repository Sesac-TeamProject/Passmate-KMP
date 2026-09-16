# 반응형 내비게이션 셸 설계 (BottomNavigation ↔ NavigationRail)

**작성**: 2026-09-16 · **브랜치**: `feature/responsive-nav-shell` (develop `18d071f` 기반) · **담당**: 홍희표
**요청**: 데스크톱 앱이 창 가로폭에 대응하지 않는다. 좁으면 하단 탭바, 넓으면 좌측 레일로 바뀌어야 한다.
**관계 문서**: `docs/Passmate_코드_패턴_규칙.md`(§2-1 · §2-1-1 · §11 · §11-1 · §11-2 · §14), `docs/superpowers/specs/2026-08-30-home-shell-tabs-design.md`(탭 셸 원 설계)

## 0. 결정 요약

| # | 결정 | 근거 |
|---|---|---|
| 1 | 전환 기준은 **가로폭 600dp** (≥600 → Rail, <600 → BottomBar) | Material3 window size class의 compact/medium 경계. 사용자 결정 2026-09-16 — 가로/세로 비율 기준은 "500x400처럼 작지만 가로가 긴 창"에서 가뜩이나 좁은 폭을 80dp 더 먹는 문제가 있어 반려 |
| 2 | 적용 범위는 **3플랫폼 전부**(Android · Desktop · iOS) | 사용자 결정 2026-09-16. Android 폰·iPhone은 폭이 600dp 미만이라 실질 변화 없음 |
| 3 | Rail 모양은 **하단바를 그대로 세운 형태**(폭 80dp · 아이콘 24 + 라벨 11sp) | 사용자 결정 2026-09-16. 기존 `PassmateBottomTabBar`의 토큰·아이콘·항목 구성을 그대로 재사용해 시안과 어긋나지 않는다 |
| 4 | 넓은 창에서 **본문 최대폭 600dp + 가운데 정렬** | 사용자 결정 2026-09-16. 전 화면이 모바일 폭 기준이라 클램프가 없으면 1600dp 창에서 PIN 입력칸·카드가 화면 끝까지 늘어난다 |
| 5 | M3 `NavigationRail`을 쓰지 않고 **커스텀 `PassmateNavigationRail`** 신설 | 기존 탭바가 이미 M3 `NavigationBar`가 아닌 피그마 v6 커스텀 구현이고, iOS에는 M3가 없다. 3플랫폼 1:1(규칙 §14)을 지키려면 커스텀이어야 한다 |
| 6 | 배치 책임을 **공통 셸 컴포넌트 `PassmateNavShell` 하나**로 모은다 | 탭바를 그리는 코드가 지금 4군데에 흩어져 있다. 반응형을 각각 넣으면 판정이 4벌이 되어 규칙 §2-1-1의 "판정은 한 곳에 둔다"를 어긴다 |
| 7 | Android `Scaffold`를 걷어내고 셸로 대체 | `contentWindowInsets`가 이미 `WindowInsets(0,0,0,0)`이고 snackbar·FAB를 쓰지 않아 `bottomBar` 슬롯 외에 하는 일이 없다. 셸이 같은 일을 하므로 남기면 Android만 구조가 달라진다 |

## 1. 판정 정책 (단일 진실)

### 1-1. Compose — `composeApp/src/commonMain/.../navigation/AppShellLayout.kt` (신규)

```kotlin
enum class AppShellLayout { BOTTOM_BAR, RAIL }

object AppShellLayoutPolicy {
    // Material3 window size class의 compact/medium 경계
    val RAIL_MIN_WIDTH = 600.dp

    // 전 화면이 모바일 폭 기준 시안이라 넓은 창에서 본문을 이 폭으로 묶는다
    val CONTENT_MAX_WIDTH = 600.dp

    fun layoutFor(width: Dp): AppShellLayout {
        return if (width >= RAIL_MIN_WIDTH) AppShellLayout.RAIL else AppShellLayout.BOTTOM_BAR
    }
}
```

- 경계는 **이상(≥)** 이다. 정확히 600dp면 Rail.
- 측정 대상은 **셸이 받은 가용 폭**(창 폭)이지 본문 폭이 아니다. Rail이 80dp를 먹어 본문이 520dp가 되어도 Rail 상태가 유지된다 — 아니면 600dp 근처에서 Rail↔Bar가 무한히 튄다.

### 1-2. iOS 미러 — `iosApp/iosApp/navigation/AppShellLayout.swift` (신규)

같은 이름·같은 값(`railMinWidth = 600` · `contentMaxWidth = 600` · `layoutFor(width:)`)으로 미러한다. 규칙 §14의 "Compose 화면과 iosApp 미러가 1:1인가" 항목 대상이다.

폭 측정은 `horizontalSizeClass`가 아니라 **실제 폭**으로 한다. size class는 iPad 분할 화면에서 경계가 600pt와 어긋나 3플랫폼 판정이 달라진다.

## 2. 셸 컨테이너 `PassmateNavShell`

### 2-1. 책임

탭 내비게이션(Rail 또는 BottomBar)과 본문의 **배치**만 담당한다. 어떤 탭이 켜져 있는지, 탭을 눌렀을 때 무엇을 할지는 지금처럼 호출부(각 `AppNavHost` · `ContentView`)와 `AppShellViewModel`이 정한다.

### 2-2. 시그니처 (Compose — `component/PassmateNavShell.kt` 신규)

```kotlin
@Composable
fun PassmateNavShell(
    selectedTab: AppTab?,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)
```

### 2-3. 레이아웃

```
PassmateNavShell
├ BoxWithConstraints 로 가용 폭 측정 → AppShellLayoutPolicy.layoutFor(maxWidth)
├ RAIL       → Row    { [Rail 80dp] ; ContentArea(weight 1f) }
└ BOTTOM_BAR → Column { ContentArea(weight 1f) ; [BottomTabBar] }
```

- `[...]`는 **`selectedTab != null`일 때만** 그린다. 세션 플로우(`Waiting`·`Play`·`Result`·`Payment`)·`SignIn`·`M-09` 명성·`M-T4` 정산에서 바를 숨기는 기존 규칙(규칙 §2-1-1)이 Rail에도 그대로 적용된다.
- `ContentArea`:
  ```
  Box(fillMaxSize, background = PassmateColors.BackgroundMint, align = TopCenter) {
      Box(Modifier.widthIn(max = CONTENT_MAX_WIDTH).fillMaxSize()) { content() }
  }
  ```
  - `widthIn`이 먼저 들어오는 제약을 600dp로 깎고, 그다음 `fillMaxSize`가 깎인 제약을 채운다. 순서를 바꾸면 클램프가 무시된다.
  - 좌우 여백 색은 `BackgroundMint`다. 화면들이 스스로 `Surface`(흰색)를 깔기 때문에, 여백까지 흰색이면 본문 컬럼이 어디까지인지 보이지 않는다.
- 폭이 600dp 미만이면 클램프가 걸리지 않으므로 **Android 폰(≤430dp)·iPhone은 현재와 픽셀 단위로 동일**하다. 변하는 것은 Desktop · iPad · 태블릿뿐이다.

### 2-4. iOS 미러 (`iosApp/iosApp/component/PassmateNavShell.swift` 신규)

```swift
struct PassmateNavShell<Content: View>: View {
    let selectedTab: AppTab?
    let onSelectTab: (AppTab) -> Void
    @ViewBuilder let content: () -> Content
}
```

`GeometryReader`를 **셸 내부 최상위에만** 두고 자식을 `.frame(width:height:)`로 명시 고정한다. `GeometryReader`는 콘텐츠 크기로 줄지 않고 자식을 topLeading에 붙이므로, 이 고정이 없으면 `NavigationView`(stack)·`TabView` 안에서 레이아웃이 흔들린다. iOS 15 대상이므로 iOS 16+ API(`Layout`·`NavigationStack`)는 쓰지 않는다(규칙 §2-1).

## 3. `PassmateNavigationRail` (신규 공통 컴포넌트)

`component/PassmateNavigationRail.kt`(Compose) + `iosApp/iosApp/component/PassmateNavigationRail.swift`(미러).

| 항목 | 값 | 근거 |
|---|---|---|
| 폭 | 80dp | Material3 `NavigationRail` 기본 폭 |
| 배경 | `PassmateColors.Surface` | 하단바와 동일 |
| 구분선 | 오른쪽 세로 1dp `PassmateColors.Border` | 하단바의 위쪽 1dp Divider를 90° 돌린 것 |
| 항목 | 아이콘 24dp + 라벨 11sp, gap 4dp, 선택 `Primary` / 비선택 `TextTertiary`, 선택 시 Bold | `PassmateBottomTabBar.TabItem`과 동일 — 같은 코드를 공유한다 |
| 항목 배치 | 4개를 8dp 간격으로 **위에서부터** 쌓고, 바 위 가장자리에서 10dp 띄운다 | 사용자 결정 2026-09-16(데스크톱 실행 화면 확인 후, 기존 "세로 가운데 정렬"을 뒤집음) — 데스크톱 사이드 내비는 상단부터 시작하는 것이 자연스럽다. 10dp는 하단바의 `padding(top = 10.dp)`와 같은 값이라 새 숫자가 아니다. 하단바의 6:5:5:5:6 비율을 세로로 쓰지 않는 것은 그대로 — 900dp 높이 창에서 항목이 화면 전체로 흩어진다 |
| 인셋 | `statusBarsPadding()` + `navigationBarsPadding()` | Rail은 화면 높이를 다 쓰므로 상·하단 시스템 바를 스스로 피해야 한다 |

`TabItem`은 지금 `PassmateBottomTabBar.kt`의 `private` 컴포저블이다. Rail과 공유하기 위해 **`component/PassmateTabItem.kt`로 승격**한다(규칙 §11 "화면별 중복 컴포넌트는 공통 컴포넌트로 승격한다"). 아이콘 매핑 `iconFor(tab)`도 함께 옮긴다. 하단바의 시각 결과는 바뀌지 않는다.

## 4. 호출부 변경

| 파일 | 변경 |
|---|---|
| `composeApp/.../navigation/AppNavHost.android.kt` | `Scaffold(contentWindowInsets=0, bottomBar={...}) { inner -> NavHost(Modifier.padding(inner)) }` → `PassmateNavShell(currentTab, onSelectTab) { NavHost(...) }`. `Scaffold`·`innerPadding`·관련 import 제거 |
| `composeApp/.../navigation/AppNavHost.jvm.kt` | 끝의 `Column { Box(weight 1f){ when(currentDestination) }; if (currentTab != null) PassmateBottomTabBar(...) }` → `PassmateNavShell(currentTab, onSelectTab) { when(currentDestination){...} }` |
| `iosApp/iosApp/ContentView.swift` (2곳) | ① 루트 `VStack { TabView ...; PassmateBottomTabBar(...) }` → `PassmateNavShell { TabView ... }` ② push 래퍼(`route.tabBarOwner != nil` 분기)의 `VStack { destinationView; PassmateBottomTabBar(...) }` → `PassmateNavShell { destinationView }` |
| `composeApp/src/jvmMain/.../main.kt` | `Window`에 `state = rememberWindowState(width = 1100.dp, height = 760.dp)` 지정. 현재 무지정(기본 800x600)이라 이미 Rail 구간이지만, 본문 클램프(600) + Rail(80)이 여유 있게 들어가는 크기로 연다 |

탭 선택 콜백은 세 곳 모두 지금과 같다 — Compose는 `shellViewModel.onAction(AppShellAction.SelectTab(it))`, iOS는 `shellViewModel.action(.selectTab($0))`. 게스트 가드(`AppShellViewModel`)는 손대지 않는다.

## 5. 테스트

`composeApp/src/jvmTest/.../navigation/AppShellLayoutTest.kt` (신규)

- `layoutFor(599.dp) == BOTTOM_BAR`
- `layoutFor(600.dp) == RAIL` (경계는 이상)
- `layoutFor(601.dp) == RAIL`
- `layoutFor(0.dp) == BOTTOM_BAR` (첫 측정 프레임 방어)

레이아웃 자체(Row/Column 전환)는 Compose UI 테스트 인프라가 이 리포에 없어 단위 테스트 대상에서 제외하고, 데스크톱 실행 스모크로 확인한다.

## 6. 검증 계획

1. `gradlew.bat :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinJvm` — 3타깃 중 Android·jvm 컴파일 (WSL gradle EIO 회피로 Windows `gradlew.bat` 사용)
2. `gradlew.bat :composeApp:jvmTest --tests "*AppShellLayoutTest*"` — 경계 판정
3. `gradlew.bat :composeApp:run` — 데스크톱 실행 후 창을 좌우로 리사이즈해 600dp 경계에서 Rail ↔ BottomBar 전환, 넓은 창에서 본문 가운데 정렬 확인
4. iOS는 **WSL에서 Swift 컴파일이 불가능하다.** 신규 Swift 3파일을 작성하고 `iosApp.xcodeproj/project.pbxproj`에 수동 등록한 뒤(다음 빈 idx `161`·`162`·`163`, `component`·`navigation` 그룹), `docs/Passmate_Mac_검증_체크리스트.md`에 항목을 추가한다. Mac 검증 전까지 iOS는 **미검증**으로 보고한다.

## 7. 리스크

| 리스크 | 완화 |
|---|---|
| iOS `GeometryReader`가 `NavigationView`(stack)·`TabView` 레이아웃을 흔든다 (iOS 15) | 셸 내부에만 두고 자식을 명시 `frame`으로 고정. Mac 실기기 확인 항목으로 체크리스트에 명시 |
| 본문 600dp 클램프가 Desktop·iPad 전 화면에 영향 | 상수 한 줄(`CONTENT_MAX_WIDTH`)이라 조정·철회가 쉽다. 폰은 무영향 |
| Android `Scaffold` 제거로 인셋 회귀 | 기존 `contentWindowInsets`가 이미 0이고 화면들이 각자 `statusBarsPadding()`을 준다. 하단바의 `navigationBarsPadding()`은 컴포넌트 내부에 있어 그대로 따라온다 |
| 리포 전체에 CRLF 플립 노이즈(53파일) | 실제 수정 파일만 경로로 명시 `git add`. `git commit -- <dir>` 금지 |

## 8. 범위 밖

- 화면별 2열(목록+상세) 태블릿 레이아웃 — 별도 시안이 필요하고 화면마다 네비게이션 규칙을 재설계해야 한다(사용자 반려 2026-09-16)
- Rail 상단 브랜드 마크·헤더 — 시안 v6에 없는 요소(사용자 반려 2026-09-16)
- 탭별 백스택 보존 — 원 설계(2026-08-30 §1-4)에서 이미 범위 밖
