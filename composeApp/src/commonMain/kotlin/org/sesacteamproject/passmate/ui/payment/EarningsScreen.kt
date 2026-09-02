package org.sesacteamproject.passmate.ui.payment

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateBackButton
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.payment.domain.model.Earnings
import org.sesacteamproject.passmate.payment.domain.model.NextPayout
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccountSummary
import org.sesacteamproject.passmate.payment.domain.model.SettlementItem
import org.sesacteamproject.passmate.payment.domain.model.SettlementStatus
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// Figma "UI 디자인 v6" M-T4(349:10199) — 정산: 이번 달 수익(80%)·다음 지급·결제/정산 내역+계좌 관리(M-12-3 시트)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(onNavigate: (NavigationAction) -> Unit) {
    val viewModel: EarningsViewModel = koinScreenViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val accountSheetState = rememberModalBottomSheetState()
    var isAccountSheetVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onAction(EarningsAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is EarningsEvent.RequireSignIn -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToEarnings)
                )
                is EarningsEvent.OpenAccountSheet -> isAccountSheetVisible = true
                is EarningsEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        EarningsContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onClickBack = { onNavigate(NavigationAction.NavigateBack) }
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    if (isAccountSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { isAccountSheetVisible = false },
            sheetState = accountSheetState,
            containerColor = PassmateColors.Surface
        ) {
            SettlementAccountSheet(
                onSaved = {
                    isAccountSheetVisible = false
                    viewModel.onAction(EarningsAction.AccountSaved)
                },
                onNotice = { message ->
                    viewModel.onAction(EarningsAction.Notice(message))
                },
                onClose = { isAccountSheetVisible = false }
            )
        }
    }
}

@Composable
private fun EarningsContentScreen(
    uiState: EarningsUiState,
    onAction: (EarningsAction) -> Unit,
    onClickBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
    ) {
        when {
            uiState.isLoading -> LoadingBox()
            uiState.loadFailed || uiState.earnings == null -> ErrorBox(onRetry = { onAction(EarningsAction.Retry) })
            else -> LoadedEarnings(
                earnings = uiState.earnings,
                uiState = uiState,
                onAction = onAction,
                onClickBack = onClickBack
            )
        }
    }
}

@Composable
private fun LoadedEarnings(
    earnings: Earnings,
    uiState: EarningsUiState,
    onAction: (EarningsAction) -> Unit,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PassmateBackButton(onClick = onClickBack)
                Text(
                    text = "정산",
                    color = PassmateColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.48).sp
                )
            }
            Text(
                text = "계좌 관리",
                color = PassmateColors.PrimaryDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp,
                modifier = Modifier
                    .clickable { onAction(EarningsAction.ClickManageAccount) }
                    .padding(4.dp)
            )
        }
        SummaryCard(earnings = earnings)
        Text(
            text = "결제 · 정산 내역",
            color = PassmateColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.36).sp
        )
        if (uiState.items.isEmpty()) {
            EmptyItems()
        }
        uiState.items.forEach { item ->
            SettlementRow(item = item)
        }
        if (uiState.nextCursor != null) {
            LoadMoreRow(
                isLoadingMore = uiState.isLoadingMore,
                onClick = { onAction(EarningsAction.LoadMore) }
            )
        }
        earnings.account?.let { account ->
            AccountRow(
                bankName = account.bankName,
                maskedNumber = account.maskedNumber,
                payoutNote = account.payoutNote,
                onClick = { onAction(EarningsAction.ClickManageAccount) }
            )
        }
    }
}

@Composable
private fun SummaryCard(earnings: Earnings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.BackgroundMint, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "이번 달 수익 (선생님 ${earnings.hostSharePercent}%)",
            color = PassmateColors.PrimaryDeep,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.26).sp
        )
        Text(
            text = "₩ ${formatAmount(earnings.monthlyTotal)}",
            color = PassmateColors.PrimaryDeep,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp
        )
        Text(
            text = summaryLine(earnings),
            color = PassmateColors.TextSecondary,
            fontSize = 13.sp,
            letterSpacing = (-0.26).sp
        )
    }
}

@Composable
private fun SettlementRow(item: SettlementItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(PassmateColors.BackgroundMint, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.dateLabel,
                color = PassmateColors.PrimaryDeep,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = item.roomTitle,
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.28).sp
            )
            Text(
                text = "${item.participantCount}명 · 참가비 ₩${formatAmount(item.entryFeeTotal)} · 수수료 ₩${formatAmount(item.feeAmount)}",
                color = PassmateColors.TextSecondary,
                fontSize = 12.sp,
                letterSpacing = (-0.24).sp
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "₩${formatAmount(item.payoutAmount)}",
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.28).sp
            )
            StatusChip(status = item.status)
        }
    }
}

@Composable
private fun StatusChip(status: SettlementStatus) {
    val (label, bg, fg) = when (status) {
        SettlementStatus.SCHEDULED -> Triple("정산 예정", PassmateColors.ChipGold, PassmateColors.ChipGoldText)
        SettlementStatus.PAID -> Triple("지급 완료", PassmateColors.ChipGreen, PassmateColors.ChipGreenText)
        SettlementStatus.HELD -> Triple("보류", PassmateColors.WrongPink, PassmateColors.WrongPinkText)
        SettlementStatus.UNKNOWN -> Triple("확인 중", PassmateColors.FieldGray, PassmateColors.TextSecondary)
    }

    Text(
        text = label,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun AccountRow(
    bankName: String,
    maskedNumber: String,
    payoutNote: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(PassmateColors.BackgroundMint, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bankName.take(1),
                color = PassmateColors.PrimaryDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "$bankName $maskedNumber",
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
            payoutNote?.let { note ->
                Text(
                    text = note,
                    color = PassmateColors.TextTertiary,
                    fontSize = 12.sp,
                    letterSpacing = (-0.24).sp
                )
            }
        }
    }
}

@Composable
private fun LoadMoreRow(
    isLoadingMore: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(14.dp))
            .clickable(enabled = !isLoadingMore, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoadingMore) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = PassmateColors.Primary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "전체 보기",
                color = PassmateColors.PrimaryDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
    }
}

@Composable
private fun EmptyItems() {
    Text(
        text = "아직 정산 내역이 없어요 · 유료 방을 열면 여기에 쌓여요",
        color = PassmateColors.TextSecondary,
        fontSize = 14.sp,
        letterSpacing = (-0.28).sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
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
            text = "정산 정보를 불러오지 못했어요",
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

private fun summaryLine(earnings: Earnings): String {
    val parts = mutableListOf<String>()

    earnings.nextPayout?.let { parts.add("다음 지급 ${it.dateLabel} · ₩${formatAmount(it.amount)} 예정") }
    parts.add("유료 방 ${earnings.paidRoomCount}회")
    parts.add("${earnings.studentCount}명")

    return parts.joinToString(" · ")
}

private fun formatAmount(amount: Long): String {
    return amount.toString().reversed().chunked(3).joinToString(",").reversed()
}

// --- Preview ---

private val previewSettlementItems = listOf(
    SettlementItem(settlementId = 8001, dateLabel = "2026.08.28", roomTitle = "8월 4주차 Spring 스터디", participantCount = 24, entryFeeTotal = 12000L, feeAmount = 2400L, payoutAmount = 9600L, status = SettlementStatus.SCHEDULED),
    SettlementItem(settlementId = 8002, dateLabel = "2026.08.14", roomTitle = "확률과 통계 총정리", participantCount = 18, entryFeeTotal = 9000L, feeAmount = 1800L, payoutAmount = 7200L, status = SettlementStatus.PAID),
    SettlementItem(settlementId = 8003, dateLabel = "2026.08.02", roomTitle = "함수의 극한 퀴즈", participantCount = 11, entryFeeTotal = 5500L, feeAmount = 1100L, payoutAmount = 4400L, status = SettlementStatus.HELD)
)

@PassmatePreview
@Composable
private fun EarningsContentScreenPreview() {
    PassmateTheme {
        EarningsContentScreen(
            uiState = EarningsUiState(
                isLoading = false,
                earnings = Earnings(
                    monthlyTotal = 64000L,
                    hostSharePercent = 80,
                    nextPayout = NextPayout(dateLabel = "9/5", amount = 9600L),
                    paidRoomCount = 3,
                    studentCount = 53,
                    items = previewSettlementItems,
                    nextCursor = null,
                    hasNext = false,
                    account = SettlementAccountSummary(bankName = "국민", maskedNumber = "***-***-4821", payoutNote = "매월 5일 지급")
                ),
                items = previewSettlementItems
            ),
            onAction = {},
            onClickBack = {}
        )
    }
}

// 정산 내역 없음 — 계좌 미등록
@PassmatePreview
@Composable
private fun EarningsContentScreenEmptyPreview() {
    PassmateTheme {
        EarningsContentScreen(
            uiState = EarningsUiState(
                isLoading = false,
                earnings = Earnings(
                    monthlyTotal = 0L,
                    hostSharePercent = 80,
                    nextPayout = null,
                    paidRoomCount = 0,
                    studentCount = 0,
                    items = emptyList(),
                    nextCursor = null,
                    hasNext = false,
                    account = null
                ),
                items = emptyList()
            ),
            onAction = {},
            onClickBack = {}
        )
    }
}

@PassmatePreview
@Composable
private fun EarningsContentScreenFailedPreview() {
    PassmateTheme {
        EarningsContentScreen(
            uiState = EarningsUiState(isLoading = false, loadFailed = true),
            onAction = {},
            onClickBack = {}
        )
    }
}
