package org.sesacteamproject.passmate.theme

import androidx.compose.ui.graphics.Color

// 시맨틱 토큰 — 화면 코드 hex 하드코딩 금지 (규칙 §11-2). iOS PassmateColors.swift와 1:1 유지
object PassmateColors {

    val Primary = Color(0xFF17B884)

    val PrimaryDeep = Color(0xFF0E8A63)

    val BackgroundMint = Color(0xFFEAF8F1)

    val Surface = Color(0xFFFFFFFF)

    // 스플래시(M-00) 브랜드 배경 위 텍스트 — 흰색 80% / 50%
    val SplashSubtleText = Color(0xCCFFFFFF)

    val SplashFaintText = Color(0x80FFFFFF)

    // 입력 필드·비활성 칩 배경 (v6 M-01 PIN 박스·닉네임 필드)
    val FieldGray = Color(0xFFF3F4F6)

    // 빈 상태 일러스트 원형 배경 (v6 M-08 참여한 방 빈 상태) — FieldGray보다 한 톤 밝다
    val EmptyIconBg = Color(0xFFF6F6F7)

    val TextPrimary = Color(0xFF1B1F24)

    val TextSecondary = Color(0xFF6B7280)

    val TextTertiary = Color(0xFFA0A6B0)

    val Border = Color(0xFFE5E7EB)

    // 로딩 스켈레톤 블록 (시안 "07 · 로딩 · 스켈레톤" 규격) — 민트 계열을 절대 쓰지 않는다.
    // 로딩은 상태가 아니라 대기라서 브랜드색으로 강조하지 않는다.
    val SkeletonBlock = Color(0xFFE7E9EC)

    // 스켈레톤 보조 블록 — 캡션·부가 정보 자리
    val SkeletonBlockSoft = Color(0xFFF1F3F5)

    // 스켈레톤 카드 바탕 (테두리 없는 히어로 자리)
    val SkeletonSurface = Color(0xFFF6F6F7)

    // 진행 타이머·프로그레스 (v6 M-03 타이머 링·문항 세그먼트)
    val TimerAmber = Color(0xFFF3B440)

    // 남은 시간 진행 바의 바탕 (v6 M-03·M-T2) — TimerAmber를 연하게 깐 트랙
    val TimerTrack = Color(0xFFFDEBCF)

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

    // 오류 아이콘 원형 배경 — WrongPink 계열 연한 톤 (v6 M-05e 리포트 불러오기 실패·E-List 목록 실패·M-14 미제출 카드). 대응 텍스트는 WrongPinkText
    val ErrorIconBg = Color(0xFFFDEEEF)

    val WeakTopicBg = Color(0xFFFDEFDE)

    val WeakTopicText = Color(0xFFBF3F0C)

    // 정답률 분포 41~60% 구간 막대 (v6 M-14 개요) — 0~40% WrongPink · 61~80% RatingTagSelectedBg · 81~100% Primary
    val AccuracyBandMid = Color(0xFFFFD79A)

    // 파괴적 동작(로그아웃·회원 탈퇴) — 시안 M-12 card/기타
    val Destructive = Color(0xFFD9534F)

    // 별점 전용 골드 (디자인 시스템 §StarRating — 별점에만 허용)
    val StarGold = Color(0xFFF2C94C)

    // 명성 뱃지·평가 태그 선택 (v6 M-06 v2 별점 시트·M-10 프로필)
    val ReputationBadgeBg = Color(0xFFC4EEDB)

    val ReputationBadgeText = Color(0xFF0B6B4C)

    val RatingTagSelectedBg = Color(0xFFD6F3E6)

    val RatingTagSelectedText = Color(0xFF0B6B4C)

    // 목록 필터 칩 선택 배경 (v6 M-12-9 코인 내역 전체/충전/사용)
    val FilterChipSelectedBg = Color(0xFFD6F3E6)

    // 목록 불러오기 실패 아이콘 틴트 (v6 E-List 공통 패턴)
    val ErrorIconTint = Color(0xFF7C1F26)
    // 업적 뱃지 타일 테두리 (v6 M-09 명성 · 뱃지 컬렉션)
    val AchievementBadgeBorder = Color(0xFFBFEBD8)

    // 서드파티 브랜드 색상 (규칙 §11-2 예외 허용 대상을 토큰으로 관리)
    val BrandGoogleBlue = Color(0xFF4285F4)

    val BrandAppleBlack = Color(0xFF111111)
}
