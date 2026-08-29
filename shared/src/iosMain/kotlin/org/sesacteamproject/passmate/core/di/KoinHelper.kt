package org.sesacteamproject.passmate.core.di

import org.koin.mp.KoinPlatform
import org.sesacteamproject.passmate.auth.domain.usecase.BuildGoogleSignInUrlUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.CompleteSignInUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.SignOutUseCase
import org.sesacteamproject.passmate.core.network.SessionEventStreamWatcher
import org.sesacteamproject.passmate.payment.domain.policy.CoinPolicy
import org.sesacteamproject.passmate.payment.domain.usecase.ConfirmChargeUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetCoinTransactionsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetEarningsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetMyCoinsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetPublicRoomsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetSettlementAccountUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.SaveSettlementAccountUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.SetPaymentMethodUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.PayEntryFeeUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.RequestChargeUseCase
import org.sesacteamproject.passmate.rating.domain.usecase.SubmitRatingUseCase
import org.sesacteamproject.passmate.report.domain.usecase.BuildReportSummaryUseCase
import org.sesacteamproject.passmate.report.domain.usecase.BuildRoomReportSummaryUseCase
import org.sesacteamproject.passmate.report.domain.usecase.GetLearningReportUseCase
import org.sesacteamproject.passmate.report.domain.usecase.GetRoomReportUseCase
import org.sesacteamproject.passmate.report.domain.usecase.GetSessionResultUseCase
import org.sesacteamproject.passmate.question.domain.usecase.GetMyQuestionSetsUseCase
import org.sesacteamproject.passmate.room.domain.policy.JoinInputPolicy
import org.sesacteamproject.passmate.room.domain.usecase.CreateRoomUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetHostedRoomsUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetMyParticipationUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetParticipantsUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.JoinRoomUseCase
import org.sesacteamproject.passmate.room.domain.usecase.LeaveRoomUseCase
import org.sesacteamproject.passmate.session.domain.policy.SnapshotPolicy
import org.sesacteamproject.passmate.session.domain.usecase.EndCurrentQuestionUseCase
import org.sesacteamproject.passmate.session.domain.usecase.EndSessionUseCase
import org.sesacteamproject.passmate.session.domain.usecase.GetSessionSnapshotUseCase
import org.sesacteamproject.passmate.session.domain.usecase.GetSubmissionsUseCase
import org.sesacteamproject.passmate.session.domain.usecase.GetVoiceHintsUseCase
import org.sesacteamproject.passmate.session.domain.usecase.NextQuestionUseCase
import org.sesacteamproject.passmate.session.domain.usecase.PublishVoiceHintUseCase
import org.sesacteamproject.passmate.session.domain.usecase.SetScreenLockUseCase
import org.sesacteamproject.passmate.session.domain.usecase.StartSessionUseCase
import org.sesacteamproject.passmate.session.domain.usecase.SubmitAnswerUseCase
import org.sesacteamproject.passmate.user.domain.usecase.BlockHostUseCase
import org.sesacteamproject.passmate.user.domain.usecase.CompleteGuestClaimUseCase
import org.sesacteamproject.passmate.user.domain.usecase.DeleteAccountUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetMyProfileUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetNotificationSettingsUseCase
import org.sesacteamproject.passmate.user.domain.usecase.UpdateMyProfileUseCase
import org.sesacteamproject.passmate.user.domain.usecase.UpdateNotificationSettingsUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetHostProfileUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetMyBadgesUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetMyGradeUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetMyPageUseCase
import org.sesacteamproject.passmate.user.domain.usecase.ReportHostUseCase
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

    fun getMyGradeUseCase(): GetMyGradeUseCase = KoinPlatform.getKoin().get()

    fun getMyBadgesUseCase(): GetMyBadgesUseCase = KoinPlatform.getKoin().get()

    fun getHostProfileUseCase(): GetHostProfileUseCase = KoinPlatform.getKoin().get()

    fun blockHostUseCase(): BlockHostUseCase = KoinPlatform.getKoin().get()

    fun reportHostUseCase(): ReportHostUseCase = KoinPlatform.getKoin().get()

    fun signOutUseCase(): SignOutUseCase = KoinPlatform.getKoin().get()

    fun getMyProfileUseCase(): GetMyProfileUseCase = KoinPlatform.getKoin().get()

    fun updateMyProfileUseCase(): UpdateMyProfileUseCase = KoinPlatform.getKoin().get()

    fun deleteAccountUseCase(): DeleteAccountUseCase = KoinPlatform.getKoin().get()

    fun getNotificationSettingsUseCase(): GetNotificationSettingsUseCase = KoinPlatform.getKoin().get()

    fun updateNotificationSettingsUseCase(): UpdateNotificationSettingsUseCase = KoinPlatform.getKoin().get()

    fun setPaymentMethodUseCase(): SetPaymentMethodUseCase = KoinPlatform.getKoin().get()

    fun getHostedRoomsUseCase(): GetHostedRoomsUseCase = KoinPlatform.getKoin().get()

    fun createRoomUseCase(): CreateRoomUseCase = KoinPlatform.getKoin().get()

    fun getMyQuestionSetsUseCase(): GetMyQuestionSetsUseCase = KoinPlatform.getKoin().get()

    fun getRoomReportUseCase(): GetRoomReportUseCase = KoinPlatform.getKoin().get()

    fun buildRoomReportSummaryUseCase(): BuildRoomReportSummaryUseCase = KoinPlatform.getKoin().get()

    fun startSessionUseCase(): StartSessionUseCase = KoinPlatform.getKoin().get()

    fun nextQuestionUseCase(): NextQuestionUseCase = KoinPlatform.getKoin().get()

    fun endCurrentQuestionUseCase(): EndCurrentQuestionUseCase = KoinPlatform.getKoin().get()

    fun endSessionUseCase(): EndSessionUseCase = KoinPlatform.getKoin().get()

    fun setScreenLockUseCase(): SetScreenLockUseCase = KoinPlatform.getKoin().get()

    fun getSubmissionsUseCase(): GetSubmissionsUseCase = KoinPlatform.getKoin().get()

    fun publishVoiceHintUseCase(): PublishVoiceHintUseCase = KoinPlatform.getKoin().get()

    fun getEarningsUseCase(): GetEarningsUseCase = KoinPlatform.getKoin().get()

    fun getSettlementAccountUseCase(): GetSettlementAccountUseCase = KoinPlatform.getKoin().get()

    fun saveSettlementAccountUseCase(): SaveSettlementAccountUseCase = KoinPlatform.getKoin().get()

    fun sessionEventStreamWatcher(): SessionEventStreamWatcher = SessionEventStreamWatcher(KoinPlatform.getKoin().get())
}
