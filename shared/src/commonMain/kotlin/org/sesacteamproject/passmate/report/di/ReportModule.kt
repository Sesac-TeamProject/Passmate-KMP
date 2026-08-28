package org.sesacteamproject.passmate.report.di

import org.koin.dsl.module
import org.sesacteamproject.passmate.report.data.remote.ResultRemoteDataSource
import org.sesacteamproject.passmate.report.data.repository.ResultRepositoryImpl
import org.sesacteamproject.passmate.report.domain.repository.ResultRepository
import org.sesacteamproject.passmate.report.domain.usecase.BuildReportSummaryUseCase
import org.sesacteamproject.passmate.report.domain.usecase.GetLearningReportUseCase
import org.sesacteamproject.passmate.report.domain.usecase.GetSessionResultUseCase

val reportModule = module {
    single { ResultRemoteDataSource(get()) }
    single<ResultRepository> { ResultRepositoryImpl(get()) }
    factory { GetSessionResultUseCase(get()) }
    factory { GetLearningReportUseCase(get()) }
    factory { BuildReportSummaryUseCase() }
}
