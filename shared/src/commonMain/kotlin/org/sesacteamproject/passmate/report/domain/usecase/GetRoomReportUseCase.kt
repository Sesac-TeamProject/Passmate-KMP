package org.sesacteamproject.passmate.report.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.report.domain.model.RoomReport
import org.sesacteamproject.passmate.report.domain.repository.ResultRepository

class GetRoomReportUseCase(
    private val resultRepository: ResultRepository
) {
    suspend operator fun invoke(roomId: Long): AppResult<RoomReport> {
        return resultRepository.getRoomReport(roomId)
    }
}
