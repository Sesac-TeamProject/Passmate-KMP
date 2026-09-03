package org.sesacteamproject.passmate.report.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.report.data.dto.LearningReportResponse
import org.sesacteamproject.passmate.report.data.dto.SessionResultResponse
import org.sesacteamproject.passmate.report.domain.model.AiFeedbackStatus
import org.sesacteamproject.passmate.report.domain.model.AnswerVerdict

// GET /rooms/{roomId}/results/me · /reports/me — 백엔드 실제 스키마(2026-09-03) 기준.
class SessionResultMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun mapsResultWithNestedRatingAndGuestFlag() {
        val raw = """
            {
              "roomId": 2,
              "roomTitle": "8월 4주차 Spring 스터디",
              "status": "FINISHED",
              "endedAt": "2026-09-03T03:30:00",
              "participantId": 5,
              "nickname": "준영",
              "avatarId": "fox",
              "guest": true,
              "rank": 3,
              "totalScore": 780,
              "correctCount": 6,
              "submitCount": 8,
              "questionCount": 8,
              "questions": [
                {
                  "sessionQuestionId": 91,
                  "questionId": 41,
                  "orderNo": 1,
                  "type": "MCQ",
                  "content": "다음 중 옳은 것은?",
                  "points": 100,
                  "answer": "나",
                  "explanation": "나가 정답인 이유",
                  "submitted": "가",
                  "isCorrect": false,
                  "score": 0,
                  "finalScore": 0
                },
                {
                  "sessionQuestionId": 92,
                  "questionId": 42,
                  "orderNo": 2,
                  "type": "ESSAY",
                  "content": "트랜잭션 전파를 설명하시오",
                  "points": 100,
                  "submitted": "REQUIRED는 기존 트랜잭션에 참여합니다",
                  "isCorrect": true,
                  "score": 80,
                  "finalScore": 90,
                  "analysisStatus": "DONE",
                  "analysis": {
                    "keyPoints": ["전파 속성"],
                    "missingPoints": ["REQUIRES_NEW"],
                    "suggestions": "새 트랜잭션 케이스를 더 쓰세요",
                    "summary": "핵심은 짚었으나 예시가 부족합니다"
                  },
                  "teacherReview": {
                    "comment": "좋습니다",
                    "improvement": "예시 추가",
                    "adjustedScore": 90
                  }
                }
              ],
              "rating": {"available": true, "alreadyRated": false}
            }
        """.trimIndent()

        val result = json.decodeFromString<SessionResultResponse>(raw).toDomain()
        val mcq = result.questions.first()
        val essay = result.questions.last()

        assertEquals("8월 4주차 Spring 스터디", result.roomTitle)
        assertEquals(3, result.rank)
        assertEquals(780.0, result.totalScore)
        assertEquals(6, result.correctCount)
        assertEquals(8, result.questionCount)
        // 서버는 guest·rating.available로 준다
        assertEquals(true, result.isGuest)
        assertEquals(true, result.canRate)

        // orderNo → 화면 문항 번호, content → 제목, submitted → 내 답변, answer → 정답
        assertEquals(41L, mcq.questionId)
        assertEquals(1, mcq.questionNo)
        assertEquals("다음 중 옳은 것은?", mcq.title)
        assertEquals(AnswerVerdict.WRONG, mcq.verdict)
        assertEquals("가", mcq.myAnswer)
        assertEquals("나", mcq.correctAnswer)
        assertEquals(0.0, mcq.earnedScore)
        assertNull(mcq.aiFeedback)

        // 첨삭이 있으면 finalScore가 최종 점수다
        assertEquals(AnswerVerdict.CORRECT, essay.verdict)
        assertEquals(90.0, essay.earnedScore)
        assertEquals(AiFeedbackStatus.DONE, essay.aiFeedback?.status)
        assertEquals(listOf("전파 속성"), essay.aiFeedback?.coveredConcepts)
        assertEquals(listOf("REQUIRES_NEW"), essay.aiFeedback?.missingConcepts)
        assertEquals("좋습니다", essay.hostReview?.comment)
        assertEquals(90.0, essay.hostReview?.adjustedScore)
    }

    @Test
    fun mapsLearningReportAccuracyFromServerPercent() {
        val raw = """
            {
              "roomId": 2,
              "roomTitle": "8월 4주차 Spring 스터디",
              "participantId": 5,
              "nickname": "준영",
              "totalQuestions": 8,
              "correctCount": 6,
              "accuracy": 75.0,
              "totalScore": 780,
              "finalRank": 3,
              "weakTopics": ["트랜잭션"],
              "improvementPoints": ["예시를 더 들어보세요"],
              "generatedAt": "2026-09-03T03:31:00"
            }
        """.trimIndent()

        val report = json.decodeFromString<LearningReportResponse>(raw).toDomain()

        // 서버가 0~100 퍼센트로 준다
        assertEquals(75, report.accuracyPercent)
        assertEquals(listOf("트랜잭션"), report.weakTopics)
        assertEquals(listOf("예시를 더 들어보세요"), report.improvementPoints)
    }
}
