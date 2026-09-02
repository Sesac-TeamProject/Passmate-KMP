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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.LevelEmblem
import org.sesacteamproject.passmate.component.PassmateBackButton
import org.sesacteamproject.passmate.component.ReputationBadge
import org.sesacteamproject.passmate.component.StudentAvatar
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme
import org.sesacteamproject.passmate.user.domain.model.Badge
import org.sesacteamproject.passmate.user.domain.model.BadgeType
import org.sesacteamproject.passmate.user.domain.model.GradeCriterion
import org.sesacteamproject.passmate.user.domain.model.GradeStats
import org.sesacteamproject.passmate.user.domain.model.MyGrade
import org.sesacteamproject.passmate.user.domain.model.NextGrade
import org.sesacteamproject.passmate.user.domain.model.UserProfile

// 한 줄에 놓는 뱃지 수 (시안 M-09 뱃지 컬렉션 그리드)
private const val BADGES_PER_ROW = 4

// Figma "UI 디자인 v6" M-09(349:9770) — 명성 · 뱃지 상세: 프로필+등급 카드(승급 진행도·조건)+뱃지 컬렉션
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
        ReputationHeader(onClickBack = onClickBack)
        when {
            uiState.isLoading -> LoadingBox()
            uiState.loadFailed -> ErrorBox(onRetry = { onAction(ReputationAction.Retry) })
            else -> LoadedReputation(uiState = uiState)
        }
    }
}

@Composable
private fun ReputationHeader(onClickBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 52.dp, end = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PassmateBackButton(onClick = onClickBack)
        Text(
            text = "명성 · 뱃지",
            color = PassmateColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.48).sp
        )
    }
}

@Composable
private fun LoadedReputation(uiState: ReputationUiState) {
    val grade = uiState.grade
    val profile = uiState.profile

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (profile != null) {
            ProfileCard(
                profile = profile,
                stats = grade?.stats,
                level = grade?.level ?: profile.level
            )
        }
        if (grade != null) {
            GradeCard(grade = grade)
        }
        BadgeSection(badges = uiState.badges)
        if (grade != null && grade.level.level < HostLevel.VERIFIED.level) {
            PaidRoomLockedCta()
        }
    }
}

@Composable
private fun ProfileCard(
    profile: UserProfile,
    stats: GradeStats?,
    level: HostLevel?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(20.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudentAvatar(
            avatarId = profile.avatarId,
            modifier = Modifier.size(56.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = profile.nickname,
                    color = PassmateColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.32).sp
                )
                if (level != null) {
                    ReputationBadge(level = level)
                }
            }
            if (stats != null) {
                Text(
                    text = statsLine(stats),
                    color = PassmateColors.TextSecondary,
                    fontSize = 12.sp,
                    letterSpacing = (-0.24).sp
                )
            }
        }
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
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.32).sp
                )
                Text(
                    text = nextLevelLine(grade),
                    color = PassmateColors.TextSecondary,
                    fontSize = 12.sp,
                    letterSpacing = (-0.24).sp
                )
            }
            if (next != null) {
                Text(
                    text = "${next.progressPercent}%",
                    color = PassmateColors.PrimaryDeep,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.28).sp
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
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun ProgressBar(percent: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(PassmateColors.FieldGray, CircleShape)
    ) {
        val fraction = percent.coerceIn(0, 100) / 100f

        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
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
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LevelEmblem(
            level = HostLevel.VERIFIED,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = note,
            color = PassmateColors.ReputationBadgeText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
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
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        } else {
            Text(
                text = "${formatNumber(criterion.current)} / ${formatNumber(criterion.target)}",
                color = PassmateColors.WeakTopicText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.32).sp
            )
            Text(
                text = "${badges.count { it.earned }} / ${badges.size}",
                color = PassmateColors.PrimaryDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PassmateColors.Border, RoundedCornerShape(20.dp))
                .background(PassmateColors.Surface, RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            badges.chunked(BADGES_PER_ROW).forEach { rowBadges ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    rowBadges.forEach { badge ->
                        BadgeCell(
                            badge = badge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(BADGES_PER_ROW - rowBadges.size) {
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
    val labelColor = if (badge.earned) PassmateColors.TextPrimary else PassmateColors.TextTertiary
    val tileAlpha = if (badge.earned) 1f else 0.3f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .alpha(tileAlpha)
                .border(1.5.dp, PassmateColors.AchievementBadgeBorder, RoundedCornerShape(13.dp))
                .background(PassmateColors.BackgroundMint, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeGlyph(badge),
                color = PassmateColors.PrimaryDeep,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = badge.type.label,
            color = labelColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            textAlign = TextAlign.Center
        )
    }
}

// 시안의 잠긴 CTA — Lv.3 미만에서만 노출한다. 방 개설 진입은 '내가 만든 방' 탭이 담당하므로 안내 전용이다
@Composable
private fun PaidRoomLockedCta() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(PassmateColors.FieldGray, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🔒 유료 방 만들기 — Lv.3부터",
            color = PassmateColors.TextTertiary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
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

// 시안 "참여 18회 · 평균 정답률 72% · 방 운영 12회" — 서버가 준 집계만 이어 붙인다
private fun statsLine(stats: GradeStats): String {
    val parts = mutableListOf("참여 ${stats.participationCount}회")

    stats.avgAccuracyPercent?.let { parts.add("평균 정답률 ${it}%") }
    parts.add("방 운영 ${stats.roomCount}회")

    return parts.joinToString(" · ")
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

private fun formatNumber(value: Double): String {
    val rounded = (value * 10).toLong()

    return if (rounded % 10 == 0L) {
        (rounded / 10).toString()
    } else {
        "${rounded / 10}.${rounded % 10}"
    }
}

// --- Preview ---

@PassmatePreview
@Composable
private fun ReputationContentScreenPreview() {
    PassmateTheme {
        ReputationContentScreen(
            uiState = ReputationUiState(
                isLoading = false,
                profile = UserProfile(
                    nickname = "준영",
                    email = "junyoung@example.com",
                    joinedAt = "2026.06.01",
                    avatarId = 6,
                    level = HostLevel.GROWING,
                    coins = 1200,
                    joinedRoomCount = 18,
                    hostedRoomCount = 12
                ),
                grade = MyGrade(
                    level = HostLevel.GROWING,
                    achievedAt = "2026.08.12",
                    stats = GradeStats(
                        participationCount = 18,
                        avgAccuracyPercent = 72,
                        roomCount = 12,
                        totalStudents = 96,
                        avgStars = 4.7,
                        ratingCount = 32
                    ),
                    next = NextGrade(
                        level = HostLevel.VERIFIED,
                        progressPercent = 60,
                        criteria = listOf(
                            GradeCriterion(label = "방 운영 20회 이상", current = 12.0, target = 20.0, met = false),
                            GradeCriterion(label = "평균 별점 4.0 이상", current = 4.7, target = 4.0, met = true),
                            GradeCriterion(label = "총 학생 150명 이상", current = 96.0, target = 150.0, met = false)
                        )
                    )
                ),
                badges = listOf(
                    Badge(type = BadgeType.FIRST_ROOM, earned = true, earnedAt = "2026.06.02", progressCurrent = null, progressTarget = null),
                    Badge(type = BadgeType.ROOMS_10, earned = false, earnedAt = null, progressCurrent = 8, progressTarget = 10),
                    Badge(type = BadgeType.STUDENTS_100, earned = false, earnedAt = null, progressCurrent = 96, progressTarget = 100),
                    Badge(type = BadgeType.RATING_45, earned = true, earnedAt = "2026.08.25", progressCurrent = null, progressTarget = null),
                    Badge(type = BadgeType.RATINGS_50, earned = false, earnedAt = null, progressCurrent = 32, progressTarget = 50),
                    Badge(type = BadgeType.STREAK_30, earned = true, earnedAt = "2026.08.30", progressCurrent = null, progressTarget = null),
                    Badge(type = BadgeType.FIRST_PAID_ROOM, earned = false, earnedAt = null, progressCurrent = 0, progressTarget = 1),
                    Badge(type = BadgeType.AI_SETS_50, earned = false, earnedAt = null, progressCurrent = 8, progressTarget = 50)
                )
            ),
            onAction = {},
            onClickBack = {}
        )
    }
}

@PassmatePreview
@Composable
private fun ReputationContentScreenFailedPreview() {
    PassmateTheme {
        ReputationContentScreen(
            uiState = ReputationUiState(isLoading = false, loadFailed = true),
            onAction = {},
            onClickBack = {}
        )
    }
}
