package com.example.data.remote

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val latestTag: String,
    val downloadUrl: String,
    val releaseNotes: String
)

object AppUpdateChecker {
    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/SONUSHARMABJLDRBKH/pissa/releases/latest"

    suspend fun checkForNewerRelease(fallbackUrl: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "RoyalBullet350-Android/${BuildConfig.VERSION_NAME}")
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val tag = json.optString("tag_name", "").trim()
                val htmlUrl = json.optString("html_url", "").trim()
                val releaseBody = json.optString("body", "").trim()

                var apkDownloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        val url = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                            apkDownloadUrl = url
                            break
                        }
                    }
                }

                val targetUrl = apkDownloadUrl.ifBlank { htmlUrl.ifBlank { fallbackUrl } }
                if (tag.isNotBlank() && isRemoteNewer(tag, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)) {
                    AppUpdateInfo(
                        latestTag = tag,
                        downloadUrl = targetUrl,
                        releaseNotes = releaseBody
                    )
                } else {
                    null
                }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun isRemoteNewer(remoteTag: String, currentVersionName: String, currentVersionCode: Int): Boolean {
        val cleanRemote = remoteTag.lowercase().removePrefix("v").trim()
        val remoteNumbers = Regex("\\d+").findAll(cleanRemote).mapNotNull { it.value.toIntOrNull() }.toList()
        if (remoteNumbers.isEmpty()) return false

        // Compare semantic-like tags (e.g. "2.0", "1.2") against installed versionName (e.g. "1.1")
        // Ignore pure legacy debug tags like "debug-apk-build-1-1" unless major > currentVersionCode
        if (cleanRemote.startsWith("debug-apk-build")) {
            val buildVer = remoteNumbers.firstOrNull() ?: 0
            return buildVer > currentVersionCode
        }

        val currentNumbers = Regex("\\d+").findAll(currentVersionName).mapNotNull { it.value.toIntOrNull() }.toList()
        val maxLen = maxOf(remoteNumbers.size, currentNumbers.size)
        for (i in 0 until maxLen) {
            val r = remoteNumbers.getOrElse(i) { 0 }
            val c = currentNumbers.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
