package org.sesacteamproject.passmate.smoke

import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.sesacteamproject.passmate.core.di.initKoin
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.report.domain.usecase.GetLearningReportUseCase
import org.sesacteamproject.passmate.report.domain.usecase.GetSessionResultUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.JoinRoomUseCase
import org.sesacteamproject.passmate.session.domain.usecase.GetSessionSnapshotUseCase
import org.sesacteamproject.passmate.session.domain.usecase.SubmitAnswerUseCase

// 풀이~결과 구간 스모크 — 문항이 있는 방이 필요하고, 세션 시작·다음 문항은
// 호스트가 REST로 진행한다(제어는 REST, 전파는 WS). 호스트 토큰을 함께 준다:
//
//   SMOKE_PIN=028627 SMOKE_HOST_TOKEN=<accessToken> \
//     ./gradlew :shared:jvmTest --tests '*LivePlayFlowSmokeTest*' -i
class LivePlayFlowSmokeTest {

    @Test
    fun playFlowAgainstLocalBackend() = runBlocking {
        val pin = System.getenv("SMOKE_PIN")
        val hostToken = System.getenv("SMOKE_HOST_TOKEN")

        if (pin.isNullOrBlank() || hostToken.isNullOrBlank()) {
            println("[SMOKE] SMOKE_PIN·SMOKE_HOST_TOKEN이 없어 건너뜁니다")
            return@runBlocking
        }
        initKoin()
        try {
            runPlayFlow(pin, hostToken)
        } finally {
            stopKoin()
        }
    }

    private suspend fun runPlayFlow(pin: String, hostToken: String) {
        val koin = KoinPlatform.getKoin()
        val roomResult = koin.get<GetRoomInfoUseCase>().invoke(pin)

        check(roomResult is AppResult.Success) { "방 조회 실패" }
        val room = roomResult.value
        val join = koin.get<JoinRoomUseCase>().invoke(room, "풀이스모크${(1..9999).random()}", 3)

        println("[SMOKE] joinRoom → $join")
        check(join is AppResult.Success) { "게스트 입장 실패" }

        HostControl(hostToken).startSession(room.roomId)
        val snapshot = koin.get<GetSessionSnapshotUseCase>().invoke(room.roomId)

        println("[SMOKE] snapshot(RUNNING) → $snapshot")
        check(snapshot is AppResult.Success) { "스냅샷 실패" }

        val question = snapshot.value.currentQuestion
        println("[SMOKE] currentQuestion → $question")
        check(question != null) { "세션 시작 후에도 현재 문항이 비었다" }

        val submit = koin.get<SubmitAnswerUseCase>().invoke(room.roomId, question.questionId, "2")
        println("[SMOKE] submitAnswer → $submit")
        check(submit is AppResult.Success) { "답안 제출 실패" }

        HostControl(hostToken).endSession(room.roomId)
        val result = koin.get<GetSessionResultUseCase>().invoke(room.roomId)
        val report = koin.get<GetLearningReportUseCase>().invoke(room.roomId)

        println("[SMOKE] sessionResult → $result")
        println("[SMOKE] learningReport → $report")
        check(result is AppResult.Success) { "결과 조회 실패" }
    }
}
