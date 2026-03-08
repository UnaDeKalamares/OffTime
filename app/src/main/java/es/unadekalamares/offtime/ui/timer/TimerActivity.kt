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
import es.unadekalamares.offtime.data.service.TimerService
import es.unadekalamares.offtime.domain.permissions.PermissionStatus
import es.unadekalamares.offtime.domain.permissions.PermissionsManager
import es.unadekalamares.offtime.ui.model.RunningTimer
import es.unadekalamares.offtime.ui.theme.OffTimeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class TimerActivity : ComponentActivity() {

    private val viewModel: TimerActivityViewModel by inject()
    private val permissionsManager: PermissionsManager by inject()

    private var requestedRunningTimer: RunningTimer = RunningTimer.None

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
                startTimer()
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
                            startTimer()
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
        onTopTimerClick: (RunningTimer) -> Unit,
        onBottomTimerClick: (RunningTimer) -> Unit
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
                    state = timer.value.topTimer.status,
                    modifier = Modifier.weight(1f),
                    value = timer.value.topTimer.timer,
                    onClick = { onTopTimerClick(RunningTimer.TopTimer) }
                )
                ControlsUI(
                    isEnabled = timer.value.isTopRunning() || timer.value.isBottomRunning() || timer.value.areAllPaused(),
                    isPaused = timer.value.areAllPaused(),
                    onButtonClick = {
                        if (timer.value.areAllPaused()) {
                            stopTimer()
                        } else {
                            pauseTimers()
                        }
                    })
                TimerUI(
                    isTopTimer = false,
                    state = timer.value.bottomTimer.status,
                    modifier = Modifier.weight(1f),
                    value = timer.value.bottomTimer.timer,
                    onClick = { onBottomTimerClick(RunningTimer.BottomTimer) }
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

    private fun tryStartTimer(runningTimer: RunningTimer) {
        requestedRunningTimer = runningTimer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lifecycleScope.launch {
                permissionsManager.checkPermission(this@TimerActivity)
            }
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        val serviceIntent = getServiceIntent().also {
            bindService(it, serviceConnection, 0)
        }
        serviceIntent.putExtra(TimerService.RUNNING_TIMER, requestedRunningTimer)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        viewModel.notifyTimerStarted(requestedRunningTimer)
    }

    private fun getServiceIntent(): Intent =
        Intent(this, TimerService::class.java)

    private fun stopTimer() {
        requestedRunningTimer = RunningTimer.None
        viewModel.stopTimers()
        unbindService(serviceConnection)
    }

    private fun pauseTimers() {
        viewModel.pauseTimers(this@TimerActivity)
        requestedRunningTimer = RunningTimer.None
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            viewModel.initServiceBinder(
                service as TimerService.LocalBinder,
                this@TimerActivity
            )
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