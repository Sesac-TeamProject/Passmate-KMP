import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-09(349:9770) 미러 — 내 명성·뱃지 상세: 등급 카드(승급 진행도·조건)+뱃지 컬렉션
struct ReputationView: View {
    var onRequireSignIn: () -> Void = {}

    var onBack: () -> Void = {}

    @StateObject private var viewModel = ReputationViewModel(
        getMyGradeUseCase: KoinHelper.shared.getMyGradeUseCase(),
        getMyBadgesUseCase: KoinHelper.shared.getMyBadgesUseCase(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    var body: some View {
        ReputationContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onClickBack: onBack
        )
        .onAppear {
            viewModel.action(.enter)
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .requireSignIn:
                onRequireSignIn()
            }
        }
    }
}

private struct ReputationContentView: View {
    let uiState: ReputationUiState

    let onAction: (ReputationAction) -> Void

    let onClickBack: () -> Void

    var body: some View {
        Group {
            if uiState.isLoading {
                ProgressView()
                    .tint(PassmateColors.primary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if uiState.loadFailed {
                errorView
            } else {
                loadedView
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private var errorView: some View {
        VStack(spacing: 12) {
            Text("명성 정보를 불러오지 못했어요")
                .font(.system(size: 16, weight: .medium))
                .kerning(-0.32)
                .foregroundColor(PassmateColors.textPrimary)
            Button {
                onAction(.retry)
            } label: {
                Text("다시 시도")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var loadedView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    Text("내 명성")
                        .font(.system(size: 24, weight: .bold))
                        .kerning(-0.48)
                        .foregroundColor(PassmateColors.textPrimary)
                    Spacer()
                    Button(action: onClickBack) {
                        Text("닫기")
                            .font(.system(size: 14, weight: .medium))
                            .kerning(-0.28)
                            .foregroundColor(PassmateColors.textSecondary)
                    }
                }
                if let grade = uiState.grade {
                    GradeCardView(grade: grade)
                }
                BadgeSectionView(badges: uiState.badges)
            }
            .padding(.horizontal, 20)
            .padding(.top, 32)
            .padding(.bottom, 24)
        }
    }
}

private struct GradeCardView: View {
    let grade: MyGrade

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                LevelEmblemView(level: HostLevel.from(Int(grade.level.level)) ?? .seedling)
                    .frame(width: 48, height: 48)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Lv.\(grade.level.level) \(grade.level.label)")
                        .font(.system(size: 18, weight: .bold))
                        .kerning(-0.36)
                        .foregroundColor(PassmateColors.textPrimary)
                    Text(nextLevelLine)
                        .font(.system(size: 13))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.textSecondary)
                }
                Spacer()
                if let next = grade.next {
                    Text("\(next.progressPercent)%")
                        .font(.system(size: 18, weight: .bold))
                        .kerning(-0.36)
                        .foregroundColor(PassmateColors.primaryDeep)
                }
            }
            if let next = grade.next {
                progressBar(percent: Int(next.progressPercent))
                if next.level == Shared.HostLevel.verified {
                    unlockNoteBox
                }
                ForEach(criteria(next), id: \.label) { criterion in
                    CriterionRowView(criterion: criterion)
                }
            }
            Text("Lv.3 달성 후 하락 없음 · Lv.4~5만 30일 활동 유지 조건")
                .font(.system(size: 12))
                .kerning(-0.24)
                .foregroundColor(PassmateColors.textTertiary)
        }
        .padding(18)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(PassmateColors.border, lineWidth: 1))
    }

    private var nextLevelLine: String {
        if let next = grade.next {
            return "다음 레벨 Lv.\(next.level.level) \(next.level.label)까지 \(next.progressPercent)%"
        } else {
            return "최고 등급이에요"
        }
    }

    private var unlockNoteBox: some View {
        HStack(spacing: 8) {
            LevelEmblemView(level: .verified)
                .frame(width: 18, height: 18)
            Text("Lv.3이 되면 유료 방을 열고 참가비의 80%를 정산받아요")
                .font(.system(size: 13, weight: .medium))
                .kerning(-0.26)
                .foregroundColor(PassmateColors.primaryDeep)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(PassmateColors.backgroundMint)
        .cornerRadius(12)
    }

    private func progressBar(percent: Int) -> some View {
        GeometryReader { geo in
            let fraction = CGFloat(min(max(percent, 0), 100)) / 100

            ZStack(alignment: .leading) {
                Capsule().fill(PassmateColors.fieldGray)
                if fraction > 0 {
                    Capsule()
                        .fill(PassmateColors.primary)
                        .frame(width: geo.size.width * fraction)
                }
            }
        }
        .frame(height: 10)
    }

    private func criteria(_ next: NextGrade) -> [GradeCriterion] {
        next.criteria.compactMap { $0 as? GradeCriterion }
    }
}

private struct CriterionRowView: View {
    let criterion: GradeCriterion

    var body: some View {
        HStack {
            Text(criterion.label)
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textPrimary)
            Spacer()
            if criterion.met {
                Text("✓ \(formatNumber(criterion.current))")
                    .font(.system(size: 14, weight: .bold))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
            } else {
                Text("\(formatNumber(criterion.current)) / \(formatNumber(criterion.target))")
                    .font(.system(size: 14, weight: .bold))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.weakTopicText)
            }
        }
    }

    private func formatNumber(_ value: Double) -> String {
        let rounded = Int((value * 10).rounded())

        if rounded % 10 == 0 {
            return "\(rounded / 10)"
        } else {
            return "\(rounded / 10).\(rounded % 10)"
        }
    }
}

private struct BadgeSectionView: View {
    let badges: [Badge]

    private var earnedCount: Int {
        badges.filter { $0.earned }.count
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("내 뱃지")
                    .font(.system(size: 18, weight: .bold))
                    .kerning(-0.36)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
                Text("\(earnedCount) / \(badges.count)")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textSecondary)
            }
            VStack(spacing: 14) {
                ForEach(0..<rows.count, id: \.self) { rowIndex in
                    HStack(alignment: .top, spacing: 10) {
                        ForEach(rows[rowIndex], id: \.type) { badge in
                            BadgeCellView(badge: badge)
                                .frame(maxWidth: .infinity)
                        }
                        ForEach(0..<(4 - rows[rowIndex].count), id: \.self) { _ in
                            Spacer().frame(maxWidth: .infinity)
                        }
                    }
                }
            }
            .padding(14)
            .frame(maxWidth: .infinity)
            .background(PassmateColors.backgroundMint)
            .cornerRadius(20)
        }
    }

    private var rows: [[Badge]] {
        stride(from: 0, to: badges.count, by: 4).map { start in
            Array(badges[start..<min(start + 4, badges.count)])
        }
    }
}

private struct BadgeCellView: View {
    let badge: Badge

    var body: some View {
        VStack(spacing: 6) {
            Text(glyph)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(badge.earned ? PassmateColors.primaryDeep : PassmateColors.textTertiary)
                .frame(width: 52, height: 52)
                .background(badge.earned ? PassmateColors.surface : PassmateColors.fieldGray)
                .cornerRadius(14)
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(badge.earned ? PassmateColors.primary : PassmateColors.border, lineWidth: 1)
                )
            Text(label)
                .font(.system(size: 12, weight: .medium))
                .kerning(-0.24)
                .foregroundColor(badge.earned ? PassmateColors.textPrimary : PassmateColors.textTertiary)
                .multilineTextAlignment(.center)
        }
    }

    private var glyph: String {
        if badge.type == BadgeType.firstRoom {
            return "⚑"
        } else if badge.type == BadgeType.rooms10 {
            return "10"
        } else if badge.type == BadgeType.students100 {
            return "100"
        } else if badge.type == BadgeType.rating45 {
            return "★"
        } else if badge.type == BadgeType.ratings50 {
            return "50"
        } else if badge.type == BadgeType.streak30 {
            return "30"
        } else if badge.type == BadgeType.firstPaidRoom {
            return "₩"
        } else {
            return "AI"
        }
    }

    private var label: String {
        if !badge.earned, let current = badge.progressCurrent?.intValue, let target = badge.progressTarget?.intValue {
            return "\(badge.type.label) \(current)/\(target)"
        } else {
            return badge.type.label
        }
    }
}
