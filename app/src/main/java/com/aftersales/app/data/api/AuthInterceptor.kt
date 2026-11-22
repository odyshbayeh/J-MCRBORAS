package com.aftersales.app.data.api

import com.aftersales.app.data.prefs.SessionPreferences
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionPreferences: SessionPreferences,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionPreferences.accessToken
        val request = chain.request().newBuilder().apply {
            header("Accept", "application/json")
            if (!token.isNullOrBlank()) header(HEADER_AUTHORIZATION, "Bearer $token")
        }.build()
        return chain.proceed(request)
    }

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
    }
}
