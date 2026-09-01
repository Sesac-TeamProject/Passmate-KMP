import SwiftUI
import Shared

// 한 줄에 놓는 뱃지 수 (시안 M-09 뱃지 컬렉션 그리드)
private let badgesPerRow = 4

// Figma "UI 디자인 v6" M-09(349:9770) 미러 — 명성 · 뱃지 상세: 프로필+등급 카드(승급 진행도·조건)+뱃지 컬렉션
struct ReputationView: View {
    var onRequireSignIn: () -> Void = {}

    var onBack: () -> Void = {}

    @StateObject private var viewModel = ReputationViewModel(
        getMyGradeUseCase: KoinHelper.shared.getMyGradeUseCase(),
        getMyBadgesUseCase: KoinHelper.shared.getMyBadgesUseCase(),
        getMyProfileUseCase: KoinHelper.shared.getMyProfileUseCase(),
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
        VStack(alignment: .leading, spacing: 0) {
            header
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
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private var header: some View {
        HStack(spacing: 12) {
            Button(action: onClickBack) {
                Text("←")
                    .font(.system(size: 22))
                    .foregroundColor(PassmateColors.textPrimary)
                    .frame(width: 24, height: 24)
            }
            Text("명성 · 뱃지")
                .font(.system(size: 24, weight: .bold))
                .kerning(-0.48)
                .foregroundColor(PassmateColors.textPrimary)
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
        .padding(.bottom, 12)
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
            VStack(alignment: .leading, spacing: 12) {
                if let profile = uiState.profile {
                    ProfileCardView(
                        profile: profile,
                        stats: uiState.grade?.stats,
                        level: emblemLevel(grade: uiState.grade, profile: profile)
                    )
                }
                if let grade = uiState.grade {
                    GradeCardView(grade: grade)
                }
                BadgeSectionView(badges: uiState.badges)
                if let grade = uiState.grade, grade.level.level < Shared.HostLevel.verified.level {
                    paidRoomLockedCta
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 20)
        }
    }

    // 시안의 잠긴 CTA — Lv.3 미만에서만 노출한다. 방 개설 진입은 '내가 만든 방' 탭이 담당하므로 안내 전용이다
    private var paidRoomLockedCta: some View {
        Text("🔒 유료 방 만들기 — Lv.3부터")
            .font(.system(size: 14, weight: .medium))
            .kerning(-0.28)
            .foregroundColor(PassmateColors.textTertiary)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(PassmateColors.fieldGray)
            .cornerRadius(16)
    }
}

// Kotlin(Shared)의 HostLevel과 화면용 Swift enum HostLevel은 이름이 같아 서로 가린다.
// MyInfoView.localLevel과 같은 방식으로 level 값만 꺼내 변환한다.
private func emblemLevel(grade: MyGrade?, profile: UserProfile) -> HostLevel? {
    let raw = grade?.level.level ?? profile.level?.level

    return HostLevel.from(raw.map { Int($0) })
}

private struct ProfileCardView: View {
    let profile: UserProfile

    let stats: GradeStats?

    let level: HostLevel?

    var body: some View {
        HStack(spacing: 14) {
            StudentAvatarView(avatarId: profile.avatarId.map { Int(truncating: $0) } ?? StudentAvatars.defaultId)
                .frame(width: 56, height: 56)
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text(profile.nickname)
                        .font(.system(size: 16, weight: .medium))
                        .kerning(-0.32)
                        .foregroundColor(PassmateColors.textPrimary)
                    if let level {
                        ReputationBadgeView(level: level)
                    }
                }
                if let stats {
                    Text(statsLine(stats))
                        .font(.system(size: 12))
                        .kerning(-0.24)
                        .foregroundColor(PassmateColors.textSecondary)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(PassmateColors.border, lineWidth: 1))
    }

    // 시안 "참여 18회 · 평균 정답률 72% · 방 운영 12회" — 서버가 준 집계만 이어 붙인다
    private func statsLine(_ stats: GradeStats) -> String {
        var parts = ["참여 \(stats.participationCount)회"]

        if let accuracy = stats.avgAccuracyPercent?.intValue {
            parts.append("평균 정답률 \(accuracy)%")
        }
        parts.append("방 운영 \(stats.roomCount)회")

        return parts.joined(separator: " · ")
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
                        .font(.system(size: 16, weight: .medium))
                        .kerning(-0.32)
                        .foregroundColor(PassmateColors.textPrimary)
                    Text(nextLevelLine)
                        .font(.system(size: 12))
                        .kerning(-0.24)
                        .foregroundColor(PassmateColors.textSecondary)
                }
                Spacer()
                if let next = grade.next {
                    Text("\(next.progressPercent)%")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
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
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
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
                .frame(width: 24, height: 24)
            Text("Lv.3이 되면 유료 방을 열고 참가비의 80%를 정산받아요")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.reputationBadgeText)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
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
        .frame(height: 8)
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
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
            } else {
                Text("\(formatNumber(criterion.current)) / \(formatNumber(criterion.target))")
                    .font(.system(size: 14, weight: .medium))
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
                    .font(.system(size: 16, weight: .medium))
                    .kerning(-0.32)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
                Text("\(earnedCount) / \(badges.count)")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
            VStack(spacing: 12) {
                ForEach(0..<rows.count, id: \.self) { rowIndex in
                    HStack(alignment: .top, spacing: 8) {
                        ForEach(rows[rowIndex], id: \.type) { badge in
                            BadgeCellView(badge: badge)
                                .frame(maxWidth: .infinity)
                        }
                        ForEach(0..<(badgesPerRow - rows[rowIndex].count), id: \.self) { _ in
                            Spacer().frame(maxWidth: .infinity)
                        }
                    }
                }
            }
            .padding(14)
            .frame(maxWidth: .infinity)
            .background(PassmateColors.surface)
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(PassmateColors.border, lineWidth: 1))
        }
    }

    private var rows: [[Badge]] {
        stride(from: 0, to: badges.count, by: badgesPerRow).map { start in
            Array(badges[start..<min(start + badgesPerRow, badges.count)])
        }
    }
}

private struct BadgeCellView: View {
    let badge: Badge

    var body: some View {
        VStack(spacing: 5) {
            Text(glyph)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(PassmateColors.primaryDeep)
                .frame(width: 44, height: 44)
                .background(PassmateColors.backgroundMint)
                .cornerRadius(13)
                .overlay(
                    RoundedRectangle(cornerRadius: 13)
                        .stroke(PassmateColors.achievementBadgeBorder, lineWidth: 1.5)
                )
                .opacity(badge.earned ? 1 : 0.3)
            Text(badge.type.label)
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
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
}
