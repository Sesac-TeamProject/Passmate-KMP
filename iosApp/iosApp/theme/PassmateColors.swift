import SwiftUI

// 시맨틱 토큰 — Compose PassmateColors.kt와 1:1 유지 (규칙 §11-2)
enum PassmateColors {
    static let primary = Color(hex: 0x17B884)

    static let primaryDeep = Color(hex: 0x0E8A63)

    static let backgroundMint = Color(hex: 0xEAF8F1)

    static let surface = Color(hex: 0xFFFFFF)

    // 입력 필드·비활성 칩 배경 (v6 M-01 PIN 박스·닉네임 필드)
    static let fieldGray = Color(hex: 0xF3F4F6)

    static let textPrimary = Color(hex: 0x1B1F24)

    static let textSecondary = Color(hex: 0x6B7280)

    static let textTertiary = Color(hex: 0xA0A6B0)

    static let border = Color(hex: 0xE5E7EB)

    // 진행 타이머·프로그레스 (v6 M-03 타이머 링·문항 세그먼트)
    static let timerAmber = Color(hex: 0xF3B440)

    // 짙은 잉크 그린 — 민트 배경 위 라벨 (v6 M-05 최종 결과)
    static let inkGreen = Color(hex: 0x0F3D2E)

    // 순위·선지 칩 파스텔 4색 + 대응 텍스트 (v6 M-03 선지·M-05 포디움/랭킹)
    static let chipBlue = Color(hex: 0xAAC6F6)

    static let chipBlueText = Color(hex: 0x173872)

    static let chipGold = Color(hex: 0xF7DBA1)

    static let chipGoldText = Color(hex: 0x6E4C06)

    static let chipGreen = Color(hex: 0xA9DEC3)

    static let chipGreenText = Color(hex: 0x14523B)

    static let chipOrange = Color(hex: 0xF8C6A4)

    static let chipOrangeText = Color(hex: 0x7A3A11)

    // 서드파티 브랜드 색상 (규칙 §11-2 예외 허용 대상을 토큰으로 관리)
    static let brandGoogleBlue = Color(hex: 0x4285F4)

    static let brandAppleBlack = Color(hex: 0x111111)
}

private extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: 1.0
        )
    }
}
