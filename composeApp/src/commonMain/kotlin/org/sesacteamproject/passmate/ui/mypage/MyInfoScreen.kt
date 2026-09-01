package org.sesacteamproject.passmate.ui.mypage

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.ReputationBadge
import org.sesacteamproject.passmate.component.StudentAvatar
import org.sesacteamproject.passmate.component.StudentAvatars
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.AppTab
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.payment.domain.model.NextPayout
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccountSummary
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme
import org.sesacteamproject.passmate.ui.payment.PaymentMethodSheet
import org.sesacteamproject.passmate.ui.payment.SettlementAccountSheet
import org.sesacteamproject.passmate.user.domain.model.UserProfile

// 시트 4종 중 무엇이 열려 있는지 — 표시 여부는 이 화면이 소유한다 (규칙 §11-1)
private enum class MyInfoSheet {
    EDIT_PROFILE,
    PAYMENT_METHOD,
    SETTLEMENT_ACCOUNT,
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

    LaunchedEffect(Unit) {
        viewModel.onAction(MyInfoAction.Enter)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is MyInfoEvent.RequireSignIn -> onNavigate(
                    NavigationAction.NavigateToSignIn(NavigationAction.NavigateToTab(AppTab.MY_INFO))
                )
                is MyInfoEvent.OpenReputation -> onNavigate(NavigationAction.NavigateToReputation)
                is MyInfoEvent.OpenEditProfile -> {
                    editInitial = event.nickname to event.avatarId
                    activeSheet = MyInfoSheet.EDIT_PROFILE
                }
                is MyInfoEvent.OpenPaymentMethod -> activeSheet = MyInfoSheet.PAYMENT_METHOD
                is MyInfoEvent.OpenCoinHistory -> onNavigate(NavigationAction.NavigateToCoinHistory)
                is MyInfoEvent.OpenCharge -> onNavigate(NavigationAction.NavigateToCoinCharge)
                is MyInfoEvent.OpenSettlementAccount -> activeSheet = MyInfoSheet.SETTLEMENT_ACCOUNT
                is MyInfoEvent.OpenEarnings -> onNavigate(NavigationAction.NavigateToEarnings)
                is MyInfoEvent.OpenNotifications -> activeSheet = MyInfoSheet.NOTIFICATIONS
                is MyInfoEvent.OpenSettings -> onNavigate(NavigationAction.NavigateToSettings)
                is MyInfoEvent.OpenDeleteAccount -> onNavigate(NavigationAction.NavigateToDeleteAccount)
                is MyInfoEvent.SignedOut -> onNavigate(NavigationAction.NavigateToHome)
                is MyInfoEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        MyInfoContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onClickSignOut = { showSignOutConfirm = true }
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
                        viewModel.onAction(MyInfoAction.PaymentMethodUpdated)
                    },
                    onNotice = { viewModel.onAction(MyInfoAction.Notice(it)) },
                    onClose = { activeSheet = null }
                )
                MyInfoSheet.SETTLEMENT_ACCOUNT -> SettlementAccountSheet(
                    onSaved = {
                        activeSheet = null
                        viewModel.onAction(MyInfoAction.AccountUpdated)
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
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = {
                Text(
                    text = "로그아웃 할까요?",
                    color = PassmateColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "다시 로그인하면 기록과 코인은 그대로 있어요.",
                    color = PassmateColors.TextSecondary,
                    fontSize = 14.sp,
                    letterSpacing = (-0.28).sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirm = false
                        viewModel.onAction(MyInfoAction.ConfirmSignOut)
                    }
                ) {
                    Text(text = "로그아웃", color = PassmateColors.WeakTopicText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text(text = "취소", color = PassmateColors.TextSecondary)
                }
            },
            containerColor = PassmateColors.Surface
        )
    }
}

@Composable
private fun MyInfoContentScreen(
    uiState: MyInfoUiState,
    onAction: (MyInfoAction) -> Unit,
    onClickSignOut: () -> Unit
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
                onClickSignOut = onClickSignOut
            )
        }
    }
}

@Composable
private fun LoadedMyInfo(
    uiState: MyInfoUiState,
    onAction: (MyInfoAction) -> Unit,
    onClickSignOut: () -> Unit
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
                text = "마이",
                color = PassmateColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.48).sp
            )
            Text(
                text = "설정",
                color = PassmateColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.28).sp,
                modifier = Modifier
                    .clickable { onAction(MyInfoAction.ClickSettings) }
                    .padding(4.dp)
            )
        }
        if (uiState.hasPartialFailure) {
            PartialFailureBanner()
        }
        uiState.profile?.let { profile ->
            ProfileCard(
                profile = profile,
                onClick = { onAction(MyInfoAction.ClickProfile) }
            )
            SectionCard {
                InfoRow(
                    title = "닉네임",
                    subtitle = profile.nickname,
                    actionLabel = "변경",
                    onClick = { onAction(MyInfoAction.ClickEditProfile) }
                )
                RowDivider()
                InfoRow(
                    title = "내 캐릭터",
                    subtitle = StudentAvatars.nameOf(profile.avatarId ?: StudentAvatars.DEFAULT_ID),
                    actionLabel = "변경",
                    onClick = { onAction(MyInfoAction.ClickEditProfile) }
                )
            }
            // 코인·정산은 카드 단위로만 실패시킨다 — 프로필이 정상이면 화면 전체를 덮지 않는다 (규칙 §9)
            if (uiState.isCoinInfoFailed) {
                FailureCard(
                    message = "코인 정보를 불러오지 못했어요",
                    onRetry = { onAction(MyInfoAction.RetryCoinInfo) }
                )
            } else {
                SectionCard {
                    CoinRow(
                        coins = profile.coins,
                        onClickCharge = { onAction(MyInfoAction.ClickCharge) }
                    )
                    RowDivider()
                    InfoRow(
                        title = "결제 수단",
                        subtitle = paymentMethodSubtitle(uiState),
                        actionLabel = "관리",
                        onClick = { onAction(MyInfoAction.ClickPaymentMethod) }
                    )
                    RowDivider()
                    InfoRow(
                        title = "코인 내역",
                        subtitle = recentTransactionSubtitle(uiState),
                        actionLabel = "보기",
                        onClick = { onAction(MyInfoAction.ClickCoinHistory) }
                    )
                }
            }
            if (uiState.isEarningsFailed) {
                FailureCard(
                    message = "정산 정보를 불러오지 못했어요",
                    onRetry = { onAction(MyInfoAction.RetryEarnings) }
                )
            } else {
                SectionCard {
                    InfoRow(
                        title = "정산 계좌",
                        subtitle = settlementAccountSubtitle(uiState),
                        actionLabel = "변경",
                        onClick = { onAction(MyInfoAction.ClickSettlementAccount) }
                    )
                    RowDivider()
                    InfoRow(
                        title = "이번 달 정산 예정",
                        subtitle = nextPayoutSubtitle(uiState),
                        actionLabel = "내역",
                        onClick = { onAction(MyInfoAction.ClickEarnings) }
                    )
                }
            }
            SectionCard {
                InfoRow(
                    title = "알림 설정",
                    subtitle = "세션 시작 · 별점 요청 · 정산",
                    actionLabel = "변경",
                    onClick = { onAction(MyInfoAction.ClickNotifications) }
                )
                RowDivider()
                // 확인 다이얼로그를 거쳐야 실제 로그아웃 — 다이얼로그 소유는 상위 Screen (규칙 §11-1)
                InfoRow(
                    title = "로그아웃",
                    subtitle = null,
                    actionLabel = "",
                    actionColor = PassmateColors.Destructive,
                    onClick = {
                        if (!uiState.isProcessing) {
                            onClickSignOut()
                        }
                    }
                )
                RowDivider()
                InfoRow(
                    title = "회원 탈퇴",
                    subtitle = null,
                    actionLabel = "",
                    actionColor = PassmateColors.Destructive,
                    onClick = { onAction(MyInfoAction.ClickDeleteAccount) }
                )
                RowDivider()
                InfoRow(
                    title = "약관 · 개인정보 처리방침",
                    subtitle = "버전 1.0.0",
                    actionLabel = "보기",
                    onClick = { onAction(MyInfoAction.ClickTerms) }
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: UserProfile,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(20.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
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
                text = "참여한 방 ${profile.joinedRoomCount ?: 0} · 내가 만든 방 ${profile.hostedRoomCount ?: 0}",
                color = PassmateColors.TextSecondary,
                fontSize = 13.sp,
                letterSpacing = (-0.26).sp
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        content()
    }
}

@Composable
private fun RowDivider() {
    Divider(color = PassmateColors.Border, thickness = 1.dp)
}

@Composable
private fun InfoRow(
    title: String,
    subtitle: String?,
    actionLabel: String,
    actionColor: Color = PassmateColors.PrimaryDeep,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = PassmateColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.3).sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = PassmateColors.TextSecondary,
                    fontSize = 13.sp,
                    letterSpacing = (-0.26).sp
                )
            }
        }
        Text(
            text = if (actionLabel.isEmpty()) "›" else "$actionLabel ›",
            color = actionColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun CoinRow(
    coins: Long?,
    onClickCharge: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "보유 코인",
                color = PassmateColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = "${formatNumber(coins ?: 0L)} C · 유료 방 참가비에 사용",
                color = PassmateColors.TextSecondary,
                fontSize = 13.sp,
                letterSpacing = (-0.26).sp
            )
        }
        Text(
            text = "코인 충전",
            color = PassmateColors.Surface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(PassmateColors.Primary, RoundedCornerShape(12.dp))
                .clickable(onClick = onClickCharge)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

// 카드 하나라도 실패했을 때의 상단 안내 (시안 M-12e banner/부분 실패)
@Composable
private fun PartialFailureBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(PassmateColors.ErrorIconBg, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "일부 정보를 불러오지 못했어요 · 아래에서 다시 시도",
            color = PassmateColors.WrongPinkText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.13).sp
        )
    }
}

// 카드 단위 실패 자리표시자 (시안 M-12e card/실패) — 해당 섹션만 다시 불러온다
@Composable
private fun FailureCard(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(16.dp))
            .background(PassmateColors.Surface, RoundedCornerShape(16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AlertCircleIcon()
        Text(
            text = message,
            color = PassmateColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.14).sp,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = "다시 시도",
            color = PassmateColors.PrimaryDeep,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.13).sp,
            modifier = Modifier
                .padding(top = 2.dp)
                .clickable(onClick = onRetry)
                .padding(8.dp)
        )
    }
}

// 시안 icon/alert-circle — 원 테두리 + 느낌표 (아이콘 세트 의존 없이 직접 그린다)
@Composable
private fun AlertCircleIcon() {
    Canvas(modifier = Modifier.size(22.dp)) {
        val strokeWidth = 2.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val iconColor = PassmateColors.TextTertiary

        drawCircle(
            color = iconColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        drawLine(
            color = iconColor,
            start = Offset(center.x, center.y - radius * 0.45f),
            end = Offset(center.x, center.y + radius * 0.1f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = iconColor,
            radius = strokeWidth / 2f,
            center = Offset(center.x, center.y + radius * 0.45f)
        )
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

// 실패는 카드 자체가 FailureCard로 대체되므로 여기서는 성공·빈 값만 다룬다
private fun paymentMethodSubtitle(uiState: MyInfoUiState): String {
    val method = uiState.defaultMethod

    return if (method != null) {
        "${method.label} · 포트원 안전결제"
    } else {
        "기본 결제 수단을 설정해 주세요"
    }
}

private fun recentTransactionSubtitle(uiState: MyInfoUiState): String {
    val recent = uiState.recentTransaction

    return if (recent != null) {
        "최근 ${shortDate(recent)} ${signedCoins(recent.amount)} C"
    } else {
        "아직 내역이 없어요"
    }
}

private fun settlementAccountSubtitle(uiState: MyInfoUiState): String {
    val account = uiState.settlementAccount

    return if (account != null) {
        "${account.bankName} ${account.maskedNumber}"
    } else {
        "계좌를 등록해 주세요"
    }
}

private fun nextPayoutSubtitle(uiState: MyInfoUiState): String {
    val payout = uiState.nextPayout

    return if (payout != null) {
        "₩${formatNumber(payout.amount)} · ${payout.dateLabel} 지급"
    } else {
        "정산 예정 없음"
    }
}

// "2026-08-22T10:00:00Z" → "8/22". 파싱 실패 시 원문 앞 10자
private fun shortDate(transaction: CoinTransaction): String {
    val raw = transaction.createdAt ?: return ""
    val parts = raw.take(10).split("-")

    return if (parts.size == 3) {
        "${parts[1].trimStart('0')}/${parts[2].trimStart('0')}"
    } else {
        raw.take(10)
    }
}

private fun signedCoins(amount: Int): String {
    return if (amount > 0) {
        "+${formatNumber(amount.toLong())}"
    } else {
        "-${formatNumber(-amount.toLong())}"
    }
}

private fun formatNumber(value: Long): String {
    return value.toString().reversed().chunked(3).joinToString(",").reversed()
}

// --- Preview ---

@PassmatePreview
@Composable
private fun MyInfoContentScreenPreview() {
    PassmateTheme {
        MyInfoContentScreen(
            uiState = MyInfoUiState(
                isLoading = false,
                profile = UserProfile(
                    nickname = "준영",
                    email = "junyoung@example.com",
                    joinedAt = "2026-08-01",
                    avatarId = 1,
                    level = HostLevel.GROWING,
                    coins = 1200L,
                    joinedRoomCount = 32,
                    hostedRoomCount = 12
                ),
                defaultMethod = PaymentMethod.KAKAO_PAY,
                settlementAccount = SettlementAccountSummary(
                    bankName = "국민",
                    maskedNumber = "***-***-4821",
                    payoutNote = null
                ),
                nextPayout = NextPayout(dateLabel = "9/5", amount = 64000L)
            ),
            onAction = {},
            onClickSignOut = {}
        )
    }
}

// 코인·정산 카드만 실패 — 프로필은 정상이라 전체 에러로 처리하지 않는다 (규칙 §9)
@PassmatePreview
@Composable
private fun MyInfoContentScreenPartialFailurePreview() {
    PassmateTheme {
        MyInfoContentScreen(
            uiState = MyInfoUiState(
                isLoading = false,
                profile = UserProfile(
                    nickname = "준영",
                    email = null,
                    joinedAt = null,
                    avatarId = 1,
                    level = null,
                    coins = null,
                    joinedRoomCount = null,
                    hostedRoomCount = null
                ),
                isCoinInfoFailed = true,
                isEarningsFailed = true
            ),
            onAction = {},
            onClickSignOut = {}
        )
    }
}

// 프로필 로드 실패 — 전체 에러
@PassmatePreview
@Composable
private fun MyInfoContentScreenFailedPreview() {
    PassmateTheme {
        MyInfoContentScreen(
            uiState = MyInfoUiState(isLoading = false, loadFailed = true),
            onAction = {},
            onClickSignOut = {}
        )
    }
}
