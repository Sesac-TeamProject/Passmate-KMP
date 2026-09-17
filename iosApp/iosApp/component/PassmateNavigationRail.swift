import SwiftUI

// 좌측 4탭 레일 (넓은 창) — Compose component/PassmateNavigationRail.kt 미러 (규칙 §14).
// 하단 탭바를 그대로 세운 형태이고 항목은 위에서부터 쌓는다. 표시 여부 판정은 호출부(PassmateNavShell)가 한다.
// 상·하단 시스템 바 회피는 SwiftUI 기본 세이프에어리어가 해준다 (Compose의 statusBarsPadding에 해당)
struct PassmateNavigationRail: View {
    // Material3 NavigationRail 기본 폭. 오른쪽 1pt 구분선을 포함한 바깥 폭이다
    static let width: CGFloat = 80

    // 항목 사이 간격 — 하단바의 6:5:5:5:6 비율을 세로로 그대로 쓰면 높이 900 창에서 항목이 화면 전체로 흩어진다
    private static let itemGap: CGFloat = 8

    // 항목 묶음을 바 위 가장자리에서 띄우는 값 — 하단바의 .padding(.top, 10)과 같은 값을 쓴다
    private static let itemTopPadding: CGFloat = 10

    let selectedTab: AppTab?

    let onSelectTab: (AppTab) -> Void

    var body: some View {
        HStack(spacing: 0) {
            VStack(spacing: Self.itemGap) {
                ForEach(AppTab.allCases, id: \.self) { tab in
                    PassmateTabItemView(
                        tab: tab,
                        isSelected: tab == selectedTab,
                        onTap: { onSelectTab(tab) }
                    )
                }
            }
            // 항목은 위에서부터 쌓는다 — alignment: .top이 없으면 SwiftUI가 가운데로 놓는다
            // (Compose Arrangement.spacedBy(ITEM_GAP) 미러, 사용자 결정 2026-09-16)
            .padding(.top, Self.itemTopPadding)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(PassmateColors.surface)
            // 하단바의 위쪽 1pt 구분선을 90° 돌린 것
            Rectangle()
                .fill(PassmateColors.border)
                .frame(width: 1)
        }
        .frame(width: Self.width)
    }
}
