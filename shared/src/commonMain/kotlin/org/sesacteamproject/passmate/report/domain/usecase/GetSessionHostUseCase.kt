package org.sesacteamproject.passmate.report.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository
import org.sesacteamproject.passmate.user.domain.model.HostProfile
import org.sesacteamproject.passmate.user.domain.repository.UserRepository

// 세션을 연 선생님 — 평가 시트 카드(M-06 v2)의 이름·등급·캐릭터.
// GET /rooms/{roomId}/results/me에 호스트 정보가 없어(계약 갭 G-8) 방 상세로 hostUserId를 얻은 뒤
// 공개 프로필을 조회한다. 방 상세는 방에 속한 사람(호스트·참가자)이면 볼 수 있다.
// 계약에 host가 추가되면 이 UseCase는 한 번의 조회로 줄어든다.
class GetSessionHostUseCase(
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(roomId: Long): AppResult<HostProfile?> {
        return when (val hostUserId = roomRepository.getRoomHostUserId(roomId)) {
            is AppResult.Failure -> hostUserId
            is AppResult.Success -> {
                val userId = hostUserId.value

                if (userId == null) {
                    AppResult.Success(null)
                } else {
                    when (val profile = userRepository.getHostProfile(userId)) {
                        is AppResult.Failure -> profile
                        is AppResult.Success -> AppResult.Success(profile.value)
                    }
                }
            }
        }
    }
}
