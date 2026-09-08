import SwiftUI

// 유료 방 표시 칩 "₩ 유료" — 시안 RoomType 컴포넌트(M-11 유료 방 입장 · M-T4 정산 내역 · M-10 프로필 시트).
// Compose component/PaidRoomChip.kt와 1:1 미러
struct PaidRoomChipView: View {
    var body: some View {
        Text("₩ 유료")
            .font(.system(size: 14, weight: .medium))
            .kerning(-0.28)
            .foregroundColor(PassmateColors.weakTopicText)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(PassmateColors.weakTopicBg)
            .clipShape(Capsule())
    }
}
