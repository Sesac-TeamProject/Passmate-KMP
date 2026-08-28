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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.theme.PassmateColors

// 공개 방 목록·탐색 (M-11) — 검색 + 유형 필터 + 인기 방 카드. 방 선택 시 입장 화면으로 이동한다.
@Composable
fun RoomListScreen(onNavigate: (NavigationAction) -> Unit) {
    val viewModel: RoomListViewModel = koinScreenViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is RoomListEvent.OpenRoom -> onNavigate(NavigationAction.NavigateToJoin(event.pin))
                is RoomListEvent.OpenPinEntry -> onNavigate(NavigationAction.NavigateToJoin())
                is RoomListEvent.ShowNotice -> {}
            }
        }
    }
    RoomListContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
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
private fun RoomList(uiState: RoomListUiState, onClickRoom: (String) -> Unit, onLoadMore: () -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(uiState.rooms, key = { it.roomId }) { room ->
            RoomCard(room = room, onClick = { onClickRoom(room.pin) })
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
private fun RoomCard(room: PublicRoom, onClick: () -> Unit) {
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
