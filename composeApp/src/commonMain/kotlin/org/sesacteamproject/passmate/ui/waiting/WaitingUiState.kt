package org.sesacteamproject.passmate.ui.waiting

import org.sesacteamproject.passmate.room.domain.model.Participant

data class WaitingUiState(
    val isLoading: Boolean = true,
    val roomTitle: String = "",
    val pin: String = "",
    val myParticipantId: Long? = null,
    val myNickname: String? = null,
    val participants: List<Participant> = emptyList(),
    val totalCount: Int = 0,
    // 참가자 목록은 방 정보와 따로 로드된다 — "아직 못 불러옴"과 "정말 0명"을 구분해야
    // 조회 실패가 "학생 0명이 함께해요"로 둔갑하지 않는다
    val isParticipantsLoading: Boolean = true,
    val hasParticipantsError: Boolean = false,
    // 세션 종료는 상태로도 남긴다 — 대기실이 백스택에 있는 동안 발행한 event는 아무도 받지 못한다 (규칙 §7 replay=0)
    val isSessionFinished: Boolean = false
)
