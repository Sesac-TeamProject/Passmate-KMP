import SwiftUI

// 탭 항목 하나 — Compose component/PassmateTabItem.kt 미러 (규칙 §14).
// 하단 탭바(PassmateBottomTabBar)와 좌측 레일(PassmateNavigationRail)이 공유한다
struct PassmateTabItemView: View {
    let tab: AppTab

    let isSelected: Bool

    let onTap: () -> Void

    private var color: Color {
        isSelected ? PassmateColors.primary : PassmateColors.textTertiary
    }

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 4) {
                PassmateIconView(icon: tab.icon, tint: color, size: 24)
                Text(tab.label)
                    .font(.system(size: 11, weight: isSelected ? .bold : .medium))
                    .kerning(-0.22)
                    .foregroundColor(color)
            }
            // 좌우 여백을 두면 하단바의 안쪽 간격에만 24가 더해져 6:5:5:5:6이 어긋난다 — 여백은 전부 부모가 쥔다
            .padding(.vertical, 4)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(tab.label)
    }
}
