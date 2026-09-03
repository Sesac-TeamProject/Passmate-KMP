package org.sesacteamproject.passmate.room.data.mapper

import org.sesacteamproject.passmate.room.data.dto.ParticipantDto
import kotlin.math.roundToInt
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.room.data.dto.CreateRoomResponse
import org.sesacteamproject.passmate.room.data.dto.HostedRoomsResponse
import org.sesacteamproject.passmate.room.data.dto.RoomInfoResponse
import org.sesacteamproject.passmate.room.domain.model.CreatedRoom
import org.sesacteamproject.passmate.room.domain.model.HostedRoom
import org.sesacteamproject.passmate.room.domain.model.Participant
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.model.StudentAvatarKeys

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

// 서버 avatarId는 문자열 키다 — 화면이 쓰는 1..12 인덱스로 바꾼다.
// 접속 여부는 이 응답에 없고 WS 이벤트로 갱신되므로 초기값은 접속 중으로 둔다.
fun ParticipantDto.toDomain(): Participant {
    return Participant(
        participantId = id,
        nickname = nickname,
        avatarId = StudentAvatarKeys.toIndex(avatarId),
        isGuest = isGuest,
        isConnected = true
    )
}

// 서버가 진행 중·종료를 나눠 주고 페이징하지 않는다. 화면은 status로 다시 가르므로
// 한 목록으로 합쳐 도메인 `PagedResult` 계약(§6)을 유지한다.
fun HostedRoomsResponse.toDomain(): PagedResult<HostedRoom> {
    return PagedResult(
        items = active.map { it.toDomain() } + ended.map { it.toDomain() },
        nextCursor = null,
        hasNext = false
    )
}

fun HostedRoomsResponse.ActiveRoomDto.toDomain(): HostedRoom {
    return HostedRoom(
        roomId = roomId,
        pin = pin,
        title = title,
        status = RoomStatus.from(status),
        participantCount = participantCount,
        scheduledAt = scheduledAt,
        endedAtLabel = null,
        avgAccuracyPercent = null
    )
}

// 종료 방에는 pin·status가 없다 — 종료로 확정하고 pin은 빈 값으로 둔다.
// 화면(M-13)은 종료 카드에서 PIN을 쓰지 않는다.
fun HostedRoomsResponse.EndedRoomDto.toDomain(): HostedRoom {
    return HostedRoom(
        roomId = roomId,
        pin = "",
        title = title,
        status = RoomStatus.FINISHED,
        participantCount = studentCount,
        scheduledAt = null,
        endedAtLabel = displayRoomDate(endedAt),
        avgAccuracyPercent = correctRate?.roundToInt()
    )
}

// LocalDateTime 문자열의 날짜 부분을 화면 표기(YYYY.MM.DD)로 바꾼다
private fun displayRoomDate(isoDateTime: String?): String? {
    val date = isoDateTime?.substringBefore("T")

    return if (date != null && date.length == 10) {
        date.replace("-", ".")
    } else {
        null
    }
}

fun CreateRoomResponse.toDomain(): CreatedRoom {
    return CreatedRoom(
        roomId = roomId,
        pin = pin,
        qrUrl = qrUrl
    )
}
