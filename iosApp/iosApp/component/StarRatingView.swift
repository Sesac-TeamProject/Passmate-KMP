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

// 명성 레벨 뱃지 — 등급별 엠블럼 + "Lv.N {등급명}" (T086, 디자인 시스템 §ReputationBadge)
struct ReputationBadgeView: View {
    let level: HostLevel

    var body: some View {
        HStack(spacing: 4) {
            LevelEmblemView(level: level)
                .frame(width: 14, height: 14)
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

// T086(US12) 명성 레벨 엠블럼 — 육각형 배경 + 등급별 심볼(새싹·성장·체크·별·왕관).
// Compose LevelEmblem.kt와 1:1. Lv.5는 골드 육각
struct LevelEmblemView: View {
    let level: HostLevel

    private var hexColor: Color {
        level == .master ? PassmateColors.starGold : PassmateColors.primary
    }

    private var symbolColor: Color {
        level == .master ? PassmateColors.primaryDeep : PassmateColors.surface
    }

    var body: some View {
        GeometryReader { geo in
            let r = min(geo.size.width, geo.size.height) / 2
            let c = CGPoint(x: geo.size.width / 2, y: geo.size.height / 2)

            ZStack {
                hexagon(center: c, radius: r).fill(hexColor)
                symbol(center: c, radius: r)
            }
        }
    }

    private func hexagon(center: CGPoint, radius: CGFloat) -> Path {
        Path { p in
            for i in 0..<6 {
                let angle = CGFloat.pi / 180 * CGFloat(60 * i - 90)
                let pt = CGPoint(x: center.x + radius * cos(angle), y: center.y + radius * sin(angle))

                if i == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
            }
            p.closeSubpath()
        }
    }

    @ViewBuilder
    private func symbol(center c: CGPoint, radius r: CGFloat) -> some View {
        switch level {
        case .seedling:
            sproutView(center: c, radius: r, leaves: 2)
        case .growing:
            sproutView(center: c, radius: r, leaves: 3)
        case .verified:
            checkPath(center: c, radius: r).stroke(symbolColor, style: StrokeStyle(lineWidth: r * 0.18, lineCap: .round, lineJoin: .round))
        case .popular:
            starPath(center: c, radius: r * 0.55).fill(symbolColor)
        case .master:
            crownPath(center: c, radius: r).fill(symbolColor)
        }
    }

    private func sproutView(center c: CGPoint, radius r: CGFloat, leaves: Int) -> some View {
        ZStack {
            Path { p in
                p.move(to: CGPoint(x: c.x, y: c.y + r * 0.5))
                p.addLine(to: CGPoint(x: c.x, y: c.y - r * 0.15))
            }
            .stroke(symbolColor, style: StrokeStyle(lineWidth: r * 0.14, lineCap: .round))
            Ellipse().fill(symbolColor)
                .frame(width: r * 0.5, height: r * 0.32)
                .position(x: c.x - r * 0.25, y: c.y - r * 0.19)
            Ellipse().fill(symbolColor)
                .frame(width: r * 0.5, height: r * 0.32)
                .position(x: c.x + r * 0.25, y: c.y - r * 0.19)
            if leaves >= 3 {
                Ellipse().fill(symbolColor)
                    .frame(width: r * 0.44, height: r * 0.3)
                    .position(x: c.x, y: c.y - r * 0.47)
            }
        }
    }

    private func checkPath(center c: CGPoint, radius r: CGFloat) -> Path {
        Path { p in
            p.move(to: CGPoint(x: c.x - r * 0.42, y: c.y + r * 0.02))
            p.addLine(to: CGPoint(x: c.x - r * 0.1, y: c.y + r * 0.35))
            p.addLine(to: CGPoint(x: c.x + r * 0.45, y: c.y - r * 0.35))
        }
    }

    private func starPath(center c: CGPoint, radius: CGFloat) -> Path {
        Path { p in
            let inner = radius * 0.45

            for i in 0..<10 {
                let rad = i % 2 == 0 ? radius : inner
                let angle = CGFloat.pi / 180 * CGFloat(36 * i - 90)
                let pt = CGPoint(x: c.x + rad * cos(angle), y: c.y + rad * sin(angle))

                if i == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
            }
            p.closeSubpath()
        }
    }

    private func crownPath(center c: CGPoint, radius r: CGFloat) -> Path {
        Path { p in
            let top = c.y - r * 0.4
            let bottom = c.y + r * 0.4
            let left = c.x - r * 0.5
            let right = c.x + r * 0.5

            p.move(to: CGPoint(x: left, y: bottom))
            p.addLine(to: CGPoint(x: left, y: top))
            p.addLine(to: CGPoint(x: c.x - r * 0.22, y: c.y + r * 0.05))
            p.addLine(to: CGPoint(x: c.x, y: top - r * 0.08))
            p.addLine(to: CGPoint(x: c.x + r * 0.22, y: c.y + r * 0.05))
            p.addLine(to: CGPoint(x: right, y: top))
            p.addLine(to: CGPoint(x: right, y: bottom))
            p.closeSubpath()
        }
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
