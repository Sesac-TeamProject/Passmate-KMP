import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-02(349:9252) 미러 — 입장 완료 카드 + 참가자 실시간 표시
struct WaitingView: View {
    let pin: String

    var onSessionStarted: (String) -> Void = { _ in }

    var onRoomClosed: () -> Void = {}

    var onLeft: () -> Void = {}

    @StateObject private var viewModel = WaitingViewModel(
        getRoomInfoUseCase: KoinHelper.shared.getRoomInfoUseCase(),
        getParticipantsUseCase: KoinHelper.shared.getParticipantsUseCase(),
        leaveRoomUseCase: KoinHelper.shared.leaveRoomUseCase(),
        getMyParticipationUseCase: KoinHelper.shared.getMyParticipationUseCase(),
        eventWatcher: KoinHelper.shared.sessionEventStreamWatcher()
    )

    @State private var noticeMessage: String?

    var body: some View {
        WaitingContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) }
        )
        .onAppear {
            viewModel.action(.enter(pin: pin))
        }
        .onDisappear {
            viewModel.stopWatching()
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case let .sessionStarted(pin):
                onSessionStarted(pin)
            case let .roomClosed(message):
                noticeMessage = message
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                    onRoomClosed()
                }
            case .left:
                onLeft()
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .overlay(alignment: .bottom) {
            if let noticeMessage {
                WaitingNoticeToast(message: noticeMessage)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
    }
}

private struct WaitingContentView: View {
    let uiState: WaitingUiState

    let onAction: (WaitingAction) -> Void

    var body: some View {
        VStack(spacing: 0) {
            header
            if uiState.isLoading {
                Spacer()
                ProgressView()
                    .tint(PassmateColors.primary)
                Spacer()
            } else {
                enteredCard
                Spacer()
                PassmateWaitingDots()
                    .padding(.bottom, 40)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private var header: some View {
        ZStack(alignment: .topTrailing) {
            VStack(alignment: .leading, spacing: 6) {
                Text(uiState.roomTitle)
                    .font(.system(size: 20, weight: .bold))
                    .kerning(-0.4)
                    .foregroundColor(PassmateColors.textPrimary)
                Text("PIN \(formattedPin)")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                onAction(.clickLeave)
            } label: {
                Text("나가기")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textSecondary)
                    .padding(4)
            }
        }
        .padding(.horizontal, 24)
        .padding(.top, 40)
        .padding(.bottom, 48)
    }

    private var formattedPin: String {
        let digits = Array(uiState.pin)

        return stride(from: 0, to: digits.count, by: 3)
            .map { String(digits[$0..<min($0 + 3, digits.count)]) }
            .joined(separator: " ")
    }

    private var enteredCard: some View {
        PassmateCard {
            VStack(spacing: 16) {
                ZStack {
                    Circle()
                        .fill(PassmateColors.fieldGray)
                        .frame(width: 88, height: 88)
                    PassmateMascotView(mascot: .waiting)
                        .frame(width: 60, height: 66)
                }
                Text("입장 완료!")
                    .font(.system(size: 24, weight: .bold))
                    .kerning(-0.48)
                    .foregroundColor(PassmateColors.textPrimary)
                Text(waitingMessage)
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textSecondary)
                participantAvatarRow
                Text("학생 \(uiState.totalCount)명이 함께해요")
                    .font(.system(size: 14))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textSecondary)
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 30)
        }
        .padding(.horizontal, 20)
    }

    private var waitingMessage: String {
        if let nickname = uiState.myNickname {
            return "\(nickname) 님, 선생님이 곧 시작해요"
        } else {
            return "선생님이 곧 시작해요"
        }
    }

    // 아바타 행은 나를 제외한 참가자를 보여준다 (M-02: 본인은 문구, 다른 학생은 아바타)
    private var participantAvatarRow: some View {
        let others = uiState.participants.filter { $0.participantId != uiState.myParticipantId }
        let visible = Array(others.prefix(4))
        let overflow = others.count - visible.count

        return HStack(spacing: 6) {
            ForEach(visible, id: \.participantId) { participant in
                StudentAvatarView(avatarId: participant.avatarId?.intValue ?? StudentAvatars.defaultId)
                    .frame(width: 34, height: 34)
            }
            if overflow > 0 {
                ZStack {
                    Circle()
                        .fill(PassmateColors.fieldGray)
                    Text("+\(overflow)")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textSecondary)
                }
                .frame(width: 34, height: 34)
            }
        }
    }
}

private struct WaitingNoticeToast: View {
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

// MARK: - 프리뷰 (Figma 시안 비교용, Koin 미초기화 상태에서도 안전한 콘텐츠 뷰 기반)

#Preview("참가자 3명") {
    WaitingContentView(
        uiState: WaitingUiState(
            isLoading: false,
            roomTitle: "8월 4주차 Spring 스터디",
            pin: "482913",
            myParticipantId: 9001,
            myNickname: "민지",
            participants: [
                Participant(participantId: 9001, nickname: "민지", avatarId: KotlinInt(int: 1), isGuest: false, isConnected: true),
                Participant(participantId: 9002, nickname: "준영", avatarId: KotlinInt(int: 2), isGuest: false, isConnected: true),
                Participant(participantId: 9003, nickname: "혜림", avatarId: KotlinInt(int: 5), isGuest: false, isConnected: true)
            ],
            totalCount: 3
        ),
        onAction: { _ in }
    )
}

#Preview("참가자 0명") {
    WaitingContentView(
        uiState: WaitingUiState(
            isLoading: false,
            roomTitle: "8월 4주차 Spring 스터디",
            pin: "482913",
            myParticipantId: 9001,
            myNickname: "민지",
            participants: [],
            totalCount: 0
        ),
        onAction: { _ in }
    )
}
