package org.sesacteamproject.passmate.question.data.mapper

import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.question.data.dto.QuestionSetsResponse
import org.sesacteamproject.passmate.question.domain.model.QuestionSetSummary

fun QuestionSetsResponse.toDomain(): PagedResult<QuestionSetSummary> {
    return PagedResult(
        items = items.map { it.toDomain() },
        nextCursor = nextCursor,
        hasNext = hasNext
    )
}

fun QuestionSetsResponse.QuestionSetDto.toDomain(): QuestionSetSummary {
    return QuestionSetSummary(
        setId = setId,
        title = title,
        isConfirmed = status?.uppercase() == "CONFIRMED",
        questionCount = questionCount
    )
}
