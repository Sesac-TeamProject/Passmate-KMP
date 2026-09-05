package org.sesacteamproject.passmate.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

// 웹뷰에 넣는 HTML은 CSS 색 문자열이 필요하다. 화면 코드에 hex를 다시 적으면 토큰과 갈라지므로
// (규칙 §11-2) 토큰을 여기서 문자열로 바꿔 쓴다. iOS 미러는 PassmateColors.swift의 cssHex다
fun Color.toCssHex(): String {
    // 반투명 토큰(스플래시 텍스트 등)을 불투명으로 삼켜버리지 않도록 alpha가 1이 아니면 #RRGGBBAA로 낸다
    val channels = if (alpha < 1f) listOf(red, green, blue, alpha) else listOf(red, green, blue)

    return channels.joinToString(prefix = "#", separator = "") { channel ->
        (channel * 255f).roundToInt().toString(16).padStart(2, '0').uppercase()
    }
}
