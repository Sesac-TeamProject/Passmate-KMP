package org.sesacteamproject.passmate.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.LevelEmblem
import org.sesacteamproject.passmate.component.ReputationBadge
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.payment.domain.model.PublicRoom
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.user.domain.model.BadgeType
import org.sesacteamproject.passmate.user.domain.model.HostProfile
import org.sesacteamproject.passmate.user.domain.model.ReportReason

// Figma "UI 디자인 v6" M-10(349:9851) — 선생님 프로필 시트: 레벨·평가·뱃지·운영 중인 방 + 신고·차단.
// 시트 표시 여부는 호스팅 화면(RoomList 등)이 소유하고, 이 컨테이너는 시트 내부 내용만 담당한다 (규칙 §11-1)
@Composable
fun HostProfileSheet(
    hostId: Long,
    onJoinRoom: (String) -> Unit,
    onRequireSignIn: () -> Unit,
    onBlocked: () -> Unit,
    onNotice: (String) -> Unit
) {
    val viewModel: HostProfileViewModel = koinScreenViewModel()
    val uiState by viewModel.uiState.collectAsState()
    var showReportDialog by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(hostId) {
        viewModel.onAction(HostProfileAction.Enter(hostId))
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is HostProfileEvent.RequireSignIn -> onRequireSignIn()
                is HostProfileEvent.JoinRoom -> onJoinRoom(event.pin)
                is HostProfileEvent.BlockedAndClose -> onBlocked()
                is HostProfileEvent.ShowNotice -> onNotice(event.message)
            }
        }
    }
    HostProfileContentView(
        uiState = uiState,
        onAction = viewModel::onAction,
        onRetry = { viewModel.onAction(HostProfileAction.Retry(hostId)) },
        onClickReport = { showReportDialog = true },
        onClickBlock = { showBlockConfirm = true }
    )
    if (showReportDialog) {
        ReportReasonDialog(
            onSelect = { reason ->
                showReportDialog = false
                viewModel.onAction(HostProfileAction.SubmitReport(reason))
            },
            onDismiss = { showReportDialog = false }
        )
    }
    if (showBlockConfirm) {
        BlockConfirmDialog(
            nickname = uiState.profile?.nickname.orEmpty(),
            onConfirm = {
                showBlockConfirm = false
                viewModel.onAction(HostProfileAction.ClickBlock)
            },
            onDismiss = { showBlockConfirm = false }
        )
    }
}

@Composable
private fun HostProfileContentView(
    uiState: HostProfileUiState,
    onAction: (HostProfileAction) -> Unit,
    onRetry: () -> Unit,
    onClickReport: () -> Unit,
    onClickBlock: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.Surface)
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
    ) {
        val profile = uiState.profile

        when {
            uiState.isLoading -> LoadingBox()
            uiState.loadFailed || profile == null -> ErrorBox(onRetry = onRetry)
            else -> LoadedProfile(
                profile = profile,
                isReported = uiState.isReported,
                onClickRoom = { onAction(HostProfileAction.ClickRoom(it)) },
                onClickReport = onClickReport,
                onClickBlock = onClickBlock
            )
        }
    }
}

@Composable
private fun LoadedProfile(
    profile: HostProfile,
    isReported: Boolean,
    onClickRoom: (Long) -> Unit,
    onClickReport: () -> Unit,
    onClickBlock: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${profile.nickname} 선생님",
                    color = PassmateColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.44).sp
                )
                profile.level?.let { level ->
                    ReputationBadge(level = level)
                }
                profile.intro?.let { intro ->
                    Text(
                        text = intro,
                        color = PassmateColors.TextSecondary,
                        fontSize = 13.sp,
                        letterSpacing = (-0.26).sp
                    )
                }
            }
            profile.level?.let { level ->
                LevelEmblem(
                    level = level,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        StatsRow(profile = profile)
        if (profile.badges.isNotEmpty()) {
            BadgeChipsSection(badges = profile.badges)
        }
        if (profile.rooms.isNotEmpty()) {
            Text(
                text = "운영 중인 방",
                color = PassmateColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.32).sp
            )
            profile.rooms.forEach { room ->
                HostRoomRow(
                    room = room,
                    onClickJoin = { onClickRoom(room.roomId) }
                )
            }
        }
        if (isReported) {
            Text(
                text = "신고가 접수됐어요",
                color = PassmateColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.26).sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "프로필 신고",
                    color = PassmateColors.TextTertiary,
                    fontSize = 13.sp,
                    letterSpacing = (-0.26).sp,
                    modifier = Modifier
                        .clickable(onClick = onClickReport)
                        .padding(4.dp)
                )
                Text(
                    text = "·",
                    color = PassmateColors.TextTertiary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "차단",
                    color = PassmateColors.TextTertiary,
                    fontSize = 13.sp,
                    letterSpacing = (-0.26).sp,
                    modifier = Modifier
                        .clickable(onClick = onClickBlock)
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun StatsRow(profile: HostProfile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.FieldGray, RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp)
    ) {
        StatCell(
            value = profile.avgStars?.let { formatRating(it) } ?: "-",
            label = "평균 평가",
            modifier = Modifier.weight(1f)
        )
        StatCell(
            value = "${profile.roomCount}회",
            label = "방 운영",
            modifier = Modifier.weight(1f)
        )
        StatCell(
            value = "${profile.totalStudents}명",
            label = "누적 학생",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            color = PassmateColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.34).sp
        )
        Text(
            text = label,
            color = PassmateColors.TextSecondary,
            fontSize = 12.sp,
            letterSpacing = (-0.24).sp
        )
    }
}

@Composable
private fun BadgeChipsSection(badges: List<BadgeType>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "획득한 뱃지",
                color = PassmateColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.32).sp
            )
            Text(
                text = "${badges.size}개",
                color = PassmateColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.26).sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            badges.forEach { badge ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, PassmateColors.Primary, RoundedCornerShape(12.dp))
                        .background(PassmateColors.BackgroundMint, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeGlyph(badge),
                        color = PassmateColors.PrimaryDeep,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HostRoomRow(
    room: PublicRoom,
    onClickJoin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = room.title,
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp,
                modifier = Modifier.weight(1f)
            )
            if (room.isPaid) {
                Text(
                    text = "유료 ${room.entryFee ?: 0} C",
                    color = PassmateColors.ChipGoldText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(PassmateColors.ChipGold, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = participantsText(room),
                color = PassmateColors.TextTertiary,
                fontSize = 12.sp,
                letterSpacing = (-0.24).sp,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .background(PassmateColors.Primary, RoundedCornerShape(10.dp))
                    .clickable(onClick = onClickJoin)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "참여하기",
                    color = PassmateColors.Surface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.26).sp
                )
            }
        }
    }
}

@Composable
private fun ReportReasonDialog(
    onSelect: (ReportReason) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "프로필 신고",
                color = PassmateColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                ReportReason.entries.forEach { reason ->
                    Text(
                        text = reason.label,
                        color = PassmateColors.TextPrimary,
                        fontSize = 15.sp,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(reason) }
                            .padding(vertical = 10.dp)
                    )
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "취소", color = PassmateColors.TextSecondary)
            }
        },
        containerColor = PassmateColors.Surface
    )
}

@Composable
private fun BlockConfirmDialog(
    nickname: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "선생님 차단",
                color = PassmateColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "$nickname 선생님을 차단하면 이 선생님의 방이 인기 방·탐색 목록에서 보이지 않아요. 이미 참여 중인 방은 유지돼요.",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                letterSpacing = (-0.28).sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "차단", color = PassmateColors.WeakTopicText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "취소", color = PassmateColors.TextSecondary)
            }
        },
        containerColor = PassmateColors.Surface
    )
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PassmateColors.Primary)
    }
}

@Composable
private fun ErrorBox(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "프로필을 불러오지 못했어요",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
        Text(
            text = "다시 시도",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            modifier = Modifier
                .padding(top = 10.dp)
                .clickable(onClick = onRetry)
                .padding(6.dp)
        )
    }
}

private fun badgeGlyph(type: BadgeType): String {
    return when (type) {
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

private fun participantsText(room: PublicRoom): String {
    val current = room.participantCount ?: 0
    val max = room.maxParticipants

    return if (max != null) {
        "참여 $current / $max 명"
    } else {
        "참여 $current 명"
    }
}

private fun formatRating(value: Double): String {
    val rounded = (value * 10).toInt()

    return "${rounded / 10}.${rounded % 10}"
}
