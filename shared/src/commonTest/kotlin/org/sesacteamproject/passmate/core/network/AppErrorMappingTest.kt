package org.sesacteamproject.passmate.core.network

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.sesacteamproject.passmate.core.model.AppError

// 서버 오류 응답을 상태 코드별 AppError로 옮기고 {code}를 보존한다 (규칙 §10).
// code 문자열은 서버가 실제로 보내는 값 그대로 적는다(백엔드 ErrorCode)
class AppErrorMappingTest {

    // 게스트가 유료 방 같은 회원 전용 기능에 닿으면 서버는 403 GUEST_NOT_ALLOWED를 준다 — 로그인 유도로 다룬다 (규칙 §8)
    @Test
    fun guestNotAllowedBecomesLoginRequired() {
        val error = appErrorOf(HttpStatusCode.Forbidden, "GUEST_NOT_ALLOWED", "회원만 이용할 수 있습니다. 로그인해 주세요.")

        assertIs<AppError.LoginRequired>(error)
        assertEquals("GUEST_NOT_ALLOWED", error.serverCode)
    }

    @Test
    fun otherForbiddenStaysPermissionDenied() {
        val error = appErrorOf(HttpStatusCode.Forbidden, "HOST_CANNOT_JOIN", null)

        assertIs<AppError.PermissionDenied>(error)
        assertEquals("HOST_CANNOT_JOIN", error.serverCode)
    }

    @Test
    fun unauthorizedStaysUnauthorized() {
        val error = appErrorOf(HttpStatusCode.Unauthorized, "TOKEN_EXPIRED", null)

        assertIs<AppError.Unauthorized>(error)
    }
}
