package org.sesacteamproject.passmate.payment.domain.model

// 정산 계좌 은행 목록 (M-12-3 은행 드롭다운) — code는 금융결제원 표준 은행 코드로,
// PUT /users/me/settlement-account의 bankCode(필수)에 그대로 실린다.
enum class Bank(val code: String, val displayName: String) {

    KOOKMIN("004", "국민은행"),
    SHINHAN("088", "신한은행"),
    WOORI("020", "우리은행"),
    HANA("081", "하나은행"),
    NONGHYUP("011", "농협은행"),
    IBK("003", "기업은행"),
    KAKAO("090", "카카오뱅크"),
    TOSS("092", "토스뱅크"),
    KBANK("089", "케이뱅크"),
    SC("023", "SC제일은행"),
    CITI("027", "씨티은행"),
    SUHYUP("007", "수협은행"),
    POST("071", "우체국"),
    MG("045", "새마을금고"),
    SHINHYUP("048", "신협"),
    IM("031", "iM뱅크(대구)"),
    BUSAN("032", "부산은행"),
    GYEONGNAM("039", "경남은행"),
    GWANGJU("034", "광주은행"),
    JEONBUK("037", "전북은행"),
    JEJU("035", "제주은행"),
    KDB("002", "산업은행");

    companion object {

        // Swift는 Kotlin enum entries를 못 읽어 companion 프로퍼티로 노출한다 (RatingTag.companion.all과 같은 방식)
        val all: List<Bank> = entries

        fun fromCode(code: String?): Bank? {
            return entries.firstOrNull { it.code == code }
        }
    }
}
