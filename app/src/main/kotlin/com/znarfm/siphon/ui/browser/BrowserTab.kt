package com.znarfm.siphon.ui.browser

import androidx.compose.runtime.Immutable
import java.util.UUID

const val NEW_TAB_URL = "about:newtab"

@Immutable
data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = NEW_TAB_URL,
    val title: String = "New Tab",
    val isIncognito: Boolean = false
)
