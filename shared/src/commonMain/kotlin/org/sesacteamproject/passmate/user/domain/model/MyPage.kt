package org.sesacteamproject.passmate.user.domain.model

// 마이페이지(학습 기록) 화면 데이터 — GET /users/me/rooms/joined 한 번으로 요약+진행중+목록 구성
data class MyPage(
    val summary: MyPageSummary,
    val ongoing: OngoingRoom?,
    val rooms: List<JoinedRoom>,
    val nextCursor: String?
)
