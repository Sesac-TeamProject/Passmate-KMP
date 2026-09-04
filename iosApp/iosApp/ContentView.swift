import SwiftUI
import Shared

// 하단 4탭 셸 — NavigationView(stack) 하나가 TabView를 감싸는 단일 [Route] 스택. push된 화면은 TabView 전체를 덮어 탭 바가 숨는다
// (규칙 §2-1, iOS 15 호환 스펙 2026-08-31 §2). 앱 시작 기본 진입은 항상 Home(게스트 포함, 규칙 §2-1-1)
struct ContentView: View {
    @StateObject private var shellViewModel = AppShellViewModel(
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    @State private var selectedTab: AppTab = .home

    // 탭 루트 위에 쌓인 push 경로 — 탭별 보존 없음(push 중엔 탭 바가 숨어 전환 불가, Android·Desktop과 동일)
    @State private var path: [Route] = []

    // 로그인·로그아웃·탈퇴 시 증가 — NavigationView 전체를 재생성해 탭 루트(JoinView·MyInfoView 등)가 세션 상태를 다시 읽게 한다 (규칙 §8)
    @State private var sessionGeneration = 0

    var body: some View {
        NavigationView {
            // 탭바는 겹치지 않고 자리를 차지한다 — ZStack으로 덮으면 탭 루트 콘텐츠의
            // 마지막 줄·버튼이 탭바 밑으로 들어간다(M-01 로그인 안내·M-13 + 버튼·M-12 하단)
            VStack(spacing: 0) {
            TabView(selection: tabSelection) {
                // 홈 탭 = 입장 폼 인라인 (M-01 v6) — JoinView 재사용
                JoinView(
                    initialPin: nil,
                    onJoined: { pin in path.append(.waiting(pin: pin)) },
                    onPaymentRequired: { pin in path.append(.payment(pin: pin)) },
                    onSignInRequested: { pushSignIn(pendingRoute: nil, path: $path) },
                    onSignInRequiredForPaidRoom: { pin in
                        pushSignIn(pendingRoute: .payment(pin: pin), path: $path)
                    }
                )
                .tabItem { Text(AppTab.home.label) }
                .tag(AppTab.home)

                HostedRoomsView(
                    onRequireSignIn: { pushSignIn(pendingRoute: .hostedRooms, path: $path) },
                    onOpenReputation: { path.append(.reputation) },
                    onOpenRoomReport: { roomId in path.append(.roomReport(roomId: roomId)) },
                    onOpenSessionControl: { roomId, pin in path.append(.sessionControl(roomId: roomId, pin: pin)) }
                )
                .tabItem { Text(AppTab.hostedRooms.label) }
                .tag(AppTab.hostedRooms)

                JoinedRoomsView(
                    onRequireSignIn: { pushSignIn(pendingRoute: .joinedRooms, path: $path) },
                    onOpenReport: { roomId in path.append(.result(roomId: roomId)) },
                    onRejoin: { pin in path.append(.waiting(pin: pin)) },
                    // 빈 상태 CTA — 홈 탭이 곧 PIN 입장 폼. 탭 전환은 셸 가드를 거친다 (규칙 §2-1-1)
                    onOpenPinEntry: { shellViewModel.action(.selectTab(.home)) }
                )
                .tabItem { Text(AppTab.joinedRooms.label) }
                .tag(AppTab.joinedRooms)

                MyInfoView(
                    onRequireSignIn: { pushSignIn(pendingRoute: .myInfo, path: $path) },
                    onOpenReputation: { path.append(.reputation) },
                    onOpenCoinHistory: { path.append(.coinHistory) },
                    onOpenCharge: { path.append(.coinCharge) },
                    onOpenEarnings: { path.append(.earnings) },
                    onOpenDeleteAccount: { path.append(.deleteAccount) },
                    onOpenEditProfile: { path.append(.editProfile) },
                    onOpenPaymentMethod: { path.append(.paymentMethod) },
                    onOpenSettlementAccount: { path.append(.settlementAccount) },
                    onOpenNotifications: { path.append(.notificationSettings) },
                    onSignedOut: {
                        path = []
                        selectedTab = .home
                        sessionGeneration += 1
                    }
                )
                .tabItem { Text(AppTab.myInfo.label) }
                .tag(AppTab.myInfo)
            }
            .passmateHidesNativeTabBar()
            .navigationBarHidden(true)
            .background(
                NavigationLink(isActive: isStackActive) {
                    RouteStackLevel(path: $path, index: 0) { route, path in
                        // 시안이 탭바를 유지하는 화면(M-12-x·M-14)에서는 push 위에도 탭바를 그린다.
                        // push가 TabView 전체를 덮는 구조라 화면마다 직접 얹어야 한다 (규칙 §2-1)
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
                    }
                } label: {
                    EmptyView()
                }
                .isDetailLink(false)
            )
            .frame(maxHeight: .infinity)
            // 시안 v6 nav/4탭 — 기본 탭 바 대신 Compose와 같은 커스텀 바를 그린다 (규칙 §14)
            PassmateBottomTabBar(
                selectedTab: selectedTab,
                onSelectTab: { shellViewModel.action(.selectTab($0)) }
            )
            }
        }
        .navigationViewStyle(.stack)
        .id(sessionGeneration)
        .tint(PassmateColors.primary)
        .onReceive(shellViewModel.event) { event in
            switch event {
            case let .navigateToTab(tab):
                selectedTab = tab
            case .requireSignIn:
                // 탭 가드 — pendingRoute는 셸이 이미 목적 탭으로 저장했다. pushSignIn을 거치면 nil로 덮어쓴다 (스펙 §2-2)
                path.append(.signIn)
            case let .resumePendingRoute(route):
                resume(to: route)
            case .navigateToHome:
                path = []
                selectedTab = .home
                sessionGeneration += 1
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

    // 루트 링크 활성 여부(경로가 비어 있지 않으면 push). 시스템 pop으로 false가 되면 경로를 비운다
    private var isStackActive: Binding<Bool> {
        Binding(
            get: { !path.isEmpty },
            set: { isActive in
                if !isActive && !path.isEmpty {
                    path = []
                }
            }
        )
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
                onRequireSignIn: { pushSignIn(pendingRoute: .roomList, path: path) }
            )
        case .signIn:
            SignInView(
                onSignedIn: { shellViewModel.action(.resumeAfterSignIn) },
                onGuestEnter: { path.wrappedValue = [] }
            )
        case let .join(pin):
            JoinView(
                initialPin: pin,
                onJoined: { pin in path.wrappedValue.append(.waiting(pin: pin)) },
                onPaymentRequired: { pin in path.wrappedValue.append(.payment(pin: pin)) },
                onSignInRequested: { pushSignIn(pendingRoute: nil, path: path) },
                onSignInRequiredForPaidRoom: { pin in
                    pushSignIn(pendingRoute: .payment(pin: pin), path: path)
                },
                onBack: { popOnce(path) }
            )
        case let .payment(pin):
            PaymentView(
                pin: pin,
                onEnterRoom: { pin in path.wrappedValue.append(.waiting(pin: pin)) },
                onSignInRequired: { pushSignIn(pendingRoute: .payment(pin: pin), path: path) },
                onBack: { popOnce(path) }
            )
        case .coinHistory:
            CoinHistoryView(
                onBack: { popOnce(path) },
                onOpenCoinCharge: { path.wrappedValue.append(.coinCharge) }
            )
        case .coinCharge:
            CoinChargeView(onBack: { popOnce(path) })
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
                    // 세션 플로우 엔트리(Join·Payment·Waiting·Play)만 제거, 탭 루트 유지 (규칙 §2-1-2, 스펙 §1-5)
                    path.wrappedValue.removeAll { $0.isSessionRoute }
                    path.wrappedValue.append(.result(roomId: roomId))
                },
                // 풀이 중 로그인은 토큰 신원이 바뀌어 복귀 대상에서 제외한다 (스펙 §8-1)
                onOpenSignup: { pushSignIn(pendingRoute: nil, path: path) }
            )
        case let .result(roomId):
            ResultView(
                roomId: roomId,
                onClickHome: { path.wrappedValue = [] },
                onNavigateToSignup: { pushSignIn(pendingRoute: .result(roomId: roomId), path: path) }
            )
        case .reputation:
            ReputationView(
                onRequireSignIn: { pushSignIn(pendingRoute: .reputation, path: path) },
                onBack: { popOnce(path) }
            )
        case .earnings:
            EarningsView(
                onRequireSignIn: { pushSignIn(pendingRoute: .earnings, path: path) },
                onOpenSettlementAccount: { path.wrappedValue.append(.settlementAccount) },
                // 빈 상태 CTA — 방 개설 진입점인 「내가 만든 방」 탭으로. 탭 전환은 셸 가드를 거친다 (규칙 §2-1-1)
                onOpenHostedRooms: {
                    path.wrappedValue = []
                    shellViewModel.action(.selectTab(.hostedRooms))
                },
                onOpenCoinHistory: { path.wrappedValue.append(.coinHistory) },
                onBack: { popOnce(path) }
            )
        case let .sessionControl(roomId, pin):
            SessionControlView(
                roomId: roomId,
                pin: pin,
                onRequireSignIn: { pushSignIn(pendingRoute: .sessionControl(roomId: roomId, pin: pin), path: path) },
                onSessionEnded: { roomId in path.wrappedValue.append(.roomReport(roomId: roomId)) },
                onBack: { popOnce(path) }
            )
        case let .roomReport(roomId):
            RoomReportView(
                roomId: roomId,
                onRequireSignIn: { pushSignIn(pendingRoute: .roomReport(roomId: roomId), path: path) },
                onBack: { popOnce(path) }
            )
        case .deleteAccount:
            DeleteAccountView(
                onAccountDeleted: {
                    path.wrappedValue = []
                    selectedTab = .home
                    sessionGeneration += 1
                },
                onBack: { popOnce(path) }
            )
        case .editProfile:
            EditProfileView(
                onBack: { popOnce(path) },
                onOpenCharacterEdit: { path.wrappedValue.append(.characterEdit) }
            )
        case .characterEdit:
            CharacterEditView(onBack: { popOnce(path) })
        case .settlementAccount:
            SettlementAccountView(onBack: { popOnce(path) })
        case .paymentMethod:
            PaymentMethodView(onBack: { popOnce(path) })
        case .notificationSettings:
            NotificationSettingsView(onBack: { popOnce(path) })
        }
    }

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
