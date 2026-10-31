package com.siphon.app.ui.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class ContextMenuTarget(
    val linkUrl: String? = null,
    val imageUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkContextMenuSheet(
    target: ContextMenuTarget,
    onDismiss: () -> Unit,
    onOpenInNewTab: (url: String) -> Unit,
    onOpenInIncognitoTab: (url: String) -> Unit,
    onDownloadImage: ((url: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            val previewUrl = target.linkUrl ?: target.imageUrl ?: ""
            Text(
                text = previewUrl,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            if (!target.linkUrl.isNullOrBlank()) {
                val link = target.linkUrl
                ContextMenuItem(title = "Open in new tab") {
                    onDismiss()
                    onOpenInNewTab(link)
                }

                ContextMenuItem(title = "Open in incognito tab") {
                    onDismiss()
                    onOpenInIncognitoTab(link)
                }

                ContextMenuItem(title = "Copy link address") {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Link", link))
                    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }

                ContextMenuItem(title = "Share link") {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, link)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share link"))
                    onDismiss()
                }
            }

            if (!target.imageUrl.isNullOrBlank()) {
                val img = target.imageUrl
                if (!target.linkUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                }

                ContextMenuItem(title = "Open image in new tab") {
                    onDismiss()
                    onOpenInNewTab(img)
                }

                ContextMenuItem(title = "Download image") {
                    onDismiss()
                    onDownloadImage?.invoke(img)
                }

                ContextMenuItem(title = "Copy image address") {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Image URL", img))
                    Toast.makeText(context, "Image URL copied to clipboard", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ContextMenuItem(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
