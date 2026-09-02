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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateBackButton
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.report.domain.model.ReportQuestion
import org.sesacteamproject.passmate.report.domain.model.ReportStudent
import org.sesacteamproject.passmate.report.domain.model.RoomReport
import org.sesacteamproject.passmate.report.domain.model.RoomReportSummary
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.session.domain.model.QuestionType
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme
import org.sesacteamproject.passmate.ui.result.rememberReportSharer

// Figma "UI 디자인 v6" M-14(432:5366) — 방 리포트: 요약 카드+개요/문항별/학생별 탭+내보내기(텍스트 공유)
@Composable
fun RoomReportScreen(
    roomId: Long,
    viewModel: RoomReportViewModel = koinScreenViewModel(),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val shareReport = rememberReportSharer()

    LaunchedEffect(roomId) {
        viewModel.onAction(RoomReportAction.Enter(roomId))
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is RoomReportEvent.RequireSignIn -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToRoomReport(roomId))
                )
                is RoomReportEvent.ShareReport -> shareReport(event.summary)
            }
        }
    }
    RoomReportContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onRetry = { viewModel.onAction(RoomReportAction.Retry(roomId)) },
        onClickBack = { onNavigate(NavigationAction.NavigateBack) }
    )
}

@Composable
private fun RoomReportContentScreen(
    uiState: RoomReportUiState,
    onAction: (RoomReportAction) -> Unit,
    onRetry: () -> Unit,
    onClickBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
    ) {
        when {
            uiState.isLoading -> LoadingBox()
            uiState.loadFailed || uiState.report == null -> ErrorBox(onRetry = onRetry)
            else -> LoadedReport(
                report = uiState.report,
                selectedTab = uiState.selectedTab,
                onAction = onAction,
                onClickBack = onClickBack
            )
        }
    }
}

@Composable
private fun LoadedReport(
    report: RoomReport,
    selectedTab: ReportTab,
    onAction: (RoomReportAction) -> Unit,
    onClickBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 60.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PassmateBackButton(onClick = onClickBack)
            Text(
                text = "방 리포트",
                color = PassmateColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.36).sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            Text(
                text = "내보내기",
                color = PassmateColors.PrimaryDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp,
                modifier = Modifier
                    .clickable { onAction(RoomReportAction.ClickExport) }
                    .padding(4.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = report.roomTitle,
                color = PassmateColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.44).sp
            )
            Text(
                text = reportSubtitle(report),
                color = PassmateColors.TextSecondary,
                fontSize = 13.sp,
                letterSpacing = (-0.26).sp
            )
        }
        StatCardsGrid(report = report)
        TabChips(
            selectedTab = selectedTab,
            onSelect = { onAction(RoomReportAction.SelectTab(it)) }
        )
        when (selectedTab) {
            ReportTab.OVERVIEW -> OverviewTab(report = report)
            ReportTab.QUESTIONS -> QuestionsTab(questions = report.questions)
            ReportTab.STUDENTS -> StudentsTab(students = report.students)
        }
    }
}

@Composable
private fun StatCardsGrid(report: RoomReport) {
    val summary = report.summary

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                value = summary.avgAccuracyPercent?.let { "${it}%" } ?: "—",
                label = "평균 정답률",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "${summary.studentCount}명",
                label = "참가 학생",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                value = "${summary.questionCount}개",
                label = "문항",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "${summary.aiAnalysisCount}건",
                label = "AI 분석",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(14.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            color = PassmateColors.PrimaryDeep,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        )
        Text(
            text = label,
            color = PassmateColors.TextSecondary,
            fontSize = 13.sp,
            letterSpacing = (-0.26).sp
        )
    }
}

@Composable
private fun TabChips(
    selectedTab: ReportTab,
    onSelect: (ReportTab) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReportTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val bg = if (isSelected) PassmateColors.RatingTagSelectedBg else PassmateColors.FieldGray
            val fg = if (isSelected) PassmateColors.RatingTagSelectedText else PassmateColors.TextSecondary

            Text(
                text = tab.label,
                color = fg,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.26).sp,
                modifier = Modifier
                    .background(bg, CircleShape)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun OverviewTab(report: RoomReport) {
    val summary = report.summary
    val choiceCount = report.questions.count { it.type == QuestionType.MULTIPLE_CHOICE }
    val oxCount = report.questions.count { it.type == QuestionType.OX }
    val essayCount = report.questions.count { it.type == QuestionType.ESSAY }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OverviewRow(label = "평균 점수", value = summary.avgScore?.let { "${it.toLong()}점" } ?: "—")
        OverviewRow(label = "최고 점수", value = summary.topScore?.let { "${it.toLong()}점" } ?: "—")
        OverviewRow(label = "문항 구성", value = "객관식 $choiceCount · OX $oxCount · 서술형 $essayCount")
        OverviewRow(label = "AI 분석", value = "${summary.aiAnalysisCount}건")
    }
}

@Composable
private fun OverviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            letterSpacing = (-0.28).sp
        )
        Text(
            text = value,
            color = PassmateColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun QuestionsTab(questions: List<ReportQuestion>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
    ) {
        questions.forEachIndexed { index, question ->
            if (index > 0) {
                Divider(color = PassmateColors.Border, thickness = 1.dp)
            }
            QuestionRow(question = question)
        }
        if (questions.isEmpty()) {
            EmptyTabText(text = "문항 통계가 없어요")
        }
    }
}

@Composable
private fun QuestionRow(question: ReportQuestion) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(PassmateColors.FieldGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Q${question.questionNo}",
                    color = PassmateColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
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
            question.aiFeedbackCount?.let { count ->
                Text(
                    text = "AI 분석 ${count}건",
                    color = PassmateColors.PrimaryDeep,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.24).sp
                )
            }
            if (question.type == QuestionType.ESSAY && question.aiFeedbackCount == null) {
                Text(
                    text = "서술형",
                    color = PassmateColors.PrimaryDeep,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.24).sp
                )
            }
            Text(
                text = question.accuracyPercent?.let { "${it}%" } ?: "—",
                color = PassmateColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.26).sp
            )
        }
        AccuracyBar(percent = question.accuracyPercent)
    }
}

@Composable
private fun AccuracyBar(percent: Int?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(PassmateColors.FieldGray, CircleShape)
    ) {
        val fraction = (percent ?: 0).coerceIn(0, 100) / 100f

        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .background(PassmateColors.Primary, CircleShape)
            )
        }
    }
}

@Composable
private fun StudentsTab(students: List<ReportStudent>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
    ) {
        students.forEachIndexed { index, student ->
            if (index > 0) {
                Divider(color = PassmateColors.Border, thickness = 1.dp)
            }
            StudentRow(student = student)
        }
        if (students.isEmpty()) {
            EmptyTabText(text = "참가 학생이 없어요")
        }
    }
}

@Composable
private fun StudentRow(student: ReportStudent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(PassmateColors.FieldGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = student.rank?.toString() ?: "-",
                color = PassmateColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = student.nickname,
            color = PassmateColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            modifier = Modifier.weight(1f)
        )
        if (student.isGuest) {
            Text(
                text = "게스트",
                color = PassmateColors.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(PassmateColors.FieldGray, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Text(
            text = "정답 ${student.correctCount}",
            color = PassmateColors.TextSecondary,
            fontSize = 13.sp,
            letterSpacing = (-0.26).sp
        )
        Text(
            text = "${student.totalScore.toLong()}점",
            color = PassmateColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun EmptyTabText(text: String) {
    Text(
        text = text,
        color = PassmateColors.TextSecondary,
        fontSize = 14.sp,
        letterSpacing = (-0.28).sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        textAlign = TextAlign.Center
    )
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

private fun reportSubtitle(report: RoomReport): String {
    val parts = mutableListOf<String>()

    report.dateLabel?.let { parts.add("$it 진행") }
    parts.add("종료된 방")
    parts.add("PIN ${report.pin.chunked(3).joinToString(" ")}")

    return parts.joinToString(" · ")
}

// --- Preview ---

private val previewRoomReport = RoomReport(
    roomTitle = "8월 4주차 Spring 스터디",
    pin = "482913",
    status = RoomStatus.FINISHED,
    dateLabel = "2026.08.28",
    summary = RoomReportSummary(
        avgAccuracyPercent = 74,
        studentCount = 24,
        questionCount = 8,
        aiAnalysisCount = 12,
        avgScore = 820.0,
        topScore = 1240.0
    ),
    questions = listOf(
        ReportQuestion(questionId = 1, questionNo = 1, title = "등차수열의 공차", type = QuestionType.MULTIPLE_CHOICE, accuracyPercent = 92, aiFeedbackCount = null),
        ReportQuestion(questionId = 2, questionNo = 2, title = "이차함수의 최댓값 OX", type = QuestionType.OX, accuracyPercent = 58, aiFeedbackCount = null),
        // 서술형 미채점 — accuracyPercent가 null이면 "—"로 렌더링한다
        ReportQuestion(questionId = 3, questionNo = 3, title = "이차방정식의 판별식 활용 서술형", type = QuestionType.ESSAY, accuracyPercent = null, aiFeedbackCount = 12)
    ),
    students = listOf(
        ReportStudent(participantId = 9002, nickname = "준영", rank = 1, totalScore = 1240.0, correctCount = 8, isGuest = false),
        ReportStudent(participantId = 9003, nickname = "혜림", rank = 2, totalScore = 1180.0, correctCount = 7, isGuest = false),
        ReportStudent(participantId = 9001, nickname = "민지", rank = 3, totalScore = 990.0, correctCount = 6, isGuest = true)
    )
)

@PassmatePreview
@Composable
private fun RoomReportContentScreenQuestionsPreview() {
    PassmateTheme {
        RoomReportContentScreen(
            uiState = RoomReportUiState(isLoading = false, report = previewRoomReport, selectedTab = ReportTab.QUESTIONS),
            onAction = {},
            onRetry = {},
            onClickBack = {}
        )
    }
}

@PassmatePreview
@Composable
private fun RoomReportContentScreenOverviewPreview() {
    PassmateTheme {
        RoomReportContentScreen(
            uiState = RoomReportUiState(isLoading = false, report = previewRoomReport, selectedTab = ReportTab.OVERVIEW),
            onAction = {},
            onRetry = {},
            onClickBack = {}
        )
    }
}

@PassmatePreview
@Composable
private fun RoomReportContentScreenStudentsPreview() {
    PassmateTheme {
        RoomReportContentScreen(
            uiState = RoomReportUiState(isLoading = false, report = previewRoomReport, selectedTab = ReportTab.STUDENTS),
            onAction = {},
            onRetry = {},
            onClickBack = {}
        )
    }
}

@PassmatePreview
@Composable
private fun RoomReportContentScreenFailedPreview() {
    PassmateTheme {
        RoomReportContentScreen(
            uiState = RoomReportUiState(isLoading = false, loadFailed = true),
            onAction = {},
            onRetry = {},
            onClickBack = {}
        )
    }
}
