package com.znarfm.siphon.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.znarfm.siphon.core.updater.UpdateState
import com.znarfm.siphon.ui.theme.ExpressiveShapeTokens

@Composable
fun AppUpdateDialogHost(
    updateState: UpdateState,
    startupPromptDismissed: Boolean,
    isAboutScreen: Boolean,
    onDismissPrompt: () -> Unit,
    onDownloadAndVerify: () -> Unit,
    onInstallReadyApk: (Context) -> Boolean
) {
    if (startupPromptDismissed || isAboutScreen) return

    val context = LocalContext.current

    when (updateState) {
        is UpdateState.UpdateAvailable -> {
            val release = updateState.release
            val shaPreview = release.expectedSha256?.take(16)?.plus("...") ?: "Missing"
            AlertDialog(
                onDismissRequest = onDismissPrompt,
                title = { Text("Update Available (${release.tagName})") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (release.releaseNotes.isNotBlank()) {
                            Text(
                                text = release.releaseNotes.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "SHA-256: $shaPreview",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    FilledTonalButton(
                        onClick = onDownloadAndVerify,
                        shape = ExpressiveShapeTokens.PillShape
                    ) {
                        Text("Download & Verify")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = onDismissPrompt,
                        shape = ExpressiveShapeTokens.PillShape
                    ) {
                        Text("Later")
                    }
                }
            )
        }

        is UpdateState.Downloading -> {
            val progressFraction = (updateState.progressPercent / 100f).coerceIn(0f, 1f)
            AlertDialog(
                onDismissRequest = onDismissPrompt,
                title = { Text("Downloading ${updateState.release.tagName}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${updateState.progressPercent}% downloaded...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = onDismissPrompt,
                        shape = ExpressiveShapeTokens.PillShape
                    ) {
                        Text("Hide")
                    }
                }
            )
        }

        is UpdateState.Verifying -> {
            AlertDialog(
                onDismissRequest = onDismissPrompt,
                title = { Text("Verifying ${updateState.release.tagName}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = "Verifying SHA-256 checksum...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = onDismissPrompt,
                        shape = ExpressiveShapeTokens.PillShape
                    ) {
                        Text("Hide")
                    }
                }
            )
        }

        is UpdateState.ReadyToInstall -> {
            AlertDialog(
                onDismissRequest = onDismissPrompt,
                title = { Text("Update Ready (${updateState.release.tagName})") },
                text = {
                    Text(
                        text = "SHA-256 verified: ${updateState.verifiedSha256.take(16)}...",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                },
                confirmButton = {
                    FilledTonalButton(
                        onClick = {
                            if (!onInstallReadyApk(context)) {
                                Toast.makeText(
                                    context,
                                    "Allow installing unknown apps for Siphon, then tap Install again",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        shape = ExpressiveShapeTokens.PillShape
                    ) {
                        Text("Install")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = onDismissPrompt,
                        shape = ExpressiveShapeTokens.PillShape
                    ) {
                        Text("Later")
                    }
                }
            )
        }

        else -> Unit
    }
}
