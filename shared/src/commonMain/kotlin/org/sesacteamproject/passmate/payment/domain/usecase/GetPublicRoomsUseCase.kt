package org.sesacteamproject.passmate.payment.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.payment.domain.model.PublicRoom
import org.sesacteamproject.passmate.payment.domain.model.RoomSort
import org.sesacteamproject.passmate.payment.domain.model.RoomTypeFilter
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository

class GetPublicRoomsUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(
        sort: RoomSort = RoomSort.POPULAR,
        query: String? = null,
        type: RoomTypeFilter = RoomTypeFilter.ALL,
        cursor: String? = null
    ): AppResult<PagedResult<PublicRoom>> {
        return paymentRepository.getPublicRooms(sort, query, type, cursor)
    }
}
