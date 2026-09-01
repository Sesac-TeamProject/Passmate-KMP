package org.sesacteamproject.passmate.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.AppTab
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.user.domain.model.JoinedRoom
import org.sesacteamproject.passmate.user.domain.model.MyPageSummary
import org.sesacteamproject.passmate.user.domain.model.OngoingRoom

// Figma "UI 디자인 v6" M-08(349:9544) — 참여한 방 탭 루트: 진행 중 방·누적 요약·보완 주제·참여 목록(→리포트)
@Composable
fun JoinedRoomsScreen(
    viewModel: JoinedRoomsViewModel = koinScreenViewModel(),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onAction(JoinedRoomsAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is JoinedRoomsEvent.RequireSignIn -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToTab(AppTab.JOINED_ROOMS))
                )
                is JoinedRoomsEvent.OpenReport -> onNavigate(NavigationAction.NavigateToResult(event.roomId))
                is JoinedRoomsEvent.Rejoin -> onNavigate(NavigationAction.NavigateToWaiting(event.pin))
                is JoinedRoomsEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        JoinedRoomsContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun JoinedRoomsContentScreen(
    uiState: JoinedRoomsUiState,
    onAction: (JoinedRoomsAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
    ) {
        when {
            uiState.isLoading -> LoadingBox()
            uiState.loadFailed -> ErrorBox(onRetry = { onAction(JoinedRoomsAction.Retry) })
            else -> LoadedJoinedRooms(
                uiState = uiState,
                onAction = onAction
            )
        }
    }
}

@Composable
private fun ColumnScope.LoadedJoinedRooms(
    uiState: JoinedRoomsUiState,
    onAction: (JoinedRoomsAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 60.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "참여한 방",
            color = PassmateColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.48).sp
        )
        val ongoing = uiState.ongoing
        val summary = uiState.summary

        if (ongoing != null) {
            OngoingCard(
                ongoing = ongoing,
                onClickRejoin = { onAction(JoinedRoomsAction.ClickRejoin(ongoing.pin)) }
            )
        }
        if (summary != null) {
            SummaryCard(summary = summary)
            WeakTopicsRow(topics = summary.weakTopics)
        }
        if (uiState.rooms.isEmpty() && ongoing == null) {
            EmptyRooms()
        } else {
            uiState.rooms.forEach { room ->
                JoinedRoomRow(
                    room = room,
                    onClickReport = { onAction(JoinedRoomsAction.ClickRoomReport(room.roomId)) }
                )
            }
        }
        if (uiState.nextCursor != null) {
            LoadMoreRow(
                isLoadingMore = uiState.isLoadingMore,
                onClick = { onAction(JoinedRoomsAction.LoadMore) }
            )
        }
    }
}

@Composable
private fun OngoingCard(
    ongoing: OngoingRoom,
    onClickRejoin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Primary, RoundedCornerShape(16.dp))
            .background(PassmateColors.BackgroundMint, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(PassmateColors.Primary, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "진행 중",
                    color = PassmateColors.Surface,
                    fontSize = 12.sp,
                    letterSpacing = (-0.24).sp
                )
            }
            Text(
                text = ongoing.title,
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
        Text(
            text = ongoingSubtitle(ongoing),
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            letterSpacing = (-0.28).sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(PassmateColors.Primary, RoundedCornerShape(12.dp))
                .clickable(onClick = onClickRejoin),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "다시 들어가기",
                color = PassmateColors.Surface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
    }
}

@Composable
private fun SummaryCard(summary: MyPageSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(20.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .border(6.dp, PassmateColors.Primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${summary.accuracyPercent}%",
                    color = PassmateColors.PrimaryDeep,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.32).sp
                )
                Text(
                    text = "평균",
                    color = PassmateColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.28).sp
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = summaryLine(summary),
                color = PassmateColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.32).sp
            )
            val trendText = summary.trendText

            if (trendText != null) {
                Text(
                    text = trendText,
                    color = PassmateColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.28).sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeakTopicsRow(topics: List<String>) {
    if (topics.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "보완할 주제",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp,
                modifier = Modifier.padding(vertical = 6.dp)
            )
            topics.forEach { topic ->
                Box(
                    modifier = Modifier
                        .background(PassmateColors.WeakTopicBg, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = topic,
                        color = PassmateColors.WeakTopicText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.28).sp
                    )
                }
            }
        }
    }
}

@Composable
private fun JoinedRoomRow(
    room: JoinedRoom,
    onClickReport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RankBadge(rank = room.myRank)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = room.title,
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
            Text(
                text = "${room.dateLabel} · ${room.questionCount}문항",
                color = PassmateColors.TextSecondary,
                fontSize = 12.sp,
                letterSpacing = (-0.24).sp
            )
        }
        val myScore = room.myScore

        if (myScore != null) {
            Text(
                text = "${formatScore(myScore)}점",
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
        if (room.hasReport) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .background(PassmateColors.FieldGray, CircleShape)
                    .clickable(onClick = onClickReport)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "리포트",
                    color = PassmateColors.PrimaryDeep,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.28).sp
                )
            }
        }
    }
}

@Composable
private fun RankBadge(rank: Int?) {
    val (background, textColor) = rankStyle(rank)

    Box(
        modifier = Modifier
            .size(26.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rank?.toString() ?: "-",
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun LoadMoreRow(
    isLoadingMore: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(14.dp))
            .clickable(enabled = !isLoadingMore, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoadingMore) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = PassmateColors.Primary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "더 보기",
                color = PassmateColors.PrimaryDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
    }
}

@Composable
private fun EmptyRooms() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "아직 참여한 방이 없어요",
            color = PassmateColors.TextSecondary,
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
            text = "기록을 불러오지 못했어요",
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

private fun ongoingSubtitle(ongoing: OngoingRoom): String {
    val parts = mutableListOf<String>()

    ongoing.progressLabel?.let { parts.add(it) }
    ongoing.hostNickname?.let { parts.add("$it 선생님") }
    parts.add("PIN ${formatPin(ongoing.pin)}")

    return parts.joinToString(" · ")
}

private fun summaryLine(summary: MyPageSummary): String {
    val rankPart = summary.avgRank?.let { " · 평균 ${formatRank(it)}위" } ?: ""

    return "${summary.participationCount}회 참여$rankPart"
}

private fun rankStyle(rank: Int?): Pair<Color, Color> {
    return when (rank) {
        1 -> PassmateColors.ChipGold to PassmateColors.ChipGoldText
        2 -> PassmateColors.ChipBlue to PassmateColors.ChipBlueText
        3 -> PassmateColors.ChipOrange to PassmateColors.ChipOrangeText
        else -> PassmateColors.FieldGray to PassmateColors.TextSecondary
    }
}

private fun formatPin(pin: String): String {
    return pin.chunked(3).joinToString(" ")
}

private fun formatRank(rank: Double): String {
    val rounded = (rank * 10).toLong()

    return if (rounded % 10 == 0L) {
        (rounded / 10).toString()
    } else {
        "${rounded / 10}.${rounded % 10}"
    }
}

private fun formatScore(score: Double): String {
    val digits = score.toLong().toString()

    return digits.reversed().chunked(3).joinToString(",").reversed()
}
