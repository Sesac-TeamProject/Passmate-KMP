package org.sesacteamproject.passmate.component

// 아이콘 리소스 키 — 화면은 이 키만 알고 파일 위치는 플랫폼 로더(PassmateIcon)가 안다.
// resourceName = Android drawable 이름 = Desktop classpath 파일 이름(확장자 제외).
// 새 아이콘 추가 절차는 specs/001-joined-rooms-empty-resources/contracts/icon-resource.md §5 참조
enum class PassmateIcons(val resourceName: String) {

    // 문이 열린 아이콘 (v6 M-08 참여한 방 빈 상태) — iOS 에셋 이름은 "DoorOpen"
    DoorOpen("ic_door_open"),

    // 북마크 아이콘 (v6 M-T4 정산 빈 상태) — iOS 에셋 이름은 "Bookmark"
    Bookmark("ic_bookmark"),

    // 경고 원형 아이콘 (v6 M-T4 계좌 미등록·목록 불러오기 실패) — iOS 에셋 이름은 "AlertCircle"
    AlertCircle("ic_alert_circle")
}
