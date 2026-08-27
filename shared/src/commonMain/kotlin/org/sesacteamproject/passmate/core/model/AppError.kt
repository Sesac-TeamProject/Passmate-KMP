package org.sesacteamproject.passmate.core.model

sealed class AppError {

    abstract val serverCode: String?

    abstract val serverMessage: String?

    data class Unauthorized(
        override val serverCode: String? = null,
        override val serverMessage: String? = null
    ) : AppError()

    data class LoginRequired(
        override val serverCode: String? = null,
        override val serverMessage: String? = null
    ) : AppError()

    data class PermissionDenied(
        override val serverCode: String? = null,
        override val serverMessage: String? = null
    ) : AppError()

    data class PaymentRequired(
        override val serverCode: String? = null,
        override val serverMessage: String? = null
    ) : AppError()

    data class ValidationFailed(
        override val serverCode: String? = null,
        override val serverMessage: String? = null
    ) : AppError()

    data class NotFound(
        override val serverCode: String? = null,
        override val serverMessage: String? = null
    ) : AppError()

    data class Conflict(
        override val serverCode: String? = null,
        override val serverMessage: String? = null
    ) : AppError()

    data class Gone(
        override val serverCode: String? = null,
        override val serverMessage: String? = null
    ) : AppError()

    data class NetworkError(
        override val serverCode: String? = null,
        override val serverMessage: String? = null
    ) : AppError()

    data class Unknown(
        override val serverCode: String? = null,
        override val serverMessage: String? = null
    ) : AppError()
}
