package org.sesacteamproject.passmate.room.domain.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.room.domain.model.MyParticipation
import org.sesacteamproject.passmate.room.domain.model.Participant
import org.sesacteamproject.passmate.room.domain.model.RoomInfo

interface RoomRepository {

    suspend fun getRoomInfo(pin: String): AppResult<RoomInfo>

    // 성공 시 게스트 토큰 보관 + MyParticipation 세션 스코프 기억
    suspend fun joinRoom(room: RoomInfo, nickname: String, avatarId: Int?): AppResult<MyParticipation>

    suspend fun getParticipants(roomId: Long): AppResult<List<Participant>>

    suspend fun leaveRoom(roomId: Long): AppResult<Unit>

    fun myParticipation(): MyParticipation?
}
