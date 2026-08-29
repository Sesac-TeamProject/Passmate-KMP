package org.sesacteamproject.passmate.room.data.mapper

import org.sesacteamproject.passmate.room.data.dto.ParticipantsResponse
import org.sesacteamproject.passmate.room.data.dto.RoomInfoResponse
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
