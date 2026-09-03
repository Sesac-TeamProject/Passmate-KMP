package org.sesacteamproject.passmate.core.model

// 서버 시각 표기를 하나로 맞춘다.
//
// 서버는 응답 **본문**의 시각을 `LocalDateTime`으로 직렬화해 오프셋 없이 준다
// ("2026-09-03T12:40:50.728" — 서버 지역 시각). 반면 HTTP `Date` **헤더**는
// 규격상 GMT다. 세션 스냅샷에는 본문 시각이 없어 헤더를 서버 시각으로 쓰는데
// (SessionRemoteDataSource), 그대로 비교하면 남은 시간이 9시간으로 어긋난다.
//
// 그래서 헤더를 서버 지역 시각으로 옮겨 본문 시각과 같은 시계에 놓는다.
// 본문끼리(이벤트 occurredAt ↔ endsAt)는 이미 같은 시계라 변환하지 않는다.
//
// ⚠️ 서버가 오프셋을 실어주면(예: OffsetDateTime) 이 보정은 필요 없어진다 —
// 계약 갱신 후보다. 그때까지는 서비스 운영 지역(KST)을 기준으로 맞춘다.
object ServerClock {

    // 2026-09-03 로컬 백엔드 실측: Date 헤더와 본문 createdAt이 정확히 9시간 차였다
    private const val SERVER_OFFSET_MILLIS = 9L * 3_600_000L

    private fun pad(value: Long, length: Int): String {
        return value.toString().padStart(length, '0')
    }

    private fun civilFromDays(days: Long): Triple<Long, Long, Long> {
        val z = days + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val dayOfEra = z - era * 146097
        val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
        val year = yearOfEra + era * 400
        val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
        val monthPrime = (5 * dayOfYear + 2) / 153
        val day = dayOfYear - (153 * monthPrime + 2) / 5 + 1
        val month = if (monthPrime < 10) monthPrime + 3 else monthPrime - 9

        return Triple(if (month <= 2) year + 1 else year, month, day)
    }

    private fun formatLocalIso(epochMillis: Long): String {
        val days = if (epochMillis >= 0) epochMillis / 86_400_000L else (epochMillis - 86_399_999L) / 86_400_000L
        val millisOfDay = epochMillis - days * 86_400_000L
        val (year, month, day) = civilFromDays(days)
        val hour = millisOfDay / 3_600_000L
        val minute = (millisOfDay % 3_600_000L) / 60_000L
        val second = (millisOfDay % 60_000L) / 1_000L

        return "${pad(year, 4)}-${pad(month, 2)}-${pad(day, 2)}T${pad(hour, 2)}:${pad(minute, 2)}:${pad(second, 2)}"
    }

    // HTTP Date 헤더(GMT) → 서버 지역 시각(오프셋 없는 ISO). 본문 시각과 바로 비교된다.
    fun toServerLocalIso(httpDate: String?): String? {
        val iso = HttpDate.toIsoOrNull(httpDate) ?: return null
        val epochMillis = IsoTime.toEpochMillis(iso) ?: return null

        return formatLocalIso(epochMillis + SERVER_OFFSET_MILLIS)
    }
}
