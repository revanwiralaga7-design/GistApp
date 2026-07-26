package com.gistapp.data.remote

import com.gistapp.data.model.CreateGistRequest
import com.gistapp.data.model.Gist
import retrofit2.Response
import retrofit2.http.*

/**
 * GitHub Gist API v3 endpoints.
 * Dokumentasi: https://docs.github.com/en/rest/gists
 */
interface GitHubApiService {

    /** Ambil gists user yang terautentikasi */
    @GET("gists")
    suspend fun getMyGists(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): Response<List<Gist>>

    /** Ambil public gists */
    @GET("gists/public")
    suspend fun getPublicGists(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): Response<List<Gist>>

    /** Ambil gist spesifik by ID */
    @GET("gists/{gist_id}")
    suspend fun getGist(
        @Path("gist_id") gistId: String
    ): Response<Gist>

    /** Ambil gists user tertentu */
    @GET("users/{username}/gists")
    suspend fun getUserGists(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): Response<List<Gist>>

    /** Buat gist baru */
    @POST("gists")
    suspend fun createGist(
        @Body request: CreateGistRequest
    ): Response<Gist>

    /** Update gist (edit deskripsi / file) */
    @PATCH("gists/{gist_id}")
    suspend fun updateGist(
        @Path("gist_id") gistId: String,
        @Body request: CreateGistRequest
    ): Response<Gist>

    /** Star / unstar gist */
    @PUT("gists/{gist_id}/star")
    suspend fun starGist(@Path("gist_id") gistId: String): Response<Unit>

    @DELETE("gists/{gist_id}/star")
    suspend fun unstarGist(@Path("gist_id") gistId: String): Response<Unit>

    /** Cek apakah gist sudah distar */
    @GET("gists/{gist_id}/star")
    suspend fun isStarred(@Path("gist_id") gistId: String): Response<Unit>

    /** Hapus gist */
    @DELETE("gists/{gist_id}")
    suspend fun deleteGist(@Path("gist_id") gistId: String): Response<Unit>

    /** Verifikasi token (ambil authenticated user) */
    @GET("user")
    suspend fun getAuthenticatedUser(): Response<GitHubUserResponse>
}

/** Response dari GET /user */
data class GitHubUserResponse(
    val login: String?,
    val id: Long?,
    @com.google.gson.annotations.SerializedName("avatar_url")
    val avatarUrl: String?,
    val name: String?,
    val bio: String?,
    @com.google.gson.annotations.SerializedName("public_gists")
    val publicGists: Int?
)
