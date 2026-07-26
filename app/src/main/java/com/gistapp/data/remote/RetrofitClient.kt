package com.gistapp.data.remote

import com.gistapp.util.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit client untuk GitHub API.
 * Menggunakan token Personal Access Token (PAT) dari TokenManager.
 */
object RetrofitClient {

    private const val BASE_URL = "https://api.github.com/"

    private lateinit var apiService: GitHubApiService
    private var currentToken: String? = null

    fun getApiService(tokenManager: TokenManager): GitHubApiService {
        val token = tokenManager.getToken()

        // Rebuild jika token berubah
        if (!::apiService.isInitialized || token != currentToken) {
            currentToken = token

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder().apply {
                    addHeader("Accept", "application/vnd.github.v3+json")
                    addHeader("User-Agent", "GistApp-Android")
                    if (!token.isNullOrEmpty()) {
                        addHeader("Authorization", "token $token")
                    }
                }.build()
                chain.proceed(request)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(GitHubApiService::class.java)
        }

        return apiService
    }
}
