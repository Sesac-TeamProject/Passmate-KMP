package org.sesacteamproject.passmate.core.network.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// STOMP 이벤트 페이로드의 서버 형태 — 백엔드 `SessionEvents.kt`와 1:1.
// 화면이 쓰는 ServerEvent와 이름이 달라(orderNo/content, avatarId 문자열 키)
// 여기서 받아 ServerEventDecoder가 매핑한다 (DTO → 도메인, 규칙 §6).
internal object ServerEventPayloads {

    @Serializable
    data class QuestionStarted(
        val sessionQuestionId: Long = 0,
        val questionId: Long = 0,
        val orderNo: Int = 0,
        val totalCount: Int = 0,
        val type: String = "",
        val content: String = "",
        val choices: List<String>? = null,
        val points: Int = 0,
        val timeLimitSec: Int = 0,
        val endsAt: String = ""
    )

    // 정답·해설이 평평하게 온다 (정답은 이 이벤트에서만 — 규칙 §13)
    @Serializable
    data class QuestionEnded(
        val sessionQuestionId: Long = 0,
        val questionId: Long = 0,
        val orderNo: Int = 0,
        val answer: String? = null,
        val explanation: String? = null,
        val submitCount: Int = 0,
        val correctCount: Int = 0,
        val correctRate: Double = 0.0,
        val distribution: Map<String, Int> = emptyMap()
    )

    @Serializable
    data class RankingEntry(
        val rank: Int = 0,
        val participantId: Long = 0,
        val nickname: String = "",
        // 문자열 키 — 화면 인덱스로 바꾼다
        val avatarId: String? = null,
        val totalScore: Long = 0
    )

    // 참가자 입·퇴장 — 서버 kr.passmate.session.dto.ParticipantEventPayload 와 1:1.
    // 식별자 키는 `id`이고, 현재 인원(count)과 퇴장 사유(reason)는 서버가 싣지 않는다.
    // 인원은 화면이 명단 길이로 세고, reason은 서버가 실어 주면 그대로 동작한다.
    @Serializable
    data class ParticipantJoined(
        @SerialName("id") val participantId: Long = 0,
        val nickname: String = "",
        val isGuest: Boolean = false,
        val avatarId: String? = null
    )

    @Serializable
    data class ParticipantLeft(
        @SerialName("id") val participantId: Long = 0,
        val reason: String? = null
    )

    @Serializable
    data class SubmissionStatus(
        val sessionQuestionId: Long = 0,
        val submitCount: Int = 0,
        val participantCount: Int = 0,
        val correctCount: Int = 0,
        val correctRate: Double = 0.0,
        val distribution: Map<String, Int> = emptyMap()
    )

    @Serializable
    data class ScreenLock(
        val locked: Boolean = false
    )
}
