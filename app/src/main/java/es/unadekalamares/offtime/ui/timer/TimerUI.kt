package es.unadekalamares.offtime.ui.timer

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimerUI(
    isEnabled: Boolean ,
    modifier: Modifier,
    value: String,
    onClick: () -> Unit
) {
    val animatedPaddingDp: Dp by animateDpAsState(if (isEnabled) 0.dp else 4.dp)

    Box(
        modifier = modifier,
    ) {
        Card(
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp + animatedPaddingDp,
                    top = 8.dp + animatedPaddingDp,
                    end = 16.dp + animatedPaddingDp,
                    bottom = 8.dp + animatedPaddingDp
                ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = if (isEnabled) 8.dp else 0.dp
            ),
            onClick = onClick
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    fontSize = 32.sp,
                    text = value,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}