package es.unadekalamares.offtime.ui.timer

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.unadekalamares.offtime.ui.model.TimerStatus
import es.unadekalamares.offtime.ui.theme.FireOrange10
import es.unadekalamares.offtime.ui.theme.FireOrange30
import es.unadekalamares.offtime.ui.theme.FireOrange80

@Composable
fun TimerUI(
    isTopTimer: Boolean,
    state: TimerStatus,
    modifier: Modifier,
    value: String,
    onClick: () -> Unit
) {

    val transition = updateTransition(state, label = "timerTransition")

    val backgroundColor3 by transition.animateColor(label = "color3") { state ->
        when (state) {
            TimerStatus.Running -> FireOrange10
            else -> Transparent
        }
    }

    val backgroundColor2 by transition.animateColor(label = "color2") { state ->
        when (state) {
            TimerStatus.Running -> FireOrange30
            else -> Transparent
        }
    }

    val backgroundColor1 by transition.animateColor(label = "color1" ) { state ->
        when (state) {
            TimerStatus.Running -> FireOrange80
            else -> Transparent
        }
    }

    val backgroundGradient = listOf(backgroundColor1, backgroundColor2, backgroundColor3)

    val textColor by transition.animateColor(label = "textColor") { state ->
        when (state) {
            TimerStatus.Running -> White
            else -> MaterialTheme.colorScheme.onSurface
        }
    }

    val paddingDp by transition.animateDp(label = "padding") { state ->
        when (state) {
            TimerStatus.Running -> 8.dp
            else -> 0.dp
        }
    }

    val elevationDp by transition.animateDp(label = "defaultElevation") { state ->
        when (state) {
            TimerStatus.Running -> 0.dp
            else -> 8.dp
        }
    }

    Box(
        modifier = modifier,
    ) {
        Card(
            enabled = state != TimerStatus.Running,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp + paddingDp,
                    top = 8.dp + paddingDp,
                    end = 16.dp + paddingDp,
                    bottom = 8.dp + paddingDp
                ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = elevationDp
            ),
            onClick = onClick
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isTopTimer) {
                                backgroundGradient
                            } else {
                                backgroundGradient.reversed()
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    fontSize = 32.sp,
                    text = value,
                    color = textColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}