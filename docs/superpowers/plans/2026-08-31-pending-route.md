# pendingRoute Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 게스트·세션 만료로 로그인 화면에 보내진 사용자가 로그인 성공 후 원래 가려던 화면으로 돌아오게 한다.

**Architecture:** 가드에 걸린 쪽이 "로그인 후 갈 곳"을 `NavigateToSignIn(pendingRoute)`에 실어 보내고, 셸(`AppShellViewModel`)이 이를 보관했다가 로그인 성공(`NavigateAfterSignIn`) 시 소비한다. pendingRoute의 타입은 Compose가 `NavigationAction?`, iOS가 `Route?` — 각 플랫폼에서 인자를 담을 수 있는 유일한 목적지 타입이다. 복귀는 "SignIn 제거 후 목적지가 이미 최상단이 아닐 때만 이동"한다.

**Tech Stack:** Kotlin 1.9.20 · Compose Multiplatform 1.5.12 · Jetpack Navigation 2.7.7 · Koin 3.5.x · SwiftUI(iOS 15.0 최소 배포 타깃)

**Spec:** `docs/superpowers/specs/2026-08-31-pending-route-design.md`

## Global Constraints

- 브랜치는 `feature/pending-route`(← `develop` 690ff90). **푸시·PR은 사용자 승인 후에만.**
- 새 파일은 LF 개행으로 작성한다.
- 코드 배치는 규칙 §16을 따른다: `private` 메서드는 클래스 상단, `public`은 하단, `init`은 맨 아래. 메서드 안에서 변수 선언을 상단에 모으고 호출은 하단에 모으며 그 사이를 개행한다. 조기 반환보다 `if-else`를 우선하되 중첩 3단계 이상이면 가드로 평탄화한다.
- MVI 3프로퍼티 고정: `uiState`(StateFlow) · `event`(SharedFlow, replay=0) · `onAction`. `onAction` 전용 처리 메서드는 반드시 `private`.
- Compose 화면과 iosApp 미러의 `UiState/Action/Event`는 이름·구조가 1:1이어야 한다(담기는 목적지 타입만 플랫폼 고유 — 스펙 §2-3).
- iOS는 최소 배포 타깃 15.0. iOS 16+ 전용 API를 화면에서 직접 쓰지 않는다. **경로(`path`)를 한 번에 2단계 이상 늘리지 않는다** — 조용히 실패한다.
- 화면 코드에 hex 색상 하드코딩 금지(`PassmateColors` 토큰만).
- `gradlew`에 실행 권한이 없다 → `sh gradlew …`. **Xcode 빌드는 `gradlew`에 실행 권한을 붙이므로 빌드 직후 `git checkout -- gradlew`.**
- pbxproj를 편집하면 **다음 가용 idx = 159**. 그룹 ID(`A10120xx`)는 새로 만들거나 바꾸지 않는다. 편집 후 `plutil -lint` + 중복 ID 검사 필수. (이 계획은 **신규 Swift 파일이 없어 pbxproj를 건드리지 않는다.**)

---

## File Structure

**수정 (Compose)**
- `composeApp/src/commonMain/.../navigation/AppShellUiState.kt` — `pendingRoute` 필드 추가
- `composeApp/src/commonMain/.../navigation/AppShellAction.kt` — `RememberPendingRoute`·`ResumeAfterSignIn` 추가
- `composeApp/src/commonMain/.../navigation/AppShellEvent.kt` — `ResumePendingRoute`·`NavigateToHome` 추가
- `composeApp/src/commonMain/.../navigation/AppShellViewModel.kt` — 보관·소비 로직
- `composeApp/src/commonMain/.../navigation/NavigationAction.kt` — `NavigateToSignIn`을 data class로, `NavigateAfterSignIn` 추가
- `composeApp/src/androidMain/.../navigation/AppNavHost.android.kt` — 셸 배선 + `destinationTemplate()`
- `composeApp/src/jvmMain/.../navigation/AppNavHost.jvm.kt` — 셸 배선
- `composeApp/src/commonMain/.../ui/auth/SignInScreen.kt` — `NavigateAfterSignIn` 배선
- `composeApp/src/commonMain/.../ui/join/JoinEvent.kt`·`JoinViewModel.kt`·`JoinScreen.kt` — 이벤트 분리
- 가드 화면 11개 — pendingRoute 지정

**수정 (iOS)**
- `iosApp/iosApp/navigation/AppShellUiState.swift`·`AppShellAction.swift`·`AppShellEvent.swift`·`AppShellViewModel.swift` — Compose 미러
- `iosApp/iosApp/ContentView.swift` — 셸 배선 + 가드 15곳 pendingRoute 기록
- `iosApp/iosApp/ui/join/JoinEvent.swift`·`JoinViewModel.swift`·`JoinView.swift` — 이벤트 분리

**신규 (테스트 전용)**
- `composeApp/src/jvmTest/.../testing/FakeRoomRepository.kt`
- `composeApp/src/jvmTest/.../ui/join/JoinViewModelTest.kt`

**수정 (문서)**
- `docs/Passmate_Mac_검증_체크리스트.md` — §11 신설

---

## Task 1: Compose 셸 상태기계

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/AppShellUiState.kt`
- Modify: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/AppShellAction.kt`
- Modify: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/AppShellEvent.kt`
- Modify: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/AppShellViewModel.kt`
- Test: `composeApp/src/jvmTest/kotlin/org/sesacteamproject/passmate/navigation/AppShellViewModelTest.kt`

**Interfaces:**
- Consumes: 기존 `NavigationAction.NavigateToTab(tab)`(이미 존재), `IsSignedInUseCase`, `AppTab.requiresSignIn`
- Produces:
  - `AppShellUiState(isSignedIn: Boolean = false, pendingRoute: NavigationAction? = null)`
  - `AppShellAction.RememberPendingRoute(pendingRoute: NavigationAction?)` · `AppShellAction.ResumeAfterSignIn`
  - `AppShellEvent.ResumePendingRoute(pendingRoute: NavigationAction)` · `AppShellEvent.NavigateToHome`

- [ ] **Step 1: 실패하는 테스트 4건을 추가하고 기존 테스트 1건을 확장한다**

`AppShellViewModelTest.kt`의 기존 `guestSelectingSignInRequiredTabRequiresSignIn`에 pendingRoute 단언을 추가한다. 기존 마지막 줄 `assertEquals(false, viewModel.uiState.value.isSignedIn)` **뒤**에 한 줄 삽입:

```kotlin
        assertEquals(
            NavigationAction.NavigateToTab(AppTab.JOINED_ROOMS),
            viewModel.uiState.value.pendingRoute
        )
```

그리고 `tabRouteLookup` 테스트 **앞**에 아래 4건을 추가한다:

```kotlin
    @Test
    fun rememberPendingRouteStoresTarget() = runTest {
        val viewModel = viewModel(isSignedIn = false)

        viewModel.onAction(
            AppShellAction.RememberPendingRoute(NavigationAction.NavigateToPayment("123456"))
        )

        assertEquals(
            NavigationAction.NavigateToPayment("123456"),
            viewModel.uiState.value.pendingRoute
        )
    }

    @Test
    fun resumeAfterSignInWithPendingRouteResumesAndClears() = runTest {
        val viewModel = viewModel(isSignedIn = true)
        val events = mutableListOf<AppShellEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(
            AppShellAction.RememberPendingRoute(NavigationAction.NavigateToPayment("123456"))
        )
        viewModel.onAction(AppShellAction.ResumeAfterSignIn)

        assertEquals(
            listOf<AppShellEvent>(
                AppShellEvent.ResumePendingRoute(NavigationAction.NavigateToPayment("123456"))
            ),
            events
        )
        assertEquals(null, viewModel.uiState.value.pendingRoute)
    }

    @Test
    fun resumeAfterSignInWithoutPendingRouteGoesHome() = runTest {
        val viewModel = viewModel(isSignedIn = true)
        val events = mutableListOf<AppShellEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(AppShellAction.ResumeAfterSignIn)

        assertEquals(listOf<AppShellEvent>(AppShellEvent.NavigateToHome), events)
    }

    // 스펙 §0 stale 방지 — SignIn 진입은 항상 pendingRoute를 재정의한다
    @Test
    fun rememberPendingRouteNullOverwritesPreviousTarget() = runTest {
        val viewModel = viewModel(isSignedIn = false)
        val events = mutableListOf<AppShellEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(AppShellAction.SelectTab(AppTab.MY_INFO))
        viewModel.onAction(AppShellAction.RememberPendingRoute(null))
        viewModel.onAction(AppShellAction.ResumeAfterSignIn)

        assertEquals(
            listOf(AppShellEvent.RequireSignIn, AppShellEvent.NavigateToHome),
            events
        )
        assertEquals(null, viewModel.uiState.value.pendingRoute)
    }
```

- [ ] **Step 2: 테스트가 컴파일 실패하는 것을 확인한다**

Run: `sh gradlew :composeApp:jvmTest --tests '*AppShellViewModelTest*'`
Expected: 컴파일 실패 — `Unresolved reference: RememberPendingRoute`, `Unresolved reference: pendingRoute`

- [ ] **Step 3: `AppShellUiState.kt`를 교체한다**

```kotlin
package org.sesacteamproject.passmate.navigation

// 셸 상태 — 마지막 탭 선택 시점의 로그인 여부(탭 탭마다 동기 재조회, 규칙 §8)와
// 로그인 성공 후 복귀할 목적지(규칙 §7 pendingRoute, 스펙 §2-2)
data class AppShellUiState(
    val isSignedIn: Boolean = false,
    val pendingRoute: NavigationAction? = null
)
```

- [ ] **Step 4: `AppShellAction.kt`를 교체한다**

```kotlin
package org.sesacteamproject.passmate.navigation

sealed interface AppShellAction {
    data class SelectTab(val tab: AppTab) : AppShellAction

    // SignIn 진입 시 항상 호출한다 — 목적지가 없으면 null로 덮어써 이전 값을 무효화한다 (스펙 §0 stale 방지)
    data class RememberPendingRoute(val pendingRoute: NavigationAction?) : AppShellAction

    // 로그인 성공 — pendingRoute 유무로 목적지를 정한다
    data object ResumeAfterSignIn : AppShellAction
}
```

- [ ] **Step 5: `AppShellEvent.kt`를 교체한다**

```kotlin
package org.sesacteamproject.passmate.navigation

sealed interface AppShellEvent {
    data class NavigateToTab(val tab: AppTab) : AppShellEvent

    // 게스트가 로그인 필수 탭을 누름 — 화면을 열지 않고 SignIn으로 (결정 2)
    data object RequireSignIn : AppShellEvent

    // 로그인 성공 + pendingRoute 있음 — SignIn을 걷어내고 목적지로 복귀한다 (스펙 §4-0)
    data class ResumePendingRoute(val pendingRoute: NavigationAction) : AppShellEvent

    // 로그인 성공 + pendingRoute 없음 — 현행대로 홈으로
    data object NavigateToHome : AppShellEvent
}
```

- [ ] **Step 6: `AppShellViewModel.kt`를 교체한다**

```kotlin
package org.sesacteamproject.passmate.navigation

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.mvi.MviViewModel

// 하단 탭 게스트 가드 + pendingRoute 보관 (규칙 §7·§8, 스펙 §2-2).
// 탭을 누를 때마다 로그인 여부를 동기 조회하므로 로그인/로그아웃 후 별도 갱신이 필요 없다.
class AppShellViewModel(
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<AppShellUiState, AppShellAction, AppShellEvent>(AppShellUiState()) {

    private fun onSelectTab(tab: AppTab) {
        val isSignedIn = isSignedInUseCase.invoke()
        val isGuarded = tab.requiresSignIn && !isSignedIn

        _uiState.update {
            if (isGuarded) {
                it.copy(isSignedIn = isSignedIn, pendingRoute = NavigationAction.NavigateToTab(tab))
            } else {
                it.copy(isSignedIn = isSignedIn)
            }
        }
        viewModelScope.launch {
            if (isGuarded) {
                _event.emit(AppShellEvent.RequireSignIn)
            } else {
                _event.emit(AppShellEvent.NavigateToTab(tab))
            }
        }
    }

    private fun onRememberPendingRoute(pendingRoute: NavigationAction?) {
        _uiState.update { it.copy(pendingRoute = pendingRoute) }
    }

    private fun onResumeAfterSignIn() {
        val pendingRoute = _uiState.value.pendingRoute
        val isSignedIn = isSignedInUseCase.invoke()

        _uiState.update { it.copy(isSignedIn = isSignedIn, pendingRoute = null) }
        viewModelScope.launch {
            if (pendingRoute != null) {
                _event.emit(AppShellEvent.ResumePendingRoute(pendingRoute))
            } else {
                _event.emit(AppShellEvent.NavigateToHome)
            }
        }
    }

    override fun onAction(action: AppShellAction) {
        when (action) {
            is AppShellAction.SelectTab -> onSelectTab(action.tab)
            is AppShellAction.RememberPendingRoute -> onRememberPendingRoute(action.pendingRoute)
            is AppShellAction.ResumeAfterSignIn -> onResumeAfterSignIn()
        }
    }
}
```

- [ ] **Step 7: 테스트를 실행해 통과를 확인한다**

Run: `sh gradlew :composeApp:jvmTest --tests '*AppShellViewModelTest*'`
Expected: PASS (9건 — 기존 5건 + 신규 4건)

- [ ] **Step 8: 커밋**

```bash
git add composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/AppShell*.kt \
        composeApp/src/jvmTest/kotlin/org/sesacteamproject/passmate/navigation/AppShellViewModelTest.kt
git commit -m "feat(nav): 셸이 pendingRoute를 보관·소비 — RememberPendingRoute/ResumeAfterSignIn 액션과 ResumePendingRoute/NavigateToHome 이벤트 추가, 탭 가드는 목적 탭을 pendingRoute로 저장 (스펙 §2-2)"
```

---

## Task 2: NavigationAction 타입 변경 (동작 무변경 리팩터)

`NavigateToSignIn`을 인자 있는 `data class`로 바꾸면 `data object`로 쓰던 15곳이 전부 컴파일 실패한다. 이 태스크는 **컴파일을 그린으로 되돌리는 것까지**만 하고 동작은 바꾸지 않는다. 실제 복귀는 Task 3부터다.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/NavigationAction.kt:13`
- Modify: 화면 13개 (아래 목록)
- Modify: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/ui/auth/SignInScreen.kt:58`
- Modify: `composeApp/src/androidMain/kotlin/org/sesacteamproject/passmate/navigation/AppNavHost.android.kt`
- Modify: `composeApp/src/jvmMain/kotlin/org/sesacteamproject/passmate/navigation/AppNavHost.jvm.kt`

**Interfaces:**
- Consumes: Task 1의 `AppShellAction`/`AppShellEvent` (이 태스크에서는 아직 쓰지 않는다)
- Produces:
  - `NavigationAction.NavigateToSignIn(pendingRoute: NavigationAction? = null)` — data class
  - `NavigationAction.NavigateAfterSignIn` — data object

- [ ] **Step 1: `NavigationAction.kt`의 `NavigateToSignIn` 선언을 교체한다**

`NavigationAction.kt:13`의 `data object NavigateToSignIn : NavigationAction` 한 줄을 아래로 바꾼다:

```kotlin
    // 로그인 유도 — pendingRoute는 로그인 성공 후 복귀할 목적지. null이면 홈으로 (규칙 §7, 스펙 §2-1)
    data class NavigateToSignIn(val pendingRoute: NavigationAction? = null) : NavigationAction

    // 로그인 성공 — 목적지 결정은 셸(AppShellViewModel)에 위임한다 (스펙 §2-4)
    data object NavigateAfterSignIn : NavigationAction
```

- [ ] **Step 2: 컴파일이 실패하는 것을 확인한다**

Run: `sh gradlew :composeApp:compileDebugKotlinAndroid`
Expected: FAIL — `Classifier 'NavigateToSignIn' does not have a companion object` 계열 오류가 15곳에서 난다

- [ ] **Step 3: `onNavigate(...)` 호출 13곳 + Desktop 1곳에 `()`를 붙인다**

```bash
grep -rl 'onNavigate(NavigationAction\.NavigateToSignIn)' composeApp/src \
  | xargs sed -i '' 's/onNavigate(NavigationAction\.NavigateToSignIn)/onNavigate(NavigationAction.NavigateToSignIn())/g'
```

대상: `RoomListScreen.kt:101` · `ResultScreen.kt:69` · `JoinScreen.kt:74` · `SessionControlScreen.kt:71` · `HostedRoomsScreen.kt:63` · `RoomReportScreen.kt:57` · `EarningsScreen.kt:61` · `PaymentScreen.kt:62` · `SettingsScreen.kt:51` · `MyInfoScreen.kt:76` · `ReputationScreen.kt:56` · `JoinedRoomsScreen.kt:59` · `PlayScreen.kt:83` · `AppNavHost.jvm.kt:132`

- [ ] **Step 4: Android의 여러 줄 호출 1곳을 손으로 고친다**

`AppNavHost.android.kt`의 `LaunchedEffect` 안:

```kotlin
                is AppShellEvent.RequireSignIn -> navController.handleNavigationAction(
                    NavigationAction.NavigateToSignIn
                )
```

를 아래로 바꾼다:

```kotlin
                is AppShellEvent.RequireSignIn -> navController.handleNavigationAction(
                    NavigationAction.NavigateToSignIn()
                )
```

- [ ] **Step 5: 두 `AppNavHost`의 `when`에 `NavigateAfterSignIn` 분기를 추가한다 (동작은 현행 유지)**

`AppNavHost.android.kt`의 `handleNavigationAction` 안, `is NavigationAction.NavigateToSignIn -> navigate(Route.SignIn.route)` **바로 아래**에 추가:

```kotlin
        // Task 3에서 AppNavHost의 onNavigate 래퍼가 가로챈다. 여기 분기는 when 완전성용이며 현행 동작(홈)을 유지한다
        is NavigationAction.NavigateAfterSignIn -> navigateHome()
```

`AppNavHost.jvm.kt`의 `onNavigate` 안, `is NavigationAction.NavigateToSignIn -> routeStack.add(JvmDestination.SignIn)` **바로 아래**에 추가:

```kotlin
            // Task 3에서 셸로 위임한다. 지금은 현행 동작(홈)을 유지한다
            is NavigationAction.NavigateAfterSignIn -> switchTab(AppTab.HOME)
```

- [ ] **Step 6: `SignInScreen.kt:58`의 로그인 성공 배선을 바꾼다**

```kotlin
                is SignInEvent.SignInCompleted -> onNavigate(NavigationAction.NavigateAfterSignIn)
```

- [ ] **Step 7: 컴파일과 전체 테스트를 확인한다**

Run: `sh gradlew :composeApp:compileDebugKotlinAndroid :composeApp:jvmTest :shared:jvmTest`
Expected: BUILD SUCCESSFUL, 테스트 78건 통과(기존 74 + Task 1 신규 4)

- [ ] **Step 8: 커밋**

```bash
git add composeApp/src
git commit -m "refactor(nav): NavigateToSignIn을 pendingRoute 인자를 갖는 data class로 전환하고 NavigateAfterSignIn 추가 — 호출처 15곳 컴파일 복구, 동작은 현행 유지 (스펙 §2-1·§2-4)"
```

---

## Task 3: Compose 셸 배선 (Android + Desktop)

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/org/sesacteamproject/passmate/navigation/AppNavHost.android.kt`
- Modify: `composeApp/src/jvmMain/kotlin/org/sesacteamproject/passmate/navigation/AppNavHost.jvm.kt`

**Interfaces:**
- Consumes: Task 1의 `AppShellAction.RememberPendingRoute`·`AppShellAction.ResumeAfterSignIn`·`AppShellEvent.ResumePendingRoute`·`AppShellEvent.NavigateToHome`, Task 2의 `NavigationAction.NavigateToSignIn(pendingRoute)`·`NavigationAction.NavigateAfterSignIn`
- Produces: 두 플랫폼 셸이 pendingRoute를 저장·복귀시킨다. 화면 코드는 이 태스크에서 바뀌지 않는다.

- [ ] **Step 1: Android — 파일 상단의 private 헬퍼 2개를 추가한다**

`AppNavHost.android.kt`의 `private fun NavHostController.navigateHome()` **아래**에 추가:

```kotlin
// SignIn 엔트리 제거 — popBackStack(route)는 optional 인자가 붙은 SignIn destination과 id 해시가 달라
// 조용히 실패할 수 있다(Navigation 2.7.7). OAuth 딥링크가 SignIn 엔트리를 하나 더 만들 수도 있어
// currentDestination 기준으로 걷어낸다 (스펙 §4-1)
private fun NavHostController.popSignInEntries() {
    while (currentDestination?.route?.startsWith(Route.SignIn.route) == true) {
        if (!popBackStack()) {
            break
        }
    }
}

// 복귀 중복 판정용 라우트 템플릿 (스펙 §4-0). handleNavigationAction의 navigate 대상과 1:1로 유지한다.
// 인자는 비교하지 않는다 — 복귀 대상의 인자는 가드가 걸린 화면의 것과 항상 같다
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

- [ ] **Step 2: Android — `AppNavHost`에 `onNavigate` 래퍼를 추가한다**

`val activity = LocalContext.current.findComponentActivity()` **바로 아래**에 추가:

```kotlin
    // SignIn 관련 두 액션만 셸로 보내고 나머지 17개 분기는 기존 확장함수가 처리한다 (스펙 §4-1)
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

- [ ] **Step 3: Android — 셸 이벤트 수집을 4분기로 바꾼다**

기존 `LaunchedEffect(shellViewModel) { ... }` 블록 전체를 아래로 교체:

```kotlin
    LaunchedEffect(shellViewModel) {
        shellViewModel.event.collect { event ->
            when (event) {
                is AppShellEvent.NavigateToTab -> navController.navigateToTab(event.tab)
                is AppShellEvent.RequireSignIn -> navController.navigate(Route.SignIn.route)
                is AppShellEvent.ResumePendingRoute -> {
                    navController.popSignInEntries()
                    // 복귀 대상이 이미 최상단이면 이동하지 않는다 — 같은 화면이 두 번 쌓이는 것을 막는다 (스펙 §4-0)
                    if (navController.currentDestination?.route != event.pendingRoute.destinationTemplate()) {
                        onNavigate(event.pendingRoute)
                    }
                }
                is AppShellEvent.NavigateToHome -> navController.navigateHome()
            }
        }
    }
```

- [ ] **Step 4: Android — 모든 화면의 `onNavigate` 인자를 래퍼로 바꾼다**

```bash
sed -i '' 's/onNavigate = navController::handleNavigationAction/onNavigate = onNavigate/g' \
  composeApp/src/androidMain/kotlin/org/sesacteamproject/passmate/navigation/AppNavHost.android.kt
```

- [ ] **Step 5: Android — Task 2에서 넣은 임시 분기의 주석을 갱신한다**

`handleNavigationAction` 안의 `NavigateAfterSignIn` 분기를 아래로 바꾼다:

```kotlin
        // AppNavHost의 onNavigate 래퍼가 가로채므로 여기로는 오지 않는다. when 완전성용 방어 분기
        is NavigationAction.NavigateAfterSignIn -> navigateHome()
```

- [ ] **Step 6: Desktop — `onNavigate`의 SignIn 두 분기를 셸 위임으로 바꾼다**

`AppNavHost.jvm.kt`의 아래 두 줄을

```kotlin
            is NavigationAction.NavigateToSignIn -> routeStack.add(JvmDestination.SignIn)
            // Task 3에서 셸로 위임한다. 지금은 현행 동작(홈)을 유지한다
            is NavigationAction.NavigateAfterSignIn -> switchTab(AppTab.HOME)
```

아래로 교체한다:

```kotlin
            is NavigationAction.NavigateToSignIn -> {
                shellViewModel.onAction(AppShellAction.RememberPendingRoute(action.pendingRoute))
                routeStack.add(JvmDestination.SignIn)
            }
            is NavigationAction.NavigateAfterSignIn ->
                shellViewModel.onAction(AppShellAction.ResumeAfterSignIn)
```

- [ ] **Step 7: Desktop — 셸 이벤트 수집을 4분기로 바꾼다**

기존 `LaunchedEffect(shellViewModel) { ... }` 블록 전체를 아래로 교체:

```kotlin
    LaunchedEffect(shellViewModel) {
        shellViewModel.event.collect { event ->
            when (event) {
                is AppShellEvent.NavigateToTab -> onNavigate(NavigationAction.NavigateToTab(event.tab))
                is AppShellEvent.RequireSignIn -> routeStack.add(JvmDestination.SignIn)
                is AppShellEvent.ResumePendingRoute -> {
                    routeStack.removeAll { it is JvmDestination.SignIn }
                    onNavigate(event.pendingRoute)
                    // 같은 화면이 중복 push됐으면 걷어낸다 — JvmDestination은 data class/object라 구조적 동등 비교가 된다 (스펙 §4-0)
                    if (routeStack.size >= 2 && routeStack.last() == routeStack[routeStack.lastIndex - 1]) {
                        routeStack.removeAt(routeStack.lastIndex)
                    }
                }
                is AppShellEvent.NavigateToHome -> onNavigate(NavigationAction.NavigateToHome)
            }
        }
    }
```

- [ ] **Step 8: 컴파일과 전체 테스트를 확인한다**

Run: `sh gradlew :composeApp:compileDebugKotlinAndroid :composeApp:jvmTest :shared:jvmTest`
Expected: BUILD SUCCESSFUL, 78건 통과

- [ ] **Step 9: Desktop을 실행해 탭 가드 복귀 배선을 눈으로 확인한다**

Run: `sh gradlew :composeApp:run`
확인: 게스트 상태에서 "마이" 탭 → SignIn 화면이 열린다(탭 바 숨김). 로그인 없이 앱을 닫는다. **로그인 완료 복귀는 백엔드 OAuth가 필요해 여기서 확인할 수 없다** — Task 8에서 상태 주입으로 확인한다.

- [ ] **Step 10: 커밋**

```bash
git add composeApp/src/androidMain composeApp/src/jvmMain
git commit -m "feat(nav): Android·Desktop 셸이 pendingRoute를 저장하고 로그인 후 복귀 — SignIn 엔트리는 currentDestination 기준으로 제거, 복귀 대상이 최상단이면 이동 생략 (스펙 §4-0·§4-1·§4-2)"
```

---

## Task 4: JoinEvent 분리 + JoinViewModel 테스트

`JoinViewModel`이 세 출처(로그인 링크·유료 방 게스트 차단·서버 `LoginRequired`)를 `SignInRequested` 하나로 합쳐 내보내 목적지를 구분할 수 없다. 두 이벤트로 나눈다.

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/ui/join/JoinEvent.kt`
- Modify: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/ui/join/JoinViewModel.kt:123,146`
- Modify: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/ui/join/JoinScreen.kt:74`
- Create: `composeApp/src/jvmTest/kotlin/org/sesacteamproject/passmate/testing/FakeRoomRepository.kt`
- Create: `composeApp/src/jvmTest/kotlin/org/sesacteamproject/passmate/ui/join/JoinViewModelTest.kt`

**Interfaces:**
- Consumes: Task 2의 `NavigationAction.NavigateToSignIn(pendingRoute)`, 기존 `NavigationAction.NavigateToPayment(pin)`
- Produces:
  - `JoinEvent.SignInRequiredForPaidRoom(pin: String)`
  - `FakeRoomRepository(roomInfo: RoomInfo?)` — `var roomInfo`, `var joinResult`

- [ ] **Step 1: `FakeRoomRepository`를 만든다**

`composeApp/src/jvmTest/kotlin/org/sesacteamproject/passmate/testing/FakeRoomRepository.kt`:

```kotlin
package org.sesacteamproject.passmate.testing

import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.room.domain.model.CreatedRoom
import org.sesacteamproject.passmate.room.domain.model.HostedRoom
import org.sesacteamproject.passmate.room.domain.model.MyParticipation
import org.sesacteamproject.passmate.room.domain.model.Participant
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository

class FakeRoomRepository(
    var roomInfo: RoomInfo? = null
) : RoomRepository {

    var joinResult: AppResult<MyParticipation> = AppResult.Failure(AppError.Unknown())

    var joinCallCount: Int = 0

    override suspend fun getRoomInfo(pin: String): AppResult<RoomInfo> {
        val room = roomInfo

        return if (room != null) {
            AppResult.Success(room)
        } else {
            AppResult.Failure(AppError.NotFound())
        }
    }

    override suspend fun joinRoom(room: RoomInfo, nickname: String, avatarId: Int?): AppResult<MyParticipation> {
        joinCallCount += 1
        return joinResult
    }

    override suspend fun getParticipants(roomId: Long): AppResult<List<Participant>> {
        return AppResult.Success(emptyList())
    }

    override suspend fun leaveRoom(roomId: Long): AppResult<Unit> {
        return AppResult.Success(Unit)
    }

    override fun myParticipation(): MyParticipation? {
        return null
    }

    override suspend fun getHostedRooms(cursor: String?): AppResult<PagedResult<HostedRoom>> {
        return AppResult.Success(PagedResult(items = emptyList(), nextCursor = null, hasNext = false))
    }

    override suspend fun createRoom(
        title: String,
        questionSetId: Long?,
        isPaid: Boolean,
        entryFee: Int?
    ): AppResult<CreatedRoom> {
        return AppResult.Failure(AppError.Unknown())
    }
}
```

- [ ] **Step 2: 실패하는 테스트를 만든다**

`composeApp/src/jvmTest/kotlin/org/sesacteamproject/passmate/ui/join/JoinViewModelTest.kt`:

```kotlin
package org.sesacteamproject.passmate.ui.join

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.policy.JoinInputPolicy
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.JoinRoomUseCase
import org.sesacteamproject.passmate.testing.FakeAuthRepository
import org.sesacteamproject.passmate.testing.FakeRoomRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class JoinViewModelTest {

    private fun paidRoom(): RoomInfo {
        return RoomInfo(
            roomId = 1L,
            pin = "123456",
            title = "유료 방",
            topic = null,
            status = RoomStatus.WAITING,
            questionCount = 10,
            estimatedMinutes = 15,
            scheduledAt = null,
            participantCount = 3,
            maxParticipants = 30,
            isPaid = true,
            entryFee = 100,
            host = null
        )
    }

    private fun viewModel(roomRepository: FakeRoomRepository, isSignedIn: Boolean): JoinViewModel {
        return JoinViewModel(
            getRoomInfoUseCase = GetRoomInfoUseCase(roomRepository),
            joinRoomUseCase = JoinRoomUseCase(roomRepository),
            isSignedInUseCase = IsSignedInUseCase(FakeAuthRepository(isSignedIn)),
            joinInputPolicy = JoinInputPolicy()
        )
    }

    @BeforeTest
    fun setUp() {
        TestMainDispatcher.install()
    }

    @AfterTest
    fun tearDown() {
        TestMainDispatcher.reset()
    }

    // 규칙 §12 가드 시나리오 — 게스트의 유료 방 입장은 결제 화면을 목적지로 로그인 유도한다 (스펙 §3)
    @Test
    fun guestJoiningPaidRoomRequestsSignInWithPaymentTarget() = runTest {
        val roomRepository = FakeRoomRepository(roomInfo = paidRoom())
        val viewModel = viewModel(roomRepository, isSignedIn = false)
        val events = mutableListOf<JoinEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinAction.ChangePin("123456"))
        viewModel.onAction(JoinAction.ChangeNickname("테스터"))
        viewModel.onAction(JoinAction.ClickJoin)

        assertEquals(JoinEvent.SignInRequiredForPaidRoom("123456"), events.last())
        assertEquals(0, roomRepository.joinCallCount)
    }

    // 로그인 링크는 목적지가 없다 — 로그인 후 홈으로 (스펙 §3)
    @Test
    fun clickingSignInLinkRequestsPlainSignIn() = runTest {
        val roomRepository = FakeRoomRepository()
        val viewModel = viewModel(roomRepository, isSignedIn = false)
        val events = mutableListOf<JoinEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinAction.ClickSignIn)

        assertEquals(listOf<JoinEvent>(JoinEvent.SignInRequested), events)
    }
}
```

- [ ] **Step 3: 테스트가 컴파일 실패하는 것을 확인한다**

Run: `sh gradlew :composeApp:jvmTest --tests '*JoinViewModelTest*'`
Expected: 컴파일 실패 — `Unresolved reference: SignInRequiredForPaidRoom`

- [ ] **Step 4: `JoinEvent.kt`에 새 이벤트를 추가한다**

기존 `data object SignInRequested : JoinEvent` **바로 아래**에 추가:

```kotlin
    // 유료 방 게스트 차단·서버 LoginRequired — 로그인 후 결제 화면으로 복귀한다 (스펙 §3)
    data class SignInRequiredForPaidRoom(val pin: String) : JoinEvent
```

- [ ] **Step 5: `JoinViewModel`의 두 발행 지점을 새 이벤트로 바꾼다**

`JoinViewModel.kt:123`(`joinIfAllowed`의 유료 방 게스트 분기):

```kotlin
            _event.emit(JoinEvent.SignInRequiredForPaidRoom(room.pin))
```

`JoinViewModel.kt:146`(`handleJoinFailure`의 `AppError.LoginRequired` 분기):

```kotlin
                _event.emit(JoinEvent.SignInRequiredForPaidRoom(_uiState.value.pin))
```

`JoinViewModel.kt:84`(`onClickSignIn`)은 **바꾸지 않는다**.

- [ ] **Step 6: 테스트가 통과하는 것을 확인한다**

Run: `sh gradlew :composeApp:jvmTest --tests '*JoinViewModelTest*'`
Expected: PASS 2건

- [ ] **Step 7: `JoinScreen.kt:74`에 새 이벤트 배선을 추가한다**

기존 한 줄을 두 줄로 바꾼다:

```kotlin
                is JoinEvent.SignInRequested -> onNavigate(NavigationAction.NavigateToSignIn())
                is JoinEvent.SignInRequiredForPaidRoom -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToPayment(event.pin))
                )
```

- [ ] **Step 8: 컴파일과 전체 테스트를 확인한다**

Run: `sh gradlew :composeApp:compileDebugKotlinAndroid :composeApp:jvmTest :shared:jvmTest`
Expected: BUILD SUCCESSFUL, 80건 통과(78 + 신규 2)

- [ ] **Step 9: 커밋**

```bash
git add composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/ui/join composeApp/src/jvmTest
git commit -m "feat(join): 유료 방 게스트 차단·LoginRequired를 SignInRequiredForPaidRoom(pin)으로 분리해 로그인 후 결제 화면 복귀 — FakeRoomRepository·JoinViewModelTest 2건 추가 (규칙 §12, 스펙 §3-1)"
```

---

## Task 5: Compose 가드 11곳에 pendingRoute 지정

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/ui/home/RoomListScreen.kt:101`
- Modify: `.../ui/result/ResultScreen.kt:69`
- Modify: `.../ui/payment/PaymentScreen.kt:62`
- Modify: `.../ui/payment/EarningsScreen.kt:61`
- Modify: `.../ui/mypage/MyInfoScreen.kt:76`
- Modify: `.../ui/mypage/JoinedRoomsScreen.kt:59`
- Modify: `.../ui/mypage/ReputationScreen.kt:56`
- Modify: `.../ui/mypage/SettingsScreen.kt:51`
- Modify: `.../ui/hostroom/HostedRoomsScreen.kt:63`
- Modify: `.../ui/hostroom/RoomReportScreen.kt:57`
- Modify: `.../ui/hostroom/SessionControlScreen.kt:71`

**Interfaces:**
- Consumes: Task 2의 `NavigationAction.NavigateToSignIn(pendingRoute)`
- Produces: 없음 (호출처 배선만)

`PlayScreen.kt:83`은 **바꾸지 않는다** — 스펙 §8-1(로그인이 토큰 신원을 바꿔 진행 중 세션 참여 자격이 계약상 불명확)에 따라 현행 유지다.

- [ ] **Step 1: 인자 없는 목적지 6곳을 바꾼다**

`RoomListScreen.kt:101`
```kotlin
                    onNavigate(NavigationAction.NavigateToSignIn(NavigationAction.NavigateToRoomList))
```

`EarningsScreen.kt:61`
```kotlin
                is EarningsEvent.RequireSignIn -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToEarnings)
                )
```

`ReputationScreen.kt:56`
```kotlin
                is ReputationEvent.RequireSignIn -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToReputation)
                )
```

`SettingsScreen.kt:51`
```kotlin
                is SettingsEvent.RequireSignIn -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToSettings)
                )
```

`MyInfoScreen.kt:76`
```kotlin
                is MyInfoEvent.RequireSignIn -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToTab(AppTab.MY_INFO))
                )
```

`JoinedRoomsScreen.kt:59`
```kotlin
                is JoinedRoomsEvent.RequireSignIn -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToTab(AppTab.JOINED_ROOMS))
                )
```

`HostedRoomsScreen.kt:63`
```kotlin
                is HostedRoomsEvent.RequireSignIn -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToTab(AppTab.HOSTED_ROOMS))
                )
```

`MyInfoScreen`·`JoinedRoomsScreen`·`HostedRoomsScreen` 세 파일에는 `import org.sesacteamproject.passmate.navigation.AppTab`을 **추가한다**(세 파일 모두 현재 이 import가 없다). 기존 `import org.sesacteamproject.passmate.navigation.NavigationAction` 바로 위에 알파벳 순으로 넣는다.

- [ ] **Step 2: 인자 있는 목적지 4곳을 바꾼다**

`ResultScreen.kt:69` — 화면이 `roomId: Long` 파라미터를 갖는다
```kotlin
                is ResultEvent.NavigateToSignup -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToResult(roomId))
                )
```

`PaymentScreen.kt:62` — 화면이 `pin: String` 파라미터를 갖는다
```kotlin
                is PaymentEvent.SignInRequired -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToPayment(pin))
                )
```

`RoomReportScreen.kt:57` — 화면이 `roomId: Long` 파라미터를 갖는다
```kotlin
                is RoomReportEvent.RequireSignIn -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToRoomReport(roomId))
                )
```

`SessionControlScreen.kt:71` — 화면이 `roomId: Long`·`pin: String` 파라미터를 갖는다
```kotlin
                is SessionControlEvent.RequireSignIn -> onNavigate(
                    NavigationAction.NavigateToSignIn(
                        NavigationAction.NavigateToSessionControl(roomId, pin)
                    )
                )
```

- [ ] **Step 3: `PlayScreen.kt:83`이 인자 없이 그대로인지 확인한다**

Run: `grep -n 'NavigateToSignIn' composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/ui/play/PlayScreen.kt`
Expected: `NavigationAction.NavigateToSignIn()` — 인자 없음

- [ ] **Step 4: 컴파일과 전체 테스트를 확인한다**

Run: `sh gradlew :composeApp:compileDebugKotlinAndroid :composeApp:jvmTest :shared:jvmTest`
Expected: BUILD SUCCESSFUL, 80건 통과

- [ ] **Step 5: 커밋**

```bash
git add composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/ui
git commit -m "feat(nav): 가드 11곳에 pendingRoute 지정 — 탭 루트 3곳은 NavigateToTab, 인자 화면 4곳은 roomId·pin 포함. Play는 스펙 §8-1대로 제외 (스펙 §3)"
```

---

## Task 6: iOS 셸 미러

**Files:**
- Modify: `iosApp/iosApp/navigation/AppShellUiState.swift`
- Modify: `iosApp/iosApp/navigation/AppShellAction.swift`
- Modify: `iosApp/iosApp/navigation/AppShellEvent.swift`
- Modify: `iosApp/iosApp/navigation/AppShellViewModel.swift`

**Interfaces:**
- Consumes: 기존 `Route` enum(연관값 포함), `AppTab`, `IsSignedInUseCase`
- Produces:
  - `AppShellUiState.pendingRoute: Route?`
  - `AppShellAction.rememberPendingRoute(Route?)` · `.resumeAfterSignIn`
  - `AppShellEvent.resumePendingRoute(Route)` · `.navigateToHome`

신규 Swift 파일이 없으므로 **pbxproj를 건드리지 않는다.**

- [ ] **Step 1: `AppShellUiState.swift`를 교체한다**

```swift
import Foundation

// 셸 상태 — 마지막 탭 선택 시점의 로그인 여부(탭 탭마다 동기 재조회, 규칙 §8)와
// 로그인 성공 후 복귀할 목적지(규칙 §7 pendingRoute, 스펙 §2-2)
struct AppShellUiState {
    var isSignedIn: Bool = false

    var pendingRoute: Route?
}
```

- [ ] **Step 2: `AppShellAction.swift`를 교체한다**

```swift
import Foundation

enum AppShellAction {
    case selectTab(AppTab)

    // SignIn 진입 시 항상 호출한다 — 목적지가 없으면 nil로 덮어써 이전 값을 무효화한다 (스펙 §0 stale 방지)
    case rememberPendingRoute(Route?)

    // 로그인 성공 — pendingRoute 유무로 목적지를 정한다
    case resumeAfterSignIn
}
```

- [ ] **Step 3: `AppShellEvent.swift`를 교체한다**

```swift
import Foundation

enum AppShellEvent {
    case navigateToTab(AppTab)

    // 게스트가 로그인 필수 탭을 누름 — 화면을 열지 않고 SignIn으로 (결정 2)
    case requireSignIn

    // 로그인 성공 + pendingRoute 있음 — SignIn을 걷어내고 목적지로 복귀한다 (스펙 §4-0)
    case resumePendingRoute(Route)

    // 로그인 성공 + pendingRoute 없음 — 현행대로 홈으로
    case navigateToHome
}
```

- [ ] **Step 4: `AppShellViewModel.swift`를 교체한다**

```swift
import Combine
import Foundation
import Shared

// Compose AppShellViewModel.kt 미러 — 하단 탭 게스트 가드 + pendingRoute 보관 (규칙 §7·§8, 스펙 §2-2)
final class AppShellViewModel: ObservableObject {
    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = AppShellUiState()

    let event = PassthroughSubject<AppShellEvent, Never>()

    private func onSelectTab(_ tab: AppTab) {
        let isSignedIn = isSignedInUseCase.invoke()
        let isGuarded = tab.requiresSignIn && !isSignedIn

        uiState.isSignedIn = isSignedIn
        if isGuarded {
            uiState.pendingRoute = tab.route
            event.send(.requireSignIn)
        } else {
            event.send(.navigateToTab(tab))
        }
    }

    private func onRememberPendingRoute(_ pendingRoute: Route?) {
        uiState.pendingRoute = pendingRoute
    }

    private func onResumeAfterSignIn() {
        let pendingRoute = uiState.pendingRoute

        uiState.isSignedIn = isSignedInUseCase.invoke()
        uiState.pendingRoute = nil
        if let pendingRoute {
            event.send(.resumePendingRoute(pendingRoute))
        } else {
            event.send(.navigateToHome)
        }
    }

    func action(_ action: AppShellAction) {
        switch action {
        case let .selectTab(tab):
            onSelectTab(tab)
        case let .rememberPendingRoute(pendingRoute):
            onRememberPendingRoute(pendingRoute)
        case .resumeAfterSignIn:
            onResumeAfterSignIn()
        }
    }

    init(isSignedInUseCase: IsSignedInUseCase) {
        self.isSignedInUseCase = isSignedInUseCase
    }
}
```

- [ ] **Step 5: `AppTab`에 `route: Route` 프로퍼티를 추가한다**

Step 4의 `tab.route`가 `Route`를 반환해야 한다. `iosApp/iosApp/navigation/AppTab.swift`를 열어 `label`·`systemImage` 같은 기존 계산 프로퍼티 옆에 추가한다:

```swift
    // 탭 루트에 대응하는 Route — pendingRoute로 탭 복귀를 표현할 때 쓴다 (스펙 §2-3)
    var route: Route {
        switch self {
        case .home: return .home
        case .hostedRooms: return .hostedRooms
        case .joinedRooms: return .joinedRooms
        case .myInfo: return .myInfo
        }
    }
```

`iosApp`의 `AppTab`은 `label`·`systemImage`·`requiresSignIn` 세 프로퍼티만 갖고 있어 이름 충돌이 없다(Compose `AppTab`과 달리 라우트 문자열을 들고 있지 않다).

- [ ] **Step 6: 빌드한다**

Run:
```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' build 2>&1 | tail -20
git checkout -- gradlew
```
Expected: `BUILD SUCCEEDED`

- [ ] **Step 7: 커밋**

```bash
git add iosApp/iosApp/navigation
git commit -m "feat(ios): 셸 미러에 pendingRoute 추가 — rememberPendingRoute/resumeAfterSignIn 액션과 resumePendingRoute/navigateToHome 이벤트, AppTab.route로 탭 복귀 표현 (스펙 §2-2·§2-3)"
```

---

## Task 7: iOS ContentView 배선 + Join 이벤트 분리

**Files:**
- Modify: `iosApp/iosApp/ContentView.swift`
- Modify: `iosApp/iosApp/ui/join/JoinEvent.swift`
- Modify: `iosApp/iosApp/ui/join/JoinViewModel.swift:127,156`
- Modify: `iosApp/iosApp/ui/join/JoinView.swift`

**Interfaces:**
- Consumes: Task 6의 `AppShellAction.rememberPendingRoute`·`.resumeAfterSignIn`, `AppShellEvent.resumePendingRoute`·`.navigateToHome`
- Produces: 없음 (배선만)

- [ ] **Step 1: `JoinEvent.swift`에 새 케이스를 추가한다**

```swift
enum JoinEvent {
    case requestQrScan
    case joinCompleted(pin: String)
    case paymentRequired(pin: String)
    case signInRequested
    // 유료 방 게스트 차단·서버 LoginRequired — 로그인 후 결제 화면으로 복귀한다 (스펙 §3)
    case signInRequiredForPaidRoom(pin: String)
    case showNotice(message: String)
}
```

- [ ] **Step 2: `JoinViewModel.swift`의 두 발행 지점을 바꾼다**

`JoinViewModel.swift:127`(유료 방 게스트 차단) — `room.pin`을 싣는다:
```swift
            event.send(.signInRequiredForPaidRoom(pin: room.pin))
```

`JoinViewModel.swift:156`(서버 `LoginRequired`) — 현재 입력된 pin을 싣는다:
```swift
            event.send(.signInRequiredForPaidRoom(pin: uiState.pin))
```

`JoinViewModel.swift:79`(로그인 링크)는 **바꾸지 않는다**.

- [ ] **Step 3: `JoinView.swift`에 콜백과 배선을 추가한다**

`JoinView`의 프로퍼티 선언부(`var onSignInRequested: () -> Void = {}` 근처)에 추가:

```swift
    var onSignInRequiredForPaidRoom: (String) -> Void = { _ in }
```

`.onReceive` 안 `case .signInRequested:` **바로 아래**에 추가:

```swift
            case let .signInRequiredForPaidRoom(pin):
                onSignInRequiredForPaidRoom(pin)
```

- [ ] **Step 4: `ContentView`에 SignIn push 헬퍼를 추가한다**

`private func popOnce(_ path: Binding<[Route]>)` **바로 위**에 추가:

```swift
    // SignIn 진입은 항상 pendingRoute를 재정의한다 — 목적지가 없으면 nil로 덮어쓴다 (스펙 §0 stale 방지)
    private func pushSignIn(pendingRoute: Route?, path: Binding<[Route]>) {
        shellViewModel.action(.rememberPendingRoute(pendingRoute))
        path.wrappedValue.append(.signIn)
    }

    // 로그인 성공 후 복귀 — 목적지가 탭 루트면 스택을 비우고 재생성, push 라우트면 SignIn을 걷어낸 자리로 (스펙 §4-3)
    private func resume(to route: Route) {
        if let tab = AppTab.allCases.first(where: { $0.route == route }) {
            path = []
            selectedTab = tab
            sessionGeneration += 1
        } else {
            if !path.isEmpty {
                path.removeLast()
            }
            if path.last != route {
                path.append(route)
            }
        }
    }
```

`AppTab`은 이미 `enum AppTab: Hashable, CaseIterable`이라 `allCases`를 그대로 쓸 수 있다.

- [ ] **Step 5: 셸 이벤트 수집을 4분기로 바꾼다**

`ContentView.swift`의 `.onReceive(shellViewModel.event) { ... }` 블록 전체를 교체:

```swift
        .onReceive(shellViewModel.event) { event in
            switch event {
            case let .navigateToTab(tab):
                selectedTab = tab
            case .requireSignIn:
                // 탭 가드 — pendingRoute는 셸이 이미 목적 탭으로 저장했다
                path.append(.signIn)
            case let .resumePendingRoute(route):
                resume(to: route)
            case .navigateToHome:
                path = []
                selectedTab = .home
                sessionGeneration += 1
            }
        }
```

- [ ] **Step 6: SignIn 화면의 로그인 성공 배선을 셸로 넘긴다**

`destinationView`의 `case .signIn:` 블록을 교체:

```swift
        case .signIn:
            SignInView(
                onSignedIn: { shellViewModel.action(.resumeAfterSignIn) },
                onGuestEnter: { path.wrappedValue = [] }
            )
```

- [ ] **Step 7: 탭 루트 4개의 가드 콜백에 pendingRoute를 싣는다**

`TabView` 안의 네 뷰를 아래와 같이 바꾼다(변경된 콜백만 표시):

```swift
                JoinView(
                    initialPin: nil,
                    onJoined: { pin in path.append(.waiting(pin: pin)) },
                    onPaymentRequired: { pin in path.append(.payment(pin: pin)) },
                    onSignInRequested: { pushSignIn(pendingRoute: nil, path: $path) },
                    onSignInRequiredForPaidRoom: { pin in
                        pushSignIn(pendingRoute: .payment(pin: pin), path: $path)
                    }
                )
```

```swift
                HostedRoomsView(
                    onRequireSignIn: { pushSignIn(pendingRoute: .hostedRooms, path: $path) },
```

```swift
                JoinedRoomsView(
                    onRequireSignIn: { pushSignIn(pendingRoute: .joinedRooms, path: $path) },
```

```swift
                MyInfoView(
                    onRequireSignIn: { pushSignIn(pendingRoute: .myInfo, path: $path) },
```

- [ ] **Step 8: `destinationView`의 가드 콜백 11곳에 pendingRoute를 싣는다**

각 `case`의 `onRequireSignIn`/`onSignInRequested`/`onSignInRequired`/`onOpenSignup`/`onNavigateToSignup`을 아래로 바꾼다:

```swift
        case .roomList:
            // onRequireSignIn:
            { pushSignIn(pendingRoute: .roomList, path: path) }

        case let .join(pin):
            // onSignInRequested:
            { pushSignIn(pendingRoute: nil, path: path) }
            // onSignInRequiredForPaidRoom: (새로 추가)
            { pin in pushSignIn(pendingRoute: .payment(pin: pin), path: path) }

        case let .payment(pin):
            // onSignInRequired:
            { pushSignIn(pendingRoute: .payment(pin: pin), path: path) }

        case let .play(pin):
            // onOpenSignup: — 스펙 §8-1에 따라 목적지 없음(홈으로)
            { pushSignIn(pendingRoute: nil, path: path) }

        case let .result(roomId):
            // onNavigateToSignup:
            { pushSignIn(pendingRoute: .result(roomId: roomId), path: path) }

        case .reputation:
            // onRequireSignIn:
            { pushSignIn(pendingRoute: .reputation, path: path) }

        case .earnings:
            // onRequireSignIn:
            { pushSignIn(pendingRoute: .earnings, path: path) }

        case let .sessionControl(roomId, pin):
            // onRequireSignIn:
            { pushSignIn(pendingRoute: .sessionControl(roomId: roomId, pin: pin), path: path) }

        case let .roomReport(roomId):
            // onRequireSignIn:
            { pushSignIn(pendingRoute: .roomReport(roomId: roomId), path: path) }

        case .settings:
            // onRequireSignIn:
            { pushSignIn(pendingRoute: .settings, path: path) }
```

`destinationView` 안에서는 `path`가 이미 `Binding<[Route]>` 파라미터이므로 `$path`가 아니라 `path`를 그대로 넘긴다.

- [ ] **Step 9: 남은 `path.append(.signIn)`이 없는지 확인한다**

Run: `grep -n 'append(.signIn)' iosApp/iosApp/ContentView.swift`
Expected: `pushSignIn` 안의 1건과 `case .requireSignIn:`의 1건, 합계 2건만 남는다

- [ ] **Step 10: 빌드한다**

Run:
```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' build 2>&1 | tail -20
git checkout -- gradlew
```
Expected: `BUILD SUCCEEDED`

- [ ] **Step 11: 커밋**

```bash
git add iosApp/iosApp
git commit -m "feat(ios): ContentView 가드 15곳이 pendingRoute를 기록하고 로그인 후 복귀 — 탭 루트는 스택 비우고 재생성, push 라우트는 removeLast+조건부 append로 1단계 유지. Join 이벤트도 유료 방용으로 분리 (스펙 §3-1·§4-0·§4-3)"
```

---

## Task 8: 전체 검증 + 검증 체크리스트 갱신

**Files:**
- Modify: `docs/Passmate_Mac_검증_체크리스트.md` — §11 신설

**Interfaces:**
- Consumes: Task 1~7 전부
- Produces: 없음

- [ ] **Step 1: 3타깃 컴파일과 전체 테스트를 돌린다**

Run:
```bash
sh gradlew :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid
sh gradlew :composeApp:jvmTest :shared:jvmTest
```
Expected: BUILD SUCCESSFUL · 테스트 80건 통과(기존 74 + 셸 4 + Join 2)

- [ ] **Step 2: iOS를 클린 빌드한다**

Run:
```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' clean build 2>&1 | tail -20
git checkout -- gradlew
```
Expected: `BUILD SUCCEEDED`, 오류 0건

- [ ] **Step 3: Desktop에서 가드 진입을 확인한다**

Run: `sh gradlew :composeApp:run`
확인: 게스트 상태에서 "마이"·"참여한 방"·"내가 만든 방" 탭 → 각각 SignIn 화면이 열리고 탭 바가 숨는다. 뒤로 나오면 홈 탭이 유지된다.

- [ ] **Step 4: 상태 주입으로 복귀 배선을 확인한다 (Desktop)**

`AppNavHost.jvm.kt`의 `LaunchedEffect(shellViewModel)` 블록 **바로 아래**에 임시 코드를 넣는다:

```kotlin
    // [임시 검증 — 커밋하지 않는다] 5초 뒤 로그인 성공을 흉내내 복귀 배선을 확인한다
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5_000)
        shellViewModel.onAction(AppShellAction.ResumeAfterSignIn)
    }
```

Run: `sh gradlew :composeApp:run`
확인 절차: 앱이 뜨면 즉시 "마이" 탭을 누른다 → SignIn이 열린다 → 5초 뒤 **마이 탭으로 전환되고 SignIn이 사라진다**.
그다음 임시 코드를 지운다: `git checkout -- composeApp/src/jvmMain/kotlin/org/sesacteamproject/passmate/navigation/AppNavHost.jvm.kt`

- [ ] **Step 5: 상태 주입으로 복귀 배선을 확인한다 (iOS 시뮬레이터)**

`ContentView.swift`의 `.onReceive(shellViewModel.event)` **바로 아래**에 임시 코드를 넣는다:

```swift
        // [임시 검증 — 커밋하지 않는다] 앱 시작 8초 뒤 로그인 성공을 흉내낸다
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 8) {
                shellViewModel.action(.resumeAfterSignIn)
            }
        }
```

Xcode에서 시뮬레이터로 실행하고, 8초 안에 "마이" 탭을 눌러 SignIn을 띄운다.
확인: 8초 시점에 **마이 탭으로 전환되고 SignIn이 사라진다**. SwiftUI 경고 0건.
그다음 임시 코드를 지운다: `git checkout -- iosApp/iosApp/ContentView.swift` 후 `git checkout -- gradlew`

- [ ] **Step 6: 중복 엔트리가 생기지 않는지 확인한다 (Desktop)**

Step 4의 임시 코드를 다시 넣되 `ResumeAfterSignIn` 앞에 목적지를 심는다:

```kotlin
    // [임시 검증 — 커밋하지 않는다] RoomList 복귀 시 중복 엔트리가 생기지 않는지 확인 (스펙 §4-0)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5_000)
        shellViewModel.onAction(
            AppShellAction.RememberPendingRoute(NavigationAction.NavigateToRoomList)
        )
        shellViewModel.onAction(AppShellAction.ResumeAfterSignIn)
    }
```

Run: `sh gradlew :composeApp:run`
확인 절차: 홈에서 방 목록으로 들어간 뒤 5초를 기다린다 → 화면이 그대로 방 목록이다 → **뒤로 가면 곧바로 홈**이다(방 목록이 한 번 더 나오지 않는다).
그다음 임시 코드를 지운다: `git checkout -- composeApp/src/jvmMain/kotlin/org/sesacteamproject/passmate/navigation/AppNavHost.jvm.kt`

- [ ] **Step 7: 작업 트리가 깨끗한지 확인한다**

Run: `git status --porcelain`
Expected: 임시 검증 코드가 남아 있지 않다. `gradlew`가 수정된 상태로 남아 있으면 `git checkout -- gradlew`.

- [ ] **Step 8: Mac 검증 체크리스트에 §11을 추가한다**

`docs/Passmate_Mac_검증_체크리스트.md` 맨 끝에 추가:

```markdown
## 11. pendingRoute — 로그인 후 복귀 (feature/pending-route, 2026-08-31 — 파트2)

> 신규 Swift 파일 없음(pbxproj 무변경, **다음 가용 idx = 159** 유지). 스펙: `docs/superpowers/specs/2026-08-31-pending-route-design.md`
> **로그인 완료까지 가는 복귀는 백엔드 OAuth가 필요해 이 Mac에서 검증할 수 없다.** 복귀 판단은 단위 테스트 6건이 덮고, 셸 배선은 상태 주입으로 확인한다(계획서 Task 8 Step 4~6).

- [ ] 단위 테스트: `sh gradlew :composeApp:jvmTest :shared:jvmTest` 80건 통과 (셸 pendingRoute 4건 + Join 유료 방 2건 포함)
- [ ] Desktop: 게스트가 로그인 필수 탭 3개를 누르면 각각 SignIn이 열리고 탭 바가 숨는다
- [ ] Desktop 상태 주입: 마이 탭 가드 → `ResumeAfterSignIn` → **마이 탭으로 전환되고 SignIn이 사라진다**
- [ ] Desktop 상태 주입: 방 목록에서 `pendingRoute = NavigateToRoomList` 복귀 → 뒤로가기 1회에 홈으로(같은 화면이 두 번 나오지 않음, 스펙 §4-0)
- [ ] 시뮬(iOS): 마이 탭 가드 → `resumeAfterSignIn` → 마이 탭 복귀, SwiftUI 경고 0건
- [ ] `[백엔드]` 게스트가 유료 방 PIN 입장 → "유료 방은 로그인 후 입장할 수 있어요" → 로그인 → **결제 화면(Payment)으로 복귀**
- [ ] `[백엔드]` 게스트가 결과 화면에서 "가입하고 기록 저장" → 로그인 → 결과 화면으로 복귀(기록 연동 완료)
- [ ] `[백엔드]` Play 화면 가입 유도 → 로그인 → **홈으로**(스펙 §8-1대로 복귀하지 않는 것이 정상)
- [ ] `[실기기 iOS 15]` 위 복귀 경로에서 push/pop이 조용히 실패하지 않는지(빈 화면·상단 빈 띠 없음)
```

- [ ] **Step 9: 커밋**

```bash
git add docs/Passmate_Mac_검증_체크리스트.md
git commit -m "docs: Mac 검증 체크리스트 §11 신설 — pendingRoute 복귀(단위 6건·Desktop/iOS 상태 주입·백엔드 필요 3건·실기기 1건), pbxproj 무변경으로 다음 idx 159 유지"
```

- [ ] **Step 10: 사용자에게 푸시·PR 승인을 요청한다**

`git log --oneline origin/develop..HEAD`로 커밋 목록을 보여주고 승인을 받는다(로컬 `develop` ref는 오래돼 있으므로 반드시 `origin/develop`을 기준으로 쓴다). **승인 전에는 푸시하지 않는다.**
