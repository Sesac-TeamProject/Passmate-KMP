package org.sesacteamproject.passmate.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import org.sesacteamproject.passmate.component.ReputationBadge
import org.sesacteamproject.passmate.component.StudentAvatar
import org.sesacteamproject.passmate.report.domain.model.SessionResult
import org.sesacteamproject.passmate.user.domain.model.HostProfile
import org.sesacteamproject.passmate.component.StarRating
import org.sesacteamproject.passmate.rating.domain.model.RatingTag
import org.sesacteamproject.passmate.theme.PassmateColors

// T080(US11) 세션 평가 시트 — Figma "UI 디자인 v6" M-06 v2(349:9492).
// 별점(1~5)+태그 다중+한 줄 후기, 제출 후 수정 불가·스킵 무불이익 (FR-042~043)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatingSection(
    uiState: ResultUiState,
    onAction: (ResultAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 시안은 시트 상단에서 제목까지 62dp를 둔다(손잡이 20 + 여백). 핸들 높이를 뺀 값
            .padding(start = 20.dp, top = 34.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "이번 세션 어땠나요?",
            color = PassmateColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        )
        Text(
            text = "문제를 제출한 학생만 평가할 수 있어요 · 세션 종료 후 24시간 · 1회",
            color = PassmateColors.TextSecondary,
            fontSize = 12.sp,
            letterSpacing = (-0.24).sp
        )
        uiState.result?.let { result ->
            SessionInfoCard(result = result, host = uiState.host)
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StarRating(
                stars = uiState.ratingStars,
                onSelect = { onAction(ResultAction.SelectRatingStars(it)) }
            )
            Text(
                text = starLabel(uiState.ratingStars),
                color = PassmateColors.PrimaryDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RatingTag.all.forEach { tag ->
                RatingTagChip(
                    tag = tag,
                    isSelected = tag in uiState.ratingTags,
                    onClick = { onAction(ResultAction.ToggleRatingTag(tag)) }
                )
            }
        }
        CommentField(
            comment = uiState.ratingComment,
            onChange = { onAction(ResultAction.ChangeRatingComment(it)) }
        )
        SubmitRatingButton(
            isSubmitting = uiState.isSubmittingRating,
            enabled = uiState.ratingStars > 0,
            onClick = { onAction(ResultAction.SubmitRating) }
        )
        Text(
            text = "건너뛰기",
            color = PassmateColors.TextTertiary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            // 시안 M-06 v2 — 건너뛰기는 좌측 정렬이다
            modifier = Modifier
                .align(Alignment.Start)
                .clickable { onAction(ResultAction.SkipRating) }
                .padding(4.dp)
        )
    }
}

@Composable
private fun RatingTagChip(
    tag: RatingTag,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val background = if (isSelected) PassmateColors.RatingTagSelectedBg else PassmateColors.Surface
    val borderColor = if (isSelected) PassmateColors.Primary else PassmateColors.Border
    val textColor = if (isSelected) PassmateColors.RatingTagSelectedText else PassmateColors.TextSecondary

    Box(
        modifier = Modifier
            .border(1.dp, borderColor, CircleShape)
            .background(background, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = tag.label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun CommentField(
    comment: String,
    onChange: (String) -> Unit
) {
    BasicTextField(
        value = comment,
        onValueChange = onChange,
        textStyle = TextStyle(
            color = PassmateColors.TextPrimary,
            fontSize = 14.sp,
            letterSpacing = (-0.28).sp
        ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(PassmateColors.FieldGray, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (comment.isEmpty()) {
                    Text(
                        text = "한 줄 후기 (선택) — 선생님에게만 보여요",
                        color = PassmateColors.TextTertiary,
                        fontSize = 14.sp,
                        letterSpacing = (-0.28).sp
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun SubmitRatingButton(
    isSubmitting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background = if (enabled) PassmateColors.Primary else PassmateColors.TextTertiary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(background, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled && !isSubmitting, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.height(20.dp),
                color = PassmateColors.Surface,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "평가 보내기",
                color = PassmateColors.Surface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.32).sp
            )
        }
    }
}

private fun starLabel(stars: Int): String {
    return when (stars) {
        1 -> "1점 · 별로예요"
        2 -> "2점 · 아쉬워요"
        3 -> "3점 · 괜찮아요"
        4 -> "4점 · 좋았어요"
        5 -> "5점 · 최고예요"
        else -> "별점을 선택해 주세요"
    }
}

// 시안(M-06 v2)의 세션 정보 카드 — 선생님 아바타·이름·등급 배지 + 방 제목·문항 수·내 제출.
// 선생님 정보는 결과 응답에 없어 별도 조회한다(계약 갭 G-8) — 아직 안 왔으면 방 정보 줄만 그린다
@Composable
private fun SessionInfoCard(
    result: SessionResult,
    host: HostProfile?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.FieldGray, RoundedCornerShape(14.dp))
            .padding(start = 12.dp, top = 10.dp, end = 14.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (host != null) {
            StudentAvatar(
                avatarId = host.avatarId,
                modifier = Modifier.size(36.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (host != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${host.nickname} 선생님",
                        color = PassmateColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.28).sp
                    )
                    host.level?.let { ReputationBadge(level = it) }
                }
            }
            Text(
                text = "${result.roomTitle} · ${result.questionCount}문항 · " +
                    "내 제출 ${result.submitCount}/${result.questionCount}",
                color = PassmateColors.TextSecondary,
                fontSize = 12.sp,
                letterSpacing = (-0.24).sp
            )
        }
    }
}
