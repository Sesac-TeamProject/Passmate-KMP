package org.sesacteamproject.passmate.core.model

data class PagedResult<T>(
    val items: List<T>,
    val nextCursor: String?,
    val hasNext: Boolean
)
