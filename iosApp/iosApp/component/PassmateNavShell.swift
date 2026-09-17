import SwiftUI

// 내비게이션과 본문의 배치만 담당하는 셸 — Compose component/PassmateNavShell.kt 미러 (규칙 §14).
// selectedTab이 nil이면 레일도 하단바도 그리지 않는다 (규칙 §2-1-1).
// GeometryReader는 콘텐츠 크기로 줄지 않고 자식을 topLeading에 붙이므로 자식 frame을 명시 고정한다 —
// 안 하면 NavigationView(stack)·TabView 안에서 레이아웃이 흔들린다 (스펙 2026-09-16 §2-4)
struct PassmateNavShell<Content: View>: View {
    let selectedTab: AppTab?

    let onSelectTab: (AppTab) -> Void

    @ViewBuilder let content: () -> Content

    var body: some View {
        GeometryReader { geometry in
            shell(for: AppShellLayoutPolicy.layoutFor(width: geometry.size.width))
                .frame(width: geometry.size.width, height: geometry.size.height)
        }
    }

    // switch를 body에서 분리한다 — result builder 안의 지역 let은 Swift 버전에 따라 받아주지 않는다
    @ViewBuilder
    private func shell(for layout: AppShellLayout) -> some View {
        switch layout {
        case .rail:
            HStack(spacing: 0) {
                if let selectedTab = selectedTab {
                    PassmateNavigationRail(selectedTab: selectedTab, onSelectTab: onSelectTab)
                }
                contentArea
            }
        case .bottomBar:
            VStack(spacing: 0) {
                contentArea
                if let selectedTab = selectedTab {
                    PassmateBottomTabBar(selectedTab: selectedTab, onSelectTab: onSelectTab)
                }
            }
        }
    }

    // 본문 — 가용 폭을 그대로 채운다. 넓은 화면에서 폭을 묶어 가운데 정렬했더니 PassmateTopBar의
    // 뒤로가기 버튼이 화면 중앙 쪽에 떠 좌상단이 아니게 보이는 문제가 있어 클램프를 걷어냈다(2026-09-17).
    // 배경색은 화면 전환 중 빈 프레임을 덮는 안전망으로 남긴다 — 화면들이 스스로 흰 surface를 깐다
    private var contentArea: some View {
        ZStack(alignment: .top) {
            PassmateColors.backgroundMint
            content()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
