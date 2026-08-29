package org.sesacteamproject.passmate.report.data.mapper

import org.sesacteamproject.passmate.report.data.dto.LearningReportResponse
import org.sesacteamproject.passmate.report.data.dto.RoomReportResponse
import org.sesacteamproject.passmate.report.data.dto.SessionResultResponse
import org.sesacteamproject.passmate.report.domain.model.AiFeedback
import org.sesacteamproject.passmate.report.domain.model.AiFeedbackStatus
import org.sesacteamproject.passmate.report.domain.model.AnswerVerdict
import org.sesacteamproject.passmate.report.domain.model.HostReview
import org.sesacteamproject.passmate.report.domain.model.LearningReport
import org.sesacteamproject.passmate.report.domain.model.QuestionResult
import org.sesacteamproject.passmate.report.domain.model.ReportQuestion
import org.sesacteamproject.passmate.report.domain.model.ReportStudent
import org.sesacteamproject.passmate.report.domain.model.RoomReport
import org.sesacteamproject.passmate.report.domain.model.RoomReportSummary
import org.sesacteamproject.passmate.report.domain.model.SessionResult
import org.sesacteamproject.passmate.room.domain.model.RoomStatus
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

fun RoomReportResponse.toDomain(): RoomReport {
    return RoomReport(
        roomTitle = roomTitle,
        pin = pin,
        status = RoomStatus.from(status),
        dateLabel = dateLabel,
        summary = summary.toDomain(),
        questions = questions.map { it.toDomain() },
        students = students.map { it.toDomain() }
    )
}

fun RoomReportResponse.SummaryDto.toDomain(): RoomReportSummary {
    return RoomReportSummary(
        avgAccuracyPercent = avgAccuracyPercent,
        studentCount = studentCount,
        questionCount = questionCount,
        aiAnalysisCount = aiAnalysisCount,
        avgScore = avgScore,
        topScore = topScore
    )
}

fun RoomReportResponse.QuestionDto.toDomain(): ReportQuestion {
    return ReportQuestion(
        questionId = questionId,
        questionNo = questionNo,
        title = title,
        type = QuestionType.from(type),
        accuracyPercent = accuracyPercent,
        aiFeedbackCount = aiFeedbackCount
    )
}

fun RoomReportResponse.StudentDto.toDomain(): ReportStudent {
    return ReportStudent(
        participantId = participantId,
        nickname = nickname,
        rank = rank,
        totalScore = totalScore,
        correctCount = correctCount,
        isGuest = isGuest
    )
}
