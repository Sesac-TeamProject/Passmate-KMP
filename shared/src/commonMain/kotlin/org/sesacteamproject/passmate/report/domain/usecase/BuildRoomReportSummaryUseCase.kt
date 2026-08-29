package org.sesacteamproject.passmate.report.domain.usecase

import org.sesacteamproject.passmate.report.domain.model.ReportQuestion
import org.sesacteamproject.passmate.report.domain.model.RoomReport

// 방 리포트 내보내기(M-14) — 네이티브 공유 시트에 넣을 텍스트 요약 구성 (FR-063 모바일 공유 경로).
// 파일(CSV/PDF) 내보내기는 백엔드 export 엔드포인트를 쓰는 후속 과제로 남긴다
class BuildRoomReportSummaryUseCase(
) {
    operator fun invoke(report: RoomReport): String {
        val lines = mutableListOf<String>()

        lines.add("[패스메이트] ${report.roomTitle} 방 리포트")
        lines.add(summaryLine(report))
        report.questions.forEach { question ->
            lines.add(questionLine(question))
        }

        return lines.joinToString("\n")
    }

    private fun summaryLine(report: RoomReport): String {
        val summary = report.summary
        val accuracyPart = summary.avgAccuracyPercent?.let { "평균 정답률 ${it}% · " } ?: ""

        return "${accuracyPart}학생 ${summary.studentCount}명 · ${summary.questionCount}문항 · AI 분석 ${summary.aiAnalysisCount}건"
    }

    private fun questionLine(question: ReportQuestion): String {
        val accuracyPart = question.accuracyPercent?.let { "${it}%" } ?: "—"

        return "Q${question.questionNo} ${question.title}: $accuracyPart"
    }
}
