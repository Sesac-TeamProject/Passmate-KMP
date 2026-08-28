import SwiftUI

// 별점 1~5 (디자인 시스템 §StarRating, 골드 #F2C94C 전용). onSelect가 nil이면 읽기 전용
struct StarRatingView: View {
    let stars: Int

    var starSize: CGFloat = 34

    var onSelect: ((Int) -> Void)?

    var body: some View {
        HStack(spacing: 6) {
            ForEach(1...5, id: \.self) { index in
                let isFilled = index <= stars

                Text(isFilled ? "★" : "☆")
                    .font(.system(size: starSize, weight: .medium))
                    .foregroundColor(isFilled ? PassmateColors.starGold : PassmateColors.border)
                    .onTapGesture {
                        onSelect?(index)
                    }
            }
        }
    }
}

// 명성 레벨 뱃지 — "Lv.N {등급명}", 파스텔 배경 + 진한 잉크 (디자인 시스템 §ReputationBadge)
struct ReputationBadgeView: View {
    let level: HostLevel

    var body: some View {
        HStack(spacing: 4) {
            ZStack {
                Circle()
                    .fill(PassmateColors.primary)
                    .frame(width: 14, height: 14)
                Text("\(level.level)")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundColor(PassmateColors.surface)
            }
            Text("Lv.\(level.level) \(level.label)")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.reputationBadgeText)
        }
        .padding(.leading, 5)
        .padding(.trailing, 10)
        .padding(.vertical, 4)
        .background(PassmateColors.reputationBadgeBg)
        .clipShape(Capsule())
    }
}

// 호스트 등급 Lv.1~5 (shared HostLevel 미러) — Kotlin enum 인터롭 대신 Swift 자체 정의로 라벨 안정화
enum HostLevel: Int {
    case seedling = 1
    case growing = 2
    case verified = 3
    case popular = 4
    case master = 5

    var label: String {
        switch self {
        case .seedling: return "새싹"
        case .growing: return "성장"
        case .verified: return "검증된 운영자"
        case .popular: return "인기 운영자"
        case .master: return "마스터"
        }
    }

    static func from(_ level: Int?) -> HostLevel? {
        guard let level else { return nil }

        return HostLevel(rawValue: level)
    }
}
