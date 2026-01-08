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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow

class OverlayGraphService : Service() {
    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val overlayScale = MutableStateFlow(1f)
    private val scope = MainScope()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> showOverlay()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        removeOverlay()
        super.onDestroy()
    }

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        if (overlayView != null) return

        val composeView = ComposeView(this)
        composeView.setContent {
            NetConditionerTheme {
                val scale by overlayScale.collectAsState()
                GraphCanvasCompose(scale = scale)
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
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 160
        }

        layoutParams = params
        overlayView = composeView
        windowManager.addView(composeView, params)
        attachDragListener(composeView, params)
        composeView.setOnLongClickListener {
            overlayScale.value = if (overlayScale.value < 1.2f) 1.4f else 1f
            true
        }
    }

    private fun removeOverlay() {
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        layoutParams = null
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
                else -> false
            }
        }
    }

    companion object {
        private const val ACTION_START = "com.netconditioner.vpn.action.GRAPH_START"
        private const val ACTION_STOP = "com.netconditioner.vpn.action.GRAPH_STOP"

        fun createStartIntent(context: Context): Intent {
            return Intent(context, OverlayGraphService::class.java).apply {
                action = ACTION_START
            }
        }

        fun createStopIntent(context: Context): Intent {
            return Intent(context, OverlayGraphService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}
