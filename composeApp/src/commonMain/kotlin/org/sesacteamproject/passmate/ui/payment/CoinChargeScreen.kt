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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateBackButton
import org.sesacteamproject.passmate.component.PortOnePaymentView
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// 코인 충전 (M-12-4 금액 선택 · M-12-6 완료). 완료는 별도 라우트가 아니라 isCompleted 전환으로 그린다.
// 결제창 UI는 포트원 SDK가 그리므로(M-12-5는 그 목업) 여기서는 PortOnePaymentView만 덮어씌운다
@Composable
fun CoinChargeScreen(
    viewModel: CoinChargeViewModel = koinScreenViewModel(),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.onAction(CoinChargeAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is CoinChargeEvent.Done -> onNavigate(NavigationAction.NavigateBack)
                is CoinChargeEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        CoinChargeContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onBack = { onNavigate(NavigationAction.NavigateBack) }
        )
        uiState.checkout?.let { request ->
            Box(modifier = Modifier.fillMaxSize().background(PassmateColors.Surface)) {
                PortOnePaymentView(
                    request = request,
                    onResult = { viewModel.onAction(CoinChargeAction.ReceivePortOneResult(it)) }
                )
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun CoinChargeContentScreen(
    uiState: CoinChargeUiState,
    onAction: (CoinChargeAction) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        TopBar(onBack = onBack)
        Spacer(Modifier.height(20.dp))
        when {
            uiState.isLoading -> CenterProgress()
            uiState.hasLoadError -> RetryState(onRetry = { onAction(CoinChargeAction.Retry) })
            uiState.isCompleted -> CompletedBody(uiState = uiState, onAction = onAction)
            else -> AmountBody(uiState = uiState, onAction = onAction)
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PassmateBackButton(onClick = onBack)
        Text(
            text = "코인 충전",
            color = PassmateColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        )
    }
}

// M-12-4 — 보유 코인 · 충전 금액 · 결제 수단 · CTA
@Composable
private fun AmountBody(
    uiState: CoinChargeUiState,
    onAction: (CoinChargeAction) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BalanceCard(balance = uiState.balance)
        Spacer(Modifier.height(24.dp))
        Text("충전 금액", color = PassmateColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        AmountGrid(
            presets = uiState.presets,
            selected = uiState.selectedAmount,
            onSelect = { onAction(CoinChargeAction.SelectAmount(it)) }
        )
        Spacer(Modifier.height(22.dp))
        Text("결제 수단", color = PassmateColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        MethodList(
            selected = uiState.selectedMethod,
            onSelect = { onAction(CoinChargeAction.SelectMethod(it)) }
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "1 C = ₩1 · 포트원(PortOne) 안전 결제 · 충전 후 7일 내 미사용 시 환불 가능",
            color = PassmateColors.TextTertiary,
            fontSize = 12.sp,
            letterSpacing = (-0.24).sp
        )
        uiState.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = PassmateColors.WrongPinkText, fontSize = 13.sp)
        }
        Spacer(Modifier.height(20.dp))
        ChargeButton(uiState = uiState, onClick = { onAction(CoinChargeAction.ClickCharge) })
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BalanceCard(balance: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.BackgroundMint, RoundedCornerShape(18.dp))
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(1.5.dp, PassmateColors.InkGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("C", color = PassmateColors.InkGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(10.dp))
        Text(
            text = "보유 코인",
            color = PassmateColors.TextSecondary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${formatNumber(balance)} C",
            color = PassmateColors.PrimaryDeep,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.44).sp
        )
    }
}

// 시안 M-12-4의 2×2 그리드 — 프리셋 개수가 홀수여도 마지막 줄이 깨지지 않게 chunked로 채운다
@Composable
private fun AmountGrid(
    presets: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        presets.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { amount ->
                    AmountCell(
                        amount = amount,
                        isSelected = amount == selected,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AmountCell(
    amount: Int,
    isSelected: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) PassmateColors.Primary else PassmateColors.Border
    val textColor = if (isSelected) PassmateColors.PrimaryDeep else PassmateColors.TextPrimary

    Box(
        modifier = modifier
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onSelect(amount) }
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "₩${formatNumber(amount)}",
            color = textColor,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun MethodList(
    selected: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PaymentMethod.entries.forEach { method ->
            MethodRow(
                method = method,
                isSelected = method == selected,
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun MethodRow(
    method: PaymentMethod,
    isSelected: Boolean,
    onSelect: (PaymentMethod) -> Unit
) {
    val borderColor = if (isSelected) PassmateColors.Primary else PassmateColors.Border
    val textColor = if (isSelected) PassmateColors.TextPrimary else PassmateColors.TextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onSelect(method) }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioMark(isSelected = isSelected)
        Spacer(Modifier.size(12.dp))
        Text(
            text = method.label,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun RadioMark(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .border(1.5.dp, if (isSelected) PassmateColors.Primary else PassmateColors.Border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(modifier = Modifier.size(10.dp).background(PassmateColors.Primary, CircleShape))
        }
    }
}

@Composable
private fun ChargeButton(
    uiState: CoinChargeUiState,
    onClick: () -> Unit
) {
    val enabled = !uiState.isProcessing
    val bg = if (enabled) PassmateColors.Primary else PassmateColors.Border

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isProcessing) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
                color = PassmateColors.Surface
            )
        } else {
            Text(
                text = "₩${formatNumber(uiState.selectedAmount)} 충전하기",
                color = PassmateColors.Surface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// M-12-6 — 충전 완료
@Composable
private fun CompletedBody(
    uiState: CoinChargeUiState,
    onAction: (CoinChargeAction) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.size(72.dp).background(PassmateColors.Primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", color = PassmateColors.Surface, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "${formatNumber(uiState.chargedAmount)} C 충전 완료",
            color = PassmateColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.44).sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "보유 코인 ${formatNumber(uiState.balance)} C · " +
                "${uiState.selectedMethod.label} ₩${formatNumber(uiState.chargedAmount)}",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            letterSpacing = (-0.28).sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "결제 내역은 마이 › 코인 · 결제에서 볼 수 있어요",
            color = PassmateColors.TextTertiary,
            fontSize = 13.sp,
            letterSpacing = (-0.26).sp
        )
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PassmateColors.Primary, RoundedCornerShape(16.dp))
                .clickable { onAction(CoinChargeAction.ClickConfirmDone) }
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("확인", color = PassmateColors.Surface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun CenterProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PassmateColors.Primary)
    }
}

@Composable
private fun RetryState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("보유 코인을 불러오지 못했어요", color = PassmateColors.TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "다시 시도",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .border(1.dp, PassmateColors.Border, RoundedCornerShape(12.dp))
                .clickable { onRetry() }
                .padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

private fun formatNumber(value: Int): String {
    return value.toString().reversed().chunked(3).joinToString(",").reversed()
}

// --- Preview ---

// M-12-4 금액 선택
@PassmatePreview
@Composable
private fun CoinChargeContentScreenPreview() {
    PassmateTheme {
        CoinChargeContentScreen(
            uiState = CoinChargeUiState(
                isLoading = false,
                balance = 1200,
                presets = listOf(5_000, 10_000, 30_000, 50_000),
                selectedAmount = 10_000
            ),
            onAction = {},
            onBack = {}
        )
    }
}

// M-12-6 충전 완료 — 별도 라우트가 아니라 isCompleted 전환으로 같은 라우트에서 그린다
@PassmatePreview
@Composable
private fun CoinChargeContentScreenCompletedPreview() {
    PassmateTheme {
        CoinChargeContentScreen(
            uiState = CoinChargeUiState(
                isLoading = false,
                balance = 11_200,
                isCompleted = true,
                chargedAmount = 10_000
            ),
            onAction = {},
            onBack = {}
        )
    }
}

// 잔액·프리셋 로드 실패 (규칙 §11)
@PassmatePreview
@Composable
private fun CoinChargeContentScreenFailedPreview() {
    PassmateTheme {
        CoinChargeContentScreen(
            uiState = CoinChargeUiState(isLoading = false, hasLoadError = true),
            onAction = {},
            onBack = {}
        )
    }
}
