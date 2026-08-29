package org.sesacteamproject.passmate.report.domain.model

import org.sesacteamproject.passmate.room.domain.model.RoomStatus
import org.sesacteamproject.passmate.session.domain.model.QuestionType

// 선생님용 방 리포트 (GET /rooms/{roomId}/results) — M-14 (FR-031)
data class RoomReport(
    val roomTitle: String,
    val pin: String,
    val status: RoomStatus,
    val dateLabel: String?,
    val summary: RoomReportSummary,
    val questions: List<ReportQuestion>,
    val students: List<ReportStudent>
)

data class RoomReportSummary(
    val avgAccuracyPercent: Int?,
    val studentCount: Int,
    val questionCount: Int,
    val aiAnalysisCount: Int,
    val avgScore: Double?,
    val topScore: Double?
)

// 문항별 통계 1건 — 서술형 미채점은 accuracyPercent null ("—" 렌더링)
data class ReportQuestion(
    val questionId: Long,
    val questionNo: Int,
    val title: String,
    val type: QuestionType,
    val accuracyPercent: Int?,
    val aiFeedbackCount: Int?
)

data class ReportStudent(
    val participantId: Long,
    val nickname: String,
    val rank: Int?,
    val totalScore: Double,
    val correctCount: Int,
    val isGuest: Boolean
)
