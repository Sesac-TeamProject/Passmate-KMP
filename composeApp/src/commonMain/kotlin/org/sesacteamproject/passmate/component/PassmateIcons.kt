package org.sesacteamproject.passmate.component

// 아이콘 리소스 키 — 화면은 이 키만 알고 파일 위치는 플랫폼 로더(PassmateIcon)가 안다.
// resourceName = Android drawable 이름 = Desktop classpath 파일 이름(확장자 제외).
// 새 아이콘 추가 절차는 specs/001-joined-rooms-empty-resources/contracts/icon-resource.md §5 참조
enum class PassmateIcons(val resourceName: String) {

    // 문이 열린 아이콘 (v6 M-08 참여한 방 빈 상태) — iOS 에셋 이름은 "DoorOpen"
    DoorOpen("ic_door_open"),

    // 왼쪽 화살표 (상세 화면 뒤로가기 헤더) — iOS 에셋 이름은 "ArrowLeft"
    ArrowLeft("ic_arrow_left"),

    // 코인 마크 (v6 M-12-9 보유 코인 카드) — iOS 에셋 이름은 "Coin"
    Coin("ic_coin"),

    // 목록 아이콘 (v6 M-12-9 코인 내역 빈 상태) — iOS 에셋 이름은 "List"
    List("ic_list"),

    // 북마크 아이콘 (v6 M-T4 정산 빈 상태) — iOS 에셋 이름은 "Bookmark"
    Bookmark("ic_bookmark"),

    // 경고 원 (목록 불러오기 실패 E-List 공통 패턴 · v6 M-12e 마이 부분 실패) — iOS 에셋 이름은 "AlertCircle"
    AlertCircle("ic_alert_circle"),

    // 집 (v6 nav/4탭 홈) — iOS 에셋 이름은 "Home"
    Home("ic_home"),

    // 네모 안 더하기 (v6 nav/4탭 내가 만든 방) — iOS 에셋 이름은 "PlusSquare"
    PlusSquare("ic_plus_square"),

    // 사람 (v6 nav/4탭 마이) — iOS 에셋 이름은 "User"
    User("ic_user")
}
