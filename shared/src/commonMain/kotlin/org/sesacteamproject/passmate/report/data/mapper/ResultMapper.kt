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
        aiFeedback = aiFeedbackOrNull(),
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
        essayVerdictOf(analysisStatus)
    }
}

// 서술형은 서버가 정오를 매기지 않는다 — 분석 상태로 칩을 고른다.
// 분석 상태 어휘(DONE·FAILED·SKIPPED·PENDING)는 정오 어휘와 다르다.
private fun essayVerdictOf(analysisStatus: String?): AnswerVerdict {
    return when (analysisStatus?.uppercase()) {
        "DONE" -> AnswerVerdict.AI_ANALYZED
        "PENDING" -> AnswerVerdict.AI_PENDING
        else -> AnswerVerdict.UNGRADED
    }
}

// 분석 본문이 없어도 상태가 있으면 피드백 객체를 만든다 — 실패·대기를
// 에러가 아니라 "분석 불가" 상태로 보여줘야 한다 (규칙 §10).
private fun SessionResultResponse.AnswerResultDto.aiFeedbackOrNull(): AiFeedback? {
    val status = AiFeedbackStatus.from(analysisStatus)

    return if (status == AiFeedbackStatus.NONE && analysis == null) {
        null
    } else {
        AiFeedback(
            status = status,
            coveredConcepts = analysis?.keyPoints ?: emptyList(),
            missingConcepts = analysis?.missingPoints ?: emptyList(),
            weaknesses = analysis?.summary,
            improvement = analysis?.suggestions,
            suggestedScore = null
        )
    }
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
        roomTitle = title,
        // 서버가 pin을 주지 않는다 — 빈 값이면 화면이 PIN 조각을 생략한다 (백엔드 요청 중)
        pin = "",
        status = RoomStatus.from(status),
        dateLabel = startedAt?.substringBefore('T'),
        summary = summary.toDomain(),
        questions = questions.map { it.toDomain() },
        students = participants.map { it.toDomain() }
    )
}

fun RoomReportResponse.SummaryDto.toDomain(): RoomReportSummary {
    return RoomReportSummary(
        avgAccuracyPercent = avgCorrectRate?.roundToInt(),
        studentCount = participantCount,
        questionCount = questionCount,
        aiAnalysisCount = aiAnalysisCount,
        avgScore = avgScore,
        // 서버 응답에 최고점이 없다 — 참가자 목록에서 뽑지 않고 비운다(정렬 기준이 서버 권위다)
        topScore = null
    )
}

fun RoomReportResponse.QuestionDto.toDomain(): ReportQuestion {
    return ReportQuestion(
        questionId = questionId,
        questionNo = orderNo,
        title = content,
        type = QuestionType.from(type),
        accuracyPercent = correctRate?.roundToInt(),
        aiFeedbackCount = aiAnalysisCount
    )
}

fun RoomReportResponse.ParticipantDto.toDomain(): ReportStudent {
    return ReportStudent(
        participantId = participantId,
        nickname = nickname,
        rank = rank,
        totalScore = totalScore,
        correctCount = correctCount,
        // 서버가 게스트 여부를 주지 않는다 — 목록에서 구분 표시를 하지 않는다
        isGuest = false
    )
}
