package org.sesacteamproject.passmate.ui.join

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateCard
import org.sesacteamproject.passmate.component.PassyMascot
import org.sesacteamproject.passmate.component.StudentAvatar
import org.sesacteamproject.passmate.component.StudentAvatars
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.room.domain.model.RoomHost
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.policy.JoinInputPolicy
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// Figma "UI 디자인 v6" M-01(349:9151) 기준 — PIN 6칸·QR 입장·닉네임·캐릭터 선택·입장하기
@Composable
fun JoinScreen(
    viewModel: JoinViewModel = koinScreenViewModel(),
    initialPin: String? = null,
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val qrScanLauncher = rememberQrScanLauncher { text ->
        viewModel.onAction(JoinAction.ReceiveQrResult(text))
    }
    val currentQrScanLauncher by rememberUpdatedState(qrScanLauncher)

    LaunchedEffect(initialPin) {
        if (!initialPin.isNullOrBlank()) {
            viewModel.onAction(JoinAction.ChangePin(initialPin))
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is JoinEvent.RequestQrScan -> currentQrScanLauncher?.invoke()
                is JoinEvent.JoinCompleted -> onNavigate(NavigationAction.NavigateToWaiting(event.pin))
                is JoinEvent.PaymentRequired -> onNavigate(NavigationAction.NavigateToPayment(event.pin))
                is JoinEvent.SignInRequested -> onNavigate(NavigationAction.NavigateToSignIn())
                is JoinEvent.SignInRequiredForPaidRoom -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToPayment(event.pin))
                )
                is JoinEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        JoinContentScreen(
            uiState = uiState,
            isQrScanAvailable = qrScanLauncher != null,
            onAction = viewModel::onAction
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun JoinContentScreen(
    uiState: JoinUiState,
    isQrScanAvailable: Boolean,
    onAction: (JoinAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
            .verticalScroll(rememberScrollState())
    ) {
        JoinHeader()
        JoinCard(
            uiState = uiState,
            isQrScanAvailable = isQrScanAvailable,
            onAction = onAction
        )
        if (!uiState.isSignedIn) {
            SignInLinkRow(onClickSignIn = { onAction(JoinAction.ClickSignIn) })
        }
    }
}

@Composable
private fun JoinHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(top = 64.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "패스메이트",
                color = PassmateColors.PrimaryDeep,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.48).sp
            )
            Text(
                text = "방 코드를 입력하고 시작하세요",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
        PassyMascot(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 44.dp, end = 4.dp)
                .size(width = 68.dp, height = 75.dp)
        )
    }
}

@Composable
private fun JoinCard(
    uiState: JoinUiState,
    isQrScanAvailable: Boolean,
    onAction: (JoinAction) -> Unit
) {
    PassmateCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        JoinCardContent(
            uiState = uiState,
            isQrScanAvailable = isQrScanAvailable,
            onAction = onAction
        )
    }
}

@Composable
private fun JoinCardContent(
    uiState: JoinUiState,
    isQrScanAvailable: Boolean,
    onAction: (JoinAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "PIN으로 입장하기",
                color = PassmateColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )
            Text(
                text = "선생님 화면의 6자리 숫자를 입력하세요",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                letterSpacing = (-0.28).sp
            )
        }
        PinInputField(
            pin = uiState.pin,
            onPinChange = { onAction(JoinAction.ChangePin(it)) }
        )
        if (isQrScanAvailable) {
            QrScanButton(onClick = { onAction(JoinAction.ClickScanQr) })
        }
        // 입장 전 방 정보 슬롯 (T081) — PIN 완성 시 프리페치된 호스트 등급·별점
        val roomInfo = uiState.roomInfo

        if (roomInfo != null) {
            RoomInfoCard(room = roomInfo)
        }
        NicknameField(
            nickname = uiState.nickname,
            onNicknameChange = { onAction(JoinAction.ChangeNickname(it)) }
        )
        AvatarField(
            selectedAvatarId = uiState.avatarId,
            onSelectAvatar = { onAction(JoinAction.SelectAvatar(it)) }
        )
        JoinButton(
            isJoining = uiState.isJoining,
            onClick = { onAction(JoinAction.ClickJoin) }
        )
    }
}

@Composable
private fun PinInputField(
    pin: String,
    onPinChange: (String) -> Unit
) {
    BasicTextField(
        value = pin,
        onValueChange = onPinChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = TextStyle(color = Color.Transparent),
        decorationBox = { innerTextField ->
            Box {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val spacing = 8.dp
                    val totalSpacing = spacing * (JoinInputPolicy.PIN_LENGTH - 1)
                    val digitWidth = (maxWidth - totalSpacing) / JoinInputPolicy.PIN_LENGTH

                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        repeat(JoinInputPolicy.PIN_LENGTH) { index ->
                            PinDigitBox(
                                digit = pin.getOrNull(index),
                                isActive = index == pin.length,
                                modifier = Modifier.size(width = digitWidth, height = 56.dp)
                            )
                        }
                    }
                }
                Box(modifier = Modifier.matchParentSize().alpha(0f)) {
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun PinDigitBox(
    digit: Char?,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val background = if (isActive) {
        PassmateColors.Surface
    } else {
        PassmateColors.FieldGray
    }
    val borderModifier = if (isActive) {
        Modifier.border(2.dp, PassmateColors.Primary, RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(borderModifier)
            .background(background, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (digit != null) {
            Text(
                text = digit.toString(),
                color = PassmateColors.PrimaryDeep,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.48).sp
            )
        }
    }
}

@Composable
private fun QrScanButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(PassmateColors.FieldGray, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "QR로 입장",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun NicknameField(
    nickname: String,
    onNicknameChange: (String) -> Unit
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
            onValueChange = onNicknameChange,
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

@Composable
private fun AvatarField(
    selectedAvatarId: Int,
    onSelectAvatar: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "내 캐릭터",
            color = PassmateColors.TextPrimary,
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
                            onClick = { onSelectAvatar(avatarId) }
                        )
                    }
                }
                repeat(AVATARS_PER_ROW - rowIds.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Text(
            text = "대기실·결과 화면에서 이 캐릭터로 보여요 (닉네임과 함께)",
            color = PassmateColors.TextSecondary,
            fontSize = 12.sp,
            letterSpacing = (-0.24).sp
        )
    }
}

@Composable
private fun AvatarPickItem(
    avatarId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        PassmateColors.Primary
    } else {
        Color.Transparent
    }

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
private fun JoinButton(
    isJoining: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(PassmateColors.Primary, RoundedCornerShape(16.dp))
            .clickable(enabled = !isJoining, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isJoining) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = PassmateColors.Surface,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "입장하기",
                color = PassmateColors.Surface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.32).sp
            )
        }
    }
}

@Composable
private fun SignInLinkRow(onClickSignIn: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "기록을 남기려면",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            letterSpacing = (-0.28).sp
        )
        Text(
            text = "로그인",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            modifier = Modifier.clickable(onClick = onClickSignIn)
        )
    }
}

private const val AVATARS_PER_ROW = 6

// --- Preview ---

@PassmatePreview
@Composable
private fun JoinContentScreenPreview() {
    PassmateTheme {
        JoinContentScreen(
            uiState = JoinUiState(),
            isQrScanAvailable = true,
            onAction = {}
        )
    }
}

// PIN 6자리 입력 완료 — 방 정보 프리페치 결과 + 닉네임·캐릭터 선택
@PassmatePreview
@Composable
private fun JoinContentScreenFilledPreview() {
    PassmateTheme {
        JoinContentScreen(
            uiState = JoinUiState(
                pin = "482913",
                nickname = "민지",
                avatarId = 3,
                isSignedIn = true,
                roomInfo = RoomInfo(
                    roomId = 801,
                    pin = "482913",
                    title = "8월 4주차 Spring 스터디",
                    topic = "이차함수 심화",
                    status = RoomStatus.WAITING,
                    questionCount = 8,
                    estimatedMinutes = 20,
                    scheduledAt = null,
                    participantCount = 12,
                    maxParticipants = 30,
                    isPaid = false,
                    entryFee = null,
                    isGuestAllowed = true,
                    host = RoomHost(
                        userId = 11,
                        nickname = "김선생",
                        level = 3,
                        avgStars = 4.8,
                        ratingCount = 32
                    )
                )
            ),
            isQrScanAvailable = true,
            onAction = {}
        )
    }
}
