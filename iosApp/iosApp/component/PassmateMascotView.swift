import SwiftUI

// 마스코트 '패시' — Compose PassmateMascot의 iOS 미러다.
//
// 레이아웃 박스는 시안 프레임(120x132 비율)이고, PASS 배지·컨페티·생각 방울 같은 장식은 그 밖으로
// 번진다. 시안 인스턴스도 오버플로를 자르지 않으므로 같은 규격을 지킨다 — 그래서 에셋은 프레임보다 큰
// 공통 캔버스(144x156)이고, 여기서 캔버스만큼 키운 뒤 번짐만큼 당겨 프레임을 맞춘다.
//
// 프레임 크기를 .frame이 아니라 파라미터로 받는 이유: GeometryReader로 되읽으면 부모가 크기를
// 주지 않을 때 조용히 부모를 꽉 채운다(Compose 미러는 대신 무한대 제약으로 터진다).
// 크기는 시안 인스턴스 크기를 그대로 준다 (예: M-02는 60x66)
struct PassmateMascotView: View {
    private static let frameWidth: CGFloat = 120
    private static let frameHeight: CGFloat = 132
    private static let canvasWidth: CGFloat = 144
    private static let canvasHeight: CGFloat = 156
    private static let bleedLeft: CGFloat = 16
    private static let bleedTop: CGFloat = 24

    let mascot: PassmateMascots

    let width: CGFloat

    let height: CGFloat

    var body: some View {
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
            .frame(width: width, height: height, alignment: .topLeading)
            .accessibilityLabel("패스메이트 마스코트 패시")
    }
}
