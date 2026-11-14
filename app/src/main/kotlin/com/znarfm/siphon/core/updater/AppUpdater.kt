package com.znarfm.siphon.core.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.znarfm.siphon.core.network.NetworkClients
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val versionName: String,
    val tagName: String,
    val releaseUrl: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val expectedSha256: String?
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val currentVersion: String) : UpdateState
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateState
    data class Downloading(
        val release: ReleaseInfo,
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateState
    data class Verifying(val release: ReleaseInfo) : UpdateState
    data class ReadyToInstall(
        val release: ReleaseInfo,
        val apkFile: File,
        val verifiedSha256: String
    ) : UpdateState
    data class Error(val message: String) : UpdateState
}

class AppUpdater(private val client: OkHttpClient = defaultClient()) {

    suspend fun checkForUpdate(currentVersion: String): Result<ReleaseInfo?> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(GITHUB_API_LATEST_RELEASE)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()

                val body = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("GitHub API returned HTTP ${response.code}")
                        )
                    }
                    response.body?.string()
                }

                if (body.isNullOrBlank()) {
                    return@withContext Result.failure(
                        IOException("Empty response from GitHub releases API")
                    )
                }

                val release = parseReleaseJson(body)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Latest release does not contain a valid APK asset")
                    )

                if (isNewerVersion(release.versionName, currentVersion)) {
                    Result.success(release)
                } else {
                    Result.success(null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun downloadAndVerifyApk(
        cacheDir: File,
        release: ReleaseInfo,
        onProgress: (Int, Long, Long) -> Unit
    ): Result<Pair<File, String>> = withContext(Dispatchers.IO) {
        val expectedSha256 = normalizeSha256Hex(release.expectedSha256)
        if (expectedSha256 == null) {
            return@withContext Result.failure(
                IllegalStateException("Release is missing a valid SHA-256 checksum; aborting update for security.")
            )
        }

        val updatesDir = File(cacheDir, "updates").apply { mkdirs() }
        val partFile = File(updatesDir, "siphon-update.apk.part")
        val finalFile = File(updatesDir, "siphon-update.apk")

        partFile.delete()

        var movedToFinal = false
        try {
            val request = Request.Builder()
                .url(release.apkUrl)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            val computedHex = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Failed to download APK: HTTP ${response.code}")
                }
                val responseBody = response.body
                    ?: throw IOException("Empty APK response body")

                val totalBytes = responseBody.contentLength().takeIf { it > 0L }
                    ?: release.apkSizeBytes

                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloadedBytes = 0L
                var lastReportedPercent = -1

                responseBody.byteStream().use { input ->
                    partFile.outputStream().use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloadedBytes += read

                            val percent = if (totalBytes > 0L) {
                                ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }
                            if (percent != lastReportedPercent || downloadedBytes == totalBytes) {
                                lastReportedPercent = percent
                                onProgress(percent, downloadedBytes, totalBytes)
                            }
                        }
                        output.flush()
                    }
                }

                digest.digest().joinToString("") { "%02x".format(it) }
            }

            if (!computedHex.equals(expectedSha256, ignoreCase = true)) {
                partFile.delete()
                return@withContext Result.failure(
                    SecurityException(
                        "SHA-256 verification failed. Expected $expectedSha256 but got $computedHex"
                    )
                )
            }

            moveAtomically(partFile, finalFile)
            movedToFinal = true
            Result.success(finalFile to computedHex)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (!movedToFinal) {
                partFile.delete()
            }
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
    }

    fun createInstallPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun createInstallIntent(context: Context, apkFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun moveAtomically(source: File, target: File) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: IOException) {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    companion object {
        const val GITHUB_API_LATEST_RELEASE =
            "https://api.github.com/repos/znarfm/siphon/releases/latest"
        const val AUTHOR_GITHUB_URL = "https://github.com/znarfm"
        private const val USER_AGENT = "Siphon-Android"
        private const val DEFAULT_BUFFER_SIZE = 16 * 1024
        private val SHA256_HEX_REGEX = Regex("^[0-9a-fA-F]{64}$")

        private fun defaultClient(): OkHttpClient {
            return NetworkClients.defaultClient.newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }

        internal fun isNewerVersion(remote: String, current: String): Boolean {
            val remoteParts = parseVersionParts(remote)
            val currentParts = parseVersionParts(current)
            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }

        internal fun parseVersionParts(version: String): List<Int> {
            val cleaned = version.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore('-')
                .substringBefore('+')
                .trim()

            if (cleaned.isEmpty()) return listOf(0)

            return cleaned.split('.').map { segment ->
                segment.trim().toIntOrNull() ?: 0
            }
        }

        internal fun parseReleaseJson(json: String): ReleaseInfo? {
            val root = try {
                JSONObject(json)
            } catch (_: Exception) {
                return null
            }

            val tagName = root.optString("tag_name", "").takeIf { it.isNotBlank() } ?: return null
            val releaseUrl = root.optString("html_url", "")
            val releaseNotes = root.optString("body", "")
            val assets = root.optJSONArray("assets") ?: JSONArray()

            var apkUrl: String? = null
            var apkSizeBytes = 0L
            var expectedSha256: String? = null

            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "")
                val downloadUrl = asset.optString("browser_download_url", "")
                if (downloadUrl.isBlank()) continue

                val isApk = name.endsWith(".apk", ignoreCase = true) ||
                    downloadUrl.endsWith(".apk", ignoreCase = true)

                if (isApk && apkUrl == null) {
                    apkUrl = downloadUrl
                    apkSizeBytes = asset.optLong("size", 0L)
                    val digest = asset.optString("digest", "").takeIf { it.isNotBlank() }
                    if (digest != null && digest.startsWith("sha256:", ignoreCase = true)) {
                        expectedSha256 = normalizeSha256Hex(digest.substringAfter(':'))
                    }
                    break
                }
            }

            if (apkUrl.isNullOrBlank()) return null

            val versionName = tagName.trim().removePrefix("v").removePrefix("V")
            return ReleaseInfo(
                versionName = versionName,
                tagName = tagName,
                releaseUrl = releaseUrl,
                releaseNotes = releaseNotes,
                apkUrl = apkUrl,
                apkSizeBytes = apkSizeBytes,
                expectedSha256 = expectedSha256
            )
        }

        internal fun normalizeSha256Hex(hex: String?): String? {
            val trimmed = hex?.trim() ?: return null
            return if (SHA256_HEX_REGEX.matches(trimmed)) trimmed.lowercase() else null
        }

        internal fun sha256Hex(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            file.inputStream().use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
