package com.mirearultra.pro.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.mirearultra.pro.core.AlarmRingingState
import com.mirearultra.pro.core.CallEvent
import com.mirearultra.pro.core.CallStatus
import com.mirearultra.pro.core.ContactResolver
import com.mirearultra.pro.core.MediaTrackState
import com.mirearultra.pro.core.RearDisplayEngine
import com.mirearultra.pro.core.RearNotificationItem
import com.mirearultra.pro.core.RearScreenMode
import com.mirearultra.pro.core.RearStateRepository

class RearNotificationService : NotificationListenerService() {

    companion object {
        private var instance: RearNotificationService? = null

        fun mediaPlayPause() {
            val controller = instance?.getPrimaryMediaController() ?: return
            val state = controller.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        }

        fun mediaNext() {
            instance?.getPrimaryMediaController()?.transportControls?.skipToNext()
        }

        fun mediaPrevious() {
            instance?.getPrimaryMediaController()?.transportControls?.skipToPrevious()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        RearStateRepository.init(this)
        RearStateRepository.setNotificationListenerConnected(true)
        RearStateRepository.appendLog("🔔 Bildirim & Çağrı Yakalayıcı Servisi bağlandı.")
        registerMediaSessionListener()
        refreshMediaSessions()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        unregisterMediaSessionListener()
        instance = null
        RearStateRepository.setNotificationListenerConnected(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName?.lowercase().orEmpty()
        if (pkg == packageName || pkg == "android" || pkg == "com.android.systemui") return

        val notif = sbn.notification ?: return
        val extras = notif.extras

        val isDialerPkg = pkg.contains("dialer") || pkg.contains("incallui") ||
            pkg.contains("telecom") || pkg.contains("contacts") || pkg.contains("phone")
        val isWhatsApp = pkg.contains("whatsapp")
        val isVoipPkg = pkg.contains("telegram") || pkg.contains("skype") || pkg.contains("viber") || pkg.contains("discord") || pkg.contains("teams")

        // Missed call check - do NOT treat missed calls as incoming ringing calls
        val isMissedCall = notif.category == Notification.CATEGORY_MISSED_CALL ||
            notif.category == "missed_call" ||
            sbn.tag?.contains("missed", ignoreCase = true) == true ||
            (notif.actions?.any {
                val actTitle = it.title?.toString().orEmpty()
                actTitle.contains("Geri ara", ignoreCase = true) || actTitle.contains("Callback", ignoreCase = true)
            } == true)

        val hasCallStyle = extras?.getString(Notification.EXTRA_TEMPLATE)?.contains("CallStyle", ignoreCase = true) == true ||
            extras?.containsKey("android.callType") == true ||
            extras?.containsKey("android.callPerson") == true ||
            extras?.containsKey("android.answerIntent") == true ||
            extras?.containsKey("android.declineIntent") == true

        val hasCallAnswerAction = notif.actions?.any { action ->
            action.remoteInputs.isNullOrEmpty() && run {
                val label = action.title?.toString()?.lowercase().orEmpty()
                label.contains("cevap") || label.contains("yanıt") || label.contains("answer") ||
                label.contains("accept") || label.contains("katıl") || label == "aç"
            }
        } == true

        val hasCallDeclineAction = notif.actions?.any { action ->
            val label = action.title?.toString()?.lowercase().orEmpty()
            label.contains("reddet") || label.contains("kapat") || label.contains("decline") ||
            label.contains("reject") || label.contains("end") || label.contains("yoksay") ||
            label.contains("dismiss") || label.contains("iptal")
        } == true

        val hasCallChannel = (notif.channelId?.contains("call", true) == true) ||
                             (notif.channelId?.contains("voip", true) == true)

        val isWhatsAppCall = isWhatsApp && (
            notif.category == Notification.CATEGORY_CALL ||
            notif.category == "call" ||
            hasCallStyle ||
            extras?.containsKey("video_call") == true ||
            extras?.containsKey("call_id") == true ||
            extras?.containsKey("call_link") == true ||
            extras?.containsKey("isGroupCall") == true ||
            hasCallChannel ||
            hasCallAnswerAction ||
            notif.fullScreenIntent != null ||
            (sbn.isOngoing && (hasCallAnswerAction || hasCallDeclineAction || notif.actions?.isNotEmpty() == true))
        )

        val isIncomingCall = !isMissedCall && (
            isWhatsAppCall ||
            notif.category == Notification.CATEGORY_CALL ||
            notif.category == "call" ||
            hasCallStyle ||
            hasCallChannel ||
            (hasCallAnswerAction && hasCallDeclineAction) ||
            (notif.fullScreenIntent != null && (isDialerPkg || isWhatsApp || isVoipPkg))
        )

        if (isIncomingCall) {
            handleCallNotification(sbn, if (isWhatsApp) "WhatsApp" else resolveAppLabel(pkg))
            return
        }

        // Grup özeti bildirimleri ("3 yeni mesaj") gerçek mesaj değildir, tekrar oluşturur
        if (notif.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        var title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        var text = (extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras?.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim().orEmpty()

        // MessagingStyle (WhatsApp, Telegram, Mesajlar...): son mesajı ve gönderenini doğru al
        @Suppress("DEPRECATION")
        val styled = extras?.getParcelableArray(Notification.EXTRA_MESSAGES)
        val lastMsg = styled?.lastOrNull() as? android.os.Bundle
        if (lastMsg != null) {
            val msgText = lastMsg.getCharSequence("text")?.toString()?.trim().orEmpty()
            val msgSender = lastMsg.getCharSequence("sender")?.toString()?.trim()
                ?: (lastMsg.get("sender_person") as? android.app.Person)?.name?.toString()?.trim()
            val convTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
            if (msgText.isNotBlank()) {
                if (!convTitle.isNullOrBlank()) {
                    // Grup sohbeti: başlık grup adı, mesaj "Kişi: metin"
                    title = convTitle
                    text = if (!msgSender.isNullOrBlank() && msgSender != convTitle) "$msgSender: $msgText" else msgText
                } else {
                    if (!msgSender.isNullOrBlank()) title = msgSender
                    text = msgText
                }
            }
        }

        if (title.isBlank() && text.isBlank()) return

        // Başlık bir telefon numarasıysa rehberden kişi adını bul (izin verildiyse)
        if (looksLikePhoneNumber(title)) {
            ContactResolver.lookupName(this, title)?.let { title = it }
        }

        val appName = resolveAppLabel(pkg)

        // 0. Çalan alarm (Saat uygulaması): arka ekranda Ertele / Kapat göster
        if (notif.category == Notification.CATEGORY_ALARM &&
            (pkg.contains("clock", ignoreCase = true) || pkg.contains("alarm", ignoreCase = true))
        ) {
            handleAlarmNotification(sbn, title.ifBlank { "Alarm" })
            return
        }

        // 2. Check if this is a Live Navigation or Delivery/Ride-Hailing notification
        val lowerPkg = pkg.lowercase()
        // Navigasyon sadece gerçekten başladığında (devam eden rota bildirimi) arka ekranda sayfa olarak görünür.
        if (isNavigationNotification(sbn)) {
            RearStateRepository.updateLiveActivity {
                it.copy(
                    navActive = true,
                    navApp = appName,
                    navManeuver = title.ifBlank { "Rota devam ediyor" },
                    navStreet = text,
                    navEta = ""
                )
            }
            return
        }

        // 3. Refresh Media Session if it's a media notification
        if (notif.category == Notification.CATEGORY_TRANSPORT || extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true) {
            refreshMediaSessions()
            return
        }

        // Ignore ongoing background service notifications
        if (sbn.isOngoing) return

        // 3. Extract Direct Reply (RemoteInput) if available (WhatsApp, Telegram, Instagram, Messages, etc.)
        var replyIntent: PendingIntent? = null
        var foundRemoteInput: RemoteInput? = null
        notif.actions?.forEach { action ->
            val remoteInputs = action.remoteInputs
            if (!remoteInputs.isNullOrEmpty() && action.actionIntent != null) {
                replyIntent = action.actionIntent
                foundRemoteInput = remoteInputs.first()
            }
        }

        val item = RearNotificationItem(
            id = sbn.key ?: "${pkg}_${sbn.id}",
            packageName = pkg,
            appName = appName,
            sender = title.ifBlank { appName },
            message = text,
            timestamp = sbn.postTime,
            replyIntent = replyIntent,
            remoteInput = foundRemoteInput
        )

        RearStateRepository.postNotification(item)

        // Smart Battery Optimization: Don't wake rear screen for regular notifications if user is actively using unlocked front screen
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        val power = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val isFrontActivelyUsed = power.isInteractive && !keyguard.isKeyguardLocked && !RearStateRepository.isPhoneFaceDown.value
        val skipWake = RearStateRepository.settings.value.skipRearWakeWhenFrontActive && isFrontActivelyUsed && !item.isVip

        if (!skipWake &&
            RearStateRepository.settings.value.autoWakeRearOnNotification &&
            RearStateRepository.currentCall.value == null
        ) {
            RearDisplayEngine.launchRearExperience(this, RearScreenMode.NOTIFICATION_POPUP)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val notif = sbn.notification
        val pkg = sbn.packageName?.lowercase().orEmpty()
        val isCallNotification = notif?.category == Notification.CATEGORY_CALL ||
            notif?.category == "call" ||
            pkg.contains("dialer") || pkg.contains("incallui") ||
            (pkg.contains("whatsapp") && (sbn.tag?.contains("call", true) == true || sbn.id == 4 || notif?.fullScreenIntent != null || notif?.category == Notification.CATEGORY_CALL || notif?.extras?.containsKey("video_call") == true))

        if (isCallNotification && RearStateRepository.currentCall.value != null) {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            val callState = tm?.callState ?: android.telephony.TelephonyManager.CALL_STATE_IDLE
            if (callState == android.telephony.TelephonyManager.CALL_STATE_IDLE) {
                RearStateRepository.onCallEnded()
            }
        }
        if (notif?.category == Notification.CATEGORY_ALARM) {
            RearStateRepository.setAlarmRinging(null)
        }
        if (isNavigationNotification(sbn)) {
            RearStateRepository.updateLiveActivity { it.copy(navActive = false) }
            if (RearStateRepository.activeMode.value == RearScreenMode.LIVE_ACTIVITIES) {
                RearStateRepository.setMode(RearScreenMode.HOME_LAUNCHER)
            }
        }
    }

    private fun isNavigationNotification(sbn: StatusBarNotification): Boolean {
        val pkg = sbn.packageName?.lowercase().orEmpty()
        val notif = sbn.notification ?: return false
        if (notif.category == Notification.CATEGORY_NAVIGATION) return true
        val navApp = pkg.contains("maps") || pkg.contains("waze") || pkg.contains("navi")
        return navApp && sbn.isOngoing
    }

    private fun handleAlarmNotification(sbn: StatusBarNotification, label: String) {
        val actions = sbn.notification.actions.orEmpty()
        var snoozePi: PendingIntent? = null
        var dismissPi: PendingIntent? = null
        actions.forEach { action ->
            val t = action.title?.toString()?.lowercase().orEmpty()
            when {
                t.contains("ertele") || t.contains("snooze") -> snoozePi = action.actionIntent
                t.contains("kapat") || t.contains("durdur") || t.contains("dismiss") || t.contains("stop") ->
                    dismissPi = action.actionIntent
            }
        }
        if (snoozePi == null && dismissPi == null) {
            when {
                actions.size >= 2 -> { snoozePi = actions[0].actionIntent; dismissPi = actions[1].actionIntent }
                actions.size == 1 -> dismissPi = actions[0].actionIntent
            }
        }
        RearStateRepository.setAlarmRinging(AlarmRingingState(label, snoozePi, dismissPi))
        RearDisplayEngine.launchRearExperience(this, RearStateRepository.activeMode.value)
    }

    private fun handleCallNotification(
        sbn: StatusBarNotification,
        appName: String
    ) {
        val notif = sbn.notification ?: return
        val extras = notif.extras
        val pkg = sbn.packageName?.lowercase().orEmpty()
        val isWhatsApp = pkg.contains("whatsapp") || appName.contains("WhatsApp", ignoreCase = true)

        val tm = getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
        val callState = tm?.callState ?: android.telephony.TelephonyManager.CALL_STATE_IDLE

        // Eğer GSM telefonu açılmışsa (OFFHOOK) veya sürmekte olan arama bildirimi ise:
        if (!isWhatsApp && callState == android.telephony.TelephonyManager.CALL_STATE_OFFHOOK) {
            RearStateRepository.onCallEnded()
            if (RearStateRepository.activeMode.value == RearScreenMode.INCOMING_CALL) {
                RearStateRepository.setMode(RearScreenMode.HOME_LAUNCHER)
            }
            return
        }

        // 1. Caller Name Extraction
        var callerName = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        if (callerName.isBlank()) {
            callerName = extras?.getString("android.title")?.trim().orEmpty()
        }
        if (callerName.isBlank() && android.os.Build.VERSION.SDK_INT >= 28) {
            @Suppress("DEPRECATION")
            val person = extras?.getParcelable("android.callPerson") as? android.app.Person
            callerName = person?.name?.toString()?.trim().orEmpty()
        }
        if (callerName.isBlank()) {
            callerName = extras?.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim().orEmpty()
        }
        if (callerName.isBlank()) {
            callerName = (extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras?.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim().orEmpty()
        }
        if (looksLikePhoneNumber(callerName)) {
            ContactResolver.lookupName(this, callerName)?.let { callerName = it }
        }
        if (callerName.isBlank()) {
            callerName = if (isWhatsApp) "WhatsApp Arayan" else "Gelen Arama"
        }

        // 2. Subtitle / Phone Number extraction
        var subtitle = (extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras?.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras?.getCharSequence(Notification.EXTRA_SUB_TEXT))?.toString()?.trim().orEmpty()
        if (isWhatsApp) {
            val isVideo = extras?.getBoolean("video_call", false) == true
            if (subtitle.isBlank() || subtitle.equals(callerName, ignoreCase = true)) {
                subtitle = if (isVideo) "WhatsApp Görüntülü Arama" else "WhatsApp Sesli Arama"
            }
        } else if (subtitle.isBlank()) {
            subtitle = appName
        }

        // 3. PendingIntent Extraction (Answer & Decline)
        var answerPi: PendingIntent? = null
        var declinePi: PendingIntent? = null

        // Check CallStyle extras first (API 31+)
        @Suppress("DEPRECATION")
        val callAnswerFromExtra = if (android.os.Build.VERSION.SDK_INT >= 33) {
            extras?.getParcelable("android.answerIntent", PendingIntent::class.java)
        } else {
            extras?.getParcelable("android.answerIntent") as? PendingIntent
        }
        @Suppress("DEPRECATION")
        val callDeclineFromExtra = if (android.os.Build.VERSION.SDK_INT >= 33) {
            extras?.getParcelable("android.declineIntent", PendingIntent::class.java)
        } else {
            extras?.getParcelable("android.declineIntent") as? PendingIntent
        }
        if (callAnswerFromExtra != null) answerPi = callAnswerFromExtra
        if (callDeclineFromExtra != null) declinePi = callDeclineFromExtra

        val actions = notif.actions.orEmpty()
        actions.forEach { action ->
            val label = action.title?.toString()?.lowercase().orEmpty()
            val isQuickReply = !action.remoteInputs.isNullOrEmpty()
            if (!isQuickReply) {
                if (answerPi == null && (
                    label.contains("cevap") || label.contains("yanıt") || label.contains("answer") ||
                    label.contains("accept") || label.contains("katıl") || label == "aç"
                )) {
                    answerPi = action.actionIntent
                }
                if (declinePi == null && (
                    label.contains("reddet") || label.contains("kapat") || label.contains("decline") ||
                    label.contains("reject") || label.contains("end") || label.contains("yoksay") ||
                    label.contains("dismiss") || label.contains("iptal")
                )) {
                    declinePi = action.actionIntent
                }
            }
        }

        // Fallback when actions have icons or custom labels: Action 0 = Decline, Action 1 = Answer
        val directActions = actions.filter { it.remoteInputs.isNullOrEmpty() && it.actionIntent != null }
        if (directActions.isNotEmpty()) {
            if (declinePi == null && answerPi == null && directActions.size >= 2) {
                declinePi = directActions[0].actionIntent
                answerPi = directActions[1].actionIntent
            } else if (answerPi == null && directActions.size >= 2) {
                answerPi = directActions.firstOrNull { it.actionIntent != declinePi }?.actionIntent
            } else if (declinePi == null && directActions.isNotEmpty()) {
                declinePi = directActions.firstOrNull { it.actionIntent != answerPi }?.actionIntent
            }
        }

        // Fallback for answer: if still null, use fullScreenIntent to open incoming call UI
        if (answerPi == null && notif.fullScreenIntent != null) {
            answerPi = notif.fullScreenIntent
        }

        val callEvent = CallEvent(
            callerName = callerName,
            phoneNumber = subtitle,
            sourceApp = if (isWhatsApp) "WhatsApp" else appName,
            status = CallStatus.RINGING,
            answerIntent = answerPi,
            declineIntent = declinePi
        )

        RearStateRepository.appendLog("📞 GELEN ARAMA YAKALANDI: $callerName ($appName), AnswerPi=${answerPi != null}, DeclinePi=${declinePi != null}")
        RearStateRepository.onIncomingCall(callEvent)
        RearDisplayEngine.launchRearExperience(this, RearScreenMode.INCOMING_CALL)
    }

    private val sessionCallbacks = mutableMapOf<MediaController, MediaController.Callback>()
    private val sessionHandler = Handler(Looper.getMainLooper())

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        attachSessionCallbacks(controllers.orEmpty())
        refreshMediaSessions()
    }

    private fun attachSessionCallbacks(controllers: List<MediaController>) {
        sessionCallbacks.forEach { (c, cb) -> runCatching { c.unregisterCallback(cb) } }
        sessionCallbacks.clear()
        controllers.forEach { c ->
            val cb = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) { refreshMediaSessions() }
                override fun onMetadataChanged(metadata: MediaMetadata?) { refreshMediaSessions() }
                override fun onSessionDestroyed() { refreshMediaSessions() }
            }
            runCatching { c.registerCallback(cb, sessionHandler) }
            sessionCallbacks[c] = cb
        }
    }

    private fun registerMediaSessionListener() {
        runCatching {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val cn = ComponentName(this, RearNotificationService::class.java)
            msm.addOnActiveSessionsChangedListener(sessionsListener, cn, sessionHandler)
            attachSessionCallbacks(msm.getActiveSessions(cn))
        }
    }

    private fun unregisterMediaSessionListener() {
        runCatching {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            msm.removeOnActiveSessionsChangedListener(sessionsListener)
        }
        sessionCallbacks.forEach { (c, cb) -> runCatching { c.unregisterCallback(cb) } }
        sessionCallbacks.clear()
    }

    fun refreshMediaSessions() {
        runCatching {
            val controllers = (getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager)
                .getActiveSessions(ComponentName(this, RearNotificationService::class.java))
            val primary = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: controllers.firstOrNull()
            if (primary == null) {
                RearStateRepository.updateMediaState(MediaTrackState())
                return
            }
            val meta = primary.metadata
            val playback = primary.playbackState

            val title = (meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)).orEmpty()
            val artist = (meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)).orEmpty()
            val art = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            val duration = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
            val position = playback?.position?.coerceAtLeast(0L) ?: 0L
            val isPlaying = playback?.state == PlaybackState.STATE_PLAYING

            RearStateRepository.updateMediaState(
                MediaTrackState(
                    title = title,
                    artist = artist,
                    appName = resolveAppLabel(primary.packageName),
                    packageName = primary.packageName,
                    isPlaying = isPlaying,
                    progressPercent = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                    positionMs = position,
                    durationMs = duration,
                    positionUpdateTime = playback?.lastPositionUpdateTime ?: System.currentTimeMillis(),
                    playbackSpeed = playback?.playbackSpeed ?: 1f,
                    artwork = art
                )
            )
        }
    }

    private fun getPrimaryMediaController(): MediaController? {
        return runCatching {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val list = msm.getActiveSessions(ComponentName(this, RearNotificationService::class.java))
            list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
        }.getOrNull()
    }

    private fun resolveAppLabel(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Throwable) {
            when {
                packageName.contains("whatsapp") -> "WhatsApp"
                packageName.contains("telegram") -> "Telegram"
                packageName.contains("instagram") -> "Instagram"
                packageName.contains("mms") || packageName.contains("messaging") -> "Mesajlar"
                packageName.contains("incallui") || packageName.contains("dialer") -> "Telefon"
                else -> packageName.substringAfterLast('.')
            }
        }
    }

    private fun looksLikePhoneNumber(s: String): Boolean {
        val digits = s.count { it.isDigit() }
        return digits >= 6 && s.all { it.isDigit() || it in "+-() ." }
    }
}


