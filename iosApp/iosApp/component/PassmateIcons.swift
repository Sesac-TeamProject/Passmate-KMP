import Foundation

// 아이콘 리소스 키 — Compose component/PassmateIcons.kt와 1:1 미러다.
// rawValue = Assets.xcassets의 imageset 이름(PascalCase).
// 새 아이콘 추가 절차는 specs/001-joined-rooms-empty-resources/contracts/icon-resource.md §5 참조
enum PassmateIcons: String {

    // 문이 열린 아이콘 (v6 M-08 참여한 방 빈 상태)
    case doorOpen = "DoorOpen"

    // 왼쪽 화살표 (상세 화면 뒤로가기 헤더)
    case arrowLeft = "ArrowLeft"

    // 코인 마크 (v6 M-12-9 보유 코인 카드)
    case coin = "Coin"

    // 목록 아이콘 (v6 M-12-9 코인 내역 빈 상태)
    case list = "List"

    // 경고 원 (목록 불러오기 실패 E-List 공통 패턴)
    case alertCircle = "AlertCircle"
}
