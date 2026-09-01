package org.sesacteamproject.passmate.ui.auth

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassyMascot
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// Figma "UI 디자인 v6" M-Login(349:9040) 기준 — 민트 히어로 + 하단 로그인 시트
@Composable
fun SignInScreen(
    viewModel: SignInViewModel = koinScreenViewModel(),
    oauthAccessToken: String? = null,
    oauthRefreshToken: String? = null,
    onNavigate: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(oauthAccessToken, oauthRefreshToken) {
        if (!oauthAccessToken.isNullOrBlank() && !oauthRefreshToken.isNullOrBlank()) {
            viewModel.onAction(SignInAction.ReceiveOAuthCallback(oauthAccessToken, oauthRefreshToken))
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is SignInEvent.OpenSignInPage -> openSignInPage(event.url)
                is SignInEvent.SignInCompleted -> onNavigate(NavigationAction.NavigateAfterSignIn)
                is SignInEvent.GuestEnterRequested -> onNavigate(NavigationAction.NavigateToJoin())
                is SignInEvent.ShowNotice -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        SignInContentScreen(
            uiState = uiState,
            onAction = viewModel::onAction
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SignInContentScreen(
    uiState: SignInUiState,
    onAction: (SignInAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.BackgroundMint)
    ) {
        SignInHero(modifier = Modifier.weight(1f))
        SignInSheet(
            uiState = uiState,
            onAction = onAction
        )
    }
}

@Composable
private fun SignInHero(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 28.dp, top = 96.dp, end = 28.dp, bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PassyMascot(modifier = Modifier.size(width = 120.dp, height = 132.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(PassmateColors.Primary, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "P",
                    color = PassmateColors.Surface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "패스메이트",
                color = PassmateColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.56).sp
            )
        }
        Text(
            text = "혼자 시작한 공부,\n함께하는 합격까지.",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            letterSpacing = (-0.28).sp,
            lineHeight = 19.6.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SignInSheet(
    uiState: SignInUiState,
    onAction: (SignInAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = PassmateColors.Surface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .navigationBarsPadding()
            .padding(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GoogleSignInButton(
            isSigningIn = uiState.isSigningIn,
            onClick = { onAction(SignInAction.ClickGoogleSignIn) }
        )
        AppleSignInButton(
            isSigningIn = uiState.isSigningIn,
            onClick = { onAction(SignInAction.ClickAppleSignIn) }
        )
        OrDivider()
        GuestEnterButton(
            isSigningIn = uiState.isSigningIn,
            onClick = { onAction(SignInAction.ClickGuestEnter) }
        )
        Text(
            text = "계속하면 이용약관과 개인정보 처리방침에 동의한 것으로 봅니다\n선생님·학생 공용 계정 · 게스트 기록은 세션 후 사라져요",
            color = PassmateColors.TextTertiary,
            fontSize = 12.sp,
            letterSpacing = (-0.24).sp,
            lineHeight = 16.8.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GoogleSignInButton(
    isSigningIn: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, PassmateColors.Border, RoundedCornerShape(14.dp))
            .clickable(enabled = !isSigningIn, onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSigningIn) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = PassmateColors.Primary,
                strokeWidth = 2.dp
            )
        } else {
            GoogleSignInIcon(modifier = Modifier.size(24.dp))
        }
        Text(
            text = "Google로 계속하기",
            color = PassmateColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun AppleSignInButton(
    isSigningIn: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.BrandAppleBlack, RoundedCornerShape(14.dp))
            .clickable(enabled = !isSigningIn, onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isSigningIn) {
            AppleSignInIcon(modifier = Modifier.size(20.dp))
        }
        Text(
            text = "Apple로 계속하기",
            color = PassmateColors.Surface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(PassmateColors.Border)
        )
        Text(
            text = "또는",
            color = PassmateColors.TextTertiary,
            fontSize = 12.sp,
            letterSpacing = (-0.24).sp
        )
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(PassmateColors.Border)
        )
    }
}

@Composable
private fun GuestEnterButton(
    isSigningIn: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PassmateColors.BackgroundMint, RoundedCornerShape(14.dp))
            .clickable(enabled = !isSigningIn, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "PIN으로 바로 입장 (게스트)",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.28).sp
        )
    }
}

@Composable
internal expect fun GoogleSignInIcon(modifier: Modifier = Modifier)

@Composable
internal expect fun AppleSignInIcon(modifier: Modifier = Modifier)

// --- Preview ---

@PassmatePreview
@Composable
private fun SignInContentScreenPreview() {
    PassmateTheme {
        SignInContentScreen(
            uiState = SignInUiState(),
            onAction = {}
        )
    }
}

// 소셜 로그인 진행 중 — 버튼 비활성 + 스피너
@PassmatePreview
@Composable
private fun SignInContentScreenSigningInPreview() {
    PassmateTheme {
        SignInContentScreen(
            uiState = SignInUiState(isSigningIn = true),
            onAction = {}
        )
    }
}
