package org.sesacteamproject.passmate.report.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import org.sesacteamproject.passmate.report.domain.model.ReportQuestion
import org.sesacteamproject.passmate.report.domain.model.ReportStudent
import org.sesacteamproject.passmate.report.domain.model.RoomReport
import org.sesacteamproject.passmate.report.domain.model.RoomReportSummary
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.session.domain.model.QuestionType

class BuildRoomReportSummaryUseCaseTest {

    @Test
    fun buildsShareTextWithEssayDash() {
        val report = RoomReport(
            roomTitle = "8월 4주차 Spring 스터디",
            pin = "482913",
            status = RoomStatus.FINISHED,
            dateLabel = "8/22(금)",
            summary = RoomReportSummary(
                avgAccuracyPercent = 71,
                studentCount = 6,
                questionCount = 8,
                aiAnalysisCount = 18,
                avgScore = 720.0,
                topScore = 990.0
            ),
            questions = listOf(
                ReportQuestion(1, 1, "DI 컨테이너 개념", QuestionType.MULTIPLE_CHOICE, 100, null),
                ReportQuestion(5, 5, "Bean 기본 스코프", QuestionType.ESSAY, null, 6)
            ),
            students = listOf(
                ReportStudent(1, "준영", 1, 990.0, 8, false)
            )
        )

        val text = BuildRoomReportSummaryUseCase().invoke(report)
        val lines = text.split("\n")

        assertEquals("[패스메이트] 8월 4주차 Spring 스터디 방 리포트", lines[0])
        assertEquals("평균 정답률 71% · 학생 6명 · 8문항 · AI 분석 18건", lines[1])
        assertEquals("Q1 DI 컨테이너 개념: 100%", lines[2])
        // 서술형 미채점(accuracyPercent null)은 "—"로 표기한다
        assertEquals("Q5 Bean 기본 스코프: —", lines[3])
    }
}
