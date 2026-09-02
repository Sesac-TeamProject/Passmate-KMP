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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateBackButton
import org.sesacteamproject.passmate.component.PassmateEmptyState
import org.sesacteamproject.passmate.component.PassmateIcon
import org.sesacteamproject.passmate.component.PassmateIcons
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.AppTab
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
                // 방 개설 진입점은 「내가 만든 방」 탭의 새 방 만들기 시트(M-13)다
                is EarningsEvent.OpenHostedRooms -> onNavigate(
                    NavigationAction.NavigateToTab(AppTab.HOSTED_ROOMS)
                )
                is EarningsEvent.OpenCoinHistory -> onNavigate(NavigationAction.NavigateToCoinHistory)
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
            uiState.loadFailed || uiState.earnings == null -> LoadFailedContent(
                onRetry = { onAction(EarningsAction.Retry) },
                onClickBack = onClickBack
            )
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
        HistorySectionHeader(onClickViewAll = { onAction(EarningsAction.ClickViewAllHistory) })
        if (uiState.items.isEmpty()) {
            // 빈 상태는 두 갈래다 — 계좌가 없으면 계좌 등록이 먼저다(정산 금액이 쌓여도 지급되지 않는다).
            // 계좌가 있으면 "정산 내역이 없어요" + 유료 방 개설 유도 (v6 M-T4 빈 상태 2종)
            if (earnings.account == null) {
                EmptyAccountUnregistered(
                    onClickRegister = { onAction(EarningsAction.ClickManageAccount) }
                )
            } else {
                EmptySettlements(
                    onClickCreateRoom = { onAction(EarningsAction.ClickCreatePaidRoom) }
                )
            }
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

// 시안 M-T4 — 섹션 제목과 "전체 보기 ›" 링크를 좌우 양끝 정렬한다
@Composable
private fun HistorySectionHeader(onClickViewAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "결제 · 정산 내역",
            color = PassmateColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.36).sp
        )
        Text(
            text = "전체 보기 ›",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            modifier = Modifier
                .clickable(onClick = onClickViewAll)
                .padding(4.dp)
        )
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
                text = "더 보기",
                color = PassmateColors.PrimaryDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
    }
}

// 빈 상태 문구 (v6 M-T4 2종) — iOS EarningsView.swift의 EmptyStateText와 1:1.
// 치수·타이포는 공통 컴포넌트 PassmateEmptyState가 갖는다
private object EmptyStateText {

    const val SETTLEMENTS_TITLE = "아직 정산 내역이 없어요"

    const val SETTLEMENTS_GUIDE = "유료 방을 열고 참가비가 모이면\n매월 5일에 정산해 드려요."

    const val SETTLEMENTS_CTA = "유료 방 만들기"

    const val ACCOUNT_TITLE = "정산 계좌를 등록해 주세요"

    const val ACCOUNT_GUIDE = "계좌가 없으면 정산 금액이 쌓여도\n지급되지 않아요."

    const val ACCOUNT_CTA = "계좌 등록하기"
}

// 빈 상태 — 정산 (v6 M-T4) — 문구·아이콘만 넘기고 배치는 공통 컴포넌트가 그린다
@Composable
private fun EmptySettlements(onClickCreateRoom: () -> Unit) {
    PassmateEmptyState(
        icon = PassmateIcons.Bookmark,
        iconTint = PassmateColors.PrimaryDeep,
        title = EmptyStateText.SETTLEMENTS_TITLE,
        guide = EmptyStateText.SETTLEMENTS_GUIDE,
        ctaLabel = EmptyStateText.SETTLEMENTS_CTA,
        onClickCta = onClickCreateRoom
    )
}

// 빈 상태 — 정산 · 계좌 미등록 (v6 M-T4) — 아이콘·문구만 위와 다르다
@Composable
private fun EmptyAccountUnregistered(onClickRegister: () -> Unit) {
    PassmateEmptyState(
        icon = PassmateIcons.AlertCircle,
        iconTint = PassmateColors.WrongPinkText,
        title = EmptyStateText.ACCOUNT_TITLE,
        guide = EmptyStateText.ACCOUNT_GUIDE,
        ctaLabel = EmptyStateText.ACCOUNT_CTA,
        onClickCta = onClickRegister
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

// 목록 불러오기 실패 문구 (v6 E-List) — iOS EarningsView.swift의 LoadFailedText와 1:1
private object LoadFailedText {

    const val HEADER_TITLE = "정산"

    const val BACK = "\u2190"

    const val TITLE = "목록을 불러오지 못했어요"

    const val GUIDE = "연결이 잠시 끊겼어요.\n정산 금액은 사라지지 않아요."

    const val RETRY = "다시 시도"

    // 정산은 마이 탭에서 push된 화면이라 뒤로가기가 곧 마이다
    const val BACK_LINK = "계좌 정보는 마이에서 확인"
}

// 목록 불러오기 실패 치수·타이포 (v6 E-List) — iOS LoadFailedSpec과 1:1.
// 다만 상단 여백은 플랫폼이 다르다: Android는 엣지투엣지 + Scaffold 인셋 0이라 화면이 직접
// 60dp를 두고(다른 8개 화면과 동일), iOS는 SwiftUI 세이프에어리어가 처리한다.
// (iOS는 lineHeight가 없어 파생값 guideLineSpacing 1개가 더 있다)
private object LoadFailedSpec {

    val HeaderPaddingHorizontal = 20.dp

    // 상태바 아래로 들어가지 않게 다른 화면과 같은 60dp — 아래 여백만 14dp
    val HeaderPaddingTop = 60.dp

    val HeaderPaddingBottom = 14.dp

    val BackFontSize = 20.sp

    val BackEndPadding = 12.dp

    val BackVerticalPadding = 4.dp

    val HeaderTitleFontSize = 15.sp

    val HeaderTitleLetterSpacing = (-0.15).sp

    val ContentPaddingHorizontal = 20.dp

    val IconCircleSize = 64.dp

    val IconSize = 30.dp

    val TitleTopPadding = 24.dp

    val TitleFontSize = 19.sp

    val TitleLetterSpacing = (-0.19).sp

    val GuideTopPadding = 8.dp

    val GuideFontSize = 14.sp

    val GuideLineHeight = 23.1.sp

    val GuideLetterSpacing = (-0.14).sp

    val RetryHeight = 52.dp

    val RetryCornerRadius = 14.dp

    val RetryFontSize = 15.sp

    val RetryLetterSpacing = (-0.15).sp

    val BackLinkTopPadding = 8.dp

    val BackLinkVerticalPadding = 10.dp

    val BackLinkFontSize = 13.sp

    val BackLinkLetterSpacing = (-0.13).sp

    val BottomSpacing = 24.dp
}

// 목록 불러오기 실패 (v6 E-List 공통 패턴) — 제목은 네 화면 공통, 둘째 줄과 버튼 뒤 링크만 화면별로 다르다.
// TODO 공통화 대상: 코인 내역·참여한 방·마이가 같은 패턴을 쓴다. 화면별 적용이 끝나면 공통 컴포넌트로 승격한다
@Composable
private fun LoadFailedContent(
    onRetry: () -> Unit,
    onClickBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LoadFailedHeader(onClickBack = onClickBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = LoadFailedSpec.ContentPaddingHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(LoadFailedSpec.IconCircleSize)
                    .background(PassmateColors.ErrorIconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PassmateIcon(
                    icon = PassmateIcons.AlertCircle,
                    contentDescription = null,
                    tint = PassmateColors.WrongPinkText,
                    modifier = Modifier.size(LoadFailedSpec.IconSize)
                )
            }
            Text(
                text = LoadFailedText.TITLE,
                color = PassmateColors.TextPrimary,
                fontSize = LoadFailedSpec.TitleFontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = LoadFailedSpec.TitleLetterSpacing,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = LoadFailedSpec.TitleTopPadding)
            )
            Text(
                text = LoadFailedText.GUIDE,
                color = PassmateColors.TextSecondary,
                fontSize = LoadFailedSpec.GuideFontSize,
                lineHeight = LoadFailedSpec.GuideLineHeight,
                letterSpacing = LoadFailedSpec.GuideLetterSpacing,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = LoadFailedSpec.GuideTopPadding)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LoadFailedSpec.ContentPaddingHorizontal)
                .height(LoadFailedSpec.RetryHeight)
                .background(
                    PassmateColors.Primary,
                    RoundedCornerShape(LoadFailedSpec.RetryCornerRadius)
                )
                .clickable(onClick = onRetry),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = LoadFailedText.RETRY,
                color = PassmateColors.Surface,
                fontSize = LoadFailedSpec.RetryFontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = LoadFailedSpec.RetryLetterSpacing
            )
        }
        Text(
            text = LoadFailedText.BACK_LINK,
            color = PassmateColors.PrimaryDeep,
            fontSize = LoadFailedSpec.BackLinkFontSize,
            fontWeight = FontWeight.Medium,
            letterSpacing = LoadFailedSpec.BackLinkLetterSpacing,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LoadFailedSpec.BackLinkTopPadding)
                .clickable(onClick = onClickBack)
                .padding(vertical = LoadFailedSpec.BackLinkVerticalPadding)
        )
        Spacer(modifier = Modifier.height(LoadFailedSpec.BottomSpacing))
    }
}

@Composable
private fun LoadFailedHeader(onClickBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = LoadFailedSpec.HeaderPaddingHorizontal,
                top = LoadFailedSpec.HeaderPaddingTop,
                end = LoadFailedSpec.HeaderPaddingHorizontal,
                bottom = LoadFailedSpec.HeaderPaddingBottom
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = LoadFailedText.BACK,
            color = PassmateColors.TextPrimary,
            fontSize = LoadFailedSpec.BackFontSize,
            modifier = Modifier
                .clickable(onClick = onClickBack)
                .padding(
                    end = LoadFailedSpec.BackEndPadding,
                    top = LoadFailedSpec.BackVerticalPadding,
                    bottom = LoadFailedSpec.BackVerticalPadding
                )
        )
        Text(
            text = LoadFailedText.HEADER_TITLE,
            color = PassmateColors.TextPrimary,
            fontSize = LoadFailedSpec.HeaderTitleFontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = LoadFailedSpec.HeaderTitleLetterSpacing
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
private fun EarningsContentScreenNoSettlementsPreview() {
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
                    account = SettlementAccountSummary(
                        bankName = "국민",
                        maskedNumber = "***-***-4821",
                        payoutNote = "매월 5일 지급"
                    )
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
