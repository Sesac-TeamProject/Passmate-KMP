import Shared

struct WaitingUiState {
    var isLoading: Bool = true

    var roomTitle: String = ""

    var pin: String = ""

    var myParticipantId: Int64?

    var myNickname: String?

    var participants: [Participant] = []

    var totalCount: Int = 0

    // 참가자 목록은 방 정보와 따로 로드된다 — "아직 못 불러옴"과 "정말 0명"을 구분해야
    // 조회 실패가 "학생 0명이 함께해요"로 둔갑하지 않는다
    var isParticipantsLoading: Bool = true

    var hasParticipantsError: Bool = false

    // 세션 종료는 상태로도 남긴다 — 대기실이 스택에 있는 동안 보낸 event는 아무도 받지 못한다 (규칙 §7)
    var isSessionFinished: Bool = false

    // STOMP가 끊긴 동안 true — 컨테이너가 M-07 연결 끊김 오버레이를 띄운다
    var isDisconnected: Bool = false
}
