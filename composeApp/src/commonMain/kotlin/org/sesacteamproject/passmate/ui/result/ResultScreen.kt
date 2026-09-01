package org.sesacteamproject.passmate.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateCard
import org.sesacteamproject.passmate.component.PassyMascot
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.report.domain.model.AnswerVerdict
import org.sesacteamproject.passmate.report.domain.model.QuestionResult
import org.sesacteamproject.passmate.report.domain.model.SessionResult
import org.sesacteamproject.passmate.theme.PassmateColors

// Figma "UI 디자인 v6" M-06(349:9395) — 정답 링·보완 주제·문항 리스트·AI 분석 카드 + 내보내기·평가 (T062·T056·T080)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: ResultViewModel = koinScreenViewModel(),
    roomId: Long,
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareReport = rememberReportSharer()
    val ratingSheetState = rememberModalBottomSheetState()

    LaunchedEffect(roomId) {
        viewModel.onAction(ResultAction.Enter(roomId))
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is ResultEvent.ShareReport -> shareReport(event.summary)
                is ResultEvent.NavigateToSignup -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToResult(roomId))
                )
                is ResultEvent.RatingSubmitted -> snackbarHostState.showSnackbar(event.message)
                is ResultEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        ResultContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onClickHome = { onNavigate(NavigationAction.NavigateToHome) }
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    // 평가 시트는 컨테이너가 소유 (규칙 §11-1) — 오버레이/모달은 콘텐츠 뷰에 두지 않는다
    if (uiState.isRatingSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(ResultAction.DismissRatingSheet) },
            sheetState = ratingSheetState,
            containerColor = PassmateColors.Surface
        ) {
            RatingSection(
                uiState = uiState,
                onAction = viewModel::onAction
            )
        }
    }
}

@Composable
private fun ResultContentScreen(
    uiState: ResultUiState,
    onAction: (ResultAction) -> Unit,
    onClickHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
    ) {
        when {
            uiState.isLoading -> LoadingBox()
            uiState.loadFailed || uiState.result == null -> ErrorBox(onRetry = { onAction(ResultAction.Retry) })
            else -> LoadedResult(
                uiState = uiState,
                result = uiState.result,
                onAction = onAction,
                onClickHome = onClickHome
            )
        }
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
            text = "리포트를 불러오지 못했어요",
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

@Composable
private fun ColumnScope.LoadedResult(
    uiState: ResultUiState,
    result: SessionResult,
    onAction: (ResultAction) -> Unit,
    onClickHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 60.dp, end = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ReportHeaderCard(result = result)
        WeakTopicsRow(topics = uiState.report?.weakTopics.orEmpty())
        QuestionList(
            questions = result.questions,
            selectedQuestionNo = uiState.selectedQuestionNo,
            onSelect = { onAction(ResultAction.SelectQuestion(it)) }
        )
        val selected = result.questions.firstOrNull { it.questionNo == uiState.selectedQuestionNo }

        if (selected != null && (selected.aiFeedback != null || selected.hostReview != null)) {
            FeedbackSection(question = selected)
        }
        // 선생님 평가 진입 (T080) — 평가 자격이 있고 아직 안 했을 때만
        if (result.canRate && !uiState.hasRated) {
            RateEntryButton(onClick = { onAction(ResultAction.OpenRatingSheet) })
        }
        // 게스트 가입 유도 (T075) — 회원에게는 표시하지 않는다
        if (result.isGuest) {
            SignupPromptSection(onClickSignup = { onAction(ResultAction.ClickSignup) })
        }
    }
    ExportButton(onClick = { onAction(ResultAction.ClickExport) })
}

@Composable
private fun ReportHeaderCard(result: SessionResult) {
    PassmateCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CorrectRing(
                correctCount = result.correctCount,
                questionCount = result.questionCount
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "내 리포트",
                    color = PassmateColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.48).sp
                )
                Text(
                    text = headerSubtitle(result),
                    color = PassmateColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.28).sp
                )
            }
            PassyMascot(modifier = Modifier.size(width = 52.dp, height = 57.dp))
        }
    }
}

@Composable
private fun CorrectRing(
    correctCount: Int,
    questionCount: Int
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .border(6.dp, PassmateColors.Primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$correctCount/$questionCount",
                color = PassmateColors.PrimaryDeep,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )
            Text(
                text = "정답",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
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
                WeakTopicChip(topic = topic)
            }
        }
    }
}

@Composable
private fun WeakTopicChip(topic: String) {
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

@Composable
private fun QuestionList(
    questions: List<QuestionResult>,
    selectedQuestionNo: Int?,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        questions.forEach { question ->
            QuestionRow(
                question = question,
                isSelected = question.questionNo == selectedQuestionNo,
                onClick = { onSelect(question.questionNo) }
            )
        }
    }
}

@Composable
private fun QuestionRow(
    question: QuestionResult,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PassmateColors.Primary else PassmateColors.Border

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 24.dp)
                .background(PassmateColors.FieldGray, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Q${question.questionNo}",
                color = PassmateColors.PrimaryDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
        Text(
            text = question.title,
            color = PassmateColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            modifier = Modifier.weight(1f)
        )
        VerdictChip(verdict = question.verdict)
        Text(
            text = "›",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun VerdictChip(verdict: AnswerVerdict) {
    val (background, textColor, label) = verdictStyle(verdict)

    Box(
        modifier = Modifier
            .background(background, CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

private fun verdictStyle(verdict: AnswerVerdict): Triple<Color, Color, String> {
    return when (verdict) {
        AnswerVerdict.CORRECT -> Triple(PassmateColors.ChipGreen, PassmateColors.ChipGreenText, "정답")
        AnswerVerdict.WRONG -> Triple(PassmateColors.WrongPink, PassmateColors.WrongPinkText, "오답")
        AnswerVerdict.AI_ANALYZED -> Triple(PassmateColors.ChipGold, PassmateColors.ChipGoldText, "AI 분석")
        AnswerVerdict.AI_PENDING -> Triple(PassmateColors.ChipGold, PassmateColors.ChipGoldText, "분석 중")
        AnswerVerdict.UNGRADED -> Triple(PassmateColors.FieldGray, PassmateColors.TextSecondary, "미채점")
    }
}

@Composable
private fun RateEntryButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(PassmateColors.BackgroundMint, RoundedCornerShape(16.dp))
            .border(1.dp, PassmateColors.Primary, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "★ 선생님 평가하기",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun ExportButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
            .height(52.dp)
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "리포트 내보내기",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

private fun headerSubtitle(result: SessionResult): String {
    val rankPart = result.rank?.let { "${it}위 · " } ?: ""

    return "${result.roomTitle} · $rankPart${result.totalScore.toLong()}점"
}
