import Foundation

enum MyInfoAction {
    case enter
    case retry
    // 카드 단위 재시도 — 코인·정산은 프로필과 독립 로드다 (규칙 §9, 시안 M-12e)
    case retryCoinInfo
    case retryEarnings
    // 프로필 카드 탭 → 내 명성·뱃지 (M-09)
    case clickProfile
    // 닉네임·내 캐릭터 변경 시트 (M-12-1·M-12-7)
    case clickEditProfile
    // 코인 충전 (M-12-4~6) — 전용 화면은 후속 작업, 지금은 안내만
    case clickCharge
    case clickPaymentMethod
    case clickCoinHistory
    case clickSettlementAccount
    case clickEarnings
    case clickNotifications
    // card/기타 회원 탈퇴 행 → 전용 화면 push (M-12-12)
    case clickDeleteAccount
    // card/기타 약관 · 개인정보 처리방침 행 — 전용 화면이 아직 없어 안내만 한다
    case clickTerms
    // 확인 알림을 거친 뒤 호출된다 — 알림 소유는 화면 (규칙 §11-1)
    case confirmSignOut
    // 시트 저장 완료 — 해당 섹션만 다시 불러온다
    case profileUpdated
    case paymentMethodUpdated
    case accountUpdated
    case notice(message: String)
}
