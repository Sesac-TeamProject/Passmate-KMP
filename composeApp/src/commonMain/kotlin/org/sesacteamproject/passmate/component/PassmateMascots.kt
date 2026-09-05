package org.sesacteamproject.passmate.component

// 마스코트 '패시'의 상태 키 — 화면은 이 키만 알고 파일 위치는 플랫폼 로더(PassmateMascot)가 안다.
// 시안 컴포넌트 세트 Mascot(172:1037)의 State 변형 14개 중 앱 화면이 쓰는 5개만 옮겼다.
// resourceName = Android drawable 이름 = Desktop classpath 파일 이름(확장자 제외).
enum class PassmateMascots(val resourceName: String) {

    // 기본 (v6 C-01 로그인) — 프레임 밖으로 번지는 장식 없음. iOS 에셋 이름은 "MascotDefault"
    Default("img_mascot_default"),

    // 입장 (v6 M-01 홈·입장 · M-11 유료 방 결제) — 번지는 장식 없음. iOS 에셋 이름은 "MascotEnter"
    Enter("img_mascot_enter"),

    // 대기 (v6 M-02 대기실) — 생각 방울이 프레임 위 24 · 오른쪽 8만큼 번진다. iOS 에셋 이름은 "MascotWaiting"
    Waiting("img_mascot_waiting"),

    // 성공 (v6 M-05 최종 결과) — PASS 배지가 왼쪽 16 · 컨페티가 위 18만큼 번진다. iOS 에셋 이름은 "MascotSuccess"
    Success("img_mascot_success"),

    // 피드백 (v6 M-06 피드백·리포트) — 리포트 카드가 왼쪽 16만큼 번진다. iOS 에셋 이름은 "MascotFeedback"
    Feedback("img_mascot_feedback")
}
