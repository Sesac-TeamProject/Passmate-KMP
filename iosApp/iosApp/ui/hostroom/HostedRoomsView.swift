import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-13(384:5121) 미러 — 내가 만든 방: 명성 카드+진행 중/종료 목록+새 방 만들기 FAB.
// 진행 리모컨(M-T2)·방 리포트(M-14) 연결은 후속 태스크(T118·T119)
struct HostedRoomsView: View {
    var onRequireSignIn: () -> Void = {}

    var onOpenReputation: () -> Void = {}

    var onOpenRoomReport: (Int64) -> Void = { _ in }

    var onOpenSessionControl: (Int64, String) -> Void = { _, _ in }

    @StateObject private var viewModel = HostedRoomsViewModel(
        getHostedRoomsUseCase: KoinHelper.shared.getHostedRoomsUseCase(),
        getMyGradeUseCase: KoinHelper.shared.getMyGradeUseCase(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    @State private var isCreateSheetVisible = false

    @State private var noticeMessage: String?

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            HostedRoomsContentView(
                uiState: viewModel.uiState,
                onAction: { viewModel.action($0) }
            )
            createFab
        }
        .onAppear {
            viewModel.action(.enter)
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .requireSignIn:
                onRequireSignIn()
            case .openCreateSheet:
                isCreateSheetVisible = true
            case .openReputation:
                onOpenReputation()
            case let .openRoomReport(roomId):
                onOpenRoomReport(roomId)
            case let .openSessionControl(roomId, pin):
                onOpenSessionControl(roomId, pin)
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .sheet(isPresented: $isCreateSheetVisible) {
            CreateRoomSheetView(
                onCreated: { pin in
                    isCreateSheetVisible = false
                    viewModel.action(.roomCreated(pin: pin))
                },
                onNotice: { message in
                    viewModel.action(.notice(message: message))
                },
                onClose: { isCreateSheetVisible = false }
            )
            .presentationDetents([.large])
        }
        .overlay(alignment: .bottom) {
            if let noticeMessage {
                HostedRoomsNoticeToast(message: noticeMessage)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
    }

    private var createFab: some View {
        Button {
            viewModel.action(.clickCreate)
        } label: {
            Text("+")
                .font(.system(size: 28, weight: .medium))
                .foregroundColor(PassmateColors.surface)
                .frame(width: 56, height: 56)
                .background(PassmateColors.primary)
                .clipShape(Circle())
        }
        .padding(24)
    }
}

private struct HostedRoomsNoticeToast: View {
    let message: String

    var body: some View {
        Text(message)
            .font(.system(size: 13))
            .foregroundColor(PassmateColors.surface)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(PassmateColors.textPrimary.opacity(0.9))
            .cornerRadius(10)
            .padding(.bottom, 16)
    }
}

private struct HostedRoomsContentView: View {
    let uiState: HostedRoomsUiState

    let onAction: (HostedRoomsAction) -> Void

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
            Text("방 목록을 불러오지 못했어요")
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
                Text("내가 만든 방")
                    .font(.system(size: 24, weight: .bold))
                    .kerning(-0.48)
                    .foregroundColor(PassmateColors.textPrimary)
                if let grade = uiState.grade {
                    GradeSummaryCardView(grade: grade, onClick: { onAction(.clickReputation) })
                }
                if uiState.ongoing.isEmpty, uiState.ended.isEmpty {
                    emptyRooms
                }
                if !uiState.ongoing.isEmpty {
                    RoomSectionView(
                        chipLabel: "진행 중",
                        isOngoing: true,
                        rooms: uiState.ongoing,
                        onClickRoom: { room in onAction(.clickOngoingRoom(roomId: room.roomId, pin: room.pin)) }
                    )
                }
                if !uiState.ended.isEmpty {
                    RoomSectionView(
                        chipLabel: "종료",
                        isOngoing: false,
                        rooms: uiState.ended,
                        onClickRoom: { room in onAction(.clickEndedRoom(roomId: room.roomId)) }
                    )
                }
                if uiState.nextCursor != nil {
                    loadMoreButton
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 32)
            .padding(.bottom, 96)
        }
    }

    private var emptyRooms: some View {
        VStack(spacing: 4) {
            Text("아직 만든 방이 없어요")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
            Text("+ 버튼으로 첫 방을 만들어 보세요")
                .font(.system(size: 13))
                .kerning(-0.26)
                .foregroundColor(PassmateColors.textTertiary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
    }

    private var loadMoreButton: some View {
        Button {
            onAction(.loadMore)
        } label: {
            Group {
                if uiState.isLoadingMore {
                    ProgressView().tint(PassmateColors.primary)
                } else {
                    Text("더 보기")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.primaryDeep)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(PassmateColors.border, lineWidth: 1))
        }
        .disabled(uiState.isLoadingMore)
    }
}

private struct GradeSummaryCardView: View {
    let grade: MyGrade

    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 12) {
                LevelEmblemView(level: HostLevel.from(Int(grade.level.level)) ?? .seedling)
                    .frame(width: 36, height: 36)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Lv.\(grade.level.level) \(grade.level.label)")
                        .font(.system(size: 16, weight: .bold))
                        .kerning(-0.32)
                        .foregroundColor(PassmateColors.textPrimary)
                    Text(statsLine)
                        .font(.system(size: 13))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.textSecondary)
                }
                Spacer()
                Text("명성 상세 ›")
                    .font(.system(size: 13, weight: .medium))
                    .kerning(-0.26)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(PassmateColors.surface)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
        }
    }

    private var statsLine: String {
        var parts = ["방 운영 \(grade.stats.roomCount)회"]

        if let stars = grade.stats.avgStars?.doubleValue {
            parts.append("별점 \(String(format: "%.1f", stars))")
        }
        parts.append("학생 \(grade.stats.totalStudents)명")

        return parts.joined(separator: " · ")
    }
}

private struct RoomSectionView: View {
    let chipLabel: String

    let isOngoing: Bool

    let rooms: [HostedRoom]

    let onClickRoom: (HostedRoom) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 8) {
                Text(chipLabel)
                    .font(.system(size: 12, weight: .medium))
                    .kerning(-0.24)
                    .foregroundColor(isOngoing ? PassmateColors.surface : PassmateColors.textSecondary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(isOngoing ? PassmateColors.primary : PassmateColors.fieldGray)
                    .clipShape(Capsule())
                Text("\(rooms.count)개")
                    .font(.system(size: 13))
                    .kerning(-0.26)
                    .foregroundColor(PassmateColors.textSecondary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            ForEach(Array(rooms.enumerated()), id: \.element.roomId) { index, room in
                if index > 0 {
                    Divider().background(PassmateColors.border)
                }
                HostedRoomRowView(
                    room: room,
                    isOngoing: isOngoing,
                    onClick: { onClickRoom(room) }
                )
            }
        }
        .background(isOngoing ? PassmateColors.backgroundMint : PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

private struct HostedRoomRowView: View {
    let room: HostedRoom

    let isOngoing: Bool

    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 8) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(room.title)
                        .font(.system(size: 15, weight: .bold))
                        .kerning(-0.3)
                        .foregroundColor(PassmateColors.textPrimary)
                    Text(subtitle)
                        .font(.system(size: 13))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.textSecondary)
                }
                Spacer()
                Text(isOngoing ? "진행 ›" : "상세 ›")
                    .font(.system(size: 13, weight: .medium))
                    .kerning(-0.26)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(PassmateColors.surface)
        }
    }

    private var subtitle: String {
        var parts: [String] = []

        if isOngoing {
            if let count = room.participantCount?.intValue {
                parts.append("학생 \(count)명")
            }
            parts.append("PIN \(formatPin(room.pin))")
            if let scheduledAt = room.scheduledAt {
                parts.append("\(formatTime(scheduledAt)) 시작")
            }
        } else {
            if let endedAtLabel = room.endedAtLabel {
                parts.append(endedAtLabel)
            }
            if let count = room.participantCount?.intValue {
                parts.append("학생 \(count)명")
            }
            if let accuracy = room.avgAccuracyPercent?.intValue {
                parts.append("평균 \(accuracy)%")
            }
        }
        return parts.joined(separator: " · ")
    }

    private func formatPin(_ pin: String) -> String {
        stride(from: 0, to: pin.count, by: 3).map { start in
            let begin = pin.index(pin.startIndex, offsetBy: start)
            let end = pin.index(begin, offsetBy: min(3, pin.count - start))
            return String(pin[begin..<end])
        }.joined(separator: " ")
    }

    // ISO 문자열에서 HH:mm만 취한다 — 시간 표시는 서버가 준 예정 시각 렌더링 전용
    private func formatTime(_ isoTime: String) -> String {
        let timePart = isoTime.split(separator: "T").dropFirst().first.map(String.init) ?? ""

        if timePart.count >= 5 {
            return String(timePart.prefix(5))
        } else {
            return isoTime
        }
    }
}
