import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-T2(349:10123) 미러 — 진행 리모컨: 프로젝터는 벽, 폰은 조작.
// 화면 전환(SESSION_ENDED→리포트)은 서버 이벤트로만 일어난다 (규칙 §2-1-2)
struct SessionControlView: View {
    let roomId: Int64

    let pin: String

    var onRequireSignIn: () -> Void = {}

    var onSessionEnded: (Int64) -> Void = { _ in }

    var onBack: () -> Void = {}

    @StateObject private var viewModel = SessionControlViewModel(
        getRoomInfoUseCase: KoinHelper.shared.getRoomInfoUseCase(),
        getSessionSnapshotUseCase: KoinHelper.shared.getSessionSnapshotUseCase(),
        getSubmissionsUseCase: KoinHelper.shared.getSubmissionsUseCase(),
        startSessionUseCase: KoinHelper.shared.startSessionUseCase(),
        nextQuestionUseCase: KoinHelper.shared.nextQuestionUseCase(),
        endCurrentQuestionUseCase: KoinHelper.shared.endCurrentQuestionUseCase(),
        endSessionUseCase: KoinHelper.shared.endSessionUseCase(),
        setScreenLockUseCase: KoinHelper.shared.setScreenLockUseCase(),
        publishVoiceHintUseCase: KoinHelper.shared.publishVoiceHintUseCase(),
        eventWatcher: KoinHelper.shared.sessionEventStreamWatcher(),
        isSignedInUseCase: KoinHelper.shared.isSignedInUseCase()
    )

    @State private var showEndConfirm = false

    @State private var noticeMessage: String?

    var body: some View {
        SessionControlContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onClickBack: onBack,
            onClickEndSession: { showEndConfirm = true }
        )
        .onAppear {
            viewModel.action(.enter(roomId: roomId, pin: pin))
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .requireSignIn:
                onRequireSignIn()
            case let .sessionEnded(roomId):
                onSessionEnded(roomId)
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .alert("세션 종료", isPresented: $showEndConfirm) {
            Button("종료", role: .destructive) {
                viewModel.action(.confirmEndSession)
            }
            Button("취소", role: .cancel) {}
        } message: {
            Text("세션을 종료하면 최종 결과가 확정되고 학생별 리포트가 생성돼요.")
        }
        .overlay(alignment: .bottom) {
            if let noticeMessage {
                SessionControlNoticeToast(message: noticeMessage)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
    }
}

private struct SessionControlNoticeToast: View {
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

private struct SessionControlContentView: View {
    let uiState: SessionControlUiState

    let onAction: (SessionControlAction) -> Void

    let onClickBack: () -> Void

    let onClickEndSession: () -> Void

    // PTT 녹음 — 순수 UI 상태(레코더·누름 상태)만 콘텐츠 뷰에 둔다 (규칙 §11-1)
    @State private var recorder = VoiceHintRecorder()

    @State private var isRecording = false

    @State private var isPttPressed = false

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
            Text("방 상태를 불러오지 못했어요")
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
                HStack(spacing: 12) {
                    PassmateBackButton(onClick: onClickBack)
                    Text(uiState.roomTitle)
                        .font(.system(size: 17, weight: .bold))
                        .kerning(-0.34)
                        .foregroundColor(PassmateColors.textPrimary)
                    Spacer()
                    Text("PIN \(formatPin(uiState.pin))")
                        .font(.system(size: 14, weight: .bold))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.primaryDeep)
                }
                projectorChip
                if uiState.status == RoomStatus.waiting {
                    waitingPanel
                } else {
                    questionCard
                    pttButton
                    controlButtons
                }
                bottomControls
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 24)
        }
    }

    private var projectorChip: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(uiState.isProjectorConnected ? PassmateColors.primary : PassmateColors.textTertiary)
                .frame(width: 7, height: 7)
            Text(uiState.isProjectorConnected ? "프로젝터 연결됨 · 벽 화면과 동기화 중" : "프로젝터 미연결 · 웹에서 프로젝터 화면을 열어 주세요")
                .font(.system(size: 12, weight: .medium))
                .kerning(-0.24)
                .foregroundColor(PassmateColors.primaryDeep)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(PassmateColors.backgroundMint)
        .clipShape(Capsule())
    }

    private var waitingPanel: some View {
        VStack(spacing: 14) {
            Text("학생 \(uiState.participantCount)명 대기 중")
                .font(.system(size: 18, weight: .bold))
                .kerning(-0.36)
                .foregroundColor(PassmateColors.textPrimary)
            Text("시작하면 전체 학생에게 첫 문항이 공개돼요")
                .font(.system(size: 13))
                .kerning(-0.26)
                .foregroundColor(PassmateColors.textSecondary)
            Button {
                onAction(.clickStart)
            } label: {
                Group {
                    if uiState.isControlling {
                        ProgressView().tint(PassmateColors.surface)
                    } else {
                        Text("세션 시작")
                            .font(.system(size: 15, weight: .bold))
                            .kerning(-0.3)
                            .foregroundColor(PassmateColors.surface)
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(PassmateColors.primary)
                .cornerRadius(16)
            }
            .disabled(uiState.isControlling)
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(PassmateColors.border, lineWidth: 1))
    }

    private var questionCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let question = uiState.question {
                HStack(spacing: 8) {
                    Text("Q\(question.questionNo) / \(uiState.questionCount.map(String.init) ?? "-")")
                        .font(.system(size: 18, weight: .bold))
                        .kerning(-0.36)
                        .foregroundColor(PassmateColors.textPrimary)
                    Text(question.type.displayLabel)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(PassmateColors.ratingTagSelectedText)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(PassmateColors.ratingTagSelectedBg)
                        .cornerRadius(8)
                    Spacer()
                }
                PassmateTimerBar(
                    remainingSeconds: Int(uiState.remainingSec),
                    totalSeconds: Int(question.timeLimitSec)
                )
                Text(question.body)
                    .font(.system(size: 15, weight: .medium))
                    .kerning(-0.3)
                    .foregroundColor(PassmateColors.textPrimary)
                if let submissions = uiState.submissions {
                    SubmissionSectionView(submissions: submissions)
                }
            } else {
                Text("다음 문항을 시작해 주세요")
                    .font(.system(size: 14))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textSecondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(PassmateColors.border, lineWidth: 1))
    }

    // "길게 눌러 힌트 말하기" (M-T2, T121) — 누르는 동안 녹음, 놓으면 업로드
    private var pttButton: some View {
        let label: String
        if uiState.isSendingHint {
            label = "힌트 보내는 중…"
        } else if isRecording {
            label = "녹음 중… 놓으면 전송돼요"
        } else {
            label = "🎙 길게 눌러 힌트 말하기 (PTT)"
        }

        return Group {
            if uiState.isSendingHint {
                ProgressView().tint(PassmateColors.primaryDeep)
            } else {
                Text(label)
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(isRecording ? PassmateColors.surface : PassmateColors.primaryDeep)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 52)
        .background(isRecording ? PassmateColors.primary : PassmateColors.surface)
        .cornerRadius(16)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.primary, lineWidth: 1))
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in
                    if !isPttPressed {
                        isPttPressed = true
                        if !uiState.isSendingHint {
                            if recorder.start() {
                                isRecording = true
                            } else {
                                onAction(.notice(message: "마이크 권한을 허용한 뒤 다시 길게 눌러 주세요"))
                            }
                        }
                    }
                }
                .onEnded { _ in
                    isPttPressed = false
                    if isRecording {
                        isRecording = false
                        if let hint = recorder.stop() {
                            onAction(.sendVoiceHint(hint: hint))
                        } else {
                            onAction(.notice(message: "녹음이 너무 짧아요 · 길게 눌러 말해 주세요"))
                        }
                    }
                }
        )
    }

    private var controlButtons: some View {
        HStack(spacing: 10) {
            Button {
                onAction(.clickEndQuestion)
            } label: {
                Text("바로 마감")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(PassmateColors.border, lineWidth: 1))
            }
            .disabled(uiState.isControlling || uiState.question == nil || uiState.isQuestionClosed)
            Button {
                onAction(.clickNext)
            } label: {
                Group {
                    if uiState.isControlling {
                        ProgressView().tint(PassmateColors.surface)
                    } else {
                        Text("다음 문항 →")
                            .font(.system(size: 14, weight: .bold))
                            .kerning(-0.28)
                            .foregroundColor(PassmateColors.surface)
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(PassmateColors.primary)
                .cornerRadius(14)
            }
            .disabled(uiState.isControlling)
        }
    }

    private var bottomControls: some View {
        HStack {
            Button {
                onAction(.toggleLock)
            } label: {
                Text(uiState.isLocked ? "학생 화면 잠금 해제" : "학생 화면 잠금")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textSecondary)
            }
            .disabled(uiState.isControlling)
            Spacer()
            Button(action: onClickEndSession) {
                Text("세션 종료")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.weakTopicText)
            }
        }
    }


    private func formatPin(_ pin: String) -> String {
        stride(from: 0, to: pin.count, by: 3).map { start in
            let begin = pin.index(pin.startIndex, offsetBy: start)
            let end = pin.index(begin, offsetBy: min(3, pin.count - start))
            return String(pin[begin..<end])
        }.joined(separator: " ")
    }
}

private struct SubmissionSectionView: View {
    let submissions: SubmissionStatus

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("제출 \(submissions.submittedCount) / \(submissions.totalCount)")
                    .font(.system(size: 15, weight: .bold))
                    .kerning(-0.3)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
                if let accuracy = submissions.accuracyPercent?.intValue {
                    Text("정답률 (실시간) \(accuracy)%")
                        .font(.system(size: 13))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.textSecondary)
                }
            }
            avatarRow
            ForEach(Array(choiceCounts.enumerated()), id: \.element.label) { index, choice in
                choiceBar(choice: choice, index: index)
            }
        }
    }

    private var avatarRow: some View {
        let participants = submissionParticipants
        let submitted = participants.filter { $0.submitted }
        let pendingCount = participants.filter { !$0.submitted }.count

        return HStack(spacing: 6) {
            ForEach(submitted, id: \.participantId) { participant in
                ZStack(alignment: .bottomTrailing) {
                    StudentAvatarView(avatarId: participant.avatarId.map { Int(truncating: $0) } ?? 0)
                        .frame(width: 30, height: 30)
                    Circle()
                        .fill(PassmateColors.primary)
                        .frame(width: 9, height: 9)
                        .overlay(Circle().stroke(PassmateColors.surface, lineWidth: 1))
                }
            }
            if pendingCount > 0 {
                Text("미제출 \(pendingCount)명")
                    .font(.system(size: 13))
                    .kerning(-0.26)
                    .foregroundColor(PassmateColors.textTertiary)
            }
            Spacer()
        }
    }

    private func choiceBar(choice: ChoiceCount, index: Int) -> some View {
        let color = choiceColor(index)
        let total = max(Int(submissions.submittedCount), 1)
        let fraction = CGFloat(choice.count) / CGFloat(total)

        return HStack(spacing: 8) {
            Text(choice.label)
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(PassmateColors.textPrimary)
                .frame(width: 22, height: 22)
                .background(color.opacity(0.35))
                .cornerRadius(6)
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(PassmateColors.fieldGray)
                    if fraction > 0 {
                        Capsule()
                            .fill(color)
                            .frame(width: geo.size.width * min(fraction, 1))
                    }
                }
            }
            .frame(height: 8)
            Text("\(choice.count)명")
                .font(.system(size: 13))
                .kerning(-0.26)
                .foregroundColor(PassmateColors.textSecondary)
        }
    }

    private var choiceCounts: [ChoiceCount] {
        submissions.choices.compactMap { $0 as? ChoiceCount }
    }

    private var submissionParticipants: [SubmissionParticipant] {
        submissions.participants.compactMap { $0 as? SubmissionParticipant }
    }

    private func choiceColor(_ index: Int) -> Color {
        switch index % 4 {
        case 0: return PassmateColors.wrongPink
        case 1: return PassmateColors.chipBlue
        case 2: return PassmateColors.timerAmber
        default: return PassmateColors.chipGreen
        }
    }
}
