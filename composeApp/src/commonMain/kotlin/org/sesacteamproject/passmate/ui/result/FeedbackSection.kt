package org.sesacteamproject.passmate.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.report.domain.model.AiFeedback
import org.sesacteamproject.passmate.report.domain.model.AiFeedbackStatus
import org.sesacteamproject.passmate.report.domain.model.HostReview
import org.sesacteamproject.passmate.report.domain.model.QuestionResult
import org.sesacteamproject.passmate.theme.PassmateColors

// T056(US4) 서술형 AI 분석 피드백 뷰 — Figma "UI 디자인 v6" M-06(349:9433) 카드.
// 분석 중/완료/실패/한도 소진(SKIPPED) 상태를 표시하고, 정오·점수 확인은 막지 않는다 (규칙 §10)
@Composable
fun FeedbackSection(
    question: QuestionResult,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(20.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(20.dp))
    ) {
        FeedbackHeader(questionNo = question.questionNo)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            FeedbackBody(feedback = question.aiFeedback)
            HostReviewRow(review = question.hostReview)
        }
    }
}

@Composable
private fun FeedbackHeader(questionNo: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.Primary, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        Text(
            text = "Q$questionNo · AI 분석 (참고 의견)",
            color = PassmateColors.Surface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun FeedbackBody(feedback: AiFeedback?) {
    when (feedback?.status) {
        AiFeedbackStatus.DONE -> DoneFeedback(feedback)
        AiFeedbackStatus.PENDING -> StatusMessage("AI가 답변을 분석하고 있어요…")
        AiFeedbackStatus.FAILED -> StatusMessage("분석을 완료하지 못했어요 — 정오·점수는 위에서 확인할 수 있어요")
        AiFeedbackStatus.SKIPPED -> StatusMessage("이번 세션은 무료 분석 한도를 초과해 AI 분석이 제공되지 않았어요")
        else -> StatusMessage("이 문항에는 AI 분석이 없어요")
    }
}

@Composable
private fun DoneFeedback(feedback: AiFeedback) {
    if (feedback.coveredConcepts.isNotEmpty()) {
        FeedbackPoint(
            dotColor = PassmateColors.Primary,
            text = "핵심 포함 — ${feedback.coveredConcepts.joinToString(", ")}"
        )
    }
    val shortage = shortageText(feedback)

    if (shortage != null) {
        FeedbackPoint(
            dotColor = PassmateColors.WeakTopicText,
            text = "부족 — $shortage"
        )
    }
    if (!feedback.improvement.isNullOrBlank()) {
        FeedbackPoint(
            dotColor = PassmateColors.TextPrimary,
            text = "제안 — ${feedback.improvement}"
        )
    }
}

private fun shortageText(feedback: AiFeedback): String? {
    return when {
        feedback.missingConcepts.isNotEmpty() -> feedback.missingConcepts.joinToString(", ")
        !feedback.weaknesses.isNullOrBlank() -> feedback.weaknesses
        else -> null
    }
}

@Composable
private fun FeedbackPoint(
    dotColor: Color,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, CircleShape)
        )
        Text(
            text = text,
            color = PassmateColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun StatusMessage(message: String) {
    Text(
        text = message,
        color = PassmateColors.TextSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.28).sp
    )
}

// 첨삭 입력은 파트2 T072 — 여기선 도착 시 표시만, 미도착이면 안내 문구 (M-06 하단)
@Composable
private fun HostReviewRow(review: HostReview?) {
    if (review == null) {
        Text(
            text = "선생님 코멘트가 도착하면 여기에 표시돼요",
            color = PassmateColors.TextSecondary,
            fontSize = 12.sp,
            letterSpacing = (-0.24).sp
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "선생님 코멘트",
                color = PassmateColors.PrimaryDeep,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.24).sp
            )
            Text(
                text = review.comment,
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
            if (!review.improvement.isNullOrBlank()) {
                Text(
                    text = "개선 — ${review.improvement}",
                    color = PassmateColors.TextSecondary,
                    fontSize = 13.sp,
                    letterSpacing = (-0.26).sp
                )
            }
        }
    }
}
