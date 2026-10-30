package com.mirearultra.pro.service
import java.util.Locale

import android.Manifest
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mirearultra.pro.MainActivity
import com.mirearultra.pro.core.BatteryTelemetry
import com.mirearultra.pro.core.CallEvent
import com.mirearultra.pro.core.CallStatus
import com.mirearultra.pro.core.RearDisplayEngine
import com.mirearultra.pro.core.RearScreenMode
import com.mirearultra.pro.core.RearStateRepository
import kotlin.math.abs

class RearEngineForegroundService : Service(), SensorEventListener {
    private var lastAccelX = 0f
    private var lastAccelY = 0f
    private var lastAccelZ = 9.8f

    companion object {
        private const val CHANNEL_ID = "mirear_ultra_engine_channel"
        private const val NOTIF_ID = 17001

        fun start(context: Context) {
            val intent = Intent(context, RearEngineForegroundService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }

    private var sensorManager: SensorManager? = null
    private var displayManager: DisplayManager? = null
    private var lastFaceDownWakeTimestamp = 0L
    private var lastLevelEmitTimestamp = 0L
    private var previousRearDisplayState = android.view.Display.STATE_UNKNOWN
    private var lastRearEnsureTimestamp = 0L

    private val subScreenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (RearDisplayEngine.isProjectionActive) return
            val now = System.currentTimeMillis()
            if (now - lastRearEnsureTimestamp < 2500L) return
            if (!com.mirearultra.pro.rear.RearDisplayActivity.visibleOnRear) {
                lastRearEnsureTimestamp = now
                RearDisplayEngine.launchRearIfIdle(this@RearEngineForegroundService)
            }
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            RearDisplayEngine.scanDisplays(this@RearEngineForegroundService)
            if (displayId != android.view.Display.DEFAULT_DISPLAY) {
                RearDisplayEngine.launchRearExperience(
                    this@RearEngineForegroundService,
                    RearStateRepository.activeMode.value
                )
            }
        }

        override fun onDisplayRemoved(displayId: Int) {
            RearDisplayEngine.scanDisplays(this@RearEngineForegroundService)
        }

        override fun onDisplayChanged(displayId: Int) {
            if (displayId != android.view.Display.DEFAULT_DISPLAY) {
                val d = displayManager?.getDisplay(displayId) ?: return
                val woke = d.state == android.view.Display.STATE_ON &&
                    previousRearDisplayState != android.view.Display.STATE_ON
                previousRearDisplayState = d.state
                if (woke && !com.mirearultra.pro.rear.RearDisplayActivity.visibleOnRear) {
                    if (RearDisplayEngine.isProjectionActive) return
                    val now = System.currentTimeMillis()
                    if (now - lastRearEnsureTimestamp >= 2500L) {
                        lastRearEnsureTimestamp = now
                        RearDisplayEngine.launchRearIfIdle(this@RearEngineForegroundService)
                    }
                }
            }
        }
    }

    private fun readSysfsFloat(path: String): Float? {
        return runCatching {
            val file = java.io.File(path)
            if (file.exists() && file.canRead()) {
                file.readText().trim().toFloatOrNull()
            } else null
        }.getOrNull()
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 85)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val pct = (level * 100) / scale
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 315)
            val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4150)

            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val currentMicroAmps = abs(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW))

            var calculatedWatts = 0f
            var currentMilliAmps = 0
            var effectiveVoltageMv = voltageMv
            var chargeTypeTag = ""

            if (isCharging) {
                val usbV = readSysfsFloat("/sys/class/power_supply/usb/voltage_now")
                val usbI = readSysfsFloat("/sys/class/power_supply/usb/current_now")
                val batV = readSysfsFloat("/sys/class/power_supply/battery/voltage_now") ?: (voltageMv * 1000f)
                val batI = readSysfsFloat("/sys/class/power_supply/battery/current_now")

                if (usbV != null && usbI != null && usbV > 1_000_000f && kotlin.math.abs(usbI) > 50_000f) {
                    effectiveVoltageMv = (usbV / 1000f).toInt()
                    currentMilliAmps = (kotlin.math.abs(usbI) / 1000f).toInt()
                    calculatedWatts = (usbV / 1_000_000f) * (kotlin.math.abs(usbI) / 1_000_000f)
                } else if (batV != null && batI != null && batV > 1_000_000f && kotlin.math.abs(batI) > 50_000f) {
                    effectiveVoltageMv = (batV / 1000f).toInt()
                    currentMilliAmps = (kotlin.math.abs(batI) / 1000f).toInt()
                    calculatedWatts = (batV / 1_000_000f) * (kotlin.math.abs(batI) / 1_000_000f)
                } else {
                    val currentAmps = if (currentMicroAmps > 50_000) {
                        currentMicroAmps / 1_000_000f
                    } else {
                        currentMicroAmps / 1_000f
                    }
                    currentMilliAmps = (currentAmps * 1000f).toInt()
                    calculatedWatts = ((voltageMv / 1000f) * currentAmps)
                }

                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                val isWireless = plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS

                chargeTypeTag = when {
                    isWireless -> "50W KABLOSUZ TURBO"
                    calculatedWatts >= 85f -> "120W HYPER CHARGE"
                    calculatedWatts >= 55f -> "90W SUPER CHARGE"
                    calculatedWatts >= 40f -> "67W TURBO CHARGE"
                    calculatedWatts >= 20f -> "33W HIZLI ŞARJ"
                    calculatedWatts >= 10f -> "18W HIZLI ŞARJ"
                    else -> "STANDART ŞARJ (USB)"
                }
            }

            val wasCharging = RearStateRepository.batteryTelemetry.value.isCharging
            if (isCharging && !wasCharging) {
                RearStateRepository.appendLog("⚡ Şarj başladı (${String.format(Locale.US, "%.1f", calculatedWatts)}W, ${currentMilliAmps}mA, $chargeTypeTag) -> Arka ekranda şarj animasyonu aktif.")
                val rearId = RearDisplayEngine.getTargetRearDisplayId(this@RearEngineForegroundService)
                if (rearId > 0 && RearDisplayEngine.checkShizukuStatus()) {
                    RearDisplayEngine.runCommand(RearDisplayEngine.INPUT, "-d", rearId.toString(), "keyevent", "KEYCODE_WAKEUP")
                }
            } else if (!isCharging && wasCharging) {
                RearStateRepository.appendLog("🔌 Şarjdan çıkarıldı -> Normal saat ekranına dönüldü.")
                RearStateRepository.setMode(com.mirearultra.pro.core.RearScreenMode.HOME_LAUNCHER)
            }

            RearStateRepository.updateBatteryTelemetry(
                BatteryTelemetry(
                    levelPercent = pct,
                    isCharging = isCharging,
                    wattage = calculatedWatts,
                    currentMilliAmps = currentMilliAmps,
                    chargingType = chargeTypeTag,
                    temperatureCelsius = tempTenths / 10f,
                    voltageMv = effectiveVoltageMv
                )
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        RearDisplayEngine.init(this)
        startForeground(NOTIF_ID, createForegroundNotification())

        // 1. Register DisplayManager listener
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager?.registerDisplayListener(displayListener, null)

        // 2. Register Sensor listeners with Hardware Batching (Low-Power Doze Friendly)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { accel ->
            sensorManager?.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL, 500_000)
        }
        sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let { prox ->
            sensorManager?.registerListener(this, prox, SensorManager.SENSOR_DELAY_NORMAL, 500_000)
        }
        registerStepSensorIfPermitted()

        // 3. Register Battery telemetry & HyperOS SubScreen On receiver
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    subScreenOnReceiver,
                    IntentFilter("miui.intent.action.SUB_SCREEN_ON"),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                registerReceiver(subScreenOnReceiver, IntentFilter("miui.intent.action.SUB_SCREEN_ON"))
            }
        }

        // 4. Register Telephony Call state listener if permission granted
        registerPhoneStateMonitor()
    }

    private fun registerPhoneStateMonitor() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                tm.registerTelephonyCallback(
                    mainExecutor,
                    object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                        override fun onCallStateChanged(state: Int) {
                            handleTelephonyState(state, null)
                        }
                    }
                )
            }
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                tm.listen(
                    object : PhoneStateListener() {
                        @Deprecated("Deprecated in Java")
                        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                            handleTelephonyState(state, phoneNumber)
                        }
                    },
                    PhoneStateListener.LISTEN_CALL_STATE
                )
            }
        }
    }

    private fun handleTelephonyState(state: Int, incomingNumber: String?) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                if (RearStateRepository.currentCall.value == null) {
                    val event = CallEvent(
                        callerName = incomingNumber?.takeIf { it.isNotBlank() } ?: "Gelen Çağrı",
                        phoneNumber = incomingNumber?.takeIf { it.isNotBlank() } ?: "Mobil Hat Araması",
                        sourceApp = "Telefon",
                        status = CallStatus.RINGING
                    )
                    RearStateRepository.onIncomingCall(event)
                    if (RearStateRepository.settings.value.autoWakeRearOnCall) {
                        RearDisplayEngine.launchRearExperience(this, RearScreenMode.INCOMING_CALL)
                    }
                }
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Eğer giden arama yapılıyorsa (OUTGOING), numara çevrilirken hemen kapatma
                val call = RearStateRepository.currentCall.value
                if (call?.status != com.mirearultra.pro.core.CallStatus.OUTGOING) {
                    RearStateRepository.onCallEnded()
                    if (RearStateRepository.activeMode.value == RearScreenMode.INCOMING_CALL) {
                        RearStateRepository.setMode(RearScreenMode.HOME_LAUNCHER)
                    }
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                RearStateRepository.onCallEnded()
                if (RearStateRepository.activeMode.value == RearScreenMode.INCOMING_CALL) {
                    RearStateRepository.setMode(RearScreenMode.HOME_LAUNCHER)
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                lastAccelX = x
                lastAccelY = y
                lastAccelZ = z
                val now = System.currentTimeMillis()

                // Battery Optimization: Only emit Spirit Level StateFlow updates when user is actually viewing Tools or Camera!
                val currentMode = RearStateRepository.activeMode.value
                if ((currentMode == RearScreenMode.TOOLS_HUB || currentMode == RearScreenMode.TELEPROMPTER) &&
                    !RearStateRepository.isRearScreenDimmedSleep.value &&
                    now - lastLevelEmitTimestamp >= 400L
                ) {
                    lastLevelEmitTimestamp = now
                    val tiltX = (x * 9f).coerceIn(-45f, 45f)
                    val tiltY = (y * 9f).coerceIn(-45f, 45f)
                    RearStateRepository.updateSensorLevel(tiltX = tiltX, tiltY = tiltY)
                }

                // Detect when user flips the phone in their hand (z < -2.6f) OR places it face-down on a desk
                val faceDownNow = z < -2.6f
                val wasFaceDown = RearStateRepository.isPhoneFaceDown.value
                if (faceDownNow != wasFaceDown) {
                    RearStateRepository.setPhoneFaceDown(faceDownNow)
                    if (faceDownNow &&
                        RearStateRepository.settings.value.flipToActivateRear &&
                        now - lastFaceDownWakeTimestamp > 3500L
                    ) {
                        lastFaceDownWakeTimestamp = now
                        RearStateRepository.onUserTouchInteraction()
                        RearStateRepository.appendLog("🔄 Telefon arka ekrana çevrildi -> Arka ekran uyandırıldı.")
                        RearDisplayEngine.launchRearExperience(this, RearStateRepository.activeMode.value)
                    }
                }
            }
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values[0]
                val maxRange = event.sensor.maximumRange
                val isCovered = distance < 4.0f && distance < maxRange

                val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                val isFrontInteractive = pm?.isInteractive == true
                val isDeviceLocked = km?.isKeyguardLocked ?: false
                val isFrontScreenInUse = isFrontInteractive && !isDeviceLocked

                // Masaüstü Yüzüstü Durumu: Telefon masaya yüzüstü konduğunda z < -5.5f ve x, y yataydır.
                // Arka ekran tavana/kullanıcıya bakmaktadır. Bu KESİNLİKLE CEP DEĞİLDİR, MASA MODUDUR!
                val isLyingFlatOnDesk = lastAccelZ < -5.5f && kotlin.math.abs(lastAccelX) < 4.5f && kotlin.math.abs(lastAccelY) < 4.5f

                // Kulak Yakınlığı: Yalnızca telefon dikey tutulurken VE aktif arama varken geçerlidir (gizlilik için)
                val isVertical = kotlin.math.abs(lastAccelY) > 5.0f || kotlin.math.abs(lastAccelX) > 5.0f
                val isCallActive = RearStateRepository.currentCall.value != null
                val earCovered = isCovered && isCallActive && isVertical
                RearStateRepository.setEarProximityCovered(earCovered)

                // Cep Modu: Ön ekran aktif kullanılıyorken veya telefon masadayken ASLA cep moduna girilmez.
                // Kullanıcı ön ekranda gezinirken parmağı sensöre gelse dahi cep modu tetiklenemez.
                val inPocket = !isFrontScreenInUse &&
                    isCovered &&
                    !isLyingFlatOnDesk &&
                    isVertical &&
                    RearStateRepository.settings.value.pocketModeProtection

                RearStateRepository.setPocketMode(inPocket)

                // KESİN GÜVENLİK KURALI:
                // Android'de `input keyevent KEYCODE_SLEEP` komutu global sistem güç olayıdır ve Display 0'ı (ÖN EKRANI) anında kapatır.
                // Bu yüzden KEYCODE_SLEEP KESİNLİKLE KULLANILMAZ!
                // Arka ekran karartması NativeHyperOsRearView içinde donanımsal AMOLED saf siyah (#000000 - 0mW) ile güvenli yapılır.
                if (!inPocket && (isLyingFlatOnDesk || !isCovered)) {
                    val rearId = RearDisplayEngine.getTargetRearDisplayId(this)
                    if (rearId > 0 && RearDisplayEngine.checkShizukuStatus()) {
                        RearDisplayEngine.launchRearIfIdle(this)
                    }
                }
            }
            Sensor.TYPE_STEP_COUNTER -> handleStepCounter(event.values[0])
        }
    }

    private var stepSensorRegistered = false

    /** Adım sayacı ACTIVITY_RECOGNITION izni olmadan veri vermez; izin sonradan verilirse tekrar denenir. */
    private fun registerStepSensorIfPermitted() {
        if (stepSensorRegistered) return
        val hasPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) {
            val prefs = getSharedPreferences("mirear_steps", Context.MODE_PRIVATE)
            val last = prefs.getInt("last_steps", -1)
            if (last >= 0 && RearStateRepository.sensorLevelState.value.stepCount < 0) {
                RearStateRepository.updateSensorLevel(steps = last)
            }
            return
        }
        val step = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            ?: return
        stepSensorRegistered = sensorManager?.registerListener(this, step, SensorManager.SENSOR_DELAY_NORMAL, 1_000_000) == true
        if (stepSensorRegistered) {
            val prefs = getSharedPreferences("mirear_steps", Context.MODE_PRIVATE)
            val last = prefs.getInt("last_steps", 0)
            if (last >= 0) RearStateRepository.updateSensorLevel(steps = last)
        }
    }

    /** TYPE_STEP_COUNTER açılıştan beri toplam adımı verir; gün başı referansıyla bugünkü adımı hesaplar. */
    private fun handleStepCounter(totalSinceBoot: Float) {
        val total = totalSinceBoot.toLong()
        val prefs = getSharedPreferences("mirear_steps", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
        var baseDay = prefs.getString("base_day", null)
        var base = prefs.getLong("base_value", -1L)
        if (baseDay != today || base < 0L || total < base) {
            // Yeni gün ya da cihaz yeniden başlatıldı: referansı sıfırla (yeniden başlatmada o güne kadarki adım korunur)
            val carried = if (baseDay == today && total < base) prefs.getInt("last_steps", 0) else 0
            base = total - carried
            baseDay = today
            prefs.edit().putString("base_day", today).putLong("base_value", base).apply()
        }
        val steps = (total - base).toInt().coerceAtLeast(0)
        prefs.edit().putInt("last_steps", steps).apply()
        val level = RearStateRepository.sensorLevelState.value
        if (level.stepCount != steps) {
            RearStateRepository.updateSensorLevel(level.tiltXDeg, level.tiltYDeg, steps = steps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun createForegroundNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MiRearUltra Arka Ekran Motoru",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Xiaomi 17 Pro Max arka ekran arama ve bildirim servisi"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val switchIntent = Intent(this, RearSwitchReceiver::class.java)
        val switchPendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            switchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("MiRearUltra v1.1 Beta")
            .setContentText("Arka ekran aktif • İletişim ve kontroller devrede")
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_send, "🚀 Arka Ekrana Aktar", switchPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        keepAliveHandler.removeCallbacks(keepAliveRunnable)
        runCatching { displayManager?.unregisterDisplayListener(displayListener) }
        runCatching { sensorManager?.unregisterListener(this) }
        runCatching { unregisterReceiver(batteryReceiver) }
        runCatching { unregisterReceiver(subScreenOnReceiver) }
    }

    /**
     * Arka ekranı kalıcı tutan bekçi: uygulama açılır açılmaz (Shizuku hazır olur olmaz) arka ekrana
     * geçer; uygulama kapatılsa bile servis ayakta kaldığı sürece arka ekran içeriğini geri getirir.
     * İlk 40 sn sık (3 sn), sonrasında seyrek (30 sn) çalışır ki pil tüketimi minimum kalsın.
     */
    private val keepAliveHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var keepAliveTicks = 0
    private val keepAliveRunnable = object : Runnable {
        override fun run() {
            ensureRearActive()
            if (!stepSensorRegistered) {
                registerStepSensorIfPermitted()
            }
            keepAliveTicks++
            keepAliveHandler.postDelayed(this, if (keepAliveTicks < 14) 3_000L else 30_000L)
        }
    }

    private fun ensureRearActive() {
        if (com.mirearultra.pro.rear.RearDisplayActivity.visibleOnRear) return
        if (!RearDisplayEngine.checkShizukuStatus()) return

        // 1. Kullanıcı ön ana ekranı aktif olarak kullanırken bekçi ASLA araya girmez
        val frontDisplay = displayManager?.getDisplay(android.view.Display.DEFAULT_DISPLAY)
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        val isFrontActiveAndUnlocked = frontDisplay?.state == android.view.Display.STATE_ON && km?.isKeyguardLocked == false
        if (isFrontActiveAndUnlocked && RearStateRepository.settings.value.skipRearWakeWhenFrontActive) {
            return
        }

        // 2. Arka ekran fiziksel olarak kapalıysa arka planı uyandırma
        val rearOff = displayManager?.displays?.filter { it.displayId != android.view.Display.DEFAULT_DISPLAY }
            ?.all { it.state == android.view.Display.STATE_OFF } == true
        if (rearOff && keepAliveTicks >= 14) return

        val now = System.currentTimeMillis()
        if (now - lastRearEnsureTimestamp < 4000L) return
        lastRearEnsureTimestamp = now
        RearDisplayEngine.launchRearIfIdle(this)
    }

    private fun refreshSpeedDialContacts() {
        if (RearStateRepository.settings.value.speedDialContacts.isNotEmpty()) {
            return // Kullanıcı kendi kişilerini seçtiyse/kaydettiyse üzerine yazma!
        }
        val contacts = com.mirearultra.pro.core.ContactResolver.getFrequentContacts(this)
        if (contacts.isNotEmpty()) {
            RearStateRepository.updateSettings { it.copy(speedDialContacts = contacts.take(6)) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        refreshSpeedDialContacts()
        registerStepSensorIfPermitted()
        keepAliveTicks = 0
        keepAliveHandler.removeCallbacks(keepAliveRunnable)
        keepAliveHandler.postDelayed(keepAliveRunnable, 800L)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Kullanıcı uygulamayı son uygulamalardan kapatsa bile arka ekran ve servis devam etsin.
        super.onTaskRemoved(rootIntent)
        keepAliveTicks = 0
        keepAliveHandler.removeCallbacks(keepAliveRunnable)
        keepAliveHandler.postDelayed(keepAliveRunnable, 1_000L)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}



