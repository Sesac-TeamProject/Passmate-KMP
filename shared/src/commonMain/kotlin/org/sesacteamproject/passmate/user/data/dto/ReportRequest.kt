package org.sesacteamproject.passmate.user.data.dto

import kotlinx.serialization.Serializable

// POST /reports 요청 — contracts §평가·등급과 1:1 (게스트 익명 신고 가능)
@Serializable
data class ReportRequest(
    val targetType: String,
    val targetId: Long,
    val reason: String,
    val detail: String? = null
)
