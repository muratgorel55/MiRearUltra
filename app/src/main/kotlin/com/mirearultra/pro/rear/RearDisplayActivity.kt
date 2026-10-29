package com.mirearultra.pro.rear

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.Display
import android.view.WindowManager
import android.widget.FrameLayout
import com.mirearultra.pro.core.RearDisplayEngine
import com.mirearultra.pro.core.RearScreenMode
import com.mirearultra.pro.core.RearStateRepository

/**
 * Xiaomi 17 Pro Max İkinci Arka Ekranı (Display #1, 976x596) için
 * Saf Native Android Activity.
 *
 * KESİN GÜVENLİK İLKESİ:
 * Bu aktivite ASLA ve HİÇBİR ZAMAN ön ana ekranda (Display #0) çalışamaz.
 * Eğer Android veya HyperOS yanlışlıkla bu aktiviteyi Display #0'da başlatmaya kalkarsa,
 * aktivite 0 milisaniyede kendini anında `finishAndRemoveTask()` ile yok eder ve
 * kullanıcıyı asla engellemez, ana ekranı kilitlemez.
 */
class RearDisplayActivity : Activity() {

    private var isOnRearDisplay = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var displayManager: DisplayManager? = null
    private var rearView: NativeHyperOsRearView? = null

    private val redrawRunnable = Runnable {
        if (!isDestroyed && !isFinishing) {
            rearView?.invalidate()
            window?.decorView?.invalidate()
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (isOnRearDisplay && displayId == resolveCurrentDisplayId()) {
                pushFrameToRearPanel()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. KESİN GÜVENLİK KONTROLÜ: Ön ekrandaysa (Display #0) ANINDA YOK ET!
        val currentDisplayId = resolveCurrentDisplayId()
        if (currentDisplayId == Display.DEFAULT_DISPLAY || currentDisplayId <= 0) {
            finishAndRemoveTask()
            return
        }

        // Yalnızca doğrulanmış fiziksel arka ekrandayken (Display #1) başlatılır
        isOnRearDisplay = true
        visibleOnRear = true

        configureLockAndWakeFlags()
        RearDisplayEngine.init(this)
        handleModeIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { setTranslucent(false) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        val view = NativeHyperOsRearView(this, isPreviewOnFront = false)
        rearView = view
        root.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        displayManager?.registerDisplayListener(displayListener, mainHandler)

        RearDisplayEngine.onHostShownOnRear(this, currentDisplayId)
        pushFrameToRearPanel()
    }

    override fun onStart() {
        super.onStart()
        if (isOnRearDisplay) {
            visibleOnRear = true
            rearView?.invalidate()
            pushFrameToRearPanel()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isOnRearDisplay) {
            visibleOnRear = true
            rearView?.invalidate()
            pushFrameToRearPanel()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val displayId = resolveCurrentDisplayId()
        if (displayId == Display.DEFAULT_DISPLAY || displayId <= 0) {
            finishAndRemoveTask()
            return
        }

        isOnRearDisplay = true
        visibleOnRear = true
        RearStateRepository.onUserTouchInteraction()
        handleModeIntent(intent)
        RearDisplayEngine.onHostShownOnRear(this, displayId)
        rearView?.invalidate()
        pushFrameToRearPanel()
    }

    override fun onStop() {
        if (isOnRearDisplay) {
            visibleOnRear = false
        }
        super.onStop()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(redrawRunnable)
        runCatching { displayManager?.unregisterDisplayListener(displayListener) }
        if (isOnRearDisplay) {
            visibleOnRear = false
        }
        super.onDestroy()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        RearStateRepository.onUserTouchInteraction()
    }

    private fun resolveCurrentDisplayId(): Int {
        val d = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { display }.getOrNull()
        } else {
            @Suppress("DEPRECATION")
            runCatching { windowManager.defaultDisplay }.getOrNull()
        }
        return d?.displayId ?: Display.DEFAULT_DISPLAY
    }

    @Suppress("DEPRECATION")
    private fun pushFrameToRearPanel() {
        runCatching {
            val pm = getSystemService(PowerManager::class.java)
            pm?.newWakeLock(DRAW_WAKE_LOCK, "MiRearUltra:RearDrawLock")?.acquire(350L)
        }
        mainHandler.removeCallbacks(redrawRunnable)
        mainHandler.postDelayed(redrawRunnable, 50L)
    }

    private fun handleModeIntent(intent: Intent?) {
        val modeName = intent?.getStringExtra(RearDisplayEngine.EXTRA_MODE) ?: return
        runCatching {
            val mode = RearScreenMode.valueOf(modeName)
            RearStateRepository.setMode(mode)
        }
    }

    private fun configureLockAndWakeFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
    }

    companion object {
        private const val DRAW_WAKE_LOCK = 0x80

        @Volatile
        var visibleOnRear: Boolean = false
            private set

        fun notifyMovedToRear(token: String?) = Unit
    }
}
