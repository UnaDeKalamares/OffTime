package es.unadekalamares.offtime.ui.timer

import android.Manifest
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import es.unadekalamares.offtime.R
import es.unadekalamares.offtime.permissions.PermissionStatus
import es.unadekalamares.offtime.permissions.PermissionsManager
import es.unadekalamares.offtime.service.TimerService
import es.unadekalamares.offtime.service.TimerService.Companion.IS_TOP_ARG
import es.unadekalamares.offtime.ui.theme.OffTimeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class TimerActivity : ComponentActivity() {

    private val viewModel: TimerActivityViewModel by inject()
    private val permissionsManager: PermissionsManager by inject()
    private lateinit var timerService: TimerService

    private var isTopTimer: Boolean = false
    private var arePermissionsDenied: Boolean = false
    private var didShowRationale: Boolean = false
    private var showSettingsDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private lateinit var activityPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupActivityLauncher()
        setupCollector()
        setupUI()
    }

    private fun setupActivityLauncher() {
        activityPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isPermissionGranted ->
            if (isPermissionGranted) {
                startService(isTopTimer)
            } else if (didShowRationale) {
                Log.i("PermissionManager", "DENIED AFTER RATIONALE")
                arePermissionsDenied = true
                lifecycleScope.launch {
                    permissionsManager.setPermissionAsDenied(this@TimerActivity)
                }
            }
        }
    }

    private fun setupCollector() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                permissionsManager.permissionStateFlow.collect { permissionStatus ->
                    when (permissionStatus) {
                        is PermissionStatus.Granted -> {
                            Log.i("PermissionManager", "GRANTED")
                            startService(isTopTimer)
                        }
                        is PermissionStatus.Denied -> {
                            Log.i("PermissionManager", "DENIED")
                            showSettingsDialog.value = true
                        }
                        is PermissionStatus.Request -> {
                            Log.i("PermissionManager", "REQUEST")
                            launchPermissionRequest()
                        }
                        is PermissionStatus.RequestRationale -> {
                            Log.i("PermissionManager", "REQUEST_RATIONALE")
                            didShowRationale = true
                            showSettingsDialog.value = true
                        }
                        else -> {
                            Log.i("PermissionManager", "PENDING")
                        }
                    }
                }
            }
        }
    }

    private fun setupUI() {
        enableEdgeToEdge()
        setContent {
            OffTimeTheme {
                SetupSettingsDialog()
                TimerContent(
                    viewModel = viewModel,
                    onTopTimerClick = this::tryStartService,
                    onBottomTimerClick = this::tryStartService
                )
            }
        }
    }

    @Composable
    private fun SetupSettingsDialog() {
        SettingsDialog(
            isVisible = showSettingsDialog.collectAsState().value,
            onDismiss = {
                showSettingsDialog.value = false
            },
            confirmButtonText = if (arePermissionsDenied) {
                getString(R.string.settings_dialog_confirm_button)
            } else {
                getString(R.string.settings_dialog_prompt_button)
            },
            onConfirm = {
                if (arePermissionsDenied) {
                    openSettings()
                    showSettingsDialog.value = false
                } else {
                    launchPermissionRequest()
                    showSettingsDialog.value = false
                }
            }
        )
    }

    private fun launchPermissionRequest() {
        activityPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openSettings() {
        permissionsManager.resetStateFlow()
        val packageName = this@TimerActivity.packageName
        val uri = Uri.fromParts("package", packageName, null)
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = uri
        }
        startActivity(intent)
    }

    private fun tryStartService(isTopTimer: Boolean) {
        this.isTopTimer = isTopTimer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lifecycleScope.launch {
                permissionsManager.checkPermission(this@TimerActivity)
            }
        } else {
            startService(isTopTimer)
        }
    }

    private fun startService(isTopTimer: Boolean) {
        viewModel.setTopTimerRunning(isTopTimer)
        val serviceIntent = Intent(this, TimerService::class.java).also {
            bindService(it, serviceConnection, 0)
        }
        serviceIntent.putExtra(IS_TOP_ARG, isTopTimer)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            viewModel.listenToService(timerService, this@TimerActivity)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            TODO("Not yet implemented")
        }

    }

    override fun onDestroy() {
        timerService.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        unbindService(serviceConnection)
        super.onDestroy()
    }
}

@Composable
fun TimerContent(
    viewModel: TimerActivityViewModel,
    onTopTimerClick: (Boolean) -> Unit,
    onBottomTimerClick: (Boolean) -> Unit
) {
    val timer = viewModel.timerUIState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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
            ),
            onClick = { onTopTimerClick(true) }
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
            ),
            onClick = { onBottomTimerClick(false) }
        )
    }
}

@Composable
fun Timer(
    modifier: Modifier,
    value: String,
    cardPadding: PaddingValues,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier,
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    cardPadding
                ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 8.dp
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
/*
@Preview(showBackground = true)
@Composable
fun TimerPreview() {
    TimerContent()
}
*/