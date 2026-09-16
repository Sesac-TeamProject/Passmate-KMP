import SwiftUI

// 칩·태그를 가로로 흐르게 놓고 폭이 넘치면 줄바꿈하는 레이아웃 — iOS 15 호환(Layout 프로토콜 미사용).
// 자식을 열거하는 공개 API가 없어 데이터 기반으로 받는다: FlowLayout(items, id: \.self, spacing: 8) { item in chip(item) }
//
// 자리는 "자식 크기(가로는 줄 폭 안에서 잰다) + 줄 폭"만으로 계산한다 (스펙 2026-08-31 §3-1).
// 예전에는 alignmentGuide 클로저가 밖의 변수를 누적하며 자리를 정하고, 놓인 결과의 maxY를 되읽어 자기 높이로 썼다.
// SwiftUI가 가이드를 몇 번·어떤 순서로 부를지는 보장되지 않아, 키보드가 올라오며 시트가 다시 배치되자
// 계산이 끝나지 않고 앱이 멈췄다(실기기 B-6, 0x8BADF00D). 크기는 놓인 자리와 무관하게 재고, 배치 결과는 다시 재지 않는다
struct FlowLayout<Data: RandomAccessCollection, ID: Hashable, Content: View>: View {
    private struct Entry: Identifiable {
        let id: ID

        let index: Int

        let element: Data.Element
    }

    private struct Placement {
        let origins: [CGPoint]

        let height: CGFloat
    }

    private let entries: [Entry]

    private let spacing: CGFloat

    private let content: (Data.Element) -> Content

    // 자식마다 크기 — 놓인 자리와 상관없이 잰다
    @State private var itemSizes: [Int: CGSize] = [:]

    // 줄바꿈 기준 폭. 재기 전(0)에는 줄바꿈하지 않는다 — 한 줄로 그렸다가 폭이 오면 늘어난다
    @State private var availableWidth: CGFloat = 0

    // 같은 입력이면 몇 번 불려도 같은 값을 낸다 — 레이아웃 패스의 순서·횟수에 기대지 않는다
    private func computePlacement() -> Placement {
        var origins: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        let canWrap = availableWidth > 0

        for entry in entries {
            let size = itemSizes[entry.index] ?? .zero

            if canWrap && x > 0 && x + size.width > availableWidth {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            origins.append(CGPoint(x: x, y: y))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        return Placement(origins: origins, height: y + rowHeight)
    }

    private func sizeReader(index: Int) -> some View {
        GeometryReader { geometry in
            Color.clear.preference(key: FlowItemSizesKey.self, value: [index: geometry.size])
        }
    }

    private var widthReader: some View {
        GeometryReader { geometry in
            Color.clear.preference(key: FlowWidthKey.self, value: geometry.size.width)
        }
    }

    var body: some View {
        let placement = computePlacement()

        ZStack(alignment: .topLeading) {
            ForEach(entries) { entry in
                content(entry.element)
                    // 가로는 줄 폭 안에서 제안받는다 — 한 줄보다 긴 칩은 칩 안에서 줄바꿈하고 부모를 넓히지 않는다.
                    // 세로는 본래 높이로 잰다 — 배치로 정한 높이가 다시 칩 크기로 돌아오지 않게
                    .fixedSize(horizontal: false, vertical: true)
                    .background(sizeReader(index: entry.index))
                    // offset은 레이아웃을 바꾸지 않는다 — 옮긴 자리가 다시 크기 측정으로 돌아오지 않는다
                    .offset(x: placement.origins[entry.index].x, y: placement.origins[entry.index].y)
            }
        }
        .frame(maxWidth: .infinity, minHeight: placement.height, maxHeight: placement.height, alignment: .topLeading)
        .background(widthReader)
        .onPreferenceChange(FlowItemSizesKey.self) { itemSizes = $0 }
        .onPreferenceChange(FlowWidthKey.self) { availableWidth = $0 }
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

private struct FlowItemSizesKey: PreferenceKey {
    static var defaultValue: [Int: CGSize] = [:]

    // 자식들이 각자 자기 크기를 올린다 — 인덱스별로 모은다
    static func reduce(value: inout [Int: CGSize], nextValue: () -> [Int: CGSize]) {
        value.merge(nextValue()) { _, new in new }
    }
}

private struct FlowWidthKey: PreferenceKey {
    static var defaultValue: CGFloat = 0

    // 폭을 올리는 건 widthReader 하나지만, 같은 자리에서 칩 쪽 자식들의 기본값 0도 함께 합쳐진다.
    // 나중 값으로 덮으면 폭이 0이 되어 줄바꿈이 꺼진다 — 최댓값을 쓴다
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = max(value, nextValue())
    }
}
