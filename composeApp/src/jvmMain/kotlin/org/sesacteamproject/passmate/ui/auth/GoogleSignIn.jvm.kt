package org.sesacteamproject.passmate.ui.auth

import androidx.compose.runtime.Composable

// Desktop에는 구글 로그인 SDK가 없다 — 로컬 개발은 개발용 로그인(dev-login)으로 한다
@Composable
actual fun rememberGoogleSignInLauncher(
    onIdToken: (String) -> Unit,
    onCancel: () -> Unit,
    onFailure: () -> Unit
): () -> Unit {
    return onFailure
}
