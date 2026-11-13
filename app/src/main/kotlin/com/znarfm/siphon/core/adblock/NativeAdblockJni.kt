package com.znarfm.siphon.core.adblock

import org.json.JSONObject

data class CosmeticResources(
    val hideSelectors: List<String> = emptyList(),
    val injectedScript: String = "",
    val generichide: Boolean = false,
    val exceptions: List<String> = emptyList()
)

interface NativeAdblockBridge {
    val isLoaded: Boolean

    fun createFromRules(rules: String): Long

    fun createFromDat(data: ByteArray): Long

    fun serialize(handle: Long): ByteArray?

    fun checkNetwork(
        handle: Long,
        url: String,
        sourceUrl: String,
        requestType: String
    ): Boolean

    fun cosmeticResources(handle: Long, url: String): CosmeticResources

    fun hiddenClassIdSelectors(
        handle: Long,
        classes: Array<String>,
        ids: Array<String>,
        exceptions: Array<String>
    ): Array<String>

    fun destroy(handle: Long)
}

object NativeAdblockJni : NativeAdblockBridge {
    override val isLoaded: Boolean = try {
        System.loadLibrary("adblock_jni")
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    } catch (_: Throwable) {
        false
    }

    override fun createFromRules(rules: String): Long {
        if (!isLoaded) return 0L
        return nativeCreateFromRules(rules)
    }

    override fun createFromDat(data: ByteArray): Long {
        if (!isLoaded || data.isEmpty()) return 0L
        return nativeCreateFromDat(data)
    }

    override fun serialize(handle: Long): ByteArray? {
        if (!isLoaded || handle == 0L) return null
        return nativeSerialize(handle)
    }

    override fun checkNetwork(
        handle: Long,
        url: String,
        sourceUrl: String,
        requestType: String
    ): Boolean {
        if (!isLoaded || handle == 0L) return false
        return nativeCheckNetwork(handle, url, sourceUrl, requestType)
    }

    override fun cosmeticResources(handle: Long, url: String): CosmeticResources {
        if (!isLoaded || handle == 0L) return CosmeticResources()
        val rawJson = nativeCosmeticResourcesJson(handle, url) ?: return CosmeticResources()
        return parseCosmeticResources(rawJson)
    }

    override fun hiddenClassIdSelectors(
        handle: Long,
        classes: Array<String>,
        ids: Array<String>,
        exceptions: Array<String>
    ): Array<String> {
        if (!isLoaded || handle == 0L) return emptyArray()
        return nativeHiddenClassIdSelectors(handle, classes, ids, exceptions) ?: emptyArray()
    }

    override fun destroy(handle: Long) {
        if (!isLoaded || handle == 0L) return
        nativeDestroy(handle)
    }

    private fun parseCosmeticResources(rawJson: String): CosmeticResources {
        return try {
            val json = JSONObject(rawJson)
            val hideSelectorsArray = json.optJSONArray("hideSelectors")
            val hideSelectors = buildList {
                if (hideSelectorsArray != null) {
                    for (i in 0 until hideSelectorsArray.length()) {
                        val item = hideSelectorsArray.optString(i)
                        if (item.isNotEmpty()) add(item)
                    }
                }
            }
            val injectedScript = json.optString("injectedScript", "")
            val generichide = json.optBoolean("generichide", false)
            val exceptionsArray = json.optJSONArray("exceptions")
            val exceptions = buildList {
                if (exceptionsArray != null) {
                    for (i in 0 until exceptionsArray.length()) {
                        val item = exceptionsArray.optString(i)
                        if (item.isNotEmpty()) add(item)
                    }
                }
            }
            CosmeticResources(
                hideSelectors = hideSelectors,
                injectedScript = injectedScript,
                generichide = generichide,
                exceptions = exceptions
            )
        } catch (_: Throwable) {
            CosmeticResources()
        }
    }

    private external fun nativeCreateFromRules(rules: String): Long
    private external fun nativeCreateFromDat(data: ByteArray): Long
    private external fun nativeSerialize(handle: Long): ByteArray?
    private external fun nativeCheckNetwork(
        handle: Long,
        url: String,
        sourceUrl: String,
        requestType: String
    ): Boolean

    private external fun nativeCosmeticResourcesJson(handle: Long, url: String): String?
    private external fun nativeHiddenClassIdSelectors(
        handle: Long,
        classes: Array<String>,
        ids: Array<String>,
        exceptions: Array<String>
    ): Array<String>?

    private external fun nativeDestroy(handle: Long)
}
