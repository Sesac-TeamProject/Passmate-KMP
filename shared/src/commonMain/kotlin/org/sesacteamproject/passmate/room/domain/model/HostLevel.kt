package org.sesacteamproject.passmate.room.domain.model

// 호스트 명성 등급 Lv.1~5 (기획서 §13.3) — 방 정보·별점 시트·프로필 뱃지 표시용
enum class HostLevel(val level: Int, val label: String) {
    SEEDLING(1, "새싹"),
    GROWING(2, "성장"),
    VERIFIED(3, "검증된 운영자"),
    POPULAR(4, "인기 운영자"),
    MASTER(5, "마스터");

    companion object {

        fun from(level: Int?): HostLevel? {
            return entries.firstOrNull { it.level == level }
        }
    }
}
