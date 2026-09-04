import SwiftUI

// 시안 "07 · 로딩 · 스켈레톤" 규격 구현. Compose PassmateSkeleton.kt와 1:1 유지.
//
// 규격 요지:
//  - 블록 색은 skeletonBlock, 보조는 skeletonBlockSoft. 민트 계열은 쓰지 않는다.
//  - 텍스트 줄 모서리 6, 카드는 원본과 같은 라운드, 아바타는 원형.
//  - 폭은 실제 글자의 60~90%로 서로 다르게 잡고 마지막 줄은 짧게.
//  - 반짝임은 왼쪽에서 오른쪽으로 1.2초 반복, 흰색 0 → 75% → 0 그라데이션.

private let shimmerDuration: Double = 1.2

private let shimmerHighlightOpacity: Double = 0.75

// 반짝임 띠 폭 — 블록 폭 대비 비율
private let shimmerBandRatio: CGFloat = 0.4

// 텍스트 줄 스켈레톤 기본 모서리
private let textLineRadius: CGFloat = 6

// 글자 한 줄·칩·버튼 자리를 메우는 블록. 크기는 호출부가 frame으로 준다.
struct PassmateSkeletonBlock: View {
    var cornerRadius: CGFloat = textLineRadius

    var color: Color = PassmateColors.skeletonBlock

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius)
            .fill(color)
            .passmateShimmer()
    }
}

// 카드 자리 — 실제 카드와 같은 테두리·라운드를 유지하고 안쪽만 블록으로 채운다.
struct PassmateSkeletonCard<Content: View>: View {
    var cornerRadius: CGFloat = 18

    @ViewBuilder let content: Content

    var body: some View {
        VStack(spacing: 0) {
            content
        }
        .frame(maxWidth: .infinity)
        .background(PassmateColors.surface)
        .overlay(
            RoundedRectangle(cornerRadius: cornerRadius)
                .stroke(PassmateColors.border, lineWidth: 1)
        )
        .cornerRadius(cornerRadius)
    }
}

extension View {
    func passmateShimmer() -> some View {
        modifier(PassmateShimmerModifier())
    }
}

// 지나가는 하이라이트 띠. 그리는 대상 위에 얹는다.
private struct PassmateShimmerModifier: ViewModifier {
    @State private var progress: CGFloat = 0

    func body(content: Content) -> some View {
        content
            .overlay(
                GeometryReader { proxy in
                    let bandWidth = proxy.size.width * shimmerBandRatio
                    let travel = proxy.size.width + bandWidth * 2

                    LinearGradient(
                        gradient: Gradient(colors: [
                            Color.white.opacity(0),
                            Color.white.opacity(shimmerHighlightOpacity),
                            Color.white.opacity(0)
                        ]),
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                    .frame(width: bandWidth)
                    .offset(x: -bandWidth + travel * progress)
                }
            )
            .clipped()
            .onAppear {
                withAnimation(.linear(duration: shimmerDuration).repeatForever(autoreverses: false)) {
                    progress = 1
                }
            }
    }
}
