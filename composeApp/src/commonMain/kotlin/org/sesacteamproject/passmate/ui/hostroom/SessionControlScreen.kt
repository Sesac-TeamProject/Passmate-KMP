package org.sesacteamproject.passmate.ui.hostroom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.StudentAvatar
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.session.domain.model.QuestionType
import org.sesacteamproject.passmate.session.domain.model.SubmissionStatus
import org.sesacteamproject.passmate.theme.PassmateColors

// Figma "UI 디자인 v6" M-T2(349:10123) — 진행 리모컨: 프로젝터는 벽, 폰은 조작.
// 문항·제출 현황은 서버 이벤트·재조회로만 갱신되고, 화면 전환(SESSION_ENDED→리포트)도 서버 이벤트로만 일어난다 (규칙 §2-1-2)
@Composable
fun SessionControlScreen(
    roomId: Long,
    pin: String,
    viewModel: SessionControlViewModel = koinScreenViewModel(),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEndConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(roomId) {
        viewModel.onAction(SessionControlAction.Enter(roomId, pin))
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is SessionControlEvent.RequireSignIn -> onNavigate(NavigationAction.NavigateToSignIn)
                is SessionControlEvent.SessionEnded -> onNavigate(NavigationAction.NavigateToRoomReport(event.roomId))
                is SessionControlEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        SessionControlContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onClickBack = { onNavigate(NavigationAction.NavigateBack) },
            onClickEndSession = { showEndConfirm = true }
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            title = {
                Text(
                    text = "세션 종료",
                    color = PassmateColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "세션을 종료하면 최종 결과가 확정되고 학생별 리포트가 생성돼요.",
                    color = PassmateColors.TextSecondary,
                    fontSize = 14.sp,
                    letterSpacing = (-0.28).sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showEndConfirm = false
                    viewModel.onAction(SessionControlAction.ConfirmEndSession)
                }) {
                    Text(text = "종료", color = PassmateColors.WeakTopicText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirm = false }) {
                    Text(text = "취소", color = PassmateColors.TextSecondary)
                }
            },
            containerColor = PassmateColors.Surface
        )
    }
}

@Composable
private fun SessionControlContentScreen(
    uiState: SessionControlUiState,
    onAction: (SessionControlAction) -> Unit,
    onClickBack: () -> Unit,
    onClickEndSession: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
    ) {
        when {
            uiState.isLoading -> LoadingBox()
            uiState.loadFailed -> ErrorBox(onRetry = { onAction(SessionControlAction.Retry) })
            else -> LoadedControl(
                uiState = uiState,
                onAction = onAction,
                onClickBack = onClickBack,
                onClickEndSession = onClickEndSession
            )
        }
    }
}

@Composable
private fun LoadedControl(
    uiState: SessionControlUiState,
    onAction: (SessionControlAction) -> Unit,
    onClickBack: () -> Unit,
    onClickEndSession: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 52.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = PassmateColors.TextPrimary,
                fontSize = 20.sp,
                modifier = Modifier
                    .clickable(onClick = onClickBack)
                    .padding(4.dp)
            )
            Text(
                text = uiState.roomTitle,
                color = PassmateColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.34).sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            Text(
                text = "PIN ${formatPin(uiState.pin)}",
                color = PassmateColors.PrimaryDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.28).sp
            )
        }
        ProjectorChip(isConnected = uiState.isProjectorConnected)
        if (uiState.status == RoomStatus.WAITING) {
            WaitingPanel(
                participantCount = uiState.participantCount,
                isControlling = uiState.isControlling,
                onClickStart = { onAction(SessionControlAction.ClickStart) }
            )
        } else {
            QuestionCard(uiState = uiState)
            PttButton(
                uiState = uiState,
                onAction = onAction
            )
            ControlButtons(
                uiState = uiState,
                onAction = onAction
            )
        }
        BottomControls(
            uiState = uiState,
            onToggleLock = { onAction(SessionControlAction.ToggleLock) },
            onClickEndSession = onClickEndSession
        )
    }
}

@Composable
private fun ProjectorChip(isConnected: Boolean) {
    val label = if (isConnected) {
        "프로젝터 연결됨 · 벽 화면과 동기화 중"
    } else {
        "프로젝터 미연결 · 웹에서 프로젝터 화면을 열어 주세요"
    }
    val dotColor = if (isConnected) PassmateColors.Primary else PassmateColors.TextTertiary

    Row(
        modifier = Modifier
            .background(PassmateColors.BackgroundMint, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(dotColor, CircleShape)
        )
        Text(
            text = label,
            color = PassmateColors.PrimaryDeep,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.24).sp
        )
    }
}

@Composable
private fun WaitingPanel(
    participantCount: Int,
    isControlling: Boolean,
    onClickStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(20.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "학생 ${participantCount}명 대기 중",
            color = PassmateColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.36).sp
        )
        Text(
            text = "시작하면 전체 학생에게 첫 문항이 공개돼요",
            color = PassmateColors.TextSecondary,
            fontSize = 13.sp,
            letterSpacing = (-0.26).sp
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(PassmateColors.Primary, RoundedCornerShape(16.dp))
                .clickable(enabled = !isControlling, onClick = onClickStart),
            contentAlignment = Alignment.Center
        ) {
            if (isControlling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = PassmateColors.Surface,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "세션 시작",
                    color = PassmateColors.Surface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(uiState: SessionControlUiState) {
    val question = uiState.question

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(20.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (question == null) {
            Text(
                text = "다음 문항을 시작해 주세요",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                letterSpacing = (-0.28).sp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Q${question.questionNo} / ${uiState.questionCount ?: "-"}",
                    color = PassmateColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.36).sp
                )
                Text(
                    text = typeLabel(question.type),
                    color = PassmateColors.RatingTagSelectedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(PassmateColors.RatingTagSelectedBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                Box(modifier = Modifier.weight(1f))
                TimerCircle(
                    remainingSec = uiState.remainingSec,
                    isClosed = uiState.isQuestionClosed
                )
            }
            Text(
                text = question.body,
                color = PassmateColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.3).sp
            )
            uiState.submissions?.let { submissions ->
                SubmissionSection(submissions = submissions)
            }
        }
    }
}

@Composable
private fun TimerCircle(
    remainingSec: Int,
    isClosed: Boolean
) {
    val ringColor = if (isClosed) PassmateColors.Border else PassmateColors.TimerAmber
    val label = if (isClosed) "마감" else remainingSec.toString()

    Box(
        modifier = Modifier
            .size(48.dp)
            .border(3.dp, ringColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = PassmateColors.TextPrimary,
            fontSize = if (isClosed) 12.sp else 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubmissionSection(submissions: SubmissionStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "제출 ${submissions.submittedCount} / ${submissions.totalCount}",
                color = PassmateColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.weight(1f)
            )
            submissions.accuracyPercent?.let { accuracy ->
                Text(
                    text = "정답률 (실시간) ${accuracy}%",
                    color = PassmateColors.TextSecondary,
                    fontSize = 13.sp,
                    letterSpacing = (-0.26).sp
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            submissions.participants.filter { it.submitted }.forEach { participant ->
                SubmittedAvatar(avatarId = participant.avatarId)
            }
            val pendingCount = submissions.participants.count { !it.submitted }

            if (pendingCount > 0) {
                Text(
                    text = "미제출 ${pendingCount}명",
                    color = PassmateColors.TextTertiary,
                    fontSize = 13.sp,
                    letterSpacing = (-0.26).sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        submissions.choices.forEachIndexed { index, choice ->
            ChoiceBar(
                label = choice.label,
                count = choice.count,
                total = submissions.submittedCount,
                color = choiceColor(index)
            )
        }
    }
}

@Composable
private fun SubmittedAvatar(avatarId: Int?) {
    Box {
        StudentAvatar(
            avatarId = avatarId,
            modifier = Modifier.size(30.dp)
        )
        Box(
            modifier = Modifier
                .size(9.dp)
                .align(Alignment.BottomEnd)
                .background(PassmateColors.Primary, CircleShape)
                .border(1.dp, PassmateColors.Surface, CircleShape)
        )
    }
}

@Composable
private fun ChoiceBar(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = PassmateColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .size(22.dp)
                .background(color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .padding(top = 2.dp),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(PassmateColors.FieldGray, CircleShape)
        ) {
            val fraction = if (total > 0) count.toFloat() / total else 0f

            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(8.dp)
                        .background(color, CircleShape)
                )
            }
        }
        Text(
            text = "${count}명",
            color = PassmateColors.TextSecondary,
            fontSize = 13.sp,
            letterSpacing = (-0.26).sp
        )
    }
}

// "길게 눌러 힌트 말하기" (M-T2, T121) — 누르는 동안 녹음, 놓으면 업로드. Desktop은 미지원 안내 칩
@Composable
private fun PttButton(
    uiState: SessionControlUiState,
    onAction: (SessionControlAction) -> Unit
) {
    val recorder = rememberVoiceHintRecorder()

    if (recorder == null) {
        Text(
            text = "🎙 힌트 말하기(PTT)는 모바일 앱에서 지원돼요",
            color = PassmateColors.TextTertiary,
            fontSize = 13.sp,
            letterSpacing = (-0.26).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(PassmateColors.FieldGray, RoundedCornerShape(16.dp))
                .padding(vertical = 14.dp)
        )
    } else {
        var isRecording by remember { mutableStateOf(false) }
        val currentOnAction = rememberUpdatedState(onAction)
        val currentIsSending = rememberUpdatedState(uiState.isSendingHint)
        val background = if (isRecording) PassmateColors.Primary else PassmateColors.Surface
        val textColor = if (isRecording) PassmateColors.Surface else PassmateColors.PrimaryDeep
        val label = when {
            uiState.isSendingHint -> "힌트 보내는 중…"
            isRecording -> "녹음 중… 놓으면 전송돼요"
            else -> "🎙 길게 눌러 힌트 말하기 (PTT)"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, PassmateColors.Primary, RoundedCornerShape(16.dp))
                .background(background, RoundedCornerShape(16.dp))
                .pointerInput(recorder) {
                    detectTapGestures(
                        onPress = {
                            if (!currentIsSending.value) {
                                if (recorder.start()) {
                                    isRecording = true
                                    val released = tryAwaitRelease()

                                    isRecording = false
                                    if (released) {
                                        val hint = recorder.stop()

                                        if (hint != null) {
                                            currentOnAction.value(SessionControlAction.SendVoiceHint(hint))
                                        } else {
                                            currentOnAction.value(
                                                SessionControlAction.Notice("녹음이 너무 짧아요 · 길게 눌러 말해 주세요")
                                            )
                                        }
                                    } else {
                                        recorder.cancel()
                                    }
                                } else {
                                    currentOnAction.value(
                                        SessionControlAction.Notice("마이크 권한을 허용한 뒤 다시 길게 눌러 주세요")
                                    )
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isSendingHint) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = PassmateColors.PrimaryDeep,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.28).sp
                )
            }
        }
    }
}

@Composable
private fun ControlButtons(
    uiState: SessionControlUiState,
    onAction: (SessionControlAction) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .border(1.dp, PassmateColors.Border, RoundedCornerShape(14.dp))
                .clickable(
                    enabled = !uiState.isControlling && uiState.question != null && !uiState.isQuestionClosed,
                    onClick = { onAction(SessionControlAction.ClickEndQuestion) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "바로 마감",
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(PassmateColors.Primary, RoundedCornerShape(14.dp))
                .clickable(
                    enabled = !uiState.isControlling,
                    onClick = { onAction(SessionControlAction.ClickNext) }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isControlling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = PassmateColors.Surface,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "다음 문항 →",
                    color = PassmateColors.Surface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.28).sp
                )
            }
        }
    }
}

@Composable
private fun BottomControls(
    uiState: SessionControlUiState,
    onToggleLock: () -> Unit,
    onClickEndSession: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (uiState.isLocked) "학생 화면 잠금 해제" else "학생 화면 잠금",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            modifier = Modifier
                .clickable(enabled = !uiState.isControlling, onClick = onToggleLock)
                .padding(4.dp)
        )
        Text(
            text = "세션 종료",
            color = PassmateColors.WeakTopicText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            modifier = Modifier
                .clickable(onClick = onClickEndSession)
                .padding(4.dp)
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
            text = "방 상태를 불러오지 못했어요",
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

private fun typeLabel(type: QuestionType): String {
    return when (type) {
        QuestionType.MULTIPLE_CHOICE -> "객관식"
        QuestionType.OX -> "OX"
        QuestionType.ESSAY -> "서술형"
        QuestionType.UNKNOWN -> "문항"
    }
}

private fun choiceColor(index: Int): Color {
    return when (index % 4) {
        0 -> PassmateColors.WrongPink
        1 -> PassmateColors.ChipBlue
        2 -> PassmateColors.TimerAmber
        else -> PassmateColors.ChipGreen
    }
}

private fun formatPin(pin: String): String {
    return pin.chunked(3).joinToString(" ")
}
