package org.sesacteamproject.passmate.core.network.event

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.sesacteamproject.passmate.room.domain.model.StudentAvatarKeys

// 백엔드 `SessionEvent<T>` 봉투: {type, roomId, occurredAt, payload}
// occurredAt이 재접속 시 "스냅샷 이전 이벤트 폐기" 판정에 쓰인다 (규칙 §2-1-2).
object ServerEventDecoder {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private inline fun <reified T> decodePayload(data: JsonElement): T {
        return json.decodeFromJsonElement(data)
    }

    private fun ServerEventPayloads.RankingEntry.toEvent(): ServerEvent.RankingEntry {
        return ServerEvent.RankingEntry(
            rank = rank,
            participantId = participantId,
            nickname = nickname,
            avatarId = StudentAvatarKeys.toIndex(avatarId),
            total = totalScore.toDouble()
        )
    }

    private fun decodeRanking(data: JsonElement): List<ServerEvent.RankingEntry> {
        return decodePayload<List<ServerEventPayloads.RankingEntry>>(data).map { it.toEvent() }
    }

    private fun decodeEvent(type: String, data: JsonElement): ServerEvent? {
        return when (type) {
            "PARTICIPANT_JOINED" -> decodePayload<ServerEventPayloads.ParticipantJoined>(data).let {
                ServerEvent.ParticipantJoined(
                    participantId = it.participantId,
                    nickname = it.nickname,
                    isGuest = it.isGuest,
                    avatarId = StudentAvatarKeys.toIndex(it.avatarId),
                    count = it.count
                )
            }
            "PARTICIPANT_LEFT" -> decodePayload<ServerEventPayloads.ParticipantLeft>(data).let {
                ServerEvent.ParticipantLeft(
                    participantId = it.participantId,
                    count = it.count,
                    reason = it.reason
                )
            }
            // 서버는 페이로드를 싣지 않는다 — 프레임을 버리면 Play로 넘어가지 못한다
            "SESSION_STARTED" -> ServerEvent.SessionStarted()
            "QUESTION_STARTED" -> decodePayload<ServerEventPayloads.QuestionStarted>(data).let {
                ServerEvent.QuestionStarted(
                    questionId = it.questionId,
                    questionNo = it.orderNo,
                    type = it.type,
                    body = it.content,
                    choices = it.choices,
                    points = it.points,
                    timeLimitSec = it.timeLimitSec,
                    endsAt = it.endsAt
                )
            }
            "QUESTION_ENDED" -> decodePayload<ServerEventPayloads.QuestionEnded>(data).let {
                ServerEvent.QuestionEnded(
                    questionNo = it.orderNo,
                    answerReveal = ServerEvent.QuestionEnded.AnswerReveal(
                        answer = it.answer,
                        explanation = it.explanation,
                        distribution = it.distribution
                    ),
                    correctCount = it.correctCount
                )
            }
            "RANKING_UPDATED" -> ServerEvent.RankingUpdated(ranking = decodeRanking(data))
            "SESSION_ENDED" -> ServerEvent.SessionEnded(finalRanking = decodeRanking(data))
            "SCREEN_LOCKED" -> decodePayload<ServerEventPayloads.ScreenLock>(data).let {
                ServerEvent.ScreenLocked(locked = it.locked)
            }
            "SUBMISSION_UPDATED" -> decodePayload<ServerEventPayloads.SubmissionStatus>(data).let {
                ServerEvent.SubmissionUpdated(
                    questionNo = 0,
                    submittedCount = it.submitCount,
                    totalCount = it.participantCount
                )
            }
            "HINT_PUBLISHED" -> decodePayload<ServerEvent.HintPublished>(data)
            // 아래는 계약에는 있으나 백엔드가 아직 발행하지 않는다 — 오면 그대로 받는다
            "ANSWER_SUBMITTED" -> decodePayload<ServerEvent.AnswerSubmitted>(data)
            "SCORE_UPDATED" -> decodePayload<ServerEvent.ScoreUpdated>(data)
            "REPORT_READY" -> decodePayload<ServerEvent.ReportReady>(data)
            "ROOM_CANCELLED" -> decodePayload<ServerEvent.RoomCancelled>(data)
            "FEEDBACK_READY" -> decodePayload<ServerEvent.FeedbackReady>(data)
            "FEEDBACK_FAILED" -> decodePayload<ServerEvent.FeedbackFailed>(data)
            "REVIEW_RECEIVED" -> decodePayload<ServerEvent.ReviewReceived>(data)
            "PROJECTOR_CONNECTED" -> ServerEvent.ProjectorConnected
            "PROJECTOR_DISCONNECTED" -> ServerEvent.ProjectorDisconnected
            else -> null
        }
    }

    // 알 수 없는 type·손상 페이로드는 null — 스트림을 끊지 않고 해당 프레임만 폐기한다
    fun decode(text: String): ServerEventFrame? {
        val root = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            return null
        }
        val type = root["type"]?.jsonPrimitive?.contentOrNull ?: return null
        val ts = root["occurredAt"]?.jsonPrimitive?.contentOrNull ?: return null
        val data = root["payload"] ?: JsonObject(emptyMap())

        return try {
            decodeEvent(type, data)?.let { ServerEventFrame(ts, it) }
        } catch (e: Exception) {
            null
        }
    }
}
