package com.znarfm.siphon.ui.browser

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.net.URLDecoder

private data class ParsedMagnetInfo(
    val displayName: String,
    val infoHash: String?
)

private fun parseMagnetInfo(magnetUri: String): ParsedMagnetInfo {
    val query = magnetUri.substringAfter('?', "")
    var dn: String? = null
    var xt: String? = null

    if (query.isNotBlank()) {
        query.split('&').forEach { part ->
            val idx = part.indexOf('=')
            if (idx > 0) {
                val key = part.substring(0, idx).lowercase()
                val rawVal = part.substring(idx + 1)
                when (key) {
                    "dn" -> if (dn == null && rawVal.isNotBlank()) {
                        dn = runCatching { URLDecoder.decode(rawVal, "UTF-8") }.getOrDefault(rawVal)
                    }
                    "xt" -> if (xt == null && rawVal.isNotBlank()) {
                        val decoded = runCatching { URLDecoder.decode(rawVal, "UTF-8") }.getOrDefault(rawVal)
                        xt = decoded.substringAfterLast(':')
                    }
                }
            }
        }
    }

    return ParsedMagnetInfo(
        displayName = dn?.takeIf { it.isNotBlank() } ?: "Magnet Torrent",
        infoHash = xt?.takeIf { it.isNotBlank() }
    )
}

private fun formatDownloadLocationLabel(customDownloadUri: String?): String {
    if (customDownloadUri.isNullOrBlank()) return "System Downloads"
    return runCatching {
        val parsed = Uri.parse(customDownloadUri)
        parsed.lastPathSegment?.substringAfterLast(':')?.takeIf { it.isNotBlank() }
            ?: parsed.path?.takeIf { it.isNotBlank() }
            ?: customDownloadUri
    }.getOrDefault(customDownloadUri)
}

@Composable
fun MagnetDownloadDialog(
    magnetUri: String,
    customDownloadUri: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val magnetInfo = remember(magnetUri) { parseMagnetInfo(magnetUri) }
    val locationLabel = remember(customDownloadUri) { formatDownloadLocationLabel(customDownloadUri) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Hub,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Download Torrent?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = magnetInfo.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (!magnetInfo.infoHash.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Info Hash",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = magnetInfo.infoHash,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Save to",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = locationLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
