package org.sesacteamproject.passmate.ui.hostroom

import org.sesacteamproject.passmate.question.domain.model.QuestionSetSummary

data class CreateRoomUiState(
    val isLoadingSets: Boolean = true,
    val setsLoadFailed: Boolean = false,
    val sets: List<QuestionSetSummary> = emptyList(),
    val title: String = "",
    val selectedSetId: Long? = null,
    val isPaid: Boolean = false,
    val entryFeeText: String = "",
    // 제출 in-flight — 중복 호출 방지 (규칙 §9)
    val isSubmitting: Boolean = false
) {

    val selectedSet: QuestionSetSummary?
        get() = sets.firstOrNull { it.setId == selectedSetId }

    val canSubmit: Boolean
        get() = !isSubmitting &&
            title.isNotBlank() &&
            selectedSetId != null &&
            (!isPaid || (entryFeeText.toIntOrNull() ?: 0) > 0)
}
