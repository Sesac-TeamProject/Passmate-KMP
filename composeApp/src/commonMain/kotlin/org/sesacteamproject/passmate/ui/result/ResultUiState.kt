package org.sesacteamproject.passmate.ui.result

import org.sesacteamproject.passmate.report.domain.model.LearningReport
import org.sesacteamproject.passmate.report.domain.model.SessionResult

data class ResultUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val result: SessionResult? = null,
    val report: LearningReport? = null,
    val selectedQuestionNo: Int? = null,
    val isSharing: Boolean = false
)
