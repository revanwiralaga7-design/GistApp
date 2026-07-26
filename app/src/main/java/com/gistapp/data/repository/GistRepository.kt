package com.gistapp.data.repository

import com.gistapp.data.model.CreateGistRequest
import com.gistapp.data.model.Gist
import com.gistapp.data.model.GitHubError
import com.gistapp.data.remote.GitHubApiService
import com.gistapp.data.remote.GitHubUserResponse
import com.gistapp.util.TokenManager
import com.google.gson.Gson

/**
 * Repository layer untuk operasi Gist.
 * Menangani error parsing dan mapping dari API response.
 */
class GistRepository(private val apiService: GitHubApiService) {

    private val gson = Gson()

    // --- Gists ---

    suspend fun getMyGists(page: Int = 1): Result<List<Gist>> {
        return try {
            val response = apiService.getMyGists(page = page)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPublicGists(page: Int = 1): Result<List<Gist>> {
        return try {
            val response = apiService.getPublicGists(page = page)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGist(gistId: String): Result<Gist> {
        return try {
            val response = apiService.getGist(gistId)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserGists(username: String): Result<List<Gist>> {
        return try {
            val response = apiService.getUserGists(username)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Create / Update ---

    suspend fun createGist(request: CreateGistRequest): Result<Gist> {
        return try {
            val response = apiService.createGist(request)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGist(gistId: String, request: CreateGistRequest): Result<Gist> {
        return try {
            val response = apiService.updateGist(gistId, request)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Delete ---

    suspend fun deleteGist(gistId: String): Result<Unit> {
        return try {
            val response = apiService.deleteGist(gistId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val error = parseError(response.errorBody()?.string())
                Result.failure(Exception(error?.message ?: "Gagal menghapus gist"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Star ---

    suspend fun starGist(gistId: String): Result<Unit> {
        return try {
            val response = apiService.starGist(gistId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Gagal star gist"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unstarGist(gistId: String): Result<Unit> {
        return try {
            val response = apiService.unstarGist(gistId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Gagal unstar gist"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isStarred(gistId: String): Boolean {
        return try {
            apiService.isStarred(gistId).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    // --- Auth ---

    suspend fun verifyToken(): Result<GitHubUserResponse> {
        return try {
            val response = apiService.getAuthenticatedUser()
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Utility ---

    private fun <T> handleResponse(response: retrofit2.Response<T>): Result<T> {
        return if (response.isSuccessful) {
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
    }

    private fun parseError(errorBody: String?): GitHubError? {
        return try {
            gson.fromJson(errorBody, GitHubError::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
