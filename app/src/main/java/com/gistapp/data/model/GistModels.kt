package com.gistapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * Model untuk GitHub Gist.
 * Semua field sesuai dengan GitHub Gist API v3.
 */

// --- Gist utama ---
data class Gist(
    val id: String,
    val url: String?,
    @SerializedName("html_url")
    val htmlUrl: String?,
    val description: String?,
    val files: Map<String, GistFile>,
    val owner: GistOwner?,
    @SerializedName("public")
    val isPublic: Boolean,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    val comments: Int?
)

// --- File dalam Gist ---
data class GistFile(
    val filename: String?,
    val type: String?,
    val language: String?,
    @SerializedName("raw_url")
    val rawUrl: String?,
    val size: Long?,
    val content: String?,
    val truncated: Boolean?
)

// --- Owner Gist ---
data class GistOwner(
    val login: String?,
    val id: Long?,
    @SerializedName("avatar_url")
    val avatarUrl: String?,
    @SerializedName("html_url")
    val htmlUrl: String?
)

// --- Request body: Create / Update Gist ---
// Note: Map value nullable — null = delete file (rename support)
data class CreateGistRequest(
    val description: String?,
    @SerializedName("public")
    val isPublic: Boolean,
    val files: Map<String, GistFileContent?>
)

data class GistFileContent(
    val content: String?
)

// --- Response wrapper (opsional) ---
data class GistResponse(
    val id: String,
    val url: String?,
    @SerializedName("html_url")
    val htmlUrl: String?,
    val description: String?,
    val files: Map<String, GistFile>?,
    val owner: GistOwner?,
    @SerializedName("public")
    val isPublic: Boolean?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)

// --- Error response dari GitHub ---
data class GitHubError(
    val message: String?,
    @SerializedName("documentation_url")
    val documentationUrl: String?
)

// --- GitHub Repository ---
data class GitHubRepo(
    val id: Long,
    val name: String?,
    @SerializedName("full_name")
    val fullName: String?,
    @SerializedName("html_url")
    val htmlUrl: String?,
    val description: String?,
    val language: String?,
    @SerializedName("private")
    val isPrivate: Boolean,
    @SerializedName("stargazers_count")
    val stars: Int?,
    @SerializedName("forks_count")
    val forks: Int?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    @SerializedName("pushed_at")
    val pushedAt: String?
)
