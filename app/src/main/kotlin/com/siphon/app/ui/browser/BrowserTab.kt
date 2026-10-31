package com.siphon.app.ui.browser

import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "https://google.com",
    val title: String = "New Tab"
)
