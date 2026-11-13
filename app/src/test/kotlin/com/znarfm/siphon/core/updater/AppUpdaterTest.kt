package com.znarfm.siphon.core.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.MessageDigest

class AppUpdaterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun isNewerVersion_comparesSemverCorrectly() {
        assertTrue(AppUpdater.isNewerVersion("v0.3.2", "0.3.1"))
        assertFalse(AppUpdater.isNewerVersion("0.3.1", "v0.3.1"))
        assertFalse(AppUpdater.isNewerVersion("0.3.0", "0.3.1"))
        assertTrue(AppUpdater.isNewerVersion("v1.0.0", "0.9.9"))
    }

    @Test
    fun parseReleaseJson_extractsDigestSha256() {
        val sampleDigest = "a1b2c3d4e5f60718293a4b5c6d7e8f90123456789abcdef0123456789abcdef0"
        val json = """
            {
              "tag_name": "v0.3.2",
              "html_url": "https://github.com/znarfm/siphon/releases/tag/v0.3.2",
              "body": "Bug fixes and \"improvements\".\n- Faster downloads",
              "assets": [
                {
                  "name": "app-release.apk",
                  "size": 12345678,
                  "digest": "sha256:$sampleDigest",
                  "browser_download_url": "https://github.com/znarfm/siphon/releases/download/v0.3.2/app-release.apk"
                }
              ]
            }
        """.trimIndent()

        val release = AppUpdater.parseReleaseJson(json)
        assertNotNull(release)
        release!!
        assertEquals("0.3.2", release.versionName)
        assertEquals("v0.3.2", release.tagName)
        assertEquals("https://github.com/znarfm/siphon/releases/tag/v0.3.2", release.releaseUrl)
        assertEquals("Bug fixes and \"improvements\".\n- Faster downloads", release.releaseNotes)
        assertEquals(
            "https://github.com/znarfm/siphon/releases/download/v0.3.2/app-release.apk",
            release.apkUrl
        )
        assertEquals(12345678L, release.apkSizeBytes)
        assertEquals(sampleDigest, release.expectedSha256)
    }

    @Test
    fun parseReleaseJson_normalizesUppercaseDigestSha256() {
        val hexUpper = "ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789"
        val json = """
            {
              "tag_name": "v0.4.0",
              "html_url": "https://github.com/znarfm/siphon/releases/tag/v0.4.0",
              "body": "Release notes",
              "assets": [
                {
                  "name": "app-release.apk",
                  "size": 9876543,
                  "digest": "SHA256:$hexUpper",
                  "browser_download_url": "https://github.com/znarfm/siphon/releases/download/v0.4.0/app-release.apk"
                }
              ]
            }
        """.trimIndent()

        val release = AppUpdater.parseReleaseJson(json)
        assertNotNull(release)
        release!!
        assertEquals(hexUpper.lowercase(), release.expectedSha256)
    }

    @Test
    fun parseReleaseJson_returnsNullExpectedSha256WhenDigestMissingOrInvalid() {
        val missingDigestJson = """
            {
              "tag_name": "v0.4.0",
              "html_url": "https://github.com/znarfm/siphon/releases/tag/v0.4.0",
              "body": "Release notes",
              "assets": [
                {
                  "name": "app-release.apk",
                  "size": 9876543,
                  "browser_download_url": "https://github.com/znarfm/siphon/releases/download/v0.4.0/app-release.apk"
                }
              ]
            }
        """.trimIndent()

        val missingRelease = AppUpdater.parseReleaseJson(missingDigestJson)
        assertNotNull(missingRelease)
        assertNull(missingRelease!!.expectedSha256)

        val invalidDigestJson = """
            {
              "tag_name": "v0.4.0",
              "html_url": "https://github.com/znarfm/siphon/releases/tag/v0.4.0",
              "body": "Release notes",
              "assets": [
                {
                  "name": "app-release.apk",
                  "size": 9876543,
                  "digest": "sha256:not-a-valid-sha256",
                  "browser_download_url": "https://github.com/znarfm/siphon/releases/download/v0.4.0/app-release.apk"
                }
              ]
            }
        """.trimIndent()

        val invalidRelease = AppUpdater.parseReleaseJson(invalidDigestJson)
        assertNotNull(invalidRelease)
        assertNull(invalidRelease!!.expectedSha256)
    }

    @Test
    fun sha256Hex_computesExpectedDigestForTempFile() {
        val file = tempFolder.newFile("sample.apk")
        val content = "siphon-updater-test-payload".toByteArray()
        file.writeBytes(content)

        val expected = MessageDigest.getInstance("SHA-256")
            .digest(content)
            .joinToString("") { "%02x".format(it) }

        assertEquals(expected, AppUpdater.sha256Hex(file))
    }
}
