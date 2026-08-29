package org.sesacteamproject.passmate.ui.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.theme.PassmateColors

// Figma "UI 디자인 v6" M-12-3(437:5534) — 정산 계좌 등록/변경: 은행·계좌번호·예금주.
// 시트 표시 여부는 호스팅 화면(EarningsScreen)이 소유한다 (규칙 §11-1)
@Composable
fun SettlementAccountSheet(
    onSaved: () -> Unit,
    onNotice: (String) -> Unit,
    onClose: () -> Unit
) {
    val viewModel: SettlementAccountViewModel = koinScreenViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onAction(SettlementAccountAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is SettlementAccountEvent.Saved -> onSaved()
                is SettlementAccountEvent.ShowNotice -> onNotice(event.message)
            }
        }
    }
    SettlementAccountContentView(
        uiState = uiState,
        onAction = viewModel::onAction,
        onClose = onClose
    )
}

@Composable
private fun SettlementAccountContentView(
    uiState: SettlementAccountUiState,
    onAction: (SettlementAccountAction) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.Surface)
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "정산 계좌",
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
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PassmateColors.Primary)
            }
        } else {
            AccountField(
                label = "은행",
                value = uiState.bankName,
                placeholder = "예: 신한은행",
                onChange = { onAction(SettlementAccountAction.ChangeBankName(it)) }
            )
            AccountField(
                label = "계좌번호",
                value = uiState.accountNumber,
                placeholder = "숫자만 입력",
                keyboardType = KeyboardType.Number,
                onChange = { onAction(SettlementAccountAction.ChangeAccountNumber(it)) }
            )
            AccountField(
                label = "예금주",
                value = uiState.holderName,
                placeholder = "예금주명",
                onChange = { onAction(SettlementAccountAction.ChangeHolderName(it)) }
            )
            Text(
                text = "매월 5일 지급 · 사업소득 3.3% 원천징수(확정 전)",
                color = PassmateColors.TextTertiary,
                fontSize = 12.sp,
                letterSpacing = (-0.24).sp
            )
            SaveButton(
                enabled = uiState.canSubmit,
                isSubmitting = uiState.isSubmitting,
                onClick = { onAction(SettlementAccountAction.Submit) }
            )
        }
    }
}

@Composable
private fun AccountField(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = PassmateColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.26).sp
        )
        TextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            placeholder = {
                Text(placeholder, color = PassmateColors.TextTertiary, fontSize = 14.sp)
            },
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = PassmateColors.TextPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = PassmateColors.FieldGray,
                unfocusedContainerColor = PassmateColors.FieldGray,
                focusedIndicatorColor = PassmateColors.Primary,
                unfocusedIndicatorColor = PassmateColors.FieldGray
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
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
