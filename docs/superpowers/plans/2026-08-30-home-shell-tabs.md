# 홈 셸 + 하단 4탭 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 임시 홈 셸을 피그마 v6 하단 4탭(홈=입장 폼 / 내가 만든 방 / 참여한 방 / 마이)으로 교체하고, 마이 탭을 M-12 시안대로 채운다 — Android·Desktop·iOS 3플랫폼.

**Architecture:** 기존 push 라우트 그래프는 그대로 두고 탭 셸을 위에 씌운다(접근법 A). 탭 루트 4개에서만 하단 바를 보이고, 게스트의 로그인 필수 탭 진입은 셸의 `AppShellViewModel`이 `SignIn`으로 돌린다. 화면은 새로 그리지 않고 이름을 바꿔 재배치한다: `MyInfo*`→`JoinedRooms*`(참여한 방), `Settings*`→`MyInfo*`(마이 루트, 시안대로 확장), 새 축소 `Settings*`(회원 탈퇴).

**Tech Stack:** Kotlin 1.9.20 · Compose Multiplatform 1.5.12 · material3 · Jetpack Navigation 2.7.7(Android) · jetbrains lifecycle-viewmodel 2.8.4 · Koin 3.5.6 · kotlinx-coroutines-test 1.8.1 · SwiftUI(iOS 16, `TabView`+`NavigationStack`) · Xcode 26

**Spec:** `docs/superpowers/specs/2026-08-30-home-shell-tabs-design.md`

## Global Constraints

- 코드 규칙 `docs/Passmate_코드_패턴_규칙.md` 전부 적용. 특히 §7(uiState/event/onAction 3프로퍼티, `onAction` 전용 메서드는 `private`), §11-1(컨테이너/콘텐츠 뷰 분리, 시트·다이얼로그는 컨테이너 소유), §11-2(`PassmateColors` 토큰만, hex 금지), §16(프로퍼티 한 줄씩+개행, private 메서드 상단·public 하단, `if-else` 우선)
- shared 모듈은 **변경하지 않는다**(신규 UseCase·모델 없음). `KoinHelper` getter도 기존 것으로 충분
- ViewModel은 `composeApp/.../mvi/MviViewModel<S, A, E>` 상속. Koin 등록은 `factory`, 컨테이너 Screen에서만 `koinScreenViewModel()`
- 새 파일은 LF 개행. 파일명 = 타입명 1:1
- 라우트 이름·인자·가드 규칙은 3플랫폼 동일: 탭 루트 = `Home`·`HostedRooms`·`JoinedRooms`·`MyInfo`
- 게스트가 로그인 필수 탭을 누르면 화면을 열지 않고 `SignIn`으로 보낸다(결정 2)
- 홈 탭 = 기존 `JoinScreen`/`JoinView` 그대로(뒤로가기 버튼이 원래 없음 → `isTabRoot` 파라미터 불필요, 스펙 §2에서 조정)
- 방 찾기(`RoomList`) 진입점은 두지 않는다(라우트·화면 코드는 유지)
- 비밀번호 행·코인 충전 화면(M-12-4~6)·회원 탈퇴 체크박스 화면(M-12-12)·pendingRoute는 범위 밖. 코인 충전 버튼은 `ShowNotice("코인 충전은 준비 중이에요")`
- iOS 하단 탭 바는 네이티브 `TabView` 탭 바 사용(별도 Swift 컴포넌트 없음, 스펙 §1-4에서 조정). 색은 `PassmateColors.primary` tint
- pbxproj 신규 파일 idx는 **145부터**, 그룹 ID(`A10120xx`)는 새로 만들지 않는다. 편집 후 중복 ID 0개 확인
- `gradlew`에 실행 권한이 없다 → `sh gradlew …`. Xcode 빌드가 +x를 붙이면 커밋 전 `git checkout -- gradlew`
- 브랜치 `feature/home`은 현재 `fix/ios-build`(PR #16) 위에 있다. PR #16이 develop에 병합되면 `git rebase develop feature/home`으로 fix 커밋을 떨궈낸다
- 커밋 메시지 끝에 `Claude-Session: https://claude.ai/code/session_01EQVuB7AawaJr3kvsfXy3xN` 한 줄을 붙인다(빈 줄 뒤)

## 파일 구조

**composeApp (commonMain)**
- `navigation/AppTab.kt` — 탭 4개 enum(라우트·라벨·로그인 필수 여부)
- `navigation/AppShellUiState.kt` · `AppShellAction.kt` · `AppShellEvent.kt` · `AppShellViewModel.kt` — 셸 가드 VM
- `navigation/Route.kt` — `JoinedRooms` 추가
- `navigation/NavigationAction.kt` — `NavigateToTab(tab)` 추가
- `component/PassmateBottomTabBar.kt` — 하단 탭 바(Android·Desktop 공용)
- `ui/mypage/JoinedRooms{Screen,ViewModel,UiState,Action,Event}.kt` — 기존 `MyInfo*` rename + 메뉴 행 제거
- `ui/mypage/MyInfo{Screen,ViewModel,UiState,Action,Event}.kt` — 기존 `Settings*` rename + M-12 카드 확장
- `ui/mypage/Settings{Screen,ViewModel,UiState,Action,Event}.kt` — 신규 축소판(회원 탈퇴)
- `ui/hostroom/HostedRoomsScreen.kt` — 상단 "닫기" 제거
- `ui/home/HomeScreen.kt` — 삭제
- `di/ViewModelModule.kt` — factory 갱신
- `composeApp/src/androidMain/.../navigation/AppNavHost.android.kt`, `jvmMain/.../AppNavHost.jvm.kt` — 셸

**composeApp (jvmTest)**
- `testing/TestMainDispatcher.kt` · `testing/FakeAuthRepository.kt` · `testing/FakeUserRepository.kt` · `testing/FakePaymentRepository.kt` — 공용 테스트 도구
- `navigation/AppShellViewModelTest.kt` · `ui/mypage/JoinedRoomsViewModelTest.kt` · `ui/mypage/MyInfoViewModelTest.kt` · `ui/mypage/SettingsViewModelTest.kt`
- `di/KoinWiringTest.kt` — 갱신

**iosApp**
- `navigation/AppTab.swift` · `AppShell{UiState,Action,Event,ViewModel}.swift`
- `navigation/Route.swift` — `joinedRooms` + `isSessionRoute`
- `ContentView.swift` — `TabView` + 탭별 `NavigationStack`
- `ui/mypage/JoinedRooms{View,ViewModel,UiState,Action,Event}.swift` ← `MyInfo*` rename
- `ui/mypage/MyInfo{View,ViewModel,UiState,Action,Event}.swift` ← `Settings*` rename + 확장
- `ui/mypage/Settings{View,ViewModel,UiState,Action,Event}.swift` — 신규 축소판
- `ui/hostroom/HostedRoomsView.swift` — "닫기" 제거
- `ui/home/Home{View,ViewModel,UiState,Action}.swift` — 삭제
- `iosApp.xcodeproj/project.pbxproj` — 참조 갱신(idx 145~154)

**docs**
- `docs/Passmate_코드_패턴_규칙.md` §2-1-1·§2-1-2, `docs/Passmate_Mac_검증_체크리스트.md` §9, 스펙 조정 3건

---

### Task 1: 테스트 인프라 + `AppTab` + `AppShellViewModel`

**Files:**
- Modify: `composeApp/build.gradle.kts:43-45`
- Modify: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/Route.kt`
- Create: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/AppTab.kt`
- Create: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/AppShellUiState.kt`
- Create: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/AppShellAction.kt`
- Create: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/AppShellEvent.kt`
- Create: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/AppShellViewModel.kt`
- Create: `composeApp/src/jvmTest/kotlin/org/sesacteamproject/passmate/testing/TestMainDispatcher.kt`
- Create: `composeApp/src/jvmTest/kotlin/org/sesacteamproject/passmate/testing/FakeAuthRepository.kt`
- Test: `composeApp/src/jvmTest/kotlin/org/sesacteamproject/passmate/navigation/AppShellViewModelTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/di/ViewModelModule.kt`

**Interfaces:**
- Produces: `enum class AppTab(val route: String, val label: String, val requiresSignIn: Boolean)` with `HOME`·`HOSTED_ROOMS`·`JOINED_ROOMS`·`MY_INFO`, `AppTab.fromRoute(route: String?): AppTab?`
- Produces: `Route.JoinedRooms : Route("joinedRooms")`
- Produces: `AppShellViewModel(isSignedInUseCase: IsSignedInUseCase)` — `onAction(AppShellAction.SelectTab(tab))` → `AppShellEvent.NavigateToTab(tab)` 또는 `AppShellEvent.RequireSignIn`
- Produces: 테스트 도구 `TestMainDispatcher.install()/reset()`, `FakeAuthRepository(isSignedIn: Boolean)`

- [ ] **Step 1: composeApp 테스트 의존성 추가**

`composeApp/build.gradle.kts`의 `commonTest.dependencies` 블록을 아래로 바꾼다:

```kotlin
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
```

- [ ] **Step 2: `Route.JoinedRooms` 추가**

`Route.kt`의 `data object MyInfo : Route("myInfo")` 바로 아래에 추가:

```kotlin
    // 참여한 방 탭 루트 (M-08) — 하단 4탭 셸
    data object JoinedRooms : Route("joinedRooms")
```

- [ ] **Step 3: `AppTab` 작성**

`navigation/AppTab.kt`:

```kotlin
package org.sesacteamproject.passmate.navigation

// 하단 4탭 (피그마 v6) — 라우트·라벨·로그인 필수 여부는 3플랫폼 동일 (규칙 §2-1-1)
enum class AppTab(
    val route: String,
    val label: String,
    val requiresSignIn: Boolean
) {
    HOME(Route.Home.route, "홈", false),
    HOSTED_ROOMS(Route.HostedRooms.route, "내가 만든 방", true),
    JOINED_ROOMS(Route.JoinedRooms.route, "참여한 방", true),
    MY_INFO(Route.MyInfo.route, "마이", true);

    companion object {
        fun fromRoute(route: String?): AppTab? {
            return entries.firstOrNull { it.route == route }
        }
    }
}
```

- [ ] **Step 4: 셸 MVI 타입 3개 작성**

`navigation/AppShellUiState.kt`:

```kotlin
package org.sesacteamproject.passmate.navigation

// 셸 상태 — 마지막 탭 선택 시점의 로그인 여부 (탭 탭마다 동기 재조회, 규칙 §8)
data class AppShellUiState(
    val isSignedIn: Boolean = false
)
```

`navigation/AppShellAction.kt`:

```kotlin
package org.sesacteamproject.passmate.navigation

sealed interface AppShellAction {
    data class SelectTab(val tab: AppTab) : AppShellAction
}
```

`navigation/AppShellEvent.kt`:

```kotlin
package org.sesacteamproject.passmate.navigation

sealed interface AppShellEvent {
    data class NavigateToTab(val tab: AppTab) : AppShellEvent

    // 게스트가 로그인 필수 탭을 누름 — 화면을 열지 않고 SignIn으로 (결정 2)
    data object RequireSignIn : AppShellEvent
}
```

- [ ] **Step 5: 테스트 도구 2개 작성**

`jvmTest/.../testing/TestMainDispatcher.kt`:

```kotlin
package org.sesacteamproject.passmate.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

// viewModelScope가 쓰는 Dispatchers.Main을 테스트 디스패처로 교체한다
@OptIn(ExperimentalCoroutinesApi::class)
object TestMainDispatcher {

    fun install() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    fun reset() {
        Dispatchers.resetMain()
    }
}
```

`jvmTest/.../testing/FakeAuthRepository.kt`:

```kotlin
package org.sesacteamproject.passmate.testing

import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository
import org.sesacteamproject.passmate.core.model.AppResult

class FakeAuthRepository(
    var isSignedIn: Boolean
) : AuthRepository {

    var signOutCount: Int = 0

    override fun googleSignInUrl(): String {
        return "https://example.test/oauth"
    }

    override suspend fun completeSignIn(accessToken: String, refreshToken: String): AppResult<Unit> {
        isSignedIn = true
        return AppResult.Success(Unit)
    }

    override fun isSignedIn(): Boolean {
        return isSignedIn
    }

    override suspend fun signOut(): AppResult<Unit> {
        signOutCount += 1
        isSignedIn = false
        return AppResult.Success(Unit)
    }

    override fun clearSession() {
        isSignedIn = false
    }
}
```

- [ ] **Step 6: 실패하는 테스트 작성**

`jvmTest/.../navigation/AppShellViewModelTest.kt`:

```kotlin
package org.sesacteamproject.passmate.navigation

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.testing.FakeAuthRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class AppShellViewModelTest {

    private fun viewModel(isSignedIn: Boolean): AppShellViewModel {
        return AppShellViewModel(IsSignedInUseCase(FakeAuthRepository(isSignedIn)))
    }

    @BeforeTest
    fun setUp() {
        TestMainDispatcher.install()
    }

    @AfterTest
    fun tearDown() {
        TestMainDispatcher.reset()
    }

    @Test
    fun memberSelectingAnyTabNavigates() = runTest {
        val viewModel = viewModel(isSignedIn = true)
        val events = mutableListOf<AppShellEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(AppShellAction.SelectTab(AppTab.MY_INFO))

        assertEquals(listOf<AppShellEvent>(AppShellEvent.NavigateToTab(AppTab.MY_INFO)), events)
        assertEquals(true, viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun guestSelectingSignInRequiredTabRequiresSignIn() = runTest {
        val viewModel = viewModel(isSignedIn = false)
        val events = mutableListOf<AppShellEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(AppShellAction.SelectTab(AppTab.JOINED_ROOMS))

        assertEquals(listOf<AppShellEvent>(AppShellEvent.RequireSignIn), events)
        assertEquals(false, viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun guestSelectingHomeNavigates() = runTest {
        val viewModel = viewModel(isSignedIn = false)
        val events = mutableListOf<AppShellEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(AppShellAction.SelectTab(AppTab.HOME))

        assertEquals(listOf<AppShellEvent>(AppShellEvent.NavigateToTab(AppTab.HOME)), events)
    }

    @Test
    fun signInStateIsReadOnEverySelection() = runTest {
        val authRepository = FakeAuthRepository(isSignedIn = false)
        val viewModel = AppShellViewModel(IsSignedInUseCase(authRepository))
        val events = mutableListOf<AppShellEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(AppShellAction.SelectTab(AppTab.MY_INFO))
        authRepository.isSignedIn = true
        viewModel.onAction(AppShellAction.SelectTab(AppTab.MY_INFO))

        assertEquals(
            listOf(AppShellEvent.RequireSignIn, AppShellEvent.NavigateToTab(AppTab.MY_INFO)),
            events
        )
    }

    @Test
    fun tabRouteLookup() {
        assertEquals(AppTab.JOINED_ROOMS, AppTab.fromRoute("joinedRooms"))
        assertEquals(null, AppTab.fromRoute("waiting/{pin}"))
        assertEquals(null, AppTab.fromRoute(null))
    }
}
```

- [ ] **Step 7: 테스트 실패 확인**

Run: `sh gradlew :composeApp:jvmTest --tests "org.sesacteamproject.passmate.navigation.AppShellViewModelTest" --console=plain`
Expected: 컴파일 실패 — `Unresolved reference: AppShellViewModel`

- [ ] **Step 8: `AppShellViewModel` 작성**

`navigation/AppShellViewModel.kt`:

```kotlin
package org.sesacteamproject.passmate.navigation

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.mvi.MviViewModel

// 하단 탭 게스트 가드 — 로그인 필수 탭은 화면을 열지 않고 SignIn으로 돌린다 (규칙 §8, 결정 2).
// 탭을 누를 때마다 동기 조회하므로 로그인/로그아웃 후 별도 갱신이 필요 없다.
class AppShellViewModel(
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<AppShellUiState, AppShellAction, AppShellEvent>(AppShellUiState()) {

    private fun onSelectTab(tab: AppTab) {
        val isSignedIn = isSignedInUseCase.invoke()

        _uiState.update { it.copy(isSignedIn = isSignedIn) }
        viewModelScope.launch {
            if (tab.requiresSignIn && !isSignedIn) {
                _event.emit(AppShellEvent.RequireSignIn)
            } else {
                _event.emit(AppShellEvent.NavigateToTab(tab))
            }
        }
    }

    override fun onAction(action: AppShellAction) {
        when (action) {
            is AppShellAction.SelectTab -> onSelectTab(action.tab)
        }
    }
}
```

- [ ] **Step 9: Koin 등록**

`di/ViewModelModule.kt`에 import `org.sesacteamproject.passmate.navigation.AppShellViewModel` 추가하고 `module { … }` 첫 줄에:

```kotlin
    factory { AppShellViewModel(get()) }
```

- [ ] **Step 10: 테스트 통과 확인**

Run: `sh gradlew :composeApp:jvmTest --tests "org.sesacteamproject.passmate.navigation.AppShellViewModelTest" --console=plain`
Expected: `BUILD SUCCESSFUL`, 5 tests passed

- [ ] **Step 11: 커밋**

```bash
git add composeApp/build.gradle.kts composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/di/ViewModelModule.kt composeApp/src/jvmTest
git commit -m "feat(shell): AppTab·AppShellViewModel — 하단 4탭 게스트 가드(로그인 필수 탭→SignIn) + composeApp VM 테스트 인프라(coroutines-test·TestMainDispatcher·FakeAuthRepository)

Claude-Session: https://claude.ai/code/session_01EQVuB7AawaJr3kvsfXy3xN"
```

---

### Task 2: 화면 rename ×2 + 축소 `Settings*` 신설 (Compose)

기계적 이름 변경. `MyInfo*`→`JoinedRooms*`, `Settings*`→`MyInfo*`, 새 `Settings*`(회원 탈퇴만). 이 태스크가 끝나면 컴파일이 되고 기존 동작(내용)은 아직 그대로다.

**Files:**
- Rename: `ui/mypage/MyInfoScreen.kt`→`JoinedRoomsScreen.kt`, `MyInfoViewModel.kt`→`JoinedRoomsViewModel.kt`, `MyInfoUiState.kt`→`JoinedRoomsUiState.kt`, `MyInfoAction.kt`→`JoinedRoomsAction.kt`, `MyInfoEvent.kt`→`JoinedRoomsEvent.kt`
- Rename: `ui/mypage/SettingsScreen.kt`→`MyInfoScreen.kt`, `SettingsViewModel.kt`→`MyInfoViewModel.kt`, `SettingsUiState.kt`→`MyInfoUiState.kt`, `SettingsAction.kt`→`MyInfoAction.kt`, `SettingsEvent.kt`→`MyInfoEvent.kt`
- Create: `ui/mypage/SettingsScreen.kt` · `SettingsViewModel.kt` · `SettingsUiState.kt` · `SettingsAction.kt` · `SettingsEvent.kt` (축소판)
- Create: `jvmTest/.../testing/FakeUserRepository.kt`
- Test: `jvmTest/.../ui/mypage/SettingsViewModelTest.kt`
- Modify: `di/ViewModelModule.kt`, `jvmTest/.../di/KoinWiringTest.kt`, `androidMain/.../AppNavHost.android.kt`, `jvmMain/.../AppNavHost.jvm.kt`

**Interfaces:**
- Produces: `JoinedRoomsScreen(viewModel, onNavigate)`, `JoinedRoomsViewModel(getMyPageUseCase, isSignedInUseCase)`, `JoinedRoomsUiState/Action/Event` (내용은 기존 MyInfo와 동일 — Task 3에서 정리)
- Produces: `MyInfoScreen(onNavigate)`, `MyInfoViewModel(getMyProfileUseCase, signOutUseCase, deleteAccountUseCase, isSignedInUseCase)` (내용은 기존 Settings와 동일 — Task 4·5에서 확장)
- Produces: `SettingsScreen(onNavigate)`, `SettingsViewModel(deleteAccountUseCase, isSignedInUseCase)`, `SettingsUiState(isProcessing)`, `SettingsAction.{Enter, ConfirmDeleteAccount}`, `SettingsEvent.{RequireSignIn, AccountDeleted, ShowNotice}`
- Produces: `FakeUserRepository(profile, myPage, deleteResult)`

- [ ] **Step 1: 1차 rename — `MyInfo*` → `JoinedRooms*`**

```bash
cd composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/ui/mypage
for n in Screen ViewModel UiState Action Event; do git mv MyInfo$n.kt JoinedRooms$n.kt; done
sed -i '' 's/MyInfo/JoinedRooms/g' JoinedRoomsScreen.kt JoinedRoomsViewModel.kt JoinedRoomsUiState.kt JoinedRoomsAction.kt JoinedRoomsEvent.kt
cd -
```

`JoinedRoomsScreen.kt` 상단 주석을 `// Figma "UI 디자인 v6" M-08(349:9544) — 참여한 방 탭 루트: 진행 중 방·누적 요약·보완 주제·참여 목록(→리포트)`로 바꾼다. 함수/타입 이름이 `JoinedRoomsScreen`·`JoinedRoomsContentScreen`·`LoadedJoinedRooms`·`JoinedRoomsViewModel`·`JoinedRoomsUiState`·`JoinedRoomsAction`·`JoinedRoomsEvent`가 되었는지 `grep -n "MyInfo" JoinedRooms*.kt`로 0건 확인.

- [ ] **Step 2: 2차 rename — `Settings*` → `MyInfo*`**

```bash
cd composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/ui/mypage
for n in Screen ViewModel UiState Action Event; do git mv Settings$n.kt MyInfo$n.kt; done
sed -i '' 's/SettingsSheet/MyInfoSheet/g; s/SettingsScreen/MyInfoScreen/g; s/SettingsContentScreen/MyInfoContentScreen/g; s/LoadedSettings/LoadedMyInfo/g; s/SettingsViewModel/MyInfoViewModel/g; s/SettingsUiState/MyInfoUiState/g; s/SettingsAction/MyInfoAction/g; s/SettingsEvent/MyInfoEvent/g' MyInfoScreen.kt MyInfoViewModel.kt MyInfoUiState.kt MyInfoAction.kt MyInfoEvent.kt
sed -i '' 's/NotificationMyInfoSheet/NotificationSettingsSheet/g' MyInfoScreen.kt
cd -
```

(`SettingsSheet`→`MyInfoSheet` 치환이 `NotificationSettingsSheet`까지 바꾸므로 마지막 줄로 되돌린다.)

`MyInfoScreen.kt`의 헤더 주석 2줄(`// Figma … M-12(349:9683) …`, `// v6 하단 4탭 셸 전환은 보류 …`)을 아래 1줄로 교체:

```kotlin
// Figma "UI 디자인 v6" M-12(349:9683) — 마이 탭 루트: 프로필·계정·코인·정산·알림·로그아웃
```

`grep -n "Settings" MyInfo*.kt` 결과가 `NotificationSettingsSheet`·`NotificationSettings` 관련 줄만 남는지 확인.

- [ ] **Step 3: `FakeUserRepository` 작성**

`jvmTest/.../testing/FakeUserRepository.kt`:

```kotlin
package org.sesacteamproject.passmate.testing

import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.user.domain.model.Badge
import org.sesacteamproject.passmate.user.domain.model.HostProfile
import org.sesacteamproject.passmate.user.domain.model.MyGrade
import org.sesacteamproject.passmate.user.domain.model.MyPage
import org.sesacteamproject.passmate.user.domain.model.NotificationSettings
import org.sesacteamproject.passmate.user.domain.model.ReportReason
import org.sesacteamproject.passmate.user.domain.model.UserProfile
import org.sesacteamproject.passmate.user.domain.repository.UserRepository

class FakeUserRepository(
    var profileResult: AppResult<UserProfile> = AppResult.Failure(AppError.Unknown()),
    var myPageResults: List<AppResult<MyPage>> = emptyList(),
    var deleteResult: AppResult<Unit> = AppResult.Success(Unit)
) : UserRepository {

    var myPageCalls: MutableList<String?> = mutableListOf()

    override suspend fun getMyPage(cursor: String?): AppResult<MyPage> {
        val index = myPageCalls.size

        myPageCalls.add(cursor)
        return myPageResults.getOrElse(index) { AppResult.Failure(AppError.Unknown()) }
    }

    override suspend fun claimGuestRecord(participantId: Long): AppResult<Unit> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun getMyGrade(): AppResult<MyGrade> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun getMyBadges(): AppResult<List<Badge>> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun getHostProfile(userId: Long): AppResult<HostProfile> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun blockUser(userId: Long): AppResult<Unit> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun reportUser(userId: Long, reason: ReportReason, detail: String?): AppResult<Unit> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun getMyProfile(): AppResult<UserProfile> {
        return profileResult
    }

    override suspend fun updateMyProfile(nickname: String?, avatarId: Int?): AppResult<Unit> {
        return AppResult.Success(Unit)
    }

    override suspend fun deleteAccount(): AppResult<Unit> {
        return deleteResult
    }

    override suspend fun getNotificationSettings(): AppResult<NotificationSettings> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun updateNotificationSettings(settings: NotificationSettings): AppResult<Unit> {
        return AppResult.Success(Unit)
    }
}
```

- [ ] **Step 4: 축소 `Settings*` 실패 테스트 작성**

`jvmTest/.../ui/mypage/SettingsViewModelTest.kt`:

```kotlin
package org.sesacteamproject.passmate.ui.mypage

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.testing.FakeAuthRepository
import org.sesacteamproject.passmate.testing.FakeUserRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher
import org.sesacteamproject.passmate.user.domain.usecase.DeleteAccountUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private fun viewModel(
        isSignedIn: Boolean,
        deleteResult: AppResult<Unit> = AppResult.Success(Unit)
    ): SettingsViewModel {
        val authRepository = FakeAuthRepository(isSignedIn)
        val userRepository = FakeUserRepository(deleteResult = deleteResult)

        return SettingsViewModel(
            deleteAccountUseCase = DeleteAccountUseCase(userRepository, authRepository),
            isSignedInUseCase = IsSignedInUseCase(authRepository)
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

    @Test
    fun guestEnterRequiresSignIn() = runTest {
        val viewModel = viewModel(isSignedIn = false)
        val events = mutableListOf<SettingsEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(SettingsAction.Enter)

        assertEquals(listOf<SettingsEvent>(SettingsEvent.RequireSignIn), events)
    }

    @Test
    fun deleteAccountSuccessEmitsAccountDeleted() = runTest {
        val viewModel = viewModel(isSignedIn = true)
        val events = mutableListOf<SettingsEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(SettingsAction.Enter)
        viewModel.onAction(SettingsAction.ConfirmDeleteAccount)

        assertEquals(listOf<SettingsEvent>(SettingsEvent.AccountDeleted), events)
        assertEquals(false, viewModel.uiState.value.isProcessing)
    }

    @Test
    fun deleteAccountConflictShowsServerMessage() = runTest {
        val viewModel = viewModel(
            isSignedIn = true,
            deleteResult = AppResult.Failure(AppError.Conflict(serverMessage = "정산 대기 금액이 있어요"))
        )
        val events = mutableListOf<SettingsEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(SettingsAction.Enter)
        viewModel.onAction(SettingsAction.ConfirmDeleteAccount)

        assertEquals(listOf<SettingsEvent>(SettingsEvent.ShowNotice("정산 대기 금액이 있어요")), events)
    }
}
```

- [ ] **Step 5: 테스트 실패 확인**

Run: `sh gradlew :composeApp:jvmTest --tests "org.sesacteamproject.passmate.ui.mypage.SettingsViewModelTest" --console=plain`
Expected: 컴파일 실패 — `SettingsViewModel` 생성자 인자 불일치(현재 이름은 MyInfoViewModel로 바뀌어 없음)

- [ ] **Step 6: 축소 `Settings*` 5파일 작성**

`ui/mypage/SettingsUiState.kt`:

```kotlin
package org.sesacteamproject.passmate.ui.mypage

data class SettingsUiState(
    // 탈퇴 요청 in-flight — 중복 호출 방지 (규칙 §9)
    val isProcessing: Boolean = false
)
```

`ui/mypage/SettingsAction.kt`:

```kotlin
package org.sesacteamproject.passmate.ui.mypage

sealed interface SettingsAction {
    data object Enter : SettingsAction

    // 확인 다이얼로그를 거친 뒤 호출된다 — 다이얼로그 소유는 화면 (규칙 §11-1)
    data object ConfirmDeleteAccount : SettingsAction
}
```

`ui/mypage/SettingsEvent.kt`:

```kotlin
package org.sesacteamproject.passmate.ui.mypage

sealed interface SettingsEvent {
    // 설정은 회원 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    data object RequireSignIn : SettingsEvent

    // 탈퇴 완료 → 홈으로 (세션 정리는 shared가 수행)
    data object AccountDeleted : SettingsEvent

    data class ShowNotice(val message: String) : SettingsEvent
}
```

`ui/mypage/SettingsViewModel.kt`:

```kotlin
package org.sesacteamproject.passmate.ui.mypage

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.user.domain.usecase.DeleteAccountUseCase

// 설정 (마이 탭 우상단 "설정") — 마이 루트에서 닿지 않는 회원 탈퇴(M-12-12)만 둔다
class SettingsViewModel(
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<SettingsUiState, SettingsAction, SettingsEvent>(SettingsUiState()) {

    private fun onEnter() {
        // 회원 전용 가드 — 서버 검증이 최종 권위 (규칙 §8)
        if (!isSignedInUseCase.invoke()) {
            viewModelScope.launch {
                _event.emit(SettingsEvent.RequireSignIn)
            }
        }
    }

    private fun onConfirmDeleteAccount() {
        if (_uiState.value.isProcessing) {
            return
        }
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            deleteAccountUseCase.invoke()
                .onSuccess {
                    _uiState.update { it.copy(isProcessing = false) }
                    _event.emit(SettingsEvent.AccountDeleted)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isProcessing = false) }
                    _event.emit(SettingsEvent.ShowNotice(deleteFailMessage(error)))
                }
        }
    }

    // 서버 code 기반 문구 분기 (규칙 §10) — 409=정산 미지급분·진행 중 방 거부
    private fun deleteFailMessage(error: AppError): String {
        return if (error is AppError.Conflict) {
            error.serverMessage ?: "정산 대기 금액이나 진행 중인 방이 있어 탈퇴할 수 없어요"
        } else if (error is AppError.NetworkError) {
            "네트워크 연결을 확인해 주세요"
        } else {
            "탈퇴를 처리하지 못했어요. 다시 시도해 주세요"
        }
    }

    override fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.Enter -> onEnter()
            is SettingsAction.ConfirmDeleteAccount -> onConfirmDeleteAccount()
        }
    }
}
```

`ui/mypage/SettingsScreen.kt`:

```kotlin
package org.sesacteamproject.passmate.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.theme.PassmateColors

// 설정 — 마이 탭 우상단 "설정"에서 push. 회원 탈퇴(M-12-12, 확인 다이얼로그)만 둔다
@Composable
fun SettingsScreen(onNavigate: (NavigationAction) -> Unit) {
    val viewModel: SettingsViewModel = koinScreenViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onAction(SettingsAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is SettingsEvent.RequireSignIn -> onNavigate(NavigationAction.NavigateToSignIn)
                is SettingsEvent.AccountDeleted -> onNavigate(NavigationAction.NavigateToHome)
                is SettingsEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        SettingsContentScreen(
            uiState = uiState,
            onClickBack = { onNavigate(NavigationAction.NavigateBack) },
            onClickDelete = { showDeleteConfirm = true }
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "회원 탈퇴",
                    color = PassmateColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "탈퇴하면 참여·개설 기록과 보유 코인이 모두 삭제되고 되돌릴 수 없어요. 정산 대기 금액이나 진행 중인 방이 있으면 탈퇴할 수 없어요.",
                    color = PassmateColors.TextSecondary,
                    fontSize = 14.sp,
                    letterSpacing = (-0.28).sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.onAction(SettingsAction.ConfirmDeleteAccount)
                    }
                ) {
                    Text(text = "탈퇴", color = PassmateColors.WeakTopicText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(text = "취소", color = PassmateColors.TextSecondary)
                }
            },
            containerColor = PassmateColors.Surface
        )
    }
}

@Composable
private fun SettingsContentScreen(
    uiState: SettingsUiState,
    onClickBack: () -> Unit,
    onClickDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
            .padding(start = 20.dp, top = 60.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "설정",
                color = PassmateColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.48).sp
            )
            Text(
                text = "닫기",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp,
                modifier = Modifier
                    .clickable(onClick = onClickBack)
                    .padding(4.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
                .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
                .clickable(onClick = onClickDelete)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "회원 탈퇴",
                color = PassmateColors.WeakTopicText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(text = "›", color = PassmateColors.TextTertiary, fontSize = 18.sp)
        }
        if (uiState.isProcessing) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = PassmateColors.Primary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
```

- [ ] **Step 7: 배선 갱신 (Koin·NavHost·KoinWiringTest)**

`di/ViewModelModule.kt`: import 목록에 `org.sesacteamproject.passmate.ui.mypage.JoinedRoomsViewModel` 추가(기존 `MyInfoViewModel`·`SettingsViewModel` import는 유지). factory 3줄을 아래로 교체:

```kotlin
    factory { JoinedRoomsViewModel(get(), get()) }
    factory { MyInfoViewModel(get(), get(), get(), get()) }
    factory { SettingsViewModel(get(), get()) }
```

(기존 `factory { MyInfoViewModel(get(), get()) }`와 `factory { SettingsViewModel(get(), get(), get(), get()) }`는 삭제.)

`AppNavHost.android.kt`: import `org.sesacteamproject.passmate.ui.mypage.JoinedRoomsScreen` 추가. `composable(Route.MyInfo.route) { MyInfoScreen(onNavigate = …) }` 바로 아래에 추가:

```kotlin
        composable(Route.JoinedRooms.route) {
            JoinedRoomsScreen(onNavigate = navController::handleNavigationAction)
        }
```

`AppNavHost.jvm.kt`: `JvmDestination`에 `data object JoinedRooms : JvmDestination` 추가, `when (currentDestination)`에 `is JvmDestination.JoinedRooms -> JoinedRoomsScreen(onNavigate = onNavigate)` 추가, import 추가. (탭 전환 액션은 Task 8에서.)

`KoinWiringTest.kt`: import에 `org.sesacteamproject.passmate.ui.mypage.JoinedRoomsViewModel`·`SettingsViewModel`·`org.sesacteamproject.passmate.navigation.AppShellViewModel` 추가, 본문에 아래 3줄과 대응 `assertNotNull` 3줄 추가:

```kotlin
        val joinedRoomsViewModel = KoinPlatform.getKoin().get<JoinedRoomsViewModel>()
        val settingsViewModel = KoinPlatform.getKoin().get<SettingsViewModel>()
        val appShellViewModel = KoinPlatform.getKoin().get<AppShellViewModel>()
```

- [ ] **Step 8: 테스트·컴파일 확인**

Run: `sh gradlew :composeApp:jvmTest --console=plain`
Expected: `BUILD SUCCESSFUL` — SettingsViewModelTest 3건, KoinWiringTest 통과

Run: `sh gradlew :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid --console=plain`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: 커밋**

```bash
git add -A composeApp/src
git commit -m "refactor(mypage): 화면 재배치 — MyInfo*→JoinedRooms*(참여한 방 탭), Settings*→MyInfo*(마이 탭 루트), 축소 Settings*(회원 탈퇴) 신설 + Route.JoinedRooms 배선

Claude-Session: https://claude.ai/code/session_01EQVuB7AawaJr3kvsfXy3xN"
```

---

### Task 3: 참여한 방 탭 정리 (`JoinedRooms*`)

메뉴 행 4개·상단 "설정"·"닫기"를 걷어내고 M-08 탭 루트로 만든다.

**Files:**
- Modify: `ui/mypage/JoinedRoomsScreen.kt`, `JoinedRoomsViewModel.kt`, `JoinedRoomsAction.kt`, `JoinedRoomsEvent.kt`
- Test: `jvmTest/.../ui/mypage/JoinedRoomsViewModelTest.kt`

**Interfaces:**
- Produces: `JoinedRoomsAction.{Enter, Retry, LoadMore, ClickRoomReport(roomId), ClickRejoin(pin)}`, `JoinedRoomsEvent.{RequireSignIn, OpenReport(roomId), Rejoin(pin), ShowNotice(message)}`, `JoinedRoomsScreen(viewModel = koinScreenViewModel(), onNavigate)`

- [ ] **Step 1: 실패 테스트 작성**

`jvmTest/.../ui/mypage/JoinedRoomsViewModelTest.kt`:

```kotlin
package org.sesacteamproject.passmate.ui.mypage

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.testing.FakeAuthRepository
import org.sesacteamproject.passmate.testing.FakeUserRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher
import org.sesacteamproject.passmate.user.domain.model.JoinedRoom
import org.sesacteamproject.passmate.user.domain.model.MyPage
import org.sesacteamproject.passmate.user.domain.model.MyPageSummary
import org.sesacteamproject.passmate.user.domain.usecase.GetMyPageUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class JoinedRoomsViewModelTest {

    private val summary = MyPageSummary(
        participationCount = 3,
        accuracyPercent = 71,
        avgRank = 3.3,
        trendText = "지난주보다 정답률이 8%p 올랐어요",
        weakTopics = listOf("JPA 영속성", "트랜잭션")
    )

    private fun room(id: Long, title: String): JoinedRoom {
        return JoinedRoom(
            roomId = id,
            title = title,
            dateLabel = "8/22 (금)",
            questionCount = 8,
            myScore = 990.0,
            myRank = 3,
            hasReport = true
        )
    }

    private fun viewModel(isSignedIn: Boolean, pages: List<AppResult<MyPage>>): JoinedRoomsViewModel {
        val authRepository = FakeAuthRepository(isSignedIn)
        val userRepository = FakeUserRepository(myPageResults = pages)

        return JoinedRoomsViewModel(
            getMyPageUseCase = GetMyPageUseCase(userRepository),
            isSignedInUseCase = IsSignedInUseCase(authRepository)
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

    @Test
    fun guestEnterRequiresSignIn() = runTest {
        val viewModel = viewModel(isSignedIn = false, pages = emptyList())
        val events = mutableListOf<JoinedRoomsEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinedRoomsAction.Enter)

        assertEquals(listOf<JoinedRoomsEvent>(JoinedRoomsEvent.RequireSignIn), events)
        assertEquals(true, viewModel.uiState.value.isLoading)
    }

    @Test
    fun memberEnterLoadsFirstPageAndLoadMoreAppends() = runTest {
        val firstPage = MyPage(summary = summary, ongoing = null, rooms = listOf(room(1, "Spring 스터디")), nextCursor = "c1")
        val secondPage = MyPage(summary = summary, ongoing = null, rooms = listOf(room(2, "CS 모의면접")), nextCursor = null)
        val viewModel = viewModel(
            isSignedIn = true,
            pages = listOf(AppResult.Success(firstPage), AppResult.Success(secondPage))
        )

        viewModel.onAction(JoinedRoomsAction.Enter)

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(listOf(1L), viewModel.uiState.value.rooms.map { it.roomId })
        assertEquals("c1", viewModel.uiState.value.nextCursor)

        viewModel.onAction(JoinedRoomsAction.LoadMore)

        assertEquals(listOf(1L, 2L), viewModel.uiState.value.rooms.map { it.roomId })
        assertEquals(null, viewModel.uiState.value.nextCursor)
    }

    @Test
    fun reportAndRejoinEmitNavigationEvents() = runTest {
        val viewModel = viewModel(isSignedIn = true, pages = emptyList())
        val events = mutableListOf<JoinedRoomsEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(JoinedRoomsAction.ClickRoomReport(7L))
        viewModel.onAction(JoinedRoomsAction.ClickRejoin("482913"))

        assertEquals(
            listOf(JoinedRoomsEvent.OpenReport(7L), JoinedRoomsEvent.Rejoin("482913")),
            events
        )
    }
}
```

- [ ] **Step 2: 테스트 실행 — 통과하는지 확인 (rename 직후라 통과해야 정상)**

Run: `sh gradlew :composeApp:jvmTest --tests "org.sesacteamproject.passmate.ui.mypage.JoinedRoomsViewModelTest" --console=plain`
Expected: `BUILD SUCCESSFUL`, 3 tests passed. (이 테스트는 정리 후에도 동작이 유지되는지 지키는 회귀 테스트다.)

- [ ] **Step 3: Action/Event에서 메뉴 이벤트 제거**

`JoinedRoomsAction.kt`를 아래 내용으로 교체:

```kotlin
package org.sesacteamproject.passmate.ui.mypage

sealed interface JoinedRoomsAction {
    data object Enter : JoinedRoomsAction

    data object Retry : JoinedRoomsAction

    data object LoadMore : JoinedRoomsAction

    data class ClickRoomReport(val roomId: Long) : JoinedRoomsAction

    data class ClickRejoin(val pin: String) : JoinedRoomsAction
}
```

`JoinedRoomsEvent.kt`를 아래 내용으로 교체:

```kotlin
package org.sesacteamproject.passmate.ui.mypage

sealed interface JoinedRoomsEvent {
    // 참여한 방은 회원 전용 — 딥링크 직접 진입 대비 보험 (탭 가드는 AppShellViewModel, 규칙 §8)
    data object RequireSignIn : JoinedRoomsEvent

    data class OpenReport(val roomId: Long) : JoinedRoomsEvent

    data class Rejoin(val pin: String) : JoinedRoomsEvent

    data class ShowNotice(val message: String) : JoinedRoomsEvent
}
```

- [ ] **Step 4: ViewModel에서 메뉴 핸들러 제거**

`JoinedRoomsViewModel.kt`에서 `onClickCoinHistory`·`onClickReputation`·`onClickHostedRooms`·`onClickEarnings`·`onClickSettings` 메서드 5개를 삭제하고, `onAction`의 `when`을 아래로 교체:

```kotlin
    override fun onAction(action: JoinedRoomsAction) {
        when (action) {
            is JoinedRoomsAction.Enter -> onEnter()
            is JoinedRoomsAction.Retry -> loadFirstPage()
            is JoinedRoomsAction.LoadMore -> onLoadMore()
            is JoinedRoomsAction.ClickRoomReport -> onClickRoomReport(action.roomId)
            is JoinedRoomsAction.ClickRejoin -> onClickRejoin(action.pin)
        }
    }
```

- [ ] **Step 5: Screen에서 메뉴 행·설정·닫기 제거**

`JoinedRoomsScreen.kt`:
1. 컨테이너 `LaunchedEffect(viewModel)`의 `when`에서 `OpenCoinHistory`·`OpenReputation`·`OpenHostedRooms`·`OpenEarnings`·`OpenSettings` 5개 분기를 삭제한다.
2. `JoinedRoomsContentScreen(uiState = uiState, onAction = viewModel::onAction, onClickBack = …)` 호출에서 `onClickBack` 인자를 삭제하고, `JoinedRoomsContentScreen`·`LoadedJoinedRooms`의 `onClickBack: () -> Unit` 파라미터와 전달부를 삭제한다.
3. `LoadedJoinedRooms`의 헤더 `Row(...) { Text("참여한 방"); Row { Text("설정"); Text("닫기") } }` 전체를 아래로 교체:

```kotlin
        Text(
            text = "참여한 방",
            color = PassmateColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.48).sp
        )
```

4. `LoadedJoinedRooms` 본문에서 아래 4줄을 삭제:

```kotlin
        HostedRoomsRow(onClick = { onAction(JoinedRoomsAction.ClickHostedRooms) })
        EarningsRow(onClick = { onAction(JoinedRoomsAction.ClickEarnings) })
        ReputationRow(onClick = { onAction(JoinedRoomsAction.ClickReputation) })
        CoinHistoryRow(onClick = { onAction(JoinedRoomsAction.ClickCoinHistory) })
```

5. `private fun HostedRoomsRow`·`EarningsRow`·`ReputationRow`·`CoinHistoryRow` 컴포저블 4개(각각 `@Composable` 어노테이션부터 닫는 중괄호까지)를 삭제한다.
6. 하단 패딩 `bottom = 24.dp`를 `bottom = 96.dp`로 바꾼다(탭 바 높이만큼 여유 — Android는 Scaffold가 잡아주지만 Desktop은 직접 잡는다).
7. 안 쓰는 import(`clickable` 등)는 컴파일러 경고를 보고 정리한다.

- [ ] **Step 6: 테스트·컴파일 통과 확인**

Run: `sh gradlew :composeApp:jvmTest --console=plain && sh gradlew :composeApp:compileDebugKotlinAndroid --console=plain`
Expected: 둘 다 `BUILD SUCCESSFUL`

- [ ] **Step 7: 커밋**

```bash
git add -A composeApp/src
git commit -m "feat(mypage): 참여한 방 탭(M-08) — JoinedRooms에서 메뉴 행 4개·설정·닫기 제거, 탭 루트 헤더로 정리 + VM 테스트

Claude-Session: https://claude.ai/code/session_01EQVuB7AawaJr3kvsfXy3xN"
```

---

### Task 4: 마이 탭 ViewModel (M-12) — TDD

**Files:**
- Modify: `ui/mypage/MyInfoUiState.kt`, `MyInfoAction.kt`, `MyInfoEvent.kt`, `MyInfoViewModel.kt`
- Create: `jvmTest/.../testing/FakePaymentRepository.kt`
- Test: `jvmTest/.../ui/mypage/MyInfoViewModelTest.kt`
- Modify: `di/ViewModelModule.kt`

**Interfaces:**
- Produces: `MyInfoViewModel(getMyProfileUseCase, getMyCoinsUseCase, getEarningsUseCase, signOutUseCase, isSignedInUseCase)`
- Produces: `MyInfoUiState(isLoading, loadFailed, profile, defaultMethod, recentTransaction, isCoinInfoFailed, settlementAccount, nextPayout, isEarningsFailed, isProcessing)`
- Produces: `MyInfoAction.{Enter, Retry, ClickProfile, ClickEditProfile, ClickCharge, ClickPaymentMethod, ClickCoinHistory, ClickSettlementAccount, ClickEarnings, ClickNotifications, ClickSettings, ConfirmSignOut, ProfileUpdated, PaymentMethodUpdated, AccountUpdated, Notice(message)}`
- Produces: `MyInfoEvent.{RequireSignIn, OpenReputation, OpenEditProfile(nickname, avatarId), OpenPaymentMethod, OpenCoinHistory, OpenSettlementAccount, OpenEarnings, OpenNotifications, OpenSettings, SignedOut, ShowNotice(message)}`
- Produces: `FakePaymentRepository(coinsResult, earningsResult)`

- [ ] **Step 1: `FakePaymentRepository` 작성**

`jvmTest/.../testing/FakePaymentRepository.kt`:

```kotlin
package org.sesacteamproject.passmate.testing

import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.payment.domain.model.ChargeConfirm
import org.sesacteamproject.passmate.payment.domain.model.CoinBalance
import org.sesacteamproject.passmate.payment.domain.model.CoinCheckout
import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.payment.domain.model.Earnings
import org.sesacteamproject.passmate.payment.domain.model.EntryPayment
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.payment.domain.model.PublicRoom
import org.sesacteamproject.passmate.payment.domain.model.RoomSort
import org.sesacteamproject.passmate.payment.domain.model.RoomTypeFilter
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccount
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository

class FakePaymentRepository(
    var coinsResult: AppResult<CoinBalance> = AppResult.Failure(AppError.Unknown()),
    var earningsResult: AppResult<Earnings> = AppResult.Failure(AppError.Unknown())
) : PaymentRepository {

    var coinsCalls: Int = 0

    var earningsCalls: Int = 0

    override suspend fun getMyCoins(): AppResult<CoinBalance> {
        coinsCalls += 1
        return coinsResult
    }

    override suspend fun getCoinTransactions(cursor: String?): AppResult<PagedResult<CoinTransaction>> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun requestCharge(amount: Int, method: PaymentMethod, roomId: Long?): AppResult<CoinCheckout> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun confirmCharge(chargeId: String, paymentId: String, roomId: Long?): AppResult<ChargeConfirm> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun payEntryFee(roomId: Long, nickname: String, avatarId: Int?): AppResult<EntryPayment> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun getPublicRooms(
        sort: RoomSort,
        query: String?,
        type: RoomTypeFilter,
        cursor: String?
    ): AppResult<PagedResult<PublicRoom>> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun getEarnings(cursor: String?): AppResult<Earnings> {
        earningsCalls += 1
        return earningsResult
    }

    override suspend fun getSettlementAccount(): AppResult<SettlementAccount> {
        return AppResult.Failure(AppError.NotFound())
    }

    override suspend fun saveSettlementAccount(account: SettlementAccount): AppResult<Unit> {
        return AppResult.Success(Unit)
    }

    override suspend fun setDefaultPaymentMethod(method: PaymentMethod): AppResult<Unit> {
        return AppResult.Success(Unit)
    }
}
```

- [ ] **Step 2: 실패 테스트 작성**

`jvmTest/.../ui/mypage/MyInfoViewModelTest.kt`:

```kotlin
package org.sesacteamproject.passmate.ui.mypage

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.SignOutUseCase
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.payment.domain.model.CoinBalance
import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.payment.domain.model.CoinTransactionType
import org.sesacteamproject.passmate.payment.domain.model.Earnings
import org.sesacteamproject.passmate.payment.domain.model.NextPayout
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccountSummary
import org.sesacteamproject.passmate.payment.domain.usecase.GetEarningsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetMyCoinsUseCase
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.testing.FakeAuthRepository
import org.sesacteamproject.passmate.testing.FakePaymentRepository
import org.sesacteamproject.passmate.testing.FakeUserRepository
import org.sesacteamproject.passmate.testing.TestMainDispatcher
import org.sesacteamproject.passmate.user.domain.model.UserProfile
import org.sesacteamproject.passmate.user.domain.usecase.GetMyProfileUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class MyInfoViewModelTest {

    private val profile = UserProfile(
        nickname = "준영",
        email = "junyoung@example.com",
        joinedAt = "2026-08-01T00:00:00Z",
        avatarId = 3,
        level = HostLevel.GROWING,
        coins = 1200L,
        joinedRoomCount = 32,
        hostedRoomCount = 12
    )

    private val coins = CoinBalance(
        balance = 1200,
        defaultMethod = PaymentMethod.KAKAO_PAY,
        recent = CoinTransaction(
            id = 1L,
            type = CoinTransactionType.DEDUCT,
            amount = -10000,
            balanceAfter = 1200,
            method = null,
            roomTitle = "Spring 실전 모의고사",
            paymentNo = null,
            createdAt = "2026-08-22T10:00:00Z"
        )
    )

    private val earnings = Earnings(
        monthlyTotal = 384000L,
        hostSharePercent = 80,
        nextPayout = NextPayout(dateLabel = "9/5", amount = 64000L),
        paidRoomCount = 12,
        studentCount = 48,
        items = emptyList(),
        nextCursor = null,
        hasNext = false,
        account = SettlementAccountSummary(bankName = "국민", maskedNumber = "***-***-4821", payoutNote = null)
    )

    private lateinit var authRepository: FakeAuthRepository

    private lateinit var userRepository: FakeUserRepository

    private lateinit var paymentRepository: FakePaymentRepository

    private fun viewModel(isSignedIn: Boolean = true): MyInfoViewModel {
        authRepository = FakeAuthRepository(isSignedIn)
        return MyInfoViewModel(
            getMyProfileUseCase = GetMyProfileUseCase(userRepository),
            getMyCoinsUseCase = GetMyCoinsUseCase(paymentRepository),
            getEarningsUseCase = GetEarningsUseCase(paymentRepository),
            signOutUseCase = SignOutUseCase(authRepository),
            isSignedInUseCase = IsSignedInUseCase(authRepository)
        )
    }

    @BeforeTest
    fun setUp() {
        TestMainDispatcher.install()
        userRepository = FakeUserRepository(profileResult = AppResult.Success(profile))
        paymentRepository = FakePaymentRepository(
            coinsResult = AppResult.Success(coins),
            earningsResult = AppResult.Success(earnings)
        )
    }

    @AfterTest
    fun tearDown() {
        TestMainDispatcher.reset()
    }

    @Test
    fun guestEnterRequiresSignIn() = runTest {
        val viewModel = viewModel(isSignedIn = false)
        val events = mutableListOf<MyInfoEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(MyInfoAction.Enter)

        assertEquals(listOf<MyInfoEvent>(MyInfoEvent.RequireSignIn), events)
        assertEquals(0, paymentRepository.coinsCalls)
    }

    @Test
    fun memberEnterLoadsProfileCoinsAndEarnings() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(MyInfoAction.Enter)

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(false, state.loadFailed)
        assertEquals(profile, state.profile)
        assertEquals(PaymentMethod.KAKAO_PAY, state.defaultMethod)
        assertEquals(coins.recent, state.recentTransaction)
        assertEquals(earnings.account, state.settlementAccount)
        assertEquals(earnings.nextPayout, state.nextPayout)
        assertEquals(false, state.isCoinInfoFailed)
        assertEquals(false, state.isEarningsFailed)
    }

    @Test
    fun profileFailureMarksWholeScreenFailed() = runTest {
        userRepository.profileResult = AppResult.Failure(AppError.NetworkError())
        val viewModel = viewModel()

        viewModel.onAction(MyInfoAction.Enter)

        assertEquals(true, viewModel.uiState.value.loadFailed)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun coinFailureOnlyMarksCoinCard() = runTest {
        paymentRepository.coinsResult = AppResult.Failure(AppError.NetworkError())
        val viewModel = viewModel()

        viewModel.onAction(MyInfoAction.Enter)

        val state = viewModel.uiState.value
        assertEquals(false, state.loadFailed)
        assertEquals(profile, state.profile)
        assertEquals(true, state.isCoinInfoFailed)
        assertEquals(false, state.isEarningsFailed)
    }

    @Test
    fun earningsFailureOnlyMarksSettlementCard() = runTest {
        paymentRepository.earningsResult = AppResult.Failure(AppError.NetworkError())
        val viewModel = viewModel()

        viewModel.onAction(MyInfoAction.Enter)

        val state = viewModel.uiState.value
        assertEquals(false, state.loadFailed)
        assertEquals(true, state.isEarningsFailed)
        assertEquals(null, state.settlementAccount)
    }

    @Test
    fun confirmSignOutEmitsSignedOut() = runTest {
        val viewModel = viewModel()
        val events = mutableListOf<MyInfoEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(MyInfoAction.Enter)
        viewModel.onAction(MyInfoAction.ConfirmSignOut)

        assertEquals(listOf<MyInfoEvent>(MyInfoEvent.SignedOut), events)
        assertEquals(1, authRepository.signOutCount)
        assertEquals(false, viewModel.uiState.value.isProcessing)
    }

    @Test
    fun clickActionsEmitOpenEvents() = runTest {
        val viewModel = viewModel()
        val events = mutableListOf<MyInfoEvent>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.event.collect { events.add(it) }
        }
        viewModel.onAction(MyInfoAction.Enter)
        viewModel.onAction(MyInfoAction.ClickProfile)
        viewModel.onAction(MyInfoAction.ClickEditProfile)
        viewModel.onAction(MyInfoAction.ClickCharge)
        viewModel.onAction(MyInfoAction.ClickSettlementAccount)
        viewModel.onAction(MyInfoAction.ClickSettings)

        assertEquals(
            listOf(
                MyInfoEvent.OpenReputation,
                MyInfoEvent.OpenEditProfile(nickname = "준영", avatarId = 3),
                MyInfoEvent.ShowNotice("코인 충전은 준비 중이에요"),
                MyInfoEvent.OpenSettlementAccount,
                MyInfoEvent.OpenSettings
            ),
            events
        )
    }

    @Test
    fun updatedActionsReloadOnlyTheirSection() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(MyInfoAction.Enter)
        viewModel.onAction(MyInfoAction.PaymentMethodUpdated)
        viewModel.onAction(MyInfoAction.AccountUpdated)

        assertEquals(2, paymentRepository.coinsCalls)
        assertEquals(2, paymentRepository.earningsCalls)
    }
}
```

- [ ] **Step 3: 테스트 실패 확인**

Run: `sh gradlew :composeApp:jvmTest --tests "org.sesacteamproject.passmate.ui.mypage.MyInfoViewModelTest" --console=plain`
Expected: 컴파일 실패 — 생성자 인자·`MyInfoAction.ClickProfile` 등 미정의

- [ ] **Step 4: UiState/Action/Event 교체**

`ui/mypage/MyInfoUiState.kt`:

```kotlin
package org.sesacteamproject.passmate.ui.mypage

import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.payment.domain.model.NextPayout
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccountSummary
import org.sesacteamproject.passmate.user.domain.model.UserProfile

// 마이 탭 루트 (M-12). 프로필 실패 = 전체 에러, 코인·정산 실패 = 해당 카드만 실패 표시 (규칙 §9)
data class MyInfoUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val profile: UserProfile? = null,
    val defaultMethod: PaymentMethod? = null,
    val recentTransaction: CoinTransaction? = null,
    val isCoinInfoFailed: Boolean = false,
    val settlementAccount: SettlementAccountSummary? = null,
    val nextPayout: NextPayout? = null,
    val isEarningsFailed: Boolean = false,
    // 로그아웃 요청 in-flight — 중복 호출 방지 (규칙 §9)
    val isProcessing: Boolean = false
)
```

`ui/mypage/MyInfoAction.kt`:

```kotlin
package org.sesacteamproject.passmate.ui.mypage

sealed interface MyInfoAction {
    data object Enter : MyInfoAction

    data object Retry : MyInfoAction

    // 프로필 카드 탭 → 내 명성·뱃지 (M-09)
    data object ClickProfile : MyInfoAction

    // 닉네임·내 캐릭터 변경 시트 (M-12-1·M-12-7)
    data object ClickEditProfile : MyInfoAction

    // 코인 충전 (M-12-4~6) — 전용 화면은 후속 작업, 지금은 안내만
    data object ClickCharge : MyInfoAction

    data object ClickPaymentMethod : MyInfoAction

    data object ClickCoinHistory : MyInfoAction

    data object ClickSettlementAccount : MyInfoAction

    data object ClickEarnings : MyInfoAction

    data object ClickNotifications : MyInfoAction

    data object ClickSettings : MyInfoAction

    // 확인 다이얼로그를 거친 뒤 호출된다 — 다이얼로그 소유는 화면 (규칙 §11-1)
    data object ConfirmSignOut : MyInfoAction

    // 시트 저장 완료 — 해당 섹션만 다시 불러온다
    data object ProfileUpdated : MyInfoAction

    data object PaymentMethodUpdated : MyInfoAction

    data object AccountUpdated : MyInfoAction

    data class Notice(val message: String) : MyInfoAction
}
```

`ui/mypage/MyInfoEvent.kt`:

```kotlin
package org.sesacteamproject.passmate.ui.mypage

sealed interface MyInfoEvent {
    // 마이는 회원 전용 — 딥링크 직접 진입 대비 보험 (탭 가드는 AppShellViewModel, 규칙 §8)
    data object RequireSignIn : MyInfoEvent

    data object OpenReputation : MyInfoEvent

    data class OpenEditProfile(val nickname: String, val avatarId: Int?) : MyInfoEvent

    data object OpenPaymentMethod : MyInfoEvent

    data object OpenCoinHistory : MyInfoEvent

    data object OpenSettlementAccount : MyInfoEvent

    data object OpenEarnings : MyInfoEvent

    data object OpenNotifications : MyInfoEvent

    data object OpenSettings : MyInfoEvent

    // 로그아웃 완료 → 홈 탭으로 (세션 정리는 shared가 수행)
    data object SignedOut : MyInfoEvent

    data class ShowNotice(val message: String) : MyInfoEvent
}
```

- [ ] **Step 5: ViewModel 교체**

`ui/mypage/MyInfoViewModel.kt` 전체를 아래로 교체:

```kotlin
package org.sesacteamproject.passmate.ui.mypage

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.SignOutUseCase
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.payment.domain.usecase.GetEarningsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetMyCoinsUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetMyProfileUseCase

// 마이 탭 루트 (M-12) — 프로필·코인·정산 3섹션을 독립 로드한다. 금액·등급 계산은 전부 서버 값 렌더 (규칙 §1)
class MyInfoViewModel(
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val getMyCoinsUseCase: GetMyCoinsUseCase,
    private val getEarningsUseCase: GetEarningsUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<MyInfoUiState, MyInfoAction, MyInfoEvent>(MyInfoUiState()) {

    private var hasEntered = false

    private fun onEnter() {
        if (hasEntered) {
            return
        }
        hasEntered = true
        // 회원 전용 가드 — 서버 검증이 최종 권위 (규칙 §8)
        if (!isSignedInUseCase.invoke()) {
            emit(MyInfoEvent.RequireSignIn)
        } else {
            loadAll()
        }
    }

    private fun loadAll() {
        loadProfile()
        loadCoinInfo()
        loadEarnings()
    }

    private fun loadProfile() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            getMyProfileUseCase.invoke()
                .onSuccess { profile ->
                    _uiState.update { it.copy(isLoading = false, loadFailed = false, profile = profile) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }

    private fun loadCoinInfo() {
        viewModelScope.launch {
            getMyCoinsUseCase.invoke()
                .onSuccess { coins ->
                    _uiState.update {
                        it.copy(
                            defaultMethod = coins.defaultMethod,
                            recentTransaction = coins.recent,
                            isCoinInfoFailed = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isCoinInfoFailed = true) }
                }
        }
    }

    private fun loadEarnings() {
        viewModelScope.launch {
            getEarningsUseCase.invoke(null)
                .onSuccess { earnings ->
                    _uiState.update {
                        it.copy(
                            settlementAccount = earnings.account,
                            nextPayout = earnings.nextPayout,
                            isEarningsFailed = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isEarningsFailed = true) }
                }
        }
    }

    private fun onClickEditProfile() {
        val profile = _uiState.value.profile

        if (profile != null) {
            emit(MyInfoEvent.OpenEditProfile(profile.nickname, profile.avatarId))
        }
    }

    private fun onConfirmSignOut() {
        if (_uiState.value.isProcessing) {
            return
        }
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            // 로컬 세션 정리는 shared가 항상 수행 — 실패 케이스 없음 (M-12-11)
            signOutUseCase.invoke()
            _uiState.update { it.copy(isProcessing = false) }
            _event.emit(MyInfoEvent.SignedOut)
        }
    }

    private fun onProfileUpdated() {
        loadProfile()
        emit(MyInfoEvent.ShowNotice("내 정보를 저장했어요"))
    }

    private fun onPaymentMethodUpdated() {
        loadCoinInfo()
        emit(MyInfoEvent.ShowNotice("기본 결제 수단을 저장했어요"))
    }

    private fun onAccountUpdated() {
        loadEarnings()
        emit(MyInfoEvent.ShowNotice("정산 계좌를 저장했어요"))
    }

    private fun emit(event: MyInfoEvent) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }

    override fun onAction(action: MyInfoAction) {
        when (action) {
            is MyInfoAction.Enter -> onEnter()
            is MyInfoAction.Retry -> loadAll()
            is MyInfoAction.ClickProfile -> emit(MyInfoEvent.OpenReputation)
            is MyInfoAction.ClickEditProfile -> onClickEditProfile()
            is MyInfoAction.ClickCharge -> emit(MyInfoEvent.ShowNotice("코인 충전은 준비 중이에요"))
            is MyInfoAction.ClickPaymentMethod -> emit(MyInfoEvent.OpenPaymentMethod)
            is MyInfoAction.ClickCoinHistory -> emit(MyInfoEvent.OpenCoinHistory)
            is MyInfoAction.ClickSettlementAccount -> emit(MyInfoEvent.OpenSettlementAccount)
            is MyInfoAction.ClickEarnings -> emit(MyInfoEvent.OpenEarnings)
            is MyInfoAction.ClickNotifications -> emit(MyInfoEvent.OpenNotifications)
            is MyInfoAction.ClickSettings -> emit(MyInfoEvent.OpenSettings)
            is MyInfoAction.ConfirmSignOut -> onConfirmSignOut()
            is MyInfoAction.ProfileUpdated -> onProfileUpdated()
            is MyInfoAction.PaymentMethodUpdated -> onPaymentMethodUpdated()
            is MyInfoAction.AccountUpdated -> onAccountUpdated()
            is MyInfoAction.Notice -> emit(MyInfoEvent.ShowNotice(action.message))
        }
    }
}
```

- [ ] **Step 6: Koin factory 갱신**

`di/ViewModelModule.kt`: `factory { MyInfoViewModel(get(), get(), get(), get()) }` → `factory { MyInfoViewModel(get(), get(), get(), get(), get()) }`

- [ ] **Step 7: 테스트 통과 확인 (Screen은 아직 옛 Action을 써서 컴파일이 깨진다 → 이 단계에선 jvmTest만 돌리지 말고, Step 8의 임시 조치 후 돌린다)**

`MyInfoScreen.kt`에서 컴파일을 막는 참조를 임시로 맞춘다: `MyInfoAction.ConfirmDeleteAccount` 분기·`showDeleteConfirm` 다이얼로그·`onClickDelete` 파라미터·`"회원 탈퇴"` `SettingRow`를 삭제하고, `MyInfoEvent.AccountDeleted` 분기를 삭제하고, 이벤트 `when`에 `else -> Unit` 분기를 임시로 추가한다(새 이벤트 `OpenReputation`·`OpenSettlementAccount`·`OpenEarnings`·`OpenSettings`가 아직 화면에 없어 sealed `when`이 비완전해지기 때문). (Task 5에서 화면을 통째로 다시 쓴다.)

Run: `sh gradlew :composeApp:jvmTest --console=plain`
Expected: `BUILD SUCCESSFUL` — MyInfoViewModelTest 8건 포함 전부 통과

- [ ] **Step 8: 커밋**

```bash
git add -A composeApp/src
git commit -m "feat(mypage): 마이 탭 ViewModel(M-12) — 프로필·코인(기본 수단·최근 내역)·정산(계좌·이번 달 예정) 독립 로드, 부분 실패 상태, 로그아웃 + 테스트 8건

Claude-Session: https://claude.ai/code/session_01EQVuB7AawaJr3kvsfXy3xN"
```

---

### Task 5: 마이 탭 화면 (M-12) — Compose

**Files:**
- Modify: `ui/mypage/MyInfoScreen.kt` (전체 교체)

**Interfaces:**
- Consumes: Task 4의 `MyInfoUiState/Action/Event`, 기존 `EditProfileSheet`·`PaymentMethodSheet`·`NotificationSettingsSheet`(`ui/mypage`)·`SettlementAccountSheet`(`ui/payment`), `StudentAvatar(avatarId, modifier)`, `ReputationBadge(level, modifier)`
- Produces: `MyInfoScreen(onNavigate: (NavigationAction) -> Unit)`

- [ ] **Step 1: `MyInfoScreen.kt` 전체 교체**

```kotlin
package org.sesacteamproject.passmate.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.ReputationBadge
import org.sesacteamproject.passmate.component.StudentAvatar
import org.sesacteamproject.passmate.component.StudentAvatars
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.ui.payment.PaymentMethodSheet
import org.sesacteamproject.passmate.ui.payment.SettlementAccountSheet
import org.sesacteamproject.passmate.user.domain.model.UserProfile

// 시트 4종 중 무엇이 열려 있는지 — 표시 여부는 이 화면이 소유한다 (규칙 §11-1)
private enum class MyInfoSheet {
    EDIT_PROFILE,
    PAYMENT_METHOD,
    SETTLEMENT_ACCOUNT,
    NOTIFICATIONS
}

// Figma "UI 디자인 v6" M-12(349:9683) — 마이 탭 루트: 프로필·계정·코인·정산·알림·로그아웃
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyInfoScreen(onNavigate: (NavigationAction) -> Unit) {
    val viewModel: MyInfoViewModel = koinScreenViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState()
    var activeSheet by remember { mutableStateOf<MyInfoSheet?>(null) }
    var editInitial by remember { mutableStateOf<Pair<String, Int?>>("" to null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onAction(MyInfoAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is MyInfoEvent.RequireSignIn -> onNavigate(NavigationAction.NavigateToSignIn)
                is MyInfoEvent.OpenReputation -> onNavigate(NavigationAction.NavigateToReputation)
                is MyInfoEvent.OpenEditProfile -> {
                    editInitial = event.nickname to event.avatarId
                    activeSheet = MyInfoSheet.EDIT_PROFILE
                }
                is MyInfoEvent.OpenPaymentMethod -> activeSheet = MyInfoSheet.PAYMENT_METHOD
                is MyInfoEvent.OpenCoinHistory -> onNavigate(NavigationAction.NavigateToCoinHistory)
                is MyInfoEvent.OpenSettlementAccount -> activeSheet = MyInfoSheet.SETTLEMENT_ACCOUNT
                is MyInfoEvent.OpenEarnings -> onNavigate(NavigationAction.NavigateToEarnings)
                is MyInfoEvent.OpenNotifications -> activeSheet = MyInfoSheet.NOTIFICATIONS
                is MyInfoEvent.OpenSettings -> onNavigate(NavigationAction.NavigateToSettings)
                is MyInfoEvent.SignedOut -> onNavigate(NavigationAction.NavigateToHome)
                is MyInfoEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        MyInfoContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onClickSignOut = { showSignOutConfirm = true }
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    activeSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = PassmateColors.Surface
        ) {
            when (sheet) {
                MyInfoSheet.EDIT_PROFILE -> EditProfileSheet(
                    initialNickname = editInitial.first,
                    initialAvatarId = editInitial.second,
                    onSaved = {
                        activeSheet = null
                        viewModel.onAction(MyInfoAction.ProfileUpdated)
                    },
                    onNotice = { viewModel.onAction(MyInfoAction.Notice(it)) },
                    onClose = { activeSheet = null }
                )
                MyInfoSheet.PAYMENT_METHOD -> PaymentMethodSheet(
                    onSaved = {
                        activeSheet = null
                        viewModel.onAction(MyInfoAction.PaymentMethodUpdated)
                    },
                    onNotice = { viewModel.onAction(MyInfoAction.Notice(it)) },
                    onClose = { activeSheet = null }
                )
                MyInfoSheet.SETTLEMENT_ACCOUNT -> SettlementAccountSheet(
                    onSaved = {
                        activeSheet = null
                        viewModel.onAction(MyInfoAction.AccountUpdated)
                    },
                    onNotice = { viewModel.onAction(MyInfoAction.Notice(it)) },
                    onClose = { activeSheet = null }
                )
                MyInfoSheet.NOTIFICATIONS -> NotificationSettingsSheet(
                    onNotice = { viewModel.onAction(MyInfoAction.Notice(it)) },
                    onClose = { activeSheet = null }
                )
            }
        }
    }
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = {
                Text(
                    text = "로그아웃 할까요?",
                    color = PassmateColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "다시 로그인하면 기록과 코인은 그대로 있어요.",
                    color = PassmateColors.TextSecondary,
                    fontSize = 14.sp,
                    letterSpacing = (-0.28).sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirm = false
                        viewModel.onAction(MyInfoAction.ConfirmSignOut)
                    }
                ) {
                    Text(text = "로그아웃", color = PassmateColors.WeakTopicText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text(text = "취소", color = PassmateColors.TextSecondary)
                }
            },
            containerColor = PassmateColors.Surface
        )
    }
}

@Composable
private fun MyInfoContentScreen(
    uiState: MyInfoUiState,
    onAction: (MyInfoAction) -> Unit,
    onClickSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
    ) {
        when {
            uiState.isLoading -> LoadingBox()
            uiState.loadFailed -> ErrorBox(onRetry = { onAction(MyInfoAction.Retry) })
            else -> LoadedMyInfo(
                uiState = uiState,
                onAction = onAction,
                onClickSignOut = onClickSignOut
            )
        }
    }
}

@Composable
private fun LoadedMyInfo(
    uiState: MyInfoUiState,
    onAction: (MyInfoAction) -> Unit,
    onClickSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 60.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "마이",
                color = PassmateColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.48).sp
            )
            Text(
                text = "설정",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp,
                modifier = Modifier
                    .clickable { onAction(MyInfoAction.ClickSettings) }
                    .padding(4.dp)
            )
        }
        uiState.profile?.let { profile ->
            ProfileCard(
                profile = profile,
                onClick = { onAction(MyInfoAction.ClickProfile) }
            )
            SectionCard {
                InfoRow(
                    title = "닉네임",
                    subtitle = profile.nickname,
                    actionLabel = "변경",
                    onClick = { onAction(MyInfoAction.ClickEditProfile) }
                )
                RowDivider()
                InfoRow(
                    title = "내 캐릭터",
                    subtitle = StudentAvatars.nameOf(profile.avatarId ?: StudentAvatars.DEFAULT_ID),
                    actionLabel = "변경",
                    onClick = { onAction(MyInfoAction.ClickEditProfile) }
                )
            }
            SectionCard {
                CoinRow(
                    coins = profile.coins,
                    onClickCharge = { onAction(MyInfoAction.ClickCharge) }
                )
                RowDivider()
                InfoRow(
                    title = "결제 수단",
                    subtitle = paymentMethodSubtitle(uiState),
                    actionLabel = "관리",
                    onClick = { onAction(MyInfoAction.ClickPaymentMethod) }
                )
                RowDivider()
                InfoRow(
                    title = "코인 내역",
                    subtitle = recentTransactionSubtitle(uiState),
                    actionLabel = "보기",
                    onClick = { onAction(MyInfoAction.ClickCoinHistory) }
                )
            }
            SectionCard {
                InfoRow(
                    title = "정산 계좌",
                    subtitle = settlementAccountSubtitle(uiState),
                    actionLabel = "변경",
                    onClick = { onAction(MyInfoAction.ClickSettlementAccount) }
                )
                RowDivider()
                InfoRow(
                    title = "이번 달 정산 예정",
                    subtitle = nextPayoutSubtitle(uiState),
                    actionLabel = "내역",
                    onClick = { onAction(MyInfoAction.ClickEarnings) }
                )
            }
            SectionCard {
                InfoRow(
                    title = "알림",
                    subtitle = "세션 시작 · 별점 요청 · 정산",
                    actionLabel = "설정",
                    onClick = { onAction(MyInfoAction.ClickNotifications) }
                )
            }
        }
        SignOutButton(
            isProcessing = uiState.isProcessing,
            onClick = onClickSignOut
        )
    }
}

@Composable
private fun ProfileCard(
    profile: UserProfile,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(20.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudentAvatar(
            avatarId = profile.avatarId,
            modifier = Modifier.size(52.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = profile.nickname,
                    color = PassmateColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.36).sp
                )
                profile.level?.let { level ->
                    ReputationBadge(level = level)
                }
            }
            Text(
                text = "참여한 방 ${profile.joinedRoomCount ?: 0} · 내가 만든 방 ${profile.hostedRoomCount ?: 0}",
                color = PassmateColors.TextSecondary,
                fontSize = 13.sp,
                letterSpacing = (-0.26).sp
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        content()
    }
}

@Composable
private fun RowDivider() {
    Divider(color = PassmateColors.Border, thickness = 1.dp)
}

@Composable
private fun InfoRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = PassmateColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = subtitle,
                color = PassmateColors.TextSecondary,
                fontSize = 13.sp,
                letterSpacing = (-0.26).sp
            )
        }
        Text(
            text = "$actionLabel ›",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun CoinRow(
    coins: Long?,
    onClickCharge: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "보유 코인",
                color = PassmateColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = "${formatNumber(coins ?: 0L)} C · 유료 방 참가비에 사용",
                color = PassmateColors.TextSecondary,
                fontSize = 13.sp,
                letterSpacing = (-0.26).sp
            )
        }
        Text(
            text = "코인 충전",
            color = PassmateColors.Surface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(PassmateColors.Primary, RoundedCornerShape(12.dp))
                .clickable(onClick = onClickCharge)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun SignOutButton(
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .clickable(enabled = !isProcessing, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = PassmateColors.Primary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "로그아웃",
                color = PassmateColors.TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PassmateColors.Primary)
    }
}

@Composable
private fun ErrorBox(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "내 정보를 불러오지 못했어요",
            color = PassmateColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.32).sp
        )
        Text(
            text = "다시 시도",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            modifier = Modifier
                .padding(top = 12.dp)
                .clickable(onClick = onRetry)
                .padding(8.dp)
        )
    }
}

private fun paymentMethodSubtitle(uiState: MyInfoUiState): String {
    val method = uiState.defaultMethod

    return if (uiState.isCoinInfoFailed) {
        "불러오지 못했어요"
    } else if (method != null) {
        "${method.label} · 포트원 안전결제"
    } else {
        "기본 결제 수단을 설정해 주세요"
    }
}

private fun recentTransactionSubtitle(uiState: MyInfoUiState): String {
    val recent = uiState.recentTransaction

    return if (uiState.isCoinInfoFailed) {
        "불러오지 못했어요"
    } else if (recent != null) {
        "최근 ${shortDate(recent)} ${signedCoins(recent.amount)} C"
    } else {
        "아직 내역이 없어요"
    }
}

private fun settlementAccountSubtitle(uiState: MyInfoUiState): String {
    val account = uiState.settlementAccount

    return if (uiState.isEarningsFailed) {
        "불러오지 못했어요"
    } else if (account != null) {
        "${account.bankName} ${account.maskedNumber}"
    } else {
        "계좌를 등록해 주세요"
    }
}

private fun nextPayoutSubtitle(uiState: MyInfoUiState): String {
    val payout = uiState.nextPayout

    return if (uiState.isEarningsFailed) {
        "불러오지 못했어요"
    } else if (payout != null) {
        "₩${formatNumber(payout.amount)} · ${payout.dateLabel} 지급"
    } else {
        "정산 예정 없음"
    }
}

// "2026-08-22T10:00:00Z" → "8/22". 파싱 실패 시 원문 앞 10자
private fun shortDate(transaction: CoinTransaction): String {
    val raw = transaction.createdAt ?: return ""
    val parts = raw.take(10).split("-")

    return if (parts.size == 3) {
        "${parts[1].trimStart('0')}/${parts[2].trimStart('0')}"
    } else {
        raw.take(10)
    }
}

private fun signedCoins(amount: Int): String {
    return if (amount > 0) {
        "+${formatNumber(amount.toLong())}"
    } else {
        "-${formatNumber(-amount.toLong())}"
    }
}

private fun formatNumber(value: Long): String {
    return value.toString().reversed().chunked(3).joinToString(",").reversed()
}
```

- [ ] **Step 2: 캐릭터 이름 조회 확인**

`component/StudentAvatar.kt`의 `StudentAvatars.nameOf(avatarId: Int): String`(1=고양이 … 12=공룡, 범위 밖은 기본 1)을 그대로 쓴다. 추가 코드 없음.

- [ ] **Step 3: 컴파일·테스트**

Run: `sh gradlew :composeApp:jvmTest --console=plain && sh gradlew :composeApp:compileDebugKotlinAndroid --console=plain`
Expected: 둘 다 `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add -A composeApp/src
git commit -m "feat(mypage): 마이 탭 화면(M-12) — 프로필 카드(→명성)·계정·코인(충전 안내·결제 수단·최근 내역)·정산(계좌 시트·이번 달 예정)·알림·로그아웃 확인, 우상단 설정

Claude-Session: https://claude.ai/code/session_01EQVuB7AawaJr3kvsfXy3xN"
```

---

### Task 6: `NavigateToTab` + `PassmateBottomTabBar`

**Files:**
- Modify: `navigation/NavigationAction.kt`
- Create: `component/PassmateBottomTabBar.kt`

**Interfaces:**
- Produces: `NavigationAction.NavigateToTab(val tab: AppTab)`
- Produces: `@Composable fun PassmateBottomTabBar(selectedTab: AppTab?, onSelectTab: (AppTab) -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: `NavigationAction.NavigateToTab` 추가**

`NavigationAction.kt`의 `data object NavigateToHome : NavigationAction` 바로 아래에:

```kotlin
    // 하단 탭 전환 — 셸(AppShellViewModel) 가드 통과 후에만 발행된다
    data class NavigateToTab(val tab: AppTab) : NavigationAction
```

- [ ] **Step 2: 탭 바 컴포넌트 작성**

`component/PassmateBottomTabBar.kt`:

```kotlin
package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.navigation.AppTab
import org.sesacteamproject.passmate.theme.PassmateColors

// 하단 4탭 바 (피그마 v6) — Android·Desktop 공용. 탭 루트에서만 표시한다 (스펙 §1-2)
@Composable
fun PassmateBottomTabBar(
    selectedTab: AppTab?,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().background(PassmateColors.Surface)) {
        Divider(color = PassmateColors.Border, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            AppTab.entries.forEach { tab ->
                TabItem(
                    tab = tab,
                    isSelected = tab == selectedTab,
                    onClick = { onSelectTab(tab) }
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: AppTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (isSelected) PassmateColors.Primary else PassmateColors.TextTertiary

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = iconFor(tab),
            contentDescription = tab.label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = tab.label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = (-0.22).sp
        )
    }
}

private fun iconFor(tab: AppTab): ImageVector {
    return when (tab) {
        AppTab.HOME -> Icons.Filled.Home
        AppTab.HOSTED_ROOMS -> Icons.Filled.Add
        AppTab.JOINED_ROOMS -> Icons.Filled.List
        AppTab.MY_INFO -> Icons.Filled.Person
    }
}
```

- [ ] **Step 3: 컴파일 확인**

Run: `sh gradlew :composeApp:compileDebugKotlinAndroid --console=plain`
Expected: `BUILD SUCCESSFUL`. 만약 `androidx.compose.material.icons`가 unresolved면 `composeApp/build.gradle.kts` `commonMain.dependencies`에 `implementation(compose.materialIconsExtended)`를 추가하고 다시 컴파일한다.

- [ ] **Step 4: 커밋**

```bash
git add composeApp
git commit -m "feat(shell): NavigateToTab 액션 + PassmateBottomTabBar(홈·내가 만든 방·참여한 방·마이) 공통 컴포넌트

Claude-Session: https://claude.ai/code/session_01EQVuB7AawaJr3kvsfXy3xN"
```

---

### Task 7: Android 셸 (`AppNavHost.android.kt`)

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/org/sesacteamproject/passmate/navigation/AppNavHost.android.kt`

**Interfaces:**
- Consumes: `AppTab`, `AppShellViewModel/Action/Event`, `NavigationAction.NavigateToTab`, `PassmateBottomTabBar`, `JoinedRoomsScreen`, `JoinScreen(initialPin, onNavigate)`

- [ ] **Step 1: import 정리**

추가: `androidx.compose.foundation.layout.padding`, `androidx.compose.material3.Scaffold`, `androidx.compose.runtime.LaunchedEffect`, `androidx.compose.runtime.getValue`, `androidx.compose.ui.Modifier`, `androidx.navigation.compose.currentBackStackEntryAsState`, `org.sesacteamproject.passmate.component.PassmateBottomTabBar`, `org.sesacteamproject.passmate.di.koinScreenViewModel`, `org.sesacteamproject.passmate.ui.mypage.JoinedRoomsScreen`
삭제: `org.sesacteamproject.passmate.ui.home.HomeScreen`

- [ ] **Step 2: `handleNavigationAction` 분기 수정**

아래 4개 분기를 교체/추가한다(나머지는 그대로):

```kotlin
        is NavigationAction.NavigateToHome -> navigateToTab(AppTab.HOME)
        is NavigationAction.NavigateToTab -> navigateToTab(action.tab)
        is NavigationAction.NavigateToJoin -> {
            // 홈 탭이 곧 입장 폼 — pin 없는 Join은 홈 탭으로 (스펙 §1-1)
            if (action.pin != null) {
                navigate("join?pin=${action.pin}")
            } else {
                navigateToTab(AppTab.HOME)
            }
        }
        is NavigationAction.NavigateToResult -> {
            // 세션 플로우 엔트리(Waiting·Play·Join)만 제거하고 탭 루트는 유지한다 (규칙 §2-1-2, 스펙 §1-5)
            val hadSession = popBackStack(Route.Waiting.route, inclusive = true)

            if (hadSession) {
                popBackStack(Route.Join.route, inclusive = true)
            }
            navigate("result/${action.roomId}")
        }
```

그리고 파일 상단(`handleNavigationAction` 위)에 탭 전환 헬퍼를 추가:

```kotlin
// 표준 하단 탭 전환 — 탭별 백스택 저장/복원, 홈 탭이 항상 스택 바닥 (스펙 §1-4)
private fun NavHostController.navigateToTab(tab: AppTab) {
    navigate(tab.route) {
        popUpTo(Route.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
```

- [ ] **Step 3: `AppNavHost` 컴포저블 교체**

```kotlin
@Composable
actual fun AppNavHost() {
    val navController = rememberNavController()
    val shellViewModel: AppShellViewModel = koinScreenViewModel()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = AppTab.fromRoute(backStackEntry?.destination?.route)
    val activity = LocalContext.current.findComponentActivity()

    DisposableEffect(activity, navController) {
        val listener = Consumer<Intent> { intent -> navController.handleDeepLink(intent) }
        activity?.addOnNewIntentListener(listener)
        onDispose { activity?.removeOnNewIntentListener(listener) }
    }
    LaunchedEffect(shellViewModel) {
        shellViewModel.event.collect { event ->
            when (event) {
                is AppShellEvent.NavigateToTab -> navController.handleNavigationAction(
                    NavigationAction.NavigateToTab(event.tab)
                )
                is AppShellEvent.RequireSignIn -> navController.handleNavigationAction(
                    NavigationAction.NavigateToSignIn
                )
            }
        }
    }
    Scaffold(
        bottomBar = {
            // 탭 루트 4개에서만 하단 바 표시 (스펙 §1-2)
            if (currentTab != null) {
                PassmateBottomTabBar(
                    selectedTab = currentTab,
                    onSelectTab = { shellViewModel.onAction(AppShellAction.SelectTab(it)) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Route.Home.route) {
                // 홈 탭 = 입장 폼 인라인 (M-01 v6) — JoinScreen 재사용
                JoinScreen(onNavigate = navController::handleNavigationAction)
            }
            // … 기존 composable(...) 블록들 전부 그대로 (RoomList·SignIn·Join·Waiting·Play·Result·Payment·MyInfo·JoinedRooms·Reputation·HostedRooms·RoomReport·SessionControl·CoinHistory·Earnings·Settings)
        }
    }
}
```

기존 `composable(Route.Home.route) { HomeScreen(...) }`는 위의 `JoinScreen` 블록으로 바꾸고, 나머지 `composable` 블록은 `NavHost` 안으로 그대로 옮긴다(들여쓰기만 한 단계).

- [ ] **Step 4: 컴파일**

Run: `sh gradlew :composeApp:compileDebugKotlinAndroid --console=plain`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Android 에뮬레이터 스모크 (가능할 때)**

`JAVA_HOME`을 Homebrew JDK 17로 지정하고 `sh gradlew :composeApp:installDebug` → 에뮬레이터에서: 홈 탭에 입장 폼이 보이고 하단 바 4개, 게스트 상태에서 "마이" 탭 → 로그인 화면으로, 뒤로 → 홈. "PIN 입장"은 백엔드 없어 네트워크 오류 스낵바까지. 에뮬레이터가 없으면 Task 8의 Desktop 스모크로 대체하고 Mac 체크리스트에 남긴다.

- [ ] **Step 6: 커밋**

```bash
git add composeApp/src/androidMain
git commit -m "feat(shell): Android 하단 4탭 셸 — Scaffold 탭 바(탭 루트에서만), AppShell 가드 배선, 홈=JoinScreen, Result 진입 시 세션 엔트리만 제거

Claude-Session: https://claude.ai/code/session_01EQVuB7AawaJr3kvsfXy3xN"
```

---

### Task 8: Desktop 셸 + HostedRooms 뒤로가기 제거 + HomeScreen 삭제 + 스모크

**Files:**
- Modify: `composeApp/src/jvmMain/kotlin/org/sesacteamproject/passmate/navigation/AppNavHost.jvm.kt` (전체 교체)
- Modify: `ui/hostroom/HostedRoomsScreen.kt`
- Delete: `ui/home/HomeScreen.kt`

- [ ] **Step 1: `AppNavHost.jvm.kt` 전체 교체**

```kotlin
package org.sesacteamproject.passmate.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.sesacteamproject.passmate.component.PassmateBottomTabBar
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.ui.auth.SignInScreen
import org.sesacteamproject.passmate.ui.home.RoomListScreen
import org.sesacteamproject.passmate.ui.hostroom.HostedRoomsScreen
import org.sesacteamproject.passmate.ui.hostroom.RoomReportScreen
import org.sesacteamproject.passmate.ui.hostroom.SessionControlScreen
import org.sesacteamproject.passmate.ui.join.JoinScreen
import org.sesacteamproject.passmate.ui.mypage.JoinedRoomsScreen
import org.sesacteamproject.passmate.ui.mypage.MyInfoScreen
import org.sesacteamproject.passmate.ui.mypage.ReputationScreen
import org.sesacteamproject.passmate.ui.mypage.SettingsScreen
import org.sesacteamproject.passmate.ui.payment.CoinHistoryScreen
import org.sesacteamproject.passmate.ui.payment.EarningsScreen
import org.sesacteamproject.passmate.ui.payment.PaymentScreen
import org.sesacteamproject.passmate.ui.play.PlayScreen
import org.sesacteamproject.passmate.ui.result.ResultScreen
import org.sesacteamproject.passmate.ui.waiting.WaitingScreen

// Desktop은 상태 기반 라우트 상태머신 (규칙 §2-1). 탭 전환은 스택을 [탭 루트]로 교체한다 (스펙 §1-4)
private sealed interface JvmDestination {
    data object Home : JvmDestination
    data object RoomList : JvmDestination
    data object SignIn : JvmDestination
    data class Join(val pin: String?) : JvmDestination
    data class Payment(val pin: String) : JvmDestination
    data object CoinHistory : JvmDestination
    data class Waiting(val pin: String) : JvmDestination
    data class Play(val pin: String) : JvmDestination
    data class Result(val roomId: Long) : JvmDestination
    data object MyInfo : JvmDestination
    data object JoinedRooms : JvmDestination
    data object Reputation : JvmDestination
    data object HostedRooms : JvmDestination
    data class RoomReport(val roomId: Long) : JvmDestination
    data class SessionControl(val roomId: Long, val pin: String) : JvmDestination
    data object Earnings : JvmDestination
    data object Settings : JvmDestination
}

private fun JvmDestination.toTab(): AppTab? {
    return when (this) {
        is JvmDestination.Home -> AppTab.HOME
        is JvmDestination.HostedRooms -> AppTab.HOSTED_ROOMS
        is JvmDestination.JoinedRooms -> AppTab.JOINED_ROOMS
        is JvmDestination.MyInfo -> AppTab.MY_INFO
        else -> null
    }
}

private fun AppTab.toDestination(): JvmDestination {
    return when (this) {
        AppTab.HOME -> JvmDestination.Home
        AppTab.HOSTED_ROOMS -> JvmDestination.HostedRooms
        AppTab.JOINED_ROOMS -> JvmDestination.JoinedRooms
        AppTab.MY_INFO -> JvmDestination.MyInfo
    }
}

private fun JvmDestination.isSessionFlow(): Boolean {
    return this is JvmDestination.Join || this is JvmDestination.Waiting || this is JvmDestination.Play
}

@Composable
actual fun AppNavHost() {
    val routeStack = remember { mutableStateListOf<JvmDestination>(JvmDestination.Home) }
    val shellViewModel: AppShellViewModel = koinScreenViewModel()
    val currentDestination = routeStack.last()
    val currentTab = currentDestination.toTab()
    val switchTab: (AppTab) -> Unit = { tab ->
        routeStack.clear()
        routeStack.add(tab.toDestination())
    }
    val onNavigate: (NavigationAction) -> Unit = { action ->
        when (action) {
            is NavigationAction.NavigateToHome -> switchTab(AppTab.HOME)
            is NavigationAction.NavigateToTab -> switchTab(action.tab)
            is NavigationAction.NavigateToRoomList -> routeStack.add(JvmDestination.RoomList)
            is NavigationAction.NavigateToSignIn -> routeStack.add(JvmDestination.SignIn)
            is NavigationAction.NavigateToJoin -> {
                // 홈 탭이 곧 입장 폼 — pin 없는 Join은 홈 탭으로 (스펙 §1-1)
                if (action.pin != null) {
                    routeStack.add(JvmDestination.Join(action.pin))
                } else {
                    switchTab(AppTab.HOME)
                }
            }
            is NavigationAction.NavigateToPayment -> routeStack.add(JvmDestination.Payment(action.pin))
            is NavigationAction.NavigateToWaiting -> routeStack.add(JvmDestination.Waiting(action.pin))
            is NavigationAction.NavigateToPlay -> routeStack.add(JvmDestination.Play(action.pin))
            is NavigationAction.NavigateToResult -> {
                // 세션 플로우 엔트리(Join·Waiting·Play)만 제거, 탭 루트 유지 (규칙 §2-1-2, 스펙 §1-5)
                routeStack.removeAll { it.isSessionFlow() }
                routeStack.add(JvmDestination.Result(action.roomId))
            }
            is NavigationAction.NavigateToMyInfo -> routeStack.add(JvmDestination.MyInfo)
            is NavigationAction.NavigateToReputation -> routeStack.add(JvmDestination.Reputation)
            is NavigationAction.NavigateToHostedRooms -> routeStack.add(JvmDestination.HostedRooms)
            is NavigationAction.NavigateToRoomReport -> routeStack.add(JvmDestination.RoomReport(action.roomId))
            is NavigationAction.NavigateToSessionControl -> routeStack.add(
                JvmDestination.SessionControl(action.roomId, action.pin)
            )
            is NavigationAction.NavigateToCoinHistory -> routeStack.add(JvmDestination.CoinHistory)
            is NavigationAction.NavigateToEarnings -> routeStack.add(JvmDestination.Earnings)
            is NavigationAction.NavigateToSettings -> routeStack.add(JvmDestination.Settings)
            is NavigationAction.NavigateBack -> {
                if (routeStack.size > 1) {
                    routeStack.removeAt(routeStack.lastIndex)
                }
            }
        }
    }

    LaunchedEffect(shellViewModel) {
        shellViewModel.event.collect { event ->
            when (event) {
                is AppShellEvent.NavigateToTab -> onNavigate(NavigationAction.NavigateToTab(event.tab))
                is AppShellEvent.RequireSignIn -> onNavigate(NavigationAction.NavigateToSignIn)
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (currentDestination) {
                is JvmDestination.Home -> JoinScreen(onNavigate = onNavigate)
                is JvmDestination.RoomList -> RoomListScreen(onNavigate = onNavigate)
                is JvmDestination.SignIn -> SignInScreen(onNavigate = onNavigate)
                is JvmDestination.Join -> JoinScreen(
                    initialPin = currentDestination.pin,
                    onNavigate = onNavigate
                )
                is JvmDestination.Payment -> PaymentScreen(
                    pin = currentDestination.pin,
                    onNavigate = onNavigate
                )
                is JvmDestination.CoinHistory -> CoinHistoryScreen(onNavigate = onNavigate)
                is JvmDestination.Earnings -> EarningsScreen(onNavigate = onNavigate)
                is JvmDestination.Settings -> SettingsScreen(onNavigate = onNavigate)
                is JvmDestination.Waiting -> WaitingScreen(
                    pin = currentDestination.pin,
                    onNavigate = onNavigate
                )
                is JvmDestination.Play -> PlayScreen(
                    pin = currentDestination.pin,
                    onNavigate = onNavigate
                )
                is JvmDestination.Result -> ResultScreen(
                    roomId = currentDestination.roomId,
                    onNavigate = onNavigate
                )
                is JvmDestination.MyInfo -> MyInfoScreen(onNavigate = onNavigate)
                is JvmDestination.JoinedRooms -> JoinedRoomsScreen(onNavigate = onNavigate)
                is JvmDestination.Reputation -> ReputationScreen(onNavigate = onNavigate)
                is JvmDestination.HostedRooms -> HostedRoomsScreen(onNavigate = onNavigate)
                is JvmDestination.RoomReport -> RoomReportScreen(
                    roomId = currentDestination.roomId,
                    onNavigate = onNavigate
                )
                is JvmDestination.SessionControl -> SessionControlScreen(
                    roomId = currentDestination.roomId,
                    pin = currentDestination.pin,
                    onNavigate = onNavigate
                )
            }
        }
        // 탭 루트 4개에서만 하단 바 표시 (스펙 §1-2)
        if (currentTab != null) {
            PassmateBottomTabBar(
                selectedTab = currentTab,
                onSelectTab = { shellViewModel.onAction(AppShellAction.SelectTab(it)) }
            )
        }
    }
}
```

- [ ] **Step 2: `HostedRoomsScreen` 뒤로가기 제거**

`HostedRoomsScreen.kt`에서:
1. 컨테이너의 `HostedRoomsContentScreen(... onClickBack = { onNavigate(NavigationAction.NavigateBack) })` 호출에서 `onClickBack` 인자 삭제
2. `HostedRoomsContentScreen`·`LoadedHostedRooms`의 `onClickBack: () -> Unit` 파라미터와 전달부 삭제
3. 헤더 `Row(...) { Text("내가 만든 방"); Text("닫기") }`를 아래로 교체:

```kotlin
        Text(
            text = "내가 만든 방",
            color = PassmateColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.48).sp
        )
```

- [ ] **Step 3: `HomeScreen.kt` 삭제**

```bash
git rm composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/ui/home/HomeScreen.kt
```

`grep -rn "HomeScreen" composeApp/src`가 0건인지 확인.

- [ ] **Step 4: 전체 검증**

Run: `sh gradlew :composeApp:jvmTest --console=plain && sh gradlew :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid --console=plain`
Expected: 둘 다 `BUILD SUCCESSFUL`

- [ ] **Step 5: Desktop 스모크**

Run: `sh gradlew :composeApp:run --console=plain` (창이 뜬다; 8080 백엔드 없음)
확인: (1) 첫 화면이 "패스메이트 / 방 코드를 입력하고 시작하세요" 입장 폼 + 하단 탭 4개, (2) 게스트로 "참여한 방"·"마이"·"내가 만든 방" 탭 클릭 → 로그인 화면(탭 바 숨김), 뒤로 → 홈 탭, (3) "홈" 탭 재클릭 시 폼 유지. 창을 닫아 종료.

- [ ] **Step 6: 커밋**

```bash
git checkout -- gradlew
git add -A composeApp/src
git commit -m "feat(shell): Desktop 하단 4탭 셸 + HostedRooms 탭 루트화(닫기 제거) + 임시 HomeScreen 삭제

Claude-Session: https://claude.ai/code/session_01EQVuB7AawaJr3kvsfXy3xN"
```

---

### Task 9: iOS 셸 기반 — `AppTab`·`AppShellViewModel`·`Route` (Swift)

**Files:**
- Create: `iosApp/iosApp/navigation/AppTab.swift`, `AppShellUiState.swift`, `AppShellAction.swift`, `AppShellEvent.swift`, `AppShellViewModel.swift`
- Modify: `iosApp/iosApp/navigation/Route.swift`
- Modify: `iosApp/iosApp.xcodeproj/project.pbxproj` (idx 145~149, navigation 그룹)

**Interfaces:**
- Produces: `enum AppTab: Hashable, CaseIterable { home, hostedRooms, joinedRooms, myInfo }` with `label`, `systemImage`, `requiresSignIn`
- Produces: `AppShellViewModel(isSignedInUseCase:)` — `action(.selectTab(tab))` → `event` `.navigateToTab(tab)` / `.requireSignIn`
- Produces: `Route.joinedRooms`, `Route.isSessionRoute: Bool`

- [ ] **Step 1: `AppTab.swift`**

```swift
import Foundation

// 하단 4탭 (피그마 v6) — Compose AppTab.kt 미러. 라우트·라벨·로그인 필수 여부 동일 (규칙 §2-1-1)
enum AppTab: Hashable, CaseIterable {
    case home
    case hostedRooms
    case joinedRooms
    case myInfo

    var label: String {
        switch self {
        case .home: return "홈"
        case .hostedRooms: return "내가 만든 방"
        case .joinedRooms: return "참여한 방"
        case .myInfo: return "마이"
        }
    }

    var systemImage: String {
        switch self {
        case .home: return "house"
        case .hostedRooms: return "plus.square"
        case .joinedRooms: return "rectangle.stack"
        case .myInfo: return "person"
        }
    }

    var requiresSignIn: Bool {
        return self != .home
    }
}
```

- [ ] **Step 2: 셸 MVI 타입 3개**

`AppShellUiState.swift`:

```swift
import Foundation

// 셸 상태 — 마지막 탭 선택 시점의 로그인 여부 (탭 탭마다 동기 재조회, 규칙 §8)
struct AppShellUiState {
    var isSignedIn: Bool = false
}
```

`AppShellAction.swift`:

```swift
import Foundation

enum AppShellAction {
    case selectTab(AppTab)
}
```

`AppShellEvent.swift`:

```swift
import Foundation

enum AppShellEvent {
    case navigateToTab(AppTab)

    // 게스트가 로그인 필수 탭을 누름 — 화면을 열지 않고 SignIn으로 (결정 2)
    case requireSignIn
}
```

- [ ] **Step 3: `AppShellViewModel.swift`**

```swift
import Combine
import Foundation
import Shared

// Compose AppShellViewModel.kt 미러 — 하단 탭 게스트 가드 (규칙 §8, 결정 2)
final class AppShellViewModel: ObservableObject {
    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = AppShellUiState()

    let event = PassthroughSubject<AppShellEvent, Never>()

    private func onSelectTab(_ tab: AppTab) {
        let isSignedIn = isSignedInUseCase.invoke()

        uiState.isSignedIn = isSignedIn
        if tab.requiresSignIn && !isSignedIn {
            event.send(.requireSignIn)
        } else {
            event.send(.navigateToTab(tab))
        }
    }

    func action(_ action: AppShellAction) {
        switch action {
        case let .selectTab(tab):
            onSelectTab(tab)
        }
    }

    init(isSignedInUseCase: IsSignedInUseCase) {
        self.isSignedInUseCase = isSignedInUseCase
    }
}
```

- [ ] **Step 4: `Route.swift` 갱신**

`case myInfo` 아래에 `case joinedRooms` 추가하고, enum 끝에 계산 프로퍼티 추가:

```swift
    // 세션 플로우 엔트리 — Result 진입 시 이것만 제거하고 탭 루트는 유지한다 (규칙 §2-1-2, 스펙 §1-5)
    var isSessionRoute: Bool {
        switch self {
        case .join, .waiting, .play: return true
        default: return false
        }
    }
```

- [ ] **Step 5: pbxproj 등록 (idx 145~149, navigation 그룹)**

`project.pbxproj`에 아래를 추가한다. 각 파일마다 3곳(PBXBuildFile · PBXFileReference · Sources 빌드 페이즈) + 그룹 children 1곳. 패턴은 idx 144(`PaymentMethodSheetView.swift`) 줄을 그대로 복사해 이름·번호만 바꾼다.

| idx | 파일 |
|---|---|
| 145 | AppTab.swift |
| 146 | AppShellUiState.swift |
| 147 | AppShellAction.swift |
| 148 | AppShellEvent.swift |
| 149 | AppShellViewModel.swift |

예: 145
- PBXBuildFile 섹션: `		A1010001145AABBCCDDEEFF0145 /* AppTab.swift in Sources */ = {isa = PBXBuildFile; fileRef = A1011001145AABBCCDDEEFF0145 /* AppTab.swift */; };`
- PBXFileReference 섹션: `		A1011001145AABBCCDDEEFF0145 /* AppTab.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = AppTab.swift; sourceTree = "<group>"; };`
- `/* navigation */ = {` 그룹 children(`Route.swift` 아래): `				A1011001145AABBCCDDEEFF0145 /* AppTab.swift */,`
- `PBXSourcesBuildPhase` files: `				A1010001145AABBCCDDEEFF0145 /* AppTab.swift in Sources */,`

등록 후 검사:

```bash
grep -oE '[0-9A-F]{24,28} /\* [^*]+ \*/ = \{' iosApp/iosApp.xcodeproj/project.pbxproj | awk '{print $1}' | sort | uniq -d | wc -l   # → 0
grep -c "AppShellViewModel.swift" iosApp/iosApp.xcodeproj/project.pbxproj   # → 4
```

- [ ] **Step 6: 빌드 확인**

```bash
cd iosApp && xcodebuild -project $PWD/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination "platform=iOS Simulator,id=4EA9F2CB-461B-4B4A-9977-1DC38372DD99" \
  -derivedDataPath $PWD/build/DerivedData CODE_SIGNING_ALLOWED=NO build 2>&1 | grep -E "error:|BUILD (SUCCEEDED|FAILED)"; cd ..
git checkout -- gradlew
```
Expected: `BUILD SUCCEEDED` (아직 ContentView는 옛 구조 — 새 파일이 컴파일만 되면 된다)

- [ ] **Step 7: 커밋**

```bash
git add iosApp
git commit -m "feat(ios): 셸 기반 미러 — AppTab·AppShellViewModel(게스트 가드)·Route.joinedRooms/isSessionRoute + pbxproj idx 145~149

Claude-Session: https://claude.ai/code/session_01EQVuB7AawaJr3kvsfXy3xN"
```

---

### Task 10: iOS 화면 재배치 — `JoinedRoomsView*`·축소 `SettingsView*`

**Files:**
- Rename: `ui/mypage/MyInfo{View,ViewModel,UiState,Action,Event}.swift` → `JoinedRooms*.swift`
- Rename: `ui/mypage/Settings{View,ViewModel,UiState,Action,Event}.swift` → `MyInfo*.swift`
- Create: `ui/mypage/Settings{View,ViewModel,UiState,Action,Event}.swift` (축소판)
- Modify: `ContentView.swift` (rename 반영만 — 탭 셸은 Task 12), `project.pbxproj` (경로 갱신 + idx 150~154)

**Interfaces:**
- Produces: `JoinedRoomsView(onRequireSignIn:onOpenReport:onRejoin:)`, `JoinedRoomsViewModel(getMyPageUseCase:isSignedInUseCase:)`
- Produces: `SettingsView(onRequireSignIn:onAccountDeleted:onBack:)`, `SettingsViewModel(deleteAccountUseCase:isSignedInUseCase:)`
- Produces(중간 상태): `MyInfoView` = 옛 SettingsView 내용(Task 11에서 확장)

- [ ] **Step 1: 1차 rename `MyInfo*` → `JoinedRooms*` (파일·타입·pbxproj)**

```bash
cd iosApp/iosApp/ui/mypage
for n in View ViewModel UiState Action Event; do git mv MyInfo$n.swift JoinedRooms$n.swift; done
sed -i '' 's/MyInfo/JoinedRooms/g' JoinedRoomsView.swift JoinedRoomsViewModel.swift JoinedRoomsUiState.swift JoinedRoomsAction.swift JoinedRoomsEvent.swift
cd ../../..
sed -i '' 's/MyInfoView\.swift/JoinedRoomsView.swift/g; s/MyInfoViewModel\.swift/JoinedRoomsViewModel.swift/g; s/MyInfoUiState\.swift/JoinedRoomsUiState.swift/g; s/MyInfoAction\.swift/JoinedRoomsAction.swift/g; s/MyInfoEvent\.swift/JoinedRoomsEvent.swift/g' iosApp.xcodeproj/project.pbxproj
cd ..
```

- [ ] **Step 2: `JoinedRoomsView.swift` 정리**

1. 콜백을 3개만 남긴다: `onRequireSignIn`, `onOpenReport`, `onRejoin` (`onOpenCoinHistory`·`onOpenReputation`·`onOpenHostedRooms`·`onOpenEarnings`·`onOpenSettings`·`onBack` 삭제).
2. `onReceive` `switch`에서 `.openCoinHistory`·`.openReputation`·`.openHostedRooms`·`.openEarnings`·`.openSettings` 분기 삭제.
3. `JoinedRoomsContentView(uiState:onAction:onClickBack:)` 호출과 정의에서 `onClickBack` 제거.
4. 헤더 `HStack { Text("참여한 방"); Spacer(); Button("설정"); Button("닫기") }`를 `Text("참여한 방")` 한 줄(같은 폰트 수정자)로 교체.
5. 메뉴 행 4개(`HostedRoomsRow`/`EarningsRow`/`ReputationRow`/`CoinHistoryRow` 또는 그에 해당하는 `menuRow(...)` 호출과 정의) 삭제.
6. 하단 패딩을 `.padding(.bottom, 96)`으로.
7. 파일 맨 아래 `#Preview` 블록이 삭제한 콜백을 참조하면 콜백 없이 `JoinedRoomsView()`… 가 아니라 **콘텐츠 뷰 기준**으로 수정: `JoinedRoomsContentView(uiState: JoinedRoomsUiState(isLoading: false, summary: …, rooms: […]), onAction: { _ in })`. (기존 프리뷰의 샘플 데이터는 그대로 재사용.)

`JoinedRoomsAction.swift`에서 `clickCoinHistory`·`clickReputation`·`clickHostedRooms`·`clickEarnings`·`clickSettings` 삭제. `JoinedRoomsEvent.swift`에서 `openCoinHistory`·`openReputation`·`openHostedRooms`·`openEarnings`·`openSettings` 삭제. `JoinedRoomsViewModel.swift`의 `action(_:)` `switch`에서 해당 5개 `case`와 대응 private 메서드 삭제.

- [ ] **Step 3: 2차 rename `Settings*` → `MyInfo*`**

```bash
cd iosApp/iosApp/ui/mypage
for n in View ViewModel UiState Action Event; do git mv Settings$n.swift MyInfo$n.swift; done
sed -i '' 's/SettingsSheet/MyInfoSheet/g; s/SettingsNoticeToast/MyInfoNoticeToast/g; s/SettingsContentView/MyInfoContentView/g; s/SettingsView/MyInfoView/g; s/SettingsViewModel/MyInfoViewModel/g; s/SettingsUiState/MyInfoUiState/g; s/SettingsAction/MyInfoAction/g; s/SettingsEvent/MyInfoEvent/g' MyInfoView.swift MyInfoViewModel.swift MyInfoUiState.swift MyInfoAction.swift MyInfoEvent.swift
cd ../../..
sed -i '' 's/SettingsView\.swift/MyInfoView.swift/g; s/SettingsViewModel\.swift/MyInfoViewModel.swift/g; s/SettingsUiState\.swift/MyInfoUiState.swift/g; s/SettingsAction\.swift/MyInfoAction.swift/g; s/SettingsEvent\.swift/MyInfoEvent.swift/g' iosApp.xcodeproj/project.pbxproj
cd ..
grep -n "NotificationMyInfo" iosApp/iosApp/ui/mypage/MyInfo*.swift
```

마지막 grep에 결과가 있으면 `NotificationMyInfoSheetView`→`NotificationSettingsSheetView`로 되돌린다(`sed -i '' 's/NotificationMyInfo/NotificationSettings/g' MyInfoView.swift`). `MyInfoView.swift`의 헤더 주석 2줄을 `// Figma "UI 디자인 v6" M-12(349:9683) 미러 — 마이 탭 루트: 프로필·계정·코인·정산·알림·로그아웃`로 교체.

- [ ] **Step 4: 축소 `Settings*` 5파일 작성 (idx 150~154)**

`SettingsUiState.swift`:

```swift
import Foundation

struct SettingsUiState {
    // 탈퇴 요청 in-flight — 중복 호출 방지 (규칙 §9)
    var isProcessing: Bool = false
}
```

`SettingsAction.swift`:

```swift
import Foundation

enum SettingsAction {
    case enter

    // 확인 알림을 거친 뒤 호출된다 — 알림 소유는 화면 (규칙 §11-1)
    case confirmDeleteAccount
}
```

`SettingsEvent.swift`:

```swift
import Foundation

enum SettingsEvent {
    // 설정은 회원 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    case requireSignIn

    // 탈퇴 완료 → 홈으로 (세션 정리는 shared가 수행)
    case accountDeleted

    case showNotice(message: String)
}
```

`SettingsViewModel.swift`:

```swift
import Combine
import Foundation
import Shared

// Compose SettingsViewModel.kt 미러 — 회원 탈퇴(M-12-12)만
final class SettingsViewModel: ObservableObject {
    private let deleteAccountUseCase: DeleteAccountUseCase

    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = SettingsUiState()

    let event = PassthroughSubject<SettingsEvent, Never>()

    private func onEnter() {
        // 회원 전용 가드 — 서버 검증이 최종 권위 (규칙 §8)
        if !isSignedInUseCase.invoke() {
            event.send(.requireSignIn)
        }
    }

    private func onConfirmDeleteAccount() {
        if uiState.isProcessing {
            return
        }
        uiState.isProcessing = true
        deleteAccountUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isProcessing = false
                if error == nil, result is AppResultSuccess<AnyObject> {
                    self.event.send(.accountDeleted)
                } else {
                    let appError = (result as? AppResultFailure)?.error
                    self.event.send(.showNotice(message: self.deleteFailMessage(appError)))
                }
            }
        }
    }

    // 서버 code 기반 문구 분기 (규칙 §10) — 409=정산 미지급분·진행 중 방 거부
    private func deleteFailMessage(_ error: AppError?) -> String {
        if let conflict = error as? AppError.Conflict {
            return conflict.serverMessage ?? "정산 대기 금액이나 진행 중인 방이 있어 탈퇴할 수 없어요"
        } else if error is AppError.NetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return "탈퇴를 처리하지 못했어요. 다시 시도해 주세요"
        }
    }

    func action(_ action: SettingsAction) {
        switch action {
        case .enter:
            onEnter()
        case .confirmDeleteAccount:
            onConfirmDeleteAccount()
        }
    }

    init(deleteAccountUseCase: DeleteAccountUseCase, isSignedInUseCase: IsSignedInUseCase) {
        self.deleteAccountUseCase = deleteAccountUseCase
        self.isSignedInUseCase = isSignedInUseCase
    }
}
```

`SettingsView.swift`:

```swift
import SwiftUI
import Shared

// 설정 — 마이 탭 우상단 "설정"에서 push. 회원 탈퇴(M-12-12, 확인 알림)만 둔다
struct SettingsView: View {
    var onRequireSignIn: () -> Void = {}

    var onAccountDeleted: () -> Void = {}

    var onBack: () -> Void = {}

    @StateObject private var viewModel = SettingsViewModel(
        deleteAccountUseCase: KoinHelper.shared.deleteAccountUseCase(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    @State private var showDeleteConfirm = false

    @State private var noticeMessage: String?

    var body: some View {
        SettingsContentView(
            uiState: viewModel.uiState,
            onClickBack: onBack,
            onClickDelete: { showDeleteConfirm = true }
        )
        .onAppear {
            viewModel.action(.enter)
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .requireSignIn:
                onRequireSignIn()
            case .accountDeleted:
                onAccountDeleted()
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .alert("회원 탈퇴", isPresented: $showDeleteConfirm) {
            Button("탈퇴", role: .destructive) {
                viewModel.action(.confirmDeleteAccount)
            }
            Button("취소", role: .cancel) {}
        } message: {
            Text("탈퇴하면 참여·개설 기록과 보유 코인이 모두 삭제되고 되돌릴 수 없어요. 정산 대기 금액이나 진행 중인 방이 있으면 탈퇴할 수 없어요.")
        }
        .overlay(alignment: .bottom) {
            if let noticeMessage {
                SettingsNoticeToast(message: noticeMessage)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
    }
}

private struct SettingsNoticeToast: View {
    let message: String

    var body: some View {
        Text(message)
            .font(.system(size: 13))
            .foregroundColor(PassmateColors.surface)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(PassmateColors.textPrimary.opacity(0.9))
            .cornerRadius(10)
            .padding(.bottom, 16)
    }
}

private struct SettingsContentView: View {
    let uiState: SettingsUiState

    let onClickBack: () -> Void

    let onClickDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("설정")
                    .font(.system(size: 24, weight: .bold))
                    .kerning(-0.48)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
                Button(action: onClickBack) {
                    Text("닫기")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textSecondary)
                }
            }
            Button(action: onClickDelete) {
                HStack {
                    Text("회원 탈퇴")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(PassmateColors.weakTopicText)
                    Spacer()
                    Text("›").font(.system(size: 18)).foregroundColor(PassmateColors.textTertiary)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 16)
                .background(PassmateColors.surface)
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
            }
            if uiState.isProcessing {
                HStack {
                    Spacer()
                    ProgressView().tint(PassmateColors.primary)
                    Spacer()
                }
            }
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.top, 32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PassmateColors.surface.ignoresSafeArea())
    }
}

// MARK: - 프리뷰 (Figma 시안 비교용)
#Preview("설정") {
    SettingsContentView(uiState: SettingsUiState(), onClickBack: {}, onClickDelete: {})
}
```

pbxproj 등록(Task 9 Step 5와 같은 패턴, mypage 그룹): 150 SettingsUiState.swift · 151 SettingsAction.swift · 152 SettingsEvent.swift · 153 SettingsViewModel.swift · 154 SettingsView.swift. 중복 ID 0 확인.

- [ ] **Step 5: `ContentView.swift` 임시 배선(컴파일용)**

`case .myInfo:` 블록을 옛 `SettingsView(...)` 호출 형태의 `MyInfoView(onRequireSignIn:onOpenCoinHistory:onSignedOut:onBack:)`로, `case .settings:` 블록을 `SettingsView(onRequireSignIn: { path.append(.signIn) }, onAccountDeleted: { path = [] }, onBack: { popOnce() })`로 바꾼다. `case .joinedRooms:` 추가: `JoinedRoomsView(onRequireSignIn: { path.append(.signIn) }, onOpenReport: { roomId in path.append(.result(roomId: roomId)) }, onRejoin: { pin in path.append(.waiting(pin: pin)) })`.

- [ ] **Step 6: 빌드 확인 후 커밋**

Task 9 Step 6의 xcodebuild 명령 실행 → `BUILD SUCCEEDED`. `git checkout -- gradlew`.

```bash
git add -A iosApp
git commit -m "refactor(ios): 화면 재배치 미러 — MyInfoView*→JoinedRoomsView*(메뉴 행 제거), SettingsView*→MyInfoView*, 축소 SettingsView*(회원 탈퇴) + pbxproj idx 150~154

Claude-Session: https://claude.ai/code/session_01EQVuB7AawaJr3kvsfXy3xN"
```

---

### Task 11: iOS 마이 탭 (M-12) — `MyInfoViewModel.swift`·`MyInfoView.swift` 확장

**Files:**
- Modify: `ui/mypage/MyInfoUiState.swift`, `MyInfoAction.swift`, `MyInfoEvent.swift`, `MyInfoViewModel.swift`, `MyInfoView.swift` (전체 교체)
- Modify: `component/StudentAvatar.swift` (`nameOf` 추가)

**Interfaces:**
- Consumes: Task 4 Kotlin 정의(1:1 미러), `EditProfileSheetView`·`PaymentMethodSheetView`·`NotificationSettingsSheetView`·`SettlementAccountSheetView`, `StudentAvatarView(avatarId:)`, `ReputationBadgeView(level:)`, `HostLevel.from(_:)`
- Produces: `MyInfoView(onRequireSignIn:onOpenReputation:onOpenCoinHistory:onOpenEarnings:onOpenSettings:onSignedOut:)`

- [ ] **Step 1: `StudentAvatars.nameOf` 추가 (Swift)**

`component/StudentAvatar.swift`의 `enum StudentAvatars` 안에 추가:

```swift
    // Compose StudentAvatars.nameOf 미러 — 마이 탭 "내 캐릭터" 부제
    static func nameOf(_ avatarId: Int) -> String {
        let names = ["고양이", "강아지", "곰", "판다", "토끼", "여우", "개구리", "펭귄", "부엉이", "호랑이", "너구리", "공룡"]
        let resolved = (avatarId >= 1 && avatarId <= count) ? avatarId : defaultId
        return names[resolved - 1]
    }
```

- [ ] **Step 2: UiState/Action/Event 교체**

`MyInfoUiState.swift`:

```swift
import Foundation
import Shared

// 마이 탭 루트 (M-12). 프로필 실패 = 전체 에러, 코인·정산 실패 = 해당 카드만 (규칙 §9)
struct MyInfoUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    var profile: UserProfile?

    var defaultMethod: PaymentMethod?

    var recentTransaction: CoinTransaction?

    var isCoinInfoFailed: Bool = false

    var settlementAccount: SettlementAccountSummary?

    var nextPayout: NextPayout?

    var isEarningsFailed: Bool = false

    // 로그아웃 요청 in-flight — 중복 호출 방지 (규칙 §9)
    var isProcessing: Bool = false
}
```

`MyInfoAction.swift`:

```swift
import Foundation

enum MyInfoAction {
    case enter
    case retry
    // 프로필 카드 탭 → 내 명성·뱃지 (M-09)
    case clickProfile
    // 닉네임·내 캐릭터 변경 시트 (M-12-1·M-12-7)
    case clickEditProfile
    // 코인 충전 (M-12-4~6) — 전용 화면은 후속 작업, 지금은 안내만
    case clickCharge
    case clickPaymentMethod
    case clickCoinHistory
    case clickSettlementAccount
    case clickEarnings
    case clickNotifications
    case clickSettings
    // 확인 알림을 거친 뒤 호출된다 — 알림 소유는 화면 (규칙 §11-1)
    case confirmSignOut
    // 시트 저장 완료 — 해당 섹션만 다시 불러온다
    case profileUpdated
    case paymentMethodUpdated
    case accountUpdated
    case notice(message: String)
}
```

`MyInfoEvent.swift`:

```swift
import Foundation

enum MyInfoEvent {
    // 마이는 회원 전용 — 딥링크 직접 진입 대비 보험 (탭 가드는 AppShellViewModel, 규칙 §8)
    case requireSignIn
    case openReputation
    case openEditProfile(nickname: String, avatarId: Int?)
    case openPaymentMethod
    case openCoinHistory
    case openSettlementAccount
    case openEarnings
    case openNotifications
    case openSettings
    // 로그아웃 완료 → 홈 탭으로 (세션 정리는 shared가 수행)
    case signedOut
    case showNotice(message: String)
}
```

- [ ] **Step 3: `MyInfoViewModel.swift` 전체 교체**

```swift
import Combine
import Foundation
import Shared

// Compose MyInfoViewModel.kt 미러 — 마이 탭 루트 (M-12): 프로필·코인·정산 3섹션 독립 로드
final class MyInfoViewModel: ObservableObject {
    private let getMyProfileUseCase: GetMyProfileUseCase

    private let getMyCoinsUseCase: GetMyCoinsUseCase

    private let getEarningsUseCase: GetEarningsUseCase

    private let signOutUseCase: SignOutUseCase

    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = MyInfoUiState()

    let event = PassthroughSubject<MyInfoEvent, Never>()

    private var hasEntered = false

    private func onEnter() {
        if hasEntered {
            return
        }
        hasEntered = true
        // 회원 전용 가드 — 서버 검증이 최종 권위 (규칙 §8)
        if isSignedInUseCase.invoke() {
            loadAll()
        } else {
            event.send(.requireSignIn)
        }
    }

    private func loadAll() {
        loadProfile()
        loadCoinInfo()
        loadEarnings()
    }

    private func loadProfile() {
        uiState.isLoading = true
        uiState.loadFailed = false
        getMyProfileUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let profile = (result as? AppResultSuccess<AnyObject>)?.value as? UserProfile
                self.uiState.isLoading = false
                if error == nil, let profile {
                    self.uiState.loadFailed = false
                    self.uiState.profile = profile
                } else {
                    self.uiState.loadFailed = true
                }
            }
        }
    }

    private func loadCoinInfo() {
        getMyCoinsUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let coins = (result as? AppResultSuccess<AnyObject>)?.value as? CoinBalance
                if error == nil, let coins {
                    self.uiState.defaultMethod = coins.defaultMethod
                    self.uiState.recentTransaction = coins.recent
                    self.uiState.isCoinInfoFailed = false
                } else {
                    self.uiState.isCoinInfoFailed = true
                }
            }
        }
    }

    private func loadEarnings() {
        getEarningsUseCase.invoke(cursor: nil) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let earnings = (result as? AppResultSuccess<AnyObject>)?.value as? Earnings
                if error == nil, let earnings {
                    self.uiState.settlementAccount = earnings.account
                    self.uiState.nextPayout = earnings.nextPayout
                    self.uiState.isEarningsFailed = false
                } else {
                    self.uiState.isEarningsFailed = true
                }
            }
        }
    }

    private func onClickEditProfile() {
        if let profile = uiState.profile {
            event.send(.openEditProfile(
                nickname: profile.nickname,
                avatarId: profile.avatarId.map { Int(truncating: $0) }
            ))
        }
    }

    private func onConfirmSignOut() {
        if uiState.isProcessing {
            return
        }
        uiState.isProcessing = true
        // 로컬 세션 정리는 shared가 항상 수행 — 실패 케이스 없음 (M-12-11)
        signOutUseCase.invoke { [weak self] _, _ in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isProcessing = false
                self.event.send(.signedOut)
            }
        }
    }

    func action(_ action: MyInfoAction) {
        switch action {
        case .enter:
            onEnter()
        case .retry:
            loadAll()
        case .clickProfile:
            event.send(.openReputation)
        case .clickEditProfile:
            onClickEditProfile()
        case .clickCharge:
            event.send(.showNotice(message: "코인 충전은 준비 중이에요"))
        case .clickPaymentMethod:
            event.send(.openPaymentMethod)
        case .clickCoinHistory:
            event.send(.openCoinHistory)
        case .clickSettlementAccount:
            event.send(.openSettlementAccount)
        case .clickEarnings:
            event.send(.openEarnings)
        case .clickNotifications:
            event.send(.openNotifications)
        case .clickSettings:
            event.send(.openSettings)
        case .confirmSignOut:
            onConfirmSignOut()
        case .profileUpdated:
            loadProfile()
            event.send(.showNotice(message: "내 정보를 저장했어요"))
        case .paymentMethodUpdated:
            loadCoinInfo()
            event.send(.showNotice(message: "기본 결제 수단을 저장했어요"))
        case .accountUpdated:
            loadEarnings()
            event.send(.showNotice(message: "정산 계좌를 저장했어요"))
        case let .notice(message):
            event.send(.showNotice(message: message))
        }
    }

    init(
        getMyProfileUseCase: GetMyProfileUseCase,
        getMyCoinsUseCase: GetMyCoinsUseCase,
        getEarningsUseCase: GetEarningsUseCase,
        signOutUseCase: SignOutUseCase,
        isSignedInUseCase: IsSignedInUseCase
    ) {
        self.getMyProfileUseCase = getMyProfileUseCase
        self.getMyCoinsUseCase = getMyCoinsUseCase
        self.getEarningsUseCase = getEarningsUseCase
        self.signOutUseCase = signOutUseCase
        self.isSignedInUseCase = isSignedInUseCase
    }
}
```

인터롭 주의: Kotlin `Long?`→`KotlinLong?`(`.int64Value`), `Int?`→`KotlinInt?`(`Int(truncating:)`), `AppError.Conflict`처럼 중첩 클래스 표기, `PagedResult<AnyObject>`. 빌드 오류가 나면 `fix/ios-build` 커밋 `203aa16`의 패턴을 따른다.

- [ ] **Step 4: `MyInfoView.swift` 전체 교체**

```swift
import SwiftUI
import Shared

// 시트 4종 중 무엇이 열려 있는지 — 표시 여부는 이 화면이 소유한다 (규칙 §11-1)
private enum MyInfoSheet: Identifiable {
    case editProfile(nickname: String, avatarId: Int?)
    case paymentMethod
    case settlementAccount
    case notifications

    var id: String {
        switch self {
        case .editProfile: return "editProfile"
        case .paymentMethod: return "paymentMethod"
        case .settlementAccount: return "settlementAccount"
        case .notifications: return "notifications"
        }
    }
}

// Figma "UI 디자인 v6" M-12(349:9683) 미러 — 마이 탭 루트: 프로필·계정·코인·정산·알림·로그아웃
struct MyInfoView: View {
    var onRequireSignIn: () -> Void = {}

    var onOpenReputation: () -> Void = {}

    var onOpenCoinHistory: () -> Void = {}

    var onOpenEarnings: () -> Void = {}

    var onOpenSettings: () -> Void = {}

    var onSignedOut: () -> Void = {}

    @StateObject private var viewModel = MyInfoViewModel(
        getMyProfileUseCase: KoinHelper.shared.getMyProfileUseCase(),
        getMyCoinsUseCase: KoinHelper.shared.getMyCoinsUseCase(),
        getEarningsUseCase: KoinHelper.shared.getEarningsUseCase(),
        signOutUseCase: KoinHelper.shared.signOutUseCase(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    @State private var activeSheet: MyInfoSheet?

    @State private var showSignOutConfirm = false

    @State private var noticeMessage: String?

    var body: some View {
        MyInfoContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onClickSignOut: { showSignOutConfirm = true }
        )
        .onAppear {
            viewModel.action(.enter)
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .requireSignIn:
                onRequireSignIn()
            case .openReputation:
                onOpenReputation()
            case let .openEditProfile(nickname, avatarId):
                activeSheet = .editProfile(nickname: nickname, avatarId: avatarId)
            case .openPaymentMethod:
                activeSheet = .paymentMethod
            case .openCoinHistory:
                onOpenCoinHistory()
            case .openSettlementAccount:
                activeSheet = .settlementAccount
            case .openEarnings:
                onOpenEarnings()
            case .openNotifications:
                activeSheet = .notifications
            case .openSettings:
                onOpenSettings()
            case .signedOut:
                onSignedOut()
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .sheet(item: $activeSheet) { sheet in
            sheetContent(sheet)
                .presentationDetents([.medium, .large])
        }
        .alert("로그아웃 할까요?", isPresented: $showSignOutConfirm) {
            Button("로그아웃", role: .destructive) {
                viewModel.action(.confirmSignOut)
            }
            Button("취소", role: .cancel) {}
        } message: {
            Text("다시 로그인하면 기록과 코인은 그대로 있어요.")
        }
        .overlay(alignment: .bottom) {
            if let noticeMessage {
                MyInfoNoticeToast(message: noticeMessage)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
    }

    @ViewBuilder
    private func sheetContent(_ sheet: MyInfoSheet) -> some View {
        switch sheet {
        case let .editProfile(nickname, avatarId):
            EditProfileSheetView(
                initialNickname: nickname,
                initialAvatarId: avatarId,
                onSaved: {
                    activeSheet = nil
                    viewModel.action(.profileUpdated)
                },
                onNotice: { viewModel.action(.notice(message: $0)) },
                onClose: { activeSheet = nil }
            )
        case .paymentMethod:
            PaymentMethodSheetView(
                onSaved: {
                    activeSheet = nil
                    viewModel.action(.paymentMethodUpdated)
                },
                onNotice: { viewModel.action(.notice(message: $0)) },
                onClose: { activeSheet = nil }
            )
        case .settlementAccount:
            SettlementAccountSheetView(
                onSaved: {
                    activeSheet = nil
                    viewModel.action(.accountUpdated)
                },
                onNotice: { viewModel.action(.notice(message: $0)) },
                onClose: { activeSheet = nil }
            )
        case .notifications:
            NotificationSettingsSheetView(
                onNotice: { viewModel.action(.notice(message: $0)) },
                onClose: { activeSheet = nil }
            )
        }
    }
}

private struct MyInfoNoticeToast: View {
    let message: String

    var body: some View {
        Text(message)
            .font(.system(size: 13))
            .foregroundColor(PassmateColors.surface)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(PassmateColors.textPrimary.opacity(0.9))
            .cornerRadius(10)
            .padding(.bottom, 16)
    }
}

private struct MyInfoContentView: View {
    let uiState: MyInfoUiState

    let onAction: (MyInfoAction) -> Void

    let onClickSignOut: () -> Void

    var body: some View {
        Group {
            if uiState.isLoading {
                ProgressView()
                    .tint(PassmateColors.primary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if uiState.loadFailed {
                errorView
            } else {
                loadedView
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private var errorView: some View {
        VStack(spacing: 12) {
            Text("내 정보를 불러오지 못했어요")
                .font(.system(size: 16, weight: .medium))
                .kerning(-0.32)
                .foregroundColor(PassmateColors.textPrimary)
            Button {
                onAction(.retry)
            } label: {
                Text("다시 시도")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var loadedView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    Text("마이")
                        .font(.system(size: 24, weight: .bold))
                        .kerning(-0.48)
                        .foregroundColor(PassmateColors.textPrimary)
                    Spacer()
                    Button(action: { onAction(.clickSettings) }) {
                        Text("설정")
                            .font(.system(size: 14, weight: .medium))
                            .kerning(-0.28)
                            .foregroundColor(PassmateColors.textSecondary)
                    }
                }
                if let profile = uiState.profile {
                    ProfileCardView(profile: profile, onClick: { onAction(.clickProfile) })
                    sectionCard {
                        infoRow(title: "닉네임", subtitle: profile.nickname, actionLabel: "변경") { onAction(.clickEditProfile) }
                        rowDivider
                        infoRow(title: "내 캐릭터", subtitle: avatarName(profile), actionLabel: "변경") { onAction(.clickEditProfile) }
                    }
                    sectionCard {
                        coinRow(coins: profile.coins?.int64Value ?? 0) { onAction(.clickCharge) }
                        rowDivider
                        infoRow(title: "결제 수단", subtitle: paymentMethodSubtitle, actionLabel: "관리") { onAction(.clickPaymentMethod) }
                        rowDivider
                        infoRow(title: "코인 내역", subtitle: recentTransactionSubtitle, actionLabel: "보기") { onAction(.clickCoinHistory) }
                    }
                    sectionCard {
                        infoRow(title: "정산 계좌", subtitle: settlementAccountSubtitle, actionLabel: "변경") { onAction(.clickSettlementAccount) }
                        rowDivider
                        infoRow(title: "이번 달 정산 예정", subtitle: nextPayoutSubtitle, actionLabel: "내역") { onAction(.clickEarnings) }
                    }
                    sectionCard {
                        infoRow(title: "알림", subtitle: "세션 시작 · 별점 요청 · 정산", actionLabel: "설정") { onAction(.clickNotifications) }
                    }
                }
                signOutButton
            }
            .padding(.horizontal, 20)
            .padding(.top, 32)
            .padding(.bottom, 24)
        }
    }

    private var rowDivider: some View {
        Rectangle()
            .fill(PassmateColors.border)
            .frame(height: 1)
    }

    private var signOutButton: some View {
        Button(action: onClickSignOut) {
            HStack {
                Spacer()
                if uiState.isProcessing {
                    ProgressView().tint(PassmateColors.primary)
                } else {
                    Text("로그아웃")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(PassmateColors.textSecondary)
                }
                Spacer()
            }
            .padding(.vertical, 16)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
        }
        .disabled(uiState.isProcessing)
        .padding(.top, 10)
    }

    private var paymentMethodSubtitle: String {
        if uiState.isCoinInfoFailed {
            return "불러오지 못했어요"
        } else if let method = uiState.defaultMethod {
            return "\(method.label) · 포트원 안전결제"
        } else {
            return "기본 결제 수단을 설정해 주세요"
        }
    }

    private var recentTransactionSubtitle: String {
        if uiState.isCoinInfoFailed {
            return "불러오지 못했어요"
        } else if let recent = uiState.recentTransaction {
            return "최근 \(shortDate(recent.createdAt)) \(signedCoins(Int(recent.amount))) C"
        } else {
            return "아직 내역이 없어요"
        }
    }

    private var settlementAccountSubtitle: String {
        if uiState.isEarningsFailed {
            return "불러오지 못했어요"
        } else if let account = uiState.settlementAccount {
            return "\(account.bankName) \(account.maskedNumber)"
        } else {
            return "계좌를 등록해 주세요"
        }
    }

    private var nextPayoutSubtitle: String {
        if uiState.isEarningsFailed {
            return "불러오지 못했어요"
        } else if let payout = uiState.nextPayout {
            return "₩\(formatNumber(payout.amount)) · \(payout.dateLabel) 지급"
        } else {
            return "정산 예정 없음"
        }
    }

    private func avatarName(_ profile: UserProfile) -> String {
        return StudentAvatars.nameOf(profile.avatarId.map { Int(truncating: $0) } ?? StudentAvatars.defaultId)
    }

    private func sectionCard<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(spacing: 0) {
            content()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
    }

    private func infoRow(title: String, subtitle: String, actionLabel: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 15, weight: .medium))
                        .kerning(-0.3)
                        .foregroundColor(PassmateColors.textPrimary)
                    Text(subtitle)
                        .font(.system(size: 13))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.textSecondary)
                }
                Spacer()
                Text("\(actionLabel) ›")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
            .padding(.vertical, 14)
        }
    }

    private func coinRow(coins: Int64, onClickCharge: @escaping () -> Void) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("보유 코인")
                    .font(.system(size: 15, weight: .medium))
                    .kerning(-0.3)
                    .foregroundColor(PassmateColors.textPrimary)
                Text("\(formatNumber(coins)) C · 유료 방 참가비에 사용")
                    .font(.system(size: 13))
                    .kerning(-0.26)
                    .foregroundColor(PassmateColors.textSecondary)
            }
            Spacer()
            Button(action: onClickCharge) {
                Text("코인 충전")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(PassmateColors.surface)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(PassmateColors.primary)
                    .cornerRadius(12)
            }
        }
        .padding(.vertical, 14)
    }

    // "2026-08-22T10:00:00Z" → "8/22"
    private func shortDate(_ raw: String?) -> String {
        guard let raw else { return "" }
        let parts = raw.prefix(10).split(separator: "-")
        if parts.count == 3 {
            let month = Int(parts[1]).map(String.init) ?? String(parts[1])
            let day = Int(parts[2]).map(String.init) ?? String(parts[2])
            return "\(month)/\(day)"
        } else {
            return String(raw.prefix(10))
        }
    }

    private func signedCoins(_ amount: Int) -> String {
        if amount > 0 {
            return "+\(formatNumber(Int64(amount)))"
        } else {
            return "-\(formatNumber(Int64(-amount)))"
        }
    }

    private func formatNumber(_ value: Int64) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        return formatter.string(from: NSNumber(value: value)) ?? "\(value)"
    }
}

private struct ProfileCardView: View {
    let profile: UserProfile

    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 14) {
                StudentAvatarView(avatarId: profile.avatarId.map { Int(truncating: $0) } ?? 0)
                    .frame(width: 52, height: 52)
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Text(profile.nickname)
                            .font(.system(size: 18, weight: .bold))
                            .kerning(-0.36)
                            .foregroundColor(PassmateColors.textPrimary)
                        if let level = localLevel {
                            ReputationBadgeView(level: level)
                        }
                    }
                    Text("참여한 방 \(profile.joinedRoomCount?.intValue ?? 0) · 내가 만든 방 \(profile.hostedRoomCount?.intValue ?? 0)")
                        .font(.system(size: 13))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.textSecondary)
                }
                Spacer()
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 16)
            .background(PassmateColors.surface)
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(PassmateColors.border, lineWidth: 1))
        }
    }

    private var localLevel: HostLevel? {
        guard let level = profile.level else { return nil }
        return HostLevel.from(Int(level.level))
    }
}

// MARK: - 프리뷰 (Figma 시안 비교용)
#Preview("마이 — 로드 완료") {
    MyInfoContentView(
        uiState: MyInfoUiState(
            isLoading: false,
            profile: UserProfile(
                nickname: "준영",
                email: "junyoung@example.com",
                joinedAt: "2026-08-01",
                avatarId: KotlinInt(int: 1),
                level: Shared.HostLevel.growing,
                coins: KotlinLong(value: 1200),
                joinedRoomCount: KotlinInt(int: 32),
                hostedRoomCount: KotlinInt(int: 12)
            ),
            defaultMethod: PaymentMethod.kakaoPay,
            settlementAccount: SettlementAccountSummary(bankName: "국민", maskedNumber: "***-***-4821", payoutNote: nil),
            nextPayout: NextPayout(dateLabel: "9/5", amount: 64000)
        ),
        onAction: { _ in },
        onClickSignOut: {}
    )
}

#Preview("마이 — 코인·정산 실패") {
    MyInfoContentView(
        uiState: MyInfoUiState(
            isLoading: false,
            profile: UserProfile(
                nickname: "준영", email: nil, joinedAt: nil, avatarId: KotlinInt(int: 1),
                level: nil, coins: nil, joinedRoomCount: nil, hostedRoomCount: nil
            ),
            isCoinInfoFailed: true,
            isEarningsFailed: true
        ),
        onAction: { _ in },
        onClickSignOut: {}
    )
}
```

프리뷰의 Kotlin enum 케이스 이름(`PaymentMethod.kakaoPay`, `Shared.HostLevel.growing`)은 빌드 오류가 나면 Xcode 자동완성으로 실제 노출 이름(소문자 카멜)에 맞춘다.

- [ ] **Step 5: `ContentView.swift`의 `.myInfo` 배선을 새 콜백으로 갱신**

```swift
        case .myInfo:
            MyInfoView(
                onRequireSignIn: { path.append(.signIn) },
                onOpenReputation: { path.append(.reputation) },
                onOpenCoinHistory: { path.append(.coinHistory) },
                onOpenEarnings: { path.append(.earnings) },
                onOpenSettings: { path.append(.settings) },
                onSignedOut: { path = [] }
            )
```

- [ ] **Step 6: 빌드 확인 후 커밋**

Task 9 Step 6의 xcodebuild → `BUILD SUCCEEDED`. `git checkout -- gradlew`.

```bash
git add -A iosApp
git commit -m "feat(ios): 마이 탭 미러(M-12) — 프로필·계정·코인·정산·알림 카드, 시트 4종, 로그아웃 확인 + 프리뷰 2개

Claude-Session: https://claude.ai/code/session_01EQVuB7AawaJr3kvsfXy3xN"
```

---

### Task 12: iOS `ContentView` 탭 셸 + `Home*` 삭제 + 시뮬레이터 스모크

**Files:**
- Modify: `iosApp/iosApp/ContentView.swift` (전체 교체)
- Modify: `ui/hostroom/HostedRoomsView.swift` (닫기 제거)
- Delete: `ui/home/HomeView.swift`, `HomeViewModel.swift`, `HomeUiState.swift`, `HomeAction.swift` (+ pbxproj 참조 4×3곳)

- [ ] **Step 1: `ContentView.swift` 전체 교체**

```swift
import SwiftUI
import Shared

// 하단 4탭 셸 — 탭마다 NavigationStack, 탭 루트에서만 탭 바 표시 (규칙 §2-1, 스펙 §1-4).
// 앱 시작 기본 진입은 항상 Home(게스트 포함, 규칙 §2-1-1)
struct ContentView: View {
    @StateObject private var shellViewModel = AppShellViewModel(
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    @State private var selectedTab: AppTab = .home

    @State private var homePath: [Route] = []

    @State private var hostedRoomsPath: [Route] = []

    @State private var joinedRoomsPath: [Route] = []

    @State private var myInfoPath: [Route] = []

    var body: some View {
        TabView(selection: tabSelection) {
            tabStack(path: $homePath) {
                // 홈 탭 = 입장 폼 인라인 (M-01 v6) — JoinView 재사용
                JoinView(
                    initialPin: nil,
                    onJoined: { pin in homePath.append(.waiting(pin: pin)) },
                    onPaymentRequired: { pin in homePath.append(.payment(pin: pin)) },
                    onSignInRequested: { homePath.append(.signIn) }
                )
            }
            .tabItem { Label(AppTab.home.label, systemImage: AppTab.home.systemImage) }
            .tag(AppTab.home)

            tabStack(path: $hostedRoomsPath) {
                HostedRoomsView(
                    onRequireSignIn: { hostedRoomsPath.append(.signIn) },
                    onOpenReputation: { hostedRoomsPath.append(.reputation) },
                    onOpenRoomReport: { roomId in hostedRoomsPath.append(.roomReport(roomId: roomId)) },
                    onOpenSessionControl: { roomId, pin in hostedRoomsPath.append(.sessionControl(roomId: roomId, pin: pin)) }
                )
            }
            .tabItem { Label(AppTab.hostedRooms.label, systemImage: AppTab.hostedRooms.systemImage) }
            .tag(AppTab.hostedRooms)

            tabStack(path: $joinedRoomsPath) {
                JoinedRoomsView(
                    onRequireSignIn: { joinedRoomsPath.append(.signIn) },
                    onOpenReport: { roomId in joinedRoomsPath.append(.result(roomId: roomId)) },
                    onRejoin: { pin in joinedRoomsPath.append(.waiting(pin: pin)) }
                )
            }
            .tabItem { Label(AppTab.joinedRooms.label, systemImage: AppTab.joinedRooms.systemImage) }
            .tag(AppTab.joinedRooms)

            tabStack(path: $myInfoPath) {
                MyInfoView(
                    onRequireSignIn: { myInfoPath.append(.signIn) },
                    onOpenReputation: { myInfoPath.append(.reputation) },
                    onOpenCoinHistory: { myInfoPath.append(.coinHistory) },
                    onOpenEarnings: { myInfoPath.append(.earnings) },
                    onOpenSettings: { myInfoPath.append(.settings) },
                    onSignedOut: {
                        myInfoPath = []
                        selectedTab = .home
                    }
                )
            }
            .tabItem { Label(AppTab.myInfo.label, systemImage: AppTab.myInfo.systemImage) }
            .tag(AppTab.myInfo)
        }
        .tint(PassmateColors.primary)
        .onReceive(shellViewModel.event) { event in
            switch event {
            case let .navigateToTab(tab):
                selectedTab = tab
            case .requireSignIn:
                // 현재 탭 스택 위에 로그인 push — 로그인 후 pendingRoute 복귀는 후속 작업
                currentPath.wrappedValue.append(.signIn)
            }
        }
    }

    // 탭 선택은 셸 가드를 거친다 — 게스트의 로그인 필수 탭은 SignIn으로 (결정 2)
    private var tabSelection: Binding<AppTab> {
        Binding(
            get: { selectedTab },
            set: { shellViewModel.action(.selectTab($0)) }
        )
    }

    private var currentPath: Binding<[Route]> {
        switch selectedTab {
        case .home: return $homePath
        case .hostedRooms: return $hostedRoomsPath
        case .joinedRooms: return $joinedRoomsPath
        case .myInfo: return $myInfoPath
        }
    }

    private func tabStack<Root: View>(path: Binding<[Route]>, @ViewBuilder root: () -> Root) -> some View {
        NavigationStack(path: path) {
            root()
                .navigationDestination(for: Route.self) { route in
                    destinationView(for: route, path: path)
                        .navigationBarBackButtonHidden(true)
                        .toolbar(.hidden, for: .tabBar)
                }
        }
    }

    @ViewBuilder
    private func destinationView(for route: Route, path: Binding<[Route]>) -> some View {
        switch route {
        case .home, .hostedRooms, .joinedRooms, .myInfo:
            // 탭 루트는 push 대상이 아니다 — 방어적으로 빈 뷰
            EmptyView()
        case .roomList:
            RoomListView(
                onOpenRoom: { pin in path.wrappedValue.append(.join(pin: pin)) },
                onOpenPinEntry: { path.wrappedValue = [] },
                onRequireSignIn: { path.wrappedValue.append(.signIn) }
            )
        case .signIn:
            SignInView(
                onSignedIn: { path.wrappedValue = [] },
                onGuestEnter: { path.wrappedValue = [] }
            )
        case let .join(pin):
            JoinView(
                initialPin: pin,
                onJoined: { pin in path.wrappedValue.append(.waiting(pin: pin)) },
                onPaymentRequired: { pin in path.wrappedValue.append(.payment(pin: pin)) },
                onSignInRequested: { path.wrappedValue.append(.signIn) },
                onBack: { popOnce(path) }
            )
        case let .payment(pin):
            PaymentView(
                pin: pin,
                onEnterRoom: { pin in path.wrappedValue.append(.waiting(pin: pin)) },
                onSignInRequired: { path.wrappedValue.append(.signIn) },
                onBack: { popOnce(path) }
            )
        case .coinHistory:
            CoinHistoryView(onBack: { popOnce(path) })
        case let .waiting(pin):
            WaitingView(
                pin: pin,
                onSessionStarted: { pin in path.wrappedValue.append(.play(pin: pin)) },
                onRoomClosed: { path.wrappedValue = [] },
                onLeft: { popOnce(path) }
            )
        case let .play(pin):
            PlayView(
                pin: pin,
                onLeft: { path.wrappedValue = [] },
                onRoomClosed: { path.wrappedValue = [] },
                onOpenResult: { roomId in
                    // 세션 플로우 엔트리(Join·Waiting·Play)만 제거, 탭 루트 유지 (규칙 §2-1-2, 스펙 §1-5)
                    path.wrappedValue.removeAll { $0.isSessionRoute }
                    path.wrappedValue.append(.result(roomId: roomId))
                },
                onOpenSignup: { path.wrappedValue.append(.signIn) }
            )
        case let .result(roomId):
            ResultView(
                roomId: roomId,
                onClickHome: { path.wrappedValue = [] },
                onNavigateToSignup: { path.wrappedValue.append(.signIn) }
            )
        case .reputation:
            ReputationView(
                onRequireSignIn: { path.wrappedValue.append(.signIn) },
                onBack: { popOnce(path) }
            )
        case .earnings:
            EarningsView(
                onRequireSignIn: { path.wrappedValue.append(.signIn) },
                onBack: { popOnce(path) }
            )
        case let .sessionControl(roomId, pin):
            SessionControlView(
                roomId: roomId,
                pin: pin,
                onRequireSignIn: { path.wrappedValue.append(.signIn) },
                onSessionEnded: { roomId in path.wrappedValue.append(.roomReport(roomId: roomId)) },
                onBack: { popOnce(path) }
            )
        case let .roomReport(roomId):
            RoomReportView(
                roomId: roomId,
                onRequireSignIn: { path.wrappedValue.append(.signIn) },
                onBack: { popOnce(path) }
            )
        case .settings:
            SettingsView(
                onRequireSignIn: { path.wrappedValue.append(.signIn) },
                onAccountDeleted: {
                    path.wrappedValue = []
                    selectedTab = .home
                },
                onBack: { popOnce(path) }
            )
        }
    }

    private func popOnce(_ path: Binding<[Route]>) {
        if !path.wrappedValue.isEmpty {
            path.wrappedValue.removeLast()
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
```

기존 `ContentView`의 `destinationView` 각 case 호출 인자는 현재 파일 내용을 기준으로 옮긴다(위는 develop `44df2f3` 기준 — `RoomReportView`·`SessionControlView` 등 인자가 다르면 기존 것을 유지하고 `path.append` → `path.wrappedValue.append`, `path = []` → `path.wrappedValue = []`, `popOnce()` → `popOnce(path)`로만 바꾼다).

- [ ] **Step 2: `HostedRoomsView.swift` 닫기 제거**

`onBack` 프로퍼티, `HostedRoomsContentView`의 `onClickBack` 파라미터·전달, 헤더 `HStack { Text("내가 만든 방"); Spacer(); Button("닫기") }` → `Text("내가 만든 방")` 한 줄. `#Preview`가 `onBack`을 쓰면 제거.

- [ ] **Step 3: `Home*` 4파일 삭제 + pbxproj 정리**

```bash
git rm iosApp/iosApp/ui/home/HomeView.swift iosApp/iosApp/ui/home/HomeViewModel.swift iosApp/iosApp/ui/home/HomeUiState.swift iosApp/iosApp/ui/home/HomeAction.swift
sed -i '' '/Home\(View\|ViewModel\|UiState\|Action\)\.swift/d' iosApp/iosApp.xcodeproj/project.pbxproj
grep -c "HomeView\|HomeViewModel\|HomeUiState\|HomeAction" iosApp/iosApp.xcodeproj/project.pbxproj   # → 0
grep -rn "HomeView\b\|HomeViewModel\|HomeUiState\|HomeAction" iosApp/iosApp   # → 0건
```

(`sed`의 `\|`가 macOS에서 안 먹으면 `-E`와 `Home(View|ViewModel|UiState|Action)\.swift`로.)

- [ ] **Step 4: 빌드 + 시뮬레이터 스모크**

```bash
cd iosApp && xcodebuild -project $PWD/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination "platform=iOS Simulator,id=4EA9F2CB-461B-4B4A-9977-1DC38372DD99" \
  -derivedDataPath $PWD/build/DerivedData CODE_SIGNING_ALLOWED=NO build 2>&1 | grep -E "error:|BUILD (SUCCEEDED|FAILED)"
xcrun simctl boot 4EA9F2CB-461B-4B4A-9977-1DC38372DD99 2>/dev/null
xcrun simctl install 4EA9F2CB-461B-4B4A-9977-1DC38372DD99 build/DerivedData/Build/Products/Debug-iphonesimulator/Passmate.app
xcrun simctl launch 4EA9F2CB-461B-4B4A-9977-1DC38372DD99 org.sesacteamproject.passmate.Passmate
sleep 4; xcrun simctl io 4EA9F2CB-461B-4B4A-9977-1DC38372DD99 screenshot ../home-tabs.png
cd .. && git checkout -- gradlew
```

확인(스크린샷 + 시뮬레이터 조작): (1) 첫 화면 = 입장 폼 + 하단 탭 4개, (2) "마이" 탭 → 로그인 화면 push(탭 바 숨김), 뒤로(닫기) → 홈 탭 유지, (3) 홈 탭에서 "PIN 입장" 시도 → 네트워크 오류 토스트(백엔드 없음).

- [ ] **Step 5: 커밋**

```bash
git add -A iosApp
git commit -m "feat(ios): 하단 4탭 셸 — TabView+탭별 NavigationStack, 셸 가드 배선, 홈=JoinView, Result 진입 시 세션 엔트리만 제거, HostedRooms 탭 루트화, 임시 Home* 삭제

Claude-Session: https://claude.ai/code/session_01EQVuB7AawaJr3kvsfXy3xN"
```

---

### Task 13: 문서·스펙 정리 + 최종 검증

**Files:**
- Modify: `docs/Passmate_코드_패턴_규칙.md` §2-1-1·§2-1-2
- Modify: `docs/Passmate_Mac_검증_체크리스트.md` (§9 추가)
- Modify: `docs/superpowers/specs/2026-08-30-home-shell-tabs-design.md` (구현 중 조정 3건)

- [ ] **Step 1: 규칙 문서 §2-1-1 갱신**

`- 루트 라우트: \`Home\`, \`SignIn\`, …` 줄 아래에 추가:

```markdown
- 하단 탭 루트(피그마 v6, 2026-08-30): `Home`(홈=입장 폼 인라인) · `HostedRooms`(내가 만든 방) · `JoinedRooms`(참여한 방) · `MyInfo`(마이). 탭 바는 이 4개 루트에서만 표시하고 push된 화면에서는 숨긴다. 게스트가 로그인 필수 탭(`HostedRooms`·`JoinedRooms`·`MyInfo`)을 누르면 화면을 열지 않고 `SignIn`으로 보낸다(판단은 셸 `AppShellViewModel`).
- `Join`은 `join?pin=`(QR·딥링크·방 목록 참여)일 때만 push 라우트로 쓴다. pin 없는 입장은 `Home` 탭이 담당한다.
```

`- \`Settings\`는 \`MyInfo\`에서 진입하는 상세 push 라우트로 취급한다.`는 그대로 둔다.

§2-1-2의 `- \`Result\` 진입 시 세션 플로우 백스택(\`Join/Waiting/Play\`)을 클리어한다.`를 아래로 교체:

```markdown
- `Result` 진입 시 세션 플로우 엔트리(`Join/Waiting/Play`)만 백스택에서 제거하고, 그 아래의 탭 루트(`Home`·`JoinedRooms` 등)는 유지한다.
```

- [ ] **Step 2: Mac 체크리스트 §9 추가**

파일 끝에:

```markdown
## 9. 홈 셸·하단 4탭 (feature/home, 2026-08-30 — 파트2)

> 신규 Swift 10개(navigation 5 + mypage Settings 5) pbxproj idx **145~154**, Home* 4파일 삭제, MyInfo*↔Settings*↔JoinedRooms* 이름 재배치. **다음 가용 idx = 155.** 그룹 ID 신규 없음.

- [ ] 앱 시작 = 입장 폼(JoinView) + 하단 탭 4개(홈·내가 만든 방·참여한 방·마이) — 탭 바 tint Primary
- [ ] 게스트: 내가 만든 방/참여한 방/마이 탭 → SignInView push(탭 바 숨김) → 닫기 → 직전 탭 유지
- [ ] `[백엔드]` 회원: 참여한 방 탭(M-08) 로드 · 마이 탭(M-12) 프로필/코인/정산 카드 · 시트 4종 저장 후 카드 갱신 · 로그아웃 → 홈 탭 + 게스트 전환
- [ ] `[백엔드]` 홈 탭 입장 → 대기실 → 풀이 → 결과에서 뒤로가기 시 홈 탭 루트로(세션 엔트리 제거)
- [ ] 마이 → 설정 → 회원 탈퇴 확인 알림(409 시 서버 문구) `[백엔드]`
```

- [ ] **Step 3: 스펙 조정 3건 반영**

`docs/superpowers/specs/2026-08-30-home-shell-tabs-design.md`:
1. §1-3의 `action: \`SelectTab(tab)\` · \`Refresh\`…` 문장을 `action: \`SelectTab(tab)\` — 탭을 누를 때마다 \`IsSignedInUseCase\`를 동기 조회하므로 별도 Refresh가 필요 없다`로 바꾸고, §4-1 로그아웃 행의 `+ 셸 \`Refresh\``를 삭제.
2. §2 첫 두 항목을 `- \`Route.Home\`이 \`JoinScreen()\` / \`JoinView(initialPin: nil)\`를 렌더한다. Join에는 원래 뒤로가기 버튼이 없어 \`isTabRoot\` 파라미터가 불필요하다(구현 중 확인).`로 교체.
3. §1-4 표 아래 `탭 바 컴포넌트` 항목을 `탭 바 컴포넌트: Compose \`component/PassmateBottomTabBar.kt\`(Android·Desktop 공유). iOS는 네이티브 \`TabView\` 탭 바(SF Symbols, tint Primary)를 쓴다.`로 교체. §4-1 코인 내역 행 부제를 `\`CoinBalance.recent\`에서 "최근 8/22 -10,000 C"`로, "단순화" 문장에서 코인 내역 부제 언급을 삭제.

- [ ] **Step 4: 보류 주석 잔재 확인**

```bash
grep -rn "4탭 셸 전환은 보류" composeApp iosApp   # → 0건
grep -rn "HomeScreen\|HomeView\b" composeApp/src iosApp/iosApp   # → 0건
```

- [ ] **Step 5: 최종 검증**

```bash
sh gradlew :composeApp:jvmTest :shared:jvmTest --console=plain
sh gradlew :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid --console=plain
cd iosApp && xcodebuild -project $PWD/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination "platform=iOS Simulator,id=4EA9F2CB-461B-4B4A-9977-1DC38372DD99" -derivedDataPath $PWD/build/DerivedData CODE_SIGNING_ALLOWED=NO build 2>&1 | grep -E "BUILD (SUCCEEDED|FAILED)"; cd ..
git checkout -- gradlew; git status --short
```
Expected: 테스트 전부 통과, 컴파일·빌드 성공, 작업 트리 깨끗(HANDOVER.md 제외)

- [ ] **Step 6: 커밋**

```bash
git add docs
git commit -m "docs: 규칙 §2-1-1 탭 루트 4개·Result 클리어 범위 갱신, Mac 체크리스트 §9(홈 셸·탭), 스펙 구현 조정 3건

Claude-Session: https://claude.ai/code/session_01EQVuB7AawaJr3kvsfXy3xN"
```

- [ ] **Step 7: PR 준비 (push는 사용자 승인 후)**

PR #16(`fix/ios-build`)이 develop에 병합됐으면 `git rebase develop feature/home`으로 fix 커밋을 떨궈낸 뒤 Step 5를 한 번 더 돌린다. 아니면 PR 본문에 "PR #16 병합 후 리베이스 예정"을 적는다. PR 본문 요지: 스펙 §0 결정 표 + 규칙 문서 §2-1-1 변경(홍희표 님 리뷰 요청) + 검증 결과(테스트 수·빌드·스모크 스크린샷).
