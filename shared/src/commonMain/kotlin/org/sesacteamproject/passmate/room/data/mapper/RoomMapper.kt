package org.sesacteamproject.passmate.room.data.mapper

import org.sesacteamproject.passmate.room.data.dto.ParticipantsResponse
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.room.data.dto.CreateRoomResponse
import org.sesacteamproject.passmate.room.data.dto.HostedRoomsResponse
import org.sesacteamproject.passmate.room.data.dto.RoomInfoResponse
import org.sesacteamproject.passmate.room.domain.model.CreatedRoom
import org.sesacteamproject.passmate.room.domain.model.HostedRoom
import org.sesacteamproject.passmate.room.domain.model.Participant
import org.sesacteamproject.passmate.room.domain.model.RoomHost
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus

fun RoomInfoResponse.toDomain(): RoomInfo {
    return RoomInfo(
        roomId = roomId,
        pin = pin,
        title = title,
        topic = topic,
        status = RoomStatus.from(status),
        questionCount = questionCount,
        estimatedMinutes = estimatedMinutes,
        scheduledAt = scheduledAt,
        participantCount = participantCount,
        maxParticipants = maxParticipants,
        isPaid = isPaid,
        entryFee = entryFee,
        host = host?.toDomain()
    )
}

fun RoomInfoResponse.Host.toDomain(): RoomHost {
    return RoomHost(
        userId = userId,
        nickname = nickname,
        level = level,
        avgStars = avgStars,
        ratingCount = ratingCount
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
