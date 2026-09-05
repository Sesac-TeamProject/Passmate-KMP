import SwiftUI

// 마스코트 '패시' — Compose PassmateMascot의 iOS 미러다.
//
// 레이아웃 박스는 시안 프레임(120x132)이고, PASS 배지·컨페티·생각 방울 같은 장식은 그 밖으로 번진다.
// 시안 인스턴스도 오버플로를 자르지 않으므로 같은 규격을 지킨다 — 그래서 에셋은 프레임보다 큰
// 공통 캔버스(144x156)이고, 여기서 캔버스만큼 키운 뒤 번짐만큼 왼쪽·위로 당겨 프레임을 맞춘다.
//
// 호출부는 .frame(width:height:)로 프레임 크기를 준다 (시안 인스턴스 크기 그대로 — 예: M-02는 60x66)
struct PassmateMascotView: View {
    private static let frameWidth: CGFloat = 120
    private static let frameHeight: CGFloat = 132
    private static let canvasWidth: CGFloat = 144
    private static let canvasHeight: CGFloat = 156
    private static let bleedLeft: CGFloat = 16
    private static let bleedTop: CGFloat = 24

    let mascot: PassmateMascots

    var body: some View {
        GeometryReader { geometry in
            let width = geometry.size.width
            let height = geometry.size.height

            Image(mascot.rawValue)
                .resizable()
                .frame(
                    width: width * (Self.canvasWidth / Self.frameWidth),
                    height: height * (Self.canvasHeight / Self.frameHeight)
                )
                .offset(
                    x: -width * (Self.bleedLeft / Self.frameWidth),
                    y: -height * (Self.bleedTop / Self.frameHeight)
                )
                .accessibilityLabel("패스메이트 마스코트 패시")
        }
    }
}
