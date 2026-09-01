package org.sesacteamproject.passmate.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateCard
import org.sesacteamproject.passmate.component.ReputationBadge
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.payment.domain.model.PublicRoom
import org.sesacteamproject.passmate.payment.domain.model.RoomTypeFilter
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme
import org.sesacteamproject.passmate.ui.profile.HostProfileSheet

// 공개 방 목록·탐색 (M-11) — 검색 + 유형 필터 + 인기 방 카드. 방 선택 시 입장 화면으로 이동한다.
// 선생님 이름 탭 시 프로필 시트(M-10)를 연다 — 시트 표시 여부는 이 화면이 소유한다 (규칙 §11-1)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(onNavigate: (NavigationAction) -> Unit) {
    val viewModel: RoomListViewModel = koinScreenViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val profileSheetState = rememberModalBottomSheetState()
    var profileHostId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is RoomListEvent.OpenRoom -> onNavigate(NavigationAction.NavigateToJoin(event.pin))
                is RoomListEvent.OpenPinEntry -> onNavigate(NavigationAction.NavigateToJoin())
                is RoomListEvent.OpenHostProfile -> profileHostId = event.hostId
                is RoomListEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        RoomListContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    val hostId = profileHostId

    if (hostId != null) {
        ModalBottomSheet(
            onDismissRequest = { profileHostId = null },
            sheetState = profileSheetState,
            containerColor = PassmateColors.Surface
        ) {
            HostProfileSheet(
                hostId = hostId,
                onJoinRoom = { pin ->
                    profileHostId = null
                    onNavigate(NavigationAction.NavigateToJoin(pin))
                },
                onRequireSignIn = {
                    profileHostId = null
                    onNavigate(NavigationAction.NavigateToSignIn(NavigationAction.NavigateToRoomList))
                },
                onBlocked = {
                    profileHostId = null
                    // 차단 호스트의 방은 공개 목록에서 숨겨진다 — 목록 새로고침 (M-10)
                    viewModel.onAction(RoomListAction.Retry)
                },
                onNotice = { message ->
                    viewModel.onAction(RoomListAction.Notice(message))
                }
            )
        }
    }
}

@Composable
private fun RoomListContentScreen(
    uiState: RoomListUiState,
    onAction: (RoomListAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.BackgroundMint)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "방 찾기",
                color = PassmateColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "PIN 입장",
                color = PassmateColors.Surface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(PassmateColors.Primary, RoundedCornerShape(12.dp))
                    .clickable { onAction(RoomListAction.ClickPinEntry) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        SearchField(
            query = uiState.query,
            onChange = { onAction(RoomListAction.ChangeQuery(it)) },
            onSubmit = { onAction(RoomListAction.SubmitSearch) }
        )
        Spacer(Modifier.height(12.dp))
        TypeFilterRow(
            selected = uiState.typeFilter,
            onSelect = { onAction(RoomListAction.SelectType(it)) }
        )
        Spacer(Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> CenterProgress()
                uiState.hasError -> RetryState(onRetry = { onAction(RoomListAction.Retry) })
                uiState.isEmpty -> EmptyState()
                else -> RoomList(
                    uiState = uiState,
                    onClickRoom = { onAction(RoomListAction.ClickRoom(it)) },
                    onClickHost = { onAction(RoomListAction.ClickHost(it)) },
                    onLoadMore = { onAction(RoomListAction.LoadMore) }
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, onSubmit: () -> Unit) {
    TextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        placeholder = {
            Text("방 이름·선생님·주제 검색", color = PassmateColors.TextTertiary, fontSize = 14.sp)
        },
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = PassmateColors.TextPrimary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = PassmateColors.Surface,
            unfocusedContainerColor = PassmateColors.Surface,
            focusedIndicatorColor = PassmateColors.Primary,
            unfocusedIndicatorColor = PassmateColors.Border
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TypeFilterRow(selected: RoomTypeFilter, onSelect: (RoomTypeFilter) -> Unit) {
    val options = listOf(
        RoomTypeFilter.ALL to "전체",
        RoomTypeFilter.FREE to "무료",
        RoomTypeFilter.PAID to "유료"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (type, label) ->
            val isSelected = type == selected
            val bg = if (isSelected) PassmateColors.Primary else PassmateColors.Surface
            val fg = if (isSelected) PassmateColors.Surface else PassmateColors.TextSecondary

            Text(
                text = label,
                color = fg,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(bg, CircleShape)
                    .clickable { onSelect(type) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun RoomList(
    uiState: RoomListUiState,
    onClickRoom: (String) -> Unit,
    onClickHost: (Long) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(uiState.rooms, key = { it.roomId }) { room ->
            RoomCard(
                room = room,
                onClick = { onClickRoom(room.pin) },
                onClickHost = onClickHost
            )
        }
        if (uiState.hasNext) {
            item(key = "load-more") {
                LaunchedEffect(uiState.rooms.size) { onLoadMore() }
                Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = PassmateColors.Primary)
                }
            }
        }
        item(key = "bottom-space") { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun RoomCard(
    room: PublicRoom,
    onClick: () -> Unit,
    onClickHost: (Long) -> Unit
) {
    PassmateCard(modifier = Modifier.clickable { onClick() }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = room.title,
                    color = PassmateColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                FeeBadge(isPaid = room.isPaid, entryFee = room.entryFee)
            }
            room.topic?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = PassmateColors.TextSecondary, fontSize = 13.sp)
            }
            Spacer(Modifier.height(10.dp))
            val hostId = room.hostId
            // 선생님 이름을 누르면 레벨·별점·뱃지 프로필 시트 (M-10, M-11 노트)
            val hostRowModifier = if (hostId != null) {
                Modifier.clickable { onClickHost(hostId) }
            } else {
                Modifier
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = hostRowModifier
            ) {
                Text(room.hostName, color = PassmateColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                HostLevel.from(room.hostLevel)?.let { level ->
                    ReputationBadge(level = level)
                }
                room.hostRating?.let {
                    Text("★ ${formatRating(it)}", color = PassmateColors.StarGold, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = participantsText(room),
                color = PassmateColors.TextTertiary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun FeeBadge(isPaid: Boolean, entryFee: Int?) {
    val label = if (isPaid) {
        "유료 ${entryFee ?: 0} C"
    } else {
        "무료"
    }
    val bg = if (isPaid) PassmateColors.ChipGold else PassmateColors.ChipGreen
    val fg = if (isPaid) PassmateColors.ChipGoldText else PassmateColors.ChipGreenText

    Text(
        text = label,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(bg, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
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

@Composable
private fun CenterProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PassmateColors.Primary)
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("조건에 맞는 방이 없어요", color = PassmateColors.TextTertiary, fontSize = 14.sp)
    }
}

@Composable
private fun RetryState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("목록을 불러오지 못했어요", color = PassmateColors.TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "다시 시도",
            color = PassmateColors.Surface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .background(PassmateColors.Primary, RoundedCornerShape(12.dp))
                .clickable { onRetry() }
                .padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
}

// --- Preview ---

private val previewPublicRooms = listOf(
    PublicRoom(
        roomId = 701,
        pin = "482913",
        title = "8월 4주차 Spring 스터디",
        topic = "이차함수 심화",
        hostId = 11,
        hostName = "김선생",
        hostLevel = 3,
        hostRating = 4.8,
        status = RoomStatus.WAITING,
        participantCount = 12,
        maxParticipants = 30,
        isPaid = true,
        entryFee = 500,
        scheduledAt = null
    ),
    PublicRoom(
        roomId = 702,
        pin = "115820",
        title = "확률과 통계 총정리",
        topic = "조건부확률",
        hostId = 11,
        hostName = "이선생",
        hostLevel = 2,
        hostRating = 4.5,
        status = RoomStatus.WAITING,
        participantCount = 8,
        maxParticipants = null,
        isPaid = false,
        entryFee = null,
        scheduledAt = null
    ),
    PublicRoom(
        roomId = 703,
        pin = "930447",
        title = "함수의 극한 무료 특강",
        topic = null,
        hostId = 11,
        hostName = "박선생",
        hostLevel = null,
        hostRating = null,
        status = RoomStatus.RUNNING,
        participantCount = 20,
        maxParticipants = 20,
        isPaid = false,
        entryFee = null,
        scheduledAt = null
    )
)

@PassmatePreview
@Composable
private fun RoomListContentScreenPreview() {
    PassmateTheme {
        RoomListContentScreen(
            uiState = RoomListUiState(isLoading = false, rooms = previewPublicRooms),
            onAction = {}
        )
    }
}

// 검색 결과 없음 — 빈 상태 (규칙 §11)
@PassmatePreview
@Composable
private fun RoomListContentScreenEmptyPreview() {
    PassmateTheme {
        RoomListContentScreen(
            uiState = RoomListUiState(isLoading = false, query = "미적분", rooms = emptyList()),
            onAction = {}
        )
    }
}

@PassmatePreview
@Composable
private fun RoomListContentScreenErrorPreview() {
    PassmateTheme {
        RoomListContentScreen(
            uiState = RoomListUiState(isLoading = false, hasError = true),
            onAction = {}
        )
    }
}
