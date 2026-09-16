import SwiftUI
import UIKit

// iOS 15 호환 배열 기반 push — [Route]의 한 레벨(path[index])을 그리고, 다음 레벨로 가는 숨은 NavigationLink를 정확히 1개 가진다.
// NavigationStack(path:) 대체. iOS 15 NavigationView는 한 뷰에 링크가 여럿이면 오동작하므로 레벨을 재귀로 쌓는다 (스펙 2026-08-31 §2-3, 규칙 §2-1)
struct RouteStackLevel<Destination: View>: View {
    @Binding var path: [Route]

    let index: Int

    let destination: (Route, Binding<[Route]>) -> Destination

    // 이 레벨이 마지막으로 그린 라우트 — 경로가 줄어 밀려나가는(pop) 동안 빈 화면이 되지 않게 유지한다
    @State private var shownRoute: Route?

    // 이 레벨로 들어오는 push 전환이 끝났는가. 전환 도중에 다음 링크를 켜면 UIKit이 그 push를 조용히 버리고
    // 경로와 화면이 어긋난다 — 재입장 직후 RUNNING이라 곧바로 풀이로 넘기면 대기실에 멈췄다 (실기기 A-4)
    @State private var hasSettled = false

    private var currentRoute: Route? {
        if index < path.count {
            return path[index]
        } else {
            return shownRoute
        }
    }

    // 다음 레벨 링크 활성 여부. 전환이 끝나기 전에 쌓인 경로는 끝나는 순간 이어서 연다.
    // 시스템 pop으로 false가 되면 배열을 잘라 동기화한다
    private var isNextActive: Binding<Bool> {
        Binding(
            get: { hasSettled && path.count > index + 1 },
            set: { isActive in
                if !isActive && path.count > index + 1 {
                    path.removeSubrange((index + 1)...)
                }
            }
        )
    }

    private func syncShownRoute(_ newPath: [Route]) {
        if index < newPath.count {
            shownRoute = newPath[index]
        }
    }

    // 다음 레벨 링크는 이 레벨에 라우트가 있을 때만 만든다 — 경로 끝에서 재귀를 끊어 깊이를 path.count + 1로 묶는다
    @ViewBuilder
    private var nextLevelLink: some View {
        if currentRoute != nil {
            NavigationLink(isActive: isNextActive) {
                RouteStackLevel(path: $path, index: index + 1, destination: destination)
            } label: {
                EmptyView()
            }
            .isDetailLink(false)
        } else {
            EmptyView()
        }
    }

    var body: some View {
        Group {
            if let route = currentRoute {
                destination(route, $path)
            } else {
                EmptyView()
            }
        }
        .passmateHidesNativeNavigationBar()
        .navigationBarBackButtonHidden(true)
        .background(nextLevelLink)
        .background(PushTransitionEndReporter { hasSettled = true })
        .onAppear { syncShownRoute(path) }
        .onChange(of: path) { syncShownRoute($0) }
    }

    init(
        path: Binding<[Route]>,
        index: Int,
        @ViewBuilder destination: @escaping (Route, Binding<[Route]>) -> Destination
    ) {
        self._path = path
        self.index = index
        self.destination = destination
    }
}

// push 전환이 끝난 순간을 알린다. SwiftUI onAppear는 전환이 시작될 때 불려(실측 append 후 약 40ms) 쓸 수 없고,
// 뷰 뒤에 심은 빈 컨트롤러의 viewDidAppear는 UIKit이 전환을 마친 뒤에 부른다
private struct PushTransitionEndReporter: UIViewControllerRepresentable {
    let onTransitionEnd: () -> Void

    func makeUIViewController(context: Context) -> TransitionEndReportingController {
        TransitionEndReportingController(onTransitionEnd: onTransitionEnd)
    }

    func updateUIViewController(_ uiViewController: TransitionEndReportingController, context: Context) {
        uiViewController.onTransitionEnd = onTransitionEnd
    }
}

private final class TransitionEndReportingController: UIViewController {
    var onTransitionEnd: () -> Void

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        onTransitionEnd()
    }

    init(onTransitionEnd: @escaping () -> Void) {
        self.onTransitionEnd = onTransitionEnd
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        nil
    }
}
