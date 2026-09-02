package org.sesacteamproject.passmate.component

// 아이콘 리소스 키 — 화면은 이 키만 알고 파일 위치는 플랫폼 로더(PassmateIcon)가 안다.
// resourceName = Android drawable 이름 = Desktop classpath 파일 이름(확장자 제외).
// 새 아이콘 추가 절차는 specs/001-joined-rooms-empty-resources/contracts/icon-resource.md §5 참조
enum class PassmateIcons(val resourceName: String) {

    // 문이 열린 아이콘 (v6 M-08 참여한 방 빈 상태) — iOS 에셋 이름은 "DoorOpen"
    DoorOpen("ic_door_open"),

    // 경고 원 아이콘 (v6 E-List 목록 불러오기 실패) — iOS 에셋 이름은 "AlertCircle"
    AlertCircle("ic_alert_circle")
}
