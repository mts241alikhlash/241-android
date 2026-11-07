package id.sch.mts241alikhlash.exam.data.auth

import okhttp3.logging.HttpLoggingInterceptor

fun authLoggingInterceptor(
    enabled: Boolean,
    logger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT,
): HttpLoggingInterceptor =
    HttpLoggingInterceptor(logger).apply {
        level = if (enabled) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
    }
