package org.sesacteamproject.passmate.ui.mypage

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.StudentAvatar
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.theme.PassmateColors

// Figma "UI 디자인 v6" M-12-1(437:5424)+M-12-7(450:5938) — 계정 정보(닉네임)·내 캐릭터 변경 통합 시트.
// 시트 표시 여부는 호스팅 화면(SettingsScreen)이 소유한다 (규칙 §11-1)
@Composable
fun EditProfileSheet(
    initialNickname: String,
    initialAvatarId: Int?,
    onSaved: () -> Unit,
    onNotice: (String) -> Unit,
    onClose: () -> Unit
) {
    val viewModel: EditProfileViewModel = koinScreenViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onAction(EditProfileAction.Enter(initialNickname, initialAvatarId))
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is EditProfileEvent.Saved -> onSaved()
                is EditProfileEvent.ShowNotice -> onNotice(event.message)
            }
        }
    }
    EditProfileContentView(
        uiState = uiState,
        onAction = viewModel::onAction,
        onClose = onClose
    )
}

@Composable
private fun EditProfileContentView(
    uiState: EditProfileUiState,
    onAction: (EditProfileAction) -> Unit,
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
                text = "계정 정보",
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
        Text(
            text = "닉네임",
            color = PassmateColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.26).sp
        )
        TextField(
            value = uiState.nickname,
            onValueChange = { onAction(EditProfileAction.ChangeNickname(it)) },
            singleLine = true,
            placeholder = {
                Text("닉네임 (최대 12자)", color = PassmateColors.TextTertiary, fontSize = 14.sp)
            },
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = PassmateColors.TextPrimary),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = PassmateColors.FieldGray,
                unfocusedContainerColor = PassmateColors.FieldGray,
                focusedIndicatorColor = PassmateColors.Primary,
                unfocusedIndicatorColor = PassmateColors.FieldGray
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "내 캐릭터 — 대기실·결과 화면에 표시돼요",
            color = PassmateColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.26).sp
        )
        AvatarGrid(
            selectedId = uiState.avatarId,
            onSelect = { onAction(EditProfileAction.SelectAvatar(it)) }
        )
        SaveButton(
            enabled = uiState.canSubmit,
            isSubmitting = uiState.isSubmitting,
            onClick = { onAction(EditProfileAction.Submit) }
        )
    }
}

@Composable
private fun AvatarGrid(
    selectedId: Int?,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        (1..12).chunked(6).forEach { rowIds ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowIds.forEach { avatarId ->
                    val isSelected = avatarId == selectedId
                    val borderColor = if (isSelected) PassmateColors.Primary else PassmateColors.Border
                    val borderWidth = if (isSelected) 2.dp else 1.dp

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(borderWidth, borderColor, CircleShape)
                            .clickable { onSelect(avatarId) }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        StudentAvatar(
                            avatarId = avatarId,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
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
