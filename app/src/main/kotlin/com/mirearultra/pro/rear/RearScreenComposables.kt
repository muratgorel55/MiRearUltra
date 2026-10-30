package com.mirearultra.pro.rear

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mirearultra.pro.core.BatteryTelemetry
import com.mirearultra.pro.core.CallActionController
import com.mirearultra.pro.core.CallEvent
import com.mirearultra.pro.core.CallStatus
import com.mirearultra.pro.core.ClockFaceStyle
import com.mirearultra.pro.core.LiveActivityState
import com.mirearultra.pro.core.MediaTrackState
import com.mirearultra.pro.core.NotificationReplyController
import com.mirearultra.pro.core.RearDisplayEngine
import com.mirearultra.pro.core.RearNotificationItem
import com.mirearultra.pro.core.RearScreenMode
import com.mirearultra.pro.core.RearStateRepository
import com.mirearultra.pro.core.RearVoiceRecorderController
import com.mirearultra.pro.core.SensorLevelState
import com.mirearultra.pro.core.UserSettingsState
import com.mirearultra.pro.service.RearNotificationService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

private val LeicaRed = Color(0xFFE51937)
private val CyberCyan = Color(0xFF00E5FF)
private val EmeraldGreen = Color(0xFF00E676)
private val AmberGold = Color(0xFFFFB300)
private val AmoledBlack = Color(0xFF050508)
private val CardSurface = Color(0xFF13151F)

@Composable
fun RearScreenInteractiveView(
    modifier: Modifier = Modifier,
    isPhysicalRearDisplay: Boolean = false
) {
    AndroidView(
        factory = { ctx ->
            NativeHyperOsRearView(
                context = ctx,
                isPreviewOnFront = !isPhysicalRearDisplay
            )
        },
        update = { view ->
            view.invalidate()
        },
        modifier = modifier
    )
}

@Composable
private fun LegacyComposeRearScreenView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeMode by RearStateRepository.activeMode.collectAsState()
    val currentCall by RearStateRepository.currentCall.collectAsState()
    val notifications by RearStateRepository.notifications.collectAsState()
    val latestPopup by RearStateRepository.latestPopupNotification.collectAsState()
    val liveActivity by RearStateRepository.liveActivityState.collectAsState()
    val mediaState by RearStateRepository.mediaState.collectAsState()
    val sensorLevel by RearStateRepository.sensorLevelState.collectAsState()
    val battery by RearStateRepository.batteryTelemetry.collectAsState()
    val settings by RearStateRepository.settings.collectAsState()
    val isDimmedSleep by RearStateRepository.isRearScreenDimmedSleep.collectAsState()

    // AMOLED Burn-in Protection (Pixel Shift every 60s)
    var pixelShiftX by remember { mutableIntStateOf(0) }
    var pixelShiftY by remember { mutableIntStateOf(0) }
    LaunchedEffect(settings.amoledBurnInProtection) {
        val offsets = listOf(-2 to -1, 2 to 1, -1 to 2, 1 to -2, 0 to 0)
        var idx = 0
        while (settings.amoledBurnInProtection) {
            delay(60_000L)
            val pair = offsets[idx % offsets.size]
            pixelShiftX = pair.first
            pixelShiftY = pair.second
            idx++
        }
        pixelShiftX = 0
        pixelShiftY = 0
    }

    // Pocket / Double-Tap Touch Lock State
    var isTouchLocked by remember(settings.doubleTapTouchLock) { mutableStateOf(settings.doubleTapTouchLock) }
    var isStealthDimmed by remember(settings.stealthPrivacyDimmer) { mutableStateOf(settings.stealthPrivacyDimmer) }
    var lastUnlockTapMs by remember { mutableLongStateOf(0L) }

    val isVipAlert = (currentCall?.isVipCaller == true) ||
        (activeMode == RearScreenMode.NOTIFICATION_POPUP && latestPopup?.isVip == true)

    // LTPO 1Hz Optimization: Only run 120Hz infinite glow animation when an active alert/mode requires it!
    val shouldAnimateEdge = !isDimmedSleep && !RearStateRepository.isEffectiveEcoMode() &&
        (isVipAlert || activeMode == RearScreenMode.INCOMING_CALL || activeMode == RearScreenMode.NOTIFICATION_POPUP || activeMode == RearScreenMode.LED_MARQUEE)

    val glowAlpha = if (shouldAnimateEdge) {
        val infiniteTransition = rememberInfiniteTransition(label = "edge_glow")
        val anim by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_alpha"
        )
        anim
    } else {
        0.75f
    }

    val borderColor = when {
        isVipAlert -> AmberGold.copy(alpha = glowAlpha)
        activeMode == RearScreenMode.INCOMING_CALL -> EmeraldGreen.copy(alpha = glowAlpha)
        activeMode == RearScreenMode.NOTIFICATION_POPUP -> CyberCyan.copy(alpha = glowAlpha)
        activeMode == RearScreenMode.LIVE_ACTIVITIES -> AmberGold.copy(alpha = glowAlpha)
        activeMode == RearScreenMode.LED_MARQUEE -> LeicaRed.copy(alpha = glowAlpha)
        else -> Color(0xFF2B2F42)
    }

    val coroutineScope = rememberCoroutineScope()

    // Build the ordered list of full-screen Vertical Swipe Pages based on enabled modules
    val verticalPages = remember(
        settings.modMediaLyrics,
        settings.modSpeedDial,
        settings.modLiveActivities,
        settings.modDailyCards,
        settings.modTeleprompter,
        settings.modLedSignAndSos,
        settings.modCyberTools,
        settings.modMiniGame
    ) {
        buildList {
            add(RearScreenMode.HOME_LAUNCHER)
            if (settings.modMediaLyrics) add(RearScreenMode.MEDIA_LYRICS)
            if (settings.modSpeedDial) add(RearScreenMode.SPEED_DIAL)
            if (settings.modLiveActivities) add(RearScreenMode.LIVE_ACTIVITIES)
            if (settings.modDailyCards) add(RearScreenMode.DAILY_CARDS)
            if (settings.modTeleprompter) add(RearScreenMode.TELEPROMPTER)
            if (settings.modLedSignAndSos) add(RearScreenMode.LED_MARQUEE)
            if (settings.modCyberTools) add(RearScreenMode.TOOLS_HUB)
            if (settings.modMiniGame) add(RearScreenMode.MINI_GAME)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { verticalPages.size }
    )

    // When activeMode changes to one of the swipeable pages, scroll VerticalPager to that page
    LaunchedEffect(activeMode, verticalPages) {
        val targetIndex = verticalPages.indexOf(activeMode)
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    // Determine whether an Incoming Call or Incoming Message overlay should blur the active theme
    val showCallOverlay = (currentCall != null) || (activeMode == RearScreenMode.INCOMING_CALL)
    val showMessageOverlay = !showCallOverlay && (activeMode == RearScreenMode.NOTIFICATION_POPUP)
    val isOverlayBlurActive = showCallOverlay || showMessageOverlay

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(AmoledBlack)
            .border(if (isVipAlert) 3.dp else 2.dp, borderColor, RoundedCornerShape(28.dp))
            .padding(10.dp)
    ) {
        // LAYER 1: Full-Screen Vertical Swipe Pager (Blurs when a Call or Message arrives!)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = pixelShiftX.dp, y = pixelShiftY.dp)
                .then(if (isOverlayBlurActive) Modifier.blur(20.dp) else Modifier)
        ) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 22.dp)
            ) { pageIndex ->
                val pageMode = verticalPages.getOrElse(pageIndex) { RearScreenMode.HOME_LAUNCHER }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (pageMode) {
                        RearScreenMode.HOME_LAUNCHER -> RearHomeLauncherView(
                            battery = battery,
                            settings = settings,
                            notifications = notifications,
                            liveActivity = liveActivity,
                            onOpenNotifications = { RearStateRepository.setMode(RearScreenMode.NOTIFICATION_POPUP) },
                            onOpenLiveActivity = {
                                val idx = verticalPages.indexOf(RearScreenMode.LIVE_ACTIVITIES)
                                if (idx >= 0) coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                            }
                        )
                        RearScreenMode.SPEED_DIAL -> RearSpeedDialAndKeypadView(settings = settings)
                        RearScreenMode.LIVE_ACTIVITIES -> RearLiveActivitiesView(liveState = liveActivity)
                        RearScreenMode.DAILY_CARDS -> RearDailyCardsView(settings = settings)
                        RearScreenMode.MEDIA_LYRICS -> RearMediaLyricsView(media = mediaState)
                        RearScreenMode.TELEPROMPTER -> RearTeleprompterAndCameraView(
                            settings = settings,
                            sensorLevel = sensorLevel
                        )
                        RearScreenMode.LED_MARQUEE -> RearLedMarqueeAndSosView(settings = settings)
                        RearScreenMode.TOOLS_HUB -> RearToolsHubView(
                            settings = settings,
                            battery = battery,
                            sensorLevel = sensorLevel
                        )
                        RearScreenMode.MINI_GAME -> RearMiniGameView()
                        else -> RearHomeLauncherView(
                            battery = battery,
                            settings = settings,
                            notifications = notifications,
                            liveActivity = liveActivity,
                            onOpenNotifications = { RearStateRepository.setMode(RearScreenMode.NOTIFICATION_POPUP) },
                            onOpenLiveActivity = {}
                        )
                    }
                }
            }

            // Right-edge sleek Vertical Page Stepper & Dots Indicator (Swipe Up/Down or Tap ▲/▼)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF10131D))
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "▲",
                    color = CyberCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.clickable {
                        val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                        coroutineScope.launch { pagerState.animateScrollToPage(prev) }
                    }
                )
                verticalPages.forEachIndexed { idx, _ ->
                    val isCurrent = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .size(width = 6.dp, height = if (isCurrent) 14.dp else 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isCurrent) LeicaRed else Color(0xFF394158))
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                            }
                    )
                }
                Text(
                    text = "▼",
                    color = CyberCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.clickable {
                        val next = (pagerState.currentPage + 1).coerceAtMost(verticalPages.lastIndex)
                        coroutineScope.launch { pagerState.animateScrollToPage(next) }
                    }
                )
            }
        }

        // LAYER 2: Frosted Glass Blurred Overlay for Incoming Call OR Incoming Message!
        if (isOverlayBlurActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(4.dp)
            ) {
                if (showCallOverlay) {
                    RearIncomingCallView(
                        callEvent = currentCall ?: CallEvent(
                            callerName = "Eşim ❤️ (VIP)",
                            phoneNumber = "+90 532 999 88 77",
                            sourceApp = "Telefon",
                            status = CallStatus.RINGING,
                            isVipCaller = true
                        ),
                        settings = settings
                    )
                } else if (showMessageOverlay) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💬 GELEN MESAJ (Arka Plan Bulanık)",
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Surface(
                                color = LeicaRed,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    RearStateRepository.setMode(RearScreenMode.HOME_LAUNCHER)
                                }
                            ) {
                                Text(
                                    text = "✕ Kapat",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            RearNotificationsView(
                                notifications = notifications,
                                activePopup = latestPopup ?: notifications.firstOrNull(),
                                settings = settings
                            )
                        }
                    }
                }
            }
        }

        // HyperOS 4 Style Stealth Privacy Dimmer Curtain (Anti-Peeking Filter)
        if (isStealthDimmed && !isTouchLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { isStealthDimmed = false },
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    color = Color(0xFF1A1E2E),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "🕶️ Hayalet Perde Aktif (Açmak için dokunun)",
                        color = CyberCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Pocket / Bag Accidental Touch Lock Guard Overlay (Double-Tap to Unlock)
        if (isTouchLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha = 0.88f))
                    .clickable {
                        val now = System.currentTimeMillis()
                        if (now - lastUnlockTapMs < 650L) {
                            isTouchLocked = false
                        }
                        lastUnlockTapMs = now
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Kilitli",
                        tint = AmberGold,
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        text = "CEP / DOKUNMA KİLİDİ AKTİF",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Kilidi açmak için ekrana ÇİFT DOKUNUN",
                        color = AmberGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Ultra-Low Power LTPO 1Hz Deep-Black Sleep Overlay (99% Pixels Off after Timeout)
        if (isDimmedSleep && currentCall == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black)
                    .clickable { RearStateRepository.onUserTouchInteraction() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.offset(x = pixelShiftX.dp, y = pixelShiftY.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale("tr", "TR")) }
                    Text(
                        text = timeFmt.format(Date()),
                        color = Color(0xFF454B5E),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "🔋 LTPO 1Hz Uyku • %${battery.levelPercent} (Uyandırmak için dokunun)",
                        color = Color(0xFF2E3342),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RearFloatingActionFooter(
    isLocked: Boolean,
    isStealth: Boolean,
    showStealthBtn: Boolean,
    onBackClick: () -> Unit,
    onMoveToFrontClick: () -> Unit,
    onStealthToggle: () -> Unit,
    onLockClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0E1018))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onBackClick() }
                .padding(horizontal = 5.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color(0xFFB4BCD0), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Geri", color = Color(0xFFB4BCD0), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onMoveToFrontClick() }
                .padding(horizontal = 5.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ScreenShare, contentDescription = "Ön Ekrana Al", tint = CyberCyan, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Ön Ekrana Al", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        if (showStealthBtn) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onStealthToggle() }
                    .padding(horizontal = 5.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isStealth) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Hayalet",
                    tint = EmeraldGreen,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text("Hayalet", color = EmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onLockClick() }
                .padding(horizontal = 5.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Cep Kilidi",
                tint = AmberGold,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text("Kilit", color = AmberGold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RearTopDock(
    activeMode: RearScreenMode,
    settings: UserSettingsState,
    unreadCount: Int,
    hasActiveCall: Boolean,
    onSelectMode: (RearScreenMode) -> Unit
) {
    val items = buildList<Triple<RearScreenMode, ImageVector, String>> {
        add(Triple(RearScreenMode.HOME_LAUNCHER, Icons.Default.Home, "Ana"))
        if (settings.modIncomingCall) {
            add(Triple(RearScreenMode.INCOMING_CALL, Icons.Default.Call, if (hasActiveCall) "Çalıyor!" else "Çağrı"))
        }
        if (settings.modSpeedDial) {
            add(Triple(RearScreenMode.SPEED_DIAL, Icons.Default.Dialpad, "Hızlı Ara"))
        }
        if (settings.modNotifications) {
            add(Triple(RearScreenMode.NOTIFICATION_POPUP, Icons.Default.Chat, if (unreadCount > 0) "Mesaj($unreadCount)" else "Mesaj"))
        }
        if (settings.modLiveActivities) {
            add(Triple(RearScreenMode.LIVE_ACTIVITIES, Icons.Default.Navigation, "Canlı"))
        }
        if (settings.modDailyCards) {
            add(Triple(RearScreenMode.DAILY_CARDS, Icons.Default.Dashboard, "Kartlar"))
        }
        if (settings.modMediaLyrics) {
            add(Triple(RearScreenMode.MEDIA_LYRICS, Icons.Default.MusicNote, "Müzik"))
        }
        if (settings.modTeleprompter) {
            add(Triple(RearScreenMode.TELEPROMPTER, Icons.Default.Videocam, "Kamera"))
        }
        if (settings.modLedSignAndSos) {
            add(Triple(RearScreenMode.LED_MARQUEE, Icons.Default.Campaign, "Tabela"))
        }
        if (settings.modCyberTools) {
            add(Triple(RearScreenMode.TOOLS_HUB, Icons.Default.QrCode2, "Araçlar"))
        }
        if (settings.modMiniGame) {
            add(Triple(RearScreenMode.MINI_GAME, Icons.Default.SportsEsports, "Oyun"))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (mode, icon, label) ->
            val selected = activeMode == mode
            val tint = when {
                mode == RearScreenMode.INCOMING_CALL && hasActiveCall -> EmeraldGreen
                selected -> Color.White
                else -> Color(0xFF9EA4B8)
            }
            val bg = when {
                selected -> LeicaRed
                mode == RearScreenMode.INCOMING_CALL && hasActiveCall -> EmeraldGreen.copy(alpha = 0.2f)
                else -> Color.Transparent
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .clickable { onSelectMode(mode) }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    color = tint,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RearHomeLauncherView(
    battery: BatteryTelemetry,
    settings: UserSettingsState,
    notifications: List<RearNotificationItem>,
    liveActivity: LiveActivityState,
    onOpenNotifications: () -> Unit,
    onOpenLiveActivity: () -> Unit
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(settings.ltpo1HzClockMode, settings.ecoBatterySaverMode) {
        val use1Hz = settings.ltpo1HzClockMode || RearStateRepository.isEffectiveEcoMode()
        val timeFmt = SimpleDateFormat(if (use1Hz) "HH:mm" else "HH:mm:ss", Locale("tr", "TR"))
        val dateFmt = SimpleDateFormat("dd MMMM EEEE", Locale("tr", "TR"))
        while (true) {
            val now = Date()
            currentTime = timeFmt.format(now)
            currentDate = dateFmt.format(now)
            delay(if (use1Hz) 15_000L else 1000L)
        }
    }

    val accentColor = when (settings.clockFaceStyle) {
        ClockFaceStyle.LEICA_CYBER -> LeicaRed
        ClockFaceStyle.HYPER_MINIMAL -> CyberCyan
        ClockFaceStyle.NEON_MATRIX -> EmeraldGreen
        ClockFaceStyle.ANALOG_CHRONO -> AmberGold
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.22f), CardSurface, AmoledBlack)
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        // Top Header: Theme Name & Battery Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = settings.clockFaceStyle.titleTr.uppercase(),
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AmoledBlack.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = if (battery.isCharging) EmeraldGreen else Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (battery.isCharging) {
                        "%${battery.levelPercent} • ${"%.0f".format(battery.wattage)}W"
                    } else {
                        "%${battery.levelPercent}"
                    },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center: Clean Full-Screen Hero Clock & Date
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTime.ifBlank { "11:45" },
                color = Color.White,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentDate.ifBlank { "29 Ekim Perşembe" },
                color = Color(0xFFD0D7E8),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Bottom: Subtle Vertical Swipe Hint + Unread Pill (if any)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "↕ Aşağı / Yukarı Kaydır: Sayfalar",
                color = Color(0xFF8F96AB),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            val unread = notifications.count { !it.isReplied }
            if (settings.modNotifications && unread > 0) {
                Surface(
                    color = CyberCyan.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { onOpenNotifications() }
                ) {
                    Text(
                        text = "💬 $unread Mesaj",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RearSpeedDialAndKeypadView(
    settings: UserSettingsState
) {
    val context = LocalContext.current
    var dialedNumber by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "☎️ ARKA EKRANDAN HIZLI ARAMA (HOPARLÖR)",
            color = EmeraldGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )

        // Speed Dial Favorite Contacts Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            settings.speedDialContacts.forEach { contact ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardSurface)
                        .border(1.dp, EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable {
                            CallActionController.placeCallFromRear(context, contact.name, contact.phone)
                        }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(contact.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(contact.roleLabel, color = Color(0xFF9EA6BA), fontSize = 9.sp, maxLines = 1)
                }
            }
        }

        // Mini T9 Dialer Input & Keypad
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dialedNumber.ifBlank { "Numara çevirin..." },
                color = if (dialedNumber.isBlank()) Color.Gray else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            if (dialedNumber.isNotEmpty()) {
                Text(
                    text = "⌫ Sil",
                    color = LeicaRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { dialedNumber = dialedNumber.dropLast(1) }
                )
            }
        }

        val rows = listOf(
            listOf("1", "2", "3", "4"),
            listOf("5", "6", "7", "8"),
            listOf("9", "0", "+", "ARA")
        )
        rows.forEach { r ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                r.forEach { key ->
                    val isCallBtn = key == "ARA"
                    Surface(
                        color = if (isCallBtn) EmeraldGreen else CardSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (isCallBtn) {
                                    CallActionController.placeCallFromRear(context, dialedNumber, dialedNumber)
                                } else {
                                    dialedNumber += key
                                }
                            }
                    ) {
                        Text(
                            text = key,
                            color = if (isCallBtn) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RearDailyCardsView(
    settings: UserSettingsState
) {
    var quickTimerSec by remember { mutableIntStateOf(0) }
    var quickTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(quickTimerRunning, quickTimerSec) {
        while (quickTimerRunning && quickTimerSec > 0) {
            delay(1000L)
            quickTimerSec--
        }
        if (quickTimerSec == 0) quickTimerRunning = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // 1. Live Finance / Gold / Crypto Ticker Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardSurface)
                .border(1.dp, AmberGold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .padding(9.dp)
        ) {
            Text(
                text = "📈 CANLI PİYASA & ALTIN / DÖVİZ KARTI",
                color = AmberGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FinanceBadge("USD/TRY", "36.45 ₺", "+0.3%")
                FinanceBadge("EUR/TRY", "39.80 ₺", "+0.4%")
                FinanceBadge("GR ALTIN", "3.420 ₺", "+1.2%")
                FinanceBadge("BTC", "\$94.2K", "+2.8%")
            }
        }

        // 2. Water Intake & Habit Counter Card (Xiaomi 18 HyperOS 4 Mini-App Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardSurface)
                .padding(9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "💧 GÜNLÜK SU SAYACI: ${settings.waterGlassesCount} / ${settings.waterGoalCount} Bardak",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (settings.waterGlassesCount.toFloat() / settings.waterGoalCount.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    color = CyberCyan,
                    trackColor = AmoledBlack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = CyberCyan,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable {
                    RearStateRepository.updateSettings {
                        it.copy(waterGlassesCount = (it.waterGlassesCount + 1) % (it.waterGoalCount + 3))
                    }
                }
            ) {
                Text(
                    text = "+1 İçtim",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }

        // 3. Parking Spot / Floor Pin Card
        // 4. Quick Kitchen / Tea / Egg Timers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardSurface)
                .padding(9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (quickTimerRunning) "⏲️ %02d:%02d".format(quickTimerSec / 60, quickTimerSec % 60)
                else "⏲️ Hızlı Sayaç:",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("3 dk Çay" to 180, "5 dk Yumurta" to 300, "10 dk" to 600).forEach { (lbl, secs) ->
                    Surface(
                        color = LeicaRed,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            quickTimerSec = secs
                            quickTimerRunning = true
                        }
                    ) {
                        Text(
                            text = lbl,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceBadge(symbol: String, price: String, change: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(symbol, color = Color(0xFF9EA6BA), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(price, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Text(change, color = EmeraldGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RearLedMarqueeAndSosView(
    settings: UserSettingsState
) {
    var isSosStrobe by remember { mutableStateOf(false) }
    var strobePhase by remember { mutableStateOf(false) }

    val marqueeTransition = rememberInfiniteTransition(label = "marquee")
    val scrollOffset by marqueeTransition.animateFloat(
        initialValue = 260f,
        targetValue = -420f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "marquee_x"
    )

    LaunchedEffect(isSosStrobe) {
        while (isSosStrobe) {
            delay(220L)
            strobePhase = !strobePhase
        }
    }

    val bgColor = if (isSosStrobe) {
        if (strobePhase) LeicaRed else Color(0xFF0044FF)
    } else {
        AmoledBlack
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(2.dp, if (isSosStrobe) Color.White else CyberCyan, RoundedCornerShape(18.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSosStrobe) "🚨 ACİL DURUM / SOS ÇAKAR AKTİF" else "🪧 KAYAN LED TABELA (MARQUEE)",
                color = if (isSosStrobe) Color.White else CyberCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Surface(
                color = if (isSosStrobe) Color.Black else LeicaRed,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { isSosStrobe = !isSosStrobe }
            ) {
                Text(
                    text = if (isSosStrobe) "Çakarı Kapat" else "🚨 SOS Çakar Aç",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = settings.marqueeBannerText,
                color = if (isSosStrobe) Color.White else EmeraldGreen,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.offset(x = scrollOffset.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("TOPLANTIDAYIM", "TAKSİ 🚕", "ACİL DURUM SOS", "MÜSAİT DEĞİLİM").forEach { preset ->
                Surface(
                    color = CardSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            RearStateRepository.updateSettings { it.copy(marqueeBannerText = preset) }
                        }
                ) {
                    Text(
                        text = preset,
                        color = Color.White,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RearMiniGameView() {
    var score by remember { mutableIntStateOf(0) }
    var playerX by remember { mutableIntStateOf(3) }
    var playerY by remember { mutableIntStateOf(3) }
    var targetX by remember { mutableIntStateOf(5) }
    var targetY by remember { mutableIntStateOf(2) }

    fun movePlayer(dx: Int, dy: Int) {
        playerX = (playerX + dx).coerceIn(0, 7)
        playerY = (playerY + dy).coerceIn(0, 4)
        if (playerX == targetX && playerY == targetY) {
            score += 10
            targetX = Random.nextInt(0, 8)
            targetY = Random.nextInt(0, 5)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎮 ARKA EKRAN CYBER SNAKE", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            Text("Skor: $score", color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }

        // 8x5 Grid Arena
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AmoledBlack)
                .padding(6.dp)
        ) {
            val cols = 8
            val rows = 5
            val cellW = size.width / cols
            val cellH = size.height / rows

            // Target Energy Orb
            drawCircle(
                color = LeicaRed,
                radius = minOf(cellW, cellH) * 0.36f,
                center = Offset((targetX + 0.5f) * cellW, (targetY + 0.5f) * cellH)
            )

            // Player Cyber Cube
            drawRect(
                color = EmeraldGreen,
                topLeft = Offset(playerX * cellW + 4f, playerY * cellH + 4f),
                size = Size(cellW - 8f, cellH - 8f)
            )
        }

        // Touch D-Pad Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                "◀ Sol" to (-1 to 0),
                "▲ Yukarı" to (0 to -1),
                "▼ Aşağı" to (0 to 1),
                "Sağ ▶" to (1 to 0)
            ).forEach { (label, delta) ->
                Surface(
                    color = AmoledBlack,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { movePlayer(delta.first, delta.second) }
                ) {
                    Text(
                        text = label,
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RearLiveActivitiesView(
    liveState: LiveActivityState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Turn-by-Turn Navigation Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardSurface)
                .border(1.dp, EmeraldGreen.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "CANLI NAVİGASYON • ${liveState.navApp.uppercase()}",
                        color = EmeraldGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text = liveState.navEta,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = liveState.navManeuver,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = liveState.navStreet,
                color = Color(0xFFB2BACD),
                fontSize = 11.sp
            )

            LinearProgressIndicator(
                progress = { liveState.navProgress },
                color = EmeraldGreen,
                trackColor = AmoledBlack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        // 2. Live Delivery / Order / Ride-Hailing Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardSurface)
                .border(1.dp, AmberGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = AmberGold, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "SİPARİŞ & KURYE TAKİBİ • ${liveState.deliveryApp.uppercase()}",
                        color = AmberGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Text(
                text = liveState.deliveryTitle,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = liveState.deliveryEta,
                color = CyberCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            LinearProgressIndicator(
                progress = { liveState.deliveryProgress },
                color = AmberGold,
                trackColor = AmoledBlack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun RearIncomingCallView(
    callEvent: CallEvent,
    settings: UserSettingsState
) {
    val context = LocalContext.current
    var showSmsSheet by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "call_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val badgeColor = if (callEvent.isVipCaller) AmberGold else EmeraldGreen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (callEvent.isVipCaller) Color(0xFF281E06).copy(alpha = 0.55f) else Color(0xFF0A1E17).copy(alpha = 0.55f),
                        AmoledBlack.copy(alpha = 0.45f)
                    )
                )
            )
            .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Caller Badge
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = badgeColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = when {
                        callEvent.status == CallStatus.ACTIVE -> "🟢 AKTİF GÖRÜŞME • ${callEvent.sourceApp.uppercase()}"
                        callEvent.isVipCaller -> "🌟 VIP ÖNCELİKLİ ARAMA • ${callEvent.sourceApp.uppercase()}"
                        else -> "📞 GELEN ARAMA • ${callEvent.sourceApp.uppercase()}"
                    },
                    color = badgeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(if (callEvent.status == CallStatus.RINGING) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.22f))
                    .border(2.dp, badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = callEvent.callerName,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = callEvent.phoneNumber,
                color = Color(0xFFB0B8CC),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (showSmsSheet) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Hızlı Mesaj Gönder & Reddet:",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                settings.quickReplyTemplates.forEach { tpl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardSurface)
                            .clickable {
                                CallActionController.rejectWithQuickSms(context, tpl)
                                showSmsSheet = false
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tpl,
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Action Controls
        if (callEvent.status == CallStatus.RINGING) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { CallActionController.rejectOrEndCall(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = LeicaRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Reddet", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reddet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            CallActionController.answerCall(
                                context = context,
                                enableSpeakerphone = true
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.35f)
                            .height(40.dp)
                    ) {
                        Icon(
                            Icons.Default.SpeakerPhone,
                            contentDescription = "Hoparlör",
                            tint = Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hoparlör Aç",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = CardSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                CallActionController.answerCall(
                                    context = context,
                                    enableSpeakerphone = settings.autoSpeakerOnRearAnswer
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Normal Aç", color = Color.White, fontSize = 10.sp)
                        }
                    }

                    Surface(
                        color = CardSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { CallActionController.silenceRinger(context) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VolumeOff, contentDescription = null, tint = AmberGold, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sessize Al", color = Color.White, fontSize = 10.sp)
                        }
                    }

                    Surface(
                        color = CardSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showSmsSheet = !showSmsSheet }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Reply, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SMS Gönder", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { CallActionController.toggleSpeakerphone(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (callEvent.isSpeakerOn) CyberCyan else CardSurface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = if (callEvent.isSpeakerOn) Color.Black else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (callEvent.isSpeakerOn) "Hoparlör Açık" else "Hoparlör",
                        color = if (callEvent.isSpeakerOn) Color.Black else Color.White,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { CallActionController.toggleMute(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (callEvent.isMuted) AmberGold else CardSurface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.MicOff,
                        contentDescription = null,
                        tint = if (callEvent.isMuted) Color.Black else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (callEvent.isMuted) "Sessizde" else "Mikrofon",
                        color = if (callEvent.isMuted) Color.Black else Color.White,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { CallActionController.rejectOrEndCall(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = LeicaRed),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Kapat", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RearNotificationsView(
    notifications: List<RearNotificationItem>,
    activePopup: RearNotificationItem?,
    settings: UserSettingsState
) {
    val context = LocalContext.current
    var selectedItem by remember(activePopup) { mutableStateOf(activePopup ?: notifications.firstOrNull()) }
    var privacyRevealed by remember(selectedItem?.id) { mutableStateOf(!settings.privacyMaskNotifications) }
    var customReplyText by remember { mutableStateOf("") }

    val voiceReplyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank() && selectedItem != null) {
                NotificationReplyController.sendDirectReply(context, selectedItem!!, spoken)
            }
        }
    }

    val item = selectedItem
    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Arka ekranda okunmamış bildirim yok.", color = Color.Gray, fontSize = 12.sp)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Horizontal Notification Switcher Pills
        if (notifications.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                notifications.forEach { notif ->
                    val isSelected = notif.id == item.id
                    Surface(
                        color = if (isSelected) CyberCyan.copy(alpha = 0.25f) else CardSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { selectedItem = notif }
                    ) {
                        Text(
                            text = "${notif.appName}: ${notif.sender}",
                            color = if (isSelected) CyberCyan else Color.White,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Message Card + TTS Speaker Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardSurface)
                .border(
                    width = 1.dp,
                    color = if (item.isVip) AmberGold else CyberCyan.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (item.isVip) "🌟 VIP • ${item.appName.uppercase()} • ${item.sender}"
                    else "${item.appName.uppercase()} • ${item.sender}",
                    color = if (item.isVip) AmberGold else CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )

                // TTS Read Aloud Button (Sesli Oku)
                if (settings.modTextToSpeech) {
                    Surface(
                        color = AmoledBlack,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            CallActionController.speakNotificationAloud(context, item)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Sesli Oku", tint = AmberGold, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Sesli Oku", color = AmberGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (!privacyRevealed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmoledBlack)
                        .clickable { privacyRevealed = true }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Gizlilik Koruması Aktif — Mesajı Okumak İçin Dokunun",
                        color = AmberGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = item.message,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
                if (item.lastReplyText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✓ Gönderilen yanıt: \"${item.lastReplyText}\"",
                        color = EmeraldGreen,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Quick Emoji Reply Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("👍 Tamam", "❤️ Teşekkürler", "👌 Geliyorum", "🙏 Peki").forEach { quickEmoji ->
                Surface(
                    color = CardSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            NotificationReplyController.sendDirectReply(context, item, quickEmoji)
                        }
                ) {
                    Text(
                        text = quickEmoji,
                        color = Color.White,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }

        // Preset Quick Reply Templates
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            settings.quickReplyTemplates.take(3).forEach { template ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardSurface)
                        .clickable {
                            NotificationReplyController.sendDirectReply(context, item, template)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = template,
                        color = Color(0xFFD5DCEB),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Gönder",
                        tint = CyberCyan,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Direct Custom Text + Voice Dictation Reply Input on Rear Screen
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = customReplyText,
                onValueChange = { customReplyText = it },
                placeholder = { Text("Mesaj yaz veya sesli yanıtla...", fontSize = 11.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = Color(0xFF32384D)
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    runCatching {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "${item.sender} kişisine sesli yanıt söyleyin...")
                        }
                        voiceReplyLauncher.launch(intent)
                    }.onFailure {
                        RearStateRepository.appendLog("🎙️ Sesli dikte başlatılamadı: ${it.message}")
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LeicaRed)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Sesli Dikte", tint = Color.White)
            }
            IconButton(
                onClick = {
                    if (customReplyText.isNotBlank()) {
                        NotificationReplyController.sendDirectReply(context, item, customReplyText)
                        customReplyText = ""
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberCyan)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Gönder", tint = Color.Black)
            }
        }
    }
}

@Composable
private fun RearMediaLyricsView(
    media: MediaTrackState
) {
    var localPlaying by remember(media.isPlaying) { mutableStateOf(media.isPlaying) }
    var activeLine by remember { mutableIntStateOf(0) }

    LaunchedEffect(localPlaying) {
        while (localPlaying && media.lyricsLines.isNotEmpty()) {
            delay(2400L)
            activeLine = (activeLine + 1) % media.lyricsLines.size
        }
    }

    val waveTransition = rememberInfiniteTransition(label = "visualizer")
    val wavePhase by waveTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.appName.uppercase(),
                    color = LeicaRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = media.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = media.artist,
                    color = Color(0xFFA2A9BE),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Canvas(modifier = Modifier.size(width = 56.dp, height = 28.dp)) {
                val barCount = 6
                val barWidth = size.width / (barCount * 1.6f)
                for (i in 0 until barCount) {
                    val factor = if (localPlaying) {
                        ((wavePhase + (i * 0.17f)) % 1f).coerceIn(0.2f, 1f)
                    } else {
                        0.2f
                    }
                    val barHeight = size.height * factor
                    drawRect(
                        color = if (i % 2 == 0) LeicaRed else CyberCyan,
                        topLeft = Offset(i * barWidth * 1.5f, size.height - barHeight),
                        size = Size(barWidth, barHeight)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AmoledBlack)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            media.lyricsLines.forEachIndexed { index, line ->
                val isCurrent = index == activeLine
                Text(
                    text = line,
                    color = if (isCurrent) CyberCyan else Color(0xFF6E758A),
                    fontSize = if (isCurrent) 13.sp else 11.sp,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { RearNotificationService.mediaPrevious() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Önceki", tint = Color.White)
            }
            Button(
                onClick = {
                    localPlaying = !localPlaying
                    RearNotificationService.mediaPlayPause()
                },
                colors = ButtonDefaults.buttonColors(containerColor = LeicaRed),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (localPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Oynat/Duraklat",
                    tint = Color.White
                )
            }
            IconButton(onClick = { RearNotificationService.mediaNext() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Sonraki", tint = Color.White)
            }
        }
    }
}

@Composable
private fun RearTeleprompterAndCameraView(
    settings: UserSettingsState,
    sensorLevel: SensorLevelState
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var isScrolling by remember { mutableStateOf(true) }
    var viewfinderGridOverlay by remember { mutableStateOf(true) }
    var countdownSec by remember { mutableIntStateOf(0) }

    LaunchedEffect(isScrolling, settings.teleprompterSpeed) {
        while (isScrolling) {
            delay(45L)
            val next = scrollState.value + (settings.teleprompterSpeed * 2).toInt().coerceAtLeast(1)
            if (next <= scrollState.maxValue) {
                scrollState.scrollTo(next)
            } else {
                scrollState.scrollTo(0)
            }
        }
    }

    LaunchedEffect(countdownSec) {
        if (countdownSec > 0) {
            delay(1000L)
            countdownSec--
            if (countdownSec == 0) {
                RearDisplayEngine.launchAppOnRearDisplay(context, "com.android.camera")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(AmoledBlack)
            .border(1.dp, LeicaRed.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔴 LEICA VİZÖR + TELEPROMPTER",
                color = LeicaRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Surface(
                    color = CardSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { viewfinderGridOverlay = !viewfinderGridOverlay }
                ) {
                    Text(
                        text = if (viewfinderGridOverlay) "Ufuk: ${"%.1f".format(sensorLevel.tiltXDeg)}°" else "Izgara Aç",
                        color = EmeraldGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = CardSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { isScrolling = !isScrolling }
                ) {
                    Text(
                        text = if (isScrolling) "Duraklat" else "Başlat",
                        color = CyberCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = LeicaRed,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { countdownSec = 3 }
                ) {
                    Text(
                        text = if (countdownSec > 0) "📸 $countdownSec..." else "📸 3sn Çek",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurface)
        ) {
            if (viewfinderGridOverlay) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Rule of Thirds Camera Framing Grid + Horizon Level Line
                    val w3 = size.width / 3f
                    val h3 = size.height / 3f
                    val gridCol = Color.White.copy(alpha = 0.12f)
                    drawLine(gridCol, Offset(w3, 0f), Offset(w3, size.height), 1.5f)
                    drawLine(gridCol, Offset(w3 * 2, 0f), Offset(w3 * 2, size.height), 1.5f)
                    drawLine(gridCol, Offset(0f, h3), Offset(size.width, h3), 1.5f)
                    drawLine(gridCol, Offset(0f, h3 * 2), Offset(size.width, h3 * 2), 1.5f)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = settings.teleprompterScript,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun RearToolsHubView(
    settings: UserSettingsState,
    battery: BatteryTelemetry,
    sensorLevel: SensorLevelState
) {
    val context = LocalContext.current
    // 0 = QR Wallet, 1 = Voice Recorder, 2 = Spirit Level & Steps, 3 = Mini Calculator, 4 = Selfie Light
    var activeToolTab by remember { mutableIntStateOf(0) }
    var selectedLightColor by remember { mutableStateOf(Color(0xFFFFF3E0)) }

    val isRecording by RearVoiceRecorderController.isRecording.collectAsState()
    val savedNotes by RearVoiceRecorderController.savedNotes.collectAsState()
    var recElapsedSec by remember { mutableIntStateOf(0) }

    // Embedded Calculator state
    var calcDisplay by remember { mutableStateOf("0") }
    var calcPendingOp by remember { mutableStateOf<String?>(null) }
    var calcFirstVal by remember { mutableStateOf(0.0) }

    LaunchedEffect(isRecording) {
        recElapsedSec = 0
        while (isRecording) {
            delay(1000L)
            recElapsedSec++
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                Triple(0, Icons.Default.QrCode2, "QR Kart"),
                Triple(1, Icons.Default.Mic, "Ses Kaydı"),
                Triple(2, Icons.Default.Explore, "Terazi/Adım"),
                Triple(3, Icons.Default.Calculate, "Hesapla"),
                Triple(4, Icons.Default.FlashlightOn, "Işık")
            ).forEach { (idx, icon, title) ->
                val selected = activeToolTab == idx
                Surface(
                    color = if (selected) CyberCyan else CardSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { activeToolTab = idx }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (selected) Color.Black else Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = title,
                            color = if (selected) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        when (activeToolTab) {
            0 -> {
                // QR Matrix & IBAN / Boarding Pass Card
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(6.dp)
                    ) {
                        val grid = 13
                        val cell = size.width / grid
                        val seed = settings.qrCardContent.hashCode()
                        for (r in 0 until grid) {
                            for (c in 0 until grid) {
                                val isFinder = (r < 3 && c < 3) || (r < 3 && c >= grid - 3) || (r >= grid - 3 && c < 3)
                                val bit = abs((seed xor (r * 31 + c * 17))) % 2 == 0
                                if (isFinder || bit) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(c * cell, r * cell),
                                        size = Size(cell * 0.92f, cell * 0.92f)
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = settings.qrCardTitle,
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = settings.qrCardContent,
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            1 -> {
                // One-Tap Rear Screen Voice Memo Recorder
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRecording) "🔴 KAYIT YAPILIYOR..." else "🎙️ TOPLANTI & SESLİ NOT KAYDEDİCİ",
                            color = if (isRecording) LeicaRed else CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "%02d:%02d".format(recElapsedSec / 60, recElapsedSec % 60),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    val latestNote = savedNotes.firstOrNull()
                    if (latestNote != null) {
                        Text(
                            text = "Son Kayıt: ${latestNote.fileName} (${latestNote.durationSec} sn • ${latestNote.timestamp})",
                            color = Color(0xFFB4BCD0),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { RearVoiceRecorderController.toggleRecording(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) LeicaRed else EmeraldGreen
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isRecording) Color.White else Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRecording) "Kaydı Durdur ve Kaydet (.m4a)" else "Arka Ekrandan Ses Kaydı Başlat",
                            color = if (isRecording) Color.White else Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            2 -> {
                // Spirit Level (Su Terazisi X/Y°) & Pedometer Step Counter
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(AmoledBlack)
                            .border(2.dp, EmeraldGreen, CircleShape)
                    ) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        drawCircle(Color.White.copy(alpha = 0.25f), radius = size.width * 0.22f, center = Offset(cx, cy))
                        val bx = (cx + sensorLevel.tiltXDeg * 1.4f).coerceIn(14f, size.width - 14f)
                        val by = (cy + sensorLevel.tiltYDeg * 1.4f).coerceIn(14f, size.height - 14f)
                        drawCircle(EmeraldGreen, radius = 10f, center = Offset(bx, by))
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🧭 SU TERAZİSİ & ADIMSAYAR", color = EmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = "Eğim X: ${"%.1f".format(sensorLevel.tiltXDeg)}°  |  Y: ${"%.1f".format(sensorLevel.tiltYDeg)}°",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "👟 Bugün Atılan Adım: ${sensorLevel.stepCount} adım",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            3 -> {
                // Embedded Rear Screen Calculator
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmoledBlack)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "C",
                            color = LeicaRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.clickable {
                                calcDisplay = "0"
                                calcPendingOp = null
                                calcFirstVal = 0.0
                            }
                        )
                        Text(
                            text = calcDisplay,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    val calcRows = listOf(
                        listOf("7", "8", "9", "+"),
                        listOf("4", "5", "6", "-"),
                        listOf("1", "2", "3", "×"),
                        listOf("0", ".", "=", "÷")
                    )
                    calcRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.forEach { btn ->
                                Surface(
                                    color = if (btn in listOf("+", "-", "×", "÷", "=")) AmberGold else AmoledBlack,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            when (btn) {
                                                in "0".."9", "." -> {
                                                    calcDisplay = if (calcDisplay == "0") btn else calcDisplay + btn
                                                }
                                                "+", "-", "×", "÷" -> {
                                                    calcFirstVal = calcDisplay.toDoubleOrNull() ?: 0.0
                                                    calcPendingOp = btn
                                                    calcDisplay = "0"
                                                }
                                                "=" -> {
                                                    val second = calcDisplay.toDoubleOrNull() ?: 0.0
                                                    val res = when (calcPendingOp) {
                                                        "+" -> calcFirstVal + second
                                                        "-" -> calcFirstVal - second
                                                        "×" -> calcFirstVal * second
                                                        "÷" -> if (second != 0.0) calcFirstVal / second else 0.0
                                                        else -> second
                                                    }
                                                    calcDisplay = if (res % 1.0 == 0.0) res.toInt().toString() else "%.2f".format(res)
                                                    calcPendingOp = null
                                                }
                                            }
                                        }
                                ) {
                                    Text(
                                        text = btn,
                                        color = if (btn in listOf("+", "-", "×", "÷", "=")) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                // Rear Camera Selfie Soft Ring Light / Torch
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(selectedLightColor)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ARKA KAMERA SELFIE DOLGU IŞIĞI",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Pil: %${battery.levelPercent}",
                        color = Color.Black.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Sıcak" to Color(0xFFFFF3E0),
                            "Gün Işığı" to Color(0xFFFFFFFF),
                            "Soğuk" to Color(0xFFE0F7FA),
                            "Leica" to Color(0xFFFFCDD2)
                        ).forEach { (label, color) ->
                            Surface(
                                color = Color.Black.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable { selectedLightColor = color }
                            ) {
                                Text(
                                    text = label,
                                    color = color,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Xiaomi 17/18 Arka Ekran - Gelen VIP Arama",
    widthDp = 420,
    heightDp = 300,
    showBackground = true,
    backgroundColor = 0xFF050507
)
@Composable
fun RearVipCallPreview() {
    RearStateRepository.onIncomingCall(
        CallEvent(
            callerName = "Eşim ❤️ (VIP)",
            phoneNumber = "+90 532 999 88 77",
            sourceApp = "Telefon",
            status = CallStatus.RINGING,
            isVipCaller = true
        )
    )
    RearScreenInteractiveView()
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Xiaomi 17/18 Arka Ekran - Ana Merkez",
    widthDp = 420,
    heightDp = 300,
    showBackground = true,
    backgroundColor = 0xFF050507
)
@Composable
fun RearHomeHubPreview() {
    RearStateRepository.setMode(RearScreenMode.HOME_LAUNCHER)
    RearScreenInteractiveView()
}

