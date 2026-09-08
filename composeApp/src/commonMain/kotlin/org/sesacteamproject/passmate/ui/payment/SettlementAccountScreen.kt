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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassmateBackButton
import org.sesacteamproject.passmate.component.PassmateIcon
import org.sesacteamproject.passmate.component.PassmateIcons
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.payment.domain.model.Bank
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// Figma "UI 디자인 v6" M-12-3(437:5534) — 정산 계좌 등록/변경: 은행·계좌번호·예금주.
// 시안이 전체 페이지라 라우트 push로 띄운다 (규칙 §2-1 — 상세는 모달이 아니라 push)
@Composable
fun SettlementAccountScreen(
    viewModel: SettlementAccountViewModel = koinScreenViewModel(),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.onAction(SettlementAccountAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is SettlementAccountEvent.Saved -> onNavigate(NavigationAction.NavigateBack)
                is SettlementAccountEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        SettlementAccountContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onBack = { onNavigate(NavigationAction.NavigateBack) }
        )
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SettlementAccountContentScreen(
    uiState: SettlementAccountUiState,
    onAction: (SettlementAccountAction) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
            // 배경은 상태바 뒤까지, 하단 인셋은 탭바(PassmateBottomTabBar)가 준다
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PassmateBackButton(onClick = onBack)
            Text(
                text = "정산 계좌 등록",
                color = PassmateColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
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
            AccountFormCard(
                uiState = uiState,
                onAction = onAction
            )
            RegisterButton(
                enabled = uiState.canSubmit,
                isSubmitting = uiState.isSubmitting,
                onClick = { onAction(SettlementAccountAction.Submit) }
            )
        }
    }
}

// 시안 card — 필드 3개 + 안내문을 테두리 카드(r16, pad 16, gap 16)로 감싼다
@Composable
private fun AccountFormCard(
    uiState: SettlementAccountUiState,
    onAction: (SettlementAccountAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BankSelectField(
            bankName = uiState.bankName,
            onSelect = { onAction(SettlementAccountAction.SelectBank(it)) }
        )
        AccountField(
            label = "계좌번호",
            value = uiState.accountNumber,
            placeholder = uiState.maskedAccountNumber.ifBlank { "숫자만 입력" },
            keyboardType = KeyboardType.Number,
            onChange = { onAction(SettlementAccountAction.ChangeAccountNumber(it)) }
        )
        AccountField(
            label = "예금주",
            value = uiState.holderName,
            placeholder = "예금주명",
            onChange = { onAction(SettlementAccountAction.ChangeHolderName(it)) }
        )
        // 지급 주기는 시안끼리 어긋난다(M-12-3 "매주 월요일" vs M-T4 "매월 5일") — 앱은 매월 5일로 통일
        Text(
            text = "매월 5일 지급 · 사업소득 3.3% 원천징수(확정 전)",
            color = PassmateColors.TextSecondary,
            fontSize = 12.sp,
            letterSpacing = (-0.24).sp
        )
    }
}

// 시안 field/은행 select — 은행 목록(Bank)에서 고른다. 자유 입력이면 백엔드 필수값 bankCode를 못 채운다
@Composable
private fun BankSelectField(
    bankName: String,
    onSelect: (Bank) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(FieldLabelGap)) {
        FieldLabel(text = "은행")
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FieldHeight)
                    .background(PassmateColors.FieldGray, RoundedCornerShape(FieldCorner))
                    .clickable { isExpanded = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bankName.ifBlank { "은행 선택" },
                    color = if (bankName.isBlank()) PassmateColors.TextTertiary else PassmateColors.TextPrimary,
                    fontSize = 14.sp,
                    letterSpacing = (-0.28).sp,
                    modifier = Modifier.weight(1f)
                )
                PassmateIcon(
                    icon = PassmateIcons.ChevronDown,
                    contentDescription = null,
                    tint = PassmateColors.TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false }
            ) {
                Bank.all.forEach { bank ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = bank.displayName,
                                color = PassmateColors.TextPrimary,
                                fontSize = 14.sp,
                                letterSpacing = (-0.28).sp
                            )
                        },
                        onClick = {
                            isExpanded = false
                            onSelect(bank)
                        }
                    )
                }
            }
        }
    }
}

// 시안 field input — 48 높이 · FieldGray · r12. Material TextField는 최소 56이라 BasicTextField로 그린다
@Composable
private fun AccountField(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(FieldLabelGap)) {
        FieldLabel(text = label)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp,
                letterSpacing = (-0.28).sp,
                color = PassmateColors.TextPrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(PassmateColors.Primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FieldHeight)
                        .background(PassmateColors.FieldGray, RoundedCornerShape(FieldCorner))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = PassmateColors.TextTertiary,
                            fontSize = 14.sp,
                            letterSpacing = (-0.28).sp
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = PassmateColors.TextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.28).sp
    )
}

// 시안 button/등록하기 — 48 높이 · r12 · 14sp Medium. 등록/변경 모두 같은 문구를 쓴다
@Composable
private fun RegisterButton(
    enabled: Boolean,
    isSubmitting: Boolean,
    onClick: () -> Unit
) {
    val background = if (enabled) PassmateColors.Primary else PassmateColors.Border

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(FieldHeight)
            .background(background, RoundedCornerShape(FieldCorner))
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
                text = "등록하기",
                color = PassmateColors.Surface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp
            )
        }
    }
}

private val FieldHeight = 48.dp

private val FieldCorner = 12.dp

private val FieldLabelGap = 8.dp

// --- Preview ---

@PassmatePreview
@Composable
private fun SettlementAccountContentScreenPreview() {
    PassmateTheme {
        SettlementAccountContentScreen(
            uiState = SettlementAccountUiState(
                isLoading = false,
                bankCode = "004",
                bankName = "국민은행",
                accountNumber = "123456-01-234567",
                holderName = "이한결"
            ),
            onAction = {},
            onBack = {}
        )
    }
}

@PassmatePreview
@Composable
private fun SettlementAccountContentScreenEmptyPreview() {
    PassmateTheme {
        SettlementAccountContentScreen(
            uiState = SettlementAccountUiState(isLoading = false),
            onAction = {},
            onBack = {}
        )
    }
}
