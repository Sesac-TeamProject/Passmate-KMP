package org.sesacteamproject.passmate.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.LevelEmblem
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.user.domain.model.Badge
import org.sesacteamproject.passmate.user.domain.model.BadgeType
import org.sesacteamproject.passmate.user.domain.model.GradeCriterion
import org.sesacteamproject.passmate.user.domain.model.MyGrade

// Figma "UI 디자인 v6" M-09(349:9770) — 내 명성·뱃지 상세: 등급 카드(승급 진행도·조건)+뱃지 컬렉션
@Composable
fun ReputationScreen(
    viewModel: ReputationViewModel = koinScreenViewModel(),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onAction(ReputationAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is ReputationEvent.RequireSignIn -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToReputation)
                )
            }
        }
    }
    ReputationContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onClickBack = { onNavigate(NavigationAction.NavigateBack) }
    )
}

@Composable
private fun ReputationContentScreen(
    uiState: ReputationUiState,
    onAction: (ReputationAction) -> Unit,
    onClickBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
    ) {
        when {
            uiState.isLoading -> LoadingBox()
            uiState.loadFailed -> ErrorBox(onRetry = { onAction(ReputationAction.Retry) })
            else -> LoadedReputation(
                uiState = uiState,
                onClickBack = onClickBack
            )
        }
    }
}

@Composable
private fun LoadedReputation(
    uiState: ReputationUiState,
    onClickBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 60.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "내 명성",
                color = PassmateColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.48).sp
            )
            Text(
                text = "닫기",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp,
                modifier = Modifier
                    .clickable(onClick = onClickBack)
                    .padding(4.dp)
            )
        }
        val grade = uiState.grade

        if (grade != null) {
            GradeCard(grade = grade)
        }
        BadgeSection(badges = uiState.badges)
    }
}

@Composable
private fun GradeCard(grade: MyGrade) {
    val next = grade.next

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(20.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LevelEmblem(
                level = grade.level,
                modifier = Modifier.size(48.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Lv.${grade.level.level} ${grade.level.label}",
                    color = PassmateColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.36).sp
                )
                Text(
                    text = nextLevelLine(grade),
                    color = PassmateColors.TextSecondary,
                    fontSize = 13.sp,
                    letterSpacing = (-0.26).sp
                )
            }
            if (next != null) {
                Text(
                    text = "${next.progressPercent}%",
                    color = PassmateColors.PrimaryDeep,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.36).sp
                )
            }
        }
        if (next != null) {
            ProgressBar(percent = next.progressPercent)
            unlockNote(next.level)?.let { note ->
                UnlockNoteBox(note = note)
            }
            next.criteria.forEach { criterion ->
                CriterionRow(criterion = criterion)
            }
        }
        Text(
            text = "Lv.3 달성 후 하락 없음 · Lv.4~5만 30일 활동 유지 조건",
            color = PassmateColors.TextTertiary,
            fontSize = 12.sp,
            letterSpacing = (-0.24).sp
        )
    }
}

@Composable
private fun ProgressBar(percent: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(PassmateColors.FieldGray, CircleShape)
    ) {
        val fraction = percent.coerceIn(0, 100) / 100f

        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .background(PassmateColors.Primary, CircleShape)
            )
        }
    }
}

@Composable
private fun UnlockNoteBox(note: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.BackgroundMint, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LevelEmblem(
            level = HostLevel.VERIFIED,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = note,
            color = PassmateColors.PrimaryDeep,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.26).sp
        )
    }
}

@Composable
private fun CriterionRow(criterion: GradeCriterion) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = criterion.label,
            color = PassmateColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
        if (criterion.met) {
            Text(
                text = "✓ ${formatNumber(criterion.current)}",
                color = PassmateColors.PrimaryDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.28).sp
            )
        } else {
            Text(
                text = "${formatNumber(criterion.current)} / ${formatNumber(criterion.target)}",
                color = PassmateColors.WeakTopicText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.28).sp
            )
        }
    }
}

@Composable
private fun BadgeSection(badges: List<Badge>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "내 뱃지",
                color = PassmateColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.36).sp
            )
            Text(
                text = "${badges.count { it.earned }} / ${badges.size}",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PassmateColors.BackgroundMint, RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            badges.chunked(4).forEach { rowBadges ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowBadges.forEach { badge ->
                        BadgeCell(
                            badge = badge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(4 - rowBadges.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeCell(
    badge: Badge,
    modifier: Modifier = Modifier
) {
    val iconBg = if (badge.earned) PassmateColors.Surface else PassmateColors.FieldGray
    val iconBorder = if (badge.earned) PassmateColors.Primary else PassmateColors.Border
    val iconColor = if (badge.earned) PassmateColors.PrimaryDeep else PassmateColors.TextTertiary
    val labelColor = if (badge.earned) PassmateColors.TextPrimary else PassmateColors.TextTertiary

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .border(1.dp, iconBorder, RoundedCornerShape(14.dp))
                .background(iconBg, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeGlyph(badge),
                color = iconColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = badgeLabel(badge),
            color = labelColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.24).sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PassmateColors.Primary)
    }
}

@Composable
private fun ErrorBox(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "명성 정보를 불러오지 못했어요",
            color = PassmateColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.32).sp
        )
        Text(
            text = "다시 시도",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            modifier = Modifier
                .padding(top = 12.dp)
                .clickable(onClick = onRetry)
                .padding(8.dp)
        )
    }
}

private fun nextLevelLine(grade: MyGrade): String {
    val next = grade.next

    return if (next != null) {
        "다음 레벨 Lv.${next.level.level} ${next.level.label}까지 ${next.progressPercent}%"
    } else {
        "최고 등급이에요"
    }
}

// 등급별 해금 안내 (기획서 §13.3 수익 모델) — Lv.3부터 유료 방 개설·8:2 정산
private fun unlockNote(nextLevel: HostLevel): String? {
    return if (nextLevel == HostLevel.VERIFIED) {
        "Lv.3이 되면 유료 방을 열고 참가비의 80%를 정산받아요"
    } else {
        null
    }
}

private fun badgeGlyph(badge: Badge): String {
    return when (badge.type) {
        BadgeType.FIRST_ROOM -> "⚑"
        BadgeType.ROOMS_10 -> "10"
        BadgeType.STUDENTS_100 -> "100"
        BadgeType.RATING_45 -> "★"
        BadgeType.RATINGS_50 -> "50"
        BadgeType.STREAK_30 -> "30"
        BadgeType.FIRST_PAID_ROOM -> "₩"
        BadgeType.AI_SETS_50 -> "AI"
    }
}

private fun badgeLabel(badge: Badge): String {
    val progressCurrent = badge.progressCurrent
    val progressTarget = badge.progressTarget

    return if (!badge.earned && progressCurrent != null && progressTarget != null) {
        "${badge.type.label} ${progressCurrent}/${progressTarget}"
    } else {
        badge.type.label
    }
}

private fun formatNumber(value: Double): String {
    val rounded = (value * 10).toLong()

    return if (rounded % 10 == 0L) {
        (rounded / 10).toString()
    } else {
        "${rounded / 10}.${rounded % 10}"
    }
}
