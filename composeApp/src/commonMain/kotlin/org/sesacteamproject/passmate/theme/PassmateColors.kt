package org.sesacteamproject.passmate.theme

import androidx.compose.ui.graphics.Color

// 시맨틱 토큰 — 화면 코드 hex 하드코딩 금지 (규칙 §11-2). iOS PassmateColors.swift와 1:1 유지
object PassmateColors {

    val Primary = Color(0xFF17B884)

    val PrimaryDeep = Color(0xFF0E8A63)

    val BackgroundMint = Color(0xFFEAF8F1)

    val Surface = Color(0xFFFFFFFF)

    val TextPrimary = Color(0xFF1B1F24)

    val TextSecondary = Color(0xFF6B7280)

    val TextTertiary = Color(0xFFA0A6B0)

    val Border = Color(0xFFE5E7EB)

    // 서드파티 브랜드 색상 (규칙 §11-2 예외 허용 대상을 토큰으로 관리)
    val BrandGoogleBlue = Color(0xFF4285F4)

    val BrandAppleBlack = Color(0xFF111111)
}
