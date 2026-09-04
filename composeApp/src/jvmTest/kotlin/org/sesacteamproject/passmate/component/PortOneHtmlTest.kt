package org.sesacteamproject.passmate.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// 결제 WebView HTML — 방 제목 같은 서버 문자열이 인라인 <script>를 끊지 못해야 한다
class PortOneHtmlTest {

    private fun requestWith(orderName: String): PortOneRequest {
        return PortOneRequest(
            storeId = "store-1",
            channelKey = "channel-1",
            paymentId = "payment-1",
            orderName = orderName,
            totalAmount = 5000,
            currency = "KRW",
            payMethod = "CARD"
        )
    }

    @Test
    fun keepsScriptTagCountFixedWhenOrderNameCarriesClosingTag() {
        val attack = "수학 특강</script><script>alert(1)</script>"
        val html = buildPortOneHtml(requestWith(attack))
        val closingTags = html.split("</script>").size - 1

        // SDK 로드 태그 1개 + 인라인 스크립트 1개 = 2개. 제목이 태그를 늘리면 스크립트가 끊긴 것이다.
        assertEquals(2, closingTags, "방 제목이 <script> 태그를 늘렸다")
        assertFalse(html.contains(attack), "제목이 이스케이프 없이 그대로 들어갔다")
        assertTrue(html.contains("\\u003C"), "꺾쇠가 유니코드 이스케이프로 바뀌지 않았다")
    }

    @Test
    fun keepsStringLiteralIntactWhenOrderNameCarriesQuoteAndBackslash() {
        val html = buildPortOneHtml(requestWith("""오늘의 "특강" \ 세트"""))

        assertTrue(html.contains("""orderName: "오늘의 \"특강\" \\ 세트""""), "따옴표·역슬래시 이스케이프가 어긋난다")
    }

    @Test
    fun keepsPlainKoreanTitleReadable() {
        val html = buildPortOneHtml(requestWith("중등 수학 특강"))

        assertTrue(html.contains("""orderName: "중등 수학 특강""""), "평범한 제목까지 훼손하면 안 된다")
    }

    @Test
    fun stopsWaitingWhenSdkNeverLoads() {
        val html = buildPortOneHtml(requestWith("중등 수학 특강"))

        // SDK가 끝내 안 뜨면 "결제창을 여는 중"에서 멈추지 않고 실패로 끝나야 한다
        assertTrue(html.contains("reject(new Error("), "SDK 로드 타임아웃이 없다")
        assertTrue(html.contains("Date.now() - startedAt >= 10000"), "타임아웃 상수가 스크립트에 반영되지 않았다")
    }
}
