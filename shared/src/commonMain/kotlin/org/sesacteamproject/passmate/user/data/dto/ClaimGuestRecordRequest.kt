package org.sesacteamproject.passmate.user.data.dto

import kotlinx.serialization.Serializable

// POST /guest-records/claim — 가입 직후 게스트 participantId를 계정에 연동 (FR-036)
@Serializable
data class ClaimGuestRecordRequest(
    val participantId: Long
)
