package org.sesacteamproject.passmate.user.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.user.domain.model.MyPage
import org.sesacteamproject.passmate.user.domain.repository.UserRepository

class GetMyPageUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(cursor: String? = null): AppResult<MyPage> {
        return userRepository.getMyPage(cursor)
    }
}
