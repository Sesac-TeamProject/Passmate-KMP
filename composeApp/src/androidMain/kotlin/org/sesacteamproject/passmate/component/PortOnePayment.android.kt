package org.sesacteamproject.passmate.component

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

// 포트원 결제창을 WebView로 띄운다. 결과는 PassmateBridge(JS 인터페이스)로 받아 onResult로 1회 전달한다.
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun PortOnePaymentView(
    request: PortOneRequest,
    onResult: (PortOneResult) -> Unit
) {
    val currentOnResult = rememberUpdatedState(onResult)

    AndroidView(
        modifier = Modifier,
        factory = { context ->
            val delivered = booleanArrayOf(false)

            fun deliver(result: PortOneResult) {
                if (!delivered[0]) {
                    delivered[0] = true
                    currentOnResult.value(result)
                }
            }

            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onSuccess(paymentId: String) {
                            post { deliver(PortOneResult.Success(paymentId)) }
                        }

                        @JavascriptInterface
                        fun onFailure(message: String) {
                            post { deliver(PortOneResult.Failure(message.ifBlank { "결제에 실패했어요" })) }
                        }

                        @JavascriptInterface
                        fun onCancel() {
                            post { deliver(PortOneResult.Cancelled) }
                        }
                    },
                    "PassmateBridge"
                )
                webViewClient = object : WebViewClient() {
                    // 간편결제(카카오·네이버 등)는 앱 스킴/intent로 열린다 — 외부 앱으로 위임한다
                    override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
                        val url = req.url.toString()

                        return if (url.startsWith("http://") || url.startsWith("https://")) {
                            false
                        } else {
                            launchExternal(view, url)
                            true
                        }
                    }
                }
                loadDataWithBaseURL(
                    "https://passmate.app",
                    buildPortOneHtml(request),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

private fun launchExternal(view: WebView, url: String) {
    try {
        val intent = if (url.startsWith("intent://")) {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }

        view.context.startActivity(intent)
    } catch (e: Exception) {
        // 미설치 앱 등은 무시 — 사용자가 결제창에서 다른 수단을 고를 수 있다
    }
}
