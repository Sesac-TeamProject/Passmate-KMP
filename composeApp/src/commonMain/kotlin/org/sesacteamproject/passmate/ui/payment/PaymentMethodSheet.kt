package org.sesacteamproject.passmate.ui.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.theme.PassmateColors

// Figma "UI 디자인 v6" M-12-8(450:6184) — 결제 수단 관리: 기본 수단 5종 선택(카드 정보는 포트원 처리).
// 시트 표시 여부는 호스팅 화면(SettingsScreen)이 소유한다 (규칙 §11-1)
@Composable
fun PaymentMethodSheet(
    onSaved: () -> Unit,
    onNotice: (String) -> Unit,
    onClose: () -> Unit
) {
    val viewModel: PaymentMethodViewModel = koinScreenViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onAction(PaymentMethodAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is PaymentMethodEvent.Saved -> onSaved()
                is PaymentMethodEvent.ShowNotice -> onNotice(event.message)
            }
        }
    }
    PaymentMethodContentView(
        uiState = uiState,
        onAction = viewModel::onAction,
        onClose = onClose
    )
}

@Composable
private fun PaymentMethodContentView(
    uiState: PaymentMethodUiState,
    onAction: (PaymentMethodAction) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.Surface)
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "결제 수단 관리",
                color = PassmateColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )
            Text(
                text = "✕",
                color = PassmateColors.TextSecondary,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .padding(4.dp)
            )
        }
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PassmateColors.Primary)
            }
        } else {
            Text(
                text = "코인 충전 시 기본으로 선택될 수단이에요 · 카드 정보는 저장되지 않아요",
                color = PassmateColors.TextTertiary,
                fontSize = 12.sp,
                letterSpacing = (-0.24).sp
            )
            PaymentMethod.entries.forEach { method ->
                MethodRow(
                    method = method,
                    isSelected = method == uiState.selected,
                    onSelect = { onAction(PaymentMethodAction.Select(method)) }
                )
            }
            SaveButton(
                enabled = uiState.canSubmit,
                isSubmitting = uiState.isSubmitting,
                onClick = { onAction(PaymentMethodAction.Submit) }
            )
        }
    }
}

@Composable
private fun MethodRow(
    method: PaymentMethod,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) PassmateColors.Primary else PassmateColors.Border
    val background = if (isSelected) PassmateColors.BackgroundMint else PassmateColors.Surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .background(background, RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(
                    width = if (isSelected) 5.dp else 1.dp,
                    color = if (isSelected) PassmateColors.Primary else PassmateColors.Border,
                    shape = CircleShape
                )
        )
        Text(
            text = method.label,
            color = PassmateColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.3).sp
        )
    }
}

@Composable
private fun SaveButton(
    enabled: Boolean,
    isSubmitting: Boolean,
    onClick: () -> Unit
) {
    val background = if (enabled) PassmateColors.Primary else PassmateColors.Border

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(background, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = PassmateColors.Surface,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "저장",
                color = PassmateColors.Surface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
        }
    }
}
