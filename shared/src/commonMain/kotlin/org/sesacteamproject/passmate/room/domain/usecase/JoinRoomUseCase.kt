package org.sesacteamproject.passmate.room.domain.usecase

import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.room.domain.model.MyParticipation
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository

class JoinRoomUseCase(
    private val roomRepository: RoomRepository
) {
    // 강퇴(403)는 새로 들어오는 것도 막아야 한다 — join으로 우회되면 내보낸 의미가 없다
    private fun isKicked(result: AppResult<MyParticipation>): Boolean {
        return result is AppResult.Failure && result.error is AppError.PermissionDenied
    }

    // 백엔드 우회 — join()이 닉네임 중복을 기입장보다 먼저 검사하고, 그 검사가 내 옛 행도 센다.
    // 그래서 같은 이름으로 돌아오면 재입장에 닿지 못한다.
    // 백엔드가 닉네임 검사에서 본인 행을 제외하면 이 분기는 지운다
    private fun isNicknameTakenByUnknownRow(result: AppResult<MyParticipation>): Boolean {
        return result is AppResult.Failure && result.error.serverCode == NICKNAME_DUPLICATED
    }

    // 서버는 회원을 계정으로 알아보지만 게스트는 토큰 없이는 못 알아본다.
    // 그래서 이어갈 게스트 기록이 있으면 새 입장 대신 재입장을 먼저 부른다 —
    // 그냥 join 하면 새 참가자 행이 생겨 대기실에 같은 사람이 둘로 보이고 점수가 갈라진다
    suspend operator fun invoke(room: RoomInfo, nickname: String, avatarId: Int?): AppResult<MyParticipation> {
        val canResume = roomRepository.hasGuestSession(room.roomId)

        if (canResume) {
            val resumed = roomRepository.rejoinRoom(room)

            // 토큰이 만료(401)됐거나 그 방 기록이 없으면(404) 평소대로 새로 입장한다
            if (resumed is AppResult.Success || isKicked(resumed)) {
                return resumed
            }
        }

        val joined = roomRepository.joinRoom(room, nickname.trim(), avatarId)

        // 재입장은 닉네임을 인자로 받지 않는다 — 서버가 내 기록으로만 판정하므로
        // 진짜 남의 이름과 겹친 경우엔 404가 오고 원래 안내로 돌아간다
        if (!canResume && isNicknameTakenByUnknownRow(joined)) {
            val resumed = roomRepository.rejoinRoom(room)

            if (resumed is AppResult.Success) {
                return resumed
            }
        }

        return joined
    }

    companion object {
        // 입장 실패의 서버 코드 (백엔드 ErrorCode)
        private const val NICKNAME_DUPLICATED = "NICKNAME_DUPLICATED"
    }
}
