package org.sesacteamproject.passmate.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateCard
import org.sesacteamproject.passmate.component.PassyMascot
import org.sesacteamproject.passmate.component.StudentAvatar
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.session.domain.model.AnswerResult
import org.sesacteamproject.passmate.session.domain.model.QuestionType
import org.sesacteamproject.passmate.session.domain.model.RankEntry
import org.sesacteamproject.passmate.session.domain.model.SessionQuestion
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// Figma "UI 디자인 v6" M-03(349:9277)·M-04(349:9333)·M-05(349:9352) 기준 —
// 문항 풀이·제출 결과·최종 결과를 서버 이벤트 단계(Phase)로 렌더링한다
@Composable
fun PlayScreen(
    viewModel: PlayViewModel = koinScreenViewModel(),
    pin: String,
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val voiceHintPlayer = rememberVoiceHintPlayer()
    var isLeaveDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(pin) {
        viewModel.onAction(PlayAction.Enter(pin))
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is PlayEvent.PlayVoiceHint -> {
                    if (voiceHintPlayer != null) {
                        voiceHintPlayer.play(event.hint.clipUrl)
                    } else {
                        snackbarHostState.showSnackbar("선생님이 음성 힌트를 보냈어요")
                    }
                }
                is PlayEvent.OpenResult -> onNavigate(NavigationAction.NavigateToResult(event.roomId))
                is PlayEvent.OpenSignup -> onNavigate(NavigationAction.NavigateToSignIn())
                is PlayEvent.RoomClosed -> {
                    snackbarHostState.showSnackbar(event.message)
                    onNavigate(NavigationAction.NavigateToHome)
                }
                is PlayEvent.Left -> onNavigate(NavigationAction.NavigateToHome)
                is PlayEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    // 문항 전환·세션 종료로 배너가 사라지면 재생도 함께 멈춘다
    LaunchedEffect(uiState.activeVoiceHint) {
        if (uiState.activeVoiceHint == null) {
            voiceHintPlayer?.stop()
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        PlayContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onClickLeave = { isLeaveDialogVisible = true }
        )
        // 음성 힌트 배너 — 오버레이 소유는 컨테이너 (규칙 §11-1)
        val activeVoiceHint = uiState.activeVoiceHint

        if (activeVoiceHint != null && uiState.phase != PlayUiState.Phase.FINISHED) {
            VoiceHintBanner(
                hint = activeVoiceHint,
                controller = voiceHintPlayer,
                onClickReplay = { viewModel.onAction(PlayAction.ClickReplayHint) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 20.dp, end = 20.dp, bottom = 92.dp)
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    // 진행 중 퇴장은 확인 다이얼로그를 거친다 (규칙 §2-1-2) — 오버레이는 컨테이너가 소유 (규칙 §11-1)
    if (isLeaveDialogVisible) {
        AlertDialog(
            onDismissRequest = { isLeaveDialogVisible = false },
            title = { Text("방을 나갈까요?") },
            text = { Text("진행 중인 세션에서 나가면 남은 문항을 풀 수 없어요.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isLeaveDialogVisible = false
                        viewModel.onAction(PlayAction.ConfirmLeave)
                    }
                ) {
                    Text("나가기", color = PassmateColors.PrimaryDeep)
                }
            },
            dismissButton = {
                TextButton(onClick = { isLeaveDialogVisible = false }) {
                    Text("계속 풀기", color = PassmateColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PlayContentScreen(
    uiState: PlayUiState,
    onAction: (PlayAction) -> Unit,
    onClickLeave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
            // 화면 배경은 상태바 뒤까지 깔고 콘텐츠만 내린다 (iOS의 background(...).ignoresSafeArea() 미러)
            .statusBarsPadding()
    ) {
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
            when {
                uiState.phase == PlayUiState.Phase.FINISHED -> FinalResultContent(
                    uiState = uiState,
                    onAction = onAction
                )
                uiState.phase == PlayUiState.Phase.QUESTION && !uiState.hasSubmitted -> QuestionContent(
                    uiState = uiState,
                    onAction = onAction,
                    onClickLeave = onClickLeave
                )
                else -> WaitingNextContent(
                    uiState = uiState,
                    onClickLeave = onClickLeave
                )
            }
        }
    }
}

// ─── M-03 문항 풀이 ───

@Composable
private fun ColumnScope.QuestionContent(
    uiState: PlayUiState,
    onAction: (PlayAction) -> Unit,
    onClickLeave: () -> Unit
) {
    val question = uiState.question

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
    ) {
        PlayHeader(
            uiState = uiState,
            onClickLeave = onClickLeave
        )
        if (uiState.isLocked) {
            LockedBanner()
        }
        if (question != null) {
            QuestionCard(
                uiState = uiState,
                question = question,
                onAction = onAction
            )
        }
    }
    SubmitButton(
        isSubmitting = uiState.isSubmitting,
        onClick = { onAction(PlayAction.ClickSubmit) }
    )
}

@Composable
private fun PlayHeader(
    uiState: PlayUiState,
    onClickLeave: () -> Unit
) {
    val question = uiState.question

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 60.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Q${question?.questionNo ?: "-"} / ${uiState.questionCount} · ${questionTypeLabel(question?.type)}",
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
            Text(
                text = "나가기",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp,
                modifier = Modifier
                    .clickable(onClick = onClickLeave)
                    .padding(4.dp)
            )
        }
        QuestionProgressBar(
            currentQuestionNo = question?.questionNo ?: 0,
            questionCount = uiState.questionCount
        )
    }
}

@Composable
private fun QuestionProgressBar(
    currentQuestionNo: Int,
    questionCount: Int
) {
    if (questionCount > 0) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(questionCount) { index ->
                val isReached = index < currentQuestionNo

                Box(
                    modifier = Modifier
                        .width(if (isReached) 26.dp else 12.dp)
                        .height(6.dp)
                        .background(
                            color = if (isReached) PassmateColors.TimerAmber else PassmateColors.FieldGray,
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun LockedBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .background(PassmateColors.FieldGray, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "선생님이 화면을 잠갔어요",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun QuestionCard(
    uiState: PlayUiState,
    question: SessionQuestion,
    onAction: (PlayAction) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 46.dp, end = 20.dp, bottom = 16.dp)
    ) {
        PassmateCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 44.dp, end = 20.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = question.body,
                    color = PassmateColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                AnswerInputArea(
                    uiState = uiState,
                    question = question,
                    onAction = onAction
                )
            }
        }
        TimerBadge(
            remainingSeconds = uiState.remainingSeconds,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-30).dp)
        )
    }
}

@Composable
private fun TimerBadge(
    remainingSeconds: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(60.dp)
            .background(PassmateColors.Surface, CircleShape)
            .border(6.dp, PassmateColors.TimerAmber, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = remainingSeconds.toString(),
            color = PassmateColors.PrimaryDeep,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        )
    }
}

@Composable
private fun AnswerInputArea(
    uiState: PlayUiState,
    question: SessionQuestion,
    onAction: (PlayAction) -> Unit
) {
    when (question.type) {
        QuestionType.ESSAY -> EssayAnswerField(
            essayAnswer = uiState.essayAnswer,
            onChange = { onAction(PlayAction.ChangeEssayAnswer(it)) }
        )
        QuestionType.OX -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("O", "X").forEachIndexed { index, label ->
                ChoiceRow(
                    chipLabel = label,
                    text = label,
                    chipIndex = index,
                    isSelected = uiState.selectedChoiceIndex == index,
                    onClick = { onAction(PlayAction.SelectChoice(index)) }
                )
            }
        }
        else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            question.choices.forEachIndexed { index, choice ->
                ChoiceRow(
                    chipLabel = ('A' + index).toString(),
                    text = choice,
                    chipIndex = index,
                    isSelected = uiState.selectedChoiceIndex == index,
                    onClick = { onAction(PlayAction.SelectChoice(index)) }
                )
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    chipLabel: String,
    text: String,
    chipIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val rowBackground = if (isSelected) PassmateColors.Primary else PassmateColors.FieldGray
    val chipBackground = if (isSelected) PassmateColors.Surface else chipColor(chipIndex)
    val chipTextColor = if (isSelected) PassmateColors.PrimaryDeep else chipTextColor(chipIndex)
    val textColor = if (isSelected) PassmateColors.Surface else PassmateColors.TextPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .background(rowBackground, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(chipBackground, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chipLabel,
                color = chipTextColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.32).sp,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Text(
                text = "✓",
                color = PassmateColors.Surface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EssayAnswerField(
    essayAnswer: String,
    onChange: (String) -> Unit
) {
    BasicTextField(
        value = essayAnswer,
        onValueChange = onChange,
        textStyle = TextStyle(
            color = PassmateColors.TextPrimary,
            fontSize = 14.sp,
            letterSpacing = (-0.28).sp
        ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 140.dp)
                    .background(PassmateColors.FieldGray, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                if (essayAnswer.isEmpty()) {
                    Text(
                        text = "답변을 입력해 주세요",
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

@Composable
private fun SubmitButton(
    isSubmitting: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
            .height(54.dp)
            .background(PassmateColors.Primary, RoundedCornerShape(16.dp))
            .clickable(enabled = !isSubmitting, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = PassmateColors.Surface,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "제출하기",
                color = PassmateColors.Surface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.32).sp
            )
        }
    }
}

// ─── M-04 제출 결과 · 다음 문항 대기 ───

@Composable
private fun ColumnScope.WaitingNextContent(
    uiState: PlayUiState,
    onClickLeave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 60.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (uiState.question != null) "Q${uiState.question.questionNo} / ${uiState.questionCount}" else "잠시만요",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            modifier = Modifier.weight(2f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "나가기",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClickLeave)
                .padding(4.dp)
        )
    }
    Spacer(modifier = Modifier.weight(1f))
    MyResultCard(uiState = uiState)
    Spacer(modifier = Modifier.weight(1f))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "다음 문항을 기다리고 있어요",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun MyResultCard(uiState: PlayUiState) {
    val result = uiState.myAnswerResult
    val reveal = uiState.reveal

    PassmateCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PassyMascot(modifier = Modifier.size(width = 84.dp, height = 92.dp))
            Text(
                text = resultTitle(uiState),
                color = if (result?.correct == false) PassmateColors.TextPrimary else PassmateColors.PrimaryDeep,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.68).sp
            )
            if (uiState.rank != null) {
                RankChip(
                    rank = uiState.rank,
                    rankDelta = result?.rankDelta
                )
            }
            resultCaption(uiState)?.let { caption ->
                Text(
                    text = caption,
                    color = PassmateColors.TextSecondary,
                    fontSize = 14.sp,
                    letterSpacing = (-0.28).sp,
                    textAlign = TextAlign.Center
                )
            }
            if (reveal?.answer != null) {
                Text(
                    text = "정답 ${reveal.answer} · ${reveal.correctAnswererCount}명 정답",
                    color = PassmateColors.PrimaryDeep,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.28).sp,
                    textAlign = TextAlign.Center
                )
            }
            if (reveal?.explanation != null) {
                Text(
                    text = reveal.explanation,
                    color = PassmateColors.TextSecondary,
                    fontSize = 13.sp,
                    letterSpacing = (-0.26).sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RankChip(
    rank: Int,
    rankDelta: Int?
) {
    Row(
        modifier = Modifier
            .background(PassmateColors.FieldGray, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "현재 ${rank}위",
            color = PassmateColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
        if (rankDelta != null && rankDelta != 0) {
            Text(
                text = if (rankDelta > 0) "▲$rankDelta" else "▼${-rankDelta}",
                color = if (rankDelta > 0) PassmateColors.Primary else PassmateColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── M-05 최종 결과 ───

@Composable
private fun ColumnScope.FinalResultContent(
    uiState: PlayUiState,
    onAction: (PlayAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
    ) {
        FinalResultHeader(uiState = uiState)
        FinalRankingCard(
            uiState = uiState,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .offset(y = (-40).dp)
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = if (uiState.isGuest) 10.dp else 24.dp)
            .height(54.dp)
            .background(PassmateColors.Primary, RoundedCornerShape(16.dp))
            .clickable { onAction(PlayAction.ClickViewReport) },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "내 리포트 보기",
            color = PassmateColors.Surface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.32).sp
        )
    }
    // 게스트 가입 유도 (T075) — 세션 종료 화면에서 기록 저장 (M-05 하단 버튼)
    if (uiState.isGuest) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
                .height(50.dp)
                .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
                .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
                .clickable { onAction(PlayAction.ClickSignup) },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "가입하고 이 기록 저장하기",
                color = PassmateColors.PrimaryDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
    }
}

@Composable
private fun FinalResultHeader(uiState: PlayUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.BackgroundMint)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, bottom = 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "최종 결과",
                color = PassmateColors.InkGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
            PodiumRow(finalRanking = uiState.finalRanking)
        }
        PassyMascot(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 36.dp, end = 6.dp)
                .size(width = 60.dp, height = 66.dp)
        )
    }
}

@Composable
private fun PodiumRow(finalRanking: List<RankEntry>) {
    val top3 = finalRanking.filter { it.rank in 1..3 }
    val ordered = listOf(2, 1, 3).mapNotNull { rank -> top3.firstOrNull { it.rank == rank } }

    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        ordered.forEach { entry ->
            PodiumBlock(entry = entry)
        }
    }
}

@Composable
private fun PodiumBlock(entry: RankEntry) {
    val blockHeight = when (entry.rank) {
        1 -> 102.dp
        2 -> 74.dp
        else -> 62.dp
    }

    Box(contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.padding(top = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .width(80.dp)
                    .height(blockHeight)
                    .background(rankColor(entry.rank), RoundedCornerShape(12.dp))
                    .padding(top = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = entry.rank.toString(),
                    color = rankTextColor(entry.rank),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.32).sp
                )
            }
        }
        StudentAvatar(
            avatarId = entry.avatarId,
            modifier = Modifier.size(44.dp)
        )
    }
}

@Composable
private fun FinalRankingCard(
    uiState: PlayUiState,
    modifier: Modifier = Modifier
) {
    PassmateCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = finalSummaryText(uiState),
                color = PassmateColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.32).sp
            )
            uiState.finalRanking.forEach { entry ->
                FinalRankingRow(
                    entry = entry,
                    isMe = entry.participantId == uiState.myParticipantId
                )
            }
        }
    }
}

@Composable
private fun FinalRankingRow(
    entry: RankEntry,
    isMe: Boolean
) {
    val textColor = if (isMe) PassmateColors.PrimaryDeep else PassmateColors.TextPrimary
    val rowModifier = if (isMe) {
        Modifier.background(PassmateColors.FieldGray, RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(rowModifier)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(rankColor(entry.rank), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.rank.toString(),
                color = rankTextColor(entry.rank),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
        Text(
            text = if (isMe) "나 (${entry.nickname})" else entry.nickname,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatScore(entry.total),
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

// ─── 공통 헬퍼 ───

private fun questionTypeLabel(type: QuestionType?): String {
    return when (type) {
        QuestionType.MULTIPLE_CHOICE -> "객관식"
        QuestionType.OX -> "OX"
        QuestionType.ESSAY -> "서술형"
        else -> "문항"
    }
}

private fun resultTitle(uiState: PlayUiState): String {
    val result = uiState.myAnswerResult

    return when {
        result != null && result.isProvisional -> "+${result.earnedScore.toInt()}점 (잠정)"
        result != null -> "+${result.earnedScore.toInt()}점"
        uiState.reveal != null && !uiState.hasSubmitted -> "시간 종료!"
        else -> "곧 문제가 시작돼요!"
    }
}

private fun resultCaption(uiState: PlayUiState): String? {
    val result = uiState.myAnswerResult

    return when {
        result != null && result.isProvisional -> "서술형은 AI 분석·선생님 첨삭 후 확정돼요"
        result?.correct == true -> "기본 +${result.baseScore.toInt()} · 속도 보너스 +${result.speedBonus.toInt()}"
        result?.correct == false -> "아쉬워요, 다음 문항에서 만회해요"
        uiState.reveal != null && !uiState.hasSubmitted -> "미제출로 처리됐어요"
        else -> null
    }
}

private fun finalSummaryText(uiState: PlayUiState): String {
    val rankPart = uiState.rank?.let { "${it}위 · " } ?: ""

    return "$rankPart${formatScore(uiState.totalScore)}점 · 정답 ${uiState.myCorrectCount}/${uiState.questionCount}"
}

private fun chipColor(index: Int): Color {
    return when (index % 4) {
        0 -> PassmateColors.ChipOrange
        1 -> PassmateColors.ChipBlue
        2 -> PassmateColors.ChipGold
        else -> PassmateColors.ChipGreen
    }
}

private fun chipTextColor(index: Int): Color {
    return when (index % 4) {
        0 -> PassmateColors.ChipOrangeText
        1 -> PassmateColors.ChipBlueText
        2 -> PassmateColors.ChipGoldText
        else -> PassmateColors.ChipGreenText
    }
}

private fun rankColor(rank: Int): Color {
    return when (rank) {
        1 -> PassmateColors.ChipGold
        2 -> PassmateColors.ChipBlue
        3 -> PassmateColors.ChipOrange
        else -> PassmateColors.FieldGray
    }
}

private fun rankTextColor(rank: Int): Color {
    return when (rank) {
        1 -> PassmateColors.ChipGoldText
        2 -> PassmateColors.ChipBlueText
        3 -> PassmateColors.ChipOrangeText
        else -> PassmateColors.TextSecondary
    }
}

private fun formatScore(total: Double): String {
    val digits = total.toLong().toString()

    return digits.reversed().chunked(3).joinToString(",").reversed()
}

// --- Preview ---

private fun previewQuestion(isClosed: Boolean): SessionQuestion {
    return SessionQuestion(
        questionId = 501,
        questionNo = 3,
        type = QuestionType.MULTIPLE_CHOICE,
        body = "등차수열 2, 5, 8, 11, ...의 공차를 구하세요.",
        choices = listOf("1", "2", "3", "4"),
        points = 100,
        timeLimitSec = 30,
        endsAt = "2026-08-28T10:15:30Z",
        isClosed = isClosed
    )
}

// 문항 풀이 중 — 남은 시간은 서버 endsAt 기준 렌더링만 한다 (규칙 §5)
@PassmatePreview
@Composable
private fun PlayContentScreenQuestionPreview() {
    PassmateTheme {
        PlayContentScreen(
            uiState = PlayUiState(
                isLoading = false,
                phase = PlayUiState.Phase.QUESTION,
                questionCount = 8,
                question = previewQuestion(isClosed = false),
                selectedChoiceIndex = 2,
                remainingSeconds = 18,
                myParticipantId = 9001,
                myNickname = "민지",
                isGuest = false
            ),
            onAction = {},
            onClickLeave = {}
        )
    }
}

// QUESTION_ENDED 정답 공개 — 정답은 이 시점에만 온다 (규칙 §13)
@PassmatePreview
@Composable
private fun PlayContentScreenRevealPreview() {
    PassmateTheme {
        PlayContentScreen(
            uiState = PlayUiState(
                isLoading = false,
                phase = PlayUiState.Phase.QUESTION,
                questionCount = 8,
                question = previewQuestion(isClosed = true),
                remainingSeconds = 0,
                hasSubmitted = true,
                myAnswerResult = AnswerResult(
                    correct = true,
                    baseScore = 100.0,
                    speedBonus = 20.0,
                    earnedScore = 120.0,
                    totalScore = 480.0,
                    rank = 2,
                    rankDelta = 1,
                    isProvisional = false
                ),
                reveal = PlayUiState.Reveal(
                    answer = "3",
                    explanation = "이웃한 두 항의 차 5-2=3, 8-5=3으로 공차는 3이에요.",
                    correctAnswererCount = 5
                ),
                totalScore = 480.0,
                rank = 2,
                myParticipantId = 9001,
                myNickname = "민지"
            ),
            onAction = {},
            onClickLeave = {}
        )
    }
}

// GAME_FINISHED 직후 최종 순위 — Result 라우트 전환 전 화면
@PassmatePreview
@Composable
private fun PlayContentScreenFinishedPreview() {
    PassmateTheme {
        PlayContentScreen(
            uiState = PlayUiState(
                isLoading = false,
                phase = PlayUiState.Phase.FINISHED,
                questionCount = 8,
                totalScore = 990.0,
                myCorrectCount = 6,
                rank = 3,
                finalRanking = listOf(
                    RankEntry(rank = 1, participantId = 9002, nickname = "준영", avatarId = 2, total = 1240.0),
                    RankEntry(rank = 2, participantId = 9003, nickname = "혜림", avatarId = 5, total = 1180.0),
                    RankEntry(rank = 3, participantId = 9001, nickname = "민지", avatarId = 1, total = 990.0)
                ),
                myParticipantId = 9001,
                myNickname = "민지",
                isGuest = false
            ),
            onAction = {},
            onClickLeave = {}
        )
    }
}
