package org.sesacteamproject.passmate.component

import androidx.compose.runtime.Composable

// 포트원 V2 브라우저 SDK 결제창을 웹뷰로 띄우는 공통 계약.
// 서버 /coins/charges 응답(CoinCheckout)을 그대로 담아 PortOne.requestPayment(...)에 전달한다.
data class PortOneRequest(
    val storeId: String,
    val channelKey: String,
    val paymentId: String,
    val orderName: String,
    val totalAmount: Int,
    val currency: String,
    val payMethod: String
)

sealed interface PortOneResult {

    // 결제 성공 — paymentId(imp_uid)를 서버 confirm에 전달한다
    data class Success(val paymentId: String) : PortOneResult

    data class Failure(val message: String) : PortOneResult

    data object Cancelled : PortOneResult
}

// PortOne SDK 로드를 기다리는 최대 시간. 초과하면 무한 대기 대신 실패로 끝낸다.
private const val SDK_LOAD_TIMEOUT_MS = 10_000

// 플랫폼별 웹뷰(Android WebView / Desktop 미지원 안내). onResult는 정확히 1회 호출된다.
@Composable
expect fun PortOnePaymentView(
    request: PortOneRequest,
    onResult: (PortOneResult) -> Unit
)

// PortOne SDK를 로드해 requestPayment를 호출하고 결과를 네이티브 브릿지(PassmateBridge)로 넘기는 HTML.
// 결과: 성공 → onSuccess(paymentId), 실패 → onFailure(message), 사용자 취소 → onCancel().
fun buildPortOneHtml(request: PortOneRequest): String {
    val storeId = request.storeId.jsEscape()
    val channelKey = request.channelKey.jsEscape()
    val paymentId = request.paymentId.jsEscape()
    val orderName = request.orderName.jsEscape()
    val currency = request.currency.jsEscape()
    val payMethod = request.payMethod.jsEscape()

    return """
<!doctype html>
<html lang="ko">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <script src="https://cdn.portone.io/v2/browser-sdk.js"></script>
  </head>
  <body style="margin:0;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;color:#5B6B62">
    <p>결제창을 여는 중이에요…</p>
    <script>
      function report(name, arg) {
        try {
          if (window.PassmateBridge && window.PassmateBridge[name]) {
            window.PassmateBridge[name](arg || "");
          } else if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.PassmateBridge) {
            window.webkit.messageHandlers.PassmateBridge.postMessage({ name: name, arg: arg || "" });
          }
        } catch (e) {}
      }
      async function start() {
        try {
          const waitSdk = new Promise((resolve, reject) => {
            const startedAt = Date.now();
            const t = setInterval(() => {
              if (window.PortOne != null) {
                clearInterval(t);
                resolve();
              } else if (Date.now() - startedAt >= $SDK_LOAD_TIMEOUT_MS) {
                clearInterval(t);
                reject(new Error("결제 모듈을 불러오지 못했어요. 잠시 후 다시 시도해 주세요"));
              }
            }, 50);
          });
          await waitSdk;
          const payment = await PortOne.requestPayment({
            storeId: "$storeId",
            channelKey: "$channelKey",
            paymentId: "$paymentId",
            orderName: "$orderName",
            totalAmount: ${request.totalAmount},
            currency: "$currency",
            payMethod: "$payMethod"
          });
          if (payment && payment.code !== undefined) {
            report("onFailure", payment.message || "결제에 실패했어요");
          } else {
            report("onSuccess", (payment && payment.paymentId) || "$paymentId");
          }
        } catch (e) {
          report("onFailure", (e && e.message) || "결제 중 오류가 발생했어요");
        }
      }
      start();
    </script>
  </body>
</html>
    """.trimIndent()
}

// 인라인 <script> 안의 JS 문자열 리터럴에 값을 끼워 넣는다.
// 따옴표뿐 아니라 꺾쇠도 막아야 한다 — 방 제목에 "</script>"가 들어오면 스크립트가 거기서 끊긴다.
private fun String.jsEscape(): String {
    val escaped = StringBuilder(length)

    for (char in this) {
        when (char) {
            '\\' -> escaped.append("\\\\")
            '"' -> escaped.append("\\\"")
            '\'' -> escaped.append("\\'")
            '\n' -> escaped.append("\\n")
            '\r' -> escaped.append("\\r")
            '<' -> escaped.append("\\u003C")
            '>' -> escaped.append("\\u003E")
            '&' -> escaped.append("\\u0026")
            '\u2028' -> escaped.append("\\u2028")
            '\u2029' -> escaped.append("\\u2029")
            else -> escaped.append(char)
        }
    }

    return escaped.toString()
}
