package org.sesacteamproject.passmate.core.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.ServerErrorCode
import org.sesacteamproject.passmate.core.network.dto.ErrorResponse

private val errorJson = Json { ignoreUnknownKeys = true }

private suspend fun HttpResponse.toErrorResponse(): ErrorResponse {
    return try {
        errorJson.decodeFromString(ErrorResponse.serializer(), bodyAsText())
    } catch (e: Exception) {
        ErrorResponse()
    }
}

// 상태 코드와 서버 code로 AppError를 고른다 — code는 화면 문구 분기에 쓰도록 보존한다 (규칙 §10).
// 게스트가 회원 전용 기능(유료 방 입장 등)에 닿으면 서버는 403 GUEST_NOT_ALLOWED를 준다 —
// 권한 거부가 아니라 로그인 유도로 다룬다 (규칙 §8)
internal fun appErrorOf(status: HttpStatusCode, code: String?, message: String?): AppError {
    return when (status) {
        HttpStatusCode.Unauthorized -> AppError.Unauthorized(code, message)
        HttpStatusCode.PaymentRequired -> AppError.PaymentRequired(code, message)
        HttpStatusCode.Forbidden -> {
            if (code == ServerErrorCode.GUEST_NOT_ALLOWED) {
                AppError.LoginRequired(code, message)
            } else {
                AppError.PermissionDenied(code, message)
            }
        }
        HttpStatusCode.BadRequest -> AppError.ValidationFailed(code, message)
        HttpStatusCode.NotFound -> AppError.NotFound(code, message)
        HttpStatusCode.Conflict -> AppError.Conflict(code, message)
        HttpStatusCode.Gone -> AppError.Gone(code, message)
        else -> AppError.Unknown(code, message)
    }
}

private suspend fun HttpResponse.toAppError(): AppError {
    val error = toErrorResponse()

    return appErrorOf(status, error.code, error.message)
}

// Repository 계층 전용 — DataSource 예외를 AppResult/AppError로 변환한다 (규칙 §6·§10)
suspend fun <T> apiCall(block: suspend () -> T): AppResult<T> {
    return try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ClientRequestException) {
        AppResult.Failure(e.response.toAppError())
    } catch (e: ServerResponseException) {
        AppResult.Failure(e.response.toAppError())
    } catch (e: IOException) {
        AppResult.Failure(AppError.NetworkError(serverMessage = e.message))
    } catch (e: Exception) {
        AppResult.Failure(AppError.Unknown(serverMessage = e.message))
    }
}
