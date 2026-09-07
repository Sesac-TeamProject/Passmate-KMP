import SwiftUI

// 칩·태그를 가로로 흐르게 놓고 폭이 넘치면 줄바꿈하는 레이아웃 — iOS 15 호환(Layout 프로토콜 미사용).
// 자식을 열거하는 공개 API가 없어 데이터 기반으로 받는다: FlowLayout(items, id: \.self, spacing: 8) { item in chip(item) }
// 배치는 ZStack(.topLeading) + alignmentGuide 누적 오프셋, 전체 높이는 PreferenceKey로 되읽어 고정한다 (스펙 2026-08-31 §3-1)
struct FlowLayout<Data: RandomAccessCollection, ID: Hashable, Content: View>: View {
    private struct Entry: Identifiable {
        let id: ID

        let index: Int

        let element: Data.Element
    }

    private let entries: [Entry]

    private let spacing: CGFloat

    private let content: (Data.Element) -> Content

    private static var coordinateSpace: String { "PassmateFlowLayout" }

    @State private var totalHeight: CGFloat = 0

    // 자식은 alignmentGuide 음수 오프셋으로 아래 줄에 놓인다. 그 이동은 ZStack 자체 크기에는
    // 반영되지 않으므로 ZStack 프레임을 재면 항상 "한 줄" 높이가 나온다(줄바꿈된 칩 위로 다음
    // 뷰가 겹친다). 자식마다 실제 놓인 자리의 maxY를 재서 그중 최댓값을 전체 높이로 쓴다
    private func boundsReader(in space: String) -> some View {
        GeometryReader { geometry in
            Color.clear.preference(
                key: FlowLayoutHeightKey.self,
                value: geometry.frame(in: .named(space)).maxY
            )
        }
    }

    // alignmentGuide 클로저는 레이아웃 패스마다 자식 순서대로 호출된다(.leading 다음 .top).
    // 마지막 항목에서 누적값을 0으로 되돌려 다음 패스를 준비한다. 반환값은 음수 오프셋(leading/top 기준 이동량).
    private func rows(in geometry: GeometryProxy) -> some View {
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        let maxWidth = geometry.size.width
        let lastIndex = entries.count - 1

        return ZStack(alignment: .topLeading) {
            ForEach(entries) { entry in
                content(entry.element)
                    .alignmentGuide(.leading) { dimensions in
                        if x + dimensions.width > maxWidth, x > 0 {
                            x = 0
                            y += rowHeight + spacing
                            rowHeight = 0
                        }
                        let result = x

                        rowHeight = max(rowHeight, dimensions.height)
                        if entry.index == lastIndex {
                            x = 0
                        } else {
                            x += dimensions.width + spacing
                        }
                        return -result
                    }
                    .alignmentGuide(.top) { _ in
                        let result = y

                        if entry.index == lastIndex {
                            y = 0
                            rowHeight = 0
                        }
                        return -result
                    }
                    .background(boundsReader(in: Self.coordinateSpace))
            }
        }
        .coordinateSpace(name: Self.coordinateSpace)
    }

    var body: some View {
        GeometryReader { geometry in
            rows(in: geometry)
        }
        .frame(height: totalHeight)
        .onPreferenceChange(FlowLayoutHeightKey.self) { totalHeight = $0 }
    }

    init(
        _ data: Data,
        id: KeyPath<Data.Element, ID>,
        spacing: CGFloat = 8,
        @ViewBuilder content: @escaping (Data.Element) -> Content
    ) {
        self.entries = data.enumerated().map { offset, element in
            Entry(id: element[keyPath: id], index: offset, element: element)
        }
        self.spacing = spacing
        self.content = content
    }
}

private struct FlowLayoutHeightKey: PreferenceKey {
    static var defaultValue: CGFloat = 0

    // 자식들이 각자 자기 maxY를 올린다 — 가장 아래 자식이 전체 높이다
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = max(value, nextValue())
    }
}
