package ru.uust.schedule.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal const val GITHUB_REPOSITORY = "Gplay666/GUUSTShedule"
internal const val LATEST_RELEASE_URL =
    "https://api.github.com/repos/$GITHUB_REPOSITORY/releases/latest"

@Serializable
internal data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
internal data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0,
)
