package org.sesacteamproject.passmate.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.sesacteamproject.passmate.session.domain.model.QuestionDeadline

// 서버는 시각을 두 가지 형태로 준다:
//  - 본문: 오프셋 없는 `LocalDateTime` ("2026-09-04T06:43:43.263979")
//  - 헤더: HTTP Date를 GMT로 ("Fri, 04 Sep 2026 06:43:43 GMT")
// 표기는 다르지만 같은 시계(UTC)다. 남은 시간 계산(§5)이 이 전제에 걸려 있다.
class ServerClockTest {

    @Test
    fun httpDateStaysOnTheSameClockAsBodyTimes() {
        // 2026-09-04 로컬 백엔드 실측 — 같은 응답의 헤더와 본문이 초까지 일치했다
        assertEquals(
            "2026-09-04T06:43:43Z",
            ServerClock.toServerLocalIso("Fri, 04 Sep 2026 06:43:43 GMT")
        )
        assertNull(ServerClock.toServerLocalIso("아무 값"))
        assertNull(ServerClock.toServerLocalIso(null))
    }

    @Test
    fun deadlineFromServerTimesIsSecondsNotHours() {
        val serverNow = ServerClock.toServerLocalIso("Fri, 04 Sep 2026 06:43:20 GMT")!!
        val deadline = QuestionDeadline.fromServerTimes("2026-09-04T06:43:50.728", serverNow)

        // 30.728초 → 표시는 올림해서 31초
        assertEquals(31, deadline?.remainingSeconds())
    }

    @Test
    fun liveQuestionKeepsItsRemainingSeconds() {
        // 실측 스냅샷과 같은 모양 — 20초짜리 OX 문항이 막 시작된 순간.
        // 헤더에 시간대 보정을 얹으면 남은 시간이 음수가 되어 0으로 깎인다.
        // 남은 시간을 0이 아닌 값으로 못박아 그 실수를 잡는다.
        val serverNow = ServerClock.toServerLocalIso("Fri, 04 Sep 2026 06:43:43 GMT")!!
        val deadline = QuestionDeadline.fromServerTimes("2026-09-04T06:44:03", serverNow)

        assertEquals(20, deadline?.remainingSeconds())
    }

    @Test
    fun naiveServerTimesCompareDirectly() {
        // 본문끼리는 같은 시계라 변환 없이 비교된다 (이벤트 occurredAt ↔ endsAt)
        val deadline = QuestionDeadline.fromServerTimes("2026-09-03T12:41:00", "2026-09-03T12:40:50")

        assertEquals(10, deadline?.remainingSeconds())
    }
}
