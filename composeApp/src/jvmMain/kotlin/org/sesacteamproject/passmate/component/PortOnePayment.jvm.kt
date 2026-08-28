package org.sesacteamproject.passmate.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.theme.PassmateColors

// Desktop은 포트원 결제창(웹뷰) 미지원 — 실패로 처리하고 모바일 결제 안내만 노출한다.
@Composable
actual fun PortOnePaymentView(
    request: PortOneRequest,
    onResult: (PortOneResult) -> Unit
) {
    LaunchedEffect(request.paymentId) {
        onResult(PortOneResult.Failure("데스크톱에서는 결제를 지원하지 않아요. 모바일 앱에서 진행해 주세요"))
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "결제는 모바일 앱에서 진행해 주세요",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
