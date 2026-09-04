package org.sesacteamproject.passmate.ui.mypage

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import org.sesacteamproject.passmate.component.StudentAvatar
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// Figma "UI 디자인 v6" M-12-1(437:5424) — 계정 정보 변경: 캐릭터 요약 + 닉네임 + 이메일(읽기 전용).
// 캐릭터 자체는 M-12-7(CharacterEditScreen)에서 바꾼다. 시안이 전체 페이지라 라우트 push다 (규칙 §2-1)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = koinScreenViewModel(),
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.onAction(EditProfileAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is EditProfileEvent.Saved -> onNavigate(NavigationAction.NavigateBack)
                is EditProfileEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        EditProfileContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onBack = { onNavigate(NavigationAction.NavigateBack) },
            onClickChangeCharacter = { onNavigate(NavigationAction.NavigateToCharacterEdit) }
        )
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun EditProfileContentScreen(
    uiState: EditProfileUiState,
    onAction: (EditProfileAction) -> Unit,
    onBack: () -> Unit,
    onClickChangeCharacter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
            // 배경은 상태바 뒤까지, 하단 인셋은 탭바(PassmateBottomTabBar)가 준다
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PassmateBackButton(onClick = onBack)
            Text(
                text = "계정 정보 변경",
                color = PassmateColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )
        }
        if (uiState.isLoading) {
            LoadingBox()
        } else if (uiState.hasLoadError) {
            RetryBox(onRetry = { onAction(EditProfileAction.Retry) })
        } else {
            ProfileCard(
                uiState = uiState,
                onAction = onAction,
                onClickChangeCharacter = onClickChangeCharacter
            )
            SaveButton(
                enabled = uiState.canSubmit,
                isSubmitting = uiState.isSubmitting,
                onClick = { onAction(EditProfileAction.Submit) }
            )
        }
    }
}

@Composable
private fun ProfileCard(
    uiState: EditProfileUiState,
    onAction: (EditProfileAction) -> Unit,
    onClickChangeCharacter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudentAvatar(
                avatarId = uiState.avatarId,
                modifier = Modifier.size(56.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "프로필 캐릭터",
                    color = PassmateColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "캐릭터 바꾸기 →",
                    color = PassmateColors.Primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.28).sp,
                    modifier = Modifier.clickable(onClick = onClickChangeCharacter)
                )
            }
        }
        FieldLabel(text = "닉네임")
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
        FieldLabel(text = "이메일")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(PassmateColors.FieldGray, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = uiState.email ?: "-",
                color = PassmateColors.TextTertiary,
                fontSize = 14.sp
            )
        }
        Text(
            text = "이메일은 로그인 ID라 바꿀 수 없어요. 닉네임은 방 안에서 학생 · 선생님에게 보여요",
            color = PassmateColors.TextTertiary,
            fontSize = 12.sp,
            letterSpacing = (-0.24).sp
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
private fun LoadingBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PassmateColors.Primary)
    }
}

@Composable
private fun RetryBox(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "계정 정보를 불러오지 못했어요",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "다시 시도",
            color = PassmateColors.Primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onRetry).padding(8.dp)
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
                text = "저장하기",
                color = PassmateColors.Surface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
        }
    }
}

@PassmatePreview
@Composable
private fun EditProfilePreview() {
    PassmateTheme {
        EditProfileContentScreen(
            uiState = EditProfileUiState(
                nickname = "한결",
                email = "hangyeol@example.com",
                avatarId = 1,
                isLoading = false
            ),
            onAction = {},
            onBack = {},
            onClickChangeCharacter = {}
        )
    }
}
