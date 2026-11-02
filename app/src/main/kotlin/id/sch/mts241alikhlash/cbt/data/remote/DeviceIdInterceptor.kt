package id.sch.mts241alikhlash.cbt.data.remote

import okhttp3.Interceptor
import okhttp3.Response

const val DEVICE_ID_HEADER = "X-Device-Id"
const val CLIENT_PLATFORM_HEADER = "X-Client-Platform"
const val CLIENT_PLATFORM_ANDROID = "android"

class DeviceIdInterceptor(private val deviceId: () -> String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val id = deviceId()
        val request = chain.request()
            .newBuilder()
            .header(CLIENT_PLATFORM_HEADER, CLIENT_PLATFORM_ANDROID)
            .build()
        if (id.isBlank()) return chain.proceed(request)
        return chain.proceed(
            request.newBuilder().header(DEVICE_ID_HEADER, id).build()
        )
    }
}
