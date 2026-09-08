package org.sesacteamproject.passmate.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.core.config.GoogleAuthConfig

private fun Context.findActivity(): Activity? {
    var current: Context? = this

    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}

private fun googleIdTokenOf(credential: Credential): String? {
    val isGoogleIdToken = credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL

    return if (isGoogleIdToken) {
        GoogleIdTokenCredential.createFrom(credential.data).idToken
    } else {
        null
    }
}

// Credential Manager는 시트를 띄울 Activity 컨텍스트를 요구한다 — application 컨텍스트로는 뜨지 않는다
@Composable
actual fun rememberGoogleSignInLauncher(
    onIdToken: (String) -> Unit,
    onCancel: () -> Unit,
    onFailure: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    // serverClientId는 웹 클라이언트 ID다 — 그래야 ID 토큰의 aud가 서버가 검증하는 값과 맞는다
    val request = remember {
        GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(GoogleAuthConfig.WEB_CLIENT_ID).build())
            .build()
    }

    return {
        val activity = context.findActivity()

        if (activity == null) {
            onFailure()
        } else {
            scope.launch {
                try {
                    val credential = credentialManager.getCredential(activity, request).credential
                    val idToken = googleIdTokenOf(credential)

                    if (idToken == null) {
                        onFailure()
                    } else {
                        onIdToken(idToken)
                    }
                } catch (e: GetCredentialCancellationException) {
                    // 사용자가 시트를 닫았다 — 실패가 아니다
                    onCancel()
                } catch (e: Exception) {
                    // 콘솔에 Android 클라이언트(패키지+SHA-1)가 없으면 여기로 떨어진다 — 원인을 로그로 남긴다
                    println("Passmate/GoogleSignIn: ${e::class.simpleName} ${e.message}")
                    onFailure()
                }
            }
        }
    }
}
