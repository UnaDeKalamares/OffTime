package es.unadekalamares.offtime.ui.timer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun ControlsUI(
    isPaused: Boolean,
    onButtonClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            elevation = ButtonDefaults.elevatedButtonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp
            ),
            onClick = onButtonClick) {
            Icon(
                painter = if (isPaused) {
                    painterResource(android.R.drawable.ic_menu_revert)
                } else {
                    painterResource(android.R.drawable.ic_media_pause)
                },
                contentDescription = if (isPaused) {
                    "Reset button"
                } else {
                    "Pause button"
                }
            )
        }
    }
}