package org.sesacteamproject.passmate.report.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.report.domain.model.LearningReport
import org.sesacteamproject.passmate.report.domain.repository.ResultRepository

class GetLearningReportUseCase(
    private val resultRepository: ResultRepository
) {
    suspend operator fun invoke(roomId: Long): AppResult<LearningReport> {
        return resultRepository.getLearningReport(roomId)
    }
}
