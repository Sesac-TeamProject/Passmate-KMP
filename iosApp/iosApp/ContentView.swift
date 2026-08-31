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
            TabView(selection: tabSelection) {
                // 홈 탭 = 입장 폼 인라인 (M-01 v6) — JoinView 재사용
                JoinView(
                    initialPin: nil,
                    onJoined: { pin in path.append(.waiting(pin: pin)) },
                    onPaymentRequired: { pin in path.append(.payment(pin: pin)) },
                    onSignInRequested: { path.append(.signIn) }
                )
                .tabItem { Label(AppTab.home.label, systemImage: AppTab.home.systemImage) }
                .tag(AppTab.home)

                HostedRoomsView(
                    onRequireSignIn: { path.append(.signIn) },
                    onOpenReputation: { path.append(.reputation) },
                    onOpenRoomReport: { roomId in path.append(.roomReport(roomId: roomId)) },
                    onOpenSessionControl: { roomId, pin in path.append(.sessionControl(roomId: roomId, pin: pin)) }
                )
                .tabItem { Label(AppTab.hostedRooms.label, systemImage: AppTab.hostedRooms.systemImage) }
                .tag(AppTab.hostedRooms)

                JoinedRoomsView(
                    onRequireSignIn: { path.append(.signIn) },
                    onOpenReport: { roomId in path.append(.result(roomId: roomId)) },
                    onRejoin: { pin in path.append(.waiting(pin: pin)) }
                )
                .tabItem { Label(AppTab.joinedRooms.label, systemImage: AppTab.joinedRooms.systemImage) }
                .tag(AppTab.joinedRooms)

                MyInfoView(
                    onRequireSignIn: { path.append(.signIn) },
                    onOpenReputation: { path.append(.reputation) },
                    onOpenCoinHistory: { path.append(.coinHistory) },
                    onOpenEarnings: { path.append(.earnings) },
                    onOpenSettings: { path.append(.settings) },
                    onSignedOut: {
                        path = []
                        selectedTab = .home
                        sessionGeneration += 1
                    }
                )
                .tabItem { Label(AppTab.myInfo.label, systemImage: AppTab.myInfo.systemImage) }
                .tag(AppTab.myInfo)
            }
            .navigationBarHidden(true)
            .background(
                NavigationLink(isActive: isStackActive) {
                    RouteStackLevel(path: $path, index: 0) { route, path in
                        destinationView(for: route, path: path)
                    }
                } label: {
                    EmptyView()
                }
                .isDetailLink(false)
            )
        }
        .navigationViewStyle(.stack)
        .id(sessionGeneration)
        .tint(PassmateColors.primary)
        .onReceive(shellViewModel.event) { event in
            switch event {
            case let .navigateToTab(tab):
                selectedTab = tab
            case .requireSignIn:
                // 탭 루트 위에 로그인 push — 로그인 후 pendingRoute 복귀는 후속 작업(최상단 교체로 구현, 스펙 §2-5)
                path.append(.signIn)
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
                onRequireSignIn: { path.wrappedValue.append(.signIn) }
            )
        case .signIn:
            SignInView(
                onSignedIn: { path.wrappedValue = []; sessionGeneration += 1 },
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
                    sessionGeneration += 1
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
