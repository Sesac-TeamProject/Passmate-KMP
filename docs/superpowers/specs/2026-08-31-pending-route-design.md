# pendingRoute — 로그인 후 원래 가려던 화면으로 복귀 설계

**작성**: 2026-08-31 · **상태**: 확정 (구현 착수 전 설계)
**브랜치**: `feature/pending-route` ← `develop`(690ff90). PR은 `develop` 대상.
**관계 문서**: 규칙 문서 §2-1(라우트 가드)·§7(pendingRoute/pendingAction)·§8(인증 패턴)·§12(가드 시나리오 테스트), 홈 셸 스펙 [2026-08-30-home-shell-tabs-design.md](2026-08-30-home-shell-tabs-design.md) §9(pendingRoute를 범위 밖으로 미룸 — 이 문서가 해소), iOS 15 호환 스펙 [2026-08-31-ios15-compat-design.md](2026-08-31-ios15-compat-design.md) §2-5(최상단 교체 지침 — 이 문서 §4-3이 적용).

## 0. 결정 요약

| 항목 | 결정 | 근거 |
|---|---|---|
| 복귀 범위 | **탭 가드 + 화면 가드** (pendingAction 제외) | 게스트의 유료 방 입장 흐름이 끊기는 문제까지 해소. 액션 재실행은 화면별 직렬화가 필요해 별건 |
| 보관 위치 | **셸(`AppShellViewModel` / iOS `AppShellViewModel`) 메모리** | 규칙 §12가 요구하는 가드 시나리오 테스트를 한 곳에서 작성 가능. 영속화는 §8 참조 |
| pendingRoute 타입 | Compose `NavigationAction?` · iOS `Route?` | 각 플랫폼에서 "인자를 담을 수 있는 유일한 목적지 타입". §2-3 참조 |
| 유실 대응 | **메모리만** (프로세스 사망 시 홈 = 현행 동작으로 폴백) | iOS는 `ASWebAuthenticationSession`(앱 내 시트)이라 무관. Android Custom Tabs 중 프로세스 사망은 드물고, 폴백이 현행 동작이라 회귀 없음 |
| stale 방지 | **SignIn 진입 시 항상 덮어쓴다**(인자 없으면 `null`) | 뒤로가기로 로그인을 벗어나 값이 남아도 다음 진입이 재정의. 로그인 완료는 SignIn 화면에서만 발생하므로 오복귀 창이 없음 |
| 복귀 절차 | **SignIn 제거 후, 목적지가 이미 최상단이 아닐 때만 이동** | 매핑표 대부분이 "가드가 걸린 화면 = 복귀 대상"이라 무조건 이동하면 같은 화면이 두 번 쌓인다. §4-0 |
| Play 복귀 | **제외**(현행 유지 = 홈) | 로그인이 토큰 신원을 바꾸는데 진행 중 세션 참여 자격 처리가 계약상 불명확. §8-1 참조 |

## 1. 배경 — 현재 가드 실태

가드 이벤트는 이미 전 화면에 깔려 있고, **복귀만 없다.**

- Compose `NavigationAction.NavigateToSignIn` 호출처 **13곳**(`RoomListScreen:101` · `ResultScreen:69` · `JoinScreen:74` · `SessionControlScreen:71` · `HostedRoomsScreen:63` · `RoomReportScreen:57` · `EarningsScreen:61` · `PaymentScreen:62` · `SettingsScreen:51` · `MyInfoScreen:76` · `ReputationScreen:56` · `JoinedRoomsScreen:59` · `PlayScreen:83`) + 셸 탭 가드(`AppShellViewModel.onSelectTab`)
- iOS `path.append(.signIn)` 동일 위치(`ContentView.swift` 15곳)

로그인 성공 후 동작은 **무조건 홈 리셋**이다.

- Compose: `SignInEvent.SignInCompleted` → `SignInScreen:58` → `NavigationAction.NavigateToHome`
- iOS: `SignInView(onSignedIn:)` → `ContentView.swift:124` → `path = []; sessionGeneration += 1`

`ContentView.swift:84`에 이미 `// 로그인 후 pendingRoute 복귀는 후속 작업(최상단 교체로 구현, 스펙 §2-5)` 주석이 남아 있다.

## 2. 공통 계약

### 2-1. 타입 변경 (Compose)

```kotlin
// navigation/NavigationAction.kt
// 로그인 유도 — pendingRoute는 로그인 성공 후 복귀할 목적지. null이면 홈으로 (규칙 §7)
data class NavigateToSignIn(val pendingRoute: NavigationAction? = null) : NavigationAction

// 로그인 성공 — 목적지 결정은 셸에 위임한다(pendingRoute 있으면 복귀, 없으면 홈)
data object NavigateAfterSignIn : NavigationAction
```

`pendingRoute`의 타입이 `NavigationAction` 자신이다. 새 타입 없이 인자 있는 목적지(`NavigateToPayment(pin)`)와 탭(`NavigateToTab(MY_INFO)`)이 한 타입으로 표현된다.

기본값 `null` 덕분에 목적지를 지정할 필요가 없는 호출처는 `NavigateToSignIn()`으로 **현행 동작을 그대로 유지**한다.

### 2-2. 셸 상태기계

```kotlin
// AppShellUiState.kt
data class AppShellUiState(
    val isSignedIn: Boolean = false,
    val pendingRoute: NavigationAction? = null
)

// AppShellAction.kt
data class SelectTab(val tab: AppTab) : AppShellAction
data class RememberPendingRoute(val pendingRoute: NavigationAction?) : AppShellAction
data object ResumeAfterSignIn : AppShellAction

// AppShellEvent.kt
data class NavigateToTab(val tab: AppTab) : AppShellEvent
data object RequireSignIn : AppShellEvent
data class ResumePendingRoute(val pendingRoute: NavigationAction) : AppShellEvent
data object NavigateToHome : AppShellEvent
```

동작 규칙은 두 줄이 전부다.

- **세팅**: `NavigateToSignIn` 처리 시 **항상** `RememberPendingRoute`로 덮어쓴다(인자가 없으면 `null`로).
- **소비**: `ResumeAfterSignIn` 수신 시 `pendingRoute`가 있으면 `ResumePendingRoute`, 없으면 `NavigateToHome`을 내고 `pendingRoute`를 비운다.

탭 가드(`onSelectTab`)는 가드에 걸릴 때 자기 자신에게 `pendingRoute = NavigateToTab(tab)`을 저장한 뒤 `RequireSignIn`을 낸다 — 이벤트에 인자를 싣지 않는다.

`pendingRoute`를 내부 필드가 아니라 `uiState`에 두는 이유: 규칙 §7(상태는 `uiState`) 준수 + 단위 테스트 관측.

### 2-3. iOS 미러 — 목적지 타입만 비대칭

iOS는 `pendingRoute: Route?`를 쓴다. 구조·이름·의미는 1:1로 유지하고 **담기는 목적지 타입만** 플랫폼 고유 타입을 쓴다.

| | Compose | iOS |
|---|---|---|
| UiState 필드 | `pendingRoute: NavigationAction?` | `pendingRoute: Route?` |
| Action | `RememberPendingRoute` · `ResumeAfterSignIn` | `.rememberPendingRoute` · `.resumeAfterSignIn` |
| Event | `ResumePendingRoute` · `NavigateToHome` | `.resumePendingRoute` · `.navigateToHome` |

근거:

- Compose `Route`는 **문자열 템플릿**이다(`data object Payment : Route("payment/{pin}")`). Jetpack Navigation이 문자열 라우트로 이동하므로 `{pin}` 자리에 실제 값을 담을 수 없다. 값을 담는 타입은 `NavigationAction`뿐이다.
- iOS `Route`는 **연관값 enum**이라 값을 직접 담고(`case payment(pin: String)`), 탭 루트 케이스(`.home`·`.myInfo` 등)도 이미 갖고 있다. 그리고 iOS에는 `NavigationAction` 타입이 존재하지 않는다 — 이동은 `path.append(.payment(pin:))`로 Route를 직접 쌓는다.

타입까지 맞추는 두 대안은 모두 손해라 기각했다.

- iOS에 `NavigationAction`을 신설 → iOS 셸은 `[Route]`로 도므로 변환 계층만 늘어나는 순수 보일러플레이트.
- Compose `Route`에 인자를 도입 → 라우트 문자열 생성 계층이 추가로 필요하고 `AppNavHost.android.kt` 전체가 흔들린다. iOS 15 작업으로 방금 안정화한 셸을 다시 건드리게 된다.

규칙 §14가 요구하는 1:1은 `UiState/Action/Event`의 구조 일치이며, 위 표대로 충족한다. 셸은 이미 전체가 플랫폼 고유 구조다(Compose `NavHostController` ↔ iOS `[Route]`).

### 2-4. SignIn 화면 계약 변경

로그인 성공·게스트 진입 처리를 셸로 넘긴다. `SignInViewModel`(3플랫폼 모두)은 **무변경**이다 — 바뀌는 것은 컨테이너의 이벤트 배선뿐이다.

| 이벤트 | 현행 | 변경 후 |
|---|---|---|
| `SignInCompleted` (Compose `SignInScreen:58`) | `NavigationAction.NavigateToHome` | `NavigationAction.NavigateAfterSignIn` |
| `signInCompleted` (iOS `ContentView.swift:124` `onSignedIn`) | `path = []; sessionGeneration += 1` | `shellViewModel.action(.resumeAfterSignIn)` |
| `GuestEnterRequested` / `onGuestEnter` | 홈으로 (`NavigateToJoin()` / `path = []`) | **무변경** |

게스트 진입에서 `pendingRoute`를 지우지 않아도 되는 이유: §0의 stale 방지 규칙(SignIn 진입 시 항상 덮어쓴다)에 따라 남은 값은 다음 SignIn 진입에서 반드시 재정의되고, 로그인 완료는 SignIn 화면에서만 발생하므로 오복귀할 창이 없다.

## 3. 가드 매핑

| 호출처 | pendingRoute |
|---|---|
| 탭 가드(셸 `onSelectTab`) | `NavigateToTab(tab)` / iOS `.hostedRooms`·`.joinedRooms`·`.myInfo` |
| Join — 유료 방 게스트 차단·서버 `LoginRequired` | `NavigateToPayment(pin)` / `.payment(pin:)` |
| Join — 로그인 링크(`ClickSignIn`) | 없음 → 홈 |
| RoomList — 호스트 프로필 시트 | `NavigateToRoomList` / `.roomList` |
| Result — 가입 유도 | `NavigateToResult(roomId)` / `.result(roomId:)` |
| Payment — 401·`LoginRequired` | `NavigateToPayment(pin)` / `.payment(pin:)` |
| MyInfo · JoinedRooms · HostedRooms | 각 `NavigateToTab(...)` / 각 탭 루트 Route |
| Reputation · Earnings · Settings · CoinHistory | 각 라우트 |
| RoomReport(roomId) · SessionControl(roomId, pin) | 각 라우트(인자 포함) |
| Play — 가입 유도 | **없음 → 홈 (현행 유지)**, §8-1 |

### 3-1. `JoinEvent` 분리 (부수 작업)

`JoinViewModel`이 세 출처를 `SignInRequested` 하나로 합쳐 내보내고 있어 목적지를 구분할 수 없다.

- `JoinViewModel.kt:84` `onClickSignIn` — 로그인 링크
- `JoinViewModel.kt:123` `joinIfAllowed` — 유료 방 게스트 차단
- `JoinViewModel.kt:146` `handleJoinFailure` — 서버 `LoginRequired`

iOS도 동일하다(`JoinViewModel.swift:79`·`127`·`156`). 두 이벤트로 분리한다.

- `SignInRequested` — 로그인 링크(목적지 없음)
- `SignInRequiredForPaidRoom(pin)` — 유료 방 차단 + `LoginRequired`(목적지 `Payment(pin)`)

## 4. 플랫폼 적용

### 4-0. 복귀 절차 — 3플랫폼 공통 규칙

> **SignIn을 제거한 뒤, 목적지가 이미 최상단이 아닐 때만 이동한다.**

§3 매핑표의 대부분은 **가드가 걸린 화면 자신이 복귀 대상**이다(RoomList·Result·Payment·Reputation·Earnings·Settings·RoomReport·SessionControl 8곳). SignIn만 걷어내면 그 화면이 이미 최상단에 드러나므로, 무조건 이동하면 같은 화면이 두 번 쌓인다.

```
가드 전:  Home → RoomList
SignIn:   Home → RoomList → SignIn
무조건 이동: Home → RoomList → RoomList   ← 뒤로가기가 같은 화면을 다시 보여준다
```

탭 복귀는 영향받지 않는다(`navigateToTab`이 `popUpTo(Home)` + `launchSingleTop`). Join → `Payment(pin)`도 원래 다른 화면이라 무관하다.

**"그 엔트리까지 걷어내고 새로 push"(= 화면 재생성) 대신 재사용을 택한 이유**: Android `popBackStack(route, inclusive)`의 인자 매칭 동작이 Navigation 버전에 따라 다르고 오프라인에서 확인할 수 없으며, iOS는 pop+push가 경로 길이를 두 번 바꿔 iOS 15 호환 스펙 §2-5가 경고한 조용한 실패 위험이 있다. 검증 가능한 쪽을 택했다. 재사용의 대가는 §5에 기록한다.

### 4-1. Android

`handleNavigationAction`은 `NavHostController` 확장함수라 셸 VM에 접근할 수 없다. 확장함수는 그대로 두고 **두 액션만 가로채는 얇은 래퍼**를 `AppNavHost` 안에 둔다(Desktop이 이미 쓰는 형태와 동일).

```kotlin
val onNavigate: (NavigationAction) -> Unit = { action ->
    when (action) {
        is NavigationAction.NavigateToSignIn -> {
            shellViewModel.onAction(AppShellAction.RememberPendingRoute(action.pendingRoute))
            navController.navigate(Route.SignIn.route)
        }
        is NavigationAction.NavigateAfterSignIn ->
            shellViewModel.onAction(AppShellAction.ResumeAfterSignIn)
        else -> navController.handleNavigationAction(action)
    }
}
```

나머지 17개 분기는 손대지 않는다. 각 `composable`의 `onNavigate` 인자를 `navController::handleNavigationAction` → `onNavigate`로 교체한다.

**SignIn 엔트리 제거는 라우트 문자열 매칭에 의존하지 않는다.** SignIn의 composable route는 OAuth 인자가 붙은 `"signIn?accessToken={accessToken}&refreshToken={refreshToken}"`이다. `navigate("signIn")`은 딥링크 매처가 처리해 동작하지만(현행 `AppNavHost.android.kt:81`), `popBackStack("signIn", inclusive = true)`는 destination id 해시로 찾으므로 **조용히 실패할 수 있다**(Navigation 2.7.7). OAuth 콜백의 `handleDeepLink`가 SignIn 엔트리를 하나 더 만들 가능성도 있다.

```kotlin
is AppShellEvent.ResumePendingRoute -> {
    // currentDestination이 SignIn인 동안 pop — 딥링크가 만든 중복 엔트리까지 걷어낸다
    while (navController.currentDestination?.route?.startsWith(Route.SignIn.route) == true) {
        if (!navController.popBackStack()) break
    }
    // §4-0 — 복귀 대상이 이미 최상단이면 이동하지 않는다
    if (navController.currentDestination?.route != event.pendingRoute.destinationTemplate()) {
        onNavigate(event.pendingRoute)
    }
}
is AppShellEvent.NavigateToHome -> navController.navigateHome()
is AppShellEvent.RequireSignIn -> navController.navigate(Route.SignIn.route)
```

중복 판정은 **라우트 템플릿** 비교로 한다. `Route`의 기존 템플릿 상수를 그대로 쓰므로 새 문자열을 만들지 않는다. 인자는 비교하지 않는다 — 복귀 대상의 인자는 가드가 걸린 화면의 것과 항상 같기 때문이다.

```kotlin
// 복귀 중복 판정용 (§4-0). handleNavigationAction의 navigate 대상과 1:1로 유지한다
private fun NavigationAction.destinationTemplate(): String? {
    return when (this) {
        is NavigationAction.NavigateToTab -> tab.route
        is NavigationAction.NavigateToRoomList -> Route.RoomList.route
        is NavigationAction.NavigateToPayment -> Route.Payment.route
        is NavigationAction.NavigateToResult -> Route.Result.route
        is NavigationAction.NavigateToReputation -> Route.Reputation.route
        is NavigationAction.NavigateToEarnings -> Route.Earnings.route
        is NavigationAction.NavigateToSettings -> Route.Settings.route
        is NavigationAction.NavigateToCoinHistory -> Route.CoinHistory.route
        is NavigationAction.NavigateToRoomReport -> Route.RoomReport.route
        is NavigationAction.NavigateToSessionControl -> Route.SessionControl.route
        else -> null
    }
}
```

### 4-2. Desktop

```kotlin
is AppShellEvent.ResumePendingRoute -> {
    routeStack.removeAll { it is JvmDestination.SignIn }
    onNavigate(event.pendingRoute)
    // §4-0 — 같은 화면이 중복 push됐으면 걷어낸다. JvmDestination은 data class/object라 구조적 동등 비교가 된다
    if (routeStack.size >= 2 && routeStack.last() == routeStack[routeStack.lastIndex - 1]) {
        routeStack.removeAt(routeStack.lastIndex)
    }
}
is AppShellEvent.NavigateToHome -> onNavigate(NavigationAction.NavigateToHome)
```

Desktop은 `NavigationAction → JvmDestination` 매핑이 `onNavigate` 안에만 있으므로, 템플릿 비교 대신 **이동 후 중복 제거**로 §4-0을 만족시킨다. 탭 복귀는 `switchTab`이 스택을 통째로 교체하므로 SignIn 제거도 자동 처리된다.

### 4-3. iOS — 목적지 종류에 따라 두 갈래

**A. 탭 루트 복귀**(`.home`·`.hostedRooms`·`.joinedRooms`·`.myInfo`)

```swift
path = []
selectedTab = tab
sessionGeneration += 1
```

현행 로그인 후 동작(`path = []; sessionGeneration += 1`)에 탭 지정만 더한 형태 — 이미 검증된 경로다.

**B. push 라우트 복귀**(`.payment(pin)` 등)

```swift
// SignIn은 로그인 완료 시점에 항상 path.last다. 걷어낸 뒤 목적지가 이미 최상단이 아닐 때만 push (§4-0)
path.removeLast()
if path.last != route {
    path.append(route)   // 1단계 append (iOS 15 호환 스펙 §2-5)
}
```

두 변경을 한 함수 안에서 처리하므로 SwiftUI는 최종 상태만 렌더한다. 결과적으로 §2-5의 "최상단 교체"와 같아 **경로 길이가 늘지 않고**, 2단계 증가 실패를 원천 회피한다. 복귀 대상이 이미 아래에 있으면 길이가 1 줄어드는 pop이 되는데, 이 역시 1단계 변경이라 안전하다.

**B에서 `sessionGeneration`을 올리지 않는 이유**: `.id(sessionGeneration)` 변경은 `NavigationView`를 통째로 재생성하는데, path가 이미 비어있지 않은 채로 시작하는 첫 렌더에서 `NavigationLink(isActive: true)`가 실제 push로 이어지는지 검증된 적이 없다. 검증된 조합(빈 path로 재생성 → 이후 1단계 append)만 쓴다. 지연 push(`DispatchQueue.main.async { path = [target] }`)도 검토했으나 타이밍 의존 + 깜빡임 위험으로 기각했다.

**SignIn 진입 시 pendingRoute 기록**: `ContentView`의 `path.append(.signIn)` 15곳 각각에서 `shellViewModel.action(.rememberPendingRoute(...))`를 함께 호출한다. 목적지가 없는 곳은 `.rememberPendingRoute(nil)`을 명시적으로 호출한다(§0 stale 방지 규칙).

## 5. 알려진 한계 — 3플랫폼 공통

push 라우트로 복귀하면 **그 아래 백스택의 탭 루트는 로그인 이전 세션 상태를 유지**한다. 예: Payment로 복귀 후 뒤로 나오면 홈이 게스트 UI로 보일 수 있다.

- Android: 백스택의 홈 엔트리가 재생성되지 않는다
- Desktop: `routeStack` 아래 항목이 그대로다
- iOS: §4-3 B가 `sessionGeneration`을 올리지 않는다

또한 §4-0에 따라 **복귀 대상이 가드 직전 화면과 같으면 그 화면은 재사용되고 재생성되지 않는다.** 예: Result에서 가입 유도 → 로그인 → Result로 돌아와도 화면이 스스로 새로고침하지 않아 "가입하고 기록 저장" 버튼이 남아 있을 수 있다(claim 자체는 `SignInViewModel.claimPendingGuestRecord()`가 이미 수행한 뒤다).

근본 해법은 규칙 §8이 지정한 `observeCurrentUser()` 스트림이며, 이미 후속 과제로 잡혀 있다(홈 셸 스펙 §9). 이번 범위 밖.

## 6. 테스트

`composeApp/src/jvmTest/.../navigation/AppShellViewModelTest.kt`에 5건 추가 — 규칙 §12가 요구하는 "게스트 → 로그인 유도 → `pendingRoute` 복귀" 시나리오가 여기서 커버된다.

1. 게스트가 로그인 필수 탭 선택 → `uiState.pendingRoute == NavigateToTab(tab)` + `RequireSignIn` 이벤트
2. `RememberPendingRoute(x)` → `uiState.pendingRoute == x`
3. `ResumeAfterSignIn` + 값 있음 → `ResumePendingRoute(x)` 이벤트, `pendingRoute`가 `null`로 비워짐
4. `ResumeAfterSignIn` + 값 없음 → `NavigateToHome` 이벤트
5. `RememberPendingRoute(null)`이 이전 값을 덮어씀 (stale 방지)

`JoinViewModelTest`에 1건 추가 — 유료 방 게스트 입장 시도 → `SignInRequiredForPaidRoom(pin)`.

기존 74건(`composeApp` 21 + `shared` 53) 회귀 통과 필수.

## 7. 검증

**로그인 완료까지 가는 end-to-end 복귀는 백엔드 OAuth 없이 검증할 수 없다.** 목킹 스위치도 리포에 없다(후속 과제). 따라서 검증을 3층으로 나눈다.

| 층 | 대상 | 방법 |
|---|---|---|
| 복귀 판단 로직 | 있으면 복귀 / 없으면 홈 / stale 방지 | 셸 단위 테스트 5건 (§6) — 완전 커버 |
| 셸 배선 | 이벤트 → 실제 화면 이동 | 상태 주입 기법(iOS 15 세션과 동일)으로 시뮬레이터·에뮬레이터 확인. 검증용 변경은 커밋하지 않는다 |
| 가드 진입 | 게스트 → 로그인 필수 탭 → SignIn 표시 | Android 에뮬레이터·Desktop 실행으로 직접 확인 |

빌드 검증: `sh gradlew :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid` + `sh gradlew :composeApp:jvmTest :shared:jvmTest`. Xcode 빌드 후 `git checkout -- gradlew`(빌드가 실행 권한을 붙임).

## 8. 범위 밖

### 8-1. Play 화면 복귀 — 보류

로그인은 **토큰 신원을 바꾼다**. `ApiClient.kt:56`과 `StompClient.kt:24`가 모두 `tokenStorage.accessToken() ?: tokenStorage.guestToken` 순서라, 회원 토큰이 저장되는 순간 게스트 토큰은 무시된다. `completeSignIn`은 게스트 토큰을 지우지 않지만(`AuthRepositoryImpl.kt:20-26`) 우선순위상 결과는 같다.

Play 화면은 게스트 participationId에 바인딩된 세션에 참여 중이므로, 복귀시키면 STOMP 재연결·답안 제출이 전부 회원 토큰으로 나간다. 서버가 이 회원을 그 방의 participant로 인정하는지는 **claim이 진행 중(RUNNING) 세션의 참여까지 이관하는지**에 달려 있고, 계약 문서 없이는 확인할 수 없다.

**착수 조건**: 계약 문서 확보 후 아래 2개를 확인한다.

1. `POST /participations/claim`(또는 해당 엔드포인트)이 RUNNING 상태 세션의 participation도 회원에게 이관하는가?
2. 이관 후 `GET /rooms/{pin}/session/snapshot`을 회원 토큰으로 조회하면 그 participation 상태가 돌아오는가?

둘 다 예이면 매핑표에 `Play → NavigateToPlay(pin)`을 추가하고 재접속 스냅샷 경로(규칙 §2-1-2)로 검증한다.

### 8-2. 그 밖

- **pendingAction**(로그인 후 하려던 동작 자동 재실행) — 규칙 §7의 나머지 절반. 화면별 액션 직렬화가 필요해 별건.
- **pendingRoute 영속화** — 프로세스 사망 대응. 현재는 홈 폴백(= 현행 동작).
- **`observeCurrentUser()` 스트림** — §5 한계의 근본 해법.
