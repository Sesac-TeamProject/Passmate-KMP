package org.sesacteamproject.passmate.session.data.dto

import kotlinx.serialization.Serializable

// content — 객관식: 선택한 보기 원문, OX: "O"|"X", 서술형: 자유 텍스트 (contracts §Session)
@Serializable
data class SubmitAnswerRequest(
    val content: String
)
