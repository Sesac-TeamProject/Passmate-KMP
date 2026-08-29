package org.sesacteamproject.passmate.ui.hostroom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.question.domain.model.QuestionSetSummary
import org.sesacteamproject.passmate.theme.PassmateColors

// Figma "UI 디자인 v6" M-13 새 방 만들기 시트(406:5893) — 방 이름·문제 세트·방 유형 → PIN 발급.
// 시트 표시 여부는 호스팅 화면(HostedRoomsScreen)이 소유한다 (규칙 §11-1)
@Composable
fun CreateRoomSheet(
    onCreated: (String) -> Unit,
    onNotice: (String) -> Unit,
    onClose: () -> Unit
) {
    val viewModel: CreateRoomViewModel = koinScreenViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onAction(CreateRoomAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is CreateRoomEvent.Created -> onCreated(event.pin)
                is CreateRoomEvent.ShowNotice -> onNotice(event.message)
            }
        }
    }
    CreateRoomContentView(
        uiState = uiState,
        onAction = viewModel::onAction,
        onClose = onClose
    )
}

@Composable
private fun CreateRoomContentView(
    uiState: CreateRoomUiState,
    onAction: (CreateRoomAction) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "새 방 만들기",
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
        FieldLabel(text = "방 이름")
        TextField(
            value = uiState.title,
            onValueChange = { onAction(CreateRoomAction.ChangeTitle(it)) },
            singleLine = true,
            placeholder = {
                Text("예: 8월 4주차 Spring 스터디", color = PassmateColors.TextTertiary, fontSize = 14.sp)
            },
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = PassmateColors.TextPrimary),
            colors = createFieldColors(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        FieldLabel(text = "문제 세트")
        SetSelector(
            uiState = uiState,
            onSelect = { onAction(CreateRoomAction.SelectSet(it)) },
            onRetry = { onAction(CreateRoomAction.RetrySets) }
        )
        FieldLabel(text = "방 유형")
        PaidToggle(
            isPaid = uiState.isPaid,
            onSelect = { onAction(CreateRoomAction.SelectPaid(it)) }
        )
        if (uiState.isPaid) {
            FieldLabel(text = "참가비 (1 C = ₩1)")
            TextField(
                value = uiState.entryFeeText,
                onValueChange = { onAction(CreateRoomAction.ChangeEntryFee(it)) },
                singleLine = true,
                placeholder = {
                    Text("예: 10000", color = PassmateColors.TextTertiary, fontSize = 14.sp)
                },
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = PassmateColors.TextPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = createFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = "PIN은 방을 만들면 자동 발급 · 프로젝터 화면은 웹에서",
            color = PassmateColors.TextTertiary,
            fontSize = 12.sp,
            letterSpacing = (-0.24).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        SubmitButton(
            enabled = uiState.canSubmit,
            isSubmitting = uiState.isSubmitting,
            onClick = { onAction(CreateRoomAction.Submit) }
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = PassmateColors.TextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.26).sp
    )
}

@Composable
private fun createFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = PassmateColors.FieldGray,
    unfocusedContainerColor = PassmateColors.FieldGray,
    focusedIndicatorColor = PassmateColors.Primary,
    unfocusedIndicatorColor = PassmateColors.FieldGray
)

@Composable
private fun SetSelector(
    uiState: CreateRoomUiState,
    onSelect: (Long) -> Unit,
    onRetry: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    when {
        uiState.isLoadingSets -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(PassmateColors.FieldGray, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = PassmateColors.Primary,
                strokeWidth = 2.dp
            )
        }
        uiState.setsLoadFailed -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PassmateColors.FieldGray, RoundedCornerShape(14.dp))
                .clickable(onClick = onRetry)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "세트를 불러오지 못했어요 · 다시 시도",
                color = PassmateColors.WeakTopicText,
                fontSize = 14.sp,
                letterSpacing = (-0.28).sp
            )
        }
        uiState.sets.isEmpty() -> Text(
            text = "확정된 문제 세트가 없어요 · 웹에서 세트를 만들고 확정해 주세요",
            color = PassmateColors.TextSecondary,
            fontSize = 13.sp,
            letterSpacing = (-0.26).sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(PassmateColors.FieldGray, RoundedCornerShape(14.dp))
                .padding(16.dp)
        )
        else -> Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PassmateColors.FieldGray, RoundedCornerShape(14.dp))
                    .clickable { isExpanded = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.selectedSet?.let { setLabel(it) } ?: "문제 세트 선택",
                    color = PassmateColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.28).sp,
                    modifier = Modifier.weight(1f)
                )
                Text(text = "▾", color = PassmateColors.TextSecondary, fontSize = 14.sp)
            }
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false }
            ) {
                uiState.sets.forEach { set ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = setLabel(set),
                                color = PassmateColors.TextPrimary,
                                fontSize = 14.sp,
                                letterSpacing = (-0.28).sp
                            )
                        },
                        onClick = {
                            isExpanded = false
                            onSelect(set.setId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaidToggle(
    isPaid: Boolean,
    onSelect: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.FieldGray, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PaidOption(
            label = "무료",
            isSelected = !isPaid,
            onClick = { onSelect(false) },
            modifier = Modifier.weight(1f)
        )
        PaidOption(
            label = "유료 (Lv.3부터)",
            isSelected = isPaid,
            onClick = { onSelect(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PaidOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (isSelected) PassmateColors.Surface else PassmateColors.FieldGray
    val textColor = if (isSelected) PassmateColors.PrimaryDeep else PassmateColors.TextSecondary

    Box(
        modifier = modifier
            .height(44.dp)
            .background(background, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun SubmitButton(
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
                text = "방 만들기 → PIN 발급",
                color = PassmateColors.Surface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
        }
    }
}

private fun setLabel(set: QuestionSetSummary): String {
    return "${set.title} (${set.questionCount}문항)"
}
