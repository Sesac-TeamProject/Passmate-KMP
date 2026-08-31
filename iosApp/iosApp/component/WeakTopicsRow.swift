import SwiftUI

// "보완할 주제" 라벨 + 주제 칩 행 — ResultView(M-07)·JoinedRoomsView(M-08) 공용(두 화면의 private 복사본을 승격).
// 라벨이 칩들과 같은 흐름에 놓이므로 항목 enum으로 표현한다 (스펙 2026-08-31 §3-2)
struct WeakTopicsRow: View {
    private enum Item: Hashable {
        case label

        case topic(String)
    }

    let topics: [String]

    private var items: [Item] {
        [.label] + topics.map(Item.topic)
    }

    @ViewBuilder
    private func itemView(_ item: Item) -> some View {
        switch item {
        case .label:
            Text("보완할 주제")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
                .padding(.vertical, 6)
        case let .topic(topic):
            Text(topic)
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.weakTopicText)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(PassmateColors.weakTopicBg)
                .clipShape(Capsule())
        }
    }

    var body: some View {
        if !topics.isEmpty {
            FlowLayout(items, id: \.self, spacing: 8) { item in
                itemView(item)
            }
        }
    }
}
