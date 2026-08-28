package org.sesacteamproject.passmate.core.model

// 서버 ISO-8601 시각(ts·endsAt) 파서 — 남은 시간은 서버 시각 차로만 계산한다 (규칙 §5).
// kotlinx-datetime 미도입(Kotlin 1.9.20 klib 호환 리스크 회피) — UTC·±hh:mm 오프셋만 지원
object IsoTime {

    private val isoRegex = Regex(
        "(\\d{4})-(\\d{2})-(\\d{2})[Tt](\\d{2}):(\\d{2}):(\\d{2})(?:\\.(\\d{1,9}))?(Z|z|[+-]\\d{2}:?\\d{2})?"
    )

    private fun daysFromCivil(year: Long, month: Long, day: Long): Long {
        val adjustedYear = if (month <= 2) year - 1 else year
        val era = (if (adjustedYear >= 0) adjustedYear else adjustedYear - 399) / 400
        val yearOfEra = adjustedYear - era * 400
        val dayOfYear = (153 * (month + (if (month > 2) -3 else 9)) + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear

        return era * 146097 + dayOfEra - 719468
    }

    private fun offsetMillis(raw: String?): Long? {
        return if (raw == null || raw == "Z" || raw == "z") {
            0L
        } else {
            val sign = if (raw[0] == '-') -1L else 1L
            val digits = raw.drop(1).replace(":", "")
            val hours = digits.take(2).toLongOrNull()
            val minutes = digits.drop(2).toLongOrNull()

            if (hours == null || minutes == null) {
                null
            } else {
                sign * (hours * 3_600_000L + minutes * 60_000L)
            }
        }
    }

    fun toEpochMillis(iso: String): Long? {
        val match = isoRegex.find(iso.trim()) ?: return null
        val (year, month, day, hour, minute, second, fraction, offsetRaw) = match.destructured
        val offset = offsetMillis(offsetRaw.ifEmpty { null }) ?: return null
        val days = daysFromCivil(year.toLong(), month.toLong(), day.toLong())
        val millisOfDay = hour.toLong() * 3_600_000L + minute.toLong() * 60_000L + second.toLong() * 1_000L
        val fractionMillis = fraction.padEnd(3, '0').take(3).toLongOrNull() ?: 0L

        return days * 86_400_000L + millisOfDay + fractionMillis - offset
    }
}
