package org.sesacteamproject.passmate.report.domain.usecase

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.sesacteamproject.passmate.report.domain.model.LearningReport
import org.sesacteamproject.passmate.report.domain.model.SessionResult

class BuildReportSummaryUseCaseTest {

    private val useCase = BuildReportSummaryUseCase()

    private fun result(): SessionResult {
        return SessionResult(
            roomTitle = "Spring 스터디",
            rank = 3,
            totalScore = 990.0,
            correctCount = 6,
            questionCount = 8,
            questions = emptyList(),
            canRate = true,
            isGuest = false
        )
    }

    @Test
    fun includesRankScoreAndWeakTopics() {
        val report = LearningReport(
            accuracyPercent = 75,
            weakTopics = listOf("JPA 영속성", "트랜잭션"),
            improvementPoints = listOf("flush 시점 복습")
        )

        val summary = useCase.invoke(result(), report)

        assertContains(summary, "Spring 스터디")
        assertContains(summary, "3위")
        assertContains(summary, "990점")
        assertContains(summary, "정답 6/8")
        assertContains(summary, "정답률 75%")
        assertContains(summary, "JPA 영속성, 트랜잭션")
        assertContains(summary, "flush 시점 복습")
    }

    @Test
    fun worksWithoutReport() {
        val summary = useCase.invoke(result(), null)

        assertContains(summary, "990점")
        assertEquals(false, summary.contains("정답률"))
    }
}
