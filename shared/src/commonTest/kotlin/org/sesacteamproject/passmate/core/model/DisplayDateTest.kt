package org.sesacteamproject.passmate.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// 시안(design.pen)은 전 화면에서 연도 없는 M/D로 쓴다:
// M-08 "8/22 (금) · 8문항" · M-13 "8/19 · 학생 9명" · M-12 "9/5 지급" · "최근 8/22"
class DisplayDateTest {

    @Test
    fun formatsAsMonthAndDayWithoutYear() {
        assertEquals("8/22", DisplayDate.format("2026-08-22T21:10:00"))
        assertEquals("8/19", DisplayDate.format("2026-08-19"))
        // 한 자리 날짜는 0을 붙이지 않는다 (시안 "9/5 지급")
        assertEquals("9/5", DisplayDate.format("2026-09-05"))
        assertEquals("1/1", DisplayDate.format("2026-01-01"))
    }

    @Test
    fun returnsNullForMalformedInput() {
        assertNull(DisplayDate.format(null))
        assertNull(DisplayDate.format(""))
        assertNull(DisplayDate.format("2026-08"))
        assertNull(DisplayDate.format("어제"))
    }

    // M-08 참여한 방 행은 요일까지 보여준다.
    // 시안의 "8/22 (금)"은 다른 해 기준 더미다 — 2026-08-22는 토요일이라 실제 요일로 검증한다
    @Test
    fun formatsWithKoreanWeekday() {
        assertEquals("8/22 (토)", DisplayDate.formatWithWeekday("2026-08-22T21:10:00"))
        assertEquals("8/20 (목)", DisplayDate.formatWithWeekday("2026-08-20"))
        assertEquals("8/17 (월)", DisplayDate.formatWithWeekday("2026-08-17"))
    }

    // 윤년·세기 경계에서도 요일이 맞아야 한다
    @Test
    fun computesWeekdayAcrossLeapYearsAndCenturies() {
        assertEquals("2/29 (토)", DisplayDate.formatWithWeekday("2020-02-29"))
        assertEquals("1/1 (월)", DisplayDate.formatWithWeekday("2024-01-01"))
        assertEquals("3/1 (수)", DisplayDate.formatWithWeekday("2000-03-01"))
    }

    // M-14 방 리포트는 "8/22(금) 진행"처럼 요일 괄호 앞에 공백이 없다 (M-08과 다르다)
    @Test
    fun formatsWithWeekdayWithoutSpaceWhenRequested() {
        assertEquals("8/22(토)", DisplayDate.formatWithWeekday("2026-08-22T21:10:00", isSpaced = false))
        assertEquals("9/4(금)", DisplayDate.formatWithWeekday("2026-09-04T07:50:25.554235", isSpaced = false))
    }

    @Test
    fun weekdayFormatAlsoRejectsMalformedInput() {
        assertNull(DisplayDate.formatWithWeekday(null))
        assertNull(DisplayDate.formatWithWeekday("2026-13-40x"))
    }
}
