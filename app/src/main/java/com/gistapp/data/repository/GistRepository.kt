package com.gistapp.data.repository

import com.gistapp.data.model.CreateGistRequest
import com.gistapp.data.model.Gist
import com.gistapp.data.model.GitHubError
import com.gistapp.data.model.GitHubRepo
import com.gistapp.data.remote.GitHubApiService
import com.gistapp.data.remote.GitHubUserResponse
import com.gistapp.util.TokenManager
import com.google.gson.Gson

class GistRepository(private val apiService: GitHubApiService) {

    private val gson = Gson()

    // ===== Gists =====

    suspend fun getMyGists(page: Int = 1): Result<List<Gist>> =
        runApi { apiService.getMyGists(page = page) }

    suspend fun getPublicGists(page: Int = 1): Result<List<Gist>> =
        runApi { apiService.getPublicGists(page = page) }

    suspend fun getGist(gistId: String): Result<Gist> =
        runApi { apiService.getGist(gistId) }

    suspend fun getUserGists(username: String): Result<List<Gist>> =
        runApi { apiService.getUserGists(username) }

    suspend fun createGist(request: CreateGistRequest): Result<Gist> =
        runApi { apiService.createGist(request) }

    suspend fun updateGist(gistId: String, request: CreateGistRequest): Result<Gist> =
        runApi { apiService.updateGist(gistId, request) }

    suspend fun deleteGist(gistId: String): Result<Unit> {
        return try {
            val response = apiService.deleteGist(gistId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(parseError(response.errorBody()?.string())?.message ?: "Gagal menghapus gist"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun starGist(gistId: String): Result<Unit> {
        return try {
            if (apiService.starGist(gistId).isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Gagal star gist"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun unstarGist(gistId: String): Result<Unit> {
        return try {
            if (apiService.unstarGist(gistId).isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Gagal unstar gist"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun isStarred(gistId: String): Boolean {
        return try { apiService.isStarred(gistId).isSuccessful } catch (e: Exception) { false }
    }

    // ===== User / Auth =====

    suspend fun verifyToken(): Result<GitHubUserResponse> =
        runApi { apiService.getAuthenticatedUser() }

    suspend fun getUser(username: String): Result<GitHubUserResponse> =
        runApi { apiService.getUser(username) }

    // ===== Repos =====

    suspend fun getMyRepos(): Result<List<GitHubRepo>> =
        runApi { apiService.getMyRepos() }

    suspend fun getUserRepos(username: String): Result<List<GitHubRepo>> =
        runApi { apiService.getUserRepos(username) }

    // ===== Raw content (full fetch untuk file truncated) =====

    suspend fun fetchRawContent(rawUrl: String, token: String?): String {
        return try {
            GitHubApiService.fetchRawContent(rawUrl, token)
        } catch (e: Exception) { "" }
    }

    // ===== Utility =====

    private suspend fun <T> runApi(call: suspend () -> retrofit2.Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val error = parseError(errorBody)
                val message = when (response.code()) {
                    401 -> "Token tidak valid. Silakan login ulang."
                    403 -> "Rate limit tercapai atau akses ditolak."
                    404 -> "Data tidak ditemukan."
                    422 -> "Validasi gagal: ${error?.message ?: "cek input"}"
                    else -> error?.message ?: "Error ${response.code()}"
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseError(errorBody: String?): GitHubError? {
        return try { gson.fromJson(errorBody, GitHubError::class.java) } catch (e: Exception) { null }
    }
}
