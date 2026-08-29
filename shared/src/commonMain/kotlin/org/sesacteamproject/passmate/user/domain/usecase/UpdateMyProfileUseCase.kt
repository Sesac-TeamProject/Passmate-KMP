package org.sesacteamproject.passmate.user.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.user.domain.repository.UserRepository

// 닉네임·기본 캐릭터 수정 (M-12-1·M-12-7) — 최종 검증(닉네임 규칙 등)은 서버가 한다
class UpdateMyProfileUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(nickname: String?, avatarId: Int?): AppResult<Unit> {
        return userRepository.updateMyProfile(nickname, avatarId)
    }
}
