package org.sesacteamproject.passmate.payment.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.payment.data.dto.EarningsResponse
import org.sesacteamproject.passmate.payment.data.dto.SettlementAccountResponse
import org.sesacteamproject.passmate.payment.domain.model.SettlementStatus

// GET /users/me/earnings — 백엔드 실제 응답(2026-09-03 로컬 확인) 기준.
// earnings는 페이징 없이 전량이 오므로 방 수·학생 수를 여기서 집계해도 정확하다.
// 계좌 은행 정보는 이 응답에 없고 GET /users/me/settlement-account에서 온다.
class EarningsMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun mapsServerFieldsAndAggregatesRoomAndStudentCount() {
        val raw = """
            {
              "thisMonthNet": 128000,
              "pendingNet": 42000,
              "nextPayoutDate": "2026-09-05",
              "accountRegistered": true,
              "earnings": [
                {
                  "roomId": 11,
                  "roomTitle": "8월 4주차 Spring 스터디",
                  "participantCount": 12,
                  "gross": 60000,
                  "platformFee": 12000,
                  "net": 48000,
                  "status": "PAID",
                  "earnedAt": "2026-08-22T21:10:00"
                },
                {
                  "roomId": 12,
                  "roomTitle": "확률과 통계 총정리",
                  "participantCount": 8,
                  "gross": 40000,
                  "platformFee": 8000,
                  "net": 32000,
                  "status": "SCHEDULED",
                  "earnedAt": "2026-08-29T20:30:00"
                }
              ]
            }
        """.trimIndent()

        val earnings = json.decodeFromString<EarningsResponse>(raw).toDomain(account = null)
        val first = earnings.items.first()

        assertEquals(128000L, earnings.monthlyTotal)
        // 다음 지급일 + 지급 예정 금액(pendingNet)
        assertEquals("2026-09-05", earnings.nextPayout?.dateLabel)
        assertEquals(42000L, earnings.nextPayout?.amount)
        // earnings가 전량이라 집계가 정확하다
        assertEquals(2, earnings.paidRoomCount)
        assertEquals(20, earnings.studentCount)
        // 서버가 페이징하지 않는다
        assertNull(earnings.nextCursor)
        assertEquals(false, earnings.hasNext)

        assertEquals(2, earnings.items.size)
        assertEquals(11L, first.settlementId)
        assertEquals("2026.08.22", first.dateLabel)
        assertEquals("8월 4주차 Spring 스터디", first.roomTitle)
        assertEquals(12, first.participantCount)
        assertEquals(60000L, first.entryFeeTotal)
        assertEquals(12000L, first.feeAmount)
        assertEquals(48000L, first.payoutAmount)
        assertEquals(SettlementStatus.PAID, first.status)
        assertEquals(SettlementStatus.SCHEDULED, earnings.items.last().status)
    }

    @Test
    fun parsesSettlementAccountAndBuildsSummary() {
        val raw = """
            {
              "registered": true,
              "account": {
                "bankCode": "004",
                "bankName": "국민은행",
                "accountNoMasked": "1234-**-5678",
                "holderName": "홍희표",
                "verified": true
              }
            }
        """.trimIndent()

        val summary = json.decodeFromString<SettlementAccountResponse>(raw).toSummary()

        assertEquals("국민은행", summary?.bankName)
        assertEquals("1234-**-5678", summary?.maskedNumber)
    }

    @Test
    fun unregisteredAccountBecomesNullSummary() {
        val summary = json.decodeFromString<SettlementAccountResponse>("""{"registered":false}""").toSummary()

        // 계좌 미등록 빈 상태(M-T4)를 이 null이 결정한다
        assertNull(summary)
    }
}
