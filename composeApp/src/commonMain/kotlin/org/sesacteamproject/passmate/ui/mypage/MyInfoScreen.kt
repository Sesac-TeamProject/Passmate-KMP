package org.sesacteamproject.passmate.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.ReputationBadge
import org.sesacteamproject.passmate.component.StudentAvatar
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.ui.payment.PaymentMethodSheet
import org.sesacteamproject.passmate.user.domain.model.UserProfile

// 시트 3종 중 무엇이 열려 있는지 — 표시 여부는 이 화면이 소유한다 (규칙 §11-1)
private enum class MyInfoSheet {
    EDIT_PROFILE,
    PAYMENT_METHOD,
    NOTIFICATIONS
}

// Figma "UI 디자인 v6" M-12(349:9683) — 마이 탭 루트: 프로필·계정·코인·정산·알림·로그아웃
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyInfoScreen(onNavigate: (NavigationAction) -> Unit) {
    val viewModel: MyInfoViewModel = koinScreenViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState()
    var activeSheet by remember { mutableStateOf<MyInfoSheet?>(null) }
    var editInitial by remember { mutableStateOf<Pair<String, Int?>>("" to null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onAction(MyInfoAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is MyInfoEvent.RequireSignIn -> onNavigate(NavigationAction.NavigateToSignIn)
                is MyInfoEvent.OpenEditProfile -> {
                    editInitial = event.nickname to event.avatarId
                    activeSheet = MyInfoSheet.EDIT_PROFILE
                }
                is MyInfoEvent.OpenPaymentMethod -> activeSheet = MyInfoSheet.PAYMENT_METHOD
                is MyInfoEvent.OpenNotifications -> activeSheet = MyInfoSheet.NOTIFICATIONS
                is MyInfoEvent.OpenCoinHistory -> onNavigate(NavigationAction.NavigateToCoinHistory)
                is MyInfoEvent.SignedOut -> onNavigate(NavigationAction.NavigateToHome)
                is MyInfoEvent.AccountDeleted -> onNavigate(NavigationAction.NavigateToHome)
                is MyInfoEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        MyInfoContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onClickBack = { onNavigate(NavigationAction.NavigateBack) },
            onClickSignOut = { showSignOutConfirm = true },
            onClickDelete = { showDeleteConfirm = true }
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    activeSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = PassmateColors.Surface
        ) {
            when (sheet) {
                MyInfoSheet.EDIT_PROFILE -> EditProfileSheet(
                    initialNickname = editInitial.first,
                    initialAvatarId = editInitial.second,
                    onSaved = {
                        activeSheet = null
                        viewModel.onAction(MyInfoAction.ProfileUpdated)
                    },
                    onNotice = { viewModel.onAction(MyInfoAction.Notice(it)) },
                    onClose = { activeSheet = null }
                )
                MyInfoSheet.PAYMENT_METHOD -> PaymentMethodSheet(
                    onSaved = {
                        activeSheet = null
                        viewModel.onAction(MyInfoAction.Notice("기본 결제 수단을 저장했어요"))
                    },
                    onNotice = { viewModel.onAction(MyInfoAction.Notice(it)) },
                    onClose = { activeSheet = null }
                )
                MyInfoSheet.NOTIFICATIONS -> NotificationSettingsSheet(
                    onNotice = { viewModel.onAction(MyInfoAction.Notice(it)) },
                    onClose = { activeSheet = null }
                )
            }
        }
    }
    if (showSignOutConfirm) {
        ConfirmDialog(
            title = "로그아웃",
            message = "로그아웃하면 게스트로 전환돼요. 기록은 계정에 안전하게 보관돼요.",
            confirmLabel = "로그아웃",
            onConfirm = {
                showSignOutConfirm = false
                viewModel.onAction(MyInfoAction.ConfirmSignOut)
            },
            onDismiss = { showSignOutConfirm = false }
        )
    }
    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "회원 탈퇴",
            message = "탈퇴하면 참여·개설 기록과 보유 코인이 모두 삭제되고 되돌릴 수 없어요. 정산 대기 금액이나 진행 중인 방이 있으면 탈퇴할 수 없어요.",
            confirmLabel = "탈퇴",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.onAction(MyInfoAction.ConfirmDeleteAccount)
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = PassmateColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                letterSpacing = (-0.28).sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmLabel, color = PassmateColors.WeakTopicText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "취소", color = PassmateColors.TextSecondary)
            }
        },
        containerColor = PassmateColors.Surface
    )
}

@Composable
private fun MyInfoContentScreen(
    uiState: MyInfoUiState,
    onAction: (MyInfoAction) -> Unit,
    onClickBack: () -> Unit,
    onClickSignOut: () -> Unit,
    onClickDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.Surface)
    ) {
        when {
            uiState.isLoading -> LoadingBox()
            uiState.loadFailed -> ErrorBox(onRetry = { onAction(MyInfoAction.Retry) })
            else -> LoadedMyInfo(
                uiState = uiState,
                onAction = onAction,
                onClickBack = onClickBack,
                onClickSignOut = onClickSignOut,
                onClickDelete = onClickDelete
            )
        }
    }
}

@Composable
private fun LoadedMyInfo(
    uiState: MyInfoUiState,
    onAction: (MyInfoAction) -> Unit,
    onClickBack: () -> Unit,
    onClickSignOut: () -> Unit,
    onClickDelete: () -> Unit
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
            Text(
                text = "설정",
                color = PassmateColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.48).sp
            )
            Text(
                text = "닫기",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp,
                modifier = Modifier
                    .clickable(onClick = onClickBack)
                    .padding(4.dp)
            )
        }
        uiState.profile?.let { profile ->
            ProfileCard(profile = profile)
        }
        SettingRow(label = "계정 정보 · 내 캐릭터", onClick = { onAction(MyInfoAction.ClickEditProfile) })
        SettingRow(label = "결제 수단 관리", onClick = { onAction(MyInfoAction.ClickPaymentMethod) })
        SettingRow(label = "알림 설정", onClick = { onAction(MyInfoAction.ClickNotifications) })
        SettingRow(label = "코인·결제 내역", onClick = { onAction(MyInfoAction.ClickCoinHistory) })
        SettingRow(
            label = "로그아웃",
            labelColor = PassmateColors.TextSecondary,
            onClick = onClickSignOut
        )
        SettingRow(
            label = "회원 탈퇴",
            labelColor = PassmateColors.WeakTopicText,
            onClick = onClickDelete
        )
        if (uiState.isProcessing) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = PassmateColors.Primary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: UserProfile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(20.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudentAvatar(
            avatarId = profile.avatarId,
            modifier = Modifier.size(52.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = profile.nickname,
                    color = PassmateColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.36).sp
                )
                profile.level?.let { level ->
                    ReputationBadge(level = level)
                }
            }
            Text(
                text = profileSubtitle(profile),
                color = PassmateColors.TextSecondary,
                fontSize = 13.sp,
                letterSpacing = (-0.26).sp
            )
            profile.coins?.let { coins ->
                Text(
                    text = "보유 코인 ${formatCoins(coins)} C",
                    color = PassmateColors.PrimaryDeep,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.26).sp
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    labelColor: Color = PassmateColors.TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = labelColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(text = "›", color = PassmateColors.TextTertiary, fontSize = 18.sp)
    }
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

@Composable
private fun ErrorBox(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "내 정보를 불러오지 못했어요",
            color = PassmateColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.32).sp
        )
        Text(
            text = "다시 시도",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp,
            modifier = Modifier
                .padding(top = 12.dp)
                .clickable(onClick = onRetry)
                .padding(8.dp)
        )
    }
}

private fun profileSubtitle(profile: UserProfile): String {
    val parts = mutableListOf<String>()

    profile.email?.let { parts.add(it) }
    profile.joinedAt?.let { parts.add("${it.take(10)} 가입") }

    return parts.joinToString(" · ").ifEmpty { "구글 계정으로 로그인" }
}

private fun formatCoins(coins: Long): String {
    return coins.toString().reversed().chunked(3).joinToString(",").reversed()
}
