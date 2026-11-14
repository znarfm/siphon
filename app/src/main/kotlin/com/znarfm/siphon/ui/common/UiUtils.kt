package com.znarfm.siphon.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import com.znarfm.siphon.ui.theme.AppColorSeed
import com.znarfm.siphon.ui.theme.SiphonIcons

fun copyToClipboard(
    context: Context,
    label: String,
    text: String,
    toastMessage: String? = null
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    if (toastMessage != null) {
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }
}

fun shareText(
    context: Context,
    text: String,
    chooserTitle: String = "Share"
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

@Composable
fun IncognitoThemeOverlay(
    isIncognito: Boolean,
    content: @Composable () -> Unit
) {
    if (isIncognito) {
        MaterialTheme(
            colorScheme = remember { darkColorScheme() },
            content = content
        )
    } else {
        content()
    }
}

@Composable
fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = SiphonIcons.DeleteSweep,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Clear browsing history?") },
        text = {
            Text("This will permanently delete all visited pages from your browsing history.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun AppColorSeed.effectiveForDevice(): AppColorSeed =
    if (this == AppColorSeed.DYNAMIC && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        AppColorSeed.SAPPHIRE
    } else {
        this
    }
