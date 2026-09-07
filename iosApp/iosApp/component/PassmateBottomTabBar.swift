import SwiftUI

// 하단 4탭 바 — Compose component/PassmateBottomTabBar.kt의 iOS 미러다 (규칙 §14).
// iOS 기본 TabView 탭 바(알약 하이라이트·SF Symbols)는 시안 v6 nav/4탭과 다르므로 숨기고 이걸 그린다.
struct PassmateBottomTabBar: View {
    let selectedTab: AppTab?

    let onSelectTab: (AppTab) -> Void

    var body: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(PassmateColors.border)
                .frame(height: 1)
            HStack(spacing: 0) {
                ForEach(AppTab.allCases, id: \.self) { tab in
                    TabItemView(
                        tab: tab,
                        isSelected: tab == selectedTab,
                        onTap: { onSelectTab(tab) }
                    )
                    .frame(maxWidth: .infinity)
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
            .padding(.horizontal, 12)
            .padding(.vertical, 4)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(tab.label)
    }
}
