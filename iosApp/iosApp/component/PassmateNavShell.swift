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

    // 본문 — 넓은 화면에서 시안 폭을 넘지 않게 묶고 가운데 정렬한다. 남는 좌우는 앱 배경색으로 채운다.
    // 화면들이 스스로 흰 surface를 깔기 때문에 여백까지 흰색이면 본문이 어디까지인지 보이지 않는다
    private var contentArea: some View {
        ZStack(alignment: .top) {
            PassmateColors.backgroundMint
            content()
                .frame(maxWidth: AppShellLayoutPolicy.contentMaxWidth)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
