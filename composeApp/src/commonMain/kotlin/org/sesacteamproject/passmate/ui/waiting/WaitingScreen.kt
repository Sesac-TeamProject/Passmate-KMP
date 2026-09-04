package org.sesacteamproject.passmate.ui.waiting

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateCard
import org.sesacteamproject.passmate.component.PassyMascot
import org.sesacteamproject.passmate.component.StudentAvatar
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.room.domain.model.Participant
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// Figma "UI 디자인 v6" M-02(349:9252) 기준 — 입장 완료 카드 + 참가자 실시간 표시
@Composable
fun WaitingScreen(
    viewModel: WaitingViewModel = koinScreenViewModel(),
    pin: String,
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(pin) {
        viewModel.onAction(WaitingAction.Enter(pin))
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is WaitingEvent.SessionStarted -> onNavigate(NavigationAction.NavigateToPlay(event.pin))
                is WaitingEvent.RoomClosed -> {
                    snackbarHostState.showSnackbar(event.message)
                    onNavigate(NavigationAction.NavigateToHome)
                }
                is WaitingEvent.Left -> onNavigate(NavigationAction.NavigateBack)
                is WaitingEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        WaitingContentScreen(
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
private fun WaitingContentScreen(
    uiState: WaitingUiState,
    onAction: (WaitingAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
            // 화면 배경은 상태바 뒤까지 깔고 콘텐츠만 내린다 (iOS의 background(...).ignoresSafeArea() 미러)
            .statusBarsPadding()
    ) {
        WaitingHeader(
            uiState = uiState,
            onClickLeave = { onAction(WaitingAction.ClickLeave) }
        )
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PassmateColors.Primary)
            }
        } else {
            EnteredCard(uiState = uiState)
            Spacer(modifier = Modifier.weight(1f))
            WaitingDots(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 40.dp)
            )
        }
    }
}

@Composable
private fun WaitingHeader(
    uiState: WaitingUiState,
    onClickLeave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 64.dp, end = 24.dp, bottom = 48.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = uiState.roomTitle,
                color = PassmateColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )
            Text(
                text = "PIN ${formatPin(uiState.pin)}",
                color = PassmateColors.PrimaryDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
        Text(
            text = "나가기",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clickable(onClick = onClickLeave)
                .padding(4.dp)
        )
    }
}

@Composable
private fun EnteredCard(uiState: WaitingUiState) {
    PassmateCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(PassmateColors.FieldGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PassyMascot(modifier = Modifier.size(width = 60.dp, height = 66.dp))
            }
            Text(
                text = "입장 완료!",
                color = PassmateColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.48).sp
            )
            Text(
                text = waitingMessage(uiState.myNickname),
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
            ParticipantAvatarRow(
                participants = uiState.participants,
                myParticipantId = uiState.myParticipantId
            )
            Text(
                text = "학생 ${uiState.totalCount}명이 함께해요",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                letterSpacing = (-0.28).sp
            )
        }
    }
}

// 아바타 행은 나를 제외한 참가자를 보여준다 (M-02: 본인은 문구, 다른 학생은 아바타)
@Composable
private fun ParticipantAvatarRow(
    participants: List<Participant>,
    myParticipantId: Long?
) {
    val others = participants.filter { it.participantId != myParticipantId }
    val visible = others.take(MAX_VISIBLE_AVATARS)
    val overflow = others.size - visible.size

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visible.forEach { participant ->
            StudentAvatar(
                avatarId = participant.avatarId,
                modifier = Modifier.size(34.dp)
            )
        }
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(PassmateColors.FieldGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$overflow",
                    color = PassmateColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.28).sp
                )
            }
        }
    }
}

@Composable
private fun WaitingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = DOT_COUNT.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(DOT_COUNT) { index ->
            val isActive = phase.toInt() % DOT_COUNT == index

            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(if (isActive) 1f else 0.4f)
                    .background(
                        color = if (isActive) PassmateColors.Primary else PassmateColors.Border,
                        shape = CircleShape
                    )
            )
        }
    }
}

private fun formatPin(pin: String): String {
    return pin.chunked(3).joinToString(" ")
}

private fun waitingMessage(nickname: String?): String {
    return if (nickname != null) {
        "$nickname 님, 선생님이 곧 시작해요"
    } else {
        "선생님이 곧 시작해요"
    }
}

private const val MAX_VISIBLE_AVATARS = 4

private const val DOT_COUNT = 3

// --- Preview ---

@PassmatePreview
@Composable
private fun WaitingContentScreenPreview() {
    PassmateTheme {
        WaitingContentScreen(
            uiState = WaitingUiState(
                isLoading = false,
                roomTitle = "8월 4주차 Spring 스터디",
                pin = "482913",
                myParticipantId = 9001,
                myNickname = "민지",
                participants = listOf(
                    Participant(participantId = 9001, nickname = "민지", avatarId = 1, isGuest = false, isConnected = true),
                    Participant(participantId = 9002, nickname = "준영", avatarId = 2, isGuest = false, isConnected = true),
                    Participant(participantId = 9003, nickname = "혜림", avatarId = 5, isGuest = true, isConnected = false)
                ),
                totalCount = 3
            ),
            onAction = {}
        )
    }
}

// 아직 아무도 안 들어온 대기실 — 빈 상태
@PassmatePreview
@Composable
private fun WaitingContentScreenEmptyPreview() {
    PassmateTheme {
        WaitingContentScreen(
            uiState = WaitingUiState(
                isLoading = false,
                roomTitle = "8월 4주차 Spring 스터디",
                pin = "482913",
                myParticipantId = 9001,
                myNickname = "민지",
                participants = emptyList(),
                totalCount = 0
            ),
            onAction = {}
        )
    }
}
