package com.mirearultra.pro.core

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class RearScreenMode(val titleTr: String) {
    HOME_LAUNCHER("Ana Ekran"),
    INCOMING_CALL("Gelen Arama"),
    SPEED_DIAL("Hızlı Ara & Tuşlar"),
    NOTIFICATION_POPUP("Mesajlar"),
    LIVE_ACTIVITIES("Canlı Takip"),
    DAILY_CARDS("Günlük Kartlar"),
    MEDIA_LYRICS("Müzik & Söz"),
    TELEPROMPTER("Kamera & Vlog"),
    LED_MARQUEE("LED Tabela & SOS"),
    TOOLS_HUB("Araçlar & Ses"),
    MINI_GAME("Mini Oyun")
}

enum class ClockFaceStyle(val titleTr: String, val subtitleTr: String) {
    LEICA_CYBER("Leica Cyber Pro", "Kırmızı vurgulu telemetri & bildirim kadranı"),
    HYPER_MINIMAL("HyperOS 4 Minimal", "Xiaomi 18 serisi büyük tipografik saat ve pil halkası"),
    NEON_MATRIX("Neon Matrix", "Yüksek kontrastlı AMOLED siberpunk göstergeler"),
    ANALOG_CHRONO("Klasik Kronograf", "Akrep/yelkovan ve canlı saniye kadranı")
}

enum class RearWallpaperTheme(val id: String, val titleTr: String, val subtitleTr: String) {
    MINIMAL_OBSIDIAN("obsidian", "Minimal Obsidyen (AMOLED)", "Saf siyah & gece mavisi, sıfır pil tüketimi"),
    COSMIC_NEBULA("nebula", "Kozmik Nebula", "Koyu mor & yıldız tozu derin uzay gradyanı"),
    CYBER_LEICA("cyber", "Cyber Leica Neon", "Xiaomi 17 Pro Leica kırmızısı & siber ızgara"),
    NORDIC_AURORA("aurora", "Kuzey Işıkları (Aurora)", "Zümrüt yeşili & turkuaz yumuşak ışık akışı"),
    CARBON_FIBER("carbon", "Lüks Karbon Fiber", "Yüksek teknoloji dokulu yarışçı karbon örgü"),
    SUNSET_AMBER("sunset", "HyperOS Gün Batımı", "Sıcak Xiaomi kehribar ve gün batımı geçişi"),
    CUSTOM_PHOTO("custom", "Özel Galeri Fotoğrafı", "Kendi seçtiğiniz duvar kağıdı / görsel")
}

enum class CallStatus {
    IDLE,
    RINGING,
    ACTIVE,
    OUTGOING,
    ENDED
}

data class CallEvent(
    val callerName: String,
    val phoneNumber: String,
    val sourceApp: String = "Telefon",
    val status: CallStatus = CallStatus.RINGING,
    val isSpeakerOn: Boolean = false,
    val isMuted: Boolean = false,
    val isVipCaller: Boolean = false,
    val answerIntent: PendingIntent? = null,
    val declineIntent: PendingIntent? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class SpeedDialContact(
    val name: String,
    val phone: String,
    val roleLabel: String
)

data class RearNotificationItem(
    val id: String,
    val packageName: String,
    val appName: String,
    val sender: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val replyIntent: PendingIntent? = null,
    val remoteInput: RemoteInput? = null,
    val isReplied: Boolean = false,
    val lastReplyText: String? = null,
    val isVip: Boolean = false
)

data class LiveActivityState(
    val navActive: Boolean = false,
    val navApp: String = "Google Haritalar",
    val navManeuver: String = "↱ 250 m sonra Sağa Dön",
    val navStreet: String = "Bağdat Caddesi yönüne devam edin",
    val navEta: String = "12 dk • 4.6 km",
    val navProgress: Float = 0.68f,
    val deliveryActive: Boolean = false,
    val deliveryApp: String = "Yemeksepeti / Getir",
    val deliveryTitle: String = "🛵 Kurye Yola Çıktı",
    val deliveryEta: String = "Tahmini Teslimat: 7 dk",
    val deliveryProgress: Float = 0.82f
)

data class MediaTrackState(
    val title: String = "",
    val artist: String = "",
    val appName: String = "",
    val packageName: String = "",
    val isPlaying: Boolean = false,
    val progressPercent: Float = 0f,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val positionUpdateTime: Long = 0L,
    val playbackSpeed: Float = 1f,
    val artwork: Bitmap? = null,
    val lyricsLines: List<String> = emptyList(),
    val activeLyricIndex: Int = 0
) {
    val hasTrack: Boolean get() = title.isNotBlank()
}

data class AlarmRingingState(
    val label: String,
    val snoozeIntent: PendingIntent?,
    val dismissIntent: PendingIntent?
)

data class SensorLevelState(
    val tiltXDeg: Float = 0.0f,
    val tiltYDeg: Float = 0.0f,
    val stepCount: Int = -1
)

data class BatteryTelemetry(
    val levelPercent: Int = 85,
    val isCharging: Boolean = false,
    val wattage: Float = 0f,
    val currentMilliAmps: Int = 0,
    val chargingType: String = "",
    val temperatureCelsius: Float = 31.5f,
    val voltageMv: Int = 4150
)

data class RearDisplayHardwareInfo(
    val displayId: Int,
    val name: String,
    val widthPx: Int,
    val heightPx: Int,
    val refreshRateHz: Int,
    val densityDpi: Int,
    val isRearHardwareDisplay: Boolean
)

data class UserSettingsState(
    // Module ON/OFF Toggles (User Feature Manager)
    val modIncomingCall: Boolean = true,
    val modSpeedDial: Boolean = true,
    val modNotifications: Boolean = true,
    val modTextToSpeech: Boolean = true,
    val modLiveActivities: Boolean = true,
    val modDailyCards: Boolean = true,
    val modMediaLyrics: Boolean = true,
    val modAppLauncher: Boolean = true,
    val modTeleprompter: Boolean = true,
    val modLedSignAndSos: Boolean = true,
    val modCyberTools: Boolean = true,
    val modMiniGame: Boolean = false,
    val modParkingCard: Boolean = false,
    val modStealthAndWave: Boolean = true,

    // Core & Automation Settings
    val shizukuEnabled: Boolean = true,
    val preferredRearDisplayId: Int = -1, // -1 = Auto Smart Xiaomi 17/18 Subscreen Detection
    val autoWakeRearOnCall: Boolean = true,
    val autoSpeakerOnRearAnswer: Boolean = true,
    val autoWakeRearOnNotification: Boolean = true,
    val earProximityCallPrivacy: Boolean = true,
    val maskCallerPhoneNumber: Boolean = true,
    val maskSpeedDialNumbers: Boolean = true,
    val privacyMaskNotifications: Boolean = false,
    val stealthPrivacyDimmer: Boolean = false,
    val waveGestureSilenceCall: Boolean = true,
    val flipToActivateRear: Boolean = true,
    val pocketModeProtection: Boolean = true,
    val amoledBurnInProtection: Boolean = true,
    val doubleTapTouchLock: Boolean = false,
    val autoSleepInactivity10s: Boolean = false,
    val rearScreenTimeoutSec: Int = 20,
    val ecoBatterySaverMode: Boolean = false,
    val autoEcoBelow20Percent: Boolean = true,
    val skipRearWakeWhenFrontActive: Boolean = true,
    val gameModeProtection: Boolean = true,
    val ltpo1HzClockMode: Boolean = true,
    val vipEdgeLightingEnabled: Boolean = true,
    val vipContactsKeywords: String = "Annem, Babam, Eşim, Patron, Acil",
    val autoStartOnBoot: Boolean = true,
    val autoResetDpiOnReturn: Boolean = true,
    val clockFaceStyle: ClockFaceStyle = ClockFaceStyle.LEICA_CYBER,
    val targetRearDpi: Int = 240,
    val rearDisplayRotation: Int = 0, // 0=0°, 1=90°, 2=180°, 3=270°
    val onlyWhenFaceDown: Boolean = false,
    val followDndMode: Boolean = true,
    val rearWallpaperTheme: RearWallpaperTheme = RearWallpaperTheme.MINIMAL_OBSIDIAN,
    val customWallpaperPath: String? = null,

    // Daily Cards, Marquee, Parking & Content Settings
    val waterGlassesCount: Int = 4,
    val waterGoalCount: Int = 8,
    val parkingSpotNote: String = "B2 Katı • Sarı Alan • Sütun C14",
    val marqueeBannerText: String = "TOPLANTIDAYIM LÜTFEN BÖLMEYİN • ACİLSE MESAJ BIRAKIN",
    val speedDialContacts: List<SpeedDialContact> = emptyList(),
    val quickReplyTemplates: List<String> = listOf(
        "Toplantıdayım, seni birazdan arayacağım.",
        "Şu an müsait değilim, mesaj yazabilir misin?",
        "Yoldayım, 10 dakikaya oradayım.",
        "Tamamdır, teşekkürler! 👍",
        "Harika, görüşürüz!"
    ),
    val teleprompterScript: String = "Merhaba arkadaşlar! Bugün Xiaomi 17 & 18 Pro Max'in arka Leica kameralarıyla kendimi çekerken, arka ekrandaki MiRearUltra Teleprompter ve Canlı Vizör üzerinden bu metni doğrudan kameraya bakarak okuyorum!",
    val teleprompterSpeed: Float = 1.2f,
    val qrCardTitle: String = "Dijital Kartvizit / IBAN / Biniş Kartı",
    val qrCardContent: String = "TR00 0000 0000 0000 0000 0000 00 - Xiaomi Rear Wallet",
    val appLanguage: String = "auto",
    val favoriteRearApps: List<String> = listOf(
        "com.whatsapp",
        "com.spotify.music",
        "com.google.android.apps.maps",
        "com.google.android.youtube",
        "com.instagram.android",
        "com.android.calculator2"
    )
)

object RearStateRepository {

    private const val PREFS_NAME = "mirear_ultra_prefs"
    private var prefs: SharedPreferences? = null

    private val _activeMode = MutableStateFlow(RearScreenMode.HOME_LAUNCHER)
    val activeMode: StateFlow<RearScreenMode> = _activeMode.asStateFlow()

    private val _currentCall = MutableStateFlow<CallEvent?>(null)
    val currentCall: StateFlow<CallEvent?> = _currentCall.asStateFlow()

    private val _notifications = MutableStateFlow<List<RearNotificationItem>>(emptyList())
    val notifications: StateFlow<List<RearNotificationItem>> = _notifications.asStateFlow()

    private val _latestPopupNotification = MutableStateFlow<RearNotificationItem?>(null)
    val latestPopupNotification: StateFlow<RearNotificationItem?> = _latestPopupNotification.asStateFlow()

    private val _liveActivityState = MutableStateFlow(LiveActivityState())
    val liveActivityState: StateFlow<LiveActivityState> = _liveActivityState.asStateFlow()

    private val _mediaState = MutableStateFlow(MediaTrackState())
    val mediaState: StateFlow<MediaTrackState> = _mediaState.asStateFlow()

    private val _sensorLevelState = MutableStateFlow(SensorLevelState())
    val sensorLevelState: StateFlow<SensorLevelState> = _sensorLevelState.asStateFlow()

    private val _batteryTelemetry = MutableStateFlow(BatteryTelemetry())
    val batteryTelemetry: StateFlow<BatteryTelemetry> = _batteryTelemetry.asStateFlow()

    private val _detectedDisplays = MutableStateFlow<List<RearDisplayHardwareInfo>>(emptyList())
    val detectedDisplays: StateFlow<List<RearDisplayHardwareInfo>> = _detectedDisplays.asStateFlow()

    private val _shizukuReady = MutableStateFlow(false)
    val shizukuReady: StateFlow<Boolean> = _shizukuReady.asStateFlow()

    private val _notificationListenerConnected = MutableStateFlow(false)
    val notificationListenerConnected: StateFlow<Boolean> = _notificationListenerConnected.asStateFlow()

    private val _isEarProximityCovered = MutableStateFlow(false)
    val isEarProximityCovered: StateFlow<Boolean> = _isEarProximityCovered.asStateFlow()

    fun setEarProximityCovered(covered: Boolean) {
        if (_isEarProximityCovered.value != covered) {
            _isEarProximityCovered.value = covered
        }
    }

    private val _isPocketMode = MutableStateFlow(false)
    val isPocketMode: StateFlow<Boolean> = _isPocketMode.asStateFlow()

    fun setPocketMode(active: Boolean) {
        if (_isPocketMode.value != active) {
            _isPocketMode.value = active
        }
    }

    private val _isPhoneFaceDown = MutableStateFlow(false)
    val isPhoneFaceDown: StateFlow<Boolean> = _isPhoneFaceDown.asStateFlow()

    private val _isRearScreenDimmedSleep = MutableStateFlow(false)
    val isRearScreenDimmedSleep: StateFlow<Boolean> = _isRearScreenDimmedSleep.asStateFlow()

    private val _isProjectingTaskToRear = MutableStateFlow(false)
    val isProjectingTaskToRear: StateFlow<Boolean> = _isProjectingTaskToRear.asStateFlow()

    fun setProjectingTaskToRear(active: Boolean) {
        _isProjectingTaskToRear.value = active
    }

    private val _lastInteractionTimeMs = MutableStateFlow(System.currentTimeMillis())
    val lastInteractionTimeMs: StateFlow<Long> = _lastInteractionTimeMs.asStateFlow()

    private val _settings = MutableStateFlow(UserSettingsState())
    val settings: StateFlow<UserSettingsState> = _settings.asStateFlow()

    private val _statusLog = MutableStateFlow<List<String>>(
        listOf("MiRearUltra v1.3.0 (Xiaomi 17 Pro Max) başlatıldı.")
    )
    val statusLog: StateFlow<List<String>> = _statusLog.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val styleName = sp.getString("clock_style", ClockFaceStyle.LEICA_CYBER.name) ?: ClockFaceStyle.LEICA_CYBER.name
        val style = runCatching { ClockFaceStyle.valueOf(styleName) }.getOrDefault(ClockFaceStyle.LEICA_CYBER)
        val themeName = sp.getString("rear_wallpaper_theme", RearWallpaperTheme.MINIMAL_OBSIDIAN.name) ?: RearWallpaperTheme.MINIMAL_OBSIDIAN.name
        val wallpaperTheme = runCatching { RearWallpaperTheme.valueOf(themeName) }.getOrDefault(RearWallpaperTheme.MINIMAL_OBSIDIAN)
        val customPath = sp.getString("custom_wallpaper_path", null)
        val savedReplies = sp.getString("quick_replies", null)?.split("||")?.filter { it.isNotBlank() }

        // Adım sayacı önbelleğini anında yükle (ekranda 'adım izni gerekli' beklemesini önler)
        val stepPrefs = context.applicationContext.getSharedPreferences("mirear_steps", Context.MODE_PRIVATE)
        val cachedSteps = stepPrefs.getInt("last_steps", -1)
        if (cachedSteps >= 0) {
            updateSensorLevel(steps = cachedSteps)
        }
        _settings.update { current ->
            current.copy(
                modIncomingCall = sp.getBoolean("mod_call", true),
                modSpeedDial = sp.getBoolean("mod_speed_dial", true),
                modNotifications = sp.getBoolean("mod_notif", true),
                modTextToSpeech = sp.getBoolean("mod_tts", true),
                modLiveActivities = sp.getBoolean("mod_live", true),
                modDailyCards = sp.getBoolean("mod_daily", true),
                modMediaLyrics = sp.getBoolean("mod_media", true),
                modAppLauncher = sp.getBoolean("mod_launcher", true),
                modTeleprompter = sp.getBoolean("mod_prompter", true),
                modLedSignAndSos = sp.getBoolean("mod_led_sos", true),
                modCyberTools = sp.getBoolean("mod_tools", true),
                modMiniGame = sp.getBoolean("mod_game", true),
                modStealthAndWave = sp.getBoolean("mod_stealth_wave", true),
                shizukuEnabled = sp.getBoolean("shizuku_enabled", true),
                preferredRearDisplayId = sp.getInt("pref_rear_display_id", -1),
                autoWakeRearOnCall = sp.getBoolean("auto_wake_call", true),
                autoSpeakerOnRearAnswer = sp.getBoolean("auto_speaker_answer", true),
                autoWakeRearOnNotification = sp.getBoolean("auto_wake_notif", true),
                earProximityCallPrivacy = sp.getBoolean("ear_prox_privacy", true),
                maskCallerPhoneNumber = sp.getBoolean("mask_caller_num", true),
                maskSpeedDialNumbers = sp.getBoolean("mask_speed_dial_num", true),
                privacyMaskNotifications = sp.getBoolean("privacy_mask", true),
                stealthPrivacyDimmer = sp.getBoolean("stealth_dimmer", false),
                waveGestureSilenceCall = sp.getBoolean("wave_gesture", true),
                flipToActivateRear = sp.getBoolean("flip_activate", true),
                pocketModeProtection = sp.getBoolean("pocket_mode", true),
                amoledBurnInProtection = sp.getBoolean("amoled_burn_in", true),
                doubleTapTouchLock = sp.getBoolean("double_tap_lock", false),
                autoSleepInactivity10s = sp.getBoolean("auto_sleep_inactivity_10s", false),
                rearScreenTimeoutSec = sp.getInt("rear_timeout_sec", 20),
                ecoBatterySaverMode = sp.getBoolean("eco_battery_saver", false),
                autoEcoBelow20Percent = sp.getBoolean("auto_eco_20", true),
                skipRearWakeWhenFrontActive = sp.getBoolean("skip_wake_front_active", true),
                gameModeProtection = sp.getBoolean("game_mode_protection", true),
                ltpo1HzClockMode = sp.getBoolean("ltpo_1hz_clock", true),
                vipEdgeLightingEnabled = sp.getBoolean("vip_edge_lighting", true),
                vipContactsKeywords = sp.getString("vip_keywords", current.vipContactsKeywords) ?: current.vipContactsKeywords,
                autoStartOnBoot = sp.getBoolean("auto_start_boot", true),
                autoResetDpiOnReturn = sp.getBoolean("auto_reset_dpi", true),
                clockFaceStyle = style,
                targetRearDpi = sp.getInt("target_rear_dpi", 240),
                rearDisplayRotation = sp.getInt("rear_display_rotation", 0),
                onlyWhenFaceDown = sp.getBoolean("only_when_face_down", false),
                followDndMode = sp.getBoolean("follow_dnd_mode", true),
                rearWallpaperTheme = wallpaperTheme,
                customWallpaperPath = customPath,
                waterGlassesCount = sp.getInt("water_count", 4),
                parkingSpotNote = sp.getString("parking_note", current.parkingSpotNote) ?: current.parkingSpotNote,
                marqueeBannerText = sp.getString("marquee_text", current.marqueeBannerText) ?: current.marqueeBannerText,
                quickReplyTemplates = savedReplies ?: current.quickReplyTemplates,
                teleprompterScript = sp.getString("teleprompter_script", current.teleprompterScript) ?: current.teleprompterScript,
                qrCardTitle = sp.getString("qr_title", current.qrCardTitle) ?: current.qrCardTitle,
                qrCardContent = sp.getString("qr_content", current.qrCardContent) ?: current.qrCardContent,
                speedDialContacts = sp.getString("speed_dial_contacts", null)?.split("||")?.mapNotNull { item ->
                    val parts = item.split("@@")
                    if (parts.size >= 2) SpeedDialContact(name = parts[0], phone = parts[1], roleLabel = parts.getOrNull(2) ?: "Rehber")
                    else null
                } ?: current.speedDialContacts,
                appLanguage = sp.getString("app_language", current.appLanguage) ?: current.appLanguage
            )
        }
        // Ayarlar yüklendikten SONRA yayınla: aksi halde eşzamanlı updateSettings varsayılanları kalıcı yazabilirdi
        prefs = sp
    }

    @Synchronized
    fun updateSettings(transform: (UserSettingsState) -> UserSettingsState) {
        val updated = transform(_settings.value)
        _settings.value = updated
        run {
            prefs?.edit()?.apply {
                putBoolean("mod_call", updated.modIncomingCall)
                putBoolean("mod_speed_dial", updated.modSpeedDial)
                putBoolean("mod_notif", updated.modNotifications)
                putBoolean("mod_tts", updated.modTextToSpeech)
                putBoolean("mod_live", updated.modLiveActivities)
                putBoolean("mod_daily", updated.modDailyCards)
                putBoolean("mod_media", updated.modMediaLyrics)
                putBoolean("mod_launcher", updated.modAppLauncher)
                putBoolean("mod_prompter", updated.modTeleprompter)
                putBoolean("mod_led_sos", updated.modLedSignAndSos)
                putBoolean("mod_tools", updated.modCyberTools)
                putBoolean("mod_game", updated.modMiniGame)
                putBoolean("mod_stealth_wave", updated.modStealthAndWave)
                putBoolean("shizuku_enabled", updated.shizukuEnabled)
                putInt("pref_rear_display_id", updated.preferredRearDisplayId)
                putBoolean("auto_wake_call", updated.autoWakeRearOnCall)
                putBoolean("auto_speaker_answer", updated.autoSpeakerOnRearAnswer)
                putBoolean("auto_wake_notif", updated.autoWakeRearOnNotification)
                putBoolean("ear_prox_privacy", updated.earProximityCallPrivacy)
                putBoolean("mask_caller_num", updated.maskCallerPhoneNumber)
                putBoolean("mask_speed_dial_num", updated.maskSpeedDialNumbers)
                putBoolean("privacy_mask", updated.privacyMaskNotifications)
                putBoolean("stealth_dimmer", updated.stealthPrivacyDimmer)
                putBoolean("wave_gesture", updated.waveGestureSilenceCall)
                putBoolean("flip_activate", updated.flipToActivateRear)
                putBoolean("pocket_mode", updated.pocketModeProtection)
                putBoolean("amoled_burn_in", updated.amoledBurnInProtection)
                putBoolean("double_tap_lock", updated.doubleTapTouchLock)
                putBoolean("auto_sleep_inactivity_10s", updated.autoSleepInactivity10s)
                putInt("rear_timeout_sec", updated.rearScreenTimeoutSec)
                putBoolean("eco_battery_saver", updated.ecoBatterySaverMode)
                putBoolean("auto_eco_20", updated.autoEcoBelow20Percent)
                putBoolean("skip_wake_front_active", updated.skipRearWakeWhenFrontActive)
                putBoolean("game_mode_protection", updated.gameModeProtection)
                putBoolean("ltpo_1hz_clock", updated.ltpo1HzClockMode)
                putBoolean("vip_edge_lighting", updated.vipEdgeLightingEnabled)
                putString("vip_keywords", updated.vipContactsKeywords)
                putBoolean("auto_start_boot", updated.autoStartOnBoot)
                putString("rear_wallpaper_theme", updated.rearWallpaperTheme.name)
                putString("custom_wallpaper_path", updated.customWallpaperPath)
                putBoolean("auto_reset_dpi", updated.autoResetDpiOnReturn)
                putString("clock_style", updated.clockFaceStyle.name)
                putInt("target_rear_dpi", updated.targetRearDpi)
                putInt("rear_display_rotation", updated.rearDisplayRotation)
                putBoolean("only_when_face_down", updated.onlyWhenFaceDown)
                putBoolean("follow_dnd_mode", updated.followDndMode)
                putInt("water_count", updated.waterGlassesCount)
                putString("parking_note", updated.parkingSpotNote)
                putString("marquee_text", updated.marqueeBannerText)
                putString("quick_replies", updated.quickReplyTemplates.joinToString("||"))
                putString("teleprompter_script", updated.teleprompterScript)
                putString("qr_title", updated.qrCardTitle)
                putString("qr_content", updated.qrCardContent)
                putString("speed_dial_contacts", updated.speedDialContacts.joinToString("||") { "${it.name}@@${it.phone}@@${it.roleLabel}" })
                putString("app_language", updated.appLanguage)
                apply()
            }
        }
    }

    fun isVipSender(senderOrName: String): Boolean {
        if (!_settings.value.vipEdgeLightingEnabled) return false
        val keywords = _settings.value.vipContactsKeywords
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        val lower = senderOrName.lowercase()
        return keywords.any { lower.contains(it) }
    }

    fun setMode(mode: RearScreenMode) {
        _activeMode.value = mode
        onUserTouchInteraction()
    }

    fun onUserTouchInteraction() {
        _lastInteractionTimeMs.value = System.currentTimeMillis()
        if (_isRearScreenDimmedSleep.value) {
            _isRearScreenDimmedSleep.value = false
        }
    }

    fun setRearScreenDimmedSleep(dimmed: Boolean) {
        _isRearScreenDimmedSleep.value = dimmed
    }

    fun isEffectiveEcoMode(): Boolean {
        val s = _settings.value
        val bat = _batteryTelemetry.value
        return s.ecoBatterySaverMode || (s.autoEcoBelow20Percent && bat.levelPercent <= 20 && !bat.isCharging)
    }

    fun onIncomingCall(event: CallEvent) {
        if (!_settings.value.modIncomingCall) return
        val enriched = event.copy(isVipCaller = event.isVipCaller || isVipSender(event.callerName))
        _currentCall.value = enriched
        _activeMode.value = RearScreenMode.INCOMING_CALL
        appendLog(
            if (enriched.isVipCaller) "🌟 VIP Gelen Arama: ${enriched.callerName} (${enriched.sourceApp})"
            else "📞 Gelen arama algılandı: ${enriched.callerName} (${enriched.sourceApp})"
        )
    }

    fun onOutgoingCall(name: String, phone: String) {
        val event = CallEvent(
            callerName = name,
            phoneNumber = phone,
            sourceApp = "Telefon",
            status = CallStatus.OUTGOING,
            timestamp = System.currentTimeMillis()
        )
        _currentCall.value = event
        _activeMode.value = RearScreenMode.INCOMING_CALL
        appendLog("☎️ Dış arama başlatıldı: $name ($phone)")
    }

    fun updateCallState(transform: (CallEvent) -> CallEvent) {
        _currentCall.update { current -> current?.let(transform) }
    }

    fun onCallEnded() {
        _currentCall.value = null
        if (_activeMode.value == RearScreenMode.INCOMING_CALL) {
            _activeMode.value = RearScreenMode.HOME_LAUNCHER
        }
        appendLog("📞 Arama sonlandı, arka ekran ana ekrana döndü.")
    }

    fun postNotification(item: RearNotificationItem) {
        if (!_settings.value.modNotifications) return
        val enriched = item.copy(isVip = item.isVip || isVipSender(item.sender))
        _notifications.update { list ->
            val filtered = list.filterNot { it.id == enriched.id }
            (listOf(enriched) + filtered).take(30)
        }
        _latestPopupNotification.value = enriched
        notificationDismissHandler.removeCallbacks(notificationDismissRunnable)
        notificationDismissHandler.postDelayed(notificationDismissRunnable, 7_000L)
        if (_currentCall.value == null && _settings.value.autoWakeRearOnNotification) {
            _activeMode.value = RearScreenMode.NOTIFICATION_POPUP
        }
        appendLog(
            if (enriched.isVip) "🌟 VIP Bildirim [${enriched.appName}]: ${enriched.sender}"
            else "💬 Bildirim yakalandı [${enriched.appName}]: ${enriched.sender}"
        )
    }

    fun updateLiveActivity(transform: (LiveActivityState) -> LiveActivityState) {
        _liveActivityState.update(transform)
    }

    fun updateSensorLevel(tiltX: Float = 0f, tiltY: Float = 0f, steps: Int? = null) {
        _sensorLevelState.update { current ->
            current.copy(
                tiltXDeg = tiltX,
                tiltYDeg = tiltY,
                stepCount = steps ?: current.stepCount
            )
        }
    }

    fun markNotificationReplied(id: String, replyText: String) {
        _notifications.update { list ->
            list.map {
                if (it.id == id) it.copy(isReplied = true, lastReplyText = replyText) else it
            }
        }
        _latestPopupNotification.update { current ->
            if (current?.id == id) current.copy(isReplied = true, lastReplyText = replyText) else current
        }
        appendLog("✅ Arka ekrandan yanıt gönderildi: \"$replyText\"")
    }

    private val notificationDismissHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }
    private val notificationDismissRunnable = Runnable {
        _latestPopupNotification.value = null
        if (_activeMode.value == RearScreenMode.NOTIFICATION_POPUP) {
            _activeMode.value = RearScreenMode.HOME_LAUNCHER
        }
    }

    fun dismissLatestNotification() {
        notificationDismissHandler.removeCallbacks(notificationDismissRunnable)
        _latestPopupNotification.value = null
        if (_activeMode.value == RearScreenMode.NOTIFICATION_POPUP) {
            _activeMode.value = RearScreenMode.HOME_LAUNCHER
        }
    }

    fun clearNotifications() {
        notificationDismissHandler.removeCallbacks(notificationDismissRunnable)
        _notifications.value = emptyList()
        _latestPopupNotification.value = null
    }

    fun updateMediaState(state: MediaTrackState) {
        val prev = _mediaState.value
        _mediaState.value = state
        // Müzik yeni başladığında bir kez müzik sayfasını göster; sonrasında sayfayı kullanıcı seçer.
        if (state.isPlaying && !prev.isPlaying && state.hasTrack &&
            _settings.value.modMediaLyrics && _currentCall.value == null &&
            _activeMode.value == RearScreenMode.HOME_LAUNCHER
        ) {
            _activeMode.value = RearScreenMode.MEDIA_LYRICS
        }
        if (!state.hasTrack && _activeMode.value == RearScreenMode.MEDIA_LYRICS) {
            _activeMode.value = RearScreenMode.HOME_LAUNCHER
        }
    }

    private val _alarmRinging = MutableStateFlow<AlarmRingingState?>(null)
    val alarmRinging: StateFlow<AlarmRingingState?> = _alarmRinging.asStateFlow()

    fun setAlarmRinging(state: AlarmRingingState?) {
        _alarmRinging.value = state
    }

    fun updateBatteryTelemetry(telemetry: BatteryTelemetry) {
        _batteryTelemetry.value = telemetry
    }

    fun updateDetectedDisplays(displays: List<RearDisplayHardwareInfo>) {
        _detectedDisplays.value = displays
    }

    fun setShizukuReady(ready: Boolean) {
        _shizukuReady.value = ready
    }

    fun setNotificationListenerConnected(connected: Boolean) {
        _notificationListenerConnected.value = connected
    }

    fun setPhoneFaceDown(faceDown: Boolean) {
        _isPhoneFaceDown.value = faceDown
    }

    fun appendLog(entry: String) {
        _statusLog.update { current ->
            (listOf(entry) + current).take(40)
        }
    }
}


