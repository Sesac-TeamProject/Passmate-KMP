package org.sesacteamproject.passmate.room.data.remote

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.sesacteamproject.passmate.core.network.ApiClient
import org.sesacteamproject.passmate.room.data.dto.CreateRoomRequest
import org.sesacteamproject.passmate.room.data.dto.CreateRoomResponse
import org.sesacteamproject.passmate.room.data.dto.HostedRoomsResponse
import org.sesacteamproject.passmate.room.data.dto.JoinRoomRequest
import org.sesacteamproject.passmate.room.data.dto.JoinRoomResponse
import org.sesacteamproject.passmate.room.data.dto.ParticipantDto
import org.sesacteamproject.passmate.room.data.dto.RoomDetailResponse
import org.sesacteamproject.passmate.room.data.dto.RoomInfoResponse

// 전송만 담당 — AppResult 변환·매핑은 Repository가 한다 (규칙 §6)
class RoomRemoteDataSource(
    private val apiClient: ApiClient
) {
    suspend fun fetchRoomByPin(pin: String): RoomInfoResponse {
        return apiClient.http.get("${apiClient.baseUrl}/rooms/pin/$pin").body()
    }

    // 공개 방 목록에는 pin이 없어 roomId로 방을 조회해 pin을 얻는다
    suspend fun fetchRoomById(roomId: Long): RoomDetailResponse {
        return apiClient.http.get("${apiClient.baseUrl}/rooms/$roomId").body()
    }

    suspend fun join(roomId: Long, request: JoinRoomRequest): JoinRoomResponse {
        return apiClient.http.post("${apiClient.baseUrl}/rooms/$roomId/participants") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    // 서버는 참가자 배열을 그대로 준다 (계약 `ParticipantResponse[]`)
    suspend fun fetchParticipants(roomId: Long): List<ParticipantDto> {
        return apiClient.http.get("${apiClient.baseUrl}/rooms/$roomId/participants").body()
    }

    suspend fun leave(roomId: Long) {
        apiClient.http.delete("${apiClient.baseUrl}/rooms/$roomId/participants/me")
    }

    suspend fun fetchHostedRooms(cursor: String?): HostedRoomsResponse {
        return apiClient.http.get("${apiClient.baseUrl}/users/me/rooms/hosted") {
            if (cursor != null) {
                parameter("cursor", cursor)
            }
        }.body()
    }

    suspend fun createRoom(request: CreateRoomRequest): CreateRoomResponse {
        return apiClient.http.post("${apiClient.baseUrl}/rooms") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
