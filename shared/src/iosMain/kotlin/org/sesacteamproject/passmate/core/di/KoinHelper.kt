package org.sesacteamproject.passmate.core.di

import org.koin.mp.KoinPlatform
import org.sesacteamproject.passmate.auth.domain.usecase.BuildGoogleSignInUrlUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.CompleteSignInUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.network.SessionEventStreamWatcher
import org.sesacteamproject.passmate.payment.domain.policy.CoinPolicy
import org.sesacteamproject.passmate.payment.domain.usecase.ConfirmChargeUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetCoinTransactionsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetMyCoinsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetPublicRoomsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.PayEntryFeeUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.RequestChargeUseCase
import org.sesacteamproject.passmate.rating.domain.usecase.SubmitRatingUseCase
import org.sesacteamproject.passmate.report.domain.usecase.BuildReportSummaryUseCase
import org.sesacteamproject.passmate.report.domain.usecase.GetLearningReportUseCase
import org.sesacteamproject.passmate.report.domain.usecase.GetSessionResultUseCase
import org.sesacteamproject.passmate.room.domain.policy.JoinInputPolicy
import org.sesacteamproject.passmate.room.domain.usecase.GetMyParticipationUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetParticipantsUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.JoinRoomUseCase
import org.sesacteamproject.passmate.room.domain.usecase.LeaveRoomUseCase
import org.sesacteamproject.passmate.session.domain.policy.SnapshotPolicy
import org.sesacteamproject.passmate.session.domain.usecase.GetSessionSnapshotUseCase
import org.sesacteamproject.passmate.session.domain.usecase.GetVoiceHintsUseCase
import org.sesacteamproject.passmate.session.domain.usecase.SubmitAnswerUseCase
import org.sesacteamproject.passmate.user.domain.usecase.CompleteGuestClaimUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetMyPageUseCase
import org.sesacteamproject.passmate.user.domain.usecase.RequestGuestClaimUseCase

// Swift는 reified 제네릭을 못 쓰므로 화면(Swift VM)별 의존성을 명시 getter로 노출한다 (아키텍처 설계 §4-5)
object KoinHelper {

    fun doInitKoin() {
        initKoin()
    }

    fun buildGoogleSignInUrlUseCase(): BuildGoogleSignInUrlUseCase = KoinPlatform.getKoin().get()

    fun completeSignInUseCase(): CompleteSignInUseCase = KoinPlatform.getKoin().get()

    fun isSignedInUseCase(): IsSignedInUseCase = KoinPlatform.getKoin().get()

    fun joinInputPolicy(): JoinInputPolicy = KoinPlatform.getKoin().get()

    fun getRoomInfoUseCase(): GetRoomInfoUseCase = KoinPlatform.getKoin().get()

    fun joinRoomUseCase(): JoinRoomUseCase = KoinPlatform.getKoin().get()

    fun getParticipantsUseCase(): GetParticipantsUseCase = KoinPlatform.getKoin().get()

    fun leaveRoomUseCase(): LeaveRoomUseCase = KoinPlatform.getKoin().get()

    fun getMyParticipationUseCase(): GetMyParticipationUseCase = KoinPlatform.getKoin().get()

    fun getSessionSnapshotUseCase(): GetSessionSnapshotUseCase = KoinPlatform.getKoin().get()

    fun submitAnswerUseCase(): SubmitAnswerUseCase = KoinPlatform.getKoin().get()

    fun getVoiceHintsUseCase(): GetVoiceHintsUseCase = KoinPlatform.getKoin().get()

    fun snapshotPolicy(): SnapshotPolicy = KoinPlatform.getKoin().get()

    fun getSessionResultUseCase(): GetSessionResultUseCase = KoinPlatform.getKoin().get()

    fun getLearningReportUseCase(): GetLearningReportUseCase = KoinPlatform.getKoin().get()

    fun buildReportSummaryUseCase(): BuildReportSummaryUseCase = KoinPlatform.getKoin().get()

    fun getMyPageUseCase(): GetMyPageUseCase = KoinPlatform.getKoin().get()

    fun requestGuestClaimUseCase(): RequestGuestClaimUseCase = KoinPlatform.getKoin().get()

    fun completeGuestClaimUseCase(): CompleteGuestClaimUseCase = KoinPlatform.getKoin().get()

    fun submitRatingUseCase(): SubmitRatingUseCase = KoinPlatform.getKoin().get()

    fun getMyCoinsUseCase(): GetMyCoinsUseCase = KoinPlatform.getKoin().get()

    fun getCoinTransactionsUseCase(): GetCoinTransactionsUseCase = KoinPlatform.getKoin().get()

    fun requestChargeUseCase(): RequestChargeUseCase = KoinPlatform.getKoin().get()

    fun confirmChargeUseCase(): ConfirmChargeUseCase = KoinPlatform.getKoin().get()

    fun payEntryFeeUseCase(): PayEntryFeeUseCase = KoinPlatform.getKoin().get()

    fun getPublicRoomsUseCase(): GetPublicRoomsUseCase = KoinPlatform.getKoin().get()

    fun coinPolicy(): CoinPolicy = KoinPlatform.getKoin().get()

    fun sessionEventStreamWatcher(): SessionEventStreamWatcher = SessionEventStreamWatcher(KoinPlatform.getKoin().get())
}
