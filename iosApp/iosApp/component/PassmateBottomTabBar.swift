import SwiftUI

// 하단 4탭 바 — Compose component/PassmateBottomTabBar.kt의 iOS 미러다 (규칙 §14).
// iOS 기본 TabView 탭 바(알약 하이라이트·SF Symbols)는 시안 v6 nav/4탭과 다르므로 숨기고 이걸 그린다.
struct PassmateBottomTabBar: View {
    let selectedTab: AppTab?

    let onSelectTab: (AppTab) -> Void

    private let outerGapUnits = 6

    private let innerGapUnits = 5

    // 같은 폭의 Spacer를 units개 늘어놓아 비율 여백을 만든다 — SwiftUI Spacer에는 비율이 없다
    @ViewBuilder
    private func gap(units: Int) -> some View {
        ForEach(0..<units, id: \.self) { _ in
            Spacer(minLength: 0)
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(PassmateColors.border)
                .frame(height: 1)
            // 좌우 바깥 여백과 항목 사이 간격, 다섯 군데를 6:5:5:5:6으로 나눈다 (Compose weight 미러).
            // SwiftUI Spacer에는 비율이 없으므로 같은 폭의 Spacer를 그 수만큼 늘어놓아 비율을 만든다 —
            // HStack이 ForEach의 자식을 형제로 펴기 때문에 6개·5개가 그대로 6:5로 나뉜다
            HStack(spacing: 0) {
                gap(units: outerGapUnits)
                ForEach(Array(AppTab.allCases.enumerated()), id: \.element) { index, tab in
                    TabItemView(
                        tab: tab,
                        isSelected: tab == selectedTab,
                        onTap: { onSelectTab(tab) }
                    )
                    gap(units: index == AppTab.allCases.count - 1 ? outerGapUnits : innerGapUnits)
                }
            }
            // 시안(M-01) 탭바는 상단 10 · 항목 49 · 하단 14다. 하단 14는 시스템 바 자리를
            // 대신하는 값이라 실기기에서는 홈 인디케이터 세이프에어리어가 그 역할을 한다
            .padding(.top, 10)
            .padding(.bottom, 0)
        }
        .background(PassmateColors.surface)
    }
}

private struct TabItemView: View {
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
            // 좌우 여백을 두면 안쪽 간격에만 24가 더해져 6:5:5:5:6이 어긋난다 — 여백은 전부 Spacer가 쥔다
            .padding(.vertical, 4)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(tab.label)
    }
}
