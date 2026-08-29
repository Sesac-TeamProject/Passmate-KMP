import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-10(349:9851) 미러 — 선생님 프로필 시트: 레벨·평가·뱃지·운영 중인 방 + 신고·차단.
// 시트 표시 여부는 호스팅 화면(RoomListView 등)이 소유하고, 이 뷰는 시트 내부 내용만 담당한다 (규칙 §11-1)
struct HostProfileSheetView: View {
    let hostId: Int64

    var onJoinRoom: (String) -> Void = { _ in }

    var onRequireSignIn: () -> Void = {}

    var onBlocked: () -> Void = {}

    var onNotice: (String) -> Void = { _ in }

    @StateObject private var viewModel = HostProfileViewModel(
        getHostProfileUseCase: KoinHelper.shared.getHostProfileUseCase(),
        blockHostUseCase: KoinHelper.shared.blockHostUseCase(),
        reportHostUseCase: KoinHelper.shared.reportHostUseCase(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    @State private var showReportDialog = false

    @State private var showBlockConfirm = false

    var body: some View {
        HostProfileContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onRetry: { viewModel.action(.retry(hostId: hostId)) },
            onClickReport: { showReportDialog = true },
            onClickBlock: { showBlockConfirm = true }
        )
        .onAppear {
            viewModel.action(.enter(hostId: hostId))
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .requireSignIn:
                onRequireSignIn()
            case let .joinRoom(pin):
                onJoinRoom(pin)
            case .blockedAndClose:
                onBlocked()
            case let .showNotice(message):
                onNotice(message)
            }
        }
        .confirmationDialog("프로필 신고", isPresented: $showReportDialog, titleVisibility: .visible) {
            reportButtons
        }
        .alert("선생님 차단", isPresented: $showBlockConfirm) {
            Button("차단", role: .destructive) {
                viewModel.action(.clickBlock)
            }
            Button("취소", role: .cancel) {}
        } message: {
            Text("\(viewModel.uiState.profile?.nickname ?? "") 선생님을 차단하면 이 선생님의 방이 인기 방·탐색 목록에서 보이지 않아요. 이미 참여 중인 방은 유지돼요.")
        }
    }

    @ViewBuilder
    private var reportButtons: some View {
        Button("부적절한 닉네임") { viewModel.action(.submitReport(reason: ReportReason.nickname)) }
        Button("문제 오류") { viewModel.action(.submitReport(reason: ReportReason.questionError)) }
        Button("유료 방 문제") { viewModel.action(.submitReport(reason: ReportReason.paidRoom)) }
        Button("부적절한 운영") { viewModel.action(.submitReport(reason: ReportReason.operation)) }
        Button("도배·광고") { viewModel.action(.submitReport(reason: ReportReason.spam)) }
        Button("난이도 불일치") { viewModel.action(.submitReport(reason: ReportReason.difficulty)) }
        Button("취소", role: .cancel) {}
    }
}

private struct HostProfileContentView: View {
    let uiState: HostProfileUiState

    let onAction: (HostProfileAction) -> Void

    let onRetry: () -> Void

    let onClickReport: () -> Void

    let onClickBlock: () -> Void

    var body: some View {
        Group {
            if uiState.isLoading {
                ProgressView()
                    .tint(PassmateColors.primary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 220)
            } else if uiState.loadFailed || uiState.profile == nil {
                errorView
            } else if let profile = uiState.profile {
                loadedView(profile)
            }
        }
        .background(PassmateColors.surface)
    }

    private var errorView: some View {
        VStack(spacing: 10) {
            Text("프로필을 불러오지 못했어요")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
            Button(action: onRetry) {
                Text("다시 시도")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 220)
    }

    private func loadedView(_ profile: HostProfile) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                header(profile)
                statsRow(profile)
                if !profile.badges.isEmpty {
                    badgeSection(profile)
                }
                if !hostRooms(profile).isEmpty {
                    Text("운영 중인 방")
                        .font(.system(size: 16, weight: .bold))
                        .kerning(-0.32)
                        .foregroundColor(PassmateColors.textPrimary)
                    ForEach(hostRooms(profile), id: \.roomId) { room in
                        HostRoomRowView(room: room, onClickJoin: { onAction(.clickRoom(pin: room.pin)) })
                    }
                }
                footerRow
            }
            .padding(.horizontal, 20)
            .padding(.top, 24)
            .padding(.bottom, 28)
        }
    }

    private func header(_ profile: HostProfile) -> some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                Text("\(profile.nickname) 선생님")
                    .font(.system(size: 22, weight: .bold))
                    .kerning(-0.44)
                    .foregroundColor(PassmateColors.textPrimary)
                if let level = localLevel(profile) {
                    ReputationBadgeView(level: level)
                }
                if let intro = profile.intro {
                    Text(intro)
                        .font(.system(size: 13))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.textSecondary)
                }
            }
            Spacer()
            if let level = localLevel(profile) {
                LevelEmblemView(level: level)
                    .frame(width: 48, height: 48)
            }
        }
    }

    private func statsRow(_ profile: HostProfile) -> some View {
        HStack(spacing: 0) {
            statCell(value: profile.avgStars.map { formatRating($0.doubleValue) } ?? "-", label: "평균 평가")
            statCell(value: "\(profile.roomCount)회", label: "방 운영")
            statCell(value: "\(profile.totalStudents)명", label: "누적 학생")
        }
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity)
        .background(PassmateColors.fieldGray)
        .cornerRadius(16)
    }

    private func statCell(value: String, label: String) -> some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.system(size: 17, weight: .bold))
                .kerning(-0.34)
                .foregroundColor(PassmateColors.textPrimary)
            Text(label)
                .font(.system(size: 12))
                .kerning(-0.24)
                .foregroundColor(PassmateColors.textSecondary)
        }
        .frame(maxWidth: .infinity)
    }

    private func badgeSection(_ profile: HostProfile) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("획득한 뱃지")
                    .font(.system(size: 16, weight: .bold))
                    .kerning(-0.32)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
                Text("\(profile.badges.count)개")
                    .font(.system(size: 13, weight: .medium))
                    .kerning(-0.26)
                    .foregroundColor(PassmateColors.textSecondary)
            }
            HStack(spacing: 8) {
                ForEach(badgeTypes(profile), id: \.self) { badge in
                    Text(badgeGlyph(badge))
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(PassmateColors.primaryDeep)
                        .frame(width: 40, height: 40)
                        .background(PassmateColors.backgroundMint)
                        .cornerRadius(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(PassmateColors.primary, lineWidth: 1))
                }
            }
        }
    }

    @ViewBuilder
    private var footerRow: some View {
        if uiState.isReported {
            Text("신고가 접수됐어요")
                .font(.system(size: 13, weight: .medium))
                .kerning(-0.26)
                .foregroundColor(PassmateColors.textSecondary)
                .padding(.top, 4)
        } else {
            HStack(spacing: 6) {
                Button(action: onClickReport) {
                    Text("프로필 신고")
                        .font(.system(size: 13))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.textTertiary)
                }
                Text("·")
                    .font(.system(size: 13))
                    .foregroundColor(PassmateColors.textTertiary)
                Button(action: onClickBlock) {
                    Text("차단")
                        .font(.system(size: 13))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.textTertiary)
                }
            }
            .padding(.top, 4)
        }
    }

    private func localLevel(_ profile: HostProfile) -> HostLevel? {
        guard let level = profile.level else { return nil }

        return HostLevel.from(Int(level.level))
    }

    private func hostRooms(_ profile: HostProfile) -> [PublicRoom] {
        profile.rooms.compactMap { $0 as? PublicRoom }
    }

    private func badgeTypes(_ profile: HostProfile) -> [BadgeType] {
        profile.badges.compactMap { $0 as? BadgeType }
    }

    private func badgeGlyph(_ type: BadgeType) -> String {
        if type == BadgeType.firstRoom {
            return "⚑"
        } else if type == BadgeType.rooms10 {
            return "10"
        } else if type == BadgeType.students100 {
            return "100"
        } else if type == BadgeType.rating45 {
            return "★"
        } else if type == BadgeType.ratings50 {
            return "50"
        } else if type == BadgeType.streak30 {
            return "30"
        } else if type == BadgeType.firstPaidRoom {
            return "₩"
        } else {
            return "AI"
        }
    }

    private func formatRating(_ value: Double) -> String {
        String(format: "%.1f", value)
    }
}

private struct HostRoomRowView: View {
    let room: PublicRoom

    let onClickJoin: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text(room.title)
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
                if room.isPaid {
                    Text("유료 \(room.entryFee?.intValue ?? 0) C")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(PassmateColors.chipGoldText)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(PassmateColors.chipGold)
                        .clipShape(Capsule())
                }
            }
            HStack {
                Text(participantsText)
                    .font(.system(size: 12))
                    .kerning(-0.24)
                    .foregroundColor(PassmateColors.textTertiary)
                Spacer()
                Button(action: onClickJoin) {
                    Text("참여하기")
                        .font(.system(size: 13, weight: .medium))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.surface)
                        .padding(.horizontal, 14)
                        .frame(height: 32)
                        .background(PassmateColors.primary)
                        .cornerRadius(10)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
    }

    private var participantsText: String {
        let current = room.participantCount?.intValue ?? 0

        if let max = room.maxParticipants?.intValue {
            return "참여 \(current) / \(max) 명"
        } else {
            return "참여 \(current) 명"
        }
    }
}
