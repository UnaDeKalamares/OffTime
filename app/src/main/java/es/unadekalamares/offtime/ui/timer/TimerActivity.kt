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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.flow.collectLatest
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
    private var isPaused: MutableStateFlow<Boolean> = MutableStateFlow(false)

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
                startTimer(isTopTimer)
            } else if (didShowRationale) {
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
                permissionsManager.permissionSharedFlow.collectLatest { permissionStatus ->
                    when (permissionStatus) {
                        is PermissionStatus.Granted -> {
                            startTimer(isTopTimer)
                        }

                        is PermissionStatus.Denied -> {
                            showSettingsDialog.value = true
                        }

                        is PermissionStatus.Request -> {
                            launchPermissionRequest()
                        }

                        is PermissionStatus.RequestRationale -> {
                            didShowRationale = true
                            showSettingsDialog.value = true
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
                SetupTimerContent(
                    viewModel = viewModel,
                    onTopTimerClick = this::tryStartTimer,
                    onBottomTimerClick = this::tryStartTimer
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

    @Composable
    fun SetupTimerContent(
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
            TimerUI(
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
            ControlsUI(
                isPaused = isPaused.collectAsState().value,
                onButtonClick = {
                    if (isPaused.value) {
                        isPaused.value = false
                        stopTimer()
                    } else {
                        isPaused.value = true
                        if (this@TimerActivity::timerService.isInitialized) {
                            timerService.isPaused = true
                        }
                    }
                })
            TimerUI(
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

    private fun launchPermissionRequest() {
        activityPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openSettings() {
        val packageName = this@TimerActivity.packageName
        val uri = Uri.fromParts("package", packageName, null)
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = uri
        }
        startActivity(intent)
    }

    private fun tryStartTimer(isTopTimer: Boolean) {
        this.isTopTimer = isTopTimer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lifecycleScope.launch {
                permissionsManager.checkPermission(this@TimerActivity)
            }
        } else {
            startTimer(isTopTimer)
        }
    }

    private fun startTimer(isTopTimer: Boolean) {
        viewModel.setTopTimerRunning(isTopTimer)
        val serviceIntent = getServiceIntent().also {
            bindService(it, serviceConnection, 0)
        }
        serviceIntent.putExtra(IS_TOP_ARG, isTopTimer)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        if (this::timerService.isInitialized) {
            isPaused.value = false
            timerService.isPaused = false
        }
    }

    private fun stopTimer() {
        val serviceIntent = getServiceIntent()
        timerService.stopService(serviceIntent)
        viewModel.resetTimers()
    }

    private fun getServiceIntent(): Intent =
        Intent(this, TimerService::class.java)

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

        }

    }

    override fun onDestroy() {
        timerService.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        unbindService(serviceConnection)
        super.onDestroy()
    }
}
/*
@Preview(showBackground = true)
@Composable
fun TimerPreview() {
    TimerContent()
}
*/