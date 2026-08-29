package org.sesacteamproject.passmate.ui.hostroom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.room.domain.model.HostedRoom
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.user.domain.model.MyGrade

// Figma "UI 디자인 v6" M-13(384:5121) — 내가 만든 방: 명성 카드+진행 중/종료 목록+새 방 만들기 FAB.
// 진행 리모컨(M-T2)·방 리포트(M-14) 연결은 후속 태스크(T118·T119)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostedRoomsScreen(onNavigate: (NavigationAction) -> Unit) {
    val viewModel: HostedRoomsViewModel = koinScreenViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val createSheetState = rememberModalBottomSheetState()
    var isCreateSheetVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onAction(HostedRoomsAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is HostedRoomsEvent.RequireSignIn -> onNavigate(NavigationAction.NavigateToSignIn)
                is HostedRoomsEvent.OpenCreateSheet -> isCreateSheetVisible = true
                is HostedRoomsEvent.OpenReputation -> onNavigate(NavigationAction.NavigateToReputation)
                is HostedRoomsEvent.OpenRoomReport -> onNavigate(NavigationAction.NavigateToRoomReport(event.roomId))
                is HostedRoomsEvent.OpenSessionControl -> onNavigate(
                    NavigationAction.NavigateToSessionControl(event.roomId, event.pin)
                )
                is HostedRoomsEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        HostedRoomsContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onClickBack = { onNavigate(NavigationAction.NavigateBack) }
        )
        CreateFab(
            onClick = { viewModel.onAction(HostedRoomsAction.ClickCreate) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    if (isCreateSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { isCreateSheetVisible = false },
            sheetState = createSheetState,
            containerColor = PassmateColors.Surface
        ) {
            CreateRoomSheet(
                onCreated = { pin ->
                    isCreateSheetVisible = false
                    viewModel.onAction(HostedRoomsAction.RoomCreated(pin))
                },
                onNotice = { message ->
                    viewModel.onAction(HostedRoomsAction.Notice(message))
                },
                onClose = { isCreateSheetVisible = false }
            )
        }
    }
}

@Composable
private fun HostedRoomsContentScreen(
    uiState: HostedRoomsUiState,
    onAction: (HostedRoomsAction) -> Unit,
    onClickBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
    ) {
        when {
            uiState.isLoading -> LoadingBox()
            uiState.loadFailed -> ErrorBox(onRetry = { onAction(HostedRoomsAction.Retry) })
            else -> LoadedHostedRooms(
                uiState = uiState,
                onAction = onAction,
                onClickBack = onClickBack
            )
        }
    }
}

@Composable
private fun LoadedHostedRooms(
    uiState: HostedRoomsUiState,
    onAction: (HostedRoomsAction) -> Unit,
    onClickBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 60.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "내가 만든 방",
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
            GradeSummaryCard(
                grade = grade,
                onClick = { onAction(HostedRoomsAction.ClickReputation) }
            )
        }
        if (uiState.ongoing.isEmpty() && uiState.ended.isEmpty()) {
            EmptyRooms()
        }
        if (uiState.ongoing.isNotEmpty()) {
            RoomSection(
                chipLabel = "진행 중",
                isOngoing = true,
                rooms = uiState.ongoing,
                onClickRoom = { room -> onAction(HostedRoomsAction.ClickOngoingRoom(room.roomId, room.pin)) }
            )
        }
        if (uiState.ended.isNotEmpty()) {
            RoomSection(
                chipLabel = "종료",
                isOngoing = false,
                rooms = uiState.ended,
                onClickRoom = { room -> onAction(HostedRoomsAction.ClickEndedRoom(room.roomId)) }
            )
        }
        if (uiState.nextCursor != null) {
            LoadMoreRow(
                isLoadingMore = uiState.isLoadingMore,
                onClick = { onAction(HostedRoomsAction.LoadMore) }
            )
        }
    }
}

@Composable
private fun GradeSummaryCard(
    grade: MyGrade,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LevelEmblem(
            level = grade.level,
            modifier = Modifier.size(36.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Lv.${grade.level.level} ${grade.level.label}",
                color = PassmateColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.32).sp
            )
            Text(
                text = gradeStatsLine(grade),
                color = PassmateColors.TextSecondary,
                fontSize = 13.sp,
                letterSpacing = (-0.26).sp
            )
        }
        Text(
            text = "명성 상세 ›",
            color = PassmateColors.PrimaryDeep,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.26).sp
        )
    }
}

@Composable
private fun RoomSection(
    chipLabel: String,
    isOngoing: Boolean,
    rooms: List<HostedRoom>,
    onClickRoom: (HostedRoom) -> Unit
) {
    val chipBg = if (isOngoing) PassmateColors.Primary else PassmateColors.FieldGray
    val chipFg = if (isOngoing) PassmateColors.Surface else PassmateColors.TextSecondary
    val sectionBg = if (isOngoing) PassmateColors.BackgroundMint else PassmateColors.Surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(sectionBg, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chipLabel,
                color = chipFg,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.24).sp,
                modifier = Modifier
                    .background(chipBg, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
            Text(
                text = "${rooms.size}개",
                color = PassmateColors.TextSecondary,
                fontSize = 13.sp,
                letterSpacing = (-0.26).sp
            )
        }
        rooms.forEachIndexed { index, room ->
            if (index > 0) {
                Divider(color = PassmateColors.Border, thickness = 1.dp)
            }
            HostedRoomRow(
                room = room,
                isOngoing = isOngoing,
                onClick = { onClickRoom(room) }
            )
        }
    }
}

@Composable
private fun HostedRoomRow(
    room: HostedRoom,
    isOngoing: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = room.title,
                color = PassmateColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = roomSubtitle(room, isOngoing),
                color = PassmateColors.TextSecondary,
                fontSize = 13.sp,
                letterSpacing = (-0.26).sp
            )
        }
        Text(
            text = if (isOngoing) "진행 ›" else "상세 ›",
            color = PassmateColors.PrimaryDeep,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.26).sp
        )
    }
}

@Composable
private fun CreateFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .background(PassmateColors.Primary, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+",
            color = PassmateColors.Surface,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "아직 만든 방이 없어요",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
        Text(
            text = "+ 버튼으로 첫 방을 만들어 보세요",
            color = PassmateColors.TextTertiary,
            fontSize = 13.sp,
            letterSpacing = (-0.26).sp
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
            text = "방 목록을 불러오지 못했어요",
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

private fun gradeStatsLine(grade: MyGrade): String {
    val stats = grade.stats
    val parts = mutableListOf("방 운영 ${stats.roomCount}회")

    stats.avgStars?.let { parts.add("별점 ${formatStars(it)}") }
    parts.add("학생 ${stats.totalStudents}명")

    return parts.joinToString(" · ")
}

private fun roomSubtitle(room: HostedRoom, isOngoing: Boolean): String {
    val parts = mutableListOf<String>()

    if (isOngoing) {
        room.participantCount?.let { parts.add("학생 ${it}명") }
        parts.add("PIN ${formatPin(room.pin)}")
        room.scheduledAt?.let { parts.add("${formatTime(it)} 시작") }
    } else {
        room.endedAtLabel?.let { parts.add(it) }
        room.participantCount?.let { parts.add("학생 ${it}명") }
        room.avgAccuracyPercent?.let { parts.add("평균 ${it}%") }
    }
    return parts.joinToString(" · ")
}

private fun formatPin(pin: String): String {
    return pin.chunked(3).joinToString(" ")
}

// ISO 문자열에서 HH:mm만 취한다 — 시간 표시는 서버가 준 예정 시각 렌더링 전용
private fun formatTime(isoTime: String): String {
    val timePart = isoTime.substringAfter('T', "")

    return if (timePart.length >= 5) {
        timePart.take(5)
    } else {
        isoTime
    }
}

private fun formatStars(value: Double): String {
    val rounded = (value * 10).toInt()

    return "${rounded / 10}.${rounded % 10}"
}
