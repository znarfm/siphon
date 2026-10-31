package com.siphon.app.ui.queue

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siphon.app.core.network.HeadersCodec
import com.siphon.app.data.db.ChunkEntity
import com.siphon.app.data.db.ChunkState
import com.siphon.app.data.db.DownloadEntity
import com.siphon.app.data.db.DownloadType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsSheet(
    download: DownloadEntity,
    chunks: List<ChunkEntity>,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onCopyStreamUrl: () -> Unit,
    onCopyPageUrl: () -> Unit,
    onOpenFile: () -> Unit,
    onRename: () -> Unit
) {
    TaskInspectorBottomSheet(
        download = download,
        chunks = chunks,
        onDismiss = onDismiss,
        onShare = onShare,
        onCopyStreamUrl = onCopyStreamUrl,
        onCopyPageUrl = onCopyPageUrl,
        onOpenFile = onOpenFile,
        onRename = onRename
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskInspectorBottomSheet(
    download: DownloadEntity,
    chunks: List<ChunkEntity>,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onCopyStreamUrl: () -> Unit,
    onCopyPageUrl: () -> Unit,
    onOpenFile: () -> Unit,
    onRename: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val headers = remember(download.headersJson) { HeadersCodec.decode(download.headersJson) }
    val isHls = download.type == DownloadType.HLS

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isHls) "HLS .m3u8 Stream" else "Progressive HTTP Download",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StateBadge(download.state)
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // Output Storage Path
            Text("Storage Location", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                text = download.destinationUri ?: "Not yet allocated",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!download.destinationUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onOpenFile,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Show in Files / Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onShare,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Share", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Technical details
            Text("Stream & Metadata", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            val completedChunks = chunks.count { it.state == ChunkState.MERGED || it.state == ChunkState.DONE }
            val metaText = if (isHls) {
                "Segments: $completedChunks / ${chunks.size} • MIME: ${download.mimeType.ifBlank { "video/mp4" }}\n" +
                    "ETag: ${download.etag ?: "None"} • Last-Modified: ${download.lastModified ?: "None"}"
            } else {
                "Size: ${formatFileSize(download.totalBytes)} • Resumable: ${if (download.isResumable) "Yes (HTTP 206)" else "No"}\n" +
                    "Chunks: ${chunks.size} positional workers • MIME: ${download.mimeType.ifBlank { "binary/octet-stream" }}"
            }
            Text(text = metaText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Captured Headers
            if (headers.size > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Captured Headers", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(8.dp)
                ) {
                    headers.forEach { (k, v) ->
                        val displayVal = if (k.equals("Cookie", ignoreCase = true)) "${v.take(24)}…" else v
                        Text("$k: $displayVal", fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRename,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Rename", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onCopyStreamUrl,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Stream URL", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onCopyPageUrl,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Page URL", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun openCompletedFile(context: Context, download: DownloadEntity) {
    val uriStr = download.destinationUri
    if (!uriStr.isNullOrBlank()) {
        val uri = Uri.parse(uriStr)
        val mime = if (download.mimeType.contains("mpegurl")) "video/mp4" else download.mimeType.ifBlank { "video/*" }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { context.startActivity(Intent.createChooser(fallback, "Open with")) }
        }
    }
}

fun shareDownloadFile(context: Context, download: DownloadEntity) {
    val uriStr = download.destinationUri
    if (!uriStr.isNullOrBlank()) {
        val uri = Uri.parse(uriStr)
        val mime = if (download.mimeType.contains("mpegurl")) "video/mp4" else download.mimeType.ifBlank { "*/*" }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share file"))
        } catch (_: Exception) {
            Toast.makeText(context, "No app available to share file", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "File location is not yet ready", Toast.LENGTH_SHORT).show()
    }
}

fun copyToClipboard(context: Context, text: String, label: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
        ?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}
