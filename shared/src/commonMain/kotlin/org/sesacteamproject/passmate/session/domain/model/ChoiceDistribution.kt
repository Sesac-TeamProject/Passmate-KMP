package org.sesacteamproject.passmate.session.domain.model

// M-04 응답 분포 한 줄 — 보기 하나에 몇 명이 답했는지
data class ChoiceDistribution(
    val choiceNo: Int,
    val label: String,
    val count: Int,
    val isAnswer: Boolean,
    val isMine: Boolean
)
