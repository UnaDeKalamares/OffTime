package es.unadekalamares.offtime.ui.timer

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import es.unadekalamares.offtime.R
import es.unadekalamares.offtime.data.service.TimerService
import es.unadekalamares.offtime.domain.permissions.PermissionStatus
import es.unadekalamares.offtime.domain.permissions.PermissionsManager
import es.unadekalamares.offtime.ui.model.RunningTimer
import es.unadekalamares.offtime.ui.model.TimerUIState
import es.unadekalamares.offtime.ui.theme.OffTimeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class TimerActivity : ComponentActivity() {

    private val viewModel: TimerActivityViewModel by viewModel()
    private val permissionsManager: PermissionsManager by inject()

    private var requestedRunningTimer: RunningTimer = RunningTimer.None

    private var arePermissionsDenied: Boolean = false
    private var didShowRationale: Boolean = false
    private var showSettingsDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var isServiceBound: Boolean = false

    private lateinit var activityPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupActivityLauncher()
        setupCollector()
        setupUI()
    }

    override fun onStart() {
        super.onStart()
        getServiceIntent().also {
            attemptServiceBinding(it)
        }
    }

    override fun onStop() {
        super.onStop()
        attemptServiceUnbinding()
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
        val configuration = LocalConfiguration.current
        val timer = viewModel.timerUIState.collectAsState()
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            when (configuration.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DrawUI(
                            configuration = configuration,
                            timer = timer.value,
                            topModifier = Modifier.weight(1f),
                            onTopTimerClick = onTopTimerClick,
                            bottomModifier = Modifier.weight(1f),
                            onBottomTimerClick = onBottomTimerClick
                        )
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrawUI(
                            configuration = configuration,
                            timer = timer.value,
                            topModifier = Modifier.weight(1f),
                            onTopTimerClick = onTopTimerClick,
                            bottomModifier = Modifier.weight(1f),
                            onBottomTimerClick = onBottomTimerClick
                        )
                    }
                }
            }
        }

    }

    @Composable
    private fun DrawUI(
        timer: TimerUIState,
        configuration: Configuration,
        @SuppressLint("ModifierParameter") topModifier: Modifier,
        onTopTimerClick: (RunningTimer) -> Unit,
        bottomModifier: Modifier,
        onBottomTimerClick: (RunningTimer) -> Unit
    ) {
        TimerUI(
            configuration = configuration,
            isTopTimer = true,
            state = timer.topTimer.status,
            modifier = topModifier,
            value = timer.topTimer.timer,
            onClick = { onTopTimerClick(RunningTimer.TopTimer) }
        )
        ControlsUI(
            configuration = configuration,
            isEnabled = timer.isTopRunning() || timer.isBottomRunning() || timer.areAllPaused(),
            isPaused = timer.areAllPaused(),
            onButtonClick = {
                if (timer.areAllPaused()) {
                    stopTimer()
                } else {
                    pauseTimers()
                }
            })
        TimerUI(
            configuration = configuration,
            isTopTimer = false,
            state = timer.bottomTimer.status,
            modifier = bottomModifier,
            value = timer.bottomTimer.timer,
            onClick = { onBottomTimerClick(RunningTimer.BottomTimer) }
        )
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
            attemptServiceBinding(it)
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

    private fun attemptServiceBinding(serviceIntent: Intent) {
        isServiceBound = true
        bindService(serviceIntent, serviceConnection, 0)
    }

    private fun stopTimer() {
        requestedRunningTimer = RunningTimer.None
        viewModel.stopTimers()
        attemptServiceUnbinding()
    }

    private fun attemptServiceUnbinding() {
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
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

}
/*
@Preview(showBackground = true)
@Composable
fun TimerPreview() {
    TimerContent()
}
*/