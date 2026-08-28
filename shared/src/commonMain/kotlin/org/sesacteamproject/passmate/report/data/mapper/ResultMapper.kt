package org.sesacteamproject.passmate.report.data.mapper

import org.sesacteamproject.passmate.report.data.dto.LearningReportResponse
import org.sesacteamproject.passmate.report.data.dto.SessionResultResponse
import org.sesacteamproject.passmate.report.domain.model.AiFeedback
import org.sesacteamproject.passmate.report.domain.model.AiFeedbackStatus
import org.sesacteamproject.passmate.report.domain.model.AnswerVerdict
import org.sesacteamproject.passmate.report.domain.model.HostReview
import org.sesacteamproject.passmate.report.domain.model.LearningReport
import org.sesacteamproject.passmate.report.domain.model.QuestionResult
import org.sesacteamproject.passmate.report.domain.model.SessionResult
import org.sesacteamproject.passmate.session.domain.model.QuestionType

fun SessionResultResponse.toDomain(): SessionResult {
    return SessionResult(
        roomTitle = roomTitle,
        rank = rank,
        totalScore = totalScore,
        correctCount = correctCount,
        questionCount = questionCount,
        questions = questions.map { it.toDomain() },
        canRate = canRate,
        isGuest = isGuest
    )
}

fun SessionResultResponse.QuestionDto.toDomain(): QuestionResult {
    return QuestionResult(
        questionId = questionId,
        questionNo = questionNo,
        title = title,
        type = QuestionType.from(type),
        verdict = AnswerVerdict.from(verdict),
        myAnswer = myAnswer,
        correctAnswer = correctAnswer,
        explanation = explanation,
        earnedScore = earnedScore,
        aiFeedback = aiFeedback?.toDomain(),
        hostReview = hostReview?.toDomain()
    )
}

fun SessionResultResponse.AiFeedbackDto.toDomain(): AiFeedback {
    return AiFeedback(
        status = AiFeedbackStatus.from(status),
        coveredConcepts = coveredConcepts,
        missingConcepts = missingConcepts,
        weaknesses = weaknesses,
        improvement = improvement,
        suggestedScore = suggestedScore
    )
}

fun SessionResultResponse.HostReviewDto.toDomain(): HostReview {
    return HostReview(
        comment = comment,
        improvement = improvement,
        adjustedScore = adjustedScore
    )
}

fun LearningReportResponse.toDomain(): LearningReport {
    return LearningReport(
        accuracyPercent = accuracyPercent,
        weakTopics = weakTopics,
        improvementPoints = improvementPoints
    )
}
