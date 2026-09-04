package org.sesacteamproject.passmate.report.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.report.data.dto.RoomReportResponse
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.session.domain.model.QuestionType

// GET /rooms/{roomId}/results — 백엔드 실제 응답 기준.
// 계약 문서의 이름(roomTitle·dateLabel·students·accuracyPercent)과 다르다:
// 서버는 title·startedAt·participants·correctRate를 준다. pin은 아예 주지 않는다.
class RoomReportMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    // 로컬 실서버(2026-09-04, room 31)에서 받은 본문을 손대지 않고 그대로 붙였다.
    // 필드명이나 정답률 스케일이 바뀌면 이 시험이 먼저 깨진다
    private val raw = """
        {
          "roomId": 31,
          "title": "별점 시트 실기기 확인",
          "status": "ENDED",
          "startedAt": "2026-09-04T07:50:25.554235",
          "endedAt": "2026-09-04T07:50:54.421808",
          "summary": {
            "participantCount": 2,
            "questionCount": 2,
            "avgCorrectRate": 50.0,
            "avgScore": 71.0,
            "aiAnalysisCount": 0
          },
          "questions": [
            {
              "sessionQuestionId": 53,
              "questionId": 4,
              "orderNo": 1,
              "type": "MCQ",
              "content": "1+1은?",
              "points": 100,
              "submitCount": 2,
              "correctCount": 1,
              "correctRate": 50.0,
              "aiAnalysisCount": 0
            },
            {
              "sessionQuestionId": 54,
              "questionId": 5,
              "orderNo": 2,
              "type": "OX",
              "content": "지구는 둥글다",
              "points": 100,
              "submitCount": 0,
              "correctCount": 0,
              "correctRate": 0.0,
              "aiAnalysisCount": 0
            }
          ],
          "participants": [
            {
              "rank": 1,
              "participantId": 43,
              "nickname": "여우",
              "avatarId": "cat",
              "totalScore": 142,
              "correctCount": 1,
              "submitCount": 1
            },
            {
              "rank": 2,
              "participantId": 44,
              "nickname": "강아지",
              "avatarId": "dog",
              "totalScore": 0,
              "correctCount": 0,
              "submitCount": 1
            }
          ]
        }
    """.trimIndent()

    // 미채점 서술형만 따로 — 위 방에는 서술형이 없어 실캡처에 담기지 않았다.
    // 서버는 채점 전 서술형의 correctRate를 null로 준다 (백엔드 SessionQuestion.correctRate가 nullable)
    private val essayRaw = """
        {
          "roomId": 31,
          "title": "서술형 미채점",
          "summary": { "participantCount": 1, "questionCount": 1 },
          "questions": [
            {
              "sessionQuestionId": 60,
              "questionId": 9,
              "orderNo": 1,
              "type": "ESSAY",
              "content": "Bean 기본 스코프",
              "points": 100,
              "submitCount": 6,
              "correctCount": 0,
              "correctRate": null,
              "aiAnalysisCount": null
            }
          ],
          "participants": []
        }
    """.trimIndent()

    @Test
    fun mapsServerFieldNamesIntoDomain() {
        val report = json.decodeFromString<RoomReportResponse>(raw).toDomain()

        // 서버는 title이고 앱 도메인은 roomTitle이다 — 이름이 어긋나면 제목이 빈칸으로 그려진다
        assertEquals("별점 시트 실기기 확인", report.roomTitle)
        assertEquals(RoomStatus.FINISHED, report.status)
        assertEquals("2026-09-04", report.dateLabel)
        assertEquals(2, report.summary.studentCount)
        assertEquals(2, report.summary.questionCount)
        assertEquals(0, report.summary.aiAnalysisCount)
    }

    // 서버 정답률은 이미 0~100 퍼센트다(백엔드 correctCount * 100.0 / submitCount).
    // 매퍼가 다시 100을 곱하면 50%가 5000%로 그려진다
    @Test
    fun keepsServerPercentAsIs() {
        val report = json.decodeFromString<RoomReportResponse>(raw).toDomain()

        assertEquals(50, report.summary.avgAccuracyPercent)
        assertEquals(50, report.questions.first().accuracyPercent)
        assertEquals(0, report.questions.last().accuracyPercent)
    }

    @Test
    fun mapsQuestionsFromOrderNoAndContent() {
        val report = json.decodeFromString<RoomReportResponse>(raw).toDomain()
        val first = report.questions.first()

        assertEquals(1, first.questionNo)
        assertEquals("1+1은?", first.title)
        assertEquals(QuestionType.MULTIPLE_CHOICE, first.type)
        assertEquals(0, first.aiFeedbackCount)
    }

    // 서술형 미채점은 정답률이 없다 — 화면이 "—"를 그린다
    @Test
    fun leavesUngradedEssayAccuracyNull() {
        val report = json.decodeFromString<RoomReportResponse>(essayRaw).toDomain()
        val essay = report.questions.single()

        assertEquals(QuestionType.ESSAY, essay.type)
        assertNull(essay.accuracyPercent)
        assertNull(essay.aiFeedbackCount)
    }

    @Test
    fun mapsParticipantsIntoStudents() {
        val report = json.decodeFromString<RoomReportResponse>(raw).toDomain()
        val top = report.students.first()

        assertEquals(43L, top.participantId)
        assertEquals("여우", top.nickname)
        assertEquals(1, top.rank)
        assertEquals(142.0, top.totalScore)
    }

    // 서버가 pin을 주지 않는다 — 빈 값이어야 화면이 PIN 조각을 생략한다 (백엔드 요청 중)
    @Test
    fun leavesPinEmptyBecauseServerOmitsIt() {
        val report = json.decodeFromString<RoomReportResponse>(raw).toDomain()

        assertTrue(report.pin.isEmpty())
    }
}
