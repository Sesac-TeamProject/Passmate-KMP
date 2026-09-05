import SwiftUI

// 시안 v6의 대기 점 3개 — 대기실(M-02)과 문항 결과(M-04)가 같은 것을 쓴다.
// 비활성 점도 같은 민트를 옅게 쓴다 — border(연회색) x 0.4는 실기기에서 거의 안 보였다
struct PassmateWaitingDots: View {
    @State private var activeDot = 0

    private let timer = Timer.publish(every: 0.4, on: .main, in: .common).autoconnect()

    var body: some View {
        HStack(spacing: 6) {
            ForEach(0..<3, id: \.self) { index in
                Circle()
                    .fill(PassmateColors.primary)
                    .frame(width: 7, height: 7)
                    .opacity(index == activeDot ? 1.0 : 0.35)
            }
        }
        .onReceive(timer) { _ in
            activeDot = (activeDot + 1) % 3
        }
    }
}
