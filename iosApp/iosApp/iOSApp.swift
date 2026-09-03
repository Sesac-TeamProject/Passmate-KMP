import SwiftUI
import Shared

// 스플래시(M-00) 표시 시간. 부트스트랩이 전부 동기라 대기 구간이 없어 의도적으로 유지한다
// (2026-09-03 팀 결정 — 시안의 "준비되는 즉시"와 다름). Compose App.kt와 같은 값을 쓴다.
private let splashDurationSeconds: TimeInterval = 2.0

// 화면에 보여줄 앱 버전. Android versionName("1.0")과 같은 값을 쓴다.
private let appVersionLabel = "v1.0"

@main
struct iOSApp: App {

    // 시스템 런치 스크린(UILaunchScreen)은 표시 시간을 앱이 제어할 수 없다 —
    // 첫 프레임이 준비되면 걷히므로, 이어받아 보여줄 인앱 스플래시가 따로 필요하다.
    @State private var isSplashVisible = true

    var body: some Scene {
        WindowGroup {
            // 스플래시는 라우트가 아니라 셸이 소유하는 오버레이다 (규칙 §11-1)
            if isSplashVisible {
                PassmateSplashView(versionLabel: appVersionLabel)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + splashDurationSeconds) {
                            isSplashVisible = false
                        }
                    }
            } else {
                ContentView()
            }
        }
    }

    init() {
        KoinHelper.shared.doInitKoin()
    }
}
