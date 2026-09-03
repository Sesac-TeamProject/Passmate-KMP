package org.sesacteamproject.passmate.session.data.mapper

import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.room.domain.model.StudentAvatarKeys
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

// 서버 본문에 ts가 없어 응답 HTTP Date 헤더의 서버 시각을 받아 채운다.
// 문항별 내 답변 목록·누적 점수·순위도 스냅샷에 없다 (계약 갱신 대상) —
// 제출 여부만 오므로 myAnswers는 비우고, 점수·순위는 랭킹으로 갱신된다.
fun SessionSnapshotResponse.toDomain(serverTime: String): SessionSnapshot {
    return SessionSnapshot(
        status = RoomStatus.from(status),
        ts = serverTime,
        questionCount = totalCount,
        currentQuestion = currentQuestion?.toDomain(),
        // 서버는 문항별 답변 목록 대신 현재 문항 제출 여부만 준다 — 중복 제출 차단(§9)에 쓰인다
        myAnswers = submittedAnswers(),
        totalScore = null,
        rank = null,
        ranking = ranking.map { it.toDomain() },
        isLocked = screenLocked
    )
}

// 제출 여부만 오므로 현재 문항 한 건으로 복원한다. 정오·점수는 서버가 따로 주지 않는다.
private fun SessionSnapshotResponse.submittedAnswers(): List<SubmittedAnswer> {
    val question = currentQuestion

    return if (submitted && question != null) {
        listOf(
            SubmittedAnswer(
                questionId = question.questionId,
                correct = null,
                earnedScore = null,
                isProvisional = true
            )
        )
    } else {
        emptyList()
    }
}

fun SessionSnapshotResponse.QuestionDto.toDomain(): SessionQuestion {
    return SessionQuestion(
        questionId = questionId,
        questionNo = orderNo,
        type = QuestionType.from(type),
        body = content,
        choices = choices,
        points = points,
        timeLimitSec = timeLimitSec,
        endsAt = endsAt,
        // 마감 여부는 QUESTION_ENDED 이벤트가 알린다 — 스냅샷에는 없다
        isClosed = false
    )
}

fun SessionSnapshotResponse.RankingEntryDto.toDomain(): RankEntry {
    return RankEntry(
        rank = rank,
        participantId = participantId,
        nickname = nickname,
        avatarId = StudentAvatarKeys.toIndex(avatarId),
        total = totalScore.toDouble()
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

// 누적 점수·순위는 응답에 없다 — RANKING_UPDATED 이벤트가 갱신한다.
// 서술형은 isCorrect가 null로 와서 잠정 채점으로 표시한다.
fun SubmitAnswerResponse.toDomain(): AnswerResult {
    return AnswerResult(
        correct = isCorrect,
        baseScore = baseScore.toDouble(),
        speedBonus = speedBonus.toDouble(),
        earnedScore = score.toDouble(),
        totalScore = 0.0,
        rank = null,
        rankDelta = null,
        isProvisional = isCorrect == null
    )
}
