# 홈 셸 + 하단 4탭 설계 (M-01 v6 · M-08 · M-12)

**작성**: 2026-08-30 · **브랜치**: `feature/home` (develop `44df2f3` 기반) · **담당**: 서승혁
**시안**: 피그마 "UI 디자인"(v6) — M-01 v6 홈·입장, M-08 참여한 방, M-12 마이(내 정보 관리), M-12-x 하위 프레임
**관계 문서**: `docs/Passmate_코드_패턴_규칙.md`(§2-1·§7·§8·§11-1), `docs/Passmate_아키텍처_설계.md`(§4·§5)

## 0. 결정 요약

| # | 결정 | 근거 |
|---|---|---|
| 1 | 하단 탭 4개: 홈 / 내가 만든 방 / 참여한 방 / 마이 | 피그마 v6 확정(2026-08-28 사용자 결정). 팀원의 T122 "보류"는 본 작업에서 해제 |
| 2 | 게스트가 로그인 필수 탭(내가 만든 방·참여한 방·마이)을 누르면 **화면을 열지 않고 `SignIn`으로** 이동 | 사용자 결정 2026-08-30 |
| 3 | 홈 탭 = 입장 폼 **인라인**(기존 `JoinScreen`/`JoinView` 재사용) | M-01 v6 프레임이 PIN·QR·닉네임·캐릭터·입장하기·로그인 행으로 구성 — 기존 Join과 1:1 일치 |
| 4 | "내가 만든 방" 탭은 기존 `HostedRoomsScreen` 그대로 노출 | 사용자 결정(8/28 "MVP 제외"를 뒤집음 — 이미 구현돼 있음) |
| 5 | 방 찾기(`RoomList`) 진입점 제거, 라우트·화면은 유지 | 시안에 목록 화면 없음 |
| 6 | 셸 방식 = **탭 셸을 기존 push 라우트 위에 씌움**(접근법 A) | 변경 폭 최소, 세션 플로우 규칙 무영향 |
| 7 | 비밀번호 변경 행 제외 | 로그인이 OAuth뿐, REST 계약에 비밀번호 API 없음(규칙 §13). 계약 갱신 제안만 전달 |
| 8 | 우상단 "설정" → 축소된 `Settings`(회원 탈퇴만) | "설정" 프레임 없음. 마이 루트에서 닿지 않는 M-12-12 탈퇴를 여기에 둠 |

## 1. 라우트와 셸

### 1-1. 라우트 (규칙 §2-1-1 갱신)

- 탭 루트 4개: `Home`(홈) · `HostedRooms`(내가 만든 방, 기존) · `JoinedRooms`(참여한 방, **신규** `joinedRooms`) · `MyInfo`(마이, 기존 이름 유지 — 규칙 §8 "MyInfo=로그인 필수" 그대로)
- 나머지 라우트 무변경. `RoomList`는 유지하되 진입점 없음.
- `NavigateToJoin(pin = null)` → `NavigateToHome`과 동일하게 처리(홈 탭이 곧 입장 폼). `join?pin=`(QR·딥링크·방 목록 참여)은 기존대로 push.
- 앱 시작 기본 진입은 `Home`(게스트 포함) — 규칙 §2-1-1 유지.
- `HostedRooms`는 탭 루트 전용이 된다(마이 루트에 진입 행 없음) → `HostedRoomsScreen`/`HostedRoomsView`의 상단 뒤로가기 버튼 제거. 그 외 내용 무변경.

### 1-2. 탭 바 표시 규칙

현재 목적지가 탭 루트 4개 중 하나일 때만 하단 탭 바를 표시한다. push된 화면(Join(pin)·Waiting·Play·Result·Payment·CoinHistory·Reputation·RoomReport·SessionControl·Earnings·Settings)과 시트·다이얼로그에서는 숨긴다. 3플랫폼 동일.

### 1-3. 게스트 가드 (셸 소유)

- `AppShellViewModel`(composeApp `mvi/MviViewModel` 상속, iOS Swift 미러) — 의존: `IsSignedInUseCase`
  - `uiState`: `AppShellUiState(isSignedIn: Boolean)`
  - `action`: `SelectTab(tab)` — 탭을 누를 때마다 `IsSignedInUseCase`를 동기 조회하므로 별도 Refresh가 필요 없다
  - `event`: `NavigateToTab(tab)` · `RequireSignIn`
  - 규칙: `tab.requiresSignIn && !isSignedIn` → `RequireSignIn`, 아니면 `NavigateToTab`. `Home`은 항상 통과.
- `AppTab` enum(`HOME`·`HOSTED_ROOMS`·`JOINED_ROOMS`·`MY_INFO`, `requiresSignIn`, 라벨)은 composeApp `navigation/`에 둔다. Swift 미러 동일.
- 기존 화면 VM의 `RequireSignIn`(`HostedRooms`·`JoinedRooms`·`MyInfo`)은 딥링크 직접 진입 대비 보험으로 유지.
- `observeCurrentUser` 스트림(규칙 §8)은 아직 shared에 없다. 이번에는 탭 탭 시·`Refresh` 시 동기 조회로 충분 — 스트림 도입은 범위 밖.

### 1-4. 플랫폼별 셸

| 플랫폼 | 구현 |
|---|---|
| Android | `NavHost` 하나 유지 + `Scaffold(bottomBar = PassmateBottomTabBar)`. 현재 목적지(`currentBackStackEntryAsState`)가 탭 루트일 때만 바 표시. 탭 전환: `navigate(tab.route) { popUpTo(Home) { saveState = true }; launchSingleTop = true; restoreState = true }` |
| Desktop | `routeStack` 유지. 탭 전환 = 스택을 `[탭 루트]`로 교체(단일 스택, 탭별 보존 없음). 최상단이 탭 루트일 때만 바 표시 |
| iOS | `TabView(selection:)` 4개, 탭마다 `NavigationStack(path)`. `ContentView.destinationView(for:)`를 탭이 공유하는 `@ViewBuilder` 함수로 추출(내용 무변경). 로그인 필수 탭 선택은 `AppShellViewModel` 판정 후 `selection` 변경 또는 `.signIn` push |

- 탭 바 컴포넌트: Compose `component/PassmateBottomTabBar.kt`(Android·Desktop 공유). iOS는 네이티브 `TabView` 탭 바(SF Symbols, tint Primary)를 쓴다.

### 1-5. 세션 플로우 접점 (규칙 §2-1-2)

`NavigateToResult` 시 백스택 클리어 범위를 "Home까지"에서 **"Join(pin)·Payment·Waiting·Play 엔트리만 제거"**로 바꾼다. 참여한 방 탭의 "다시 들어가기"→Waiting→Play→Result, "리포트"→Result에서 탭 루트가 유지돼야 한다.

- Android: Waiting이 스택에 있으면 `popBackStack(Route.Waiting.route, inclusive = true)`, 있었다면 이어서 Payment·Join(pin) 순으로 `popBackStack`을 호출해 함께 제거. 없으면 단순 push.
- Desktop: `isSessionFlow()`가 걸러내는 세션 플로우 엔트리 집합을 `Join·Payment·Waiting·Play`로 확장하고, `removeAll { it.isSessionFlow() }`로 한 번에 제거.
- iOS: 세션 종료 경로(`PlayView.onOpenResult`)는 `path = [.result]`가 아니라 **`isSessionRoute`(Join·Payment·Waiting·Play) 엔트리만 제거 후 `.result` 추가**; 리포트 경로(`JoinedRooms`)는 `path.append(.result)`.

## 2. 홈 탭 (M-01 v6)

- `Route.Home`이 `JoinScreen()` / `JoinView(initialPin: nil)`를 렌더한다. Join에는 원래 뒤로가기 버튼이 없어 `isTabRoot` 파라미터가 불필요하다(구현 중 확인).
- 임시 `HomeScreen.kt`, iOS `HomeView.swift`·`HomeViewModel.swift`·`HomeUiState.swift`·`HomeAction.swift` 삭제(pbxproj 참조 제거).
- 제외: "방 찾기"·"내 학습 기록"·"로그인" 링크(각각 시안 없음·참여한 방 탭·Join 폼 로그인 행이 대체).
- `SignInScreen`의 `SignInCompleted`·`GuestEnterRequested`는 둘 다 홈 탭으로 수렴(기존 `NavigateToHome`/`NavigateToJoin()` 의미 유지). pendingRoute는 범위 밖.

## 3. 참여한 방 탭 (M-08)

- 기존 `ui/mypage/MyInfo{Screen,ViewModel,UiState,Action,Event}.kt` → `JoinedRooms*`로 **git rename**. iOS `ui/mypage/MyInfoView*` → `JoinedRoomsView*` 동일.
- `JoinedRoomsViewModel` 의존 = `GetMyPageUseCase` · `IsSignedInUseCase`(현재와 동일). shared 무변경.
- 화면(위→아래): 큰 제목 "참여한 방"(뒤로가기 없음) → 진행 중 카드(다시 들어가기) → 요약 카드(정답률 링·N회 참여·평균 순위·추세 문구) → 보완할 주제 칩 → 참여 목록(순위 뱃지·제목·날짜·문항 수·점수·리포트) → 더 보기 → 빈/에러 상태. 전부 기존 컴포저블 재사용.
- 제거: 상단 "설정" 버튼, `HostedRoomsRow`·`EarningsRow`·`ReputationRow`·`CoinHistoryRow`, `Action`/`Event`의 `OpenHostedRooms`·`OpenEarnings`·`OpenReputation`·`OpenCoinHistory`·`OpenSettings`.
- 남는 이벤트: `RequireSignIn` · `OpenReport(roomId)`→`NavigateToResult` · `Rejoin(pin)`→`NavigateToWaiting` · `ShowNotice`.

## 4. 마이 탭 (M-12)

- 기존 `ui/mypage/Settings{Screen,ViewModel,UiState,Action,Event}.kt` → `MyInfo*`로 **git rename**(라우트 `MyInfo`). iOS `SettingsView*` → `MyInfoView*`. 축소된 `Settings*`는 새로 작성(§4-4).

### 4-1. 화면 구성과 데이터 출처 (API 3회)

| 영역 | 내용 | 출처 |
|---|---|---|
| 헤더 | 큰 제목 "마이" + 우상단 "설정" 텍스트 버튼 → `NavigateToSettings` | — |
| 프로필 카드 | 아바타(`StudentAvatar`)·닉네임·`Lv.N 라벨` 칩(`LevelEmblem`)·"참여한 방 N · 내가 만든 방 N". **카드 탭 → `NavigateToReputation`(M-09)** | `GetMyProfileUseCase` → `UserProfile` |
| 계정 카드 | 닉네임 변경 › · 내 캐릭터 변경 › — 둘 다 `EditProfileSheet` 열기 | 〃 |
| 코인 카드 | 보유 코인 "1,200 C · 유료 방 참가비에 사용" + **코인 충전** 버튼(4번 작업 전까지 `ShowNotice("코인 충전은 준비 중이에요")`) · 결제 수단 관리 ›(`PaymentMethodSheet`, 부제 = 기본 수단 라벨 + " · 포트원 안전결제") · 코인 내역 보기 ›(`NavigateToCoinHistory`, 부제 = `CoinBalance.recent`에서 "최근 8/22 -10,000 C") | `UserProfile.coins`, `GetMyCoinsUseCase` → `CoinBalance.defaultMethod` |
| 정산 카드 | 정산 계좌 변경 ›(`SettlementAccountSheet`, 부제 = "은행 마스킹번호", 미등록이면 "계좌를 등록해 주세요") · 이번 달 정산 예정 내역 ›(`NavigateToEarnings`, 부제 = "₩금액 · 지급일 지급", 없으면 "정산 예정 없음") | `GetEarningsUseCase` → `Earnings.account`·`nextPayout` |
| 알림 행 | 알림 설정 ›(`NotificationSettingsSheet`), 부제 고정 "세션 시작 · 별점 요청 · 정산" | — |
| 하단 | **로그아웃** 버튼 → 확인 다이얼로그(기존) → `ConfirmSignOut` → `SignedOut` 이벤트 → `NavigateToHome` | `SignOutUseCase` |

시안과 다른 단순화(의도적): 알림 부제는 고정 문구(추가 API 호출 회피).

### 4-2. 상태 (`MyInfoUiState`)

```
isLoading · loadFailed(프로필 실패 = 전체 에러+재시도)
profile: UserProfile?
defaultMethod: PaymentMethod? · isCoinInfoFailed: Boolean
settlementAccount: SettlementAccountSummary? · nextPayout: NextPayout? · isEarningsFailed: Boolean
isProcessing: Boolean (로그아웃 in-flight, 규칙 §9)
```

코인·정산은 프로필과 독립으로 로드한다. 실패 시 해당 카드에만 "불러오지 못했어요"를 표시하고 나머지는 정상 렌더한다.

### 4-3. ViewModel

- `MyInfoViewModel` 의존: `GetMyProfileUseCase` · `GetMyCoinsUseCase` · `GetEarningsUseCase` · `SignOutUseCase` · `IsSignedInUseCase`
- `Action`: `Enter` · `Retry` · `ClickProfile` · `ClickEditProfile` · `ClickCharge` · `ClickPaymentMethod` · `ClickCoinHistory` · `ClickSettlementAccount` · `ClickEarnings` · `ClickNotifications` · `ClickSettings` · `ConfirmSignOut` · `ProfileUpdated` · `AccountUpdated` · `Notice(message)`
- `Event`: `RequireSignIn` · `OpenReputation` · `OpenEditProfile(nickname, avatarId)` · `OpenPaymentMethod` · `OpenCoinHistory` · `OpenSettlementAccount` · `OpenEarnings` · `OpenNotifications` · `OpenSettings` · `SignedOut` · `ShowNotice(message)`
- 시트 4종의 표시 여부·생명주기는 `MyInfoScreen`(컨테이너)이 소유한다(규칙 §11-1). 시트 저장 완료 → `ProfileUpdated`/`AccountUpdated`로 카드 갱신.

### 4-4. 축소된 설정 화면 (`Settings*`, 신규)

- 제목 "설정", 뒤로가기, 행 1개: **회원 탈퇴** → 기존 확인 다이얼로그 → `DeleteAccountUseCase` → 409 시 서버 메시지 표시(기존 로직 이동).
- `SettingsViewModel` 의존: `DeleteAccountUseCase` · `IsSignedInUseCase`. `Event`: `RequireSignIn` · `AccountDeleted` · `ShowNotice`.
- M-12-12 체크박스 전체 화면은 범위 밖.

## 5. iOS 미러

- `ContentView`: `TabView` + 탭별 `NavigationStack`. `destinationView(for:)`를 공유 함수로 추출. 탭 선택은 `AppShellViewModel.swift`(`KoinHelper.shared.isSignedInUseCase()` 재사용) 판정을 거친다.
- 파일: `AppShellViewModel/UiState/Action/Event.swift`, `navigation/AppTab.swift`, `component/PassmateBottomTabBar.swift`, `JoinView`에 `isTabRoot`, `MyInfoView*`→`JoinedRoomsView*`, `SettingsView*`→`MyInfoView*`, 새 `SettingsView*`, `HomeView*` 삭제.
- pbxproj: 신규 파일 idx **145**부터 등록, 삭제 파일 참조 제거, **그룹 ID(`A10120xx`) 중복 재검사**(fix/ios-build에서 충돌 있었음).
- 프리뷰: `JoinedRoomsView`·`MyInfoView`·`SettingsView`·`PassmateBottomTabBar`에 콘텐츠 뷰 기준 `#Preview`.

## 6. 문서·정리

- `docs/Passmate_코드_패턴_규칙.md` §2-1-1: 탭 루트 4개와 `JoinedRooms` 추가, "`Home` = 입장 폼(인라인)", §2-1-2 Result 진입 시 클리어 범위를 "Join·Waiting·Play 엔트리"로 명시. PR에서 홍희표 님 리뷰 요청.
- `docs/Passmate_Mac_검증_체크리스트.md` §9 추가: 홈 셸·탭 빌드·스모크 항목, 다음 가용 idx 갱신.
- `SettingsScreen.kt`·`SettingsView.swift`의 "4탭 보류" 주석 제거.
- 삭제: `HomeScreen.kt`, iOS `Home*` 4파일. `NavigateToRoomList`는 코드에 남김.
- `viewModelModule`: `AppShellViewModel`·`JoinedRoomsViewModel`·`MyInfoViewModel`·`SettingsViewModel` factory 갱신. `KoinHelper`는 기존 getter로 충분(신규 UseCase 없음).

## 7. 테스트 (composeApp `jvmTest`, fake UseCase 생성자 주입)

- `AppShellViewModelTest`: 회원→`NavigateToTab` / 게스트+로그인 필수 탭→`RequireSignIn` / 게스트+홈→통과 / `Refresh` 후 상태 재계산
- `MyInfoViewModelTest`: 프로필 실패→`loadFailed` / 코인만 실패→`isCoinInfoFailed`만 true, 프로필 렌더 / 정산만 실패→`isEarningsFailed` / `ConfirmSignOut`→`isProcessing` true→`SignedOut` / 게스트→`RequireSignIn`
- `JoinedRoomsViewModelTest`: 게스트→`RequireSignIn` / 첫 로드·더 보기 커서
- `KoinWiringTest` 갱신. shared 무변경(테스트 추가 없음).

## 8. 검증 순서

1. `:composeApp:jvmTest` → 2. `:shared:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid` → 3. Desktop 실행 스모크(탭 전환·게스트 가드·홈 폼) → 4. iOS 시뮬레이터 빌드·실행 스모크 → 5. PR(`feature/home` → `develop`)

## 9. 범위 밖 (후속)

pendingRoute(3번) · 코인 충전 화면 M-12-4~6(4번) · 회원 탈퇴 체크박스 화면(M-12-12) · 비밀번호 변경(계약 갱신 제안 선행) · `observeCurrentUser` 스트림 · `RoomList` 진입점(시안 확정 후) · Android 탭 바 딥링크 복원
