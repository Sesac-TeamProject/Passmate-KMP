package org.sesacteamproject.passmate.ui.payment

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateBackButton
import org.sesacteamproject.passmate.component.PassmateCard
import org.sesacteamproject.passmate.component.PassyMascot
import org.sesacteamproject.passmate.component.PortOnePaymentView
import org.sesacteamproject.passmate.component.ReputationBadge
import org.sesacteamproject.passmate.component.StudentAvatar
import org.sesacteamproject.passmate.component.StudentAvatars
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.room.domain.model.RoomHost
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme
import org.sesacteamproject.passmate.payment.domain.policy.SettlementPolicy

// 캐릭터 선택 한 줄에 놓는 수 (M-01 입장 폼과 동일)
private const val AVATARS_PER_ROW = 6

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
    // 결제 수단 드롭다운 펼침은 순수 UI 상태 — 컨테이너가 소유한다 (규칙 §11-1)
    var isMethodExpanded by remember { mutableStateOf(false) }

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
            onAction = { action ->
                if (action is PaymentAction.SelectMethod) {
                    isMethodExpanded = false
                }
                viewModel.onAction(action)
            },
            isMethodExpanded = isMethodExpanded,
            onToggleMethodExpanded = { isMethodExpanded = !isMethodExpanded },
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
    isMethodExpanded: Boolean,
    onToggleMethodExpanded: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
            // 화면 배경은 상태바 뒤까지 깔고 콘텐츠만 내린다 (iOS의 background(...).ignoresSafeArea() 미러)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        PaymentHeader(onBack = onBack)
        when {
            uiState.isLoading -> CenterProgress()
            uiState.hasLoadError -> RetryState(onRetry = { onAction(PaymentAction.Retry) })
            else -> LoadedPayment(
                uiState = uiState,
                onAction = onAction,
                isMethodExpanded = isMethodExpanded,
                onToggleMethodExpanded = onToggleMethodExpanded
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PaymentHeader(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        PassmateBackButton(
            onClick = onBack,
            modifier = Modifier.padding(start = 20.dp, top = 58.dp)
        )
        PassyMascot(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 44.dp, end = 28.dp)
                .size(width = 68.dp, height = 75.dp)
        )
        Column(
            modifier = Modifier.padding(start = 60.dp, top = 56.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "유료 방 입장",
                color = PassmateColors.PrimaryDeep,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.48).sp
            )
            Text(
                text = "유료 방이에요 — 결제 후 입장할 수 있어요",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
    }
}

@Composable
private fun LoadedPayment(
    uiState: PaymentUiState,
    onAction: (PaymentAction) -> Unit,
    isMethodExpanded: Boolean,
    onToggleMethodExpanded: () -> Unit
) {
    PassmateCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "코인 충전하고 입장하기",
                    color = PassmateColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    text = "PIN ${formatPin(uiState.room?.pin)} · 방 정보를 확인하세요",
                    color = PassmateColors.TextSecondary,
                    fontSize = 14.sp,
                    letterSpacing = (-0.28).sp
                )
            }
            uiState.room?.let { room ->
                RoomPreview(room = room)
            }
            EntryFeeSection(entryFee = uiState.entryFee)
            NicknameField(
                nickname = uiState.nickname,
                onChange = { onAction(PaymentAction.ChangeNickname(it)) }
            )
            AvatarField(
                selectedAvatarId = uiState.avatarId,
                onSelect = { onAction(PaymentAction.SelectAvatar(it)) }
            )
            if (!uiState.hasEnough) {
                MethodField(
                    balance = uiState.balance,
                    shortfall = uiState.shortfall,
                    selected = uiState.selectedMethod,
                    isExpanded = isMethodExpanded,
                    onToggleExpanded = onToggleMethodExpanded,
                    onSelect = { onAction(PaymentAction.SelectMethod(it)) }
                )
            }
            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = PassmateColors.WrongPinkText,
                    fontSize = 13.sp,
                    letterSpacing = (-0.26).sp
                )
            }
            PayButton(uiState = uiState, onClick = { onAction(PaymentAction.ClickPay) })
        }
    }
}

@Composable
private fun RoomPreview(room: RoomInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.FieldGray, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = room.title,
                color = PassmateColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.32).sp,
                modifier = Modifier.weight(1f)
            )
            PaidRoomChip()
        }
        val host = room.host
        val hostLevel = HostLevel.from(host?.level)

        if (host != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${host.nickname} 선생님",
                    color = PassmateColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.28).sp
                )
                if (hostLevel != null) {
                    ReputationBadge(level = hostLevel)
                }
            }
        }
        val meta = roomMetaLine(room)

        if (meta.isNotEmpty()) {
            Text(
                text = meta,
                color = PassmateColors.TextSecondary,
                fontSize = 12.sp,
                letterSpacing = (-0.24).sp
            )
        }
    }
}

@Composable
private fun PaidRoomChip() {
    Text(
        text = "₩ 유료",
        color = PassmateColors.WeakTopicText,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.28).sp,
        modifier = Modifier
            .background(PassmateColors.WeakTopicBg, CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun EntryFeeSection(entryFee: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "참가비",
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
            Text(
                text = "$entryFee C",
                color = PassmateColors.PrimaryDeep,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )
        }
        // 정산 비율은 SettlementPolicy가 단일 출처다. 금액 분해는 서버 권위라 비율만 안내한다 (규칙 §13)
        Text(
            text = "선생님 정산 ${SettlementPolicy.hostSharePercent}% · 플랫폼 수수료 ${SettlementPolicy.platformFeePercent}% · 세션 시작 전 취소 시 전액 환불",
            color = PassmateColors.TextSecondary,
            fontSize = 12.sp,
            letterSpacing = (-0.24).sp
        )
    }
}

@Composable
private fun NicknameField(
    nickname: String,
    onChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "닉네임",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
        BasicTextField(
            value = nickname,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                letterSpacing = (-0.28).sp
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(PassmateColors.FieldGray, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (nickname.isEmpty()) {
                        Text(
                            text = "이 방에서 쓸 이름",
                            color = PassmateColors.TextSecondary,
                            fontSize = 14.sp,
                            letterSpacing = (-0.28).sp
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

// 시안에는 없지만 입장(join)이 캐릭터를 요구해 남긴다 — 닉네임 필드와 같은 라벨 규격을 쓴다
@Composable
private fun AvatarField(
    selectedAvatarId: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "내 캐릭터",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
        StudentAvatars.ids.chunked(AVATARS_PER_ROW).forEach { rowIds ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowIds.forEach { avatarId ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AvatarPickItem(
                            avatarId = avatarId,
                            isSelected = avatarId == selectedAvatarId,
                            onClick = { onSelect(avatarId) }
                        )
                    }
                }
                repeat(AVATARS_PER_ROW - rowIds.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AvatarPickItem(
    avatarId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PassmateColors.Primary else Color.Transparent

    Box(
        modifier = Modifier
            .size(44.dp)
            .border(2.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        StudentAvatar(
            avatarId = avatarId,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun MethodField(
    balance: Int,
    shortfall: Int,
    selected: PaymentMethod,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSelect: (PaymentMethod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "보유 코인 $balance C · 부족 $shortfall C",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(PassmateColors.FieldGray, RoundedCornerShape(14.dp))
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selected.label} ▾",
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                letterSpacing = (-0.28).sp
            )
        }
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PassmateColors.FieldGray, RoundedCornerShape(14.dp))
                    .padding(vertical = 4.dp)
            ) {
                PaymentMethod.entries.forEach { method ->
                    val color = if (method == selected) {
                        PassmateColors.PrimaryDeep
                    } else {
                        PassmateColors.TextPrimary
                    }

                    Text(
                        text = method.label,
                        color = color,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.28).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(method) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PayButton(
    uiState: PaymentUiState,
    onClick: () -> Unit
) {
    val label = if (uiState.hasEnough) {
        "${uiState.entryFee} C 결제하고 입장"
    } else {
        "${uiState.shortfall} C 충전하고 입장"
    }
    val enabled = !uiState.isProcessing
    val background = if (enabled) PassmateColors.Primary else PassmateColors.Border

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(background, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isProcessing) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
                color = PassmateColors.Surface
            )
        } else {
            Text(
                text = label,
                color = PassmateColors.Surface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.32).sp
            )
        }
    }
}

@Composable
private fun CenterProgress() {
    Box(
        modifier = Modifier.fillMaxWidth().height(240.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PassmateColors.Primary)
    }
}

@Composable
private fun RetryState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().height(240.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "방 정보를 불러오지 못했어요",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            letterSpacing = (-0.28).sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "다시 시도",
            color = PassmateColors.Surface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            modifier = Modifier
                .background(PassmateColors.Primary, RoundedCornerShape(12.dp))
                .clickable { onRetry() }
                .padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
}

// 시안 "PIN 482 913" — 6자리를 3자리씩 끊어 읽기 쉽게 표시한다
private fun formatPin(pin: String?): String {
    val resolved = pin.orEmpty()

    return if (resolved.length == 6) {
        "${resolved.take(3)} ${resolved.drop(3)}"
    } else {
        resolved
    }
}

// 시안 "8문항 · 약 15분 · 20:00 시작 · 현재 12명 대기" — 서버가 준 값만 이어 붙인다
private fun roomMetaLine(room: RoomInfo): String {
    val parts = mutableListOf<String>()

    room.questionCount?.let { parts.add("${it}문항") }
    room.estimatedMinutes?.let { parts.add("약 ${it}분") }
    room.scheduledAt?.let { parts.add("${formatTime(it)} 시작") }
    room.participantCount?.let { parts.add("현재 ${it}명 대기") }

    return parts.joinToString(" · ")
}

private fun formatTime(isoTime: String): String {
    val timePart = isoTime.substringAfter('T', "")

    return if (timePart.length >= 5) {
        timePart.take(5)
    } else {
        isoTime
    }
}

// --- Preview ---

private val previewPaidRoom = RoomInfo(
    roomId = 601,
    pin = "482913",
    title = "8월 4주차 Spring 스터디",
    topic = "이차방정식 심화",
    status = RoomStatus.WAITING,
    questionCount = 8,
    estimatedMinutes = 15,
    scheduledAt = "2026-09-01T20:00:00",
    participantCount = 12,
    maxParticipants = 30,
    isPaid = true,
    entryFee = 500,
    isGuestAllowed = true,
    host = RoomHost(userId = 11, nickname = "김민지", level = 3, avgStars = 4.8, ratingCount = 32)
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
            isMethodExpanded = false,
            onToggleMethodExpanded = {},
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
            isMethodExpanded = false,
            onToggleMethodExpanded = {},
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
            isMethodExpanded = false,
            onToggleMethodExpanded = {},
            onBack = {}
        )
    }
}
