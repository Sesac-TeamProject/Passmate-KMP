package org.sesacteamproject.passmate.ui.payment

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateCard
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.payment.domain.model.CoinTransactionType
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// 코인 사용·충전 내역 (M-12) — 충전(+)·차감(-)·환급(+) 목록, 건별 잔액 표시.
@Composable
fun CoinHistoryScreen(
    viewModel: CoinHistoryViewModel = koinScreenViewModel(),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.onAction(CoinHistoryAction.Enter)
    }
    CoinHistoryContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = { onNavigate(NavigationAction.NavigateBack) }
    )
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
            .background(PassmateColors.BackgroundMint)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "‹ 뒤로",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.clickable { onBack() }.padding(vertical = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("코인·결제 내역", color = PassmateColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> CenterProgress()
                uiState.hasError -> RetryState(onRetry = { onAction(CoinHistoryAction.Retry) })
                uiState.isEmpty -> EmptyState()
                else -> HistoryList(uiState = uiState, onLoadMore = { onAction(CoinHistoryAction.LoadMore) })
            }
        }
    }
}

@Composable
private fun HistoryList(uiState: CoinHistoryUiState, onLoadMore: () -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(uiState.items, key = { it.id }) { tx ->
            TransactionRow(tx)
        }
        if (uiState.hasNext) {
            item(key = "load-more") {
                LaunchedEffect(uiState.items.size) { onLoadMore() }
                Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = PassmateColors.Primary)
                }
            }
        }
        item(key = "bottom-space") { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun TransactionRow(tx: CoinTransaction) {
    PassmateCard {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(titleOf(tx), color = PassmateColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                subtitleOf(tx)?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, color = PassmateColors.TextTertiary, fontSize = 12.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amountText(tx), color = amountColor(tx), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("잔액 ${tx.balanceAfter} C", color = PassmateColors.TextTertiary, fontSize = 11.sp)
            }
        }
    }
}

private fun titleOf(tx: CoinTransaction): String {
    return when (tx.type) {
        CoinTransactionType.CHARGE -> "코인 충전"
        CoinTransactionType.DEDUCT -> tx.roomTitle?.let { "참가비 · $it" } ?: "참가비 차감"
        CoinTransactionType.REFUND -> tx.roomTitle?.let { "환급 · $it" } ?: "참가비 환급"
    }
}

private fun subtitleOf(tx: CoinTransaction): String? {
    return when (tx.type) {
        CoinTransactionType.CHARGE -> tx.method?.label
        else -> tx.paymentNo
    }
}

private fun amountText(tx: CoinTransaction): String {
    val sign = if (tx.amount >= 0) {
        "+"
    } else {
        ""
    }

    return "$sign${tx.amount} C"
}

private fun amountColor(tx: CoinTransaction): androidx.compose.ui.graphics.Color {
    return if (tx.amount >= 0) {
        PassmateColors.InkGreen
    } else {
        PassmateColors.TextPrimary
    }
}

@Composable
private fun CenterProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PassmateColors.Primary)
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("아직 코인 내역이 없어요", color = PassmateColors.TextTertiary, fontSize = 14.sp)
    }
}

@Composable
private fun RetryState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("내역을 불러오지 못했어요", color = PassmateColors.TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "다시 시도",
            color = PassmateColors.Surface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .background(PassmateColors.Primary, RoundedCornerShape(12.dp))
                .clickable { onRetry() }
                .padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
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
