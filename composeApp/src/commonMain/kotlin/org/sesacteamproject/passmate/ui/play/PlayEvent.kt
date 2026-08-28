package org.sesacteamproject.passmate.ui.play

import org.sesacteamproject.passmate.session.domain.model.VoiceHint

sealed interface PlayEvent {

    // 힌트 재생은 단발 효과 — 컨테이너가 플랫폼 플레이어로 재생한다 (규칙 §7)
    data class PlayVoiceHint(val hint: VoiceHint) : PlayEvent

    data class RoomClosed(val message: String) : PlayEvent

    data object Left : PlayEvent

    data class ShowNotice(val message: String) : PlayEvent
}
