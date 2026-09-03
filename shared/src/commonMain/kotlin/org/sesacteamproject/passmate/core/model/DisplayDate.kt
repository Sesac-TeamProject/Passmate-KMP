package org.sesacteamproject.passmate.core.model

// 서버 날짜/시각 문자열의 날짜 부분을 화면 표기(YYYY.MM.DD)로 바꾼다.
// LocalDate("2026-09-05")·LocalDateTime("2026-08-22T21:10:00") 둘 다 받는다.
// 시간대 변환이 필요 없는 표시 전용이라 문자열 처리로 충분하다.
object DisplayDate {

    fun format(raw: String?): String? {
        val date = raw?.substringBefore("T")

        return if (date != null && date.length == 10) {
            date.replace("-", ".")
        } else {
            null
        }
    }
}
