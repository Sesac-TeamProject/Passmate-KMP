package org.sesacteamproject.passmate.report.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.report.data.dto.RoomReportResponse
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.session.domain.model.QuestionType

// GET /rooms/{roomId}/results — 백엔드 실제 응답(2026-09-04 로컬 확인) 기준.
// 계약 문서의 이름(roomTitle·dateLabel·students·accuracyPercent)과 다르다:
// 서버는 title·startedAt·participants·correctRate를 준다. pin은 아예 주지 않는다.
class RoomReportMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    // 실서버에서 그대로 받은 본문 — 필드명이 바뀌면 이 시험이 먼저 깨진다
    private val raw = """
        {
          "roomId": 22,
          "title": "OX 제출 검증2",
          "status": "RUNNING",
          "startedAt": "2026-09-04T03:50:32.640562",
          "summary": {
            "participantCount": 6,
            "questionCount": 8,
            "avgCorrectRate": 0.71,
            "avgScore": 82.5,
            "aiAnalysisCount": 18
          },
          "questions": [
            {
              "sessionQuestionId": 28,
              "questionId": 4,
              "orderNo": 1,
              "type": "MCQ",
              "content": "1+1은?",
              "points": 100,
              "submitCount": 6,
              "correctCount": 5,
              "correctRate": 0.83,
              "aiAnalysisCount": 6
            },
            {
              "sessionQuestionId": 29,
              "questionId": 5,
              "orderNo": 2,
              "type": "ESSAY",
              "content": "Bean 기본 스코프",
              "points": 100,
              "submitCount": 6,
              "correctCount": 0,
              "correctRate": null,
              "aiAnalysisCount": null
            }
          ],
          "participants": [
            {
              "rank": 1,
              "participantId": 31,
              "nickname": "oxfast",
              "avatarId": "cat",
              "totalScore": 180,
              "correctCount": 2,
              "submitCount": 2
            }
          ]
        }
    """.trimIndent()

    @Test
    fun mapsServerFieldNamesIntoDomain() {
        val report = json.decodeFromString<RoomReportResponse>(raw).toDomain()

        // 서버는 title이고 앱 도메인은 roomTitle이다 — 이름이 어긋나면 제목이 빈칸으로 그려진다
        assertEquals("OX 제출 검증2", report.roomTitle)
        assertEquals(RoomStatus.RUNNING, report.status)
        assertEquals("2026-09-04", report.dateLabel)
        assertEquals(6, report.summary.studentCount)
        assertEquals(8, report.summary.questionCount)
        assertEquals(18, report.summary.aiAnalysisCount)
    }

    // 서버는 0.0~1.0 비율, 화면은 퍼센트다. 그대로 쓰면 71%가 0%로 보인다
    @Test
    fun convertsCorrectRateRatioIntoPercent() {
        val report = json.decodeFromString<RoomReportResponse>(raw).toDomain()

        assertEquals(71, report.summary.avgAccuracyPercent)
        assertEquals(83, report.questions.first().accuracyPercent)
    }

    @Test
    fun mapsQuestionsFromOrderNoAndContent() {
        val report = json.decodeFromString<RoomReportResponse>(raw).toDomain()
        val first = report.questions.first()
        val essay = report.questions.last()

        assertEquals(1, first.questionNo)
        assertEquals("1+1은?", first.title)
        assertEquals(QuestionType.MULTIPLE_CHOICE, first.type)
        assertEquals(6, first.aiFeedbackCount)
        // 서술형 미채점은 정답률이 없다 — 화면이 "—"를 그린다
        assertNull(essay.accuracyPercent)
    }

    @Test
    fun mapsParticipantsIntoStudents() {
        val report = json.decodeFromString<RoomReportResponse>(raw).toDomain()
        val student = report.students.single()

        assertEquals(31L, student.participantId)
        assertEquals("oxfast", student.nickname)
        assertEquals(1, student.rank)
        assertEquals(180.0, student.totalScore)
    }

    // 서버가 pin을 주지 않는다 — 빈 값이어야 화면이 PIN 조각을 생략한다 (백엔드 요청 중)
    @Test
    fun leavesPinBlankBecauseServerOmitsIt() {
        val report = json.decodeFromString<RoomReportResponse>(raw).toDomain()

        assertTrue(report.pin.isEmpty())
    }
}
