package org.sesacteamproject.passmate.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.component.PassyMascot
import org.sesacteamproject.passmate.navigation.NavigationAction
import org.sesacteamproject.passmate.theme.PassmateColors

// 임시 셸 화면 — 본 구현은 T037(PIN 입장)·T098(공개 방 목록)에서 M-01 v6 디자인으로 대체된다
@Composable
fun HomeScreen(onNavigate: (NavigationAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PassmateColors.BackgroundMint),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PassyMascot(modifier = Modifier.size(width = 80.dp, height = 88.dp))
        Text(
            text = "패스메이트",
            color = PassmateColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = "홈 화면은 준비 중이에요",
            color = PassmateColors.TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(
            modifier = Modifier
                .padding(top = 24.dp)
                .background(PassmateColors.Primary, RoundedCornerShape(14.dp))
                .clickable { onNavigate(NavigationAction.NavigateToJoin()) }
                .padding(horizontal = 32.dp, vertical = 14.dp)
        ) {
            Text(
                text = "PIN으로 입장하기",
                color = PassmateColors.Surface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = "방 찾기",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(top = 12.dp)
                .clickable { onNavigate(NavigationAction.NavigateToRoomList) }
                .padding(4.dp)
        )
        Text(
            text = "내 학습 기록",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable { onNavigate(NavigationAction.NavigateToMyInfo) }
                .padding(4.dp)
        )
        Text(
            text = "로그인",
            color = PassmateColors.PrimaryDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable { onNavigate(NavigationAction.NavigateToSignIn) }
                .padding(4.dp)
        )
    }
}
