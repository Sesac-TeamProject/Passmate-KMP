import SwiftUI
import WebKit

// 포트원 V2 결제 요청 파라미터 (Compose PortOneRequest 미러) — 서버 /coins/charges 응답을 담는다
struct PortOneRequest: Equatable {
    let storeId: String
    let channelKey: String
    let paymentId: String
    let orderName: String
    let totalAmount: Int
    let currency: String
    let payMethod: String
}

enum PortOneResult {
    case success(paymentId: String)
    case failure(message: String)
    case cancelled
}

// 포트원 결제창을 WKWebView로 띄운다. 결과는 PassmateBridge(WKScriptMessageHandler)로 받아 onResult로 1회 전달.
struct PortOnePaymentView: UIViewRepresentable {
    let request: PortOneRequest

    let onResult: (PortOneResult) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onResult: onResult)
    }

    func makeUIView(context: Context) -> WKWebView {
        let contentController = WKUserContentController()
        contentController.add(context.coordinator, name: "PassmateBridge")

        let config = WKWebViewConfiguration()
        config.userContentController = contentController
        config.defaultWebpagePreferences.allowsContentJavaScript = true

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.navigationDelegate = context.coordinator
        webView.loadHTMLString(Self.buildHtml(request), baseURL: URL(string: "https://passmate.app"))

        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    final class Coordinator: NSObject, WKScriptMessageHandler, WKNavigationDelegate {
        private let onResult: (PortOneResult) -> Void

        private var delivered = false

        init(onResult: @escaping (PortOneResult) -> Void) {
            self.onResult = onResult
        }

        private func deliver(_ result: PortOneResult) {
            if !delivered {
                delivered = true
                onResult(result)
            }
        }

        func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
            guard let body = message.body as? [String: Any] else { return }
            let name = body["name"] as? String ?? ""
            let arg = body["arg"] as? String ?? ""

            switch name {
            case "onSuccess":
                deliver(.success(paymentId: arg))
            case "onFailure":
                deliver(.failure(message: arg.isEmpty ? "결제에 실패했어요" : arg))
            case "onCancel":
                deliver(.cancelled)
            default:
                break
            }
        }

        // 간편결제(카카오·네이버 등)는 앱 스킴으로 열린다 — 외부 앱으로 위임한다
        func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            guard let url = navigationAction.request.url else {
                decisionHandler(.allow)
                return
            }
            let scheme = url.scheme ?? ""

            if scheme == "http" || scheme == "https" || scheme == "about" {
                decisionHandler(.allow)
            } else {
                UIApplication.shared.open(url, options: [:]) { _ in }
                decisionHandler(.cancel)
            }
        }
    }

    // Compose buildPortOneHtml과 동일 — PortOne SDK 로드 후 requestPayment 호출, 결과를 브릿지로 전달
    static func buildHtml(_ request: PortOneRequest) -> String {
        let storeId = jsEscape(request.storeId)
        let channelKey = jsEscape(request.channelKey)
        let paymentId = jsEscape(request.paymentId)
        let orderName = jsEscape(request.orderName)
        let currency = jsEscape(request.currency)
        let payMethod = jsEscape(request.payMethod)

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
                  const waitSdk = new Promise((resolve) => {
                    const t = setInterval(() => {
                      if (window.PortOne != null) { clearInterval(t); resolve(); }
                    }, 50);
                  });
                  await waitSdk;
                  const payment = await PortOne.requestPayment({
                    storeId: "\(storeId)",
                    channelKey: "\(channelKey)",
                    paymentId: "\(paymentId)",
                    orderName: "\(orderName)",
                    totalAmount: \(request.totalAmount),
                    currency: "\(currency)",
                    payMethod: "\(payMethod)"
                  });
                  if (payment && payment.code !== undefined) {
                    report("onFailure", payment.message || "결제에 실패했어요");
                  } else {
                    report("onSuccess", (payment && payment.paymentId) || "\(paymentId)");
                  }
                } catch (e) {
                  report("onFailure", (e && e.message) || "결제 중 오류가 발생했어요");
                }
              }
              start();
            </script>
          </body>
        </html>
        """
    }

    private static func jsEscape(_ value: String) -> String {
        value
            .replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "\"", with: "\\\"")
            .replacingOccurrences(of: "\n", with: " ")
    }
}
