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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateIcon
import org.sesacteamproject.passmate.component.PassmateIcons
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.payment.domain.model.CoinTransactionType
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// 코인 내역 (M-12-9) — 보유 코인 카드 · 전체/충전/사용 필터 · 내역 목록
// 빈 상태는 "빈 상태 — 코인 내역", 목록 실패는 "E-List 목록 불러오기 실패 — 공통 패턴"을 따른다
@Composable
fun CoinHistoryScreen(
    viewModel: CoinHistoryViewModel = koinScreenViewModel(),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.onAction(CoinHistoryAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is CoinHistoryEvent.OpenCoinCharge -> onNavigate(NavigationAction.NavigateToCoinCharge)
                is CoinHistoryEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        CoinHistoryContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onBack = { onNavigate(NavigationAction.NavigateBack) }
        )
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun CoinHistoryContentScreen(
    uiState: CoinHistoryUiState,
    onAction: (CoinHistoryAction) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
            // 배경은 상태바 뒤까지, 하단 인셋은 탭바(PassmateBottomTabBar)가 준다 — 시안이 이 화면에 탭바를 유지한다
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(16.dp))
        TopBar(onBack = onBack)
        when {
            uiState.isLoading -> CenterProgress()
            uiState.hasError -> LoadFailureBody(onAction = onAction, onBack = onBack)
            uiState.isEmpty -> EmptyBody(onAction = onAction)
            else -> HistoryBody(uiState = uiState, onAction = onAction)
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PassmateIcon(
            icon = PassmateIcons.ArrowLeft,
            contentDescription = "뒤로 가기",
            tint = PassmateColors.TextPrimary,
            modifier = Modifier
                .clickable { onBack() }
                .padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
                .size(22.dp)
        )
        Text(
            text = "코인 내역",
            color = PassmateColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        )
    }
}

@Composable
private fun HistoryBody(
    uiState: CoinHistoryUiState,
    onAction: (CoinHistoryAction) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item(key = "balance") {
            Spacer(Modifier.height(8.dp))
            BalanceCard(balance = uiState.balance)
            Spacer(Modifier.height(16.dp))
        }
        item(key = "filters") {
            FilterChipRow(
                selected = uiState.filter,
                onSelect = { onAction(CoinHistoryAction.SelectFilter(it)) }
            )
            Spacer(Modifier.height(16.dp))
        }
        item(key = "transactions") {
            TransactionCard(items = uiState.visibleItems)
        }
        if (uiState.hasNext) {
            item(key = "load-more") {
                LaunchedEffect(uiState.items.size) { onAction(CoinHistoryAction.LoadMore) }
                Box(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = PassmateColors.Primary
                    )
                }
            }
        }
        item(key = "bottom-space") { Spacer(Modifier.height(24.dp)) }
    }
}

// 시안 card/잔액 — 민트 배경 + 코인 마크 + 우측 잔액. 잔액 조회 실패 시 "-"로 그린다
@Composable
private fun BalanceCard(balance: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.BackgroundMint, RoundedCornerShape(16.dp))
            .heightIn(min = 128.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoinMark()
        Spacer(Modifier.width(10.dp))
        Text(
            text = "보유 코인",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = balanceText(balance),
            color = PassmateColors.PrimaryDeep,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        )
    }
}

@Composable
private fun CoinMark() {
    PassmateIcon(
        icon = PassmateIcons.Coin,
        contentDescription = null,
        tint = PassmateColors.TextPrimary,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
private fun FilterChipRow(
    selected: CoinHistoryFilter,
    onSelect: (CoinHistoryFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CoinHistoryFilter.entries.forEach { filter ->
            FilterChip(
                filter = filter,
                isSelected = filter == selected,
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun FilterChip(
    filter: CoinHistoryFilter,
    isSelected: Boolean,
    onSelect: (CoinHistoryFilter) -> Unit
) {
    val background = if (isSelected) {
        PassmateColors.FilterChipSelectedBg
    } else {
        PassmateColors.FieldGray
    }
    val textColor = if (isSelected) {
        PassmateColors.TextPrimary
    } else {
        PassmateColors.TextSecondary
    }

    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .clickable { onSelect(filter) }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = filter.label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// 시안 card — 흰 카드 1장 안에 행을 쌓고 마지막 행만 구분선을 뺀다
@Composable
private fun TransactionCard(items: List<CoinTransaction>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
    ) {
        items.forEachIndexed { index, transaction ->
            TransactionRow(transaction = transaction)
            if (index != items.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PassmateColors.Border)
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: CoinTransaction) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = titleOf(transaction),
                color = PassmateColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = shortDateOf(transaction),
                color = PassmateColors.TextSecondary,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = amountText(transaction),
            color = amountColor(transaction),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// 빈 상태 — 코인 내역 (M-12-9)
@Composable
private fun EmptyBody(onAction: (CoinHistoryAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        EmptyIcon()
        Spacer(Modifier.height(24.dp))
        Text(
            text = "아직 코인 내역이 없어요",
            color = PassmateColors.TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "코인을 충전하거나 유료 방에 참여하면\n여기에 기록이 남아요.",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(52.dp)
                .background(PassmateColors.Primary, RoundedCornerShape(14.dp))
                .clickable { onAction(CoinHistoryAction.ClickCharge) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "코인 충전하기",
                color = PassmateColors.Surface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

// E-List 목록 불러오기 실패 — 공통 패턴 (M-12-9 문구: 둘째 줄 "충전 기록은 사라지지 않아요.")
// 다른 화면(참여한 방·마이·정산)에도 같은 패턴이 쓰이므로 추후 공통 컴포넌트로 승격 대상이다
@Composable
private fun LoadFailureBody(
    onAction: (CoinHistoryAction) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        ErrorIcon()
        Spacer(Modifier.height(24.dp))
        Text(
            text = "목록을 불러오지 못했어요",
            color = PassmateColors.TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = "연결이 잠시 끊겼어요.\n충전 기록은 사라지지 않아요.",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1.6f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(PassmateColors.Primary, RoundedCornerShape(14.dp))
                .clickable { onAction(CoinHistoryAction.Retry) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "다시 시도",
                color = PassmateColors.Surface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "보유 코인은 마이에서 확인",
            color = PassmateColors.PrimaryDeep,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onBack() }.padding(vertical = 4.dp, horizontal = 8.dp)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EmptyIcon() {
    Box(
        modifier = Modifier.size(64.dp).background(PassmateColors.EmptyIconBg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        PassmateIcon(
            icon = PassmateIcons.List,
            contentDescription = null,
            tint = PassmateColors.PrimaryDeep,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun ErrorIcon() {
    Box(
        modifier = Modifier.size(64.dp).background(PassmateColors.ErrorIconBg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        PassmateIcon(
            icon = PassmateIcons.AlertCircle,
            contentDescription = null,
            tint = PassmateColors.ErrorIconTint,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun CenterProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PassmateColors.Primary)
    }
}

// 계약에 내역 설명 필드가 없어 type·roomTitle·method로 조합한다 (시안 "카카오페이 충전"·"… 참가비")
private fun titleOf(transaction: CoinTransaction): String {
    return when (transaction.type) {
        CoinTransactionType.CHARGE -> transaction.method?.let { "${it.label} 충전" } ?: "코인 충전"
        CoinTransactionType.DEDUCT -> transaction.roomTitle?.let { "$it 참가비" } ?: "참가비"
        CoinTransactionType.REFUND -> transaction.roomTitle?.let { "$it 환급" } ?: "코인 환급"
    }
}

// "2026-08-22T10:00:00Z" → "8/22" (시안은 월만 앞 0을 떼고 일은 두 자리 유지). 파싱 실패 시 원문 앞 10자
private fun shortDateOf(transaction: CoinTransaction): String {
    val raw = transaction.createdAt ?: return ""
    val parts = raw.take(10).split("-")

    return if (parts.size == 3) {
        "${parts[1].trimStart('0')}/${parts[2]}"
    } else {
        raw.take(10)
    }
}

private fun amountText(transaction: CoinTransaction): String {
    val amount = transaction.amount
    val sign = if (amount >= 0) {
        "+"
    } else {
        "-"
    }

    return "$sign${coinNumber(if (amount >= 0) amount else -amount)} C"
}

private fun amountColor(transaction: CoinTransaction): Color {
    return if (transaction.amount >= 0) {
        PassmateColors.PrimaryDeep
    } else {
        PassmateColors.TextPrimary
    }
}

private fun balanceText(balance: Int?): String {
    return if (balance == null) {
        "- C"
    } else {
        "${coinNumber(balance)} C"
    }
}

private fun coinNumber(value: Int): String {
    return value.toString().reversed().chunked(3).joinToString(",").reversed()
}

// --- Preview ---

@PassmatePreview
@Composable
private fun CoinHistoryContentScreenPreview() {
    PassmateTheme {
        CoinHistoryContentScreen(
            uiState = CoinHistoryUiState(
                isLoading = false,
                items = listOf(
                    CoinTransaction(id = 9001, type = CoinTransactionType.CHARGE, amount = 1000, balanceAfter = 1000, method = PaymentMethod.KAKAO_PAY, roomTitle = null, paymentNo = "PAY-20260810-01", createdAt = "2026.08.10"),
                    CoinTransaction(id = 9002, type = CoinTransactionType.DEDUCT, amount = -500, balanceAfter = 500, method = null, roomTitle = "8월 4주차 Spring 스터디", paymentNo = null, createdAt = "2026.08.14"),
                    CoinTransaction(id = 9003, type = CoinTransactionType.REFUND, amount = 500, balanceAfter = 1000, method = null, roomTitle = "확률과 통계 총정리", paymentNo = null, createdAt = "2026.08.16"),
                    CoinTransaction(id = 9004, type = CoinTransactionType.DEDUCT, amount = -300, balanceAfter = 700, method = null, roomTitle = "함수의 극한 퀴즈", paymentNo = null, createdAt = "2026.08.20")
                )
            ),
            onAction = {},
            onBack = {}
        )
    }
}

// 내역 없음 — 빈 상태
@PassmatePreview
@Composable
private fun CoinHistoryContentScreenEmptyPreview() {
    PassmateTheme {
        CoinHistoryContentScreen(
            uiState = CoinHistoryUiState(isLoading = false, items = emptyList()),
            onAction = {},
            onBack = {}
        )
    }
}

@PassmatePreview
@Composable
private fun CoinHistoryContentScreenErrorPreview() {
    PassmateTheme {
        CoinHistoryContentScreen(
            uiState = CoinHistoryUiState(isLoading = false, hasError = true),
            onAction = {},
            onBack = {}
        )
    }
}
