package com.siphon.app.ui.browser

import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "https://en.wikipedia.org",
    val title: String = "New Tab"
)
