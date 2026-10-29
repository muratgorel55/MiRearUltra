package com.mirearultra.pro.core

import android.app.Presentation
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.mirearultra.pro.rear.NativeHyperOsRearView

/**
 * Direct Rear Display Hardware Window & Presentation Engine for Xiaomi 17 & 18 Series (HyperOS 3/4).
 * Uses [NativeHyperOsRearView] directly (zero ComposeView lifecycle overhead on Display #1).
 */
object RearOverlayController {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeOverlayView: View? = null
    private var activeOverlayWindowManager: WindowManager? = null
    private var activeOverlayDisplayId: Int = -1
    private var activePresentation: RearHardwarePresentation? = null
    private var rearWakeLock: PowerManager.WakeLock? = null

    fun findHardwareRearDisplay(context: Context): Display? {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val candidates = LinkedHashMap<Int, Display>()

        runCatching {
            dm.displays.forEach { d ->
                if (d.displayId != Display.DEFAULT_DISPLAY) candidates[d.displayId] = d
            }
        }
        runCatching {
            dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).forEach { d ->
                if (d.displayId != Display.DEFAULT_DISPLAY) candidates[d.displayId] = d
            }
        }
        runCatching {
            dm.getDisplays("android.hardware.display.category.ALL_INCLUDING_DISABLED").forEach { d ->
                if (d.displayId != Display.DEFAULT_DISPLAY) candidates[d.displayId] = d
            }
        }
        for (id in 1..8) {
            runCatching {
                val d = dm.getDisplay(id)
                if (d != null && d.displayId != Display.DEFAULT_DISPLAY) {
                    candidates[d.displayId] = d
                }
            }
        }

        val preferredId = RearStateRepository.settings.value.preferredRearDisplayId
        if (preferredId > 0 && candidates.containsKey(preferredId)) {
            return candidates[preferredId]
        }

        return candidates.values.firstOrNull { d ->
            val n = d.name.lowercase()
            !n.contains("wifi") && !n.contains("cast") && !n.contains("overlay")
        } ?: candidates.values.firstOrNull()
    }

    fun showOnRearDisplay(context: Context): Boolean {
        val appContext = context.applicationContext
        val rearDisplay = findHardwareRearDisplay(appContext) ?: return false

        mainHandler.post {
            wakeRearDisplayHardware(appContext, rearDisplay)

            if (Settings.canDrawOverlays(appContext)) {
                val attached = attachDirectRearOverlay(appContext, rearDisplay)
                if (attached) {
                    return@post
                }
            }

            showRearPresentation(appContext, rearDisplay)
        }
        return true
    }

    @Suppress("DEPRECATION")
    private fun wakeRearDisplayHardware(context: Context, rearDisplay: Display) {
        runCatching {
            val displayContext = context.createDisplayContext(rearDisplay)
            val pm = displayContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (rearWakeLock?.isHeld == true) {
                rearWakeLock?.release()
            }
            val wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
                "MiRearUltra:RearDisplayWakeLock"
            )
            wl.acquire(10_000L)
            rearWakeLock = wl
        }
    }

    private fun attachDirectRearOverlay(appContext: Context, rearDisplay: Display): Boolean {
        return try {
            if (activeOverlayView != null && activeOverlayDisplayId == rearDisplay.displayId) {
                activeOverlayView?.invalidate()
                return true
            }
            dismissOverlayInternal()

            val displayContext = appContext.createDisplayContext(rearDisplay)
            val windowContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                runCatching {
                    displayContext.createWindowContext(
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        null
                    )
                }.getOrDefault(displayContext)
            } else {
                displayContext
            }

            val wm = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val nativeView = NativeHyperOsRearView(windowContext, isPreviewOnFront = false)

            @Suppress("DEPRECATION")
            val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags,
                PixelFormat.OPAQUE
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                title = "MiRearUltraRearOverlay"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
            }

            wm.addView(nativeView, params)
            activeOverlayView = nativeView
            activeOverlayWindowManager = wm
            activeOverlayDisplayId = rearDisplay.displayId
            true
        } catch (e: Throwable) {
            false
        }
    }

    private fun showRearPresentation(appContext: Context, rearDisplay: Display) {
        try {
            if (activePresentation?.isShowing == true && activePresentation?.display?.displayId == rearDisplay.displayId) {
                return
            }
            activePresentation?.dismiss()
            val presentation = RearHardwarePresentation(appContext, rearDisplay)
            presentation.show()
            activePresentation = presentation
        } catch (_: Throwable) {
        }
    }

    fun dismissRearOverlay() {
        mainHandler.post {
            dismissOverlayInternal()
        }
    }

    private fun dismissOverlayInternal() {
        runCatching {
            val view = activeOverlayView
            val wm = activeOverlayWindowManager
            if (view != null && wm != null) {
                wm.removeViewImmediate(view)
            }
        }
        activeOverlayView = null
        activeOverlayWindowManager = null
        activeOverlayDisplayId = -1
        runCatching { activePresentation?.dismiss() }
        activePresentation = null
    }
}

private class RearHardwarePresentation(
    outerContext: Context,
    display: Display
) : Presentation(outerContext, display) {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window?.apply {
            addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
            if (Settings.canDrawOverlays(context)) {
                runCatching {
                    setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                }
            }
        }

        val nativeView = NativeHyperOsRearView(context, isPreviewOnFront = false)
        setContentView(nativeView)
    }
}
