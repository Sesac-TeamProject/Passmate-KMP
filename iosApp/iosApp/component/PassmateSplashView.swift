import SwiftUI

// 앱 실행 스플래시 (시안 M-00) — Compose PassmateSplash와 1:1 미러 (규칙 §14).
// 로고는 런치 스크린과 같은 에셋(LaunchLogo)을 쓴다.
struct PassmateSplashView: View {
    let versionLabel: String

    var body: some View {
        ZStack {
            PassmateColors.primary
                .ignoresSafeArea()

            VStack(spacing: 0) {
                Image("LaunchLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 104, height: 104)

                Text("PASSMATE")
                    .font(.system(size: 24, weight: .bold))
                    .kerning(3.36)
                    .foregroundColor(PassmateColors.surface)
                    .padding(.top, 24)

                Text("혼자 시작한 공부, 함께하는 합격까지.")
                    .font(.system(size: 14))
                    .kerning(-0.14)
                    .foregroundColor(PassmateColors.splashSubtleText)
                    .padding(.top, 18)
            }

            VStack {
                Spacer()
                Text(versionLabel)
                    .font(.system(size: 11))
                    .kerning(-0.11)
                    .foregroundColor(PassmateColors.splashFaintText)
                    .padding(.bottom, 40)
            }
        }
    }
}

#Preview("M-00 스플래시") {
    PassmateSplashView(versionLabel: "v1.0")
}
