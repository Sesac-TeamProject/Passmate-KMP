package org.sesacteamproject.passmate.session.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/{roomId}/session/hints 응답 — 다시 듣기·재접속 복구용 (FR-040·041)
@Serializable
data class VoiceHintsResponse(
    val hints: List<Entry> = emptyList()
) {

    @Serializable
    data class Entry(
        val hintId: Long,
        val questionNo: Int,
        val clipUrl: String,
        val durationMs: Long = 0L
    )
}
