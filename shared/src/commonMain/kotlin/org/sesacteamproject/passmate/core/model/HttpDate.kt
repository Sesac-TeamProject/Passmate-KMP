package org.sesacteamproject.passmate.core.model

// HTTP `Date` 응답 헤더(RFC 1123) → ISO-8601 변환.
// 세션 스냅샷 본문에 서버 시각(ts)이 없어서, 응답 헤더의 서버 시각을 대신 쓴다.
// 남은 시간 계산(§5)과 스냅샷 이전 이벤트 폐기(§2-1-2)가 이 값에 의존하므로
// 로컬 시계가 아니라 반드시 서버가 준 시각이어야 한다.
object HttpDate {

    private val rfc1123Regex = Regex(
        "[A-Za-z]{3},\\s+(\\d{2})\\s+([A-Za-z]{3})\\s+(\\d{4})\\s+(\\d{2}):(\\d{2}):(\\d{2})\\s+GMT"
    )

    private val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    fun toIsoOrNull(raw: String?): String? {
        val match = raw?.let { rfc1123Regex.find(it.trim()) } ?: return null
        val (day, monthName, year, hour, minute, second) = match.destructured
        val monthIndex = months.indexOfFirst { it.equals(monthName, ignoreCase = true) }

        return if (monthIndex < 0) {
            null
        } else {
            val month = (monthIndex + 1).toString().padStart(2, '0')
            "$year-$month-${day}T$hour:$minute:${second}Z"
        }
    }
}
