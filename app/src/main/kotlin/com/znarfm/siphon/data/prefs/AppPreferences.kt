package com.znarfm.siphon.data.prefs

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    const val PREFS_NAME = "siphon_prefs"
    const val PREF_MAX_CONCURRENT_DOWNLOADS = "max_concurrent_downloads"
    const val PREF_WORKERS_PER_DOWNLOAD = "workers_per_download"
    const val PREF_BANDWIDTH_LIMIT_BYTES = "bandwidth_limit_bytes"
    const val PREF_WIFI_ONLY = "wifi_only"
    const val PREF_CUSTOM_DOWNLOAD_URI = "pref_custom_download_uri"

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
