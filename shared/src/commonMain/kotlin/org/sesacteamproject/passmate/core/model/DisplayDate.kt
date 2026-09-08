package org.sesacteamproject.passmate.core.model

// 서버 날짜/시각 문자열의 날짜 부분을 화면 표기로 바꾼다.
// 시안(design.pen)은 전 화면에서 연도 없는 M/D를 쓴다 — M-08 "8/22 (금)" · M-13 "8/19" · M-12 "9/5 지급".
// LocalDate("2026-09-05")·LocalDateTime("2026-08-22T21:10:00") 둘 다 받는다.
// 시간대 변환이 필요 없는 표시 전용이라 문자열 처리로 충분하다.
object DisplayDate {

    private val weekdayNames = listOf("일", "월", "화", "수", "목", "금", "토")

    private fun partsOf(raw: String?): Triple<Int, Int, Int>? {
        val date = raw?.substringBefore("T")

        return if (date == null || date.length != 10) {
            null
        } else {
            val year = date.substring(0, 4).toIntOrNull()
            val month = date.substring(5, 7).toIntOrNull()
            val day = date.substring(8, 10).toIntOrNull()

            if (year == null || month == null || day == null) {
                null
            } else {
                Triple(year, month, day)
            }
        }
    }

    // Zeller의 공식 — 0=토요일이라 일요일 기준으로 옮겨 담는다. 그레고리력 전용
    private fun weekdayIndexOf(year: Int, month: Int, day: Int): Int {
        val shiftedMonth = if (month <= 2) month + 12 else month
        val shiftedYear = if (month <= 2) year - 1 else year
        val century = shiftedYear / 100
        val yearInCentury = shiftedYear % 100
        val zeller = (day + 13 * (shiftedMonth + 1) / 5 + yearInCentury +
            yearInCentury / 4 + century / 4 + 5 * century) % 7

        return (zeller + 6) % 7
    }

    fun format(raw: String?): String? {
        val parts = partsOf(raw)

        return if (parts == null) {
            null
        } else {
            "${parts.second}/${parts.third}"
        }
    }

    // isSpaced=true → "8/22 (금)"(M-08 참여한 방) · false → "8/22(금)"(M-14 방 리포트). 시안이 화면마다 다르다
    fun formatWithWeekday(raw: String?, isSpaced: Boolean = true): String? {
        val parts = partsOf(raw)

        return if (parts == null) {
            null
        } else {
            val weekday = weekdayNames[weekdayIndexOf(parts.first, parts.second, parts.third)]
            val separator = if (isSpaced) " " else ""

            "${parts.second}/${parts.third}$separator($weekday)"
        }
    }
}
