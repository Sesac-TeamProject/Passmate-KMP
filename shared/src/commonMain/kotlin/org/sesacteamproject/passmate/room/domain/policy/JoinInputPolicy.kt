package org.sesacteamproject.passmate.room.domain.policy

// PIN 자릿수·닉네임 형식의 클라이언트 검증 (규칙 §5). 최종 판정(중복·마감)은 서버 응답을 따른다
class JoinInputPolicy {

    private val pinRegex = Regex("\\d{$PIN_LENGTH}")

    private val embeddedPinRegex = Regex("(?<!\\d)\\d{$PIN_LENGTH}(?!\\d)")

    fun isValidPin(pin: String): Boolean {
        return pinRegex.matches(pin)
    }

    fun isValidNickname(nickname: String): Boolean {
        val trimmed = nickname.trim()

        return trimmed.length in 1..NICKNAME_MAX_LENGTH
    }

    // QR 텍스트에서 PIN 추출 — pin 쿼리 파라미터 우선, 없으면 단독 6자리 숫자
    fun extractPin(text: String): String? {
        val fromQuery = Regex("[?&]pin=(\\d{$PIN_LENGTH})").find(text)?.groupValues?.get(1)

        return fromQuery ?: embeddedPinRegex.find(text)?.value
    }

    companion object {
        const val PIN_LENGTH = 6
        const val NICKNAME_MAX_LENGTH = 12
    }
}
