import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-03(349:9277)·M-04(349:9333)·M-05(349:9352) 미러 —
// 문항 풀이·제출 결과·최종 결과를 서버 이벤트 단계(Phase)로 렌더링한다
struct PlayView: View {
    let pin: String

    var onLeft: () -> Void = {}

    var onRoomClosed: () -> Void = {}

    var onOpenResult: (Int64) -> Void = { _ in }

    @StateObject private var viewModel = PlayViewModel(
        getRoomInfoUseCase: KoinHelper.shared.getRoomInfoUseCase(),
        getSessionSnapshotUseCase: KoinHelper.shared.getSessionSnapshotUseCase(),
        submitAnswerUseCase: KoinHelper.shared.submitAnswerUseCase(),
        getVoiceHintsUseCase: KoinHelper.shared.getVoiceHintsUseCase(),
        leaveRoomUseCase: KoinHelper.shared.leaveRoomUseCase(),
        getMyParticipationUseCase: KoinHelper.shared.getMyParticipationUseCase(),
        snapshotPolicy: KoinHelper.shared.snapshotPolicy(),
        eventWatcher: KoinHelper.shared.sessionEventStreamWatcher()
    )

    @StateObject private var voiceHintPlayer = VoiceHintAudioPlayer()

    @State private var isLeaveDialogVisible = false

    @State private var noticeMessage: String?

    var body: some View {
        PlayContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onClickLeave: { isLeaveDialogVisible = true }
        )
        // 음성 힌트 배너 — 오버레이 소유는 컨테이너 (규칙 §11-1)
        .overlay(alignment: .bottom) {
            if let hint = viewModel.uiState.activeVoiceHint, viewModel.uiState.phase != .finished {
                VoiceHintBannerView(
                    hint: hint,
                    player: voiceHintPlayer,
                    onReplay: { viewModel.action(.clickReplayHint) }
                )
                .padding(.horizontal, 20)
                .padding(.bottom, 84)
            }
        }
        .onAppear {
            viewModel.action(.enter(pin: pin))
        }
        .onDisappear {
            viewModel.stopWatching()
            voiceHintPlayer.stop()
        }
        .onChange(of: viewModel.uiState.activeVoiceHint?.hintId) { hintId in
            // 문항 전환·세션 종료로 배너가 사라지면 재생도 함께 멈춘다
            if hintId == nil {
                voiceHintPlayer.stop()
            }
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case let .playVoiceHint(hint):
                voiceHintPlayer.play(url: hint.clipUrl)
            case let .openResult(roomId):
                onOpenResult(roomId)
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
        // 진행 중 퇴장은 확인 다이얼로그를 거친다 (규칙 §2-1-2)
        .alert("방을 나갈까요?", isPresented: $isLeaveDialogVisible) {
            Button("계속 풀기", role: .cancel) {}
            Button("나가기", role: .destructive) {
                viewModel.action(.confirmLeave)
            }
        } message: {
            Text("진행 중인 세션에서 나가면 남은 문항을 풀 수 없어요.")
        }
        .overlay(alignment: .bottom) {
            if let noticeMessage {
                PlayNoticeToast(message: noticeMessage)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
    }
}

private struct PlayContentView: View {
    let uiState: PlayUiState

    let onAction: (PlayAction) -> Void

    let onClickLeave: () -> Void

    var body: some View {
        Group {
            if uiState.isLoading {
                ProgressView()
                    .tint(PassmateColors.primary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if uiState.phase == .finished {
                FinalResultContent(uiState: uiState, onAction: onAction)
            } else if uiState.phase == .question && !uiState.hasSubmitted {
                QuestionContent(uiState: uiState, onAction: onAction, onClickLeave: onClickLeave)
            } else {
                WaitingNextContent(uiState: uiState, onClickLeave: onClickLeave)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PassmateColors.surface.ignoresSafeArea())
    }
}

// ─── M-03 문항 풀이 ───

private struct QuestionContent: View {
    let uiState: PlayUiState

    let onAction: (PlayAction) -> Void

    let onClickLeave: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 12) {
                    header
                    if uiState.isLocked {
                        lockedBanner
                    }
                    if let question = uiState.question {
                        QuestionCard(uiState: uiState, question: question, onAction: onAction)
                    }
                }
            }
            submitButton
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Q\(uiState.question.map { String($0.questionNo) } ?? "-") / \(uiState.questionCount) · \(questionTypeLabel(uiState.question?.type))")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
                Spacer()
                Button(action: onClickLeave) {
                    Text("나가기")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textSecondary)
                }
            }
            if uiState.questionCount > 0 {
                HStack(spacing: 4) {
                    ForEach(0..<uiState.questionCount, id: \.self) { index in
                        let isReached = index < Int(uiState.question?.questionNo ?? 0)

                        RoundedRectangle(cornerRadius: 3)
                            .fill(isReached ? PassmateColors.timerAmber : PassmateColors.fieldGray)
                            .frame(width: isReached ? 26 : 12, height: 6)
                    }
                    Spacer()
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 24)
    }

    private var lockedBanner: some View {
        Text("선생님이 화면을 잠갔어요")
            .font(.system(size: 14, weight: .medium))
            .kerning(-0.28)
            .foregroundColor(PassmateColors.textSecondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(PassmateColors.fieldGray)
            .cornerRadius(12)
            .padding(.horizontal, 20)
    }

    private var submitButton: some View {
        Button {
            onAction(.clickSubmit)
        } label: {
            Group {
                if uiState.isSubmitting {
                    ProgressView()
                        .tint(PassmateColors.surface)
                } else {
                    Text("제출하기")
                        .font(.system(size: 16, weight: .medium))
                        .kerning(-0.32)
                        .foregroundColor(PassmateColors.surface)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 54)
            .background(PassmateColors.primary)
            .cornerRadius(16)
        }
        .disabled(uiState.isSubmitting)
        .padding(.horizontal, 20)
        .padding(.bottom, 16)
    }
}

private struct QuestionCard: View {
    let uiState: PlayUiState

    let question: SessionQuestion

    let onAction: (PlayAction) -> Void

    var body: some View {
        ZStack(alignment: .top) {
            PassmateCard {
                VStack(spacing: 12) {
                    Text(question.body)
                        .font(.system(size: 20, weight: .bold))
                        .kerning(-0.4)
                        .foregroundColor(PassmateColors.textPrimary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                    Spacer()
                        .frame(height: 24)
                    answerInputArea
                }
                .padding(.horizontal, 20)
                .padding(.top, 44)
                .padding(.bottom, 22)
            }
            .padding(.top, 30)
            TimerBadge(remainingSeconds: uiState.remainingSeconds)
        }
        .padding(.horizontal, 20)
        .padding(.top, 16)
    }

    @ViewBuilder
    private var answerInputArea: some View {
        if question.type == QuestionType.essay {
            EssayField(
                essayAnswer: uiState.essayAnswer,
                onChange: { onAction(.changeEssayAnswer(text: $0)) }
            )
        } else if question.type == QuestionType.ox {
            VStack(spacing: 12) {
                ForEach(Array(["O", "X"].enumerated()), id: \.offset) { index, label in
                    ChoiceRow(
                        chipLabel: label,
                        text: label,
                        chipIndex: index,
                        isSelected: uiState.selectedChoiceIndex == index,
                        onTap: { onAction(.selectChoice(index: index)) }
                    )
                }
            }
        } else {
            VStack(spacing: 12) {
                ForEach(Array(question.choices.enumerated()), id: \.offset) { index, choice in
                    ChoiceRow(
                        chipLabel: choiceLetter(index),
                        text: choice,
                        chipIndex: index,
                        isSelected: uiState.selectedChoiceIndex == index,
                        onTap: { onAction(.selectChoice(index: index)) }
                    )
                }
            }
        }
    }
}

private struct TimerBadge: View {
    let remainingSeconds: Int

    var body: some View {
        ZStack {
            Circle()
                .fill(PassmateColors.surface)
            Circle()
                .stroke(PassmateColors.timerAmber, lineWidth: 6)
            Text("\(remainingSeconds)")
                .font(.system(size: 20, weight: .bold))
                .kerning(-0.4)
                .foregroundColor(PassmateColors.primaryDeep)
        }
        .frame(width: 60, height: 60)
    }
}

private struct ChoiceRow: View {
    let chipLabel: String

    let text: String

    let chipIndex: Int

    let isSelected: Bool

    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Text(chipLabel)
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(isSelected ? PassmateColors.primaryDeep : chipTextColor(chipIndex))
                    .frame(width: 30, height: 30)
                    .background(isSelected ? PassmateColors.surface : chipColor(chipIndex))
                    .cornerRadius(10)
                Text(text)
                    .font(.system(size: 16, weight: .medium))
                    .kerning(-0.32)
                    .foregroundColor(isSelected ? PassmateColors.surface : PassmateColors.textPrimary)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if isSelected {
                    Text("✓")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(PassmateColors.surface)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .frame(minHeight: 56)
            .background(isSelected ? PassmateColors.primary : PassmateColors.fieldGray)
            .cornerRadius(14)
        }
    }
}

private struct EssayField: View {
    let essayAnswer: String

    let onChange: (String) -> Void

    var body: some View {
        ZStack(alignment: .topLeading) {
            TextEditor(
                text: Binding(
                    get: { essayAnswer },
                    set: { onChange($0) }
                )
            )
            .font(.system(size: 14))
            .frame(minHeight: 140)
            .padding(8)
            if essayAnswer.isEmpty {
                Text("답변을 입력해 주세요")
                    .font(.system(size: 14))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textSecondary)
                    .padding(16)
            }
        }
        .background(PassmateColors.fieldGray)
        .cornerRadius(14)
    }
}

// ─── M-04 제출 결과 · 다음 문항 대기 ───

private struct WaitingNextContent: View {
    let uiState: PlayUiState

    let onClickLeave: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Spacer()
                    .frame(maxWidth: .infinity)
                Text(uiState.question.map { "Q\($0.questionNo) / \(uiState.questionCount)" } ?? "잠시만요")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.primaryDeep)
                    .frame(maxWidth: .infinity)
                Button(action: onClickLeave) {
                    Text("나가기")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textSecondary)
                }
                .frame(maxWidth: .infinity, alignment: .trailing)
            }
            .padding(.horizontal, 20)
            .padding(.top, 24)
            Spacer()
            MyResultCard(uiState: uiState)
            Spacer()
            Text("다음 문항을 기다리고 있어요")
                .font(.system(size: 14))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
                .padding(.bottom, 40)
        }
    }
}

private struct MyResultCard: View {
    let uiState: PlayUiState

    var body: some View {
        PassmateCard {
            VStack(spacing: 14) {
                PassyMascotView()
                    .frame(width: 84, height: 92)
                Text(resultTitle(uiState))
                    .font(.system(size: 34, weight: .bold))
                    .kerning(-0.68)
                    .foregroundColor(
                        uiState.myAnswerResult?.correct?.boolValue == false
                            ? PassmateColors.textPrimary
                            : PassmateColors.primaryDeep
                    )
                if let rank = uiState.rank {
                    RankChip(rank: rank, rankDelta: uiState.myAnswerResult?.rankDelta.map { Int(truncating: $0) })
                }
                if let caption = resultCaption(uiState) {
                    Text(caption)
                        .font(.system(size: 14))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textSecondary)
                        .multilineTextAlignment(.center)
                }
                if let answer = uiState.reveal?.answer {
                    Text("정답 \(answer) · \(uiState.reveal?.correctAnswererCount ?? 0)명 정답")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.primaryDeep)
                        .multilineTextAlignment(.center)
                }
                if let explanation = uiState.reveal?.explanation {
                    Text(explanation)
                        .font(.system(size: 13))
                        .kerning(-0.26)
                        .foregroundColor(PassmateColors.textSecondary)
                        .multilineTextAlignment(.center)
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 34)
        }
        .padding(.horizontal, 20)
    }
}

private struct RankChip: View {
    let rank: Int

    let rankDelta: Int?

    var body: some View {
        HStack(spacing: 6) {
            Text("현재 \(rank)위")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textPrimary)
            if let rankDelta, rankDelta != 0 {
                Text(rankDelta > 0 ? "▲\(rankDelta)" : "▼\(-rankDelta)")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(rankDelta > 0 ? PassmateColors.primary : PassmateColors.textSecondary)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(PassmateColors.fieldGray)
        .cornerRadius(10)
    }
}

// ─── M-05 최종 결과 ───

private struct FinalResultContent: View {
    let uiState: PlayUiState

    let onAction: (PlayAction) -> Void

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 0) {
                    finalHeader
                    FinalRankingCard(uiState: uiState)
                        .padding(.horizontal, 20)
                        .offset(y: -40)
                }
            }
            Button {
                onAction(.clickViewReport)
            } label: {
                Text("내 리포트 보기")
                    .font(.system(size: 16, weight: .medium))
                    .kerning(-0.32)
                    .foregroundColor(PassmateColors.surface)
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                    .background(PassmateColors.primary)
                    .cornerRadius(16)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
        }
    }

    private var finalHeader: some View {
        ZStack(alignment: .topTrailing) {
            VStack(spacing: 14) {
                Text("최종 결과")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.inkGreen)
                PodiumRow(finalRanking: uiState.finalRanking)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 32)
            .padding(.bottom, 70)
            .background(PassmateColors.backgroundMint)
            PassyMascotView()
                .frame(width: 60, height: 66)
                .padding(.top, 16)
                .padding(.trailing, 6)
        }
    }
}

private struct PodiumRow: View {
    let finalRanking: [RankEntry]

    var body: some View {
        let top3 = finalRanking.filter { $0.rank >= 1 && $0.rank <= 3 }
        let ordered = [2, 1, 3].compactMap { rank in top3.first { Int($0.rank) == rank } }

        HStack(alignment: .bottom, spacing: 14) {
            ForEach(ordered, id: \.participantId) { entry in
                PodiumBlock(entry: entry)
            }
        }
    }
}

private struct PodiumBlock: View {
    let entry: RankEntry

    private var blockHeight: CGFloat {
        if entry.rank == 1 {
            return 102
        } else if entry.rank == 2 {
            return 74
        } else {
            return 62
        }
    }

    var body: some View {
        ZStack(alignment: .top) {
            VStack(spacing: 0) {
                Spacer()
                    .frame(height: 30)
                VStack {
                    Text("\(entry.rank)")
                        .font(.system(size: 16, weight: .medium))
                        .kerning(-0.32)
                        .foregroundColor(rankTextColor(Int(entry.rank)))
                        .padding(.top, 18)
                    Spacer()
                }
                .frame(width: 80, height: blockHeight)
                .background(rankColor(Int(entry.rank)))
                .cornerRadius(12)
            }
            StudentAvatarView(avatarId: entry.avatarId.map { Int(truncating: $0) } ?? StudentAvatars.defaultId)
                .frame(width: 44, height: 44)
        }
    }
}

private struct FinalRankingCard: View {
    let uiState: PlayUiState

    var body: some View {
        PassmateCard {
            VStack(spacing: 10) {
                Text(finalSummaryText(uiState))
                    .font(.system(size: 16, weight: .medium))
                    .kerning(-0.32)
                    .foregroundColor(PassmateColors.textPrimary)
                ForEach(uiState.finalRanking, id: \.participantId) { entry in
                    FinalRankingRow(
                        entry: entry,
                        isMe: entry.participantId == uiState.myParticipantId
                    )
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 22)
        }
    }
}

private struct FinalRankingRow: View {
    let entry: RankEntry

    let isMe: Bool

    var body: some View {
        HStack(spacing: 12) {
            Text("\(entry.rank)")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(rankTextColor(Int(entry.rank)))
                .frame(width: 24, height: 24)
                .background(rankColor(Int(entry.rank)))
                .clipShape(Circle())
            Text(isMe ? "나 (\(entry.nickname))" : entry.nickname)
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(isMe ? PassmateColors.primaryDeep : PassmateColors.textPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(formatScore(entry.total))
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(isMe ? PassmateColors.primaryDeep : PassmateColors.textPrimary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 11)
        .background(isMe ? PassmateColors.fieldGray : Color.clear)
        .cornerRadius(12)
    }
}

private struct PlayNoticeToast: View {
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

// ─── 공통 헬퍼 ───

private func choiceLetter(_ index: Int) -> String {
    let letters = ["A", "B", "C", "D", "E", "F", "G", "H"]

    return letters[min(index, letters.count - 1)]
}

private func questionTypeLabel(_ type: QuestionType?) -> String {
    if type == QuestionType.multipleChoice {
        return "객관식"
    } else if type == QuestionType.ox {
        return "OX"
    } else if type == QuestionType.essay {
        return "서술형"
    } else {
        return "문항"
    }
}

private func resultTitle(_ uiState: PlayUiState) -> String {
    if let result = uiState.myAnswerResult {
        if result.isProvisional {
            return "+\(Int(result.earnedScore))점 (잠정)"
        } else {
            return "+\(Int(result.earnedScore))점"
        }
    } else if uiState.reveal != nil && !uiState.hasSubmitted {
        return "시간 종료!"
    } else {
        return "곧 문제가 시작돼요!"
    }
}

private func resultCaption(_ uiState: PlayUiState) -> String? {
    if let result = uiState.myAnswerResult {
        if result.isProvisional {
            return "서술형은 AI 분석·선생님 첨삭 후 확정돼요"
        } else if result.correct?.boolValue == true {
            return "기본 +\(Int(result.baseScore)) · 속도 보너스 +\(Int(result.speedBonus))"
        } else if result.correct?.boolValue == false {
            return "아쉬워요, 다음 문항에서 만회해요"
        } else {
            return nil
        }
    } else if uiState.reveal != nil && !uiState.hasSubmitted {
        return "미제출로 처리됐어요"
    } else {
        return nil
    }
}

private func finalSummaryText(_ uiState: PlayUiState) -> String {
    let rankPart = uiState.rank.map { "\($0)위 · " } ?? ""

    return "\(rankPart)\(formatScore(uiState.totalScore))점 · 정답 \(uiState.myCorrectCount)/\(uiState.questionCount)"
}

private func chipColor(_ index: Int) -> Color {
    let mod = index % 4

    if mod == 0 {
        return PassmateColors.chipOrange
    } else if mod == 1 {
        return PassmateColors.chipBlue
    } else if mod == 2 {
        return PassmateColors.chipGold
    } else {
        return PassmateColors.chipGreen
    }
}

private func chipTextColor(_ index: Int) -> Color {
    let mod = index % 4

    if mod == 0 {
        return PassmateColors.chipOrangeText
    } else if mod == 1 {
        return PassmateColors.chipBlueText
    } else if mod == 2 {
        return PassmateColors.chipGoldText
    } else {
        return PassmateColors.chipGreenText
    }
}

private func rankColor(_ rank: Int) -> Color {
    if rank == 1 {
        return PassmateColors.chipGold
    } else if rank == 2 {
        return PassmateColors.chipBlue
    } else if rank == 3 {
        return PassmateColors.chipOrange
    } else {
        return PassmateColors.fieldGray
    }
}

private func rankTextColor(_ rank: Int) -> Color {
    if rank == 1 {
        return PassmateColors.chipGoldText
    } else if rank == 2 {
        return PassmateColors.chipBlueText
    } else if rank == 3 {
        return PassmateColors.chipOrangeText
    } else {
        return PassmateColors.textSecondary
    }
}

private func formatScore(_ total: Double) -> String {
    let digits = String(Int(total))

    return String(
        digits.reversed().enumerated().map { index, char -> String in
            index > 0 && index % 3 == 0 ? ",\(char)" : String(char)
        }
        .joined()
        .reversed()
    )
}
