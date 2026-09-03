package org.sesacteamproject.passmate.smoke

import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.sesacteamproject.passmate.core.di.initKoin
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.network.SessionEventStream
import org.sesacteamproject.passmate.room.domain.usecase.GetParticipantsUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.JoinRoomUseCase
import org.sesacteamproject.passmate.session.domain.usecase.GetSessionSnapshotUseCase

// 게스트 입장 플로우 스모크 (규칙 §12) — 실제 백엔드에 붙어 DTO 정합을 확인한다.
// 서버가 필요하므로 기본은 건너뛴다. 돌리려면 로컬 백엔드를 띄우고 PIN을 준다:
//
//   SMOKE_PIN=370369 ./gradlew :shared:jvmTest --tests '*LiveGuestFlowSmokeTest*' -i
//
// 게스트 경로라 로그인이 필요 없다(팀 연동 가이드 권고). 매번 새 참가자를 만드므로
// 개발용 DB에서만 돌린다.
class LiveGuestFlowSmokeTest {

    @Test
    fun guestFlowAgainstLocalBackend() = runBlocking {
        val pin = System.getenv("SMOKE_PIN")

        if (pin.isNullOrBlank()) {
            println("[SMOKE] SMOKE_PIN이 없어 건너뜁니다 — 로컬 백엔드 연동 확인용 테스트입니다")
            return@runBlocking
        }
        initKoin()
        try {
            runGuestFlow(pin)
        } finally {
            stopKoin()
        }
    }

    private suspend fun runGuestFlow(pin: String) {
        val koin = KoinPlatform.getKoin()
        val roomResult = koin.get<GetRoomInfoUseCase>().invoke(pin)

        println("[SMOKE] getRoomInfo($pin) → $roomResult")
        check(roomResult is AppResult.Success) { "방 조회 실패 — 서버가 떴는지, PIN이 맞는지 확인" }

        val room = roomResult.value
        val join = koin.get<JoinRoomUseCase>().invoke(room, "스모크${(1..9999).random()}", 6)

        println("[SMOKE] joinRoom → $join")
        check(join is AppResult.Success) { "게스트 입장 실패" }

        val participants = koin.get<GetParticipantsUseCase>().invoke(room.roomId)
        val snapshot = koin.get<GetSessionSnapshotUseCase>().invoke(room.roomId)

        println("[SMOKE] getParticipants → $participants")
        println("[SMOKE] getSnapshot → $snapshot")
        check(participants is AppResult.Success) { "참가자 조회 실패" }
        check(snapshot is AppResult.Success) { "스냅샷 조회 실패" }
        // 스냅샷 ts는 응답 Date 헤더에서 채운다 (§2-1-2·§5) — 비면 카운트다운이 서지 않는다
        check(snapshot.value.ts.isNotBlank()) { "스냅샷 서버 시각이 비었다" }

        checkStompConnects(room.roomId)
    }

    // 세션 전환(GAME_STARTED 등)이 전부 WS로 오므로 연결·구독이 되는지 본다
    private suspend fun checkStompConnects(roomId: Long) {
        val stream = KoinPlatform.getKoin().get<SessionEventStream>()
        val first = withTimeoutOrNull(10_000) { stream.events(roomId).first() }

        println("[SMOKE] stompConnect → $first")
        check(first is SessionEventStream.StreamEvent.Connected) {
            "STOMP 연결 실패 — 게스트 토큰 CONNECT 헤더·구독 인가를 확인"
        }
    }
}
