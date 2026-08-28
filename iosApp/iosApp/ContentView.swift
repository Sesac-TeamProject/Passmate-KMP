import SwiftUI

// NavigationStack 셸 — 앱 시작 기본 진입은 항상 Home (규칙 §2-1-1)
struct ContentView: View {
    @State private var path: [Route] = []

    var body: some View {
        NavigationStack(path: $path) {
            HomeView(
                onJoinTapped: { path.append(.join(pin: nil)) },
                onSignInTapped: { path.append(.signIn) }
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
        case .signIn:
            SignInView(
                onSignedIn: { path = [] },
                onGuestEnter: { path.append(.join(pin: nil)) }
            )
        case let .join(pin):
            JoinView(
                initialPin: pin,
                onJoined: { pin in path.append(.waiting(pin: pin)) },
                onSignInRequested: { path.append(.signIn) },
                onBack: { popOnce() }
            )
        case let .waiting(pin):
            WaitingView(
                pin: pin,
                onSessionStarted: { pin in path.append(.play(pin: pin)) },
                onRoomClosed: { path = [] },
                onLeft: { popOnce() }
            )
        case let .play(pin):
            PlayView(pin: pin)
        default:
            // Result·MyInfo·Payment·Settings 라우트는 담당 스토리에서 연결된다
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
