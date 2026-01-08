package com.netconditioner.vpn

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingButtonService : Service() {
    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private val overlayScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val configsFlow = MutableStateFlow(listOf(ConfigStore.defaultConfig()))
    private val floatingScaleFlow = MutableStateFlow(1.0f)
    private val menuVisible = MutableStateFlow(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> showOverlay()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        overlayScope.cancel()
        removeOverlay()
        super.onDestroy()
    }

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        if (overlayView != null) return

        val composeView = ComposeView(this)
        composeView.setContent {
            NetConditionerTheme {
                FloatingButtonContent()
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 300
        }
        layoutParams = params
        overlayView = composeView
        windowManager.addView(composeView, params)

        attachDragListener(composeView, params)
        observeDataStore()
    }

    private fun observeDataStore() {
        overlayScope.launch {
            val store = ConfigStore(applicationContext)
            launch {
                store.configsFlow.collectLatest { configsFlow.value = it }
            }
            launch {
                store.floatingScaleFlow.collectLatest { floatingScaleFlow.value = it }
            }
        }
    }

    private fun removeOverlay() {
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
    }

    private fun attachDragListener(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (kotlin.math.abs(event.rawX - initialTouchX) > 180) {
                        menuVisible.value = false
                    }
                    true
                }
                else -> false
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun FloatingButtonContent() {
        val configs by configsFlow.collectAsState()
        val scale by floatingScaleFlow.collectAsState()
        val activeConfig by VpnController.currentConfig.collectAsState()
        val running by VpnController.isRunning.collectAsState()
        val menuOpen by menuVisible.collectAsState()

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size((56 * scale).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                    .combinedClickable(
                        onClick = {
                            if (running) {
                                startService(ConditionerVpnService.createStopIntent(this@FloatingButtonService))
                            } else {
                                startService(
                                    ConditionerVpnService.createStartIntent(
                                        this@FloatingButtonService,
                                        VpnController.currentConfig.value,
                                    ),
                                )
                            }
                        },
                        onLongClick = { menuVisible.value = !menuOpen },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (running) Icons.Outlined.StopCircle else Icons.Default.Power,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Text(
                text = activeConfig.name,
                color = Color.White,
                modifier = Modifier
                    .background(Color(0x99000000), shape = CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )

            RadialMenuCompose(
                visible = menuOpen,
                configs = configs,
                activeConfigId = activeConfig.id,
            ) { config ->
                VpnController.updateConfig(config)
                startService(ConditionerVpnService.createApplyConfigIntent(this@FloatingButtonService, config))
                menuVisible.value = false
            }
        }
    }

    companion object {
        private const val ACTION_START = "com.netconditioner.vpn.action.FLOATING_START"
        private const val ACTION_STOP = "com.netconditioner.vpn.action.FLOATING_STOP"

        fun createStartIntent(context: Context): Intent {
            return Intent(context, FloatingButtonService::class.java).apply {
                action = ACTION_START
            }
        }

        fun createStopIntent(context: Context): Intent {
            return Intent(context, FloatingButtonService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}
