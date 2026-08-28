package org.sesacteamproject.passmate.ui.result

import androidx.compose.runtime.Composable

// 리포트 내보내기(저장/공유) — Android=ACTION_SEND, Desktop=클립보드 복사. 텍스트 요약을 공유한다
@Composable
expect fun rememberReportSharer(): (String) -> Unit
