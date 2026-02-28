package es.unadekalamares.offtime.ui.timer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.getString
import es.unadekalamares.offtime.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    confirmButtonText: String,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(getString(context, R.string.settings_dialog_title)) },
            text = { Text(getString(context, R.string.settings_dialog_text)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirm
                ) {
                    Text(confirmButtonText)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(getString(context, R.string.settings_dialog_dismiss_button))
                }
            }
        )
    }
}