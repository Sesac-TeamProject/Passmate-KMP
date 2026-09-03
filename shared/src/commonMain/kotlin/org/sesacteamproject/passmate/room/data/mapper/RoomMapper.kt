package org.sesacteamproject.passmate.room.data.mapper

import org.sesacteamproject.passmate.room.data.dto.ParticipantsResponse
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.room.data.dto.CreateRoomResponse
import org.sesacteamproject.passmate.room.data.dto.HostedRoomsResponse
import org.sesacteamproject.passmate.room.data.dto.RoomInfoResponse
import org.sesacteamproject.passmate.room.domain.model.CreatedRoom
import org.sesacteamproject.passmate.room.domain.model.HostedRoom
import org.sesacteamproject.passmate.room.domain.model.Participant
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus

// 서버 `RoomSummaryResponse`에는 pin이 없다 — 조회 키로 쓴 pin을 그대로 채운다.
// questionCount·estimatedMinutes·scheduledAt·host도 주지 않아 null로 내려간다 (계약 갱신 대상).
fun RoomInfoResponse.toDomain(pin: String): RoomInfo {
    return RoomInfo(
        roomId = id,
        pin = pin,
        title = title,
        topic = topic,
        status = RoomStatus.from(status),
        questionCount = null,
        estimatedMinutes = null,
        scheduledAt = null,
        participantCount = participantCount,
        maxParticipants = maxParticipants,
        isPaid = type.equals("PAID", ignoreCase = true),
        entryFee = fee,
        host = null
    )
}

fun ParticipantsResponse.Entry.toDomain(): Participant {
    return Participant(
        participantId = participantId,
        nickname = nickname,
        avatarId = avatarId,
        isGuest = isGuest,
        isConnected = isConnected
    )
}

fun HostedRoomsResponse.toDomain(): PagedResult<HostedRoom> {
    return PagedResult(
        items = items.map { it.toDomain() },
        nextCursor = nextCursor,
        hasNext = hasNext
    )
}

fun HostedRoomsResponse.HostedRoomDto.toDomain(): HostedRoom {
    return HostedRoom(
        roomId = roomId,
        pin = pin,
        title = title,
        status = RoomStatus.from(status),
        participantCount = participantCount,
        scheduledAt = scheduledAt,
        endedAtLabel = endedAtLabel,
        avgAccuracyPercent = avgAccuracyPercent
    )
}

fun CreateRoomResponse.toDomain(): CreatedRoom {
    return CreatedRoom(
        roomId = roomId,
        pin = pin,
        qrUrl = qrUrl
    )
}
