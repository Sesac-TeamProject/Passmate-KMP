package org.sesacteamproject.passmate.ui.payment

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateCard
import org.sesacteamproject.passmate.component.PortOnePaymentView
import org.sesacteamproject.passmate.component.StudentAvatar
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.room.domain.model.RoomHost
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// 유료 방 입장 결제 (M-01 v2 / W-11). 방 정보 + 보유 코인/참가비 + 닉네임·캐릭터 + 결제 CTA.
// 포트원 결제창(웹뷰) 오버레이는 이 컨테이너가 소유한다 (규칙 §11-1).
@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel = koinScreenViewModel(),
    pin: String,
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(pin) {
        viewModel.onAction(PaymentAction.Start(pin))
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is PaymentEvent.EnterRoom -> onNavigate(NavigationAction.NavigateToWaiting(event.pin))
                is PaymentEvent.SignInRequired -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToPayment(pin))
                )
                is PaymentEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        PaymentContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onBack = { onNavigate(NavigationAction.NavigateBack) }
        )
        uiState.checkout?.let { request ->
            Box(modifier = Modifier.fillMaxSize().background(PassmateColors.Surface)) {
                PortOnePaymentView(
                    request = request,
                    onResult = { viewModel.onAction(PaymentAction.ReceivePortOneResult(it)) }
                )
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun PaymentContentScreen(
    uiState: PaymentUiState,
    onAction: (PaymentAction) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.BackgroundMint)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "‹ 뒤로",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onBack() }.padding(vertical = 4.dp)
            )
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text("유료 방 입장", color = PassmateColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        when {
            uiState.isLoading -> CenterProgress()
            uiState.hasLoadError -> RetryState(onRetry = { onAction(PaymentAction.Retry) })
            else -> LoadedPayment(uiState = uiState, onAction = onAction)
        }
    }
}

@Composable
private fun LoadedPayment(uiState: PaymentUiState, onAction: (PaymentAction) -> Unit) {
    Column {
        RoomSummaryCard(uiState)
        Spacer(Modifier.height(12.dp))
        CoinCard(uiState)
        Spacer(Modifier.height(12.dp))
        NicknameField(
            nickname = uiState.nickname,
            onChange = { onAction(PaymentAction.ChangeNickname(it)) }
        )
        Spacer(Modifier.height(12.dp))
        AvatarPicker(
            selected = uiState.avatarId,
            onSelect = { onAction(PaymentAction.SelectAvatar(it)) }
        )
        if (!uiState.hasEnough) {
            Spacer(Modifier.height(12.dp))
            MethodPicker(
                selected = uiState.selectedMethod,
                onSelect = { onAction(PaymentAction.SelectMethod(it)) }
            )
        }
        uiState.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = PassmateColors.WrongPinkText, fontSize = 13.sp)
        }
        Spacer(Modifier.height(20.dp))
        PayButton(uiState = uiState, onClick = { onAction(PaymentAction.ClickPay) })
    }
}

@Composable
private fun RoomSummaryCard(uiState: PaymentUiState) {
    PassmateCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(uiState.room?.title.orEmpty(), color = PassmateColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            uiState.room?.topic?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = PassmateColors.TextSecondary, fontSize = 13.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("참가비", color = PassmateColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("${uiState.entryFee} C", color = PassmateColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CoinCard(uiState: PaymentUiState) {
    PassmateCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("보유 코인", color = PassmateColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("${uiState.balance} C", color = PassmateColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            if (!uiState.hasEnough) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("부족 코인", color = PassmateColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("${uiState.shortfall} C", color = PassmateColors.WrongPinkText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NicknameField(nickname: String, onChange: (String) -> Unit) {
    Column {
        Text("이 방에서 쓸 닉네임", color = PassmateColors.TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        TextField(
            value = nickname,
            onValueChange = onChange,
            singleLine = true,
            placeholder = { Text("닉네임", color = PassmateColors.TextTertiary, fontSize = 14.sp) },
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = PassmateColors.TextPrimary),
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
}

@Composable
private fun AvatarPicker(selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Text("캐릭터", color = PassmateColors.TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items((1..12).toList()) { id ->
                val isSelected = id == selected
                val border = if (isSelected) PassmateColors.Primary else PassmateColors.Border

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(border, CircleShape)
                        .clickable { onSelect(id) }
                        .padding(2.dp)
                        .background(PassmateColors.Surface, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StudentAvatar(avatarId = id, modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

@Composable
private fun MethodPicker(selected: PaymentMethod, onSelect: (PaymentMethod) -> Unit) {
    Column {
        Text("충전 결제 수단", color = PassmateColors.TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PaymentMethod.entries.toList()) { method ->
                val isSelected = method == selected
                val bg = if (isSelected) PassmateColors.Primary else PassmateColors.Surface
                val fg = if (isSelected) PassmateColors.Surface else PassmateColors.TextSecondary

                Text(
                    text = method.label,
                    color = fg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(bg, CircleShape)
                        .clickable { onSelect(method) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PayButton(uiState: PaymentUiState, onClick: () -> Unit) {
    val label = if (uiState.hasEnough) {
        "${uiState.entryFee} C 결제하고 입장"
    } else {
        "${uiState.shortfall} C 충전하고 입장"
    }
    val enabled = !uiState.isProcessing
    val bg = if (enabled) PassmateColors.Primary else PassmateColors.Border

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isProcessing) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = PassmateColors.Surface)
        } else {
            Text(label, color = PassmateColors.Surface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CenterProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PassmateColors.Primary)
    }
}

@Composable
private fun RetryState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("방 정보를 불러오지 못했어요", color = PassmateColors.TextSecondary, fontSize = 14.sp)
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

private val previewPaidRoom = RoomInfo(
    roomId = 601,
    pin = "731204",
    title = "8월 4주차 Spring 스터디",
    topic = "이차방정식 심화",
    status = RoomStatus.WAITING,
    questionCount = 8,
    estimatedMinutes = 20,
    scheduledAt = null,
    participantCount = 12,
    maxParticipants = 30,
    isPaid = true,
    entryFee = 500,
    host = RoomHost(userId = 11, nickname = "김선생", level = 3, avgStars = 4.8, ratingCount = 32)
)

// 보유 코인이 참가비에 모자란 상태 — 충전 후 결제 유도
@PassmatePreview
@Composable
private fun PaymentContentScreenShortfallPreview() {
    PassmateTheme {
        PaymentContentScreen(
            uiState = PaymentUiState(
                isLoading = false,
                room = previewPaidRoom,
                balance = 200,
                shortfall = 300,
                nickname = "민지",
                avatarId = 3,
                selectedMethod = PaymentMethod.KAKAO_PAY
            ),
            onAction = {},
            onBack = {}
        )
    }
}

// 결제 요청 in-flight — 버튼 비활성 (규칙 §9)
@PassmatePreview
@Composable
private fun PaymentContentScreenProcessingPreview() {
    PassmateTheme {
        PaymentContentScreen(
            uiState = PaymentUiState(
                isLoading = false,
                room = previewPaidRoom,
                balance = 800,
                shortfall = 0,
                nickname = "민지",
                avatarId = 3,
                isProcessing = true
            ),
            onAction = {},
            onBack = {}
        )
    }
}

@PassmatePreview
@Composable
private fun PaymentContentScreenErrorPreview() {
    PassmateTheme {
        PaymentContentScreen(
            uiState = PaymentUiState(isLoading = false, hasLoadError = true),
            onAction = {},
            onBack = {}
        )
    }
}
