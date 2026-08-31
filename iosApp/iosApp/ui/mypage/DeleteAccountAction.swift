import Foundation

enum DeleteAccountAction {
    case enter
    // "위 내용을 확인했어요" 체크 토글 — 체크해야만 탈퇴할 수 있다
    case toggleConfirm
    case clickDelete
}
