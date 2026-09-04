package org.sesacteamproject.passmate.session.domain.model

// 진행 중 문항 — 정답은 절대 포함하지 않는다(정답은 QUESTION_ENDED에서만, 규칙 §13)
data class SessionQuestion(
    val questionId: Long,
    val questionNo: Int,
    val type: QuestionType,
    val body: String,
    val choices: List<String>,
    val points: Int,
    val timeLimitSec: Int,
    val endsAt: String,
    val isClosed: Boolean
) {
    // 서버는 OX 문항에 choices를 주지 않는다(실측: type=OX, choices=null, answer="O").
    // 표시와 제출이 같은 목록을 봐야 눌린 보기와 보낸 값이 어긋나지 않는다 —
    // 화면에 "O"/"X"를 따로 적어두면 제출은 빈 choices를 보고 null이 되어 답을 낼 수 없다.
    val answerChoices: List<String>
        get() = if (type == QuestionType.OX && choices.isEmpty()) OX_CHOICES else choices

    companion object {

        val OX_CHOICES = listOf("O", "X")
    }
}
