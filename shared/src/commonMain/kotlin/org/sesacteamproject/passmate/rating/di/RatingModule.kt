package org.sesacteamproject.passmate.rating.di

import org.koin.dsl.module
import org.sesacteamproject.passmate.rating.data.remote.RatingRemoteDataSource
import org.sesacteamproject.passmate.rating.data.repository.RatingRepositoryImpl
import org.sesacteamproject.passmate.rating.domain.repository.RatingRepository
import org.sesacteamproject.passmate.rating.domain.usecase.SubmitRatingUseCase

val ratingModule = module {
    single { RatingRemoteDataSource(get()) }
    single<RatingRepository> { RatingRepositoryImpl(get()) }
    factory { SubmitRatingUseCase(get()) }
}
