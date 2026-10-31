package com.siphon.app.ui.sniffer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siphon.app.core.sniffer.HlsVariantInfo
import com.siphon.app.core.sniffer.SniffedMedia
import com.siphon.app.data.db.DownloadEntity
import com.siphon.app.data.db.DownloadType
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnifferSheet(
    isOpen: Boolean,
    detectedMedia: List<SniffedMedia>,
    rebindTarget: DownloadEntity?,
    onDismiss: () -> Unit,
    onDownloadMedia: (media: SniffedMedia, selectedVariantUrl: String?) -> Unit,
    onRebindMedia: ((target: DownloadEntity, media: SniffedMedia, selectedVariantUrl: String?) -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    if (!isOpen) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detected Media Streams (${detectedMedia.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (rebindTarget != null) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text("Rebind Mode Active", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (detectedMedia.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No audio or video streams detected yet.\nPlay media in the browser to sniff.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(detectedMedia, key = { it.id }) { media ->
                        SniffedMediaItemCard(
                            media = media,
                            rebindTarget = rebindTarget,
                            onDownload = { variantUrl ->
                                onDownloadMedia(media, variantUrl)
                            },
                            onRebind = { target, variantUrl ->
                                if (onRebindMedia != null) {
                                    onRebindMedia(target, media, variantUrl)
                                } else {
                                    onDownloadMedia(media, variantUrl)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SniffedMediaItemCard(
    media: SniffedMedia,
    rebindTarget: DownloadEntity?,
    onDownload: (selectedVariantUrl: String?) -> Unit,
    onRebind: ((target: DownloadEntity, selectedVariantUrl: String?) -> Unit)? = null
) {
    var selectedVariant by remember(media) {
        mutableStateOf(media.variants.firstOrNull())
    }
    var variantDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = media.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(
                            if (media.mediaType == DownloadType.HLS) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (media.mediaType == DownloadType.HLS) "HLS" else "DIRECT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (media.mediaType == DownloadType.HLS) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatFileSize(media.totalBytes),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " • " + media.mimeType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // HLS Variant Selector
            if (media.variants.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box {
                    OutlinedButton(
                        onClick = { variantDropdownExpanded = true },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Quality: " + (selectedVariant?.resolution ?: "${selectedVariant?.bandwidth ?: 0} bps"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    DropdownMenu(
                        expanded = variantDropdownExpanded,
                        onDismissRequest = { variantDropdownExpanded = false }
                    ) {
                        media.variants.forEach { variant ->
                            DropdownMenuItem(
                                text = {
                                    Text(variant.resolution ?: "${variant.bandwidth / 1000} kbps")
                                },
                                onClick = {
                                    selectedVariant = variant
                                    variantDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (rebindTarget != null && onRebind != null) {
                        onRebind(rebindTarget, selectedVariant?.url)
                    } else {
                        onDownload(selectedVariant?.url)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = if (rebindTarget != null) "Rebind to Paused Task" else "Download Stream",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "Stream (Adaptive)"
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb >= 1000) {
        String.format(Locale.US, "%.2f GB", mb / 1024)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
}
