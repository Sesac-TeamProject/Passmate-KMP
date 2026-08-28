package org.sesacteamproject.passmate.room.domain.model

// WAITING → RUNNING → FINISHED (규칙 §2-1-2). 서버 wire 값 ENDED는 FINISHED로 매핑한다
enum class RoomStatus {
    WAITING,
    RUNNING,
    FINISHED,
    UNKNOWN;

    companion object {

        fun from(raw: String?): RoomStatus {
            return when (raw?.uppercase()) {
                "WAITING" -> WAITING
                "RUNNING" -> RUNNING
                "FINISHED", "ENDED" -> FINISHED
                else -> UNKNOWN
            }
        }
    }
}
