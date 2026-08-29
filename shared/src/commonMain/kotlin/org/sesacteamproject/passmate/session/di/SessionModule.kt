package org.sesacteamproject.passmate.session.di

import org.koin.dsl.module
import org.sesacteamproject.passmate.session.data.remote.SessionRemoteDataSource
import org.sesacteamproject.passmate.session.data.repository.SessionRepositoryImpl
import org.sesacteamproject.passmate.session.domain.policy.SnapshotPolicy
import org.sesacteamproject.passmate.session.domain.repository.SessionRepository
import org.sesacteamproject.passmate.session.domain.usecase.EndCurrentQuestionUseCase
import org.sesacteamproject.passmate.session.domain.usecase.EndSessionUseCase
import org.sesacteamproject.passmate.session.domain.usecase.GetSessionSnapshotUseCase
import org.sesacteamproject.passmate.session.domain.usecase.GetSubmissionsUseCase
import org.sesacteamproject.passmate.session.domain.usecase.GetVoiceHintsUseCase
import org.sesacteamproject.passmate.session.domain.usecase.NextQuestionUseCase
import org.sesacteamproject.passmate.session.domain.usecase.SetScreenLockUseCase
import org.sesacteamproject.passmate.session.domain.usecase.StartSessionUseCase
import org.sesacteamproject.passmate.session.domain.usecase.SubmitAnswerUseCase

val sessionModule = module {
    single { SessionRemoteDataSource(get()) }
    single<SessionRepository> { SessionRepositoryImpl(get()) }
    factory { SnapshotPolicy() }
    factory { GetSessionSnapshotUseCase(get()) }
    factory { SubmitAnswerUseCase(get()) }
    factory { GetVoiceHintsUseCase(get()) }
    factory { StartSessionUseCase(get()) }
    factory { NextQuestionUseCase(get()) }
    factory { EndCurrentQuestionUseCase(get()) }
    factory { EndSessionUseCase(get()) }
    factory { SetScreenLockUseCase(get()) }
    factory { GetSubmissionsUseCase(get()) }
}
