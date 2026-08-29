package org.sesacteamproject.passmate.question.di

import org.koin.dsl.module
import org.sesacteamproject.passmate.question.data.remote.QuestionRemoteDataSource
import org.sesacteamproject.passmate.question.data.repository.QuestionRepositoryImpl
import org.sesacteamproject.passmate.question.domain.repository.QuestionRepository
import org.sesacteamproject.passmate.question.domain.usecase.GetMyQuestionSetsUseCase

val questionModule = module {
    single { QuestionRemoteDataSource(get()) }
    single<QuestionRepository> { QuestionRepositoryImpl(get()) }
    factory { GetMyQuestionSetsUseCase(get()) }
}
