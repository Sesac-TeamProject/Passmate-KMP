package org.sesacteamproject.passmate.room.domain.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.room.domain.model.CreatedRoom
import org.sesacteamproject.passmate.room.domain.model.HostedRoom
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

    // 내가 만든 방 목록 (M-13) — 진행 중/종료 구분은 status로 클라이언트가 분리
    suspend fun getHostedRooms(cursor: String?): AppResult<PagedResult<HostedRoom>>

    // 방 생성 (M-13 새 방 만들기 시트) — 유료 방은 서버 Lv.3+ 검증 (403 HOST_LEVEL_REQUIRED)
    suspend fun createRoom(title: String, questionSetId: Long?, isPaid: Boolean, entryFee: Int?): AppResult<CreatedRoom>
}
