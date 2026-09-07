package org.sesacteamproject.passmate.ui.auth

import androidx.compose.runtime.Composable

/**
 * 플랫폼 구글 로그인 시트를 여는 런처를 만든다. 반환된 람다를 호출하면 시트가 뜨고,
 * 결과는 세 콜백 중 하나로 돌아온다 — 판단(문구·상태)은 전부 ViewModel이 한다(규칙 §7).
 *
 * Android=Credential Manager, Desktop=미지원(바로 [onFailure]).
 */
@Composable
expect fun rememberGoogleSignInLauncher(
    onIdToken: (String) -> Unit,
    onCancel: () -> Unit,
    onFailure: () -> Unit
): () -> Unit
