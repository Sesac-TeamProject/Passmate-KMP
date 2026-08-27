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
import org.sesacteamproject.passmate.core.network.dto.ErrorResponse

private val errorJson = Json { ignoreUnknownKeys = true }

private suspend fun HttpResponse.toErrorResponse(): ErrorResponse {
    return try {
        errorJson.decodeFromString(ErrorResponse.serializer(), bodyAsText())
    } catch (e: Exception) {
        ErrorResponse()
    }
}

private suspend fun HttpResponse.toAppError(): AppError {
    val error = toErrorResponse()
    val code = error.code
    val message = error.message

    return when (status) {
        HttpStatusCode.Unauthorized -> {
            if (code == "LOGIN_REQUIRED") {
                AppError.LoginRequired(code, message)
            } else {
                AppError.Unauthorized(code, message)
            }
        }
        HttpStatusCode.PaymentRequired -> AppError.PaymentRequired(code, message)
        HttpStatusCode.Forbidden -> AppError.PermissionDenied(code, message)
        HttpStatusCode.BadRequest -> AppError.ValidationFailed(code, message)
        HttpStatusCode.NotFound -> AppError.NotFound(code, message)
        HttpStatusCode.Conflict -> AppError.Conflict(code, message)
        HttpStatusCode.Gone -> AppError.Gone(code, message)
        else -> AppError.Unknown(code, message)
    }
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
