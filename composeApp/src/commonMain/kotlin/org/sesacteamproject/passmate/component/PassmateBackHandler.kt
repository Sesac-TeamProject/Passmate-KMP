package org.sesacteamproject.passmate.component

import androidx.compose.runtime.Composable

// 시스템 뒤로가기를 화면이 가로챈다. 세션 플로우는 단방향이라(규칙 §2-1-2) 풀이 중
// 뒤로가기가 대기실로 떨어지면 안 된다 — 화면이 자기 규칙대로 처리하게 넘긴다.
//
// 플랫폼별 사정:
// - Android: 시스템 뒤로가기 버튼·제스처 (androidx BackHandler)
// - Desktop: 시스템 뒤로가기가 없다 — 아무 일도 하지 않는다
// - iOS: RouteStackLevel이 navigationBarBackButtonHidden(true)라 시스템 뒤로가기 자체가 없다.
//   화면 안의 "나가기" 버튼만이 퇴장 경로이므로 미러할 대상이 없다
@Composable
expect fun PassmateBackHandler(enabled: Boolean = true, onBack: () -> Unit)
