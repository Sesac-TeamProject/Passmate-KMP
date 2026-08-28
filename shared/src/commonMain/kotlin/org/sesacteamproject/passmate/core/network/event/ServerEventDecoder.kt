package org.sesacteamproject.passmate.core.network.event

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ServerEventDecoder {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private inline fun <reified T : ServerEvent> decodeData(data: JsonElement): T {
        return json.decodeFromJsonElement(data)
    }

    private fun decodeEvent(type: String, data: JsonElement): ServerEvent? {
        return when (type) {
            "PARTICIPANT_JOINED" -> decodeData<ServerEvent.ParticipantJoined>(data)
            "PARTICIPANT_LEFT" -> decodeData<ServerEvent.ParticipantLeft>(data)
            "SESSION_STARTED" -> decodeData<ServerEvent.SessionStarted>(data)
            "QUESTION_STARTED" -> decodeData<ServerEvent.QuestionStarted>(data)
            "ANSWER_SUBMITTED" -> decodeData<ServerEvent.AnswerSubmitted>(data)
            "QUESTION_ENDED" -> decodeData<ServerEvent.QuestionEnded>(data)
            "SCORE_UPDATED" -> decodeData<ServerEvent.ScoreUpdated>(data)
            "RANKING_UPDATED" -> decodeData<ServerEvent.RankingUpdated>(data)
            "SCREEN_LOCKED" -> decodeData<ServerEvent.ScreenLocked>(data)
            "HINT_PUBLISHED" -> decodeData<ServerEvent.HintPublished>(data)
            "SESSION_ENDED" -> decodeData<ServerEvent.SessionEnded>(data)
            "REPORT_READY" -> decodeData<ServerEvent.ReportReady>(data)
            "ROOM_CANCELLED" -> decodeData<ServerEvent.RoomCancelled>(data)
            "FEEDBACK_READY" -> decodeData<ServerEvent.FeedbackReady>(data)
            "FEEDBACK_FAILED" -> decodeData<ServerEvent.FeedbackFailed>(data)
            "REVIEW_RECEIVED" -> decodeData<ServerEvent.ReviewReceived>(data)
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
        val ts = root["ts"]?.jsonPrimitive?.contentOrNull ?: return null
        val data = root["data"] ?: JsonObject(emptyMap())

        return try {
            decodeEvent(type, data)?.let { ServerEventFrame(ts, it) }
        } catch (e: Exception) {
            null
        }
    }
}
