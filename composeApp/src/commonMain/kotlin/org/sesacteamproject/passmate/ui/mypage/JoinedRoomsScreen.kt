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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateEmptyState
import org.sesacteamproject.passmate.component.PassmateIcon
import org.sesacteamproject.passmate.component.PassmateIcons
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.AppTab
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.component.PassmateSkeletonBlock
import org.sesacteamproject.passmate.component.PassmateSkeletonCard
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme
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
                // 홈 탭이 곧 PIN 입장 폼 (규칙 §2-1-1)
                is JoinedRoomsEvent.OpenPinEntry -> onNavigate(NavigationAction.NavigateToHome)
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
            // 화면 배경은 상태바 뒤까지 깔고 콘텐츠만 내린다 (iOS의 background(...).ignoresSafeArea() 미러)
            .statusBarsPadding()
    ) {
        when {
            uiState.isLoading -> JoinedRoomsSkeleton()
            uiState.loadFailed -> LoadFailureBox(
                onRetry = { onAction(JoinedRoomsAction.Retry) },
                onClickContactSupport = { onAction(JoinedRoomsAction.ClickContactSupport) }
            )
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
            EmptyRooms(onClickEnterPin = { onAction(JoinedRoomsAction.ClickEnterPin) })
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

// 빈 상태 문구 (v6 M-08) — iOS JoinedRoomsView.swift의 EmptyStateText와 1:1.
// 치수·타이포는 공통 컴포넌트 PassmateEmptyState가 갖는다
private object EmptyStateText {

    const val TITLE = "아직 참여한 방이 없어요"

    const val GUIDE = "선생님에게 받은 PIN 6자리를\n홈에서 입력해 보세요."

    const val CTA = "PIN으로 입장"
}

// 빈 상태 (v6 M-08) — 문구·아이콘만 넘기고 배치는 공통 컴포넌트가 그린다
@Composable
private fun EmptyRooms(onClickEnterPin: () -> Unit) {
    PassmateEmptyState(
        icon = PassmateIcons.DoorOpen,
        iconTint = PassmateColors.TextSecondary,
        title = EmptyStateText.TITLE,
        guide = EmptyStateText.GUIDE,
        ctaLabel = EmptyStateText.CTA,
        onClickCta = onClickEnterPin
    )
}

// 시안 "M-08 참여한 방 — 스켈레톤" — 로드된 화면(LoadedJoinedRooms)과 같은 자리에 블록을 놓는다.
// 탭 루트라 첫 진입 체감 속도를 가장 크게 좌우한다 (시안 07 로딩 규격).
@Composable
private fun JoinedRoomsSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 60.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PassmateSkeletonBlock(modifier = Modifier.size(width = 120.dp, height = 22.dp), cornerRadius = 8.dp)
        PassmateSkeletonCard(cornerRadius = 24.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PassmateSkeletonBlock(modifier = Modifier.size(width = 170.dp, height = 18.dp))
                PassmateSkeletonBlock(
                    modifier = Modifier.size(width = 240.dp, height = 14.dp),
                    color = PassmateColors.SkeletonBlockSoft
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonTopicChip(width = 84.dp)
            SkeletonTopicChip(width = 84.dp)
            SkeletonTopicChip(width = 84.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonRoomRow(titleWidth = 190.dp)
            SkeletonRoomRow(titleWidth = 150.dp)
            SkeletonRoomRow(titleWidth = 170.dp)
            // 마지막 줄은 짧게 (시안 스켈레톤 규격)
            SkeletonRoomRow(titleWidth = 110.dp)
        }
    }
}

@Composable
private fun SkeletonTopicChip(width: Dp) {
    PassmateSkeletonBlock(
        modifier = Modifier.size(width = width, height = 30.dp),
        cornerRadius = 15.dp,
        color = PassmateColors.SkeletonBlockSoft
    )
}

@Composable
private fun SkeletonRoomRow(titleWidth: Dp) {
    PassmateSkeletonCard(cornerRadius = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PassmateSkeletonBlock(modifier = Modifier.size(38.dp), cornerRadius = 19.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PassmateSkeletonBlock(modifier = Modifier.size(width = titleWidth, height = 14.dp))
                PassmateSkeletonBlock(
                    modifier = Modifier.size(width = 96.dp, height = 12.dp),
                    color = PassmateColors.SkeletonBlockSoft
                )
            }
        }
    }
}

// 실패 아이콘 크기 (시안 icon/alert-circle 30x30) — iOS FailureIconSize와 1:1
private val FailureIconSize = 30.dp

// 목록 불러오기 실패 — v6 "E-List 목록 불러오기 실패 — 공통 패턴"(코인 내역·정산·마이와 동일 레이아웃, 공통화 대상)
@Composable
private fun LoadFailureBox(
    onRetry: () -> Unit,
    onClickContactSupport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, top = 60.dp, end = 20.dp, bottom = 24.dp)
    ) {
        Text(
            text = "참여한 방",
            color = PassmateColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.15).sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(PassmateColors.ErrorIconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PassmateIcon(
                    icon = PassmateIcons.AlertCircle,
                    contentDescription = null,
                    modifier = Modifier.size(FailureIconSize),
                    tint = PassmateColors.WrongPinkText
                )
            }
            Text(
                text = "목록을 불러오지 못했어요",
                color = PassmateColors.TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.19).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = "연결이 잠시 끊겼어요.\n다시 시도해 주세요.",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 23.sp,
                letterSpacing = (-0.14).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 9.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(PassmateColors.Primary, RoundedCornerShape(14.dp))
                .clickable(onClick = onRetry),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "다시 시도",
                color = PassmateColors.Surface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.15).sp
            )
        }
        Text(
            text = "계속 안 되면 문의하기",
            color = PassmateColors.PrimaryDeep,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.13).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .clickable(onClick = onClickContactSupport)
                .padding(vertical = 8.dp)
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

// --- Preview ---

@PassmatePreview
@Composable
private fun JoinedRoomsContentScreenPreview() {
    PassmateTheme {
        JoinedRoomsContentScreen(
            uiState = JoinedRoomsUiState(
                isLoading = false,
                summary = MyPageSummary(
                    participationCount = 12,
                    accuracyPercent = 78,
                    avgRank = 2.4,
                    trendText = "지난주보다 +5%",
                    weakTopics = listOf("이차함수", "확률과 통계")
                ),
                ongoing = OngoingRoom(
                    roomId = 501,
                    pin = "482913",
                    title = "8월 4주차 Spring 스터디",
                    hostNickname = "김선생",
                    progressLabel = "5 / 8 문항 진행 중"
                ),
                rooms = listOf(
                    JoinedRoom(roomId = 401, title = "7월 3주차 미적분 특강", dateLabel = "2026.07.18", questionCount = 10, myScore = 890.0, myRank = 2, hasReport = true),
                    JoinedRoom(roomId = 402, title = "확률과 통계 총정리", dateLabel = "2026.07.10", questionCount = 8, myScore = 720.0, myRank = 5, hasReport = true),
                    JoinedRoom(roomId = 403, title = "함수의 극한 퀴즈", dateLabel = "2026.06.28", questionCount = 6, myScore = null, myRank = null, hasReport = false)
                )
            ),
            onAction = {}
        )
    }
}

// 참여한 방 없음 — 빈 상태
@PassmatePreview
@Composable
private fun JoinedRoomsContentScreenEmptyPreview() {
    PassmateTheme {
        JoinedRoomsContentScreen(
            uiState = JoinedRoomsUiState(isLoading = false, rooms = emptyList()),
            onAction = {}
        )
    }
}

@PassmatePreview
@Composable
private fun JoinedRoomsContentScreenFailedPreview() {
    PassmateTheme {
        JoinedRoomsContentScreen(
            uiState = JoinedRoomsUiState(isLoading = false, loadFailed = true),
            onAction = {}
        )
    }
}

// M-08 참여한 방 — 스켈레톤
@PassmatePreview
@Composable
private fun JoinedRoomsSkeletonPreview() {
    PassmateTheme {
        JoinedRoomsSkeleton()
    }
}
