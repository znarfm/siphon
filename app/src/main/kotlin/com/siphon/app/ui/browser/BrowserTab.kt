package com.siphon.app.ui.browser

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "https://google.com",
    val title: String = "New Tab",
    val isIncognito: Boolean = false
)
