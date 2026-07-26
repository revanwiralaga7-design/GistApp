package com.gistapp.data.remote

import com.gistapp.data.model.CreateGistRequest
import com.gistapp.data.model.Gist
import com.gistapp.data.model.GitHubRepo
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * GitHub API v3 endpoints — Gist + User + Repo.
 */
interface GitHubApiService {

    // ===== GISTS =====

    @GET("gists")
    suspend fun getMyGists(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): Response<List<Gist>>

    @GET("gists/public")
    suspend fun getPublicGists(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): Response<List<Gist>>

    @GET("gists/{gist_id}")
    suspend fun getGist(@Path("gist_id") gistId: String): Response<Gist>

    @GET("users/{username}/gists")
    suspend fun getUserGists(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): Response<List<Gist>>

    @POST("gists")
    suspend fun createGist(@Body request: CreateGistRequest): Response<Gist>

    @PATCH("gists/{gist_id}")
    suspend fun updateGist(
        @Path("gist_id") gistId: String,
        @Body request: CreateGistRequest
    ): Response<Gist>

    @HTTP(method = "DELETE", path = "gists/{gist_id}", hasBody = false)
    suspend fun deleteGist(@Path("gist_id") gistId: String): Response<Unit>

    @PUT("gists/{gist_id}/star")
    suspend fun starGist(@Path("gist_id") gistId: String): Response<Unit>

    @HTTP(method = "DELETE", path = "gists/{gist_id}/star", hasBody = false)
    suspend fun unstarGist(@Path("gist_id") gistId: String): Response<Unit>

    @GET("gists/{gist_id}/star")
    suspend fun isStarred(@Path("gist_id") gistId: String): Response<Unit>

    // ===== USER =====

    @GET("user")
    suspend fun getAuthenticatedUser(): Response<GitHubUserResponse>

    @GET("users/{username}")
    suspend fun getUser(@Path("username") username: String): Response<GitHubUserResponse>

    // ===== REPOS =====

    @GET("user/repos")
    suspend fun getMyRepos(
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 50,
        @Query("type") type: String = "owner"
    ): Response<List<GitHubRepo>>

    @GET("users/{username}/repos")
    suspend fun getUserRepos(
        @Path("username") username: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 50
    ): Response<List<GitHubRepo>>

    // ===== RAW FILE (untuk fetch full content) =====
    // Ini dilakukan via raw URL, bukan Retrofit endpoint biasa

    companion object {
        /**
         * Fetch raw file content dari raw URL.
         * Dipakai untuk mengambil konten penuh file yang truncated.
         */
        suspend fun fetchRawContent(rawUrl: String, token: String?): String {
            return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val requestBuilder = Request.Builder().url(rawUrl)
                requestBuilder.addHeader("User-Agent", "GistApp-Android")
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "token $token")
                }
                val response = client.newCall(requestBuilder.build()).execute()
                response.body?.string() ?: ""
            }
        }
    }
}

data class GitHubUserResponse(
    val login: String?,
    val id: Long?,
    @com.google.gson.annotations.SerializedName("avatar_url")
    val avatarUrl: String?,
    val name: String?,
    val bio: String?,
    val company: String?,
    val location: String?,
    val blog: String?,
    @com.google.gson.annotations.SerializedName("public_repos")
    val publicRepos: Int?,
    @com.google.gson.annotations.SerializedName("public_gists")
    val publicGists: Int?,
    val followers: Int?,
    val following: Int?
)
