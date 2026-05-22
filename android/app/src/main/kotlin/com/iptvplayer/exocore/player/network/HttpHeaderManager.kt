package com.iptvplayer.exocore.player.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

class HttpHeaderManager(
    private var customHeaders: Map<String, String> = emptyMap()
) {
    fun updateHeaders(headers: Map<String, String>) {
        customHeaders = headers
    }

    fun buildOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HeaderInterceptor { customHeaders })
            .addInterceptor(UserAgentInterceptor())
            .build()
    }

    private class HeaderInterceptor(
        private val headersProvider: () -> Map<String, String>
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val builder = original.newBuilder()
            headersProvider().forEach { (key, value) ->
                builder.header(key, value)
            }
            return chain.proceed(builder.build())
        }
    }

    private class UserAgentInterceptor : Interceptor {
        companion object {
            private const val DEFAULT_UA =
                "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
        }

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            return if (request.header("User-Agent") == null) {
                chain.proceed(
                    request.newBuilder()
                        .header("User-Agent", DEFAULT_UA)
                        .build()
                )
            } else {
                chain.proceed(request)
            }
        }
    }
}
