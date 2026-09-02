package org.sesacteamproject.passmate.preview

// ContentScreen 프리뷰에 다는 애노테이션. 렌더링 주체는 Android Studio다.
// CMP 1.5.12에는 commonMain용 @Preview가 없어 같은 FQN을 직접 선언해 쓴다 —
// 사유는 org/jetbrains/compose/ui/tooling/preview/Preview.kt 주석 참조.
//
// 이름 인자(@Preview("이름"))는 받지 않는다. 프리뷰 구분은 함수명으로 한다.
typealias PassmatePreview = org.jetbrains.compose.ui.tooling.preview.Preview
