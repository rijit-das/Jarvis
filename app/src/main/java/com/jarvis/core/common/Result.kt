package com.jarvis.core.common

/**
 * Typed result used across domain layers. Errors carry a user-presentable
 * message; nothing is silently swallowed.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : AppResult<Nothing>
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.value

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
}
