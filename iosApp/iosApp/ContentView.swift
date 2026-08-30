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
                    // 세션 플로우 엔트리(Join·Payment·Waiting·Play)만 제거, 탭 루트 유지 (규칙 §2-1-2, 스펙 §1-5)
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
