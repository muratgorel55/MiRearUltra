package com.mirearultra.pro.rear

import android.animation.ValueAnimator
import android.app.ActivityOptions
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.net.Uri
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import java.io.File
import com.mirearultra.pro.core.RearWallpaperTheme
import com.mirearultra.pro.core.RearI18n
import com.mirearultra.pro.core.RearI18n.I18nKey
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.AlarmClock
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.mirearultra.pro.core.CallActionController
import com.mirearultra.pro.core.CallStatus
import com.mirearultra.pro.core.RearDisplayEngine
import com.mirearultra.pro.core.RearScreenMode
import com.mirearultra.pro.core.RearStateRepository
import com.mirearultra.pro.core.RearVoiceRecorderController
import com.mirearultra.pro.core.SpeedDialContact
import com.mirearultra.pro.service.RearNotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Xiaomi 17 Pro Max ikinci arka ekranı (Display #1, 976x596) için
 * donanım hızlandırmalı, 120Hz LTPO saf Canvas arayüzü.
 *
 * Özellikler:
 *  - Saat: Sade dijital saat, tarih, güncel adım ve pil durumu
 *  - Müzik: YouTube Music ve medya çalarken dönen plak teması ve dokunulmadığında otomatik ambient karartma
 *  - Navigasyon: Canlı dönüş/sokak yönlendirmesi
 *  - Hızlı Arama: Tek dokunuşla hoparlörden arama başlatan rehber kartları
 *  - Günlük Kartlar: Adım ve kalori özeti + hızlı 5/10/15 dk sayaç butonları
 *  - Araçlar: Fener aç/kapat, ses kaydedici ve su terazisi
 *  - Alarm: Otantik Xiaomi HyperOS tarzı saat seçici, açma/kapatma anahtarı ve butonlar
 *
 * Soldan sağa kaydırma: Xiaomi Leica arka kamera modunu açar (arka ekrandan hemen kaybolmaz).
 * Gelen arama, mesaj veya çalan alarm: Arka plan bulanıklaşır, üstte aksiyon kartı belirir.
 */
class NativeHyperOsRearView(
    context: Context,
    private val isPreviewOnFront: Boolean = false
) : View(context) {

    private enum class Page {
        CLOCK,
        MUSIC,
        NAV,
        SPEED_DIAL,
        TOOLS,
        MINI_GAME
    }

    // Renk paleti
    private val colBgTop = Color.rgb(13, 15, 20)
    private val colBgBottom = Color.rgb(4, 5, 8)
    private val colText = Color.rgb(244, 245, 247)
    private val colTextDim = Color.rgb(140, 145, 156)
    private val colAccent = Color.rgb(143, 184, 255)
    private val colGreen = Color.rgb(50, 215, 75)
    private val colRed = Color.rgb(255, 69, 58)
    private val colYtm = Color.rgb(255, 23, 68)
    private val colMiOrange = Color.rgb(255, 110, 0)
    private val colSurface = Color.argb(40, 255, 255, 255)
    private val colCyan = Color.rgb(90, 200, 250)
    private val colGold = Color.rgb(255, 159, 10)
    private val colLeica = Color.rgb(255, 36, 66)

    private val mainHandler = Handler(Looper.getMainLooper())
    private var stateScope: CoroutineScope? = null

    // Dikey sayfa durumu
    private var currentPage = Page.CLOCK
    private var animPos = 0f
    private var pageAnimator: ValueAnimator? = null
    private var selfSetMode: RearScreenMode? = null

    // Dokunma ve boşta kalma takibi
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var lastMoveY = 0f
    private var dragMode = 0 // 0 yok, 1 dikey, 2 yatay (kamera)
    private var dragDx = 0f
    private var lastTouchTimestamp = SystemClock.uptimeMillis()

    private data class HitTarget(val rect: RectF, val action: () -> Unit)
    private val hitTargets = mutableListOf<HitTarget>()

    // Alarm sayfası durumu
    private var alarmHour = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1) % 24
    private var alarmMinute = 0
    private var alarmStatus: String? = null

    // Fener ve Gizlilik durumu
    private var isTorchOn = false
    private var messageTemporarilyUnmasked = false

    // Üst Perde (Notification Shade) Durumu
    private var isShadeOpen = false
    private var shadeDragProgress = 0f // 0f kapalı, 1f tam açık

    // Teleprompter durumu
    private var isPrompterPlaying = false
    private var prompterScrollY = 0f

    // LED Tabela & SOS durumu
    private var ledScrollX = 0f
    private var isSosStrobeOn = false
    private var sosStrobeState = false

    // Retro Pong Mini Oyun Durumu
    private var pongBallX = 220f
    private var pongBallY = 160f
    private var pongBallVx = 5f
    private var pongBallVy = 4f
    private var pongPaddleX = 200f
    private var pongPaddleW = 100f
    private var pongScore = 0
    private var pongGameOver = false

    private val numberFormat = NumberFormat.getIntegerInstance(Locale("tr", "TR"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale("tr", "TR"))
    private val dateFormat = SimpleDateFormat("d MMMM EEEE", Locale("tr", "TR"))
    private val dayFormat = SimpleDateFormat("EEEE", Locale("tr", "TR"))

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val clipPath = Path()
    private val rect = RectF()
    private val tmpPath = Path()
    private val shaderMatrix = Matrix()
    private var blurBitmap: Bitmap? = null
    private var cachedCustomWallpaperPath: String? = null
    private var cachedCustomWallpaperBmp: Bitmap? = null

    // Plak ve karartma animasyonu
    private val spinTicker = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow) return

            // Pong animasyon adımı
            if (currentPage == Page.MINI_GAME && !pongGameOver) {
                stepPongGame()
            }

            invalidate()
            if (shouldKeepTicker()) {
                mainHandler.postDelayed(this, if (isDimmedNow()) 70L else 25L)
            }
        }
    }

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            invalidate()
        }
    }

    private val autoCloseMessage = Runnable {
        if (RearStateRepository.activeMode.value == RearScreenMode.NOTIFICATION_POPUP) {
            switchMode(RearScreenMode.HOME_LAUNCHER)
        }
    }

    // ---------------------------------------------------------------- sayfalar

    private fun pages(): List<Page> {
        val s = RearStateRepository.settings.value
        return buildList {
            add(Page.CLOCK)
            if (s.modMediaLyrics) add(Page.MUSIC)
            if (s.modLiveActivities) add(Page.NAV)
            if (s.modSpeedDial) add(Page.SPEED_DIAL)
            if (s.modCyberTools) add(Page.TOOLS)
            if (s.modMiniGame) add(Page.MINI_GAME)
        }
    }

    private fun pageForMode(mode: RearScreenMode): Page? = when (mode) {
        RearScreenMode.HOME_LAUNCHER -> Page.CLOCK
        RearScreenMode.MEDIA_LYRICS -> Page.MUSIC
        RearScreenMode.LIVE_ACTIVITIES -> Page.NAV
        RearScreenMode.SPEED_DIAL -> Page.SPEED_DIAL
        RearScreenMode.TOOLS_HUB -> Page.TOOLS
        RearScreenMode.MINI_GAME -> Page.MINI_GAME
        else -> null
    }

    private fun modeForPage(page: Page): RearScreenMode = when (page) {
        Page.CLOCK -> RearScreenMode.HOME_LAUNCHER
        Page.MUSIC -> RearScreenMode.MEDIA_LYRICS
        Page.NAV -> RearScreenMode.LIVE_ACTIVITIES
        Page.SPEED_DIAL -> RearScreenMode.SPEED_DIAL
        Page.TOOLS -> RearScreenMode.TOOLS_HUB
        Page.MINI_GAME -> RearScreenMode.MINI_GAME
    }

    private fun switchMode(mode: RearScreenMode) {
        selfSetMode = mode
        RearStateRepository.setMode(mode)
        mainHandler.post { selfSetMode = null }
    }

    private fun animateTo(page: Page) {
        val list = pages()
        val idx = list.indexOf(page)
        if (idx < 0) return
        currentPage = page
        lastTouchTimestamp = SystemClock.uptimeMillis()
        pageAnimator?.cancel()
        pageAnimator = ValueAnimator.ofFloat(animPos, idx.toFloat()).apply {
            duration = 260L
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener {
                animPos = it.animatedValue as Float
                invalidate()
            }
            start()
        }
        restartSpinIfNeeded()
    }

    private fun shouldSpin(): Boolean =
        currentPage == Page.MUSIC && RearStateRepository.mediaState.value.isPlaying &&
            RearStateRepository.currentCall.value == null && RearStateRepository.alarmRinging.value == null

    private fun isDimmedNow(): Boolean {
        if (currentPage != Page.MUSIC || !RearStateRepository.mediaState.value.isPlaying) return false
        return (SystemClock.uptimeMillis() - lastTouchTimestamp) > 8_000L
    }

    private fun shouldKeepTicker(): Boolean {
        return shouldSpin() ||
                (currentPage == Page.MUSIC && RearStateRepository.mediaState.value.isPlaying) ||
                (currentPage == Page.MINI_GAME && !pongGameOver)
    }

    private fun restartSpinIfNeeded() {
        mainHandler.removeCallbacks(spinTicker)
        if (isAttachedToWindow && shouldKeepTicker()) mainHandler.post(spinTicker)
    }

    // ---------------------------------------------------------------- yaşam döngüsü

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastTouchTimestamp = SystemClock.uptimeMillis()
        runCatching {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            context.registerReceiver(timeReceiver, filter)
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        stateScope = scope

        scope.launch {
            RearStateRepository.activeMode.collect { mode ->
                if (mode == selfSetMode) return@collect
                val target = pageForMode(mode)
                if (target != null && target != currentPage && pages().contains(target)) {
                    animateTo(target)
                } else {
                    invalidate()
                }
            }
        }
        scope.launch {
            RearStateRepository.latestPopupNotification.collect { item ->
                mainHandler.removeCallbacks(autoCloseMessage)
                messageTemporarilyUnmasked = false
                if (item != null) mainHandler.postDelayed(autoCloseMessage, 20_000L)
                invalidate()
            }
        }
        scope.launch {
            RearStateRepository.isEarProximityCovered.collect {
                invalidate()
            }
        }
        scope.launch {
            combine(
                RearStateRepository.currentCall,
                RearStateRepository.alarmRinging,
                RearStateRepository.mediaState,
                RearStateRepository.liveActivityState,
                RearStateRepository.batteryTelemetry
            ) { _, _, _, _, _ -> Unit }.combine(RearStateRepository.sensorLevelState) { _, _ -> Unit }
                .combine(RearStateRepository.settings) { _, _ -> Unit }
                .collect {
                    restartSpinIfNeeded()
                    invalidate()
                }
        }
    }

    override fun onDetachedFromWindow() {
        mainHandler.removeCallbacks(spinTicker)
        mainHandler.removeCallbacks(autoCloseMessage)
        runCatching { context.unregisterReceiver(timeReceiver) }
        pageAnimator?.cancel()
        stateScope?.cancel()
        blurBitmap?.recycle()
        blurBitmap = null
        super.onDetachedFromWindow()
    }

    // ---------------------------------------------------------------- çizim

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        hitTargets.clear()

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 10f || h <= 10f) return

        val cornerRadius = min(w, h) * 0.12f
        rect.set(0f, 0f, w, h)
        clipPath.reset()
        clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        val saveAll = canvas.save()
        canvas.clipPath(clipPath)

        val leftInset = if (isPreviewOnFront) {
            w * 0.27f
        } else {
            val cutoutLeft = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                rootWindowInsets?.displayCutout?.safeInsetLeft?.toFloat() ?: 0f
            } else 0f
            if (cutoutLeft > 10f) cutoutLeft else w * 0.26f
        }
        val contentLeft = leftInset + w * 0.02f
        val contentRight = w - w * 0.07f
        val contentW = (contentRight - contentLeft).coerceAtLeast(100f)

        val list = pages()
        val idx = list.indexOf(currentPage)
        if (idx < 0) {
            currentPage = Page.CLOCK
            animPos = 0f
        }
        animPos = animPos.coerceIn(0f, (list.size - 1).toFloat())

        val call = RearStateRepository.currentCall.value
        val alarm = RearStateRepository.alarmRinging.value
        val popup = RearStateRepository.latestPopupNotification.value
        val settings = RearStateRepository.settings.value
        val isEarCovered = RearStateRepository.isEarProximityCovered.value
        val isPocket = RearStateRepository.isPocketMode.value

        // 1. KULAK YAKINLIK GİZLİLİĞİ VEYA CEP MODU:
        // Önizleme ekranı (isPreviewOnFront) ön ekrandayken ASLA cep modundan veya yakınlık sensöründen dolayı karartılmaz!
        // Telefon masada yüzüstü duruyorsa (isPhoneFaceDown) da karartılmaz.
        val isFaceDown = RearStateRepository.isPhoneFaceDown.value
        if (!isPreviewOnFront && !isFaceDown && ((isEarCovered && settings.earProximityCallPrivacy && call != null) || (isPocket && settings.pocketModeProtection))) {
            canvas.drawColor(Color.BLACK)
            canvas.restoreToCount(saveAll)
            return
        }

        val showAlarm = alarm != null
        // Kullanıcı isteği: Arama açıldıktan/cevaplandıktan sonra arka ekranda görüşme bilgisi KESİNLİKLE görünmesin (gizlilik).
        // Yalnızca gelen arama çalarken (RINGING) ekranda görünsün, açıldığı an saat/ana ekrana dönsün.
        val isRinging = call?.status == CallStatus.RINGING
        val showCall = !showAlarm && call != null && isRinging
        val overlay = showAlarm || showCall // Bildirimler artık tam ekranı kapatmaz; üst perde olarak iner

        if (!overlay) {
            val sx = canvas.save()
            if (dragMode == 2) canvas.translate(dragDx * 0.45f, 0f)
            val allowPageHits = !isShadeOpen && shadeDragProgress < 0.1f
            drawScene(canvas, w, h, contentLeft, contentW, list, withHits = allowPageHits)
            canvas.restoreToCount(sx)
            if (dragMode == 2) drawCameraHint(canvas, w, h, leftInset)

            // Tekil gelen bildirim üst perdesi
            if (popup != null && !isShadeOpen && shadeDragProgress < 0.05f) {
                drawNotificationTopShade(canvas, contentLeft, contentW, h, popup, settings)
            }

            // KULLANICI ÜSTTEN AŞAĞI ÇEKTİĞİNDE: Tam Bildirim Merkezi Perdesi
            if (isShadeOpen || shadeDragProgress > 0.02f) {
                drawNotificationShade(canvas, w, h, contentLeft, contentW)
            }
        } else {
            drawBlurredScene(canvas, w, h, contentLeft, contentW, list)
            when {
                showAlarm -> drawAlarmOverlay(canvas, contentLeft, contentW, h, alarm!!.label,
                    alarm.snoozeIntent, alarm.dismissIntent)
                showCall -> drawCallOverlay(canvas, contentLeft, contentW, h, call!!)
            }
        }

        // Müzik çalarken arka ekrana dokunulmadığında parlaklığı azaltan ambient karartma katmanı
        if (!overlay && currentPage == Page.MUSIC && RearStateRepository.mediaState.value.isPlaying) {
            val idleMs = SystemClock.uptimeMillis() - lastTouchTimestamp
            if (idleMs > 8_000L) {
                val progress = ((idleMs - 8_000L).toFloat() / 2_500f).coerceIn(0f, 1f)
                val alpha = (progress * 185).toInt()
                if (alpha > 0) {
                    fillPaint.color = Color.argb(alpha, 0, 0, 0)
                    canvas.drawRect(0f, 0f, w, h, fillPaint)
                }
            }
        }

        if (isPreviewOnFront) {
            drawPreviewLenses(canvas, leftInset, h)
            strokePaint.color = Color.argb(60, 255, 255, 255)
            strokePaint.strokeWidth = 2.5f
            rect.set(1.5f, 1.5f, w - 1.5f, h - 1.5f)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)
        }
        canvas.restoreToCount(saveAll)
    }

    private fun drawScene(
        canvas: Canvas, w: Float, h: Float, cl: Float, cw: Float,
        list: List<Page>, withHits: Boolean
    ) {
        drawBackdrop(canvas, w, h)
        val base = animPos.toInt().coerceIn(0, list.size - 1)
        val frac = animPos - base

        val s1 = canvas.save()
        canvas.translate(0f, -frac * h)
        drawPage(canvas, list[base], cl, cw, h, withHits && frac < 0.35f)
        canvas.restoreToCount(s1)

        if (frac > 0.01f && base + 1 < list.size) {
            val s2 = canvas.save()
            canvas.translate(0f, (1f - frac) * h)
            drawPage(canvas, list[base + 1], cl, cw, h, withHits && frac > 0.65f)
            canvas.restoreToCount(s2)
        }
        drawPageDots(canvas, w, h, list.size)
    }

    private fun drawDefaultObsidian(canvas: Canvas, w: Float, h: Float, ytmMusic: Boolean) {
        val top = if (ytmMusic) Color.rgb(34, 8, 14) else colBgTop
        bgPaint.shader = LinearGradient(0f, 0f, 0f, h, top, colBgBottom, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        bgPaint.shader = null
    }

    private fun drawBackdrop(canvas: Canvas, w: Float, h: Float) {
        val settings = RearStateRepository.settings.value
        val media = RearStateRepository.mediaState.value
        val ytmMusic = currentPage == Page.MUSIC && isYtm(media.packageName, media.appName)

        when (settings.rearWallpaperTheme) {
            RearWallpaperTheme.MINIMAL_OBSIDIAN -> drawDefaultObsidian(canvas, w, h, ytmMusic)
            RearWallpaperTheme.COSMIC_NEBULA -> {
                val top = if (ytmMusic) Color.rgb(45, 10, 20) else Color.rgb(24, 12, 45)
                val bottom = Color.rgb(6, 4, 14)
                bgPaint.shader = LinearGradient(0f, 0f, w, h, top, bottom, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null
                fillPaint.color = Color.argb(35, 180, 140, 255)
                canvas.drawCircle(w * 0.85f, h * 0.2f, h * 0.45f, fillPaint)
            }
            RearWallpaperTheme.CYBER_LEICA -> {
                val top = if (ytmMusic) Color.rgb(40, 8, 15) else Color.rgb(32, 10, 12)
                val bottom = Color.rgb(8, 8, 10)
                bgPaint.shader = LinearGradient(0f, 0f, 0f, h, top, bottom, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null
                fillPaint.color = Color.argb(180, 255, 23, 68)
                canvas.drawRect(0f, 0f, 4f, h, fillPaint)
            }
            RearWallpaperTheme.NORDIC_AURORA -> {
                val top = if (ytmMusic) Color.rgb(34, 10, 18) else Color.rgb(6, 32, 24)
                val bottom = Color.rgb(4, 10, 8)
                bgPaint.shader = LinearGradient(0f, 0f, w, h, top, bottom, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null
                fillPaint.color = Color.argb(35, 52, 199, 89)
                canvas.drawCircle(w * 0.7f, h * 0.3f, h * 0.5f, fillPaint)
            }
            RearWallpaperTheme.CARBON_FIBER -> {
                val top = Color.rgb(20, 22, 26)
                val bottom = Color.rgb(10, 11, 13)
                bgPaint.shader = LinearGradient(0f, 0f, 0f, h, top, bottom, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null
                strokePaint.color = Color.argb(16, 255, 255, 255)
                strokePaint.strokeWidth = 2f
                var x = -h
                while (x < w + h) {
                    canvas.drawLine(x, 0f, x + h, h, strokePaint)
                    canvas.drawLine(x, h, x + h, 0f, strokePaint)
                    x += 16f
                }
            }
            RearWallpaperTheme.SUNSET_AMBER -> {
                val top = if (ytmMusic) Color.rgb(40, 10, 15) else Color.rgb(38, 20, 8)
                val bottom = Color.rgb(12, 7, 5)
                bgPaint.shader = LinearGradient(0f, 0f, 0f, h, top, bottom, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null
                fillPaint.color = Color.argb(35, 255, 159, 10)
                canvas.drawCircle(w * 0.8f, h * 0.15f, h * 0.4f, fillPaint)
            }
            RearWallpaperTheme.CUSTOM_PHOTO -> {
                val path = settings.customWallpaperPath
                if (!path.isNullOrBlank() && File(path).exists()) {
                    if (cachedCustomWallpaperPath != path || cachedCustomWallpaperBmp == null) {
                        cachedCustomWallpaperBmp?.recycle()
                        cachedCustomWallpaperBmp = runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
                        cachedCustomWallpaperPath = path
                    }
                    val bmp = cachedCustomWallpaperBmp
                    if (bmp != null) {
                        val srcRect = Rect(0, 0, bmp.width, bmp.height)
                        val dstRect = RectF(0f, 0f, w, h)
                        canvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)
                        fillPaint.color = Color.argb(115, 0, 0, 0)
                        canvas.drawRect(0f, 0f, w, h, fillPaint)
                    } else {
                        drawDefaultObsidian(canvas, w, h, ytmMusic)
                    }
                } else {
                    drawDefaultObsidian(canvas, w, h, ytmMusic)
                }
            }
        }
    }

    private fun drawBlurredScene(
        canvas: Canvas, w: Float, h: Float, cl: Float, cw: Float, list: List<Page>
    ) {
        val bw = (w / 14f).roundToInt().coerceAtLeast(8)
        val bh = (h / 14f).roundToInt().coerceAtLeast(8)
        var bmp = blurBitmap
        if (bmp == null || bmp.width != bw || bmp.height != bh) {
            bmp?.recycle()
            bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            blurBitmap = bmp
        }
        bmp.eraseColor(Color.BLACK)
        val c = Canvas(bmp)
        c.scale(bw / w, bh / h)
        val saved = hitTargets.size
        drawScene(c, w, h, cl, cw, list, withHits = false)
        while (hitTargets.size > saved) hitTargets.removeAt(hitTargets.size - 1)
        rect.set(0f, 0f, w, h)
        canvas.drawBitmap(bmp, null, rect, bitmapPaint)
        fillPaint.color = Color.argb(150, 0, 0, 0)
        canvas.drawRect(0f, 0f, w, h, fillPaint)
    }

    private fun drawPreviewLenses(canvas: Canvas, zoneW: Float, h: Float) {
        val cx = zoneW * 0.5f
        val rOuter = min(zoneW * 0.34f, h * 0.2f)
        for (cy in listOf(h * 0.29f, h * 0.71f)) {
            fillPaint.color = Color.rgb(34, 37, 44)
            canvas.drawCircle(cx, cy, rOuter, fillPaint)
            strokePaint.color = Color.argb(80, 255, 255, 255)
            strokePaint.strokeWidth = 2f
            canvas.drawCircle(cx, cy, rOuter, strokePaint)
            fillPaint.color = Color.rgb(7, 8, 11)
            canvas.drawCircle(cx, cy, rOuter * 0.74f, fillPaint)
            fillPaint.color = Color.rgb(18, 28, 46)
            canvas.drawCircle(cx, cy, rOuter * 0.3f, fillPaint)
        }
    }

    private fun drawPageDots(canvas: Canvas, w: Float, h: Float, count: Int) {
        if (count <= 1) return
        val x = w - w * 0.026f
        val gap = h * 0.05f
        val startY = (h - (count - 1) * gap) * 0.5f
        for (i in 0 until count) {
            val cur = abs(animPos - i) < 0.5f
            val dh = if (cur) h * 0.05f else h * 0.016f
            val dw = h * 0.014f
            fillPaint.color = if (cur) Color.argb(235, 255, 255, 255) else Color.argb(70, 255, 255, 255)
            rect.set(x - dw / 2, startY + i * gap - dh / 2, x + dw / 2, startY + i * gap + dh / 2)
            canvas.drawRoundRect(rect, dw, dw, fillPaint)
        }
    }

    private fun drawCameraHint(canvas: Canvas, w: Float, h: Float, leftInset: Float) {
        val progress = (dragDx / (w * 0.22f)).coerceIn(0f, 1f)
        val cx = leftInset * 0.5f
        val cy = h * 0.5f
        val r = h * 0.07f
        strokePaint.color = Color.argb((255 * progress).toInt(), 255, 255, 255)
        strokePaint.strokeWidth = h * 0.008f
        canvas.drawCircle(cx, cy, r, strokePaint)
        canvas.drawCircle(cx, cy, r * 0.45f, strokePaint)
        setText(Typeface.create("sans-serif-medium", Typeface.NORMAL), h * 0.04f,
            Color.argb((255 * progress).toInt(), 255, 255, 255), Paint.Align.CENTER)
        canvas.drawText("Kamera", cx, cy + r + h * 0.07f, textPaint)
    }

    private fun drawPage(canvas: Canvas, page: Page, cl: Float, cw: Float, h: Float, hits: Boolean) {
        when (page) {
            Page.CLOCK -> drawClockPage(canvas, cl, cw, h)
            Page.MUSIC -> drawMusicPage(canvas, cl, cw, h, hits)
            Page.NAV -> drawNavPage(canvas, cl, cw, h, hits)
            Page.SPEED_DIAL -> drawSpeedDialPage(canvas, cl, cw, h, hits)
            Page.TOOLS -> drawToolsPage(canvas, cl, cw, h, hits)
            Page.MINI_GAME -> drawMiniGamePage(canvas, cl, cw, h, hits)
        }
    }

    // ---------------------------------------------------------------- saat sayfası

    private fun drawClockPage(canvas: Canvas, cl: Float, cw: Float, h: Float) {
        val now = Date()
        setText(Typeface.create("sans-serif-thin", Typeface.NORMAL), h * 0.36f, colText, Paint.Align.LEFT)
        canvas.drawText(timeFormat.format(now), cl, h * 0.47f, textPaint)

        setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.065f, colTextDim, Paint.Align.LEFT)
        canvas.drawText(dateFormat.format(now), cl + h * 0.01f, h * 0.57f, textPaint)

        val rawSteps = RearStateRepository.sensorLevelState.value.stepCount
        val steps = if (rawSteps >= 0) rawSteps else {
            context.getSharedPreferences("mirear_steps", Context.MODE_PRIVATE).getInt("last_steps", 0)
        }
        val battery = RearStateRepository.batteryTelemetry.value
        val barW = cw * 0.36f
        val rowY = h * 0.80f

        // Adım (Önbellekten veya donanım sayacından kesintisiz gösterim)
        setText(Typeface.create("sans-serif-medium", Typeface.NORMAL), h * 0.078f, colText, Paint.Align.LEFT)
        canvas.drawText(numberFormat.format(steps), cl + h * 0.01f, rowY, textPaint)
        setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.04f, colTextDim, Paint.Align.LEFT)
        canvas.drawText("adım", cl + h * 0.01f, rowY + h * 0.06f, textPaint)
        drawBar(canvas, cl + h * 0.01f, rowY + h * 0.09f, barW, h * 0.009f,
            if (steps > 0) (steps / 10_000f).coerceIn(0f, 1f) else 0f, colAccent)

        // Pil
        val bx = cl + cw * 0.5f
        setText(Typeface.create("sans-serif-medium", Typeface.NORMAL), h * 0.078f, colText, Paint.Align.LEFT)
        canvas.drawText("%${battery.levelPercent}", bx, rowY, textPaint)
        setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.04f, colTextDim, Paint.Align.LEFT)
        canvas.drawText(if (battery.isCharging) "şarj oluyor" else "pil", bx, rowY + h * 0.06f, textPaint)
        drawBar(canvas, bx, rowY + h * 0.09f, barW, h * 0.009f,
            battery.levelPercent / 100f, if (battery.isCharging) colGreen else colAccent)
    }

    private fun drawBar(canvas: Canvas, x: Float, y: Float, w: Float, bh: Float, progress: Float, color: Int) {
        fillPaint.color = colSurface
        rect.set(x, y, x + w, y + bh)
        canvas.drawRoundRect(rect, bh, bh, fillPaint)
        if (progress > 0f) {
            fillPaint.color = color
            rect.set(x, y, x + w * progress, y + bh)
            canvas.drawRoundRect(rect, bh, bh, fillPaint)
        }
    }

    // ---------------------------------------------------------------- müzik sayfası

    private fun isYtm(pkg: String, app: String): Boolean =
        pkg.contains("youtube.apps.youtube.music", true) || app.contains("YouTube Music", true)

    private fun drawMusicPage(canvas: Canvas, cl: Float, cw: Float, h: Float, hits: Boolean) {
        val media = RearStateRepository.mediaState.value
        val ytm = isYtm(media.packageName, media.appName)
        val accent = if (ytm) colYtm else colAccent

        val discR = h * 0.27f
        val cx = cl + discR + h * 0.01f
        val cy = h * 0.5f
        drawVinyl(canvas, cx, cy, discR, media.artwork, accent, ytm, media.isPlaying)

        val tx = cx + discR + h * 0.07f
        val tw = (cl + cw - tx).coerceAtLeast(120f)

        // Uygulama etiketi
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.04f, accent, Paint.Align.LEFT)
        val appLabel = (if (media.appName.isBlank()) "Müzik" else media.appName).uppercase(Locale("tr", "TR"))
        canvas.drawText(TextUtils.ellipsize(appLabel, textPaint, tw, TextUtils.TruncateAt.END).toString(),
            tx, h * 0.20f, textPaint)

        // Başlık (en fazla 2 satır) ve sanatçı
        val titleH = drawWrapped(canvas, media.title, tx, h * 0.235f, tw, h * 0.072f, colText, 2,
            Typeface.create("sans-serif-medium", Typeface.BOLD))
        setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.050f, colTextDim, Paint.Align.LEFT)
        canvas.drawText(TextUtils.ellipsize(media.artist, textPaint, tw, TextUtils.TruncateAt.END).toString(),
            tx, h * 0.235f + titleH + h * 0.065f, textPaint)

        // İlerleme çubuğu
        val barY = h * 0.66f
        var position = media.positionMs
        if (media.isPlaying) {
            position += ((SystemClock.elapsedRealtime() - media.positionUpdateTime) * media.playbackSpeed).toLong()
        }
        val progress = if (media.durationMs > 0) (position.toFloat() / media.durationMs).coerceIn(0f, 1f) else 0f
        drawBar(canvas, tx, barY, tw, h * 0.01f, progress, accent)

        // Kontroller
        val btnR = h * 0.075f
        val btnY = h * 0.82f
        val prevX = tx + tw * 0.14f
        val playX = tx + tw * 0.50f
        val nextX = tx + tw * 0.86f

        drawControl(canvas, prevX, btnY, btnR, colSurface, hits) { drawSkipIcon(canvas, prevX, btnY, btnR * 0.45f, false) }
            .let { if (hits) addHit(prevX, btnY, btnR * 1.2f) { RearNotificationService.mediaPrevious() } }
        drawControl(canvas, playX, btnY, btnR * 1.2f, accent, hits) {
            if (media.isPlaying) drawPauseIcon(canvas, playX, btnY, btnR * 0.5f, Color.WHITE)
            else drawPlayIcon(canvas, playX, btnY, btnR * 0.5f, Color.WHITE)
        }.let { if (hits) addHit(playX, btnY, btnR * 1.4f) { RearNotificationService.mediaPlayPause() } }
        drawControl(canvas, nextX, btnY, btnR, colSurface, hits) { drawSkipIcon(canvas, nextX, btnY, btnR * 0.45f, true) }
            .let { if (hits) addHit(nextX, btnY, btnR * 1.2f) { RearNotificationService.mediaNext() } }
    }

    private fun drawVinyl(
        canvas: Canvas, cx: Float, cy: Float, r: Float,
        art: Bitmap?, accent: Int, ytm: Boolean, playing: Boolean
    ) {
        val angle = if (playing) (SystemClock.uptimeMillis() % 4800L) / 4800f * 360f else 0f

        // Gölge
        fillPaint.shader = RadialGradient(cx, cy, r * 1.15f,
            intArrayOf(Color.argb(90, 0, 0, 0), Color.TRANSPARENT), floatArrayOf(0.8f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r * 1.15f, fillPaint)
        fillPaint.shader = null

        val save = canvas.save()
        canvas.rotate(angle, cx, cy)

        // Plak gövdesi
        fillPaint.shader = RadialGradient(cx, cy, r,
            intArrayOf(Color.rgb(30, 30, 34), Color.rgb(12, 12, 14), Color.rgb(4, 4, 5)),
            floatArrayOf(0f, 0.7f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r, fillPaint)
        fillPaint.shader = null

        // Oluklar
        strokePaint.strokeWidth = max(1f, r * 0.006f)
        for (i in 0..5) {
            strokePaint.color = Color.argb(if (i % 2 == 0) 36 else 20, 255, 255, 255)
            canvas.drawCircle(cx, cy, r * (0.46f + i * 0.09f), strokePaint)
        }

        // Dönen ışık yansıması
        fillPaint.shader = SweepGradient(cx, cy,
            intArrayOf(0x00FFFFFF, 0x38FFFFFF, 0x00FFFFFF, 0x00FFFFFF, 0x38FFFFFF, 0x00FFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, 0.1f, 0.22f, 0.5f, 0.6f, 0.72f, 1f))
        canvas.drawCircle(cx, cy, r, fillPaint)
        fillPaint.shader = null

        // Orta etiket: albüm kapağı ya da tema rengi
        val labelR = r * 0.40f
        if (art != null && !art.isRecycled) {
            val shader = BitmapShader(art, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val scale = (labelR * 2f) / min(art.width, art.height).toFloat()
            shaderMatrix.reset()
            shaderMatrix.setScale(scale, scale)
            shaderMatrix.postTranslate(cx - art.width * scale / 2f, cy - art.height * scale / 2f)
            shader.setLocalMatrix(shaderMatrix)
            bitmapPaint.shader = shader
            canvas.drawCircle(cx, cy, labelR, bitmapPaint)
            bitmapPaint.shader = null
        } else {
            fillPaint.color = accent
            canvas.drawCircle(cx, cy, labelR, fillPaint)
            if (ytm) drawPlayIcon(canvas, cx, cy, labelR * 0.42f, Color.WHITE)
        }
        strokePaint.color = Color.argb(70, 255, 255, 255)
        strokePaint.strokeWidth = max(1.5f, r * 0.012f)
        canvas.drawCircle(cx, cy, labelR, strokePaint)
        fillPaint.color = Color.rgb(8, 8, 10)
        canvas.drawCircle(cx, cy, r * 0.045f, fillPaint)
        canvas.restoreToCount(save)

        // Plak kolu
        val pivotX = cx + r * 1.02f
        val pivotY = cy - r * 0.92f
        val tipX = if (playing) cx + r * 0.52f else cx + r * 0.95f
        val tipY = if (playing) cy - r * 0.12f else cy - r * 0.55f
        strokePaint.color = Color.argb(230, 200, 204, 212)
        strokePaint.strokeWidth = r * 0.035f
        strokePaint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(pivotX, pivotY, tipX, tipY, strokePaint)
        strokePaint.strokeCap = Paint.Cap.BUTT
        fillPaint.color = Color.rgb(200, 204, 212)
        canvas.drawCircle(pivotX, pivotY, r * 0.07f, fillPaint)
        fillPaint.color = Color.rgb(90, 94, 104)
        canvas.drawCircle(tipX, tipY, r * 0.04f, fillPaint)
    }

    private inline fun drawControl(
        canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int, hits: Boolean, icon: () -> Unit
    ) {
        fillPaint.color = color
        canvas.drawCircle(cx, cy, r, fillPaint)
        icon()
    }

    private fun addHit(cx: Float, cy: Float, r: Float, action: () -> Unit) {
        hitTargets.add(HitTarget(RectF(cx - r, cy - r, cx + r, cy + r), action))
    }

    private fun drawPlayIcon(canvas: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        tmpPath.reset()
        tmpPath.moveTo(cx - s * 0.6f, cy - s)
        tmpPath.lineTo(cx + s, cy)
        tmpPath.lineTo(cx - s * 0.6f, cy + s)
        tmpPath.close()
        fillPaint.color = color
        canvas.drawPath(tmpPath, fillPaint)
    }

    private fun drawPauseIcon(canvas: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        fillPaint.color = color
        rect.set(cx - s * 0.85f, cy - s, cx - s * 0.2f, cy + s)
        canvas.drawRoundRect(rect, s * 0.15f, s * 0.15f, fillPaint)
        rect.set(cx + s * 0.2f, cy - s, cx + s * 0.85f, cy + s)
        canvas.drawRoundRect(rect, s * 0.15f, s * 0.15f, fillPaint)
    }

    private fun drawSkipIcon(canvas: Canvas, cx: Float, cy: Float, s: Float, forward: Boolean) {
        val d = if (forward) 1f else -1f
        tmpPath.reset()
        tmpPath.moveTo(cx - d * s, cy - d * s * 0.5f)
        tmpPath.lineTo(cx + d * s * 0.6f, cy)
        tmpPath.lineTo(cx - d * s, cy + d * s * 0.5f)
        tmpPath.close()
        fillPaint.color = Color.WHITE
        canvas.drawPath(tmpPath, fillPaint)
        val bx = cx + d * s * 0.9f
        rect.set(bx - s * 0.18f, cy - s * 0.8f, bx + s * 0.18f, cy + s * 0.8f)
        canvas.drawRoundRect(rect, s * 0.1f, s * 0.1f, fillPaint)
    }

    // ---------------------------------------------------------------- navigasyon sayfası

    private fun drawNavPage(canvas: Canvas, cl: Float, cw: Float, h: Float, hits: Boolean) {
        val nav = RearStateRepository.liveActivityState.value
        if (!nav.navActive) {
            setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.042f, colAccent, Paint.Align.LEFT)
            canvas.drawText(RearI18n.t(I18nKey.SPEEDOMETER), cl, h * 0.14f, textPaint)

            setText(Typeface.create("sans-serif-thin", Typeface.NORMAL), h * 0.28f, colText, Paint.Align.LEFT)
            canvas.drawText("0", cl, h * 0.48f, textPaint)
            setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.055f, colTextDim, Paint.Align.LEFT)
            canvas.drawText("km/s • GPS Hazır", cl + h * 0.22f, h * 0.44f, textPaint)

            setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.045f, Color.argb(160, 255, 255, 255), Paint.Align.LEFT)
            canvas.drawText("Google Haritalar veya Yandex navigasyonu başlayınca sokak yönlendirmeleri burada görünür.", cl, h * 0.62f, textPaint)

            drawPill(canvas, cl + cw * 0.5f, h * 0.82f, cw * 0.70f, h * 0.18f, colAccent, "🗺️ " + RearI18n.t(I18nKey.START_MAPS), Color.BLACK, h * 0.054f, hits) {
                runCatching {
                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(mapIntent)
                }
            }
            return
        }
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.042f, colAccent, Paint.Align.LEFT)
        canvas.drawText("NAVİGASYON • ${nav.navApp.uppercase(Locale("tr", "TR"))}", cl, h * 0.2f, textPaint)
        val mh = drawWrapped(canvas, nav.navManeuver, cl, h * 0.26f, cw, h * 0.10f, colText, 3,
            Typeface.create("sans-serif-medium", Typeface.BOLD))
        if (nav.navStreet.isNotBlank()) {
            drawWrapped(canvas, nav.navStreet, cl, h * 0.26f + mh + h * 0.04f, cw, h * 0.058f, colTextDim, 2,
                Typeface.create("sans-serif", Typeface.NORMAL))
        }
    }

    // ---------------------------------------------------------------- hızlı arama sayfası

    private fun drawSpeedDialPage(canvas: Canvas, cl: Float, cw: Float, h: Float, hits: Boolean) {
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.045f, colAccent, Paint.Align.LEFT)
        canvas.drawText(RearI18n.t(I18nKey.SPEED_DIAL), cl, h * 0.13f, textPaint)

        setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.038f, colTextDim, Paint.Align.LEFT)
        canvas.drawText(RearI18n.t(I18nKey.SPEED_DIAL_SUBTITLE), cl, h * 0.19f, textPaint)

        val contacts = RearStateRepository.settings.value.speedDialContacts
        if (contacts.isEmpty()) {
            setText(Typeface.create("sans-serif-medium", Typeface.NORMAL), h * 0.056f, colTextDim, Paint.Align.LEFT)
            canvas.drawText("Rehber taranıyor...", cl, h * 0.40f, textPaint)
            setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.040f, Color.argb(130, 255, 255, 255), Paint.Align.LEFT)
            canvas.drawText("En çok aradığınız gerçek kişiler otomatik listelenir.", cl, h * 0.48f, textPaint)
            return
        }

        val top6 = contacts.take(6)
        val cols = 3
        val gapX = cw * 0.025f
        val colW = (cw - (cols - 1) * gapX) / cols.toFloat()
        val startY = h * 0.23f
        val gapY = h * 0.04f
        val rowH = (h * 0.94f - startY - gapY) / 2f

        top6.forEachIndexed { idx, c ->
            val col = idx % cols
            val row = idx / cols
            val cx = cl + col * (colW + gapX)
            val cy = startY + row * (rowH + gapY)
            val cardRect = RectF(cx, cy, cx + colW, cy + rowH)

            // Card background
            fillPaint.color = Color.argb(45, 255, 255, 255)
            canvas.drawRoundRect(cardRect, rowH * 0.22f, rowH * 0.22f, fillPaint)

            // Avatar circle
            val avatarR = rowH * 0.28f
            val ax = cx + avatarR + rowH * 0.12f
            val ay = cy + rowH * 0.5f
            fillPaint.color = Color.argb(65, 143, 184, 255)
            canvas.drawCircle(ax, ay, avatarR, fillPaint)
            val initial = c.name.firstOrNull()?.uppercaseChar()?.toString() ?: "•"
            setText(Typeface.create("sans-serif-medium", Typeface.BOLD), avatarR * 1.05f, colAccent, Paint.Align.CENTER)
            canvas.drawText(initial, ax, ay - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint)

            // Name
            val tx = ax + avatarR + rowH * 0.12f
            val maxTextW = (cx + colW - tx - rowH * 0.08f).coerceAtLeast(10f)
            setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.052f, colText, Paint.Align.LEFT)
            val ellipName = TextUtils.ellipsize(c.name, textPaint, maxTextW, TextUtils.TruncateAt.END).toString()
            canvas.drawText(ellipName, tx, cy + rowH * 0.44f, textPaint)

            // Phone
            val settings = RearStateRepository.settings.value
            val phoneDisplay = if (settings.maskSpeedDialNumbers) maskPhoneNumber(c.phone) else c.phone
            setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.038f, colTextDim, Paint.Align.LEFT)
            val ellipPhone = TextUtils.ellipsize(phoneDisplay, textPaint, maxTextW, TextUtils.TruncateAt.END).toString()
            canvas.drawText(ellipPhone, tx, cy + rowH * 0.76f, textPaint)

            // Tap anywhere on card to call
            if (hits) {
                addRectHit(cx, cy, cx + colW, cy + rowH) {
                    CallActionController.placeCallFromRear(context, c.name, c.phone)
                }
            }
        }
    }

    // ---------------------------------------------------------------- günlük kartlar

    private fun drawDailyCardsPage(canvas: Canvas, cl: Float, cw: Float, h: Float, hits: Boolean) {
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.042f, colAccent, Paint.Align.LEFT)
        canvas.drawText("GÜNLÜK ÖZET", cl, h * 0.14f, textPaint)

        val rawDailySteps = RearStateRepository.sensorLevelState.value.stepCount
        val steps = if (rawDailySteps >= 0) rawDailySteps else {
            context.getSharedPreferences("mirear_steps", Context.MODE_PRIVATE).getInt("last_steps", 0)
        }
        val battery = RearStateRepository.batteryTelemetry.value
        val cardW = (cw - h * 0.04f) * 0.5f
        val cardH = h * 0.72f
        val cardY = h * 0.20f

        // Sol Kart: Adım & Mesafe
        val leftRect = RectF(cl, cardY, cl + cardW, cardY + cardH)
        fillPaint.color = Color.argb(32, 255, 255, 255)
        canvas.drawRoundRect(leftRect, cardH * 0.12f, cardH * 0.12f, fillPaint)

        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.042f, colAccent, Paint.Align.LEFT)
        canvas.drawText("ADIM HEDEFİ", cl + cardW * 0.1f, cardY + cardH * 0.22f, textPaint)

        setText(Typeface.create("sans-serif-light", Typeface.NORMAL), h * 0.15f, colText, Paint.Align.LEFT)
        canvas.drawText(numberFormat.format(steps), cl + cardW * 0.1f, cardY + cardH * 0.52f, textPaint)

        val cal = if (steps > 0) (steps * 0.038f).toInt() else 0
        val km = if (steps > 0) String.format(Locale.US, "%.1f", steps * 0.00075f) else "0.0"
        setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.048f, colTextDim, Paint.Align.LEFT)
        canvas.drawText("$cal kcal • $km km", cl + cardW * 0.1f, cardY + cardH * 0.72f, textPaint)
        drawBar(canvas, cl + cardW * 0.1f, cardY + cardH * 0.84f, cardW * 0.8f, h * 0.012f,
            if (steps > 0) (steps / 10_000f).coerceIn(0f, 1f) else 0f, colAccent)

        // Sağ Kart: Hızlı Sayaç Butonları
        val rx = cl + cardW + h * 0.04f
        val rightRect = RectF(rx, cardY, rx + cardW, cardY + cardH)
        canvas.drawRoundRect(rightRect, cardH * 0.12f, cardH * 0.12f, fillPaint)

        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.042f, colGreen, Paint.Align.LEFT)
        canvas.drawText("HIZLI SAYAÇ", rx + cardW * 0.1f, cardY + cardH * 0.22f, textPaint)

        val pw = cardW * 0.80f
        val ph = cardH * 0.18f
        val px = rx + cardW * 0.5f
        drawPill(canvas, px, cardY + cardH * 0.42f, pw, ph, colSurface, "+5 Dk Sayaç", colText, h * 0.046f, hits) { startTimer(5) }
        drawPill(canvas, px, cardY + cardH * 0.64f, pw, ph, colSurface, "+10 Dk Sayaç", colText, h * 0.046f, hits) { startTimer(10) }
        drawPill(canvas, px, cardY + cardH * 0.86f, pw, ph, colSurface, "+15 Dk Sayaç", colText, h * 0.046f, hits) { startTimer(15) }
    }

    private fun startTimer(minutes: Int) {
        runCatching {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
                putExtra(AlarmClock.EXTRA_MESSAGE, "MiRearUltra $minutes dk")
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    // ---------------------------------------------------------------- araçlar

    private fun drawToolsPage(canvas: Canvas, cl: Float, cw: Float, h: Float, hits: Boolean) {
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.042f, colAccent, Paint.Align.LEFT)
        canvas.drawText(RearI18n.t(I18nKey.QUICK_TOOLS), cl, h * 0.14f, textPaint)

        val cardW = (cw - h * 0.04f) * 0.5f
        val cardH = h * 0.72f
        val cardY = h * 0.20f

        // Sol Kart: Fener
        val leftRect = RectF(cl, cardY, cl + cardW, cardY + cardH)
        fillPaint.color = Color.argb(32, 255, 255, 255)
        canvas.drawRoundRect(leftRect, cardH * 0.12f, cardH * 0.12f, fillPaint)

        val torchActive = isTorchOn
        val torchColor = if (torchActive) Color.rgb(255, 214, 0) else colTextDim
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.045f, torchColor, Paint.Align.LEFT)
        canvas.drawText("🔦 " + RearI18n.t(I18nKey.TORCH), cl + cardW * 0.1f, cardY + cardH * 0.22f, textPaint)

        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.09f, colText, Paint.Align.LEFT)
        canvas.drawText(if (torchActive) "AÇIK" else "KAPALI", cl + cardW * 0.1f, cardY + cardH * 0.50f, textPaint)

        val btnW = cardW * 0.8f
        val btnH = cardH * 0.22f
        drawPill(canvas, cl + cardW * 0.5f, cardY + cardH * 0.78f, btnW, btnH,
            if (torchActive) torchColor else colSurface,
            if (torchActive) "Kapat" else "Feneri Aç",
            if (torchActive) Color.BLACK else colText, h * 0.052f, hits) {
            toggleTorch()
        }

        // Sağ Kart: Ses Kaydedici
        val rx = cl + cardW + h * 0.04f
        val rightRect = RectF(rx, cardY, rx + cardW, cardY + cardH)
        fillPaint.color = Color.argb(32, 255, 255, 255)
        canvas.drawRoundRect(rightRect, cardH * 0.12f, cardH * 0.12f, fillPaint)

        val isRecording = RearVoiceRecorderController.isRecording.value
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.045f,
            if (isRecording) colRed else colAccent, Paint.Align.LEFT)
        canvas.drawText("🎙️ " + RearI18n.t(I18nKey.VOICE_MEMO), rx + cardW * 0.1f, cardY + cardH * 0.22f, textPaint)

        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.085f, colText, Paint.Align.LEFT)
        canvas.drawText(if (isRecording) "KAYDEDİYOR" else "HAZIR", rx + cardW * 0.1f, cardY + cardH * 0.50f, textPaint)

        drawPill(canvas, rx + cardW * 0.5f, cardY + cardH * 0.78f, btnW, btnH,
            if (isRecording) colRed else colSurface,
            if (isRecording) "Kaydı Durdur" else "Kayıt Başlat",
            if (isRecording) Color.WHITE else colText, h * 0.052f, hits) {
            RearVoiceRecorderController.toggleRecording(context)
            invalidate()
        }
    }

    private fun toggleTorch() {
        runCatching {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            val id = cm.cameraIdList.firstOrNull() ?: return
            isTorchOn = !isTorchOn
            cm.setTorchMode(id, isTorchOn)
            invalidate()
        }
    }

    // ---------------------------------------------------------------- YENİ MODÜLLER: Teleprompter, LED Tabela, Otopark/QR, Mini Oyun

    // 1. Kamera Vlog Teleprompter Sayfası
    private fun drawTeleprompterPage(canvas: Canvas, cl: Float, cw: Float, h: Float, hits: Boolean) {
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.042f, colCyan, Paint.Align.LEFT)
        canvas.drawText("VLOG TELEPROMPTER", cl, h * 0.14f, textPaint)

        // Kamera Başlat Butonu
        val camBtnW = cw * 0.34f
        val camBtnH = h * 0.11f
        drawPill(canvas, cl + cw - camBtnW * 0.5f, h * 0.13f, camBtnW, camBtnH, colLeica, "📷 Kamera", Color.WHITE, h * 0.042f, hits) {
            RearDisplayEngine.openXiaomiCameraOnRear(context)
        }

        // Metin Kutusu (Kart)
        val boxY = h * 0.22f
        val boxH = h * 0.56f
        rect.set(cl, boxY, cl + cw, boxY + boxH)
        fillPaint.color = Color.argb(45, 255, 255, 255)
        canvas.drawRoundRect(rect, h * 0.04f, h * 0.04f, fillPaint)

        val settings = RearStateRepository.settings.value
        val script = settings.teleprompterScript.ifBlank { "Metin yok. Uygulama ayarlarından metninizi girebilirsiniz." }

        val saveClip = canvas.save()
        clipPath.reset()
        clipPath.addRoundRect(rect, h * 0.04f, h * 0.04f, Path.Direction.CW)
        canvas.clipPath(clipPath)

        val textTop = boxY + h * 0.06f - (if (isPrompterPlaying) prompterScrollY % (boxH * 3f) else prompterScrollY)
        drawWrapped(canvas, script, cl + h * 0.03f, textTop, cw - h * 0.06f, h * 0.068f, colText, 8,
            Typeface.create("sans-serif-medium", Typeface.BOLD))
        canvas.restoreToCount(saveClip)

        // Kontrol Butonları
        val btnW = cw * 0.44f
        val btnH = h * 0.14f
        val btnY = h * 0.88f
        drawPill(canvas, cl + btnW * 0.5f, btnY, btnW, btnH,
            if (isPrompterPlaying) colMiOrange else colGreen,
            if (isPrompterPlaying) "⏸️ Durdur" else "▶️ Akışı Başlat",
            Color.BLACK, h * 0.048f, hits) {
            isPrompterPlaying = !isPrompterPlaying
            restartSpinIfNeeded()
            invalidate()
        }

        drawPill(canvas, cl + cw - btnW * 0.5f, btnY, btnW, btnH, colSurface, "🔄 Başa Sar", colText, h * 0.048f, hits) {
            prompterScrollY = 0f
            invalidate()
        }
    }

    // 2. Kayan LED Tabela & SOS Sayfası
    private fun drawLedSignPage(canvas: Canvas, cl: Float, cw: Float, h: Float, hits: Boolean) {
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.042f, colGold, Paint.Align.LEFT)
        canvas.drawText("KAYAN LED TABELA & SOS", cl, h * 0.14f, textPaint)

        if (isSosStrobeOn && sosStrobeState) {
            // SOS Acil Durum Flaş Modu
            canvas.drawColor(Color.rgb(255, 36, 66))
            setText(Typeface.create("sans-serif-black", Typeface.BOLD), h * 0.28f, Color.WHITE, Paint.Align.CENTER)
            canvas.drawText("SOS", cl + cw * 0.5f, h * 0.58f, textPaint)
        } else {
            // LED Tabela Kartı
            val signY = h * 0.22f
            val signH = h * 0.48f
            rect.set(cl, signY, cl + cw, signY + signH)
            fillPaint.color = Color.rgb(10, 12, 16)
            canvas.drawRoundRect(rect, h * 0.05f, h * 0.05f, fillPaint)
            strokePaint.color = Color.argb(60, 52, 199, 89)
            strokePaint.strokeWidth = 2f
            canvas.drawRoundRect(rect, h * 0.05f, h * 0.05f, strokePaint)

            val settings = RearStateRepository.settings.value
            val bannerText = settings.marqueeBannerText.ifBlank { "XIAOMI 17 PRO MAX • HOS GELDINIZ" }

            val saveClip = canvas.save()
            clipPath.reset()
            clipPath.addRoundRect(rect, h * 0.05f, h * 0.05f, Path.Direction.CW)
            canvas.clipPath(clipPath)

            setText(Typeface.create("monospace", Typeface.BOLD), h * 0.17f, colGreen, Paint.Align.LEFT)
            val textWidth = textPaint.measureText(bannerText)
            val loopWidth = textWidth + cw * 0.5f
            val posX = cl + cw - (ledScrollX % loopWidth)
            canvas.drawText(bannerText, posX, signY + signH * 0.62f, textPaint)
            canvas.restoreToCount(saveClip)
        }

        // SOS / Normal Mod Butonları
        val btnW = cw * 0.46f
        val btnH = h * 0.18f
        val btnY = h * 0.82f
        drawPill(canvas, cl + btnW * 0.5f, btnY, btnW, btnH,
            if (isSosStrobeOn) colRed else colSurface,
            if (isSosStrobeOn) "🚨 SOS Kapat" else "🚨 SOS Acil Çakar",
            if (isSosStrobeOn) Color.WHITE else colText, h * 0.048f, hits) {
            isSosStrobeOn = !isSosStrobeOn
            restartSpinIfNeeded()
            invalidate()
        }

        val presets = listOf("YOLDAYIM", "TOPLANTIDAYIM", "ACİL")
        drawPill(canvas, cl + cw - btnW * 0.5f, btnY, btnW, btnH, colAccent, "Metni Değiştir", Color.BLACK, h * 0.048f, hits) {
            val nextPreset = presets[(presets.indexOf(RearStateRepository.settings.value.marqueeBannerText) + 1).coerceAtLeast(0) % presets.size]
            RearStateRepository.updateSettings { it.copy(marqueeBannerText = nextPreset) }
            invalidate()
        }
    }

    // 3. Otopark & QR Kartı Sayfası
    private fun drawParkingQrPage(canvas: Canvas, cl: Float, cw: Float, h: Float, hits: Boolean) {
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.042f, colGreen, Paint.Align.LEFT)
        canvas.drawText("OTOPARK NOTU & QR CÜZDAN", cl, h * 0.14f, textPaint)

        val cardW = (cw - h * 0.04f) * 0.5f
        val cardH = h * 0.72f
        val cardY = h * 0.20f

        // Sol Kart: Otopark Konumu
        val leftRect = RectF(cl, cardY, cl + cardW, cardY + cardH)
        fillPaint.color = Color.argb(32, 255, 255, 255)
        canvas.drawRoundRect(leftRect, cardH * 0.12f, cardH * 0.12f, fillPaint)

        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.045f, colGold, Paint.Align.LEFT)
        canvas.drawText("🚗 OTOPARK", cl + cardW * 0.1f, cardY + cardH * 0.22f, textPaint)

        val settings = RearStateRepository.settings.value
        val parkNote = settings.parkingSpotNote.ifBlank { "B2 Katı Sarı Alan Sütun C14" }
        drawWrapped(canvas, parkNote, cl + cardW * 0.1f, cardY + cardH * 0.40f, cardW * 0.8f, h * 0.065f, colText, 3,
            Typeface.create("sans-serif-bold", Typeface.BOLD))

        // Sağ Kart: QR Kod
        val rx = cl + cardW + h * 0.04f
        val rightRect = RectF(rx, cardY, rx + cardW, cardY + cardH)
        fillPaint.color = Color.argb(32, 255, 255, 255)
        canvas.drawRoundRect(rightRect, cardH * 0.12f, cardH * 0.12f, fillPaint)

        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.045f, colCyan, Paint.Align.LEFT)
        canvas.drawText("QR KART", rx + cardW * 0.1f, cardY + cardH * 0.22f, textPaint)

        // QR Matrisi Çizimi
        val qrSize = cardH * 0.50f
        val qx = rx + (cardW - qrSize) * 0.5f
        val qy = cardY + cardH * 0.32f
        rect.set(qx, qy, qx + qrSize, qy + qrSize)
        fillPaint.color = Color.WHITE
        canvas.drawRoundRect(rect, 8f, 8f, fillPaint)

        fillPaint.color = Color.BLACK
        val grid = 15
        val cellSize = qrSize / grid
        val seed = settings.qrCardContent.hashCode()
        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val isCorner = (r < 3 && c < 3) || (r < 3 && c >= grid - 3) || (r >= grid - 3 && c < 3)
                val bit = kotlin.math.abs(seed xor (r * 31 + c * 17)) % 2 == 0
                if (isCorner || bit) {
                    canvas.drawRect(qx + c * cellSize, qy + r * cellSize, qx + (c + 1) * cellSize, qy + (r + 1) * cellSize, fillPaint)
                }
            }
        }

        setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.038f, colTextDim, Paint.Align.CENTER)
        canvas.drawText("Taratmak için uzatın", rx + cardW * 0.5f, cardY + cardH * 0.92f, textPaint)
    }

    // 4. Retro Ping-Pong Mini Oyun Sayfası (120Hz LTPO)
    private fun drawMiniGamePage(canvas: Canvas, cl: Float, cw: Float, h: Float, hits: Boolean) {
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.042f, colCyan, Paint.Align.LEFT)
        canvas.drawText(RearI18n.t(I18nKey.MINI_GAME), cl, h * 0.14f, textPaint)

        // Skor Tablosu
        setText(Typeface.create("sans-serif-black", Typeface.BOLD), h * 0.055f, colGold, Paint.Align.RIGHT)
        canvas.drawText("${RearI18n.t(I18nKey.SCORE)}: $pongScore", cl + cw, h * 0.14f, textPaint)

        // Oyun Sahası Kartı
        val gameTop = h * 0.18f
        val gameH = h * 0.76f
        val gameBottom = gameTop + gameH
        rect.set(cl, gameTop, cl + cw, gameBottom)
        fillPaint.color = Color.rgb(10, 11, 14)
        canvas.drawRoundRect(rect, h * 0.04f, h * 0.04f, fillPaint)
        strokePaint.color = Color.argb(40, 255, 255, 255)
        strokePaint.strokeWidth = 2f
        canvas.drawRoundRect(rect, h * 0.04f, h * 0.04f, strokePaint)

        if (pongGameOver) {
            setText(Typeface.create("sans-serif-bold", Typeface.BOLD), h * 0.09f, colRed, Paint.Align.CENTER)
            canvas.drawText(RearI18n.t(I18nKey.GAME_OVER), cl + cw * 0.5f, gameTop + gameH * 0.42f, textPaint)
            setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.052f, colText, Paint.Align.CENTER)
            canvas.drawText("Son Skorunuz: $pongScore", cl + cw * 0.5f, gameTop + gameH * 0.55f, textPaint)

            drawPill(canvas, cl + cw * 0.5f, gameTop + gameH * 0.78f, cw * 0.55f, h * 0.16f, colCyan, RearI18n.t(I18nKey.RESTART), Color.BLACK, h * 0.052f, hits) {
                pongScore = 0
                pongGameOver = false
                pongBallX = cl + cw * 0.5f
                pongBallY = gameTop + h * 0.1f
                pongBallVx = 5f
                pongBallVy = 4f
                restartSpinIfNeeded()
                invalidate()
            }
            return
        }

        // Top
        fillPaint.color = Color.WHITE
        canvas.drawCircle(pongBallX, pongBallY, h * 0.035f, fillPaint)

        // Raket
        val paddleY = gameBottom - h * 0.06f
        val paddleH = h * 0.035f
        rect.set(pongPaddleX - pongPaddleW * 0.5f, paddleY, pongPaddleX + pongPaddleW * 0.5f, paddleY + paddleH)
        fillPaint.color = colCyan
        canvas.drawRoundRect(rect, paddleH * 0.5f, paddleH * 0.5f, fillPaint)

        // Raket Kontrol Alanı (Parmak nereye dokunursa raketi oraya taşı)
        addRectHit(cl, gameTop, cl + cw, gameBottom) {
            // Dokunma merkezine raketi taşı
        }
    }

    private fun stepPongGame() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 10f || h <= 10f) return

        val cl = if (isPreviewOnFront) w * 0.27f else w * 0.26f
        val cw = w - cl - w * 0.07f
        val gameTop = h * 0.18f
        val gameH = h * 0.76f
        val gameBottom = gameTop + gameH
        val ballR = h * 0.035f

        pongBallX += pongBallVx
        pongBallY += pongBallVy

        // Sol & Sağ duvardan sekme
        if (pongBallX - ballR < cl) {
            pongBallX = cl + ballR
            pongBallVx = -pongBallVx
        } else if (pongBallX + ballR > cl + cw) {
            pongBallX = cl + cw - ballR
            pongBallVx = -pongBallVx
        }

        // Üst duvardan sekme
        if (pongBallY - ballR < gameTop) {
            pongBallY = gameTop + ballR
            pongBallVy = -pongBallVy
        }

        // Raketten sekme
        val paddleY = gameBottom - h * 0.06f
        if (pongBallY + ballR >= paddleY && pongBallY - ballR <= paddleY + h * 0.035f) {
            if (pongBallX >= pongPaddleX - pongPaddleW * 0.55f && pongBallX <= pongPaddleX + pongPaddleW * 0.55f) {
                pongBallVy = -kotlin.math.abs(pongBallVy) * 1.04f
                pongBallVx = (pongBallX - pongPaddleX) * 0.15f
                pongScore++
            }
        }

        // Top aşağı düştü
        if (pongBallY > gameBottom + h * 0.05f) {
            pongGameOver = true
        }
    }

    // 5. ÜSTTEN AŞAĞI ÇEKİNCE AÇILAN TAM BİLDİRİM MERKEZİ PERDESİ (Notification Shade)
    private fun drawNotificationShade(canvas: Canvas, w: Float, h: Float, cl: Float, cw: Float) {
        val maxH = h * 0.94f
        val currentH = if (isShadeOpen) maxH else maxH * shadeDragProgress
        if (currentH <= 5f) return

        // Perde Karartma & Arka Plan (Derin Buzlu Cam)
        rect.set(0f, 0f, w, currentH)
        val alpha = (if (isShadeOpen) 248 else (shadeDragProgress.coerceIn(0f, 1f) * 248).toInt()).coerceIn(0, 255)
        fillPaint.color = Color.argb(alpha, 14, 16, 22)
        canvas.drawRect(rect, fillPaint)

        // Alt Kenarlık ve Gölgelik
        fillPaint.color = Color.argb(90, 58, 62, 75)
        canvas.drawRect(0f, currentH - 3f, w, currentH, fillPaint)

        val shadeContentLeft = cl
        val shadeContentW = cw

        // Başlık Satırı
        val headY = h * 0.12f
        if (currentH > headY) {
            val notifs = RearStateRepository.notifications.value
            setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.052f, colCyan, Paint.Align.LEFT)
            canvas.drawText("BİLDİRİMLER (${notifs.size})", shadeContentLeft, headY, textPaint)

            // Tümünü Temizle Butonu
            if (notifs.isNotEmpty()) {
                val clrW = shadeContentW * 0.32f
                val clrH = h * 0.09f
                drawPill(canvas, shadeContentLeft + shadeContentW - clrW * 0.5f - h * 0.08f, headY - h * 0.02f, clrW, clrH, Color.argb(60, 255, 69, 58), "Tümünü Sil", colRed, h * 0.042f, true) {
                    RearStateRepository.clearNotifications()
                    invalidate()
                }
            }

            // Kapat "✕" Butonu
            val closeX = shadeContentLeft + shadeContentW - h * 0.03f
            setText(Typeface.create("sans-serif", Typeface.BOLD), h * 0.06f, colTextDim, Paint.Align.CENTER)
            canvas.drawText("✕", closeX, headY, textPaint)
            addRectHit(closeX - h * 0.05f, headY - h * 0.06f, w, headY + h * 0.04f) {
                isShadeOpen = false
                shadeDragProgress = 0f
                invalidate()
            }
        }

        // Bildirim Listesi
        val notifs = RearStateRepository.notifications.value
        val listStartY = h * 0.22f
        if (currentH > listStartY + h * 0.15f) {
            if (notifs.isEmpty()) {
                setText(Typeface.create("sans-serif-medium", Typeface.NORMAL), h * 0.06f, colTextDim, Paint.Align.CENTER)
                canvas.drawText("🎉 Bildirim Yok • Hepsi Okundu", w * 0.5f, listStartY + h * 0.25f, textPaint)
            } else {
                val cardH = h * 0.28f
                val gap = h * 0.04f
                notifs.take(2).forEachIndexed { i, item ->
                    val cy = listStartY + i * (cardH + gap)
                    if (cy + cardH <= currentH) {
                        rect.set(shadeContentLeft, cy, shadeContentLeft + shadeContentW, cy + cardH)
                        fillPaint.color = Color.argb(45, 255, 255, 255)
                        canvas.drawRoundRect(rect, h * 0.04f, h * 0.04f, fillPaint)

                        // Uygulama Etiketi
                        setText(Typeface.create("sans-serif-bold", Typeface.BOLD), h * 0.038f, colAccent, Paint.Align.LEFT)
                        canvas.drawText(item.appName.uppercase(Locale("tr", "TR")), shadeContentLeft + h * 0.03f, cy + h * 0.07f, textPaint)

                        // Gönderen
                        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.060f, colText, Paint.Align.LEFT)
                        canvas.drawText(TextUtils.ellipsize(item.sender, textPaint, shadeContentW * 0.85f, TextUtils.TruncateAt.END).toString(),
                            shadeContentLeft + h * 0.03f, cy + h * 0.15f, textPaint)

                        // Mesaj Metni
                        val settings = RearStateRepository.settings.value
                        val displayMsg = if (settings.privacyMaskNotifications && !messageTemporarilyUnmasked) {
                            "🔒 Mesaj Gizlendi • Okumak için dokunun"
                        } else item.message
                        setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.048f, Color.argb(215, 240, 242, 245), Paint.Align.LEFT)
                        canvas.drawText(TextUtils.ellipsize(displayMsg, textPaint, shadeContentW * 0.90f, TextUtils.TruncateAt.END).toString(),
                            shadeContentLeft + h * 0.03f, cy + h * 0.22f, textPaint)

                        addRectHit(shadeContentLeft, cy, shadeContentLeft + shadeContentW, cy + cardH) {
                            messageTemporarilyUnmasked = !messageTemporarilyUnmasked
                            invalidate()
                        }
                    }
                }
            }
        }

        // Alt Perde Kapatma Çubuğu (Pull handle)
        val handleW = cw * 0.22f
        val handleH = 5f
        val handleX = cl + (cw - handleW) * 0.5f
        val handleY = currentH - 14f
        rect.set(handleX, handleY, handleX + handleW, handleY + handleH)
        fillPaint.color = Color.argb(140, 255, 255, 255)
        canvas.drawRoundRect(rect, handleH * 0.5f, handleH * 0.5f, fillPaint)

        addRectHit(0f, currentH - 45f, w, currentH) {
            isShadeOpen = false
            shadeDragProgress = 0f
            invalidate()
        }
    }

    // ---------------------------------------------------------------- arama, mesaj ve alarm üst katmanları

    private fun drawAlarmOverlay(
        canvas: Canvas, cl: Float, cw: Float, h: Float, label: String,
        snooze: android.app.PendingIntent?, dismiss: android.app.PendingIntent?
    ) {
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.05f, colMiOrange, Paint.Align.CENTER)
        canvas.drawText("ALARM", cl + cw / 2f, h * 0.2f, textPaint)
        setText(Typeface.create("sans-serif-thin", Typeface.NORMAL), h * 0.30f, colText, Paint.Align.CENTER)
        canvas.drawText(timeFormat.format(Date()), cl + cw / 2f, h * 0.55f, textPaint)
        setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.055f, colTextDim, Paint.Align.CENTER)
        canvas.drawText(TextUtils.ellipsize(label, textPaint, cw, TextUtils.TruncateAt.END).toString(),
            cl + cw / 2f, h * 0.65f, textPaint)

        val by = h * 0.84f
        val bw = cw * 0.42f
        val bh = h * 0.20f
        drawPill(canvas, cl + cw * 0.26f, by, bw, bh, colSurface, "Ertele", colText, h * 0.062f, true) {
            runCatching { snooze?.send() }
        }
        drawPill(canvas, cl + cw * 0.74f, by, bw, bh, colRed, "Kapat", Color.WHITE, h * 0.062f, true) {
            runCatching { dismiss?.send() }
            RearStateRepository.setAlarmRinging(null)
        }
    }

    private fun drawCallOverlay(canvas: Canvas, cl: Float, cw: Float, h: Float, call: com.mirearultra.pro.core.CallEvent) {
        val active = call.status == CallStatus.ACTIVE
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.05f,
            if (active) colGreen else colAccent, Paint.Align.CENTER)
        canvas.drawText(RearI18n.t(I18nKey.INCOMING_CALL), cl + cw / 2f, h * 0.2f, textPaint)

        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.115f, colText, Paint.Align.CENTER)
        canvas.drawText(TextUtils.ellipsize(call.callerName, textPaint, cw, TextUtils.TruncateAt.END).toString(),
            cl + cw / 2f, h * 0.43f, textPaint)
        val settings = RearStateRepository.settings.value
        val callerNumberDisplay = if (settings.maskCallerPhoneNumber) maskPhoneNumber(call.phoneNumber) else call.phoneNumber
        if (call.phoneNumber.isNotBlank() && call.phoneNumber != call.callerName) {
            setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.06f, colTextDim, Paint.Align.CENTER)
            canvas.drawText(TextUtils.ellipsize(callerNumberDisplay, textPaint, cw, TextUtils.TruncateAt.END).toString(),
                cl + cw / 2f, h * 0.54f, textPaint)
        }

        val by = h * 0.80f
        val bh = h * 0.22f
        if (active) {
            drawPill(canvas, cl + cw / 2f, by, cw * 0.62f, bh, colRed, "Görüşmeyi Bitir", Color.WHITE, h * 0.062f, true) {
                CallActionController.rejectOrEndCall(context)
            }
        } else {
            drawPill(canvas, cl + cw * 0.26f, by, cw * 0.42f, bh, colRed, RearI18n.t(I18nKey.DECLINE), Color.WHITE, h * 0.065f, true) {
                CallActionController.rejectOrEndCall(context)
            }
            drawPill(canvas, cl + cw * 0.74f, by, cw * 0.42f, bh, colGreen, RearI18n.t(I18nKey.ANSWER), Color.WHITE, h * 0.065f, true) {
                CallActionController.answerCall(
                    context,
                    enableSpeakerphone = RearStateRepository.settings.value.autoSpeakerOnRearAnswer
                )
            }
        }
    }

    private fun drawNotificationTopShade(
        canvas: Canvas,
        cl: Float,
        cw: Float,
        h: Float,
        item: com.mirearultra.pro.core.RearNotificationItem,
        settings: com.mirearultra.pro.core.UserSettingsState
    ) {
        val bannerTop = h * 0.04f
        val bannerH = h * 0.38f
        val bannerBottom = bannerTop + bannerH
        val bannerRight = cl + cw

        // Card background (Dark Glass)
        rect.set(cl, bannerTop, bannerRight, bannerBottom)
        fillPaint.color = Color.argb(242, 20, 22, 28)
        canvas.drawRoundRect(rect, h * 0.06f, h * 0.06f, fillPaint)

        // Subtle silver/cyan border
        strokePaint.color = Color.argb(55, 255, 255, 255)
        strokePaint.strokeWidth = 2f
        canvas.drawRoundRect(rect, h * 0.06f, h * 0.06f, strokePaint)

        // Header: App name badge
        val badgeW = cw * 0.32f
        val badgeH = h * 0.07f
        val badgeY = bannerTop + h * 0.035f
        rect.set(cl + h * 0.03f, badgeY, cl + h * 0.03f + badgeW, badgeY + badgeH)
        fillPaint.color = Color.argb(55, 13, 132, 255)
        canvas.drawRoundRect(rect, badgeH * 0.5f, badgeH * 0.5f, fillPaint)

        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.038f, colAccent, Paint.Align.CENTER)
        canvas.drawText(
            item.appName.uppercase(Locale("tr", "TR")),
            cl + h * 0.03f + badgeW * 0.5f,
            badgeY + badgeH * 0.70f,
            textPaint
        )

        // Close / Dismiss '✕' button on top right
        val closeX = bannerRight - h * 0.06f
        val closeY = badgeY + badgeH * 0.65f
        setText(Typeface.create("sans-serif", Typeface.BOLD), h * 0.055f, colTextDim, Paint.Align.CENTER)
        canvas.drawText("✕", closeX, closeY, textPaint)
        addRectHit(closeX - h * 0.06f, bannerTop, bannerRight, bannerTop + h * 0.12f) {
            RearStateRepository.dismissLatestNotification()
            invalidate()
        }

        // Sender title
        val textX = cl + h * 0.04f
        val senderY = badgeY + badgeH + h * 0.08f
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.072f, colText, Paint.Align.LEFT)
        val ellipSender = TextUtils.ellipsize(item.sender, textPaint, cw - h * 0.16f, TextUtils.TruncateAt.END).toString()
        canvas.drawText(ellipSender, textX, senderY, textPaint)

        // Message text (with privacy mask support & smooth horizontal marquee)
        val displayMsg = if (settings.privacyMaskNotifications && !messageTemporarilyUnmasked) {
            "🔒 Mesaj Gizlendi • Okumak için dokunun"
        } else {
            item.message
        }
        setText(Typeface.create("sans-serif", Typeface.NORMAL), h * 0.054f, Color.argb(235, 240, 242, 245), Paint.Align.LEFT)
        val availMsgW = (bannerRight - textX - h * 0.06f).coerceAtLeast(50f)
        val fullMsgW = textPaint.measureText(displayMsg)
        val msgY = senderY + h * 0.072f

        if (fullMsgW > availMsgW && (!settings.privacyMaskNotifications || messageTemporarilyUnmasked)) {
            // Kayan bildirim: Uzun bildirim metni kesilmeden pürüzsüzce kayarak okunur
            val loopDistance = fullMsgW + availMsgW * 0.5f
            val scrollPx = ((SystemClock.uptimeMillis() / 22) % loopDistance.toInt()).toFloat()
            val clipS = canvas.save()
            canvas.clipRect(textX, senderY + h * 0.01f, bannerRight - h * 0.05f, bannerBottom)
            canvas.drawText(displayMsg, textX - scrollPx, msgY, textPaint)
            if (scrollPx > fullMsgW - availMsgW) {
                canvas.drawText(displayMsg, textX - scrollPx + loopDistance, msgY, textPaint)
            }
            canvas.restoreToCount(clipS)
            postInvalidateOnAnimation()
        } else {
            val ellipMsg = TextUtils.ellipsize(displayMsg, textPaint, availMsgW, TextUtils.TruncateAt.END).toString()
            canvas.drawText(ellipMsg, textX, msgY, textPaint)
        }

        // Pull / swipe dismiss handle at bottom
        val handleW = cw * 0.18f
        val handleH = 4f
        val handleX = cl + (cw - handleW) * 0.5f
        val handleY = bannerBottom - h * 0.025f
        rect.set(handleX, handleY, handleX + handleW, handleY + handleH)
        fillPaint.color = Color.argb(90, 255, 255, 255)
        canvas.drawRoundRect(rect, handleH * 0.5f, handleH * 0.5f, fillPaint)

        // Whole shade tap target
        addRectHit(cl, bannerTop + h * 0.10f, bannerRight, bannerBottom) {
            if (settings.privacyMaskNotifications && !messageTemporarilyUnmasked) {
                messageTemporarilyUnmasked = true
                invalidate()
            } else {
                RearStateRepository.dismissLatestNotification()
                invalidate()
            }
        }
    }

    private fun drawMessageOverlay(
        canvas: Canvas, cl: Float, cw: Float, h: Float, item: com.mirearultra.pro.core.RearNotificationItem
    ) {
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.045f, colAccent, Paint.Align.LEFT)
        canvas.drawText(item.appName.uppercase(Locale("tr", "TR")), cl, h * 0.17f, textPaint)

        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), h * 0.085f, colText, Paint.Align.LEFT)
        canvas.drawText(TextUtils.ellipsize(item.sender, textPaint, cw, TextUtils.TruncateAt.END).toString(),
            cl, h * 0.30f, textPaint)

        val settings = RearStateRepository.settings.value
        val displayMsg = if (settings.privacyMaskNotifications && !messageTemporarilyUnmasked) {
            "🔒 Mesaj Gizlendi • Okumak için dokunun"
        } else {
            item.message
        }
        val msgH = drawWrapped(canvas, displayMsg, cl, h * 0.36f, cw, h * 0.062f, Color.argb(225, 244, 245, 247), 4,
            Typeface.create("sans-serif", Typeface.NORMAL))
        addRectHit(cl, h * 0.34f, cl + cw, h * 0.34f + max(msgH, h * 0.25f)) {
            messageTemporarilyUnmasked = !messageTemporarilyUnmasked
            invalidate()
        }

        val by = h * 0.86f
        val bh = h * 0.19f
        drawPill(canvas, cl + cw * 0.26f, by, cw * 0.42f, bh, colSurface, RearI18n.t(I18nKey.CLOSE), colText, h * 0.058f, true) {
            switchMode(RearScreenMode.HOME_LAUNCHER)
            invalidate()
        }
        drawPill(canvas, cl + cw * 0.74f, by, cw * 0.42f, bh, colAccent, "Sesli Oku", Color.rgb(8, 10, 16), h * 0.058f, true) {
            CallActionController.speakNotificationAloud(context, item)
        }
    }

    // ---------------------------------------------------------------- yardımcılar

    private fun maskPhoneNumber(num: String): String {
        val clean = num.trim()
        if (clean.length <= 4) return "••••"
        return if (clean.startsWith("+")) {
            val prefix = clean.take(minOf(6, clean.length))
            val suffix = clean.takeLast(2)
            "$prefix ••• •• $suffix"
        } else {
            val prefix = clean.take(minOf(3, clean.length))
            val suffix = clean.takeLast(2)
            "$prefix ••• •• $suffix"
        }
    }

    private fun setText(tf: Typeface, size: Float, color: Int, align: Paint.Align) {
        textPaint.typeface = tf
        textPaint.textSize = size
        textPaint.color = color
        textPaint.textAlign = align
    }

    private fun drawWrapped(
        canvas: Canvas, text: String, x: Float, y: Float, width: Float,
        size: Float, color: Int, maxLines: Int, tf: Typeface
    ): Float {
        if (text.isBlank()) return 0f
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            textSize = size
            this.color = color
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, tp, width.toInt().coerceAtLeast(10))
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setLineSpacing(0f, 1.05f)
            .build()
        val s = canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restoreToCount(s)
        return layout.height.toFloat()
    }

    private fun addRectHit(l: Float, t: Float, r: Float, b: Float, action: () -> Unit) {
        hitTargets.add(HitTarget(RectF(l, t, r, b), action))
    }

    private fun drawPill(
        canvas: Canvas, cx: Float, cy: Float, bw: Float, bh: Float, bg: Int,
        label: String, textColor: Int, textSize: Float, register: Boolean, onClick: () -> Unit
    ) {
        val r = RectF(cx - bw / 2f, cy - bh / 2f, cx + bw / 2f, cy + bh / 2f)
        fillPaint.color = bg
        canvas.drawRoundRect(r, bh / 2f, bh / 2f, fillPaint)
        setText(Typeface.create("sans-serif-medium", Typeface.BOLD), textSize, textColor, Paint.Align.CENTER)
        val txt = TextUtils.ellipsize(label, textPaint, bw * 0.9f, TextUtils.TruncateAt.END).toString()
        canvas.drawText(txt, cx, cy - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint)
        if (register) hitTargets.add(HitTarget(r, onClick))
    }

    // ---------------------------------------------------------------- dokunma

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val h = height.toFloat().coerceAtLeast(1f)
        val w = width.toFloat().coerceAtLeast(1f)
        val list = pages()
        val currentCall = RearStateRepository.currentCall.value
        val isRingingCall = currentCall != null && currentCall.status == com.mirearultra.pro.core.CallStatus.RINGING
        val overlayActive = isRingingCall ||
            RearStateRepository.alarmRinging.value != null ||
            (RearStateRepository.activeMode.value == RearScreenMode.NOTIFICATION_POPUP &&
                RearStateRepository.latestPopupNotification.value != null)

        val settings = RearStateRepository.settings.value
        val isPocket = RearStateRepository.isPocketMode.value
        val isEarCovered = RearStateRepository.isEarProximityCovered.value
        val isFaceDown = RearStateRepository.isPhoneFaceDown.value
        val isCall = isRingingCall
        if (!isPreviewOnFront && !isFaceDown && ((isEarCovered && settings.earProximityCallPrivacy && isCall) || (isPocket && settings.pocketModeProtection))) {
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val wasDimmed = isDimmedNow()
                lastTouchTimestamp = SystemClock.uptimeMillis()
                if (wasDimmed) {
                    invalidate()
                }
                touchDownX = event.x
                touchDownY = event.y
                lastMoveY = event.y
                dragMode = 0
                dragDx = 0f
                pageAnimator?.cancel()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                lastTouchTimestamp = SystemClock.uptimeMillis()
                if (overlayActive) return true
                val dx = event.x - touchDownX
                val dy = event.y - touchDownY

                // Mini oyun raket kontrolü
                if (currentPage == Page.MINI_GAME && !pongGameOver) {
                    pongPaddleX = event.x.coerceIn(w * 0.28f + pongPaddleW * 0.5f, w * 0.90f - pongPaddleW * 0.5f)
                    invalidate()
                }

                if (dragMode == 0) {
                    if (isShadeOpen && dy < -16f) {
                        dragMode = 3 // Perde açıkken yukarı kaydırıp kapatma
                    } else if (!isShadeOpen && touchDownY < h * 0.12f && dy > 28f && kotlin.math.abs(dy) > kotlin.math.abs(dx) * 1.5f) {
                        dragMode = 3 // YALNIZCA EKRANIN EN ÜST KENARINDAN ÇEKİLDİĞİNDE BİLDİRİM PERDESİNİ AÇMA
                    } else if (kotlin.math.abs(dy) > h * 0.04f && kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                        dragMode = 1 // Sayfalar arası dikey geçiş
                        lastMoveY = event.y
                    } else if (dx > w * 0.04f && dx > kotlin.math.abs(dy) * 1.4f) {
                        dragMode = 2 // Sağa çekince kamera
                    }
                }

                if (dragMode == 3) {
                    val progress = if (isShadeOpen) {
                        (1f + dy / (h * 0.60f)).coerceIn(0f, 1f)
                    } else {
                        (dy / (h * 0.60f)).coerceIn(0f, 1f)
                    }
                    shadeDragProgress = progress
                    invalidate()
                    return true
                } else if (dragMode == 1 && list.size > 1) {
                    animPos = (animPos - (event.y - lastMoveY) / (h * 0.75f)).coerceIn(0f, (list.size - 1).toFloat())
                    lastMoveY = event.y
                    invalidate()
                } else if (dragMode == 2) {
                    dragDx = dx.coerceIn(0f, w * 0.5f)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                lastTouchTimestamp = SystemClock.uptimeMillis()
                val mode = dragMode
                dragMode = 0

                if (mode == 3) {
                    val targetOpen = if (isShadeOpen) shadeDragProgress > 0.65f else shadeDragProgress > 0.22f
                    isShadeOpen = targetOpen
                    shadeDragProgress = if (targetOpen) 1f else 0f
                    invalidate()
                    return true
                } else if (mode == 1 && list.isNotEmpty()) {
                    val totalDy = event.y - touchDownY
                    val curIdx = list.indexOf(currentPage).coerceAtLeast(0)
                    val target = when {
                        totalDy < -h * 0.12f -> (curIdx + 1).coerceAtMost(list.size - 1)
                        totalDy > h * 0.12f -> (curIdx - 1).coerceAtLeast(0)
                        else -> animPos.roundToInt().coerceIn(0, list.size - 1)
                    }
                    val page = list[target]
                    switchMode(modeForPage(page))
                    animateTo(page)
                    restartSpinIfNeeded()
                    return true
                } else if (mode == 2) {
                    val open = event.actionMasked == MotionEvent.ACTION_UP && dragDx > w * 0.2f
                    dragDx = 0f
                    invalidate()
                    if (open) RearDisplayEngine.openXiaomiCameraOnRear(context)
                    return true
                } else if (event.actionMasked == MotionEvent.ACTION_UP) {
                    hitTargets.lastOrNull { it.rect.contains(event.x, event.y) }?.action?.invoke()
                    RearStateRepository.onUserTouchInteraction()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
