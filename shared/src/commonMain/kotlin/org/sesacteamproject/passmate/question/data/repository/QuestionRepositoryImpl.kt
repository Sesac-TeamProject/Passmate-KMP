package org.sesacteamproject.passmate.question.data.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.core.model.map
import org.sesacteamproject.passmate.core.network.apiCall
import org.sesacteamproject.passmate.question.data.mapper.toDomain
import org.sesacteamproject.passmate.question.data.remote.QuestionRemoteDataSource
import org.sesacteamproject.passmate.question.domain.model.QuestionSetSummary
import org.sesacteamproject.passmate.question.domain.repository.QuestionRepository

class QuestionRepositoryImpl(
    private val remoteDataSource: QuestionRemoteDataSource
) : QuestionRepository {

    override suspend fun getMySets(
        confirmedOnly: Boolean,
        cursor: String?
    ): AppResult<PagedResult<QuestionSetSummary>> {
        val status = if (confirmedOnly) "CONFIRMED" else null

        return apiCall { remoteDataSource.fetchMySets(status, cursor) }.map { it.toDomain() }
    }
}
