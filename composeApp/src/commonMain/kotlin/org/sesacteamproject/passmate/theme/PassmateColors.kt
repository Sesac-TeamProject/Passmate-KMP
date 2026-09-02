package org.sesacteamproject.passmate.theme

import androidx.compose.ui.graphics.Color

// 시맨틱 토큰 — 화면 코드 hex 하드코딩 금지 (규칙 §11-2). iOS PassmateColors.swift와 1:1 유지
object PassmateColors {

    val Primary = Color(0xFF17B884)

    val PrimaryDeep = Color(0xFF0E8A63)

    val BackgroundMint = Color(0xFFEAF8F1)

    val Surface = Color(0xFFFFFFFF)

    // 입력 필드·비활성 칩 배경 (v6 M-01 PIN 박스·닉네임 필드)
    val FieldGray = Color(0xFFF3F4F6)

    val TextPrimary = Color(0xFF1B1F24)

    val TextSecondary = Color(0xFF6B7280)

    val TextTertiary = Color(0xFFA0A6B0)

    val Border = Color(0xFFE5E7EB)

    // 진행 타이머·프로그레스 (v6 M-03 타이머 링·문항 세그먼트)
    val TimerAmber = Color(0xFFF3B440)

    // 짙은 잉크 그린 — 민트 배경 위 라벨 (v6 M-05 최종 결과)
    val InkGreen = Color(0xFF0F3D2E)

    // 순위·선지 칩 파스텔 4색 + 대응 텍스트 (v6 M-03 선지·M-05 포디움/랭킹)
    val ChipBlue = Color(0xFFAAC6F6)

    val ChipBlueText = Color(0xFF173872)

    val ChipGold = Color(0xFFF7DBA1)

    val ChipGoldText = Color(0xFF6E4C06)

    val ChipGreen = Color(0xFFA9DEC3)

    val ChipGreenText = Color(0xFF14523B)

    val ChipOrange = Color(0xFFF8C6A4)

    val ChipOrangeText = Color(0xFF7A3A11)

    // 오답 칩·보완 주제 (v6 M-06 리포트)
    val WrongPink = Color(0xFFF7ADB1)

    val WrongPinkText = Color(0xFF7C1F26)

    // 오류 아이콘 원형 배경 — WrongPink 계열 연한 톤 (v6 M-05e 리포트 불러오기 실패)
    val ErrorIconBg = Color(0xFFFDEEEF)

    val WeakTopicBg = Color(0xFFFDEFDE)

    val WeakTopicText = Color(0xFFBF3F0C)

    // 파괴적 동작(로그아웃·회원 탈퇴) — 시안 M-12 card/기타
    val Destructive = Color(0xFFD9534F)

    // 별점 전용 골드 (디자인 시스템 §StarRating — 별점에만 허용)
    val StarGold = Color(0xFFF2C94C)

    // 명성 뱃지·평가 태그 선택 (v6 M-06 v2 별점 시트·M-10 프로필)
    val ReputationBadgeBg = Color(0xFFC4EEDB)

    val ReputationBadgeText = Color(0xFF0B6B4C)

    val RatingTagSelectedBg = Color(0xFFD6F3E6)

    val RatingTagSelectedText = Color(0xFF0B6B4C)

    // 서드파티 브랜드 색상 (규칙 §11-2 예외 허용 대상을 토큰으로 관리)
    val BrandGoogleBlue = Color(0xFF4285F4)

    val BrandAppleBlack = Color(0xFF111111)
}
