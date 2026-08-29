package org.sesacteamproject.passmate.session.data.mapper

import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.session.data.dto.SessionSnapshotResponse
import org.sesacteamproject.passmate.session.data.dto.SubmissionsResponse
import org.sesacteamproject.passmate.session.data.dto.SubmitAnswerResponse
import org.sesacteamproject.passmate.session.data.dto.VoiceHintsResponse
import org.sesacteamproject.passmate.session.domain.model.AnswerResult
import org.sesacteamproject.passmate.session.domain.model.ChoiceCount
import org.sesacteamproject.passmate.session.domain.model.QuestionType
import org.sesacteamproject.passmate.session.domain.model.RankEntry
import org.sesacteamproject.passmate.session.domain.model.SessionQuestion
import org.sesacteamproject.passmate.session.domain.model.SessionSnapshot
import org.sesacteamproject.passmate.session.domain.model.SubmissionParticipant
import org.sesacteamproject.passmate.session.domain.model.SubmissionStatus
import org.sesacteamproject.passmate.session.domain.model.SubmittedAnswer
import org.sesacteamproject.passmate.session.domain.model.VoiceHint

fun SessionSnapshotResponse.toDomain(): SessionSnapshot {
    return SessionSnapshot(
        status = RoomStatus.from(status),
        ts = ts,
        questionCount = questionCount,
        currentQuestion = currentQuestion?.toDomain(),
        myAnswers = myAnswers.map { it.toDomain() },
        totalScore = totalScore,
        rank = rank,
        ranking = ranking.map { it.toDomain() },
        isLocked = isLocked
    )
}

fun SessionSnapshotResponse.QuestionDto.toDomain(): SessionQuestion {
    return SessionQuestion(
        questionId = questionId,
        questionNo = questionNo,
        type = QuestionType.from(type),
        body = body,
        choices = choices.orEmpty(),
        points = points,
        timeLimitSec = timeLimitSec,
        endsAt = endsAt,
        isClosed = isClosed
    )
}

fun SessionSnapshotResponse.AnswerDto.toDomain(): SubmittedAnswer {
    return SubmittedAnswer(
        questionId = questionId,
        correct = correct,
        earnedScore = earnedScore,
        isProvisional = isProvisional
    )
}

fun SessionSnapshotResponse.RankingEntryDto.toDomain(): RankEntry {
    return RankEntry(
        rank = rank,
        participantId = participantId,
        nickname = nickname,
        avatarId = avatarId,
        total = total
    )
}

fun VoiceHintsResponse.Entry.toDomain(): VoiceHint {
    return VoiceHint(
        hintId = hintId,
        questionNo = questionNo,
        clipUrl = clipUrl,
        durationMs = durationMs
    )
}

fun SubmissionsResponse.toDomain(): SubmissionStatus {
    return SubmissionStatus(
        questionNo = questionNo,
        submittedCount = submittedCount,
        totalCount = totalCount,
        accuracyPercent = accuracyPercent,
        choices = choices.orEmpty().map { ChoiceCount(label = it.label, count = it.count) },
        participants = participants.map {
            SubmissionParticipant(
                participantId = it.participantId,
                nickname = it.nickname,
                avatarId = it.avatarId,
                submitted = it.submitted
            )
        }
    )
}

fun SubmitAnswerResponse.toDomain(): AnswerResult {
    return AnswerResult(
        correct = correct,
        baseScore = baseScore,
        speedBonus = speedBonus,
        earnedScore = earnedScore,
        totalScore = totalScore,
        rank = rank,
        rankDelta = rankDelta,
        isProvisional = isProvisional
    )
}
