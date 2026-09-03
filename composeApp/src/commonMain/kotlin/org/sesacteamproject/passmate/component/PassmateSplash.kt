package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.preview.PassmatePreview
import org.sesacteamproject.passmate.theme.PassmateColors
import org.sesacteamproject.passmate.theme.PassmateTheme

// 앱 실행 스플래시 (시안 M-00) — 로고·워드마크·태그라인·버전.
// Android 네이티브 SplashScreen API는 배경색+아이콘만 지원해서 워드마크·태그라인을 담지 못한다.
// 그래서 네이티브 스플래시가 걷힌 뒤 이 화면이 이어받는다.
@Composable
fun PassmateSplash(
    versionLabel: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PassmateColors.Primary)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PassmateBrandLogo(
                contentDescription = null,
                modifier = Modifier.size(LogoSize)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "PASSMATE",
                color = PassmateColors.Surface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.36.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "혼자 시작한 공부, 함께하는 합격까지.",
                color = PassmateColors.SplashSubtleText,
                fontSize = 14.sp,
                letterSpacing = (-0.14).sp,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = versionLabel,
            color = PassmateColors.SplashFaintText,
            fontSize = 11.sp,
            letterSpacing = (-0.11).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        )
    }
}

private val LogoSize = 104.dp

// --- Preview ---

@PassmatePreview
@Composable
private fun PassmateSplashPreview() {
    PassmateTheme {
        PassmateSplash(versionLabel = "v1.0")
    }
}
