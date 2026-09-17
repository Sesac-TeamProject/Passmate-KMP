# 반응형 내비게이션 셸 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 창 가로폭 600dp를 경계로 하단 탭바(좁음) ↔ 좌측 내비게이션 레일(넓음)을 바꾸고, 넓은 창에서 본문을 600dp로 묶어 가운데 정렬한다. Android · Desktop · iOS 3플랫폼 동일.

**Architecture:** 판정은 순수 함수 `AppShellLayoutPolicy.layoutFor(width)` 한 곳에만 둔다. 배치는 공통 컴포넌트 `PassmateNavShell(selectedTab, onSelectTab) { 본문 }` 하나가 전담하고, 지금 탭바를 그리는 4개 호출부(Android `Scaffold`, Desktop `Column`, iOS 루트, iOS push 래퍼)가 전부 이 셸로 바뀐다. 탭 항목(아이콘+라벨)은 하단바와 레일이 `PassmateTabItem`으로 공유한다.

**Tech Stack:** Kotlin Multiplatform 1.9.20 · Compose Multiplatform 1.5.12 (material3 1.5.12) · SwiftUI (iOS 15.0 최소 배포) · Koin 3.5.6 · Gradle (WSL에서는 `cmd.exe /c gradlew.bat`)

**설계 문서:** `docs/superpowers/specs/2026-09-16-responsive-nav-shell-design.md`

## Global Constraints

- 브랜치는 `feature/responsive-nav-shell`(develop `18d071f` 기반). 이미 만들어져 있고 설계 문서 커밋 `81fc636`이 올라가 있다.
- **리포 전체에 CRLF 플립 노이즈가 깔려 있다(53파일, 실제 수정 아님).** `git add -A`·`git add .`·`git commit -- <dir>`를 절대 쓰지 않는다. 매 커밋은 그 태스크가 실제로 만든/고친 파일만 **경로로 명시** `git add` 한다.
- 모든 git 명령은 **cwd가 `/mnt/c/Users/hong2/IntelliJIDEAProjects/NewProject/Passmate-KMP`** 여야 한다. 상위 폴더에서 실행하면 홈 디렉터리의 다른 리포에 적용된다.
- 새 파일은 **LF 개행**으로 작성한다 (`docs/Passmate_코드_패턴_규칙.md` 작업 관행).
- 화면·컴포넌트 코드에 **hex 색상 하드코딩 금지** — `PassmateColors` 시맨틱 토큰만 쓴다 (규칙 §11-2).
- 아이콘은 `PassmateIcon(icon = PassmateIcons.X, …)`으로만 그린다. 벡터 지오메트리를 코드에 쓰지 않는다 (규칙 §11-3).
- 코드 배치: 클래스/구조체 프로퍼티는 한 줄씩 선언하고 **선언 사이를 개행**한다. 메서드 안에서는 변수 선언을 상단에, 호출을 하단에 모으고 그 사이를 개행한다 (규칙 §16).
- Compose 컴포넌트와 iosApp 미러는 **이름·값이 1:1**이어야 한다 (규칙 §14).
- iOS 최소 배포 타깃 15.0 — iOS 16+ 전용 API(`NavigationStack`·`Layout`·`presentationDetents`)를 쓰지 않는다 (규칙 §2-1).
- CMP 1.5.12에는 `HorizontalDivider`/`VerticalDivider`가 없다. 가로선은 `Divider`, **세로선은 `Box` + `background`** 로 그린다.
- 빌드는 WSL gradle이 EIO를 내므로 Windows 런처를 쓴다: `cmd.exe /c "gradlew.bat <task>"`.
- 커밋 메시지 끝에 `Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>` 한 줄을 붙인다.

---

### Task 1: 배치 판정 정책과 경계 테스트

**Files:**
- Create: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/AppShellLayout.kt`
- Test: `composeApp/src/jvmTest/kotlin/org/sesacteamproject/passmate/navigation/AppShellLayoutTest.kt`

**Interfaces:**
- Consumes: 없음 (첫 태스크)
- Produces:
  - `enum class AppShellLayout { BOTTOM_BAR, RAIL }` (패키지 `org.sesacteamproject.passmate.navigation`)
  - `object AppShellLayoutPolicy` — `val RAIL_MIN_WIDTH: Dp`, `val CONTENT_MAX_WIDTH: Dp`, `fun layoutFor(width: Dp): AppShellLayout`

- [ ] **Step 1: 실패하는 테스트 작성**

`composeApp/src/jvmTest/kotlin/org/sesacteamproject/passmate/navigation/AppShellLayoutTest.kt` 신규:

```kotlin
package org.sesacteamproject.passmate.navigation

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

// 배치 경계 판정 — 스펙 2026-09-16 §1-1
class AppShellLayoutTest {

    @Test
    fun narrowerThanBoundaryUsesBottomBar() {
        assertEquals(AppShellLayout.BOTTOM_BAR, AppShellLayoutPolicy.layoutFor(599.dp))
    }

    @Test
    fun exactlyBoundaryUsesRail() {
        assertEquals(AppShellLayout.RAIL, AppShellLayoutPolicy.layoutFor(600.dp))
    }

    @Test
    fun widerThanBoundaryUsesRail() {
        assertEquals(AppShellLayout.RAIL, AppShellLayoutPolicy.layoutFor(601.dp))
    }

    // 첫 측정 프레임에서 0이 들어와도 레일이 번쩍이지 않아야 한다
    @Test
    fun zeroWidthUsesBottomBar() {
        assertEquals(AppShellLayout.BOTTOM_BAR, AppShellLayoutPolicy.layoutFor(0.dp))
    }

    @Test
    fun boundaryMatchesMaterialCompactBreakpoint() {
        assertEquals(600.dp, AppShellLayoutPolicy.RAIL_MIN_WIDTH)
    }

    @Test
    fun contentIsClampedToDesignWidth() {
        assertEquals(600.dp, AppShellLayoutPolicy.CONTENT_MAX_WIDTH)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

Run: `cmd.exe /c "gradlew.bat :composeApp:jvmTest --tests *AppShellLayoutTest*"`
Expected: 컴파일 실패 — `Unresolved reference: AppShellLayout`

- [ ] **Step 3: 최소 구현**

`composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/AppShellLayout.kt` 신규:

```kotlin
package org.sesacteamproject.passmate.navigation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 셸이 고를 수 있는 내비게이션 배치 (스펙 2026-09-16 §1-1)
enum class AppShellLayout {
    BOTTOM_BAR,
    RAIL
}

// 가로폭 하나로 배치를 정한다. 판정은 여기 한 곳에만 둔다 (규칙 §2-1-1의 barOwnerOf와 같은 원칙)
object AppShellLayoutPolicy {

    // Material3 window size class의 compact/medium 경계
    val RAIL_MIN_WIDTH: Dp = 600.dp

    // 전 화면이 모바일 폭 기준 시안이라, 넓은 창에서는 본문을 이 폭으로 묶는다
    val CONTENT_MAX_WIDTH: Dp = 600.dp

    // 재는 값은 셸이 받은 **창 폭**이다. 레일을 뺀 본문 폭으로 재면 600 근처에서 배치가 튄다
    // (레일이 붙으면 본문이 600 밑으로 내려가 다시 하단바가 되고, 그러면 다시 600을 넘는다)
    fun layoutFor(width: Dp): AppShellLayout {
        return if (width >= RAIL_MIN_WIDTH) AppShellLayout.RAIL else AppShellLayout.BOTTOM_BAR
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `cmd.exe /c "gradlew.bat :composeApp:jvmTest --tests *AppShellLayoutTest*"`
Expected: PASS (6 tests)

- [ ] **Step 5: 커밋**

```bash
cd /mnt/c/Users/hong2/IntelliJIDEAProjects/NewProject/Passmate-KMP
git add composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/AppShellLayout.kt \
        composeApp/src/jvmTest/kotlin/org/sesacteamproject/passmate/navigation/AppShellLayoutTest.kt
git commit -m "$(cat <<'MSG'
feat(nav): 가로폭으로 내비게이션 배치를 정하는 정책을 넣는다

600dp(Material compact/medium 경계) 이상이면 레일, 미만이면 하단 탭바.
재는 값은 창 폭이다 — 레일을 뺀 본문 폭으로 재면 경계에서 배치가 튄다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
)"
```

---

### Task 2: 탭 항목을 공통 컴포넌트로 승격

하단바의 `private` 탭 항목을 레일과 나눠 쓸 수 있게 꺼낸다. **시각 결과는 1픽셀도 바뀌지 않는 순수 리팩터다.**

**Files:**
- Create: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/component/PassmateTabItem.kt`
- Modify: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/component/PassmateBottomTabBar.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `@Composable fun PassmateTabItem(tab: AppTab, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)` (패키지 `org.sesacteamproject.passmate.component`)

- [ ] **Step 1: 새 파일 작성**

`composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/component/PassmateTabItem.kt` 신규 — `PassmateBottomTabBar.kt`의 `private fun TabItem`과 `private fun iconFor`를 옮긴 것이다. **레이아웃 값(여백·크기·굵기·자간·색 토큰)은 한 글자도 바꾸지 않는다.** 달라지는 것은 둘뿐이다 — 함수명(`TabItem` → `PassmateTabItem`)과 `modifier` 파라미터 추가, 그리고 아래 주석 한 줄이 공유 컴포넌트에 맞게 다시 쓰였다(원문 "여백은 전부 Spacer가 쥔다"는 하단바에서만 참이고, 레일은 `Arrangement.spacedBy`가 여백을 쥔다):

```kotlin
package org.sesacteamproject.passmate.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.navigation.AppTab
import org.sesacteamproject.passmate.theme.PassmateColors

// 탭 항목 하나 — 하단 탭바(PassmateBottomTabBar)와 좌측 레일(PassmateNavigationRail)이 공유한다 (규칙 §11)
@Composable
fun PassmateTabItem(
    tab: AppTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) PassmateColors.Primary else PassmateColors.TextTertiary

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            // 좌우 여백을 두면 하단바의 안쪽 간격에만 24가 더해져 6:5:5:5:6이 어긋난다 — 여백은 전부 부모가 쥔다
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PassmateIcon(
            icon = iconFor(tab),
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

// 시안 v6 nav/4탭의 icon/* 과 1:1 (규칙 §11-3 — 화면 코드에 지오메트리를 쓰지 않는다)
private fun iconFor(tab: AppTab): PassmateIcons {
    return when (tab) {
        AppTab.HOME -> PassmateIcons.Home
        AppTab.HOSTED_ROOMS -> PassmateIcons.PlusSquare
        AppTab.JOINED_ROOMS -> PassmateIcons.DoorOpen
        AppTab.MY_INFO -> PassmateIcons.User
    }
}
```

- [ ] **Step 2: 하단바에서 옮긴 코드를 지우고 새 컴포넌트를 부르게 고친다**

`PassmateBottomTabBar.kt`에서:

1. 파일 끝의 `private fun TabItem(...)` 전체와 `private fun iconFor(tab: AppTab): PassmateIcons` 전체를 **삭제**한다.
2. `AppTab.entries.forEachIndexed` 안의 호출부를 바꾼다:

```kotlin
                TabItem(
                    tab = tab,
                    isSelected = tab == selectedTab,
                    onClick = { onSelectTab(tab) }
                )
```
→
```kotlin
                PassmateTabItem(
                    tab = tab,
                    isSelected = tab == selectedTab,
                    onClick = { onSelectTab(tab) }
                )
```

3. 더 이상 쓰지 않는 import를 지운다:
```
androidx.compose.foundation.clickable
androidx.compose.foundation.layout.Arrangement
androidx.compose.foundation.layout.size
androidx.compose.material3.Text
androidx.compose.ui.Alignment
androidx.compose.ui.text.font.FontWeight
androidx.compose.ui.unit.sp
```

`androidx.compose.foundation.layout.padding`은 **지우지 않는다** — 탭바 Row가 `.padding(top = 10.dp, bottom = 0.dp)`로 계속 쓴다.
(`Arrangement`는 현재 81행 `verticalArrangement = Arrangement.spacedBy(4.dp)` 한 곳에서만 쓰이고 그 코드가 통째로 옮겨가므로 함께 지운다.)

남는 import는 정확히 이 13개다: `background`, `Column`, `Row`, `Spacer`, `fillMaxWidth`, `navigationBarsPadding`, `padding`, `Divider`, `Composable`, `Modifier`, `dp`, `AppTab`, `PassmateColors`.

- [ ] **Step 3: 컴파일 확인**

Run: `cmd.exe /c "gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid"`
Expected: BUILD SUCCESSFUL, 미사용 import 경고 없음

- [ ] **Step 4: 기존 테스트가 여전히 통과하는지 확인**

Run: `cmd.exe /c "gradlew.bat :composeApp:jvmTest"`
Expected: PASS (기존 테스트 전부 + Task 1의 6개)

- [ ] **Step 5: 커밋**

```bash
cd /mnt/c/Users/hong2/IntelliJIDEAProjects/NewProject/Passmate-KMP
git add composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/component/PassmateTabItem.kt \
        composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/component/PassmateBottomTabBar.kt
git commit -m "$(cat <<'MSG'
refactor(component): 탭 항목을 PassmateTabItem으로 승격한다

하단 탭바의 private TabItem을 레일과 나눠 쓸 수 있게 꺼낸다 (규칙 §11).
내용은 그대로라 하단바의 시각 결과는 바뀌지 않는다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
)"
```

---

### Task 3: `PassmateNavigationRail` (Compose)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/component/PassmateNavigationRail.kt`

**Interfaces:**
- Consumes: Task 2의 `PassmateTabItem(tab, isSelected, onClick, modifier)`
- Produces: `@Composable fun PassmateNavigationRail(selectedTab: AppTab?, onSelectTab: (AppTab) -> Unit, modifier: Modifier = Modifier)` — 바깥 폭은 정확히 80dp

- [ ] **Step 1: 컴포넌트 작성**

```kotlin
package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sesacteamproject.passmate.navigation.AppTab
import org.sesacteamproject.passmate.theme.PassmateColors

// Material3 NavigationRail 기본 폭. 오른쪽 1dp 구분선을 포함한 바깥 폭이다
private val RAIL_WIDTH = 80.dp

// 항목 사이 간격 — 하단바의 6:5:5:5:6 비율을 세로로 그대로 쓰면 높이 900dp 창에서 항목이 화면 전체로 흩어진다
private val ITEM_GAP = 8.dp

// 항목 묶음을 바 위 가장자리에서 띄우는 값 — 하단바의 padding(top = 10.dp)와 같은 값을 쓴다
private val ITEM_TOP_PADDING = 10.dp

// 좌측 4탭 레일 (넓은 창) — 하단 탭바를 그대로 세운 형태다. 항목은 PassmateTabItem을 공유하고
// 위에서부터 쌓는다 (스펙 2026-09-16 §3). 표시 여부 판정은 호출부(PassmateNavShell)가 한다
@Composable
fun PassmateNavigationRail(
    selectedTab: AppTab?,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxHeight().width(RAIL_WIDTH)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(PassmateColors.Surface)
                // 레일은 화면 높이를 다 쓰므로 상·하단 시스템 바를 스스로 피한다
                .statusBarsPadding()
                .navigationBarsPadding()
                // 항목은 위에서부터 쌓는다 (사용자 결정 2026-09-16 — 가운데 정렬을 뒤집음)
                .padding(top = ITEM_TOP_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ITEM_GAP)
        ) {
            AppTab.entries.forEach { tab ->
                PassmateTabItem(
                    tab = tab,
                    isSelected = tab == selectedTab,
                    onClick = { onSelectTab(tab) }
                )
            }
        }
        // 하단바의 위쪽 1dp Divider를 90° 돌린 것. CMP 1.5.12에는 VerticalDivider가 없어 Box로 그린다
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(PassmateColors.Border)
        )
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `cmd.exe /c "gradlew.bat :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid"`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
cd /mnt/c/Users/hong2/IntelliJIDEAProjects/NewProject/Passmate-KMP
git add composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/component/PassmateNavigationRail.kt
git commit -m "$(cat <<'MSG'
feat(component): 좌측 4탭 내비게이션 레일을 넣는다

하단 탭바를 그대로 세운 형태 — 폭 80dp, 오른쪽 1dp 구분선, 항목은
PassmateTabItem 공유. 세로는 위에서부터 8dp 간격으로 쌓는다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
)"
```

---

### Task 4: `PassmateNavShell` + 데스크톱 배선 (첫 동작 확인)

여기서 처음으로 눈에 보이는 결과가 나온다 — 데스크톱 창을 좌우로 줄이면 레일과 하단바가 바뀐다.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/component/PassmateNavShell.kt`
- Modify: `composeApp/src/jvmMain/kotlin/org/sesacteamproject/passmate/navigation/AppNavHost.jvm.kt:182-238`
- Modify: `composeApp/src/jvmMain/kotlin/org/sesacteamproject/passmate/main.kt`

**Interfaces:**
- Consumes: Task 1의 `AppShellLayout`·`AppShellLayoutPolicy`, Task 3의 `PassmateNavigationRail`, 기존 `PassmateBottomTabBar(selectedTab, onSelectTab, modifier)`
- Produces: `@Composable fun PassmateNavShell(selectedTab: AppTab?, onSelectTab: (AppTab) -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit)`

- [ ] **Step 1: 셸 컴포넌트 작성**

`composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/component/PassmateNavShell.kt` 신규:

```kotlin
package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.sesacteamproject.passmate.navigation.AppShellLayout
import org.sesacteamproject.passmate.navigation.AppShellLayoutPolicy
import org.sesacteamproject.passmate.navigation.AppTab
import org.sesacteamproject.passmate.theme.PassmateColors

// 내비게이션과 본문의 배치만 담당하는 셸 — Android·Desktop·iOS가 모두 이걸 거친다 (스펙 2026-09-16 §2).
// 어떤 탭이 켜졌는지(selectedTab)와 눌렀을 때 할 일(onSelectTab)은 호출부가 정한다.
// selectedTab이 null이면 레일도 하단바도 그리지 않는다 — 세션 플로우·SignIn·명성·정산에서
// 바를 숨기는 기존 규칙(규칙 §2-1-1)이 레일에도 그대로 적용된다
@Composable
fun PassmateNavShell(
    selectedTab: AppTab?,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        when (AppShellLayoutPolicy.layoutFor(maxWidth)) {
            AppShellLayout.RAIL -> Row(modifier = Modifier.fillMaxSize()) {
                if (selectedTab != null) {
                    PassmateNavigationRail(
                        selectedTab = selectedTab,
                        onSelectTab = onSelectTab
                    )
                }
                ContentArea(modifier = Modifier.weight(1f), content = content)
            }
            AppShellLayout.BOTTOM_BAR -> Column(modifier = Modifier.fillMaxSize()) {
                ContentArea(modifier = Modifier.weight(1f), content = content)
                if (selectedTab != null) {
                    PassmateBottomTabBar(
                        selectedTab = selectedTab,
                        onSelectTab = onSelectTab
                    )
                }
            }
        }
    }
}

// 본문 — 넓은 창에서 시안 폭을 넘지 않게 묶고 가운데 정렬한다. 남는 좌우는 앱 배경색으로 채운다.
// 화면들이 스스로 흰 Surface를 깔기 때문에 여백까지 흰색이면 본문이 어디까지인지 보이지 않는다
@Composable
private fun ContentArea(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize().background(PassmateColors.BackgroundMint),
        contentAlignment = Alignment.TopCenter
    ) {
        // widthIn이 먼저 들어오는 제약을 깎고, 그다음 fillMaxSize가 깎인 제약을 채운다.
        // 순서를 바꾸면 fillMaxSize가 원래 제약을 먼저 채워 클램프가 무시된다
        Box(
            modifier = Modifier
                .widthIn(max = AppShellLayoutPolicy.CONTENT_MAX_WIDTH)
                .fillMaxSize()
        ) {
            content()
        }
    }
}
```

- [ ] **Step 2: 데스크톱 호스트를 셸로 바꾼다**

`composeApp/src/jvmMain/kotlin/org/sesacteamproject/passmate/navigation/AppNavHost.jvm.kt`

**a.** 파일 끝(현재 182~238행)의 `Column { Box(weight 1f) { when(...) }; if (currentTab != null) PassmateBottomTabBar(...) }` 구조를 통째로 바꾼다:

현재 파일의 구조는 이렇다:

- 182행 `Column(modifier = Modifier.fillMaxSize()) {`
- 183행 `Box(modifier = Modifier.fillMaxWidth().weight(1f)) {`
- **184~232행 `when (currentDestination) { ... }`** — `is JvmDestination.Home ->` 부터 `is JvmDestination.SessionControl -> ...` 까지 분기 22개
- 233~236행 `PassmateBottomTabBar(...)` (231행쯤의 `if (currentTab != null) {` 안)
- 238행 `}` (Column 닫음), 239행 `}` (AppNavHost 닫음)

**184~232행의 `when` 블록은 한 글자도 바꾸지 않는다.** 그걸 감싸던 182·183행의 `Column`/`Box`와, 끝의 `if (currentTab != null) { PassmateBottomTabBar(...) }` 를 걷어내고 셸 람다로 감싼다:

```kotlin
    PassmateNavShell(
        selectedTab = currentTab,
        onSelectTab = { shellViewModel.onAction(AppShellAction.SelectTab(it)) }
    ) {
        when (currentDestination) {
            is JvmDestination.Home -> JoinScreen(onNavigate = onNavigate)
            is JvmDestination.RoomList -> RoomListScreen(onNavigate = onNavigate)
            // (184~232행의 나머지 분기를 들여쓰기만 한 단계 줄여 그대로 둔다)
            is JvmDestination.SessionControl -> SessionControlScreen(
                roomId = currentDestination.roomId,
                pin = currentDestination.pin,
                onNavigate = onNavigate
            )
        }
    }
}
```

`Box`가 사라져도 화면은 눌리지 않는다 — 각 Screen이 스스로 `fillMaxSize()`를 건다.

**b.** import 정리 — 다음 6줄을 지운다:
```
androidx.compose.foundation.layout.Box
androidx.compose.foundation.layout.Column
androidx.compose.foundation.layout.fillMaxSize
androidx.compose.foundation.layout.fillMaxWidth
androidx.compose.ui.Modifier
org.sesacteamproject.passmate.component.PassmateBottomTabBar
```
그리고 한 줄을 넣는다:
```
org.sesacteamproject.passmate.component.PassmateNavShell
```

- [ ] **Step 3: 데스크톱 창 기본 크기를 준다**

`composeApp/src/jvmMain/kotlin/org/sesacteamproject/passmate/main.kt` — `Window(...)`에 `state`를 추가한다:

```kotlin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
```

```kotlin
        Window(
            onCloseRequest = ::exitApplication,
            // 본문 클램프(600) + 레일(80)이 여유 있게 들어가는 크기로 연다. 무지정 기본값은 800x600이다
            state = rememberWindowState(width = 1100.dp, height = 760.dp),
            title = "패스메이트",
            icon = painterResource("passmate-icon.png"),
        ) {
            App()
        }
```

- [ ] **Step 4: 컴파일·테스트 확인**

Run: `cmd.exe /c "gradlew.bat :composeApp:compileKotlinJvm :composeApp:jvmTest"`
Expected: BUILD SUCCESSFUL, 테스트 전부 PASS

- [ ] **Step 5: 데스크톱 실행 스모크**

Run (백그라운드로 — 창을 닫을 때까지 블록된다): `cmd.exe /c "gradlew.bat :composeApp:run"`

WSL 세션에서는 Windows 창이 보이지 않으므로 **사용자에게 아래를 직접 확인해 달라고 요청한다.** 창이 뜨지 않고 스택트레이스가 나오면 그건 이쪽에서 고친다.

- 1100x760으로 열렸을 때 **왼쪽에 레일**, 본문은 가운데 600dp 폭, 좌우에 민트색 여백
- 창 오른쪽 모서리를 잡고 가로로 줄이면 600dp 아래에서 **레일이 사라지고 하단 탭바**로 바뀐다
- 탭(홈·내가 만든 방·참여한 방·마이)을 레일에서 눌렀을 때 하단바와 똑같이 동작한다 (게스트면 로그인 화면으로)
- 로그인 화면·대기실·풀이 화면에서는 **레일도 하단바도 안 보인다**
- 마이 → 코인 내역처럼 탭바를 유지하는 상세 화면에서는 레일이 그대로 켜져 있다

- [ ] **Step 6: 커밋**

```bash
cd /mnt/c/Users/hong2/IntelliJIDEAProjects/NewProject/Passmate-KMP
git add composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/component/PassmateNavShell.kt \
        composeApp/src/jvmMain/kotlin/org/sesacteamproject/passmate/navigation/AppNavHost.jvm.kt \
        composeApp/src/jvmMain/kotlin/org/sesacteamproject/passmate/main.kt
git commit -m "$(cat <<'MSG'
feat(nav): 데스크톱이 창 가로폭에 맞춰 레일과 하단바를 바꾼다

배치 책임을 PassmateNavShell 하나로 모으고 Desktop 호스트를 거기에 태운다.
넓은 창에서는 본문을 600dp로 묶어 가운데 정렬하고 남는 좌우는 앱 배경색으로 채운다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
)"
```

---

### Task 5: Android 배선

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/org/sesacteamproject/passmate/navigation/AppNavHost.android.kt:219-237`

**Interfaces:**
- Consumes: Task 4의 `PassmateNavShell(selectedTab, onSelectTab, modifier, content)`
- Produces: 없음 (마지막 Compose 호출부)

- [ ] **Step 1: `Scaffold`를 셸로 바꾼다**

현재(219~237행):

```kotlin
    Scaffold(
        // 0을 유지한다 — 화면 배경이 상태바 뒤까지 깔려야 iOS와 같아진다.
        // 상단 인셋은 각 화면이 배경 뒤에 statusBarsPadding으로 직접 준다
        // (iOS도 화면마다 `.background(색.ignoresSafeArea())`로 같은 일을 한다)
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
```

바꾼 뒤:

```kotlin
    // 화면 배경이 상태바 뒤까지 깔려야 iOS와 같아진다 — 상단 인셋은 각 화면이 배경 뒤에
    // statusBarsPadding으로 직접 준다 (iOS도 화면마다 `.background(색.ignoresSafeArea())`로 같은 일을 한다).
    // 셸이 인셋을 먹지 않으므로 Scaffold(contentWindowInsets = 0)와 같은 결과다
    PassmateNavShell(
        selectedTab = currentTab,
        onSelectTab = { shellViewModel.onAction(AppShellAction.SelectTab(it)) }
    ) {
        NavHost(
            navController = navController,
            startDestination = Route.Home.route
        ) {
```

`NavHost` 블록 안의 `composable(...)` 분기 22개는 **한 줄도 바꾸지 않는다.** 파일 끝의 닫는 괄호 구조(`}` for NavHost, `}` for 셸, `}` for `AppNavHost`)는 그대로 유지된다.

- [ ] **Step 2: import 정리**

다음 5줄을 지운다:
```
androidx.compose.foundation.layout.WindowInsets
androidx.compose.foundation.layout.padding
androidx.compose.material3.Scaffold
androidx.compose.ui.Modifier
org.sesacteamproject.passmate.component.PassmateBottomTabBar
```
한 줄을 넣는다:
```
org.sesacteamproject.passmate.component.PassmateNavShell
```

(`Modifier`는 이 파일에서 `Modifier.padding(innerPadding)` 한 곳에만 쓰였다 — 지워도 된다.)

- [ ] **Step 3: 컴파일 확인**

Run: `cmd.exe /c "gradlew.bat :composeApp:compileDebugKotlinAndroid"`
Expected: BUILD SUCCESSFUL, 미사용 import 경고 없음

- [ ] **Step 4: 전체 검증 (3타깃 중 Android·jvm + 테스트)**

Run: `cmd.exe /c "gradlew.bat :shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinJvm :composeApp:jvmTest"`
Expected: BUILD SUCCESSFUL, 테스트 전부 PASS

- [ ] **Step 5: 커밋**

```bash
cd /mnt/c/Users/hong2/IntelliJIDEAProjects/NewProject/Passmate-KMP
git add composeApp/src/androidMain/kotlin/org/sesacteamproject/passmate/navigation/AppNavHost.android.kt
git commit -m "$(cat <<'MSG'
feat(nav): 안드로이드 호스트를 반응형 셸에 태운다

Scaffold를 걷어내고 PassmateNavShell로 바꾼다. contentWindowInsets가 이미 0이고
snackbar·FAB를 쓰지 않아 bottomBar 슬롯 말고는 하는 일이 없었다.
폰(≤430dp)은 계속 하단 탭바 — 태블릿·폴더블에서만 레일로 바뀐다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
)"
```

---

### Task 6: iOS 미러 ① 정책 + 탭 항목 + 레일

**WSL에서는 Swift를 컴파일할 수 없다.** 이 태스크와 다음 태스크의 검증은 Mac 체크리스트로 넘어간다. 대신 Compose 원본과 이름·값이 1:1인지 눈으로 대조하고, pbxproj 등록 누락이 없는지 grep으로 확인한다.

**Files:**
- Create: `iosApp/iosApp/navigation/AppShellLayout.swift`
- Create: `iosApp/iosApp/component/PassmateTabItemView.swift`
- Create: `iosApp/iosApp/component/PassmateNavigationRail.swift`
- Modify: `iosApp/iosApp/component/PassmateBottomTabBar.swift`
- Modify: `iosApp/iosApp.xcodeproj/project.pbxproj`

**Interfaces:**
- Consumes: 기존 `AppTab`(`label`·`icon`·`allCases`), `PassmateColors`, `PassmateIconView(icon:tint:size:)`
- Produces:
  - `enum AppShellLayout { case bottomBar, rail }`
  - `enum AppShellLayoutPolicy` — `static let railMinWidth: CGFloat`, `static let contentMaxWidth: CGFloat`, `static func layoutFor(width: CGFloat) -> AppShellLayout`
  - `struct PassmateTabItemView: View` — `init(tab:isSelected:onTap:)`
  - `struct PassmateNavigationRail: View` — `init(selectedTab:onSelectTab:)`, `static let width: CGFloat = 80`

- [ ] **Step 1: 판정 정책 미러 작성**

`iosApp/iosApp/navigation/AppShellLayout.swift` 신규:

```swift
import CoreGraphics

// 셸이 고를 수 있는 내비게이션 배치 — Compose navigation/AppShellLayout.kt 미러 (규칙 §14)
enum AppShellLayout {
    case bottomBar
    case rail
}

// 가로폭 하나로 배치를 정한다. 판정은 여기 한 곳에만 둔다
enum AppShellLayoutPolicy {
    // Material3 window size class의 compact/medium 경계
    static let railMinWidth: CGFloat = 600

    // 전 화면이 모바일 폭 기준 시안이라, 넓은 창에서는 본문을 이 폭으로 묶는다
    static let contentMaxWidth: CGFloat = 600

    // horizontalSizeClass가 아니라 실제 폭으로 잰다 — size class는 iPad 분할 화면에서
    // 경계가 600pt와 어긋나 3플랫폼 판정이 달라진다 (스펙 2026-09-16 §1-2)
    static func layoutFor(width: CGFloat) -> AppShellLayout {
        return width >= railMinWidth ? .rail : .bottomBar
    }
}
```

- [ ] **Step 2: 탭 항목을 파일로 승격**

`iosApp/iosApp/component/PassmateTabItemView.swift` 신규 — `PassmateBottomTabBar.swift`의 `private struct TabItemView`를 **내용 변경 없이** 옮기고 이름만 바꾼 것이다:

```swift
import SwiftUI

// 탭 항목 하나 — Compose component/PassmateTabItem.kt 미러 (규칙 §14).
// 하단 탭바(PassmateBottomTabBar)와 좌측 레일(PassmateNavigationRail)이 공유한다
struct PassmateTabItemView: View {
    let tab: AppTab

    let isSelected: Bool

    let onTap: () -> Void

    private var color: Color {
        isSelected ? PassmateColors.primary : PassmateColors.textTertiary
    }

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 4) {
                PassmateIconView(icon: tab.icon, tint: color, size: 24)
                Text(tab.label)
                    .font(.system(size: 11, weight: isSelected ? .bold : .medium))
                    .kerning(-0.22)
                    .foregroundColor(color)
            }
            // 좌우 여백을 두면 하단바의 안쪽 간격에만 24가 더해져 6:5:5:5:6이 어긋난다 — 여백은 전부 부모가 쥔다
            .padding(.vertical, 4)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(tab.label)
    }
}
```

`iosApp/iosApp/component/PassmateBottomTabBar.swift`에서:
1. 파일 끝의 `private struct TabItemView: View { ... }` 전체를 **삭제**한다.
2. 호출부를 바꾼다:
```swift
                    TabItemView(
```
→
```swift
                    PassmateTabItemView(
```

- [ ] **Step 3: 레일 미러 작성**

`iosApp/iosApp/component/PassmateNavigationRail.swift` 신규:

```swift
import SwiftUI

// 좌측 4탭 레일 (넓은 창) — Compose component/PassmateNavigationRail.kt 미러 (규칙 §14).
// 하단 탭바를 그대로 세운 형태이고 항목은 위에서부터 쌓는다. 표시 여부 판정은 호출부(PassmateNavShell)가 한다.
// 상·하단 시스템 바 회피는 SwiftUI 기본 세이프에어리어가 해준다 (Compose의 statusBarsPadding에 해당)
struct PassmateNavigationRail: View {
    // Material3 NavigationRail 기본 폭. 오른쪽 1pt 구분선을 포함한 바깥 폭이다
    static let width: CGFloat = 80

    // 항목 사이 간격 — 하단바의 6:5:5:5:6 비율을 세로로 그대로 쓰면 높이 900 창에서 항목이 화면 전체로 흩어진다
    private static let itemGap: CGFloat = 8

    // 항목 묶음을 바 위 가장자리에서 띄우는 값 — 하단바의 .padding(.top, 10)과 같은 값을 쓴다
    private static let itemTopPadding: CGFloat = 10

    let selectedTab: AppTab?

    let onSelectTab: (AppTab) -> Void

    var body: some View {
        HStack(spacing: 0) {
            VStack(spacing: Self.itemGap) {
                ForEach(AppTab.allCases, id: \.self) { tab in
                    PassmateTabItemView(
                        tab: tab,
                        isSelected: tab == selectedTab,
                        onTap: { onSelectTab(tab) }
                    )
                }
            }
            // 항목은 위에서부터 쌓는다 — alignment: .top이 없으면 SwiftUI가 가운데로 놓는다
            // (Compose Arrangement.spacedBy(ITEM_GAP) 미러, 사용자 결정 2026-09-16)
            .padding(.top, Self.itemTopPadding)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(PassmateColors.surface)
            // 하단바의 위쪽 1pt 구분선을 90° 돌린 것
            Rectangle()
                .fill(PassmateColors.border)
                .frame(width: 1)
        }
        .frame(width: Self.width)
    }
}
```

- [ ] **Step 4: pbxproj에 3파일 등록**

`iosApp/iosApp.xcodeproj/project.pbxproj`에 아래 ID로 넣는다 (204·205·206은 파일 전체에서 미사용 확인됨):

| 파일 | 그룹 | fileRef ID | buildFile ID |
|---|---|---|---|
| `AppShellLayout.swift` | navigation (`A1012019AABBCCDDEEFF0019`) | `A1011002204AABBCCDDEEFF0204` | `A1010002204AABBCCDDEEFF0204` |
| `PassmateTabItemView.swift` | component (`A1012021AABBCCDDEEFF0021`) | `A1011002205AABBCCDDEEFF0205` | `A1010002205AABBCCDDEEFF0205` |
| `PassmateNavigationRail.swift` | component (`A1012021AABBCCDDEEFF0021`) | `A1011002206AABBCCDDEEFF0206` | `A1010002206AABBCCDDEEFF0206` |

**4-a.** `PBXBuildFile` 섹션 — `A1010002203AABBCCDDEEFF0203 /* NativeNavigationBarHidden.swift in Sources */ = ...` 줄 **다음에** 세 줄을 넣는다 (들여쓰기는 탭 2개):

```
		A1010002204AABBCCDDEEFF0204 /* AppShellLayout.swift in Sources */ = {isa = PBXBuildFile; fileRef = A1011002204AABBCCDDEEFF0204 /* AppShellLayout.swift */; };
		A1010002205AABBCCDDEEFF0205 /* PassmateTabItemView.swift in Sources */ = {isa = PBXBuildFile; fileRef = A1011002205AABBCCDDEEFF0205 /* PassmateTabItemView.swift */; };
		A1010002206AABBCCDDEEFF0206 /* PassmateNavigationRail.swift in Sources */ = {isa = PBXBuildFile; fileRef = A1011002206AABBCCDDEEFF0206 /* PassmateNavigationRail.swift */; };
```

**4-b.** `PBXFileReference` 섹션 — `A1011002203AABBCCDDEEFF0203 /* NativeNavigationBarHidden.swift */ = ...` 줄 **다음에**:

```
		A1011002204AABBCCDDEEFF0204 /* AppShellLayout.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = AppShellLayout.swift; sourceTree = "<group>"; };
		A1011002205AABBCCDDEEFF0205 /* PassmateTabItemView.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = PassmateTabItemView.swift; sourceTree = "<group>"; };
		A1011002206AABBCCDDEEFF0206 /* PassmateNavigationRail.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = PassmateNavigationRail.swift; sourceTree = "<group>"; };
```

**4-c.** navigation 그룹(`A1012019AABBCCDDEEFF0019`)의 `children` 목록에서 `A1011001145AABBCCDDEEFF0145 /* AppTab.swift */,` 줄 다음에:

```
				A1011002204AABBCCDDEEFF0204 /* AppShellLayout.swift */,
```

**4-d.** component 그룹(`A1012021AABBCCDDEEFF0021`)의 `children` 목록에서 `A1011002201AABBCCDDEEFF0201 /* PassmateBottomTabBar.swift */,` 줄 다음에:

```
				A1011002205AABBCCDDEEFF0205 /* PassmateTabItemView.swift */,
				A1011002206AABBCCDDEEFF0206 /* PassmateNavigationRail.swift */,
```

**4-e.** `PBXSourcesBuildPhase`의 `files` 목록에서 `A1010002203AABBCCDDEEFF0203 /* NativeNavigationBarHidden.swift in Sources */,` 줄 다음에:

```
				A1010002204AABBCCDDEEFF0204 /* AppShellLayout.swift in Sources */,
				A1010002205AABBCCDDEEFF0205 /* PassmateTabItemView.swift in Sources */,
				A1010002206AABBCCDDEEFF0206 /* PassmateNavigationRail.swift in Sources */,
```

- [ ] **Step 5: 등록 누락 검사**

Run:
```bash
cd /mnt/c/Users/hong2/IntelliJIDEAProjects/NewProject/Passmate-KMP
for f in AppShellLayout PassmateTabItemView PassmateNavigationRail; do
  echo "$f → $(grep -c "$f.swift" iosApp/iosApp.xcodeproj/project.pbxproj)"
done
grep -c "TabItemView(" iosApp/iosApp/component/PassmateBottomTabBar.swift
```
Expected: 세 파일 모두 **4**(buildFile · fileRef · group children · sources phase). 마지막 줄은 **1**(`PassmateTabItemView(` 호출 하나 — 옛 `private struct TabItemView`가 남아 있으면 2 이상이 나온다).

- [ ] **Step 6: Compose ↔ Swift 값 대조**

Run:
```bash
cd /mnt/c/Users/hong2/IntelliJIDEAProjects/NewProject/Passmate-KMP
grep -n "600\|80\|8" composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/navigation/AppShellLayout.kt \
     composeApp/src/commonMain/kotlin/org/sesacteamproject/passmate/component/PassmateNavigationRail.kt \
     iosApp/iosApp/navigation/AppShellLayout.swift \
     iosApp/iosApp/component/PassmateNavigationRail.swift
```
Expected: `railMinWidth`=600 · `contentMaxWidth`=600 · 레일 폭=80 · 항목 간격=8 · 상단 여백=10이 양쪽에서 같은 값으로 나온다.

- [ ] **Step 7: 커밋**

```bash
cd /mnt/c/Users/hong2/IntelliJIDEAProjects/NewProject/Passmate-KMP
git add iosApp/iosApp/navigation/AppShellLayout.swift \
        iosApp/iosApp/component/PassmateTabItemView.swift \
        iosApp/iosApp/component/PassmateNavigationRail.swift \
        iosApp/iosApp/component/PassmateBottomTabBar.swift \
        iosApp/iosApp.xcodeproj/project.pbxproj
git commit -m "$(cat <<'MSG'
feat(ios): 배치 판정과 좌측 레일을 미러한다

Compose의 AppShellLayoutPolicy·PassmateTabItem·PassmateNavigationRail을
같은 이름·같은 값으로 옮긴다 (규칙 §14). 하단바의 private TabItemView는
레일과 나눠 쓰도록 PassmateTabItemView로 승격했다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
)"
```

---

### Task 7: iOS 미러 ② 셸 + `ContentView` 배선 + Mac 체크리스트

**Files:**
- Create: `iosApp/iosApp/component/PassmateNavShell.swift`
- Modify: `iosApp/iosApp/ContentView.swift:26`, `:92-106`, `:113-119`
- Modify: `iosApp/iosApp.xcodeproj/project.pbxproj`
- Modify: `docs/Passmate_Mac_검증_체크리스트.md`

**Interfaces:**
- Consumes: Task 6의 `AppShellLayoutPolicy`·`PassmateNavigationRail`, 기존 `PassmateBottomTabBar(selectedTab:onSelectTab:)`
- Produces: `struct PassmateNavShell<Content: View>: View` — `init(selectedTab:onSelectTab:content:)`

- [ ] **Step 1: 셸 미러 작성**

`iosApp/iosApp/component/PassmateNavShell.swift` 신규:

```swift
import SwiftUI

// 내비게이션과 본문의 배치만 담당하는 셸 — Compose component/PassmateNavShell.kt 미러 (규칙 §14).
// selectedTab이 nil이면 레일도 하단바도 그리지 않는다 (규칙 §2-1-1).
// GeometryReader는 콘텐츠 크기로 줄지 않고 자식을 topLeading에 붙이므로 자식 frame을 명시 고정한다 —
// 안 하면 NavigationView(stack)·TabView 안에서 레이아웃이 흔들린다 (스펙 2026-09-16 §2-4)
struct PassmateNavShell<Content: View>: View {
    let selectedTab: AppTab?

    let onSelectTab: (AppTab) -> Void

    @ViewBuilder let content: () -> Content

    var body: some View {
        GeometryReader { geometry in
            shell(for: AppShellLayoutPolicy.layoutFor(width: geometry.size.width))
                .frame(width: geometry.size.width, height: geometry.size.height)
        }
    }

    // switch를 body에서 분리한다 — result builder 안의 지역 let은 Swift 버전에 따라 받아주지 않는다
    @ViewBuilder
    private func shell(for layout: AppShellLayout) -> some View {
        switch layout {
        case .rail:
            HStack(spacing: 0) {
                if let selectedTab = selectedTab {
                    PassmateNavigationRail(selectedTab: selectedTab, onSelectTab: onSelectTab)
                }
                contentArea
            }
        case .bottomBar:
            VStack(spacing: 0) {
                contentArea
                if let selectedTab = selectedTab {
                    PassmateBottomTabBar(selectedTab: selectedTab, onSelectTab: onSelectTab)
                }
            }
        }
    }

    // 본문 — 넓은 화면에서 시안 폭을 넘지 않게 묶고 가운데 정렬한다. 남는 좌우는 앱 배경색으로 채운다.
    // 화면들이 스스로 흰 surface를 깔기 때문에 여백까지 흰색이면 본문이 어디까지인지 보이지 않는다
    private var contentArea: some View {
        ZStack(alignment: .top) {
            PassmateColors.backgroundMint
            content()
                .frame(maxWidth: AppShellLayoutPolicy.contentMaxWidth)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
```

- [ ] **Step 2: `ContentView` 루트를 셸로 바꾼다**

`iosApp/iosApp/ContentView.swift`

**a.** 24~27행의 주석 + `VStack(spacing: 0) {` + `TabView(selection: tabSelection) {` 를:

```swift
            // 탭바·레일은 겹치지 않고 자리를 차지한다 — ZStack으로 덮으면 탭 루트 콘텐츠의
            // 마지막 줄·버튼이 밑으로 들어간다(M-01 로그인 안내·M-13 + 버튼·M-12 하단)
            PassmateNavShell(
                selectedTab: selectedTab,
                onSelectTab: { shellViewModel.action(.selectTab($0)) }
            ) {
            TabView(selection: tabSelection) {
```
로 바꾼다. (`TabView` 이하 탭 4개 선언은 한 줄도 바꾸지 않는다.)

**b.** 113~119행의 꼬리를:

```swift
            .frame(maxHeight: .infinity)
            // 시안 v6 nav/4탭 — 기본 탭 바 대신 Compose와 같은 커스텀 바를 그린다 (규칙 §14)
            PassmateBottomTabBar(
                selectedTab: selectedTab,
                onSelectTab: { shellViewModel.action(.selectTab($0)) }
            )
            }
```

→ 아래로 바꾼다 (하단바 호출은 이제 셸이 한다):

```swift
            .frame(maxHeight: .infinity)
            }
```

- [ ] **Step 3: `ContentView`의 push 래퍼를 셸로 바꾼다**

92~106행:

```swift
                        if let owner = route.tabBarOwner {
                            VStack(spacing: 0) {
                                destinationView(for: route, path: path)
                                    .frame(maxHeight: .infinity)
                                PassmateBottomTabBar(
                                    selectedTab: owner,
                                    onSelectTab: { tab in
                                        self.path = []
                                        shellViewModel.action(.selectTab(tab))
                                    }
                                )
                            }
                        } else {
                            destinationView(for: route, path: path)
                        }
```

→

```swift
                        if let owner = route.tabBarOwner {
                            PassmateNavShell(
                                selectedTab: owner,
                                onSelectTab: { tab in
                                    self.path = []
                                    shellViewModel.action(.selectTab(tab))
                                }
                            ) {
                                destinationView(for: route, path: path)
                                    .frame(maxHeight: .infinity)
                            }
                        } else {
                            destinationView(for: route, path: path)
                        }
```

- [ ] **Step 4: pbxproj에 1파일 등록**

`PassmateNavShell.swift` — component 그룹(`A1012021AABBCCDDEEFF0021`), fileRef `A1011002207AABBCCDDEEFF0207`, buildFile `A1010002207AABBCCDDEEFF0207`.

Task 6의 4-a·4-b·4-d·4-e와 같은 자리(각 블록에서 206 줄 바로 다음)에 한 줄씩 넣는다:

```
		A1010002207AABBCCDDEEFF0207 /* PassmateNavShell.swift in Sources */ = {isa = PBXBuildFile; fileRef = A1011002207AABBCCDDEEFF0207 /* PassmateNavShell.swift */; };
```
```
		A1011002207AABBCCDDEEFF0207 /* PassmateNavShell.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = PassmateNavShell.swift; sourceTree = "<group>"; };
```
```
				A1011002207AABBCCDDEEFF0207 /* PassmateNavShell.swift */,
```
```
				A1010002207AABBCCDDEEFF0207 /* PassmateNavShell.swift in Sources */,
```

- [ ] **Step 5: 등록·배선 검사**

Run:
```bash
cd /mnt/c/Users/hong2/IntelliJIDEAProjects/NewProject/Passmate-KMP
echo "pbxproj 등록: $(grep -c 'PassmateNavShell.swift' iosApp/iosApp.xcodeproj/project.pbxproj)"
echo "ContentView 셸 호출: $(grep -c 'PassmateNavShell(' iosApp/iosApp/ContentView.swift)"
echo "ContentView 하단바 직접 호출: $(grep -c 'PassmateBottomTabBar(' iosApp/iosApp/ContentView.swift)"
echo "내비바 숨김: $(grep -c '\.passmateHidesNativeNavigationBar()' iosApp/iosApp/ContentView.swift)"
```
Expected: 등록 **4** · 셸 호출 **2**(루트 + push 래퍼) · 하단바 직접 호출 **0**(전부 셸이 그린다) · 내비바 숨김 **5**(루트 1 + 탭 4, iOS 15 회귀 방지 — 2026-09-14 체크리스트 §13 기준)

- [ ] **Step 6: Mac 검증 체크리스트에 항목 추가**

`docs/Passmate_Mac_검증_체크리스트.md` 맨 끝에 붙인다:

```markdown

## 14. 반응형 내비게이션 셸 (2026-09-16)

> 신규 Swift 4개 — `navigation/AppShellLayout.swift`(pbxproj idx **204**) · `component/PassmateTabItemView.swift`(**205**) · `component/PassmateNavigationRail.swift`(**206**) · `component/PassmateNavShell.swift`(**207**). 그룹 ID 신규 없음.
> 가로폭 600pt 경계로 하단 탭바 ↔ 좌측 레일(80pt)을 바꾸고, 넓은 화면에서는 본문을 600pt로 묶어 가운데 정렬한다. 레일 항목은 위에서부터 쌓는다(상단 여백 10pt). 판정은 `AppShellLayoutPolicy` 한 곳(Compose 미러). 하단바의 `private TabItemView`는 `PassmateTabItemView`로 승격돼 레일과 공유된다.
> **주의**: `PassmateNavShell`이 `GeometryReader`를 쓴다. `GeometryReader`는 콘텐츠 크기로 줄지 않고 자식을 topLeading에 붙이므로 `NavigationView`(stack)·`TabView` 안에서 레이아웃이 흔들릴 수 있다 — iOS 15 실기기 확인이 이 항목의 핵심이다.

- [ ] 컴파일: `xcodebuild … build` 오류 0 · `grep -c "PassmateBottomTabBar(" iosApp/iosApp/ContentView.swift`가 **0**(하단바는 이제 셸만 그린다)
- [ ] **iPhone (compact, 폭 < 600)**: 홈·내가 만든 방·참여한 방·마이 네 탭 모두 **하단 탭바가 이전과 똑같이** 보인다 — 높이·간격·선택 색·상단 1pt 구분선 동일
  - [ ] 본문 좌우에 민트색 여백이 생기지 않는다(폭이 600 미만이라 클램프가 걸리면 안 된다)
  - [ ] 마이 → 코인 내역·계정 정보 등 M-12-x push 화면에서도 하단 탭바가 유지된다
  - [ ] 대기실·풀이·결과·결제·명성·정산에서는 탭바가 보이지 않는다
  - [ ] iOS 15 실기기: 첫 진입 시 상단 빈 띠 없음(§13 회귀) · 참여한 방 탭 타이틀 떨림 없음
- [ ] **iPad 또는 iPhone 가로 (폭 ≥ 600)**: 왼쪽에 레일(폭 80)이 서고 하단 탭바는 사라진다
  - [ ] 레일 항목이 아이콘 24 + 라벨 11 · 선택 시 primary/Bold, 비선택 textTertiary/Medium
  - [ ] 레일 오른쪽에 1pt border 세로선
  - [ ] 레일 항목이 **위에서부터** 8pt 간격으로 쌓이고, 첫 항목이 바 위 가장자리에서 10pt 떨어져 있다(가운데 정렬 아님)
  - [ ] 본문이 600pt로 묶여 가운데 정렬되고 좌우가 민트색
  - [ ] 레일에서 탭을 누르면 하단바와 동일하게 동작(게스트가 로그인 필수 탭을 누르면 SignIn)
  - [ ] 상태바·홈 인디케이터를 레일이 침범하지 않는다(세이프에어리어)
- [ ] **경계 전환**: iPad 멀티태스킹으로 폭을 600 위아래로 오가면 레일 ↔ 하단바가 바뀌고, 전환 중 화면이 깨지거나 스크롤 위치가 튀지 않는다
- [ ] **회귀**: 로그인/로그아웃(`sessionGeneration` 재생성) 직후에도 레일/탭바 상태가 정상
```

- [ ] **Step 7: 커밋**

```bash
cd /mnt/c/Users/hong2/IntelliJIDEAProjects/NewProject/Passmate-KMP
git add iosApp/iosApp/component/PassmateNavShell.swift \
        iosApp/iosApp/ContentView.swift \
        iosApp/iosApp.xcodeproj/project.pbxproj \
        "docs/Passmate_Mac_검증_체크리스트.md"
git commit -m "$(cat <<'MSG'
feat(ios): 화면 폭에 맞춰 레일과 하단바를 바꾼다

ContentView의 루트와 push 래퍼 두 곳을 PassmateNavShell로 바꿔
하단바를 그리는 자리를 셸 한 곳으로 모은다. WSL에서 Swift 컴파일이
불가능하므로 Mac 검증 체크리스트 §14에 확인 항목을 남긴다.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
MSG
)"
```

---

## 완료 후 남는 일

- **iOS 미검증** — Mac에서 `docs/Passmate_Mac_검증_체크리스트.md` §14를 돌려야 한다. 그 전까지 iOS는 "코드만 작성됨"으로 보고한다.
- **Android 실기기 미검증** — 폰(≤430dp)은 코드 경로가 하단바 그대로라 회귀 위험이 낮지만, 태블릿·폴더블 레일은 실기기 확인이 필요하다.
- **push·PR은 사용자 몫** — 이 리포는 `cmd.exe` git push + GCM 토큰이 필요하다. 커밋까지만 해 두고 브랜치명(`feature/responsive-nav-shell`)과 커밋 목록을 보고한다.
