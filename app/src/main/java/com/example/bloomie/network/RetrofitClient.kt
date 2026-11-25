package com.example.bloomie.network

import com.example.bloomie.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Shared Retrofit/OkHttp setup for Pl@ntNet.
 *
 * Reads [API_KEY] from `local.properties` via [BuildConfig.PLANTNET_API_KEY].
 * While blank or still set to the placeholder, the identify flow uses demo matches instead.
 */
object RetrofitClient {

    /** Loaded from `local.properties` (`PLANTNET_API_KEY`). */
    val API_KEY: String = BuildConfig.PLANTNET_API_KEY

    const val BASE_URL = "https://my-api.plantnet.org/"

    /** Pl@ntNet flora project. "all" searches every known plant. */
    const val PROJECT = "all"

    private const val API_KEY_PLACEHOLDER = "YOUR_PLANTNET_API_KEY"

    /** True once a real API key has been configured in `local.properties`. */
    val isApiKeyConfigured: Boolean
        get() = API_KEY.isNotBlank() && API_KEY != API_KEY_PLACEHOLDER

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val plantNetService: PlantNetApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlantNetApiService::class.java)
    }
}