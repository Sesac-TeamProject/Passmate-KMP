import SwiftUI

// NavigationStack 셸 — 앱 시작 기본 진입은 항상 Home (규칙 §2-1-1)
struct ContentView: View {
    @State private var path: [Route] = []

    var body: some View {
        NavigationStack(path: $path) {
            HomeView(
                onJoinTapped: { path.append(.join(pin: nil)) },
                onSignInTapped: { path.append(.signIn) },
                onMyInfoTapped: { path.append(.myInfo) },
                onRoomListTapped: { path.append(.roomList) }
            )
            .navigationDestination(for: Route.self) { route in
                destinationView(for: route)
                    .navigationBarBackButtonHidden(true)
            }
        }
    }

    @ViewBuilder
    private func destinationView(for route: Route) -> some View {
        switch route {
        case .roomList:
            RoomListView(
                onOpenRoom: { pin in path.append(.join(pin: pin)) },
                onOpenPinEntry: { path.append(.join(pin: nil)) },
                onRequireSignIn: { path.append(.signIn) }
            )
        case .signIn:
            SignInView(
                onSignedIn: { path = [] },
                onGuestEnter: { path.append(.join(pin: nil)) }
            )
        case let .join(pin):
            JoinView(
                initialPin: pin,
                onJoined: { pin in path.append(.waiting(pin: pin)) },
                onPaymentRequired: { pin in path.append(.payment(pin: pin)) },
                onSignInRequested: { path.append(.signIn) },
                onBack: { popOnce() }
            )
        case let .payment(pin):
            PaymentView(
                pin: pin,
                onEnterRoom: { pin in path.append(.waiting(pin: pin)) },
                onSignInRequired: { path.append(.signIn) },
                onBack: { popOnce() }
            )
        case .coinHistory:
            CoinHistoryView(onBack: { popOnce() })
        case let .waiting(pin):
            WaitingView(
                pin: pin,
                onSessionStarted: { pin in path.append(.play(pin: pin)) },
                onRoomClosed: { path = [] },
                onLeft: { popOnce() }
            )
        case let .play(pin):
            PlayView(
                pin: pin,
                onLeft: { path = [] },
                onRoomClosed: { path = [] },
                onOpenResult: { roomId in path.append(.result(roomId: roomId)) },
                onOpenSignup: { path.append(.signIn) }
            )
        case let .result(roomId):
            ResultView(
                roomId: roomId,
                onClickHome: { path = [] },
                onNavigateToSignup: { path.append(.signIn) }
            )
        case .myInfo:
            MyInfoView(
                onRequireSignIn: { path.append(.signIn) },
                onOpenReport: { roomId in path.append(.result(roomId: roomId)) },
                onRejoin: { pin in path.append(.waiting(pin: pin)) },
                onOpenCoinHistory: { path.append(.coinHistory) },
                onOpenReputation: { path.append(.reputation) },
                onOpenHostedRooms: { path.append(.hostedRooms) },
                onOpenEarnings: { path.append(.earnings) },
                onOpenSettings: { path.append(.settings) },
                onBack: { popOnce() }
            )
        case .reputation:
            ReputationView(
                onRequireSignIn: { path.append(.signIn) },
                onBack: { popOnce() }
            )
        case .hostedRooms:
            HostedRoomsView(
                onRequireSignIn: { path.append(.signIn) },
                onOpenReputation: { path.append(.reputation) },
                onOpenRoomReport: { roomId in path.append(.roomReport(roomId: roomId)) },
                onOpenSessionControl: { roomId, pin in path.append(.sessionControl(roomId: roomId, pin: pin)) },
                onBack: { popOnce() }
            )
        case .earnings:
            EarningsView(
                onRequireSignIn: { path.append(.signIn) },
                onBack: { popOnce() }
            )
        case let .sessionControl(roomId, pin):
            SessionControlView(
                roomId: roomId,
                pin: pin,
                onRequireSignIn: { path.append(.signIn) },
                onSessionEnded: { roomId in path.append(.roomReport(roomId: roomId)) },
                onBack: { popOnce() }
            )
        case let .roomReport(roomId):
            RoomReportView(
                roomId: roomId,
                onRequireSignIn: { path.append(.signIn) },
                onBack: { popOnce() }
            )
        case .settings:
            SettingsView(
                onRequireSignIn: { path.append(.signIn) },
                onOpenCoinHistory: { path.append(.coinHistory) },
                onSignedOut: { path = [] },
                onBack: { popOnce() }
            )
        default:
            HomeView(
                onJoinTapped: { path.append(.join(pin: nil)) },
                onSignInTapped: { path.append(.signIn) }
            )
        }
    }

    private func popOnce() {
        if !path.isEmpty {
            path.removeLast()
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
