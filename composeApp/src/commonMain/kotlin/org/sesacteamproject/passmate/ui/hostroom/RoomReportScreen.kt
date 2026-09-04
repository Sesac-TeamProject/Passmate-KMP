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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

private const val MOST_MISSED_LIMIT = 3

private const val TOP_RANK_LIMIT = 3

private val ACCURACY_BAND_LABELS = listOf("0~40%", "41~60%", "61~80%", "81~100%")

private val ACCURACY_BAND_COLORS = listOf(
    PassmateColors.WrongPink,
    PassmateColors.AccuracyBandMid,
    PassmateColors.RatingTagSelectedBg,
    PassmateColors.Primary
)

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
            // 화면 배경은 시스템 바 뒤까지 깔고 콘텐츠만 안쪽으로 들인다 (iOS의 background(...).ignoresSafeArea() 미러).
            // 탭바 없는 push 화면은 Scaffold가 하단 인셋을 주지 않으므로(contentWindowInsets=0) 여기서 직접 준다
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when {
            uiState.isLoading -> LoadingBox()
            uiState.loadFailed || uiState.report == null -> ErrorBox(onRetry = onRetry)
            else -> LoadedReport(
                report = uiState.report,
                selectedTab = uiState.selectedTab,
                studentSort = uiState.studentSort,
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
    studentSort: StudentSort,
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
            ReportTab.OVERVIEW -> OverviewTab(
                report = report,
                onClickQuestionsLink = { onAction(RoomReportAction.SelectTab(ReportTab.QUESTIONS)) }
            )
            ReportTab.QUESTIONS -> QuestionsTab(questions = report.questions)
            ReportTab.STUDENTS -> StudentsTab(
                report = report,
                studentSort = studentSort,
                onSelectSort = { onAction(RoomReportAction.SelectStudentSort(it)) }
            )
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

// 개요 탭 — 정답률 분포 · 많이 틀린 문항 TOP 3 · 요약 (v6 M-14 개요)
// 시안의 "AI 총평" 카드는 계약(RoomReportResponse)에 총평 텍스트 필드가 없어 제외하고, 같은 자리에 요약 카드를 둔다
@Composable
private fun OverviewTab(
    report: RoomReport,
    onClickQuestionsLink: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AccuracyDistributionCard(report = report)
        MostMissedQuestionsCard(
            questions = report.questions,
            onClickQuestionsLink = onClickQuestionsLink
        )
        OverviewSummaryCard(report = report)
    }
}

// 정답률 분포 — 서버가 준 학생별 정답 수를 4구간으로 묶어 보여준다 (점수·정오 판정은 하지 않는다)
@Composable
private fun AccuracyDistributionCard(report: RoomReport) {
    val bands = accuracyBands(students = report.students, questionCount = report.summary.questionCount)
    val bandTotal = bands.sumOf { it.count }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CardHeaderRow(title = "정답률 분포") {
            Text(
                text = "학생 ${report.summary.studentCount}명",
                color = PassmateColors.TextTertiary,
                fontSize = 12.sp,
                letterSpacing = (-0.12).sp
            )
        }
        if (bandTotal == 0) {
            EmptyTabText(text = "정답률 분포를 계산할 수 없어요")
        } else {
            bands.forEach { band ->
                AccuracyBandRow(band = band, total = bandTotal)
            }
        }
    }
}

@Composable
private fun AccuracyBandRow(band: AccuracyBand, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = band.label,
            color = PassmateColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.12).sp,
            modifier = Modifier.width(60.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .background(PassmateColors.FieldGray, CircleShape)
        ) {
            val fraction = band.count.toFloat() / total.toFloat()

            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(10.dp)
                        .background(band.color, CircleShape)
                )
            }
        }
        Text(
            text = "${band.count}명",
            color = PassmateColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.12).sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp)
        )
    }
}

// 많이 틀린 문항 TOP 3 — 서버가 준 정답률의 여집합을 오답률로 표시한다 (미채점 서술형은 제외)
@Composable
private fun MostMissedQuestionsCard(
    questions: List<ReportQuestion>,
    onClickQuestionsLink: () -> Unit
) {
    val mostMissed = questions
        .filter { it.accuracyPercent != null }
        .sortedBy { it.accuracyPercent }
        .take(MOST_MISSED_LIMIT)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CardHeaderRow(title = "많이 틀린 문항 TOP $MOST_MISSED_LIMIT") {
            Text(
                text = "문항별 ›",
                color = PassmateColors.PrimaryDeep,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.12).sp,
                modifier = Modifier.clickable(onClick = onClickQuestionsLink)
            )
        }
        if (mostMissed.isEmpty()) {
            EmptyTabText(text = "채점된 문항이 아직 없어요")
        } else {
            mostMissed.forEach { question ->
                MostMissedRow(question = question)
            }
        }
    }
}

@Composable
private fun MostMissedRow(question: ReportQuestion) {
    val accuracyPercent = question.accuracyPercent ?: 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 22.dp)
                .background(PassmateColors.FieldGray, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Q${question.questionNo}",
                color = PassmateColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.12).sp
            )
        }
        Text(
            text = question.title,
            color = PassmateColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.13).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "오답 ${100 - accuracyPercent}%",
            color = PassmateColors.WrongPinkText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.12).sp
        )
    }
}

@Composable
private fun OverviewSummaryCard(report: RoomReport) {
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
        OverviewRow(label = "평균 점수", value = summary.avgScore?.let { "${formatScore(it)}점" } ?: "—")
        OverviewRow(label = "최고 점수", value = summary.topScore?.let { "${formatScore(it)}점" } ?: "—")
        OverviewRow(label = "문항 구성", value = "객관식 $choiceCount · OX $oxCount · 서술형 $essayCount")
        OverviewRow(label = "AI 분석", value = "${summary.aiAnalysisCount}건")
    }
}

@Composable
private fun CardHeaderRow(
    title: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = PassmateColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.15).sp
        )
        trailing()
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

// 학생별 탭 — 학생 수·정렬 칩 + 순위/정답 수/점수 목록 (v6 M-14 학생별)
// 시안의 "제출 N" · "미제출 N명" 카드는 계약에 제출 여부 필드가 없어 제외한다
@Composable
private fun StudentsTab(
    report: RoomReport,
    studentSort: StudentSort,
    onSelectSort: (StudentSort) -> Unit
) {
    val students = sortedStudents(students = report.students, sort = studentSort)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "학생 ${report.summary.studentCount}명",
                color = PassmateColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.13).sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudentSort.entries.forEach { sort ->
                    StudentSortChip(
                        sort = sort,
                        isSelected = sort == studentSort,
                        onClick = { onSelectSort(sort) }
                    )
                }
            }
        }
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
                StudentRow(student = student, questionCount = report.summary.questionCount)
            }
            if (students.isEmpty()) {
                EmptyTabText(text = "참가 학생이 없어요")
            }
        }
    }
}

@Composable
private fun StudentSortChip(
    sort: StudentSort,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val background = if (isSelected) PassmateColors.FieldGray else PassmateColors.Surface
    val textColor = if (isSelected) PassmateColors.TextSecondary else PassmateColors.TextTertiary
    val borderColor = if (isSelected) PassmateColors.FieldGray else PassmateColors.Border

    Box(
        modifier = Modifier
            .size(width = 66.dp, height = 30.dp)
            .border(1.dp, borderColor, CircleShape)
            .background(background, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = sort.label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = (-0.12).sp
        )
    }
}

@Composable
private fun StudentRow(
    student: ReportStudent,
    questionCount: Int
) {
    val rank = student.rank
    val isTopRank = rank != null && rank <= TOP_RANK_LIMIT
    val rankBackground = if (isTopRank) PassmateColors.BackgroundMint else PassmateColors.FieldGray
    val rankTextColor = if (isTopRank) PassmateColors.PrimaryDeep else PassmateColors.TextTertiary
    val scoreColor = if (isTopRank) PassmateColors.Primary else PassmateColors.TextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(rankBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank?.toString() ?: "-",
                color = rankTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.12).sp
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = student.nickname,
                    color = PassmateColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.14).sp
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
            }
            Text(
                text = "정답 ${student.correctCount}/$questionCount",
                color = PassmateColors.TextSecondary,
                fontSize = 12.sp,
                letterSpacing = (-0.12).sp
            )
        }
        Text(
            text = "${formatScore(student.totalScore)}점",
            color = scoreColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.13).sp
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

// 정답률 구간 1칸 — 라벨·인원·막대 색 (v6 M-14 개요 "정답률 분포")
private data class AccuracyBand(
    val label: String,
    val count: Int,
    val color: Color
)

private fun accuracyBands(students: List<ReportStudent>, questionCount: Int): List<AccuracyBand> {
    val counts = IntArray(ACCURACY_BAND_LABELS.size)

    if (questionCount > 0) {
        students.forEach { student ->
            val percent = student.correctCount * 100 / questionCount
            val index = when {
                percent <= 40 -> 0
                percent <= 60 -> 1
                percent <= 80 -> 2
                else -> 3
            }

            counts[index] = counts[index] + 1
        }
    }

    return ACCURACY_BAND_LABELS.mapIndexed { index, label ->
        AccuracyBand(label = label, count = counts[index], color = ACCURACY_BAND_COLORS[index])
    }
}

private fun sortedStudents(students: List<ReportStudent>, sort: StudentSort): List<ReportStudent> {
    return when (sort) {
        // 순위는 서버 값 — 점수순은 서버 순위를 그대로 따르고, 순위가 없는 학생만 점수 내림차순으로 뒤에 둔다
        StudentSort.SCORE -> students.sortedWith(
            compareBy<ReportStudent> { it.rank ?: Int.MAX_VALUE }.thenByDescending { it.totalScore }
        )
        StudentSort.NAME -> students.sortedBy { it.nickname }
    }
}

private fun formatScore(score: Double): String {
    val digits = score.toLong().toString()

    return digits.reversed().chunked(3).joinToString(",").reversed()
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
