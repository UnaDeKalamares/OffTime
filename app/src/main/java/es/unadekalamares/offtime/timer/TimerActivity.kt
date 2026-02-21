package es.unadekalamares.offtime.timer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.unadekalamares.offtime.ui.theme.OffTimeTheme
import org.koin.compose.viewmodel.koinViewModel

class TimerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OffTimeTheme {
                TimerContent()
            }
        }
    }
}

@Composable
fun TimerContent() {
    val viewModel = koinViewModel<TimerActivityViewModel>()
    val timer = viewModel.timerUIState.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Timer(
            modifier = Modifier.weight(1f),
            value = timer.value.topTimer,
            cardPadding = PaddingValues(
                start = 16.dp,
                top = 32.dp,
                end = 16.dp,
                bottom = 8.dp
            )
        )
        Controls()
        Timer(
            modifier = Modifier.weight(1f),
            value = timer.value.bottomTimer,
            cardPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = 32.dp
            )
        )
    }
}

@Composable
fun Timer(
    modifier: Modifier,
    value: String,
    cardPadding: PaddingValues
) {
    Box(
        modifier = modifier,
    ) {
        Card(
            modifier = Modifier.fillMaxSize().padding(
                cardPadding
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 8.dp
            )
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

@Composable
fun Controls() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            elevation = ButtonDefaults.elevatedButtonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp
            ),
            onClick = {}) {
            Icon(
                painter = painterResource(android.R.drawable.ic_media_pause),
                contentDescription = "Pause button"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimerPreview() {
    TimerContent()
}