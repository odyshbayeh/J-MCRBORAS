package com.aftersales.app.data.api

import com.aftersales.app.models.ApiResponse
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.coroutines.cancellation.CancellationException

sealed interface ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>
    data class Error(val error: ApiError) : ApiResult<Nothing>
}

enum class ApiError(val isRetryable: Boolean) {
    NO_CONNECTION(true),
    TIMEOUT(true),
    SERVER_ERROR(true),
    UNAUTHORIZED(false),
    REQUEST_FAILED(false),
    INVALID_RESPONSE(false),
    UNKNOWN(false),
}

suspend fun <T> apiCall(
    maxAttempts: Int = 3,
    retryDelayMillis: Long = 400L,
    request: suspend () -> ApiResponse<T>,
): ApiResult<T?> {
    repeat(maxAttempts - 1) { attempt ->
        val result = execute(request)
        if (result !is ApiResult.Error || !result.error.isRetryable) return result
        delay(retryDelayMillis * (attempt + 1))
    }
    return execute(request)
}

private suspend fun <T> execute(request: suspend () -> ApiResponse<T>): ApiResult<T?> = try {
    val response = request()
    if (response.success) ApiResult.Success(response.data) else ApiResult.Error(ApiError.REQUEST_FAILED)
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Throwable) {
    ApiResult.Error(error.toApiError())
}

private fun Throwable.toApiError(): ApiError = when (this) {
    is InterruptedIOException -> ApiError.TIMEOUT
    is IOException -> ApiError.NO_CONNECTION
    is HttpException -> when {
        code() == 401 -> ApiError.UNAUTHORIZED
        code() >= 500 -> ApiError.SERVER_ERROR
        else -> ApiError.REQUEST_FAILED
    }
    is SerializationException -> ApiError.INVALID_RESPONSE
    else -> ApiError.UNKNOWN
}
