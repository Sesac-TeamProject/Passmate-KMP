import SwiftUI

// 별점 1~5 (디자인 시스템 §StarRating, 골드 #F2C94C 전용). onSelect가 nil이면 읽기 전용.
// 별은 리소스 아이콘으로 그린다 — 예전엔 ★/☆ 글리프였는데 기기 폰트마다 모양이 달라
// 시안과 같아질 수 없었고 규칙 §11-3(화면 코드에 아이콘을 그리지 않는다)에도 어긋났다.
// Compose component/StarRating.kt와 1:1 미러다
struct StarRatingView: View {
    let stars: Int

    var starSize: CGFloat = 34

    var onSelect: ((Int) -> Void)?

    var body: some View {
        HStack(spacing: 6) {
            ForEach(1...5, id: \.self) { index in
                let isFilled = index <= stars

                PassmateIconView(
                    icon: isFilled ? .starFilled : .star,
                    tint: isFilled ? PassmateColors.starGold : PassmateColors.border,
                    size: starSize
                )
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
// 시안 LevelEmblem(M-09·M-10·M-13 실측, 48 기준): 바깥 육각 48은 위→아래 그라디언트(levelEmblemGradientTop→primary)에
// 연민트 링 1.5, 안쪽 육각 37.5는 primary 15%→55% 그라디언트에 흰 28% 선 0.75, 위쪽에 흰 22% 광택 타원 30x13.5.
// Lv.5는 골드 육각(시안 미실측 — 단색 유지). Compose LevelEmblem.kt와 1:1
struct LevelEmblemView: View {
    let level: HostLevel

    private var isMaster: Bool {
        level == .master
    }

    private var symbolColor: Color {
        isMaster ? PassmateColors.primaryDeep : PassmateColors.surface
    }

    var body: some View {
        GeometryReader { geo in
            let r = min(geo.size.width, geo.size.height) / 2
            let c = CGPoint(x: geo.size.width / 2, y: geo.size.height / 2)

            ZStack {
                if isMaster {
                    hexagon(center: c, radius: r).fill(PassmateColors.starGold)
                } else {
                    gradientHexagon(center: c, radius: r)
                }
                symbol(center: c, radius: r)
            }
        }
    }

    // 비율은 전부 시안 48 기준을 반지름(24)으로 나눈 값이다
    private func gradientHexagon(center c: CGPoint, radius r: CGFloat) -> some View {
        ZStack {
            hexagon(center: c, radius: r)
                .fill(LinearGradient(colors: [PassmateColors.levelEmblemGradientTop, PassmateColors.primary], startPoint: .top, endPoint: .bottom))
            hexagon(center: c, radius: r)
                .stroke(PassmateColors.achievementBadgeBorder, lineWidth: r * 0.0625)
            hexagon(center: c, radius: r * 0.78)
                .fill(LinearGradient(colors: [PassmateColors.primary.opacity(0.15), PassmateColors.primary.opacity(0.55)], startPoint: .top, endPoint: .bottom))
            hexagon(center: c, radius: r * 0.78)
                .stroke(PassmateColors.surface.opacity(0.28), lineWidth: r * 0.03125)
            Ellipse()
                .fill(PassmateColors.surface.opacity(0.22))
                .frame(width: r * 1.25, height: r * 0.5625)
                .position(x: c.x, y: c.y - r * 0.78 + r * 0.28125)
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
    var level: Int { rawValue }

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
