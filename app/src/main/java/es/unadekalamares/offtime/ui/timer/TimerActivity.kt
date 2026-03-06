package es.unadekalamares.offtime.ui.timer

import android.Manifest
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    sealed class RunningTimer {
        class TopTimer: RunningTimer()
        class BottomTimer: RunningTimer()
    }

    private val viewModel: TimerActivityViewModel by inject()
    private val permissionsManager: PermissionsManager by inject()
    private lateinit var timerServiceBinder: TimerService.LocalBinder

    private var willTopTimerRun: Boolean = false
    private var runningTimer: MutableStateFlow<RunningTimer?> = MutableStateFlow(null)
    private var topTimerState: MutableStateFlow<TimerUIState> = MutableStateFlow(TimerUIState.Stopped)
    private var bottomTimerState: MutableStateFlow<TimerUIState> = MutableStateFlow(TimerUIState.Stopped)
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
                startTimer(willTopTimerRun)
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
                            startTimer(willTopTimerRun)
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
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimerUI(
                    isTopTimer = true,
                    state = topTimerState.collectAsState().value,
                    modifier = Modifier.weight(1f),
                    value = timer.value.topTimer,
                    onClick = { onTopTimerClick(true) }
                )
                ControlsUI(
                    isEnabled = runningTimer.collectAsState().value != null || isPaused.collectAsState().value,
                    isPaused = isPaused.collectAsState().value,
                    onButtonClick = {
                        if (isPaused.value) {
                            isPaused.value = false
                            stopTimer()
                        } else {
                            if (this@TimerActivity::timerServiceBinder.isInitialized) {
                                pauseTimers()
                            }
                        }
                    })
                TimerUI(
                    isTopTimer = false,
                    state = bottomTimerState.collectAsState().value,
                    modifier = Modifier.weight(1f),
                    value = timer.value.bottomTimer,
                    onClick = { onBottomTimerClick(false) }
                )
            }
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
        this.willTopTimerRun = isTopTimer
        setRunningTimer(isTopTimer)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lifecycleScope.launch {
                permissionsManager.checkPermission(this@TimerActivity)
            }
        } else {
            startTimer(isTopTimer)
        }
    }

    private fun setRunningTimer(isTopTimer: Boolean) {
        if (isTopTimer) {
            runningTimer.value = RunningTimer.TopTimer()
            topTimerState.value = TimerUIState.Running
            bottomTimerState.value = TimerUIState.Stopped
        } else {
            runningTimer.value = RunningTimer.BottomTimer()
            topTimerState.value = TimerUIState.Stopped
            bottomTimerState.value = TimerUIState.Running
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
        if (this::timerServiceBinder.isInitialized) {
            isPaused.value = false
            timerServiceBinder.setIsPaused(false)
        }
    }

    private fun getServiceIntent(): Intent =
        Intent(this, TimerService::class.java)

    private fun stopTimer() {
        runningTimer.value = null
        topTimerState.value = TimerUIState.Stopped
        bottomTimerState.value = TimerUIState.Stopped
        timerServiceBinder.stopService()
        unbindService(serviceConnection)
        viewModel.resetTimers()
    }

    private fun pauseTimers() {
        isPaused.value = true
        runningTimer.value = null
        timerServiceBinder.setIsPaused(true)
        topTimerState.value = TimerUIState.Stopped
        bottomTimerState.value = TimerUIState.Stopped
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            timerServiceBinder = service as TimerService.LocalBinder
            viewModel.listenToServiceChannel(timerServiceBinder.getTimerChannel(), this@TimerActivity)
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }

    }

    override fun onDestroy() {
        stopTimer()
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