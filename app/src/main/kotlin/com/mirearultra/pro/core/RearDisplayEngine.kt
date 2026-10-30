package com.mirearultra.pro.core

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import com.mirearultra.pro.rear.RearDisplayActivity
import com.mirearultra.pro.service.RearNotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.concurrent.thread

enum class ShizukuConnectionState {
    /** Shizuku servisi çalışıyor ve MiRearUltra'ya tam yetki verildi. */
    READY,
    /** Shizuku çalışıyor, izin penceresi açılabilir. */
    NOT_ALLOWED,
    /** Shizuku izin penceresinde "Bir daha sorma / Reddet" seçilmiş; Shizuku uygulamasından açılmalı. */
    BLOCKED,
    /** Shizuku uygulaması yüklü ama servisi başlatılmamış. */
    NOT_RUNNING,
    /** Shizuku uygulaması telefonda yüklü değil. */
    NOT_INSTALLED
}

data class ShellExecResult(val code: Int?, val out: String, val err: String)

object RearDisplayEngine {

    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val SHIZUKU_REQ_CODE = 1701
    private const val AM = "/system/bin/am"
    const val INPUT = "/system/bin/input"
    private const val CMD = "/system/bin/cmd"
    private const val SERVICE_BIN = "/system/bin/service"
    private const val XIAOMI_REAR_PACKAGE = "com.xiaomi.subscreencenter"

    // Xiaomi 17 Pro Max exact rear panel resolution in natural landscape (976 x 596)
    private const val XIAOMI_17_PRO_MAX_LONG = 976
    private const val XIAOMI_17_PRO_MAX_SHORT = 596

    private const val PREFS_NAME = "mirear_display_engine"
    private const val KEY_REMEMBERED_REAR_ID = "remembered_rear_display_id"

    const val EXTRA_TOKEN = "token"
    const val EXTRA_MODE = "extra_mode"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val shellExecutor = Executors.newSingleThreadExecutor { Thread(it, "MiRearUltra-ShizukuShell") }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var hostToken: String? = null
        private set

    @Volatile
    var isProjectionActive = false
        private set

    @Volatile
    private var awaitingDirectLaunch = false

    @Volatile
    private var directLaunchBlockedUntil = 0L

    @Volatile
    private var appContextRef: Context? = null

    private val directLaunchTimeoutRunnable = Runnable {
        onDirectLaunchTimedOut()
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        val ready = checkShizukuStatus()
        if (ready) {
            RearStateRepository.appendLog("🟢 Shizuku servisi bağlandı ve hazır.")
            appContextRef?.let { autoGrantNotificationListenerViaShizuku(it) }
        } else {
            RearStateRepository.appendLog("🟡 Shizuku bağlandı; lütfen MiRearUltra iznini onaylayın.")
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        RearStateRepository.setShizukuReady(false)
        RearStateRepository.appendLog("⚠️ Shizuku servis bağlantısı kesildi.")
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_REQ_CODE) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                RearStateRepository.setShizukuReady(granted)
                if (granted) {
                    RearStateRepository.appendLog("✅ Shizuku tam yetkisi onaylandı! Arka ekran köprüsü aktif.")
                    appContextRef?.let { ctx ->
                        autoGrantNotificationListenerViaShizuku(ctx)
                        launchRearExperience(ctx, RearStateRepository.activeMode.value)
                    }
                } else {
                    RearStateRepository.appendLog("❌ Shizuku izni reddedildi. Shizuku uygulamasından MiRearUltra'ya izin verin.")
                }
            }
        }

    fun init(context: Context) {
        val appCtx = context.applicationContext
        appContextRef = appCtx
        RearStateRepository.init(appCtx)
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        }
        checkShizukuStatus()
        scanDisplays(appCtx)
    }

    fun readShizukuState(context: Context): ShizukuConnectionState {
        val ping = runCatching { !Shizuku.isPreV11() && Shizuku.pingBinder() }.getOrDefault(false)
        val installed = ping || isShizukuInstalled(context)
        if (!ping) {
            return if (installed) ShizukuConnectionState.NOT_RUNNING else ShizukuConnectionState.NOT_INSTALLED
        }
        return try {
            val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            val rationale = !granted && Shizuku.shouldShowRequestPermissionRationale()
            when {
                granted -> ShizukuConnectionState.READY
                rationale -> ShizukuConnectionState.BLOCKED
                else -> ShizukuConnectionState.NOT_ALLOWED
            }
        } catch (e: Throwable) {
            if (installed) ShizukuConnectionState.NOT_RUNNING else ShizukuConnectionState.NOT_INSTALLED
        }
    }

    private fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun openShizukuApp(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }
    }

    fun checkShizukuStatus(): Boolean {
        return try {
            if (Shizuku.isPreV11()) {
                RearStateRepository.setShizukuReady(false)
                false
            } else if (Shizuku.pingBinder()) {
                val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                RearStateRepository.setShizukuReady(granted)
                granted
            } else {
                RearStateRepository.setShizukuReady(false)
                false
            }
        } catch (e: Throwable) {
            RearStateRepository.setShizukuReady(false)
            false
        }
    }

    fun requestShizukuPermission(context: Context? = appContextRef) {
        try {
            val ctx = context ?: appContextRef
            if (!Shizuku.pingBinder()) {
                if (ctx != null && openShizukuApp(ctx)) {
                    RearStateRepository.appendLog("📲 Shizuku servisi çalışmıyor; başlatmanız için Shizuku uygulaması açıldı.")
                } else {
                    RearStateRepository.appendLog("⚠️ Shizuku servisi çalışmıyor. Lütfen önce Shizuku uygulamasından servisi başlatın.")
                }
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                RearStateRepository.setShizukuReady(true)
                RearStateRepository.appendLog("✅ Shizuku zaten yetkili ve hazır.")
                if (ctx != null) {
                    launchRearExperience(ctx, RearStateRepository.activeMode.value)
                }
                return
            }
            val blocked = runCatching { Shizuku.shouldShowRequestPermissionRationale() }.getOrDefault(false)
            if (blocked && ctx != null) {
                openShizukuApp(ctx)
                RearStateRepository.appendLog("⚠️ Shizuku izni daha önce reddedilmiş. Açılan Shizuku ekranında 'Yetkili Uygulamalar' listesinden MiRearUltra'yı aktif edin.")
                return
            }
            Shizuku.requestPermission(SHIZUKU_REQ_CODE)
        } catch (e: Throwable) {
            RearStateRepository.appendLog("⚠️ Shizuku izin isteği hatası: ${e.message}")
        }
    }

    /**
     * Automatically grants NotificationListenerService access via Shizuku so Xiaomi HyperOS's
     * "Restricted settings" block for sideloaded APKs does not stop call/notification routing.
     */
    fun autoGrantNotificationListenerViaShizuku(context: Context) {
        if (!checkShizukuStatus()) return
        val component = "${context.packageName}/${RearNotificationService::class.java.name}"
        shellExecutor.execute {
            runCommand(CMD, "notification", "allow_listener", component)
        }
    }

    /**
     * Deep-scans DisplayManager across all categories and IDs (1..8) and applies Xiaomi 17 Pro Max's
     * exact 976x596 rear display matcher (from RearDisplayPick).
     */
    fun scanDisplays(context: Context): List<RearDisplayHardwareInfo> {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displayMap = LinkedHashMap<Int, Display>()

        runCatching { dm.displays.forEach { displayMap[it.displayId] = it } }
        runCatching {
            dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).forEach {
                displayMap[it.displayId] = it
            }
        }
        runCatching {
            dm.getDisplays("android.hardware.display.category.ALL_INCLUDING_DISABLED").forEach {
                displayMap[it.displayId] = it
            }
        }
        for (id in 1..8) {
            runCatching {
                val d = dm.getDisplay(id)
                if (d != null) displayMap[d.displayId] = d
            }
        }

        val infoList = displayMap.values.map { display ->
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            runCatching { display.getRealMetrics(metrics) }
            val isRear = display.displayId != Display.DEFAULT_DISPLAY &&
                (display.flags and Display.FLAG_PRIVATE == 0)
            RearDisplayHardwareInfo(
                displayId = display.displayId,
                name = if (isRear) "${display.name} (Arka Ekran #${display.displayId})" else "${display.name} (Ön Ekran)",
                widthPx = metrics.widthPixels.takeIf { it > 0 } ?: XIAOMI_17_PRO_MAX_LONG,
                heightPx = metrics.heightPixels.takeIf { it > 0 } ?: XIAOMI_17_PRO_MAX_SHORT,
                refreshRateHz = display.refreshRate.toInt().takeIf { it > 0 } ?: 120,
                densityDpi = metrics.densityDpi.takeIf { it > 0 } ?: 260,
                isRearHardwareDisplay = isRear
            )
        }.toMutableList()

        // Remember detected rear display ID for when the rear display is sleeping
        val exact17ProMax = infoList.firstOrNull { d ->
            d.isRearHardwareDisplay &&
                maxOf(d.widthPx, d.heightPx) == XIAOMI_17_PRO_MAX_LONG &&
                minOf(d.widthPx, d.heightPx) == XIAOMI_17_PRO_MAX_SHORT
        } ?: infoList.firstOrNull { it.isRearHardwareDisplay }

        if (exact17ProMax != null) {
            saveRememberedRearId(context, exact17ProMax.displayId)
        } else {
            val rememberedId = getRememberedRearId(context) ?: 1
            infoList.add(
                RearDisplayHardwareInfo(
                    displayId = rememberedId,
                    name = "Xiaomi 17 Pro Max Arka Ekran (Display #$rememberedId)",
                    widthPx = XIAOMI_17_PRO_MAX_LONG,
                    heightPx = XIAOMI_17_PRO_MAX_SHORT,
                    refreshRateHz = 120,
                    densityDpi = 260,
                    isRearHardwareDisplay = true
                )
            )
        }

        RearStateRepository.updateDetectedDisplays(infoList)
        return infoList
    }

    private fun saveRememberedRearId(context: Context, id: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_REMEMBERED_REAR_ID, id)
            .apply()
    }

    private fun getRememberedRearId(context: Context): Int? {
        val v = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_REMEMBERED_REAR_ID, -1)
        return v.takeIf { it > 0 }
    }

    fun getTargetRearDisplayId(context: Context): Int {
        val preferred = RearStateRepository.settings.value.preferredRearDisplayId
        if (preferred > 0) return preferred

        val displays = scanDisplays(context)
        val secondaryList = displays.filter { it.displayId != Display.DEFAULT_DISPLAY && it.isRearHardwareDisplay }

        // 1. Exact Xiaomi 17 Pro Max 976 x 596 panel match
        val exactSized = secondaryList.firstOrNull { d ->
            maxOf(d.widthPx, d.heightPx) == XIAOMI_17_PRO_MAX_LONG &&
                minOf(d.widthPx, d.heightPx) == XIAOMI_17_PRO_MAX_SHORT
        }
        if (exactSized != null) return exactSized.displayId

        // 2. Any built-in secondary display (not Cast/WiFi)
        val nonCast = secondaryList.firstOrNull { d ->
            val lower = d.name.lowercase()
            !lower.contains("wifi") && !lower.contains("cast") && !lower.contains("overlay")
        }
        if (nonCast != null) return nonCast.displayId

        // 3. Remembered rear display ID or Display #1
        return getRememberedRearId(context) ?: 1
    }

    /**
     * Guaranteed Xiaomi 17 Pro Max (HyperOS 3) Rear Display Launch Engine:
     *
     * Stage 1: Wakes the rear display (`input -d <rearId> keyevent KEYCODE_WAKEUP`) and launches
     *          `RearDisplayActivity` directly onto `<rearId>` (enabled by `miui.rear.policy = 1` in manifest).
     * Stage 2: If the phone is locked OR HyperOS refuses direct launch / places the activity on
     *          the main screen (`Display 0`), launches on `Display 0` and immediately moves its
     *          root task to `<rearId>` via `/system/bin/am display move-stack <taskId> <rearId>`!
     * Stage 3: Once shown on `<rearId>`, checks `/system/bin/am stack list` and restarts
     *          `com.xiaomi.subscreencenter` if its launcher is underneath so Xiaomi doesn't close
     *          our rear screen when the display dims!
     */
    fun launchRearExperience(
        context: Context,
        mode: RearScreenMode,
        forceFrontPreviewIfNoShizuku: Boolean = false
    ) {
        val appContext = context.applicationContext
        appContextRef = appContext
        RearStateRepository.setMode(mode)
        RearStateRepository.onUserTouchInteraction()

        val rearDisplayId = getTargetRearDisplayId(appContext)
        // KESİN GÜVENLİK: Ön ekrana (Display #0) ASLA arka ekran aktivitesi başlatılamaz!
        if (rearDisplayId <= 0 || rearDisplayId == Display.DEFAULT_DISPLAY) {
            RearStateRepository.appendLog("⛔ Güvenlik Engeli: Geçerli bir arka ekran bulunamadı (#$rearDisplayId). Ön ekrana ASLA başlatılmaz.")
            return
        }

        val token = UUID.randomUUID().toString()
        hostToken = token
        mainHandler.removeCallbacks(directLaunchTimeoutRunnable)

        // Arka ekran zaten açıksa ve çağrı modu DEĞİLSE yeniden başlatma: sadece uyandır
        if (RearDisplayActivity.visibleOnRear && checkShizukuStatus() && mode != RearScreenMode.INCOMING_CALL) {
            shellExecutor.execute {
                runCommand(INPUT, "-d", rearDisplayId.toString(), "keyevent", "KEYCODE_WAKEUP")
            }
            return
        }

        if (!checkShizukuStatus()) {
            val state = readShizukuState(appContext)
            RearStateRepository.appendLog("⚠️ Shizuku hazır değil ($state). Lütfen Shizuku yetkisini onaylayın.")
            return
        }

        RearOverlayController.dismissRearOverlay()

        awaitingDirectLaunch = true
        shellExecutor.execute {
            // Ensure subscreen is enabled and awake, then launch directly onto Display #1
            runCommand(CMD, "settings", "put", "system", "subscreen_switch", "1")
            runCommand(INPUT, "-d", rearDisplayId.toString(), "keyevent", "KEYCODE_WAKEUP")
            val launchRes = launchHostCommand(appContext.packageName, rearDisplayId, mode.name, token)
            RearStateRepository.appendLog("🚀 Arka Ekran (#$rearDisplayId) doğrudan başlatıldı: $launchRes")

            mainHandler.postDelayed(directLaunchTimeoutRunnable, 700L)
        }
    }

    @Volatile
    private var suppressRelaunchUntil = 0L

    /**
     * Bekçi servis tarafından çağrılır: arka ekranda başka bir uygulama (ör. Kamera) açıksa dokunmaz,
     * aksi halde MiRearUltra arka ekranını geri getirir.
     */
    data class FrontTaskInfo(
        val taskId: Int,
        val packageName: String,
        val componentName: String? = null
    )

    fun getFrontDisplayTopTask(context: Context): FrontTaskInfo? {
        val actOut = readCommand("dumpsys", "activity", "activities", timeoutMs = 2500L)
        if (!actOut.isNullOrBlank()) {
            val topResumedRegex = Regex("""topResumedActivity=ActivityRecord\{[^\s]+ [^\s]+ ([^\s/]+)/([^\s]+) t(\d+)""")
            for (line in actOut.lineSequence()) {
                val m = topResumedRegex.find(line)
                if (m != null) {
                    val pkg = m.groupValues[1]
                    val act = m.groupValues[2]
                    val tid = m.groupValues[3].toIntOrNull() ?: continue
                    if (pkg != context.packageName && pkg != "com.android.systemui" && pkg != "com.miui.home") {
                        val fullComponent = if (act.startsWith(".")) "$pkg$act" else "$pkg/$act"
                        return FrontTaskInfo(tid, pkg, fullComponent)
                    }
                }
            }
            val resumedRegex = Regex("""mResumedActivity: ActivityRecord\{[^\s]+ [^\s]+ ([^\s/]+)/([^\s]+) t(\d+)""")
            for (line in actOut.lineSequence()) {
                val m = resumedRegex.find(line)
                if (m != null) {
                    val pkg = m.groupValues[1]
                    val act = m.groupValues[2]
                    val tid = m.groupValues[3].toIntOrNull() ?: continue
                    if (pkg != context.packageName && pkg != "com.android.systemui" && pkg != "com.miui.home") {
                        val fullComponent = if (act.startsWith(".")) "$pkg$act" else "$pkg/$act"
                        return FrontTaskInfo(tid, pkg, fullComponent)
                    }
                }
            }
        }

        val winOut = readCommand("dumpsys", "window", "displays", timeoutMs = 2500L)
        if (!winOut.isNullOrBlank()) {
            val focusRegex = Regex("""mCurrentFocus=Window\{[^\s]+ [^\s]+ ([^\s/]+)/([^\s}]+)""")
            val focusMatch = focusRegex.find(winOut)
            if (focusMatch != null) {
                val pkg = focusMatch.groupValues[1]
                val act = focusMatch.groupValues[2]
                if (pkg != context.packageName && pkg != "com.android.systemui" && pkg != "com.miui.home") {
                    val fullComponent = if (act.startsWith(".")) "$pkg$act" else "$pkg/$act"
                    return FrontTaskInfo(0, pkg, fullComponent)
                }
            }
        }

        val tasksOut = readCommand("dumpsys", "activity", "recents", timeoutMs = 2000L)
        if (!tasksOut.isNullOrBlank()) {
            val taskRegex = Regex("""TaskId=(\d+): ([^/\s]+)/([^\s]+)""")
            for (line in tasksOut.lineSequence()) {
                val m = taskRegex.find(line)
                if (m != null) {
                    val tid = m.groupValues[1].toIntOrNull() ?: continue
                    val pkg = m.groupValues[2]
                    val act = m.groupValues[3]
                    if (pkg != context.packageName && pkg != "com.android.systemui" && pkg != "com.miui.home") {
                        return FrontTaskInfo(tid, pkg, "$pkg/$act")
                    }
                }
            }
        }

        val stackOut = readCommand(AM, "stack", "list", timeoutMs = 2000L)
        if (!stackOut.isNullOrBlank()) {
            val frontTask = parseTasksOnDisplay(stackOut, Display.DEFAULT_DISPLAY).firstOrNull { (_, pkg) ->
                pkg != "com.android.systemui" && pkg != "com.miui.home" && pkg != context.packageName
            }
            if (frontTask != null) {
                return FrontTaskInfo(frontTask.first, frontTask.second, null)
            }
        }

        return null
    }

    fun getRearDisplayTopTask(context: Context, rearDisplayId: Int): FrontTaskInfo? {
        val winOut = readCommand("dumpsys", "window", "displays", timeoutMs = 2500L)
        if (!winOut.isNullOrBlank()) {
            var inRear = false
            for (line in winOut.lineSequence()) {
                if (line.contains("mDisplayId=$rearDisplayId") || line.contains("Display #$rearDisplayId")) {
                    inRear = true
                    continue
                }
                if (inRear && (line.contains("mDisplayId=") || line.contains("Display #"))) {
                    inRear = false
                }
                if (inRear) {
                    val focusRegex = Regex("""mCurrentFocus=Window\{[^\s]+ [^\s]+ ([^\s/]+)/([^\s}]+)""")
                    val m = focusRegex.find(line)
                    if (m != null) {
                        val pkg = m.groupValues[1]
                        val act = m.groupValues[2]
                        if (pkg != context.packageName && pkg != XIAOMI_REAR_PACKAGE && pkg != "com.android.systemui") {
                            return FrontTaskInfo(0, pkg, "$pkg/$act")
                        }
                    }
                }
            }
        }
        val stackOut = readCommand(AM, "stack", "list", timeoutMs = 2000L)
        if (!stackOut.isNullOrBlank()) {
            val rearTasks = parseTasksOnDisplay(stackOut, rearDisplayId)
            val nonSys = rearTasks.firstOrNull { it.second != XIAOMI_REAR_PACKAGE && it.second != context.packageName }
            if (nonSys != null) {
                return FrontTaskInfo(nonSys.first, nonSys.second, null)
            }
        }
        return null
    }

    fun isCameraActiveOnRear(context: Context): Boolean {
        if (System.currentTimeMillis() < suppressRelaunchUntil) return true
        val rearId = getTargetRearDisplayId(context)
        if (rearId <= 0 || rearId == Display.DEFAULT_DISPLAY) return false
        val task = getRearDisplayTopTask(context, rearId)
        if (task != null) {
            suppressRelaunchUntil = System.currentTimeMillis() + 60_000L
            return true
        }
        return false
    }

    fun launchRearIfIdle(context: Context) {
        val appContext = context.applicationContext
        shellExecutor.execute {
            if (isProjectionActive) {
                return@execute
            }
            if (isCameraActiveOnRear(appContext)) {
                // Kamera hala arka ekranda acik, MiRearUltra arka plani devralmasin
                suppressRelaunchUntil = System.currentTimeMillis() + 60_000L
                return@execute
            }
            mainHandler.post { launchRearExperience(appContext, RearStateRepository.activeMode.value) }
        }
    }

    private fun onDirectLaunchTimedOut() {
        if (!awaitingDirectLaunch) return
        awaitingDirectLaunch = false
        val appContext = appContextRef ?: return
        val rearDisplayId = getTargetRearDisplayId(appContext)
        if (rearDisplayId <= 0 || rearDisplayId == Display.DEFAULT_DISPLAY) return

        shellExecutor.execute {
            val stackOut = readCommand(AM, "stack", "list", timeoutMs = 3000L)
            if (stackOut != null) {
                val rearPackages = parsePackagesOnDisplay(stackOut, rearDisplayId)
                if (appContext.packageName in rearPackages) {
                    runCommand(INPUT, "-d", rearDisplayId.toString(), "keyevent", "KEYCODE_WAKEUP")
                    restartXiaomiSubscreenIfUnder(appContext.packageName, rearDisplayId)
                    RearStateRepository.appendLog("✅ Arka Ekran (#$rearDisplayId) görevi doğrulandı.")
                }
            }
        }
    }

    /**
     * Called immediately by [RearDisplayActivity.onCreate] if the activity landed on the front
     * display (`Display.DEFAULT_DISPLAY == 0`). Moves its root task (`taskId`) directly onto the
     * Xiaomi 17 Pro Max rear display (`rearDisplayId`) using `/system/bin/am display move-stack`.
     */
    fun onHostCreatedOnMain(context: Context, taskId: Int, token: String?) {
        awaitingDirectLaunch = false
        mainHandler.removeCallbacks(directLaunchTimeoutRunnable)
        val rearDisplayId = getTargetRearDisplayId(context)

        shellExecutor.execute {
            runCommand(INPUT, "-d", rearDisplayId.toString(), "keyevent", "KEYCODE_WAKEUP")
            var moveRes = moveTaskToDisplay(taskId, rearDisplayId)
            if (moveRes != "ok") {
                // Fallback binder transaction used by MiRearScreenSwitcher
                moveRes = runCommand(
                    SERVICE_BIN, "call", "activity_task", "50",
                    "i32", taskId.toString(), "i32", rearDisplayId.toString()
                )
            }
            RearStateRepository.appendLog("🎯 Görev (#$taskId) Arka Ekrana (Display #$rearDisplayId) taşındı: $moveRes")
            mainHandler.post {
                if (moveRes == "ok") {
                    RearDisplayActivity.notifyMovedToRear(token)
                }
            }
            // Also wake rear display & neutralise Xiaomi's subscreencenter launcher underneath
            runCommand(INPUT, "-d", rearDisplayId.toString(), "keyevent", "KEYCODE_WAKEUP")
            restartXiaomiSubscreenIfUnder(context.packageName, rearDisplayId)
        }
    }

    /**
     * Called by [RearDisplayActivity] as soon as it is live on the rear display (`displayId != 0`).
     */
    fun onHostShownOnRear(context: Context, displayId: Int) {
        awaitingDirectLaunch = false
        mainHandler.removeCallbacks(directLaunchTimeoutRunnable)
        saveRememberedRearId(context, displayId)
        RearStateRepository.appendLog("✅ MiRearUltra şu anda Arka Ekranda (Display #$displayId) aktif!")

        shellExecutor.execute {
            runCommand(INPUT, "-d", displayId.toString(), "keyevent", "KEYCODE_WAKEUP")
            val restarted = restartXiaomiSubscreenIfUnder(context.packageName, displayId)
            if (restarted) {
                RearStateRepository.appendLog("🛡️ Xiaomi SubScreenLauncher arka planda sıfırlandı (ekran kararınca kapanmayı önler).")
            }
        }
    }

    /**
     * Diagnostic self-test triggered from MainScreen so the user can verify Shizuku & Rear Display
     * with full step-by-step logs.
     */
    fun runFullShizukuDiagnosticAndSwitch(context: Context) {
        val appContext = context.applicationContext
        val state = readShizukuState(appContext)
        RearStateRepository.appendLog("🔍 [Teşhis] Shizuku Durumu: $state")
        if (state != ShizukuConnectionState.READY) {
            requestShizukuPermission(appContext)
            return
        }
        val rearId = getTargetRearDisplayId(appContext)
        shellExecutor.execute {
            val wakeRes = runCommand(INPUT, "-d", rearId.toString(), "keyevent", "KEYCODE_WAKEUP")
            val stackOut = readCommand(AM, "stack", "list", timeoutMs = 3000L)
            val stackStatus = if (!stackOut.isNullOrBlank()) "OK (${stackOut.lines().size} satır)" else "Alınamadı"
            RearStateRepository.appendLog("🔍 [Teşhis] Hedef Arka Ekran: #$rearId | Uyandırma: $wakeRes | Stack Listesi: $stackStatus")
            mainHandler.post {
                launchRearExperience(appContext, RearStateRepository.activeMode.value)
            }
        }
    }

    private fun launchHostCommand(
        packageName: String,
        displayId: Int,
        modeName: String,
        token: String
    ): String {
        // KESİN GÜVENLİK ENGELİ: Arka ekran aktivitesi ASLA Display #0'a (ön ekran) başlatılamaz!
        if (displayId <= 0 || displayId == Display.DEFAULT_DISPLAY) {
            RearStateRepository.appendLog("⛔ Güvenlik Engeli: Arka ekran asla ön ekrana (#0) başlatılamaz.")
            return "blocked_display_zero"
        }
        return runCommand(
            AM, "start",
            "--display", displayId.toString(),
            "-f", "0x10000000",
            "-n", "$packageName/${RearDisplayActivity::class.java.name}",
            "--es", EXTRA_MODE, modeName,
            "--es", EXTRA_TOKEN, token
        )
    }

    fun moveTaskToDisplay(taskId: Int, displayId: Int): String {
        return runCommand(AM, "display", "move-stack", taskId.toString(), displayId.toString())
    }

    /**
     * Checks `am stack list` to see if `com.xiaomi.subscreencenter` is underneath [frontPackage]
     * on [displayId]. If so, restarts `com.xiaomi.subscreencenter` once so it comes back without
     * its launcher and won't close our rear window when the rear screen dims (HyperOS 3.0.319+).
     */
    private fun restartXiaomiSubscreenIfUnder(frontPackage: String, displayId: Int): Boolean {
        val stackOutput = readCommand(AM, "stack", "list", timeoutMs = 3000L) ?: return false
        val packagesOnRear = parsePackagesOnDisplay(stackOutput, displayId)
        if (packagesOnRear.firstOrNull() == frontPackage && XIAOMI_REAR_PACKAGE in packagesOnRear) {
            val res = runCommand(AM, "force-stop", XIAOMI_REAR_PACKAGE)
            return res == "ok"
        }
        return false
    }

    private val ROOT_TASK_REGEX = Regex("""^RootTask id=\d+ .*\bdisplayId=(\d+)\b""")
    private val TASK_LINE_REGEX = Regex("""^\s+taskId=(\d+): ([^/\s]+)/""")

    private fun parseTasksOnDisplay(stackOutput: String, targetDisplayId: Int): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        var currentDisplay: Int? = null
        for (line in stackOutput.lineSequence()) {
            val rootMatch = ROOT_TASK_REGEX.find(line)
            if (rootMatch != null) {
                currentDisplay = rootMatch.groupValues[1].toIntOrNull()
                continue
            }
            if (currentDisplay != targetDisplayId) continue
            val taskMatch = TASK_LINE_REGEX.find(line)
            if (taskMatch != null) {
                val taskId = taskMatch.groupValues[1].toIntOrNull() ?: continue
                val pkg = taskMatch.groupValues[2]
                result.add(taskId to pkg)
            }
        }
        return result
    }

    private fun parsePackagesOnDisplay(stackOutput: String, targetDisplayId: Int): List<String> {
        return parseTasksOnDisplay(stackOutput, targetDisplayId).map { it.second }.distinct()
    }

    private fun launchActivityOnActualRearDisplay(
        context: Context,
        rearDisplay: Display,
        mode: RearScreenMode,
        token: String
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            val intent = Intent(context, RearDisplayActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                putExtra(EXTRA_MODE, mode.name)
                putExtra(EXTRA_TOKEN, token)
            }
            val options = ActivityOptions.makeBasic().apply {
                launchDisplayId = rearDisplay.displayId
            }
            context.startActivity(intent, options.toBundle())
        }
    }

    /**
     * Opens Xiaomi HyperOS's native Rear Display (SubScreen) system settings if installed,
     * or Display settings as fallback.
     */
    fun openXiaomiNativeSubScreenSettings(context: Context) {
        val candidates = listOf(
            ComponentName("com.xiaomi.subscreencenter", "com.xiaomi.subscreencenter.SubScreenMainActivity"),
            ComponentName("com.xiaomi.subscreencenter", "com.xiaomi.subscreencenter.settings.SubScreenSettingsActivity"),
            ComponentName("com.miui.aod", "com.miui.aod.settings.SubScreenSettingsActivity"),
            ComponentName("com.android.settings", "com.android.settings.SubScreenSettings")
        )
        for (comp in candidates) {
            val ok = runCatching {
                context.startActivity(Intent().apply {
                    component = comp
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                true
            }.getOrDefault(false)
            if (ok) {
                RearStateRepository.appendLog("⚙️ Xiaomi HyperOS Orjinal Arka Ekran Ayarları açıldı.")
                return
            }
        }
        runCatching {
            context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    fun launchAppOnRearDisplay(context: Context, targetPackageName: String) {
        val rearDisplayId = getTargetRearDisplayId(context)
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(targetPackageName)
        val component = launchIntent?.component

        shellExecutor.execute {
            if (checkShizukuStatus()) {
                val targetDpi = RearStateRepository.settings.value.targetRearDpi
                runCommand(INPUT, "-d", rearDisplayId.toString(), "keyevent", "KEYCODE_WAKEUP")
                runCommand(CMD, "window", "density", targetDpi.toString(), "-d", rearDisplayId.toString())

                if (component != null) {
                    val directRes = runCommand(
                        AM, "start", "--display", rearDisplayId.toString(),
                        "-n", component.flattenToShortString()
                    )
                    if (directRes == "ok") {
                        restartXiaomiSubscreenIfUnder(targetPackageName, rearDisplayId)
                        RearStateRepository.appendLog("🚀 $targetPackageName arka ekranda (Display #$rearDisplayId, ${targetDpi}dpi) başlatıldı.")
                        return@execute
                    }
                    // Fallback: Start on main display and move its task stack to rear display
                    runCommand(AM, "start", "-n", component.flattenToShortString())
                    Thread.sleep(350)
                    val stackOut = readCommand(AM, "stack", "list", timeoutMs = 3000L)
                    val taskOnMain = stackOut?.let { out ->
                        parseTasksOnDisplay(out, Display.DEFAULT_DISPLAY).firstOrNull { it.second == targetPackageName }
                    }
                    if (taskOnMain != null) {
                        val moveRes = moveTaskToDisplay(taskOnMain.first, rearDisplayId)
                        restartXiaomiSubscreenIfUnder(targetPackageName, rearDisplayId)
                        RearStateRepository.appendLog("🚀 $targetPackageName görev taşıma ile arka ekrana (#$rearDisplayId) aktarıldı: $moveRes")
                        return@execute
                    }
                }
            }

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                runCatching {
                    val options = ActivityOptions.makeBasic().setLaunchDisplayId(rearDisplayId)
                    context.startActivity(launchIntent, options.toBundle())
                    RearStateRepository.appendLog("📲 $targetPackageName arka ekrana (#$rearDisplayId) yönlendirildi.")
                }.onFailure {
                    RearStateRepository.appendLog("⚠️ 3. parti uygulamayı arka ekrana taşımak için Shizuku yetkisi gereklidir.")
                }
            } else {
                RearStateRepository.appendLog("ℹ️ $targetPackageName cihazda yüklü değil.")
            }
        }
    }

    fun switchForegroundTaskToRear(context: Context) {
        val rearDisplayId = getTargetRearDisplayId(context)
        shellExecutor.execute {
            if (!checkShizukuStatus()) {
                mainHandler.post { launchRearExperience(context, RearScreenMode.HOME_LAUNCHER) }
                return@execute
            }
            isProjectionActive = true
            RearStateRepository.setProjectingTaskToRear(true)
            suppressRelaunchUntil = System.currentTimeMillis() + 86400_000L

            runCommand(CMD, "statusbar", "collapse")
            runCommand(INPUT, "-d", rearDisplayId.toString(), "keyevent", "KEYCODE_WAKEUP")
            runCommand(CMD, "window", "density", "260", "-d", rearDisplayId.toString())

            val frontTask = getFrontDisplayTopTask(context)
            if (frontTask != null) {

                var moved = false
                if (frontTask.taskId > 0) {
                    val move1 = runCommand("cmd", "activity", "task", "moveToDisplay", frontTask.taskId.toString(), rearDisplayId.toString())
                    if (move1 == "ok") moved = true
                    if (!moved) {
                        val move2 = runCommand(AM, "task", "moveToDisplay", frontTask.taskId.toString(), rearDisplayId.toString())
                        if (move2 == "ok") moved = true
                    }
                    if (!moved) {
                        val move3 = moveTaskToDisplay(frontTask.taskId, rearDisplayId)
                        if (move3 == "ok") moved = true
                    }
                    if (!moved) {
                        val move4 = runCommand(SERVICE_BIN, "call", "activity_task", "50", "i32", frontTask.taskId.toString(), "i32", rearDisplayId.toString())
                        if (move4 == "ok") moved = true
                    }
                }

                if (!moved) {
                    val comp = frontTask.componentName
                    if (!comp.isNullOrBlank()) {
                        val launchRes = runCommand(AM, "start", "--display", rearDisplayId.toString(), "-n", comp)
                        if (launchRes == "ok") moved = true
                    }
                    if (!moved) {
                        val pm = context.packageManager
                        val launchIntent = pm.getLaunchIntentForPackage(frontTask.packageName)
                        val cmp = launchIntent?.component?.flattenToShortString()
                        if (cmp != null) {
                            runCommand(AM, "start", "--display", rearDisplayId.toString(), "-n", cmp)
                            moved = true
                        }
                    }
                }

                restartXiaomiSubscreenIfUnder(frontTask.packageName, rearDisplayId)
                RearStateRepository.appendLog("🚀 ${frontTask.packageName} (Görev #${frontTask.taskId}) arka ekrana (#$rearDisplayId, 260dpi) aktarıldı.")
            } else {
                RearStateRepository.appendLog("⚠️ Aktif ön ekran uygulaması tespit edilemedi. Arka ekran ana görünümüne dönülüyor.")
                mainHandler.post { launchRearExperience(context, RearScreenMode.HOME_LAUNCHER) }
            }
        }
    }

    fun restoreXiaomiOriginalRearDisplay(context: Context) {
        val rearDisplayId = getTargetRearDisplayId(context)
        shellExecutor.execute {
            suppressRelaunchUntil = System.currentTimeMillis() + 86400_000L
            isProjectionActive = false
            RearStateRepository.setProjectingTaskToRear(false)
            RearStateRepository.appendLog("🔄 Orijinal Xiaomi Arka Ekranına dönülüyor...")

            runCatching {
                val intent = Intent("com.mirearultra.pro.ACTION_DISMISS_REAR").setPackage(context.packageName)
                context.sendBroadcast(intent)
            }

            if (checkShizukuStatus()) {
                val rearTask = getRearDisplayTopTask(context, rearDisplayId)
                if (rearTask != null && rearTask.taskId > 0 && rearTask.packageName == context.packageName) {
                    runCommand(AM, "task", "lock", "stop")
                    runCommand(AM, "stack", "remove", rearTask.taskId.toString())
                }
                runCommand(AM, "start", "--display", rearDisplayId.toString(), "-n", "com.xiaomi.subscreencenter/.SubScreenLauncher")
                runCommand(INPUT, "-d", rearDisplayId.toString(), "keyevent", "KEYCODE_WAKEUP")
                RearStateRepository.appendLog("✅ Xiaomi Orijinal Arka Ekranı (#$rearDisplayId) başlatıldı.")
            }
        }
    }

    fun stopProjection(context: Context) {
        val rearDisplayId = getTargetRearDisplayId(context)
        shellExecutor.execute {
            isProjectionActive = false
            RearStateRepository.setProjectingTaskToRear(false)
            suppressRelaunchUntil = 0L
            if (checkShizukuStatus()) {
                if (RearStateRepository.settings.value.autoResetDpiOnReturn) {
                    runCommand(CMD, "window", "density", "reset", "-d", rearDisplayId.toString())
                }
                val rearTask = getRearDisplayTopTask(context, rearDisplayId)
                if (rearTask != null && rearTask.taskId > 0) {
                    runCommand("cmd", "activity", "task", "moveToDisplay", rearTask.taskId.toString(), Display.DEFAULT_DISPLAY.toString())
                    runCommand(AM, "task", "moveToDisplay", rearTask.taskId.toString(), Display.DEFAULT_DISPLAY.toString())
                }
            }
            mainHandler.post { launchRearExperience(context, RearScreenMode.HOME_LAUNCHER) }
            RearStateRepository.appendLog("⏹️ Yansıtma sonlandırıldı -> Arka ekran ana temaya döndü.")
        }
    }

    fun sendBackKeyToRearDisplay(context: Context) {
        val rearDisplayId = getTargetRearDisplayId(context)
        shellExecutor.execute {
            suppressRelaunchUntil = 0L
            if (checkShizukuStatus()) {
                val res = runCommand(INPUT, "-d", rearDisplayId.toString(), "keyevent", "KEYCODE_BACK")
                RearStateRepository.appendLog("🔙 Arka ekrana (#$rearDisplayId) GERİ (Back) komutu gönderildi: $res")
            } else {
                RearStateRepository.setMode(RearScreenMode.HOME_LAUNCHER)
                RearStateRepository.appendLog("🔙 Arka ekran ana temaya döndü.")
            }
        }
    }

    fun moveRearTaskBackToFrontDisplay(context: Context) {
        val rearDisplayId = getTargetRearDisplayId(context)
        shellExecutor.execute {
            isProjectionActive = false
            RearStateRepository.setProjectingTaskToRear(false)
            suppressRelaunchUntil = 0L
            if (checkShizukuStatus()) {
                if (RearStateRepository.settings.value.autoResetDpiOnReturn) {
                    runCommand(CMD, "window", "density", "reset", "-d", rearDisplayId.toString())
                }
                val rearTask = getRearDisplayTopTask(context, rearDisplayId)
                if (rearTask != null) {
                    if (rearTask.taskId > 0) {
                        runCommand("cmd", "activity", "task", "moveToDisplay", rearTask.taskId.toString(), Display.DEFAULT_DISPLAY.toString())
                        runCommand(AM, "task", "moveToDisplay", rearTask.taskId.toString(), Display.DEFAULT_DISPLAY.toString())
                    }
                    if (!rearTask.componentName.isNullOrBlank()) {
                        runCommand(AM, "start", "--display", Display.DEFAULT_DISPLAY.toString(), "-n", rearTask.componentName)
                    }
                    RearStateRepository.appendLog("📲 ${rearTask.packageName} ön ekrana (Display #0) geri aktarıldı.")
                } else {
                    RearStateRepository.appendLog("ℹ️ Arka ekranda taşınacak açık görev bulunamadı.")
                }
                mainHandler.post { launchRearExperience(context, RearScreenMode.HOME_LAUNCHER) }
            } else {
                val intent = Intent(context, com.mirearultra.pro.MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                runCatching { context.startActivity(intent) }
            }
        }
    }

    fun resetRearDisplayDpi(context: Context) {
        val rearDisplayId = getTargetRearDisplayId(context)
        shellExecutor.execute {
            if (checkShizukuStatus()) {
                val res = runCommand(CMD, "window", "density", "reset", "-d", rearDisplayId.toString())
                RearStateRepository.appendLog("✨ Arka ekran (#$rearDisplayId) DPI değeri sıfırlandı: $res")
            } else {
                RearStateRepository.updateSettings { it.copy(targetRearDpi = 260) }
                RearStateRepository.appendLog("✨ Hedef arka ekran DPI değeri 260 olarak sıfırlandı.")
            }
        }
    }

    /**
     * Opens Xiaomi Camera in its native rear-screen selfie mode on Display #1
     * (exact HyperOS 3 route from Xiaomi's SubScreenLauncher: flags 0x30a08000).
     */
    fun openXiaomiCameraOnRear(context: Context) {
        val rearDisplayId = getTargetRearDisplayId(context)
        // Kamera arka ekranda açıkken bekçi servis arka ekranı geri çekmesin
        suppressRelaunchUntil = System.currentTimeMillis() + 60_000L
        shellExecutor.execute {
            if (checkShizukuStatus()) {
                runCommand(INPUT, "-d", rearDisplayId.toString(), "keyevent", "KEYCODE_WAKEUP")
                val res = runCommand(
                    AM, "start", "--display", rearDisplayId.toString(),
                    "-n", "com.android.camera/.Camera", "-f", "0x30a08000"
                )
                RearStateRepository.appendLog("📷 Xiaomi Leica Arka Kamera (#$rearDisplayId) başlatıldı: $res")
            } else {
                RearStateRepository.appendLog("⚠️ Xiaomi Arka Kamera modunu doğrudan açmak için Shizuku yetkisi gereklidir.")
            }
        }
    }

    fun dispatchMediaKey(context: Context, action: String) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
        val keyCode = when (action) {
            "PREV" -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "NEXT" -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
            else -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        runCatching {
            am?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode))
            am?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode))
        }
        val current = RearStateRepository.mediaState.value
        if (action == "PLAY_PAUSE") {
            RearStateRepository.updateMediaState(current.copy(isPlaying = !current.isPlaying))
        } else if (action == "NEXT") {
            val nextIdx = (current.activeLyricIndex + 1) % current.lyricsLines.size.coerceAtLeast(1)
            RearStateRepository.updateMediaState(current.copy(activeLyricIndex = nextIdx, isPlaying = true))
        } else if (action == "PREV") {
            val prevIdx = (current.activeLyricIndex - 1).coerceAtLeast(0)
            RearStateRepository.updateMediaState(current.copy(activeLyricIndex = prevIdx, isPlaying = true))
        }
    }

    // =========================================================================
    // Low-Level Direct Shizuku Process Execution (From RearCommands architecture)
    // =========================================================================

    private val newProcessMethod by lazy {
        Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        ).apply { isAccessible = true }
    }

    private fun startShizukuProcess(command: Array<String>): Process {
        return newProcessMethod.invoke(null, command, null, null) as Process
    }

    fun runCommand(vararg command: String, timeoutMs: Long = 5000L): String {
        if (!checkShizukuStatus()) return "Shizuku hazır değil"
        return try {
            summarizeResult(execProcess(timeoutMs) { startShizukuProcess(arrayOf(*command)) })
        } catch (e: Throwable) {
            "hata: ${e.cause?.message ?: e.message}"
        }
    }

    fun readCommand(vararg command: String, timeoutMs: Long = 3500L): String? {
        if (!checkShizukuStatus()) return null
        return try {
            val result = execProcess(timeoutMs) { startShizukuProcess(arrayOf(*command)) }
            if (result.code == 0) result.out else null
        } catch (e: Throwable) {
            null
        }
    }

    private fun summarizeResult(result: ShellExecResult): String {
        val code = result.code ?: return "zaman aşımı (timeout)"
        val output = (result.out.trim() + "\n" + result.err.trim()).trim()
        val errorLine = output.lines().firstOrNull {
            // "Activity not started, intent has been delivered to currently running top most instance" zararsız bir bilgidir
            it.startsWith("Error") || it.contains("SecurityException")
        }
        return when {
            code != 0 -> "başarısız ($code): ${output.lines().lastOrNull().orEmpty()}"
            errorLine != null -> "başarısız: $errorLine"
            else -> "ok"
        }
    }

    private fun execProcess(timeoutMs: Long, start: () -> Process): ShellExecResult {
        val process = start()
        val out = StreamDrainer(process.inputStream)
        val err = StreamDrainer(process.errorStream)
        var exitCode = 0
        var failure: Exception? = null
        val waiter = thread(isDaemon = true, name = "MiRearUltra-Wait") {
            try {
                exitCode = process.waitFor()
            } catch (e: Exception) {
                failure = e
            }
        }
        waiter.join(timeoutMs)
        if (waiter.isAlive || failure != null) {
            runCatching { process.destroy() }
            out.close()
            err.close()
            failure?.let { throw it }
            return ShellExecResult(null, out.text(), err.text())
        }
        return ShellExecResult(exitCode, out.text(), err.text())
    }

    private class StreamDrainer(private val stream: InputStream) : Closeable {
        private val bytes = ByteArrayOutputStream()
        private val reader = thread(isDaemon = true, name = "MiRearUltra-Read") {
            try {
                stream.copyTo(bytes)
            } catch (_: IOException) {
            }
        }

        fun text(): String {
            reader.join(500L)
            return bytes.toString(Charsets.UTF_8.name())
        }

        override fun close() {
            runCatching { stream.close() }
        }
    }

    fun executeShizukuShell(command: String): Boolean {
        val res = runCommand("/system/bin/sh", "-c", command)
        return res == "ok"
    }

    fun executeShizukuShellWithOutput(command: String): String {
        return readCommand("/system/bin/sh", "-c", command).orEmpty()
    }
}
