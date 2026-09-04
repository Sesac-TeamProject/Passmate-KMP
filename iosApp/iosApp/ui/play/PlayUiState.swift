import Shared

struct PlayUiState {
    var isLoading: Bool = true

    var phase: Phase = .idle

    var questionCount: Int = 0

    var question: SessionQuestion?

    var selectedChoiceIndex: Int?

    var essayAnswer: String = ""

    var remainingSeconds: Int = 0

    var isSubmitting: Bool = false

    var hasSubmitted: Bool = false

    var myAnswerResult: AnswerResult?

    var reveal: Reveal?

    var totalScore: Double = 0

    var myCorrectCount: Int = 0

    var rank: Int?

    var ranking: [RankEntry] = []

    var finalRanking: [RankEntry] = []

    var myParticipantId: Int64?

    var myNickname: String?

    var isGuest: Bool = false

    var isLocked: Bool = false

    var activeVoiceHint: VoiceHint?

    // 화면 단계 — 전환은 전부 서버 이벤트·스냅샷으로만 일어난다 (규칙 §2-1-2)
    enum Phase {
        case idle
        case question
        case finished
    }

    // QUESTION_ENDED 정답 공개 페이로드 (정답은 이 이벤트에서만 온다 — 규칙 §13)
    struct Reveal {
        let answer: String?

        let explanation: String?

        let correctAnswererCount: Int

        // M-04 응답 분포 — 보기 순서대로 이미 맞춰 둔 목록이다.
        // 짝짓기는 shared의 SessionQuestion.distributionOf가 한다 (Compose·SwiftUI 공용)
        let distribution: [ChoiceDistribution]
    }
}
