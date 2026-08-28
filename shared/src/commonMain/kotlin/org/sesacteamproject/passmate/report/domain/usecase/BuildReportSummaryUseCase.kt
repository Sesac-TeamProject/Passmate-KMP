package org.sesacteamproject.passmate.report.domain.usecase

import org.sesacteamproject.passmate.report.domain.model.LearningReport
import org.sesacteamproject.passmate.report.domain.model.SessionResult

// 리포트 내보내기(저장/공유) — 네이티브 공유 시트에 넣을 텍스트 요약 구성 (FR-063 모바일 공유 경로).
// 파일(CSV/PDF) 내보내기는 백엔드 export 엔드포인트를 쓰는 후속 과제로 남긴다
class BuildReportSummaryUseCase(
) {
    operator fun invoke(result: SessionResult, report: LearningReport?): String {
        val lines = mutableListOf<String>()

        lines.add("[패스메이트] ${result.roomTitle} 학습 리포트")
        lines.add(rankScoreLine(result))
        if (report != null) {
            lines.add("정답률 ${report.accuracyPercent}%")
            if (report.weakTopics.isNotEmpty()) {
                lines.add("보완할 주제: ${report.weakTopics.joinToString(", ")}")
            }
            report.improvementPoints.forEach { point ->
                lines.add("· $point")
            }
        }

        return lines.joinToString("\n")
    }

    private fun rankScoreLine(result: SessionResult): String {
        val rankPart = result.rank?.let { "${it}위 · " } ?: ""

        return "$rankPart${result.totalScore.toLong()}점 · 정답 ${result.correctCount}/${result.questionCount}"
    }
}
