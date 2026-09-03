package org.sesacteamproject.passmate.session.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.core.model.HttpDate
import org.sesacteamproject.passmate.core.model.IsoTime
import org.sesacteamproject.passmate.session.data.dto.SessionSnapshotResponse
import org.sesacteamproject.passmate.room.domain.model.RoomStatus

// GET /rooms/{roomId}/session — 백엔드 실제 응답(2026-09-03 로컬 확인) 기준.
// 서버 본문에 ts가 없어 응답의 HTTP Date 헤더를 서버 시각으로 쓴다
// (규칙 §5 남은 시간 계산 · §2-1-2 스냅샷 이전 이벤트 폐기가 이 값에 의존한다).
class SessionSnapshotMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun convertsHttpDateHeaderToIso() {
        assertEquals("2026-09-03T03:05:38Z", HttpDate.toIsoOrNull("Thu, 03 Sep 2026 03:05:38 GMT"))
        // ISO로 바꾼 값이 기존 파서로 다시 읽혀야 카운트다운에 쓸 수 있다
        assertEquals(
            IsoTime.toEpochMillis("2026-09-03T03:05:38Z"),
            IsoTime.toEpochMillis(HttpDate.toIsoOrNull("Thu, 03 Sep 2026 03:05:38 GMT")!!)
        )
        assertNull(HttpDate.toIsoOrNull("아무 값"))
        assertNull(HttpDate.toIsoOrNull(null))
    }

    @Test
    fun mapsWaitingSnapshot() {
        val raw = """
            {"roomId":2,"status":"WAITING","currentQuestionNo":0,"totalCount":0,"screenLocked":false,"submitted":false,"ranking":[]}
        """.trimIndent()

        val snapshot = json.decodeFromString<SessionSnapshotResponse>(raw)
            .toDomain(serverTime = "2026-09-03T03:05:38Z")

        assertEquals(RoomStatus.WAITING, snapshot.status)
        assertEquals("2026-09-03T03:05:38Z", snapshot.ts)
        assertEquals(0, snapshot.questionCount)
        assertNull(snapshot.currentQuestion)
        assertEquals(false, snapshot.isLocked)
        assertEquals(0, snapshot.ranking.size)
    }

    @Test
    fun mapsRunningSnapshotWithQuestionAndRanking() {
        val raw = """
            {
              "roomId": 2,
              "status": "RUNNING",
              "currentQuestionNo": 3,
              "totalCount": 8,
              "screenLocked": true,
              "submitted": true,
              "currentQuestion": {
                "sessionQuestionId": 91,
                "questionId": 41,
                "orderNo": 3,
                "totalCount": 8,
                "type": "MCQ",
                "content": "다음 중 옳은 것은?",
                "choices": ["가", "나", "다"],
                "points": 100,
                "timeLimitSec": 30,
                "endsAt": "2026-09-03T03:06:08Z"
              },
              "ranking": [
                {"rank":1,"participantId":5,"nickname":"준영","avatarId":"fox","totalScore":300},
                {"rank":2,"participantId":6,"nickname":"민지","avatarId":"owl","totalScore":250}
              ]
            }
        """.trimIndent()

        val snapshot = json.decodeFromString<SessionSnapshotResponse>(raw)
            .toDomain(serverTime = "2026-09-03T03:05:38Z")
        val question = snapshot.currentQuestion

        assertEquals(RoomStatus.RUNNING, snapshot.status)
        assertEquals(8, snapshot.questionCount)
        assertEquals(true, snapshot.isLocked)

        assertEquals(41L, question?.questionId)
        // 서버 orderNo → 화면의 문항 번호
        assertEquals(3, question?.questionNo)
        assertEquals("다음 중 옳은 것은?", question?.body)
        assertEquals(listOf("가", "나", "다"), question?.choices)
        assertEquals("2026-09-03T03:06:08Z", question?.endsAt)

        assertEquals(2, snapshot.ranking.size)
        assertEquals("준영", snapshot.ranking.first().nickname)
        // avatarId는 문자열 키 — 화면 인덱스로 바꾼다 (fox는 6번째)
        assertEquals(6, snapshot.ranking.first().avatarId)
        assertEquals(300.0, snapshot.ranking.first().total)

        // 서버는 제출 여부(boolean)만 준다 — 현재 문항 한 건으로 복원해
        // 재접속 후에도 중복 제출을 막는다 (규칙 §9)
        assertEquals(1, snapshot.myAnswers.size)
        assertEquals(41L, snapshot.myAnswers.first().questionId)
        assertEquals(true, snapshot.myAnswers.first().isProvisional)
    }
}
