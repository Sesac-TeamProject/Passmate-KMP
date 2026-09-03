package org.sesacteamproject.passmate.report.data.mapper

import kotlin.math.roundToInt
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

// 서버는 guest·rating.available로 준다 (계약 )
fun SessionResultResponse.toDomain(): SessionResult {
    return SessionResult(
        roomTitle = roomTitle,
        rank = rank,
        totalScore = totalScore.toDouble(),
        correctCount = correctCount,
        questionCount = questionCount,
        questions = questions.map { it.toDomain() },
        canRate = rating?.available ?: false,
        isGuest = guest
    )
}

fun SessionResultResponse.AnswerResultDto.toDomain(): QuestionResult {
    return QuestionResult(
        questionId = questionId,
        questionNo = orderNo,
        title = content,
        type = QuestionType.from(type),
        verdict = verdictOf(),
        myAnswer = submitted,
        correctAnswer = answer,
        explanation = explanation,
        // 첨삭이 있으면 finalScore가 최종 점수다
        earnedScore = (finalScore ?: score).toDouble(),
        aiFeedback = analysis?.toDomain(analysisStatus),
        hostReview = teacherReview?.toDomain()
    )
}

// 서버는 정오를 isCorrect(boolean)로 주고 서술형은 분석 상태로 구분한다.
// 미제출은 채점 대상이 아니다.
private fun SessionResultResponse.AnswerResultDto.verdictOf(): AnswerVerdict {
    val correct = isCorrect

    return if (submitted == null) {
        AnswerVerdict.UNGRADED
    } else if (correct == true) {
        AnswerVerdict.CORRECT
    } else if (correct == false) {
        AnswerVerdict.WRONG
    } else {
        AnswerVerdict.from(analysisStatus)
    }
}

fun SessionResultResponse.EssayAnalysisDto.toDomain(status: String?): AiFeedback {
    return AiFeedback(
        status = AiFeedbackStatus.from(status),
        coveredConcepts = keyPoints,
        missingConcepts = missingPoints,
        weaknesses = summary,
        improvement = suggestions,
        suggestedScore = null
    )
}

fun SessionResultResponse.TeacherReviewDto.toDomain(): HostReview {
    return HostReview(
        comment = comment,
        improvement = improvement,
        adjustedScore = adjustedScore?.toDouble()
    )
}

// 서버 accuracy는 0~100 퍼센트다
fun LearningReportResponse.toDomain(): LearningReport {
    return LearningReport(
        accuracyPercent = accuracy.roundToInt(),
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
