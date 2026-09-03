package org.sesacteamproject.passmate.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.sesacteamproject.passmate.session.domain.model.QuestionDeadline

// 서버는 시각을 두 가지 형태로 준다:
//  - 본문: LocalDateTime을 오프셋 없이 ("2026-09-03T12:40:50.728") — 서버 지역 시각(KST)
//  - 헤더: HTTP Date를 GMT로 ("Thu, 03 Sep 2026 03:40:51 GMT")
// 둘을 그대로 비교하면 9시간이 어긋난다. 남은 시간 계산(§5)이 여기에 걸린다.
class ServerClockTest {

    @Test
    fun httpDateBecomesServerLocalTime() {
        // 같은 순간을 가리키는 두 표기 (2026-09-03 로컬 백엔드 실측)
        assertEquals(
            "2026-09-03T12:40:51",
            ServerClock.toServerLocalIso("Thu, 03 Sep 2026 03:40:51 GMT")
        )
        assertNull(ServerClock.toServerLocalIso("아무 값"))
        assertNull(ServerClock.toServerLocalIso(null))
    }

    @Test
    fun deadlineFromServerTimesIsSecondsNotHours() {
        val serverNow = ServerClock.toServerLocalIso("Thu, 03 Sep 2026 03:40:20 GMT")!!
        val deadline = QuestionDeadline.fromServerTimes("2026-09-03T12:40:50.728", serverNow)

        // 30.728초 → 표시는 올림해서 31초. 헤더를 그대로 쓰면 9시간이 나온다
        assertEquals(31, deadline?.remainingSeconds())
    }

    @Test
    fun naiveServerTimesCompareDirectly() {
        // 본문끼리는 같은 시계라 변환 없이 비교된다 (이벤트 occurredAt ↔ endsAt)
        val deadline = QuestionDeadline.fromServerTimes("2026-09-03T12:41:00", "2026-09-03T12:40:50")

        assertEquals(10, deadline?.remainingSeconds())
    }
}
