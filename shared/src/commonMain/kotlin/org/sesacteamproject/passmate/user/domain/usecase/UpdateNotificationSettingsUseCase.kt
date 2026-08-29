package org.sesacteamproject.passmate.user.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.user.domain.model.NotificationSettings
import org.sesacteamproject.passmate.user.domain.repository.UserRepository

class UpdateNotificationSettingsUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(settings: NotificationSettings): AppResult<Unit> {
        return userRepository.updateNotificationSettings(settings)
    }
}
