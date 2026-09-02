package org.sesacteamproject.passmate.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sesacteamproject.passmate.theme.PassmateColors

// 빈 상태 블록 치수·타이포 — iOS PassmateEmptyStateView.swift의 PassmateEmptyStateSpec과 1:1
// (iOS는 lineHeight가 없어 파생값 guideLineSpacing 1개가 더 있다).
// 자간은 이 리포 관례대로 폰트 크기의 -1%다 (19→-0.19 · 14→-0.14 · 16→-0.16)
private object PassmateEmptyStateSpec {

    val SectionPaddingVertical = 40.dp

    val IconCircleSize = 64.dp

    val IconSize = 28.dp

    val TitleTopPadding = 16.dp

    val TitleFontSize = 19.sp

    val TitleLetterSpacing = (-0.19).sp

    val GuideTopPadding = 8.dp

    val GuideFontSize = 14.sp

    val GuideLineHeight = 23.1.sp

    val GuideLetterSpacing = (-0.14).sp

    val CtaTopPadding = 24.dp

    val CtaWidth = 200.dp

    val CtaHeight = 52.dp

    val CtaCornerRadius = 14.dp

    val CtaFontSize = 16.sp

    val CtaLetterSpacing = (-0.16).sp
}

// 목록 빈 상태 블록 (v6) — 아이콘 원형 · 제목 · 안내 문구 · CTA.
// 참여한 방(M-08)·정산 빈 상태 2종(M-T4)이 같은 시안을 쓴다. 문구와 아이콘만 화면이 정하고
// 치수·타이포는 여기서 고정한다 (규칙 §11 공통 컴포넌트 승격).
@Composable
fun PassmateEmptyState(
    icon: PassmateIcons,
    iconTint: Color,
    title: String,
    guide: String,
    ctaLabel: String,
    onClickCta: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = PassmateEmptyStateSpec.SectionPaddingVertical),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(PassmateEmptyStateSpec.IconCircleSize)
                .background(PassmateColors.EmptyIconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            PassmateIcon(
                icon = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(PassmateEmptyStateSpec.IconSize)
            )
        }
        Text(
            text = title,
            color = PassmateColors.TextPrimary,
            fontSize = PassmateEmptyStateSpec.TitleFontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = PassmateEmptyStateSpec.TitleLetterSpacing,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = PassmateEmptyStateSpec.TitleTopPadding)
        )
        Text(
            text = guide,
            color = PassmateColors.TextSecondary,
            fontSize = PassmateEmptyStateSpec.GuideFontSize,
            lineHeight = PassmateEmptyStateSpec.GuideLineHeight,
            letterSpacing = PassmateEmptyStateSpec.GuideLetterSpacing,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = PassmateEmptyStateSpec.GuideTopPadding)
        )
        Row(
            modifier = Modifier
                .padding(top = PassmateEmptyStateSpec.CtaTopPadding)
                .width(PassmateEmptyStateSpec.CtaWidth)
                .height(PassmateEmptyStateSpec.CtaHeight)
                .background(
                    PassmateColors.Primary,
                    RoundedCornerShape(PassmateEmptyStateSpec.CtaCornerRadius)
                )
                .clickable(onClick = onClickCta),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ctaLabel,
                color = PassmateColors.Surface,
                fontSize = PassmateEmptyStateSpec.CtaFontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = PassmateEmptyStateSpec.CtaLetterSpacing
            )
        }
    }
}
