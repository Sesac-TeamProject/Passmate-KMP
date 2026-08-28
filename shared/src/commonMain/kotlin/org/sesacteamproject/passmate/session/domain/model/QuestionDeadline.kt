package org.sesacteamproject.passmate.session.domain.model

import kotlin.time.TimeSource
import org.sesacteamproject.passmate.core.model.IsoTime

// 서버 endsAt 기반 남은 시간 렌더링 전용 — 마감 판정은 서버가 한다(QUESTION_ENDED·410, 규칙 §1·§5).
// 로컬 시계 오차를 피하려고 (endsAt - 서버 발화 시각)을 수신 시점 기준 monotonic으로 카운트다운한다
class QuestionDeadline(
    val totalMillis: Long
) {
    private val startMark = TimeSource.Monotonic.markNow()

    fun remainingMillis(): Long {
        val elapsed = startMark.elapsedNow().inWholeMilliseconds

        return (totalMillis - elapsed).coerceAtLeast(0L)
    }

    fun remainingSeconds(): Int {
        return ((remainingMillis() + 999L) / 1000L).toInt()
    }

    companion object {

        fun fromServerTimes(endsAt: String, serverNow: String): QuestionDeadline? {
            val endsAtMillis = IsoTime.toEpochMillis(endsAt)
            val serverNowMillis = IsoTime.toEpochMillis(serverNow)

            return if (endsAtMillis == null || serverNowMillis == null) {
                null
            } else {
                QuestionDeadline((endsAtMillis - serverNowMillis).coerceAtLeast(0L))
            }
        }
    }
}
