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
            .padding(.top, 8)
            // 하단 여백은 홈 인디케이터 세이프에어리어가 이미 준다.
            // 여기에 12을 더 얹으면 실기기에서 탭바가 뜬다 — 4로 줄인다 (Compose와 같은 값)
            .padding(.bottom, 4)
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
