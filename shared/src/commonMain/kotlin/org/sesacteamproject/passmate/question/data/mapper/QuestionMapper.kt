package org.sesacteamproject.passmate.question.data.mapper

import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.question.data.dto.QuestionSetsResponse
import org.sesacteamproject.passmate.question.domain.model.QuestionSetSummary

fun QuestionSetsResponse.toDomain(): PagedResult<QuestionSetSummary> {
    return PagedResult(
        items = content.map { it.toDomain() },
        // 서버는 page/size 기반이라 커서가 없다 — 다음 페이지 번호를 커서 자리에 싣는다
        nextCursor = if (hasNext) (page + 1).toString() else null,
        hasNext = hasNext
    )
}

fun QuestionSetsResponse.QuestionSetDto.toDomain(): QuestionSetSummary {
    return QuestionSetSummary(
        setId = id,
        title = title,
        isConfirmed = status?.uppercase() == "CONFIRMED",
        questionCount = questionCount
    )
}
