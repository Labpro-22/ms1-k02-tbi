package com.if3210.nimons360.util

import kotlin.coroutines.cancellation.CancellationException

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(
        val message: String,
        val exception: Throwable? = null,
    ) : Result<Nothing>()

    data object Loading : Result<Nothing>()
}

inline fun <T> repositoryCall(block: () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (throwable: Throwable) {
        Result.Error(
            message = throwable.message ?: "Unknown repository error",
            exception = throwable,
        )
    }
}
