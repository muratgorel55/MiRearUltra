package com.mirearultra.pro.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.provider.ContactsContract
import com.mirearultra.pro.core.SpeedDialContact

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SendToMobile
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.mirearultra.pro.core.CallEvent
import com.mirearultra.pro.core.CallStatus
import com.mirearultra.pro.core.ClockFaceStyle
import com.mirearultra.pro.core.RearDisplayEngine
import com.mirearultra.pro.core.RearNotificationItem
import com.mirearultra.pro.core.RearScreenMode
import com.mirearultra.pro.core.RearStateRepository
import com.mirearultra.pro.core.ShizukuConnectionState
import com.mirearultra.pro.rear.RearScreenInteractiveView

private val LeicaRed = Color(0xFF0D84FF)
private val CyberCyan = Color(0xFF5AC8FA)
private val EmeraldGreen = Color(0xFF34C759)
private val AmberGold = Color(0xFFFF9F0A)
private val DarkBg = Color(0xFF000000)
private val CardBg = Color(0xFF1C1C1E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val shizukuReady by RearStateRepository.shizukuReady.collectAsState()
    val notifConnected by RearStateRepository.notificationListenerConnected.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(LeicaRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Arka ekran",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Xiaomi 17 Pro Max • v1.2 Alpha (Public Preview)",
                                color = Color(0xFF9BA3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        color = if (shizukuReady || notifConnected) EmeraldGreen.copy(alpha = 0.18f) else AmberGold.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (shizukuReady) "SHIZUKU AKTİF" else if (notifConnected) "SERVİS AKTİF" else "KURULUM",
                            color = if (shizukuReady || notifConnected) EmeraldGreen else AmberGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF000000))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF000000)) {
                val tabs = listOf(
                    Triple(0, Icons.Default.PhoneAndroid, "Canlı Ekran"),
                    Triple(1, Icons.Default.Tune, "Modüller"),
                    Triple(2, Icons.Default.Security, "İzin & Gizlilik"),
                    Triple(3, Icons.Default.Build, "Özelleştir")
                )
                tabs.forEach { (idx, icon, label) ->
                    NavigationBarItem(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LeicaRed,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color(0xFF8A92A6),
                            unselectedTextColor = Color(0xFF8A92A6),
                            indicatorColor = LeicaRed.copy(alpha = 0.18f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> LiveRearOverviewTab(onOpenModulesTab = { selectedTab = 1 })
                1 -> FeatureModulesToggleTab()
                2 -> SetupAndPlayPolicyTab()
                else -> CustomizationStudioTab()
            }
        }
    }
}

@Composable
private fun LiveRearOverviewTab(
    onOpenModulesTab: () -> Unit
) {
    val context = LocalContext.current
    val displays by RearStateRepository.detectedDisplays.collectAsState()
    val statusLog by RearStateRepository.statusLog.collectAsState()
    val activeMode by RearStateRepository.activeMode.collectAsState()
    val settings by RearStateRepository.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Xiaomi 17 & 18 Pro Max Interactive Back Screen Frame
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ARKA EKRAN ÖNİZLEMESİ",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Surface(
                        color = LeicaRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onOpenModulesTab() }
                    ) {
                        Text(
                            text = "⚙️ Modülleri Aç/Kapat ›",
                            color = LeicaRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                RearScreenInteractiveView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "🎨 Arka Ekran Teması:",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    com.mirearultra.pro.core.RearWallpaperTheme.entries.forEach { th ->
                        val sel = settings.rearWallpaperTheme == th
                        val lbl = when (th) {
                            com.mirearultra.pro.core.RearWallpaperTheme.MINIMAL_OBSIDIAN -> "Obsidyen"
                            com.mirearultra.pro.core.RearWallpaperTheme.COSMIC_NEBULA -> "Nebula"
                            com.mirearultra.pro.core.RearWallpaperTheme.CYBER_LEICA -> "Leica"
                            com.mirearultra.pro.core.RearWallpaperTheme.NORDIC_AURORA -> "Aurora"
                            com.mirearultra.pro.core.RearWallpaperTheme.CARBON_FIBER -> "Karbon"
                            com.mirearultra.pro.core.RearWallpaperTheme.SUNSET_AMBER -> "Gün Batımı"
                            com.mirearultra.pro.core.RearWallpaperTheme.CUSTOM_PHOTO -> "Fotoğraf"
                        }
                        Surface(
                            color = if (sel) CyberCyan else DarkBg,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (sel) CyberCyan else Color(0xFF333333), RoundedCornerShape(6.dp))
                                .clickable {
                                    RearStateRepository.updateSettings { it.copy(rearWallpaperTheme = th) }
                                }
                        ) {
                            Text(
                                text = lbl,
                                color = if (sel) Color.Black else Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 5.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 2. One-Tap Test & Rear Screen Projection Actions
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Hızlı Simülasyon & Fiziksel Arka Ekrana Gönder",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            RearStateRepository.onIncomingCall(
                                CallEvent(
                                    callerName = "Test Araması",
                                    phoneNumber = "",
                                    sourceApp = "Telefon",
                                    status = CallStatus.RINGING
                                )
                            )
                            RearDisplayEngine.launchRearExperience(context, RearScreenMode.INCOMING_CALL)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Arama Testi", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            RearStateRepository.postNotification(
                                RearNotificationItem(
                                    id = "sim_${System.currentTimeMillis()}",
                                    packageName = "com.whatsapp",
                                    appName = "WhatsApp",
                                    sender = "Test Mesajı",
                                    message = "Arka ekran mesaj bildirimi testi."
                                )
                            )
                            RearDisplayEngine.launchRearExperience(context, RearScreenMode.NOTIFICATION_POPUP)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mesaj Testi", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        RearDisplayEngine.switchForegroundTaskToRear(context)
                        android.widget.Toast.makeText(
                            context,
                            "🚀 Ön ekrandaki uygulama arka ekrana aktarılıyor...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🚀 Ön Ekrandaki Uygulamayı Arka Ekrana Aktar",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        RearDisplayEngine.runFullShizukuDiagnosticAndSwitch(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LeicaRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SendToMobile, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🚀 Arka Ekrana Gönder (Shizuku + HyperOS 3 Köprüsü)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val shizukuReadyNow by RearStateRepository.shizukuReady.collectAsState()
                val shizukuState = remember(shizukuReadyNow) { RearDisplayEngine.readShizukuState(context) }
                val shizukuBannerText = when (shizukuState) {
                    ShizukuConnectionState.READY -> "✅ Shizuku Hazır & Yetkili (Arka Ekran Köprüsü Aktif)"
                    ShizukuConnectionState.NOT_ALLOWED -> "⚠️ Shizuku Çalışıyor — Dokunup MiRearUltra İznini Onaylayın"
                    ShizukuConnectionState.BLOCKED -> "❌ Shizuku İzni Reddedilmiş — Dokunup Shizuku'dan Aktif Edin"
                    ShizukuConnectionState.NOT_RUNNING -> "⚠️ Shizuku Servisi Çalışmıyor — Dokunup Shizuku'yu Başlatın"
                    ShizukuConnectionState.NOT_INSTALLED -> "❌ Shizuku Yüklü Değil — Xiaomi 17 Pro Max İçin Shizuku Kurun"
                }
                val shizukuBannerColor = when (shizukuState) {
                    ShizukuConnectionState.READY -> EmeraldGreen
                    ShizukuConnectionState.NOT_ALLOWED -> AmberGold
                    else -> LeicaRed
                }

                Surface(
                    color = shizukuBannerColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, shizukuBannerColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable {
                            RearDisplayEngine.requestShizukuPermission(context)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = shizukuBannerText,
                            color = shizukuBannerColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (shizukuState == ShizukuConnectionState.READY) "TEST ET ›" else "DÜZELT ›",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { RearDisplayEngine.openXiaomiNativeSubScreenSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23283B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "⚙️ Xiaomi Arka Ekran Ayarı",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            RearDisplayEngine.switchForegroundTaskToRear(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF23283B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🔄 Aktif Görevi Arka Ekrana Taşı",
                            color = EmeraldGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Xiaomi 17 & 18 Series Smart Display Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Xiaomi 17 & 18 Serisi Akıllı Ekran Seçici",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Android Auto / Kablosuz TV yansıtma çakışmasını önler",
                            color = Color(0xFF8C94A8),
                            fontSize = 10.sp
                        )
                    }
                    IconButton(
                        onClick = { RearDisplayEngine.scanDisplays(context) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile", tint = CyberCyan)
                    }
                }

                displays.forEach { d ->
                    val isSelectedTarget = (settings.preferredRearDisplayId == d.displayId) ||
                        (settings.preferredRearDisplayId == -1 && d.isRearHardwareDisplay)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBg)
                            .border(
                                width = 1.dp,
                                color = if (isSelectedTarget) EmeraldGreen else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (d.displayId != 0) {
                                    RearStateRepository.updateSettings { it.copy(preferredRearDisplayId = d.displayId) }
                                }
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ID #${d.displayId} • ${d.name}",
                                color = if (d.isRearHardwareDisplay) EmeraldGreen else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${d.widthPx}x${d.heightPx} px • ${d.refreshRateHz}Hz • ${d.densityDpi} DPI",
                                color = Color(0xFF8C94A8),
                                fontSize = 11.sp
                            )
                        }
                        Surface(
                            color = if (d.isRearHardwareDisplay) LeicaRed.copy(alpha = 0.2f) else Color(0xFF252A3D),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (d.isRearHardwareDisplay) "HEDEF ARKA EKRAN" else "ÖN EKRAN",
                                color = if (d.isRearHardwareDisplay) LeicaRed else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Real-Time Engine Activity Log
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Canlı Sistem & Arka Ekran Günlüğü",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                statusLog.take(8).forEach { logLine ->
                    Text(
                        text = "• $logLine",
                        color = Color(0xFFC2C9DB),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Clean, user-friendly Feature Module Manager Tab where users can toggle any rear-screen feature ON or OFF
 * with a single tap or select quick profiles!
 */
@Composable
private fun FeatureModulesToggleTab() {
    val settings by RearStateRepository.settings.collectAsState()

    val activeModuleCount = listOf(
        settings.modIncomingCall,
        settings.modSpeedDial,
        settings.modNotifications,
        settings.modMediaLyrics,
        settings.modLiveActivities,
        settings.modCyberTools,
        settings.modMiniGame
    ).count { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Arka Ekran Canlı Önizlemesi (Özellik Yöneticisinde Etkin)
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ARKA EKRAN ÖNİZLEMESİ",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "120Hz LTPO Canlı",
                        color = EmeraldGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                RearScreenInteractiveView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }
        }

        // Header + Quick Profile Presets
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Arka Ekran Özellik Yöneticisi",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Kullanmak istemediğiniz özellikleri kapatarak arka ekranı sadeleştirebilirsiniz.",
                            color = Color(0xFF9EA6BA),
                            fontSize = 11.sp
                        )
                    }
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "$activeModuleCount / 7 Aktif",
                            color = EmeraldGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // 3 One-Tap Profile Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            RearStateRepository.updateSettings {
                                it.copy(
                                    modIncomingCall = true,
                                    modSpeedDial = true,
                                    modNotifications = true,
                                    modTextToSpeech = true,
                                    modLiveActivities = true,
                                    modDailyCards = true,
                                    modMediaLyrics = true,
                                    modAppLauncher = true,
                                    modTeleprompter = true,
                                    modLedSignAndSos = true,
                                    modCyberTools = true,
                                    modMiniGame = true,
                                    modStealthAndWave = true
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LeicaRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("✨ Tümünü Aç", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            RearStateRepository.updateSettings {
                                it.copy(
                                    modIncomingCall = true,
                                    modSpeedDial = true,
                                    modNotifications = true,
                                    modTextToSpeech = true,
                                    modLiveActivities = true,
                                    modDailyCards = false,
                                    modMediaLyrics = true,
                                    modAppLauncher = false,
                                    modTeleprompter = false,
                                    modLedSignAndSos = false,
                                    modCyberTools = false,
                                    modMiniGame = false,
                                    modStealthAndWave = true
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📞 İletişim", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            RearStateRepository.updateSettings {
                                it.copy(
                                    modIncomingCall = true,
                                    modSpeedDial = false,
                                    modNotifications = true,
                                    modTextToSpeech = false,
                                    modLiveActivities = false,
                                    modDailyCards = false,
                                    modMediaLyrics = false,
                                    modAppLauncher = false,
                                    modTeleprompter = false,
                                    modLedSignAndSos = false,
                                    modCyberTools = false,
                                    modMiniGame = false,
                                    modStealthAndWave = false
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⌚ Minimal", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Category 1: Communication & Calls
        ModuleCategoryCard(
            categoryTitle = "📞 İLETİŞİM & ARAMA MODÜLLERİ",
            accentColor = EmeraldGreen
        ) {
            SettingSwitchRow(
                title = "1. Gelen Arama Ekranı (Gizlilik Korumalı)",
                subtitle = "Çalarken kimin aradığını gösterir ve cevaplama butonları sunar. Arama açıldığı an arka ekran hemen saat moduna döner (gizlilik).",
                checked = settings.modIncomingCall,
                onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(modIncomingCall = v) } }
            )
            SettingSwitchRow(
                title = "2. Hızlı Arama (En Sık Aranan 6 Kişi)",
                subtitle = "Rehberinizden en çok aradığınız 6 kişiyi arka ekranda listeler; tek dokunuşla eller serbest arama başlatır.",
                checked = settings.modSpeedDial,
                onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(modSpeedDial = v) } }
            )
            SettingSwitchRow(
                title = "3. Üstten Çekilen Bildirim Perdesi & Kayan Bildirim",
                subtitle = "Ekranın tam tepesinden aşağı çekildiğinde tüm bildirimleri listeler; yeni bildirimler kayarak tam okunur.",
                checked = settings.modNotifications,
                onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(modNotifications = v) } }
            )
        }

        // Hızlı Aranacak Kişileri Seçme ve Yönetme Kartı (En Sık Aranan 6 Kişi)
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val ctx = LocalContext.current
            var showManualAddDialog by remember { mutableStateOf(false) }
            var manualName by remember { mutableStateOf("") }
            var manualPhone by remember { mutableStateOf("") }

            val contactPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickContact()
            ) { contactUri ->
                if (contactUri != null) {
                    val cr = ctx.contentResolver
                    runCatching {
                        cr.query(contactUri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                                val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                                val hasPhoneIdx = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                                val contactId = if (idIdx >= 0) cursor.getString(idIdx) else null
                                val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty().ifBlank { "İsimsiz" } else "İsimsiz"
                                val hasPhone = if (hasPhoneIdx >= 0) cursor.getInt(hasPhoneIdx) > 0 else false

                                var phone = ""
                                if (hasPhone && contactId != null) {
                                    cr.query(
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                        arrayOf(contactId),
                                        null
                                    )?.use { pCursor ->
                                        if (pCursor.moveToFirst()) {
                                            val pIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                            if (pIdx >= 0) phone = pCursor.getString(pIdx).orEmpty()
                                        }
                                    }
                                }
                                if (phone.isNotBlank()) {
                                    val current = settings.speedDialContacts.toMutableList()
                                    if (current.size >= 6) {
                                        current.removeAt(current.size - 1)
                                    }
                                    current.add(SpeedDialContact(name = name, phone = phone, roleLabel = "Seçilen"))
                                    RearStateRepository.updateSettings { it.copy(speedDialContacts = current) }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "👥 Hızlı Aranacak Kişiler (6 Kişi)",
                            color = EmeraldGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Arka ekranda tek tıkla hoparlörden aramak istediğiniz kişileri belirleyin.",
                            color = Color(0xFF9EA6BA),
                            fontSize = 11.sp
                        )
                    }
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "${settings.speedDialContacts.size} / 6",
                            color = EmeraldGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (settings.speedDialContacts.isEmpty()) {
                    Text(
                        text = "Henüz kişi seçilmedi. Aşağıdaki butonlarla rehberinizden kişi ekleyebilirsiniz.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    settings.speedDialContacts.forEachIndexed { index, contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBg)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "•",
                                        color = EmeraldGreen,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${index + 1}. ${contact.name}",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = contact.phone,
                                        color = Color(0xFF9EA6BA),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    val updated = settings.speedDialContacts.toMutableList()
                                    updated.removeAt(index)
                                    RearStateRepository.updateSettings { it.copy(speedDialContacts = updated) }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Sil",
                                    tint = LeicaRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Butonlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { contactPickerLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rehberden Seç", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showManualAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Manuel Ekle", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = {
                        val auto = com.mirearultra.pro.core.ContactResolver.getFrequentContacts(ctx).take(6)
                        if (auto.isNotEmpty()) {
                            RearStateRepository.updateSettings { it.copy(speedDialContacts = auto) }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("🔄 En Çok Arananları Otomatik Doldur", color = CyberCyan, fontSize = 11.sp)
                }
            }

            if (showManualAddDialog) {
                AlertDialog(
                    onDismissRequest = { showManualAddDialog = false },
                    title = { Text("Hızlı Arama Kişisi Ekle", color = Color.White) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualName,
                                onValueChange = { manualName = it },
                                label = { Text("Kişi Adı") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = manualPhone,
                                onValueChange = { manualPhone = it },
                                label = { Text("Telefon Numarası") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (manualName.isNotBlank() && manualPhone.isNotBlank()) {
                                    val current = settings.speedDialContacts.toMutableList()
                                    if (current.size >= 6) {
                                        current.removeAt(current.size - 1)
                                    }
                                    current.add(SpeedDialContact(name = manualName.trim(), phone = manualPhone.trim(), roleLabel = "Özel"))
                                    RearStateRepository.updateSettings { it.copy(speedDialContacts = current) }
                                    manualName = ""
                                    manualPhone = ""
                                    showManualAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Text("Ekle", color = Color.Black)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showManualAddDialog = false }) {
                            Text("İptal", color = Color.Gray)
                        }
                    },
                    containerColor = CardBg
                )
            }
        }

        // Category 2: Media & Live Tracking
        ModuleCategoryCard(
            categoryTitle = "🎵 MEDYA & CANLI TAKİP",
            accentColor = CyberCyan
        ) {
            SettingSwitchRow(
                title = "4. YouTube Music & Medya Çalar Kontrolleri",
                subtitle = "Dönen plak diski, parça ve sanatçı bilgileri, şarkı geçiş butonları ve dokunulmadığında otomatik ambient karartma.",
                checked = settings.modMediaLyrics,
                onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(modMediaLyrics = v) } }
            )
            SettingSwitchRow(
                title = "5. Dijital Hız Göstergesi & Navigasyon",
                subtitle = "Canlı sürüş hızı (km/s) ve Google Haritalar dönüş manevralarını arka ekranda gösterir.",
                checked = settings.modLiveActivities,
                onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(modLiveActivities = v) } }
            )
        }

        // Category 3: Tools & Games
        ModuleCategoryCard(
            categoryTitle = "🛠️ HIZLI ARAÇLAR & OYUN",
            accentColor = AmberGold
        ) {
            SettingSwitchRow(
                title = "6. Hızlı Araçlar (El Feneri, Ses Kaydı, Su Terazisi)",
                subtitle = "Kamera flaş feneri anahtarı, toplantı sesli not kaydedici ve donanımsal su terazisi.",
                checked = settings.modCyberTools,
                onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(modCyberTools = v) } }
            )
            SettingSwitchRow(
                title = "7. 120Hz LTPO Retro Pong Mini Oyunu",
                subtitle = "Arka ekranda 120Hz hızında dokunmatik raketle oynanabilen klasik ping-pong oyunu.",
                checked = settings.modMiniGame,
                onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(modMiniGame = v) } }
            )
        }
    }
}

@Composable
private fun ModuleCategoryCard(
    categoryTitle: String,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = categoryTitle,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
            content()
        }
    }
}

@Composable
private fun SetupAndPlayPolicyTab() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val shizukuReady by RearStateRepository.shizukuReady.collectAsState()
    val notifConnectedState by RearStateRepository.notificationListenerConnected.collectAsState()
    var showProminentDisclosureDialog by remember { mutableStateOf(false) }

    fun checkPhoneAndMicGranted(ctx: Context): Boolean {
        val required = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO
        )
        return required.all {
            ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkNotifListenerEnabled(ctx: Context): Boolean {
        return runCatching {
            NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
        }.getOrDefault(false) || notifConnectedState
    }

    var phoneAndMicGranted by remember { mutableStateOf(checkPhoneAndMicGranted(context)) }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var notifGranted by remember { mutableStateOf(checkNotifListenerEnabled(context)) }
    var ignoringBattery by remember {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        mutableStateOf(pm.isIgnoringBatteryOptimizations(context.packageName))
    }

    // Automatically refresh all permission statuses when returning from Android Settings!
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                phoneAndMicGranted = checkPhoneAndMicGranted(context)
                overlayGranted = Settings.canDrawOverlays(context)
                notifGranted = checkNotifListenerEnabled(context)
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                ignoringBattery = pm.isIgnoringBatteryOptimizations(context.packageName)
                RearDisplayEngine.checkShizukuStatus()
                RearDisplayEngine.scanDisplays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val runtimePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        phoneAndMicGranted = checkPhoneAndMicGranted(context)
        val grantedCount = results.values.count { it }
        RearStateRepository.appendLog("🔐 Telefon ve Mikrofon izinleri onaylandı ($grantedCount izin aktif).")
    }

    if (showProminentDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showProminentDisclosureDialog = false },
            title = {
                Text("Açık Rıza & Bildirim Erişimi Bilgilendirmesi", fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Text(
                    "MiRearUltra, gelen aramaları, mesaj bildirimlerini (WhatsApp, Telegram, SMS) ve navigasyon/medya durumunu Xiaomi 17 & 18 Pro Max cihazınızın arka ekranında göstermek ve yanıtlamanızı sağlamak için Bildirim Erişimi (NotificationListenerService) izni kullanır.\n\n" +
                        "🔒 GİZLİLİK GARANTİSİ: Uygulama İNTERNET İZNİ İSTEMEZ. Hiçbir bildirim, arama veya kişisel veriniz sunuculara aktarılmaz, kaydedilmez ve üçüncü taraflarla paylaşılmaz; tüm işlemler %100 cihaz içinde gerçekleşir."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProminentDisclosureDialog = false
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("Kabul Et ve Ayarları Aç", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProminentDisclosureDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Step 0: Overlay Permission (CRITICAL for Rear Screen Window Projection without Root)
        PermissionStepCard(
            stepNumber = "1",
            title = "Diğer Uygulamaların Üzerinde Göster (Arka Ekran Pencere İzni)",
            description = "EN ÖNEMLİ İZİN: Xiaomi HyperOS'ta arka ekrana (SubScreen) doğrudan saat, arama ve mesaj penceresi yansıtmak için gereklidir.",
            isGranted = overlayGranted,
            buttonText = if (overlayGranted) "✓ Arka Ekran Pencere İzni Açık (Yeşil)" else "Üstte Göster İznini Aç",
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    )
                }
            }
        )

        // Step 1: Notification Listener (with Prominent Disclosure Dialog)
        PermissionStepCard(
            stepNumber = "2",
            title = "Bildirim, Mesaj & Gelen Çağrı Erişimi",
            description = "Gelen aramaları, WhatsApp/Telegram/SMS mesajlarını ve canlı navigasyonu arka ekranda göstermek için gereklidir.",
            isGranted = notifGranted,
            buttonText = if (notifGranted) "✓ Bildirim Erişimi Açık (Yeşil)" else "Açık Rıza Beyanını Gör & İzin Ver",
            onClick = { showProminentDisclosureDialog = true }
        )

        // Step 2: Phone / Answer Calls / Audio Runtime Permissions (LIVE CHECK FIXED!)
        PermissionStepCard(
            stepNumber = "3",
            title = "Arama Cevaplama, Hoparlör & Sesli Not İzinleri",
            description = "Arka ekrandan aramayı cevaplamak, otomatik hoparlöre almak ve sesli not kaydetmek için gereklidir.",
            isGranted = phoneAndMicGranted,
            buttonText = if (phoneAndMicGranted) "✓ Telefon & Mikrofon İzinleri Onaylandı (Yeşil)" else "Telefon & Mikrofon İzinlerini Onayla",
            onClick = {
                val perms = mutableListOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ANSWER_PHONE_CALLS,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.RECORD_AUDIO
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                runtimePermLauncher.launch(perms.toTypedArray())
            }
        )

        // Step 3: Shizuku Bridge (Critical for Stock HyperOS 3 Rear Display Activity & Task Routing)
        val currentShizukuState = RearDisplayEngine.readShizukuState(context)
        PermissionStepCard(
            stepNumber = "4",
            title = "Shizuku Köprüsü (Xiaomi 17 Pro Max HyperOS 3 Arka Ekran Yetkisi)",
            description = "Xiaomi HyperOS 3'te arka ekranı (976x596 Display #1) uyandırmak, miui.rear.policy ile pencere taşımak ve SubScreenLauncher kapanmasını önlemek için kullanılır. Durum: $currentShizukuState",
            isGranted = shizukuReady,
            buttonText = when (currentShizukuState) {
                ShizukuConnectionState.READY -> "✓ Shizuku Yetkisi Aktif — Arka Ekranı Şimdi Test Et"
                ShizukuConnectionState.NOT_ALLOWED -> "⚠️ Shizuku Çalışıyor — İzin Penceresini Aç"
                ShizukuConnectionState.BLOCKED -> "❌ İzin Reddedilmiş — Shizuku Uygulamasından Aç"
                ShizukuConnectionState.NOT_RUNNING -> "⚠️ Shizuku Servisi Kapalı — Shizuku'yu Aç ve Başlat"
                ShizukuConnectionState.NOT_INSTALLED -> "❌ Shizuku Yüklü Değil"
            },
            onClick = { RearDisplayEngine.requestShizukuPermission(context) }
        )

        // Step 4: Battery Optimization Settings
        PermissionStepCard(
            stepNumber = "5",
            title = "Arka Plan Pil Ayarları (HyperOS Keep-Alive)",
            description = "Telefon kilitliyken veya masada yüzüstü dururken arka ekran servisinin kesintisiz çalışmasını sağlar.",
            isGranted = ignoringBattery,
            buttonText = if (ignoringBattery) "✓ Pil Ayarı Optimize Edildi (Yeşil)" else "Pil Optimizasyon Ayarlarını Aç",
            onClick = {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            }
        )
    }
}

@Composable
private fun PermissionStepCard(
    stepNumber: String,
    title: String,
    description: String,
    isGranted: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isGranted) EmeraldGreen else LeicaRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber,
                            color = if (isGranted) Color.Black else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) EmeraldGreen else AmberGold
                )
            }

            Text(
                text = description,
                color = Color(0xFFA7B0C4),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) Color(0xFF22352C) else LeicaRed
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = buttonText,
                    color = if (isGranted) EmeraldGreen else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CustomizationStudioTab() {
    val context = LocalContext.current
    val settings by RearStateRepository.settings.collectAsState()
    var newTemplateText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 🌍 10 Dil Desteği / Language Selector (Top 10 Languages)
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🌍 Dil / Language (Top 10)",
                            color = CyberCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Arka ve ön ekran dili (Otomatik sistem dili desteklenir):",
                            color = Color(0xFF9EA6BA),
                            fontSize = 11.sp
                        )
                    }
                }

                com.mirearultra.pro.core.AppLanguage.entries.forEach { lang ->
                    val selected = settings.appLanguage == lang.code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) CyberCyan.copy(alpha = 0.2f) else DarkBg)
                            .border(
                                width = 1.dp,
                                color = if (selected) CyberCyan else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                RearStateRepository.updateSettings { it.copy(appLanguage = lang.code) }
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(lang.nativeName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(lang.englishName, color = Color(0xFF9EA6BA), fontSize = 11.sp)
                        }
                        if (selected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberCyan)
                        }
                    }
                }
            }
        }

        // 1. Battery & LTPO 1Hz Power Optimization Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldGreen.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🔋 Pil & LTPO 1Hz Güç Optimizasyonu",
                    color = EmeraldGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Arka ekran otomatik uyku süresi: ${if (settings.rearScreenTimeoutSec == 0) "Sürekli Açık" else "${settings.rearScreenTimeoutSec} saniye"}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(10 to "10 sn", 20 to "20 sn", 30 to "30 sn", 60 to "60 sn", 0 to "Sürekli").forEach { (sec, lbl) ->
                        val sel = settings.rearScreenTimeoutSec == sec
                        Surface(
                            color = if (sel) EmeraldGreen else DarkBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    RearStateRepository.updateSettings { it.copy(rearScreenTimeoutSec = sec) }
                                }
                        ) {
                            Text(
                                text = lbl,
                                color = if (sel) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                SettingSwitchRow(
                    title = "LTPO 1Hz Eko Saat Modu (Saniyeyi & Boşta 120Hz Animasyonu Kapat)",
                    subtitle = "Ana ekranda saniye yenilemesini ve çerçeve animasyonunu durdurarak Xiaomi LTPO panelinin 1Hz'e düşmesini sağlar (%80'e varan ekran pil tasarrufu).",
                    checked = settings.ltpo1HzClockMode,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(ltpo1HzClockMode = v) } }
                )

                SettingSwitchRow(
                    title = "Ön Ekran Kullanımdaysa Arka Ekranı Uyandırma",
                    subtitle = "Telefonun kilidi açıkken ve ön ekrana bakıyorken gelen normal bildirimlerde arka ekranı boşuna uyandırmaz.",
                    checked = settings.skipRearWakeWhenFrontActive,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(skipRearWakeWhenFrontActive = v) } }
                )

                SettingSwitchRow(
                    title = "%20 Pil Altında Otomatik Ultra Tasarruf Modu",
                    subtitle = "Şarj %20'nin altına düştüğünde arka ekran efektlerini ve sensör sıklığını otomatik kısar.",
                    checked = settings.autoEcoBelow20Percent,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(autoEcoBelow20Percent = v) } }
                )
            }
        }

        // 1b. Call, Privacy & Sensor Automation Behavior
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Otomasyon, Gizlilik & Panel Koruması",
                    color = CyberCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                SettingSwitchRow(
                    title = "Kulak Yakınlık Gizliliği (Görüşmede Arka Ekranı Karart)",
                    subtitle = "Telefon kulağınıza götürüldüğünde arka ekrandaki tüm arama ve arayan kişi bilgilerini anında tamamen karartır.",
                    checked = settings.earProximityCallPrivacy,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(earProximityCallPrivacy = v) } }
                )

                SettingSwitchRow(
                    title = "Arayan Numara Maskeleme",
                    subtitle = "Gelen aramalarda telefon numarasını maskeleyerek etraftaki yabancıların görmesini engeller.",
                    checked = settings.maskCallerPhoneNumber,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(maskCallerPhoneNumber = v) } }
                )

                SettingSwitchRow(
                    title = "Hızlı Arama Numara Maskeleme",
                    subtitle = "Arka ekranda hızlı arama sayfasında kişilerin telefon numaralarını gizler.",
                    checked = settings.maskSpeedDialNumbers,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(maskSpeedDialNumbers = v) } }
                )

                SettingSwitchRow(
                    title = "Arka Ekrandan Cevaplayınca Otomatik Hoparlörü Aç",
                    subtitle = "Telefon yüzüstü masadayken aramayı açtığınızda otomatik olarak hoparlörü etkinleştirir.",
                    checked = settings.autoSpeakerOnRearAnswer,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(autoSpeakerOnRearAnswer = v) } }
                )

                SettingSwitchRow(
                    title = "Gizlilik Maskesi (Mesaj İçeriğini Dokunana Kadar Gizle)",
                    subtitle = "Siz ekrana dokunana kadar mesaj metnini etraftakilerden gizler.",
                    checked = settings.privacyMaskNotifications,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(privacyMaskNotifications = v) } }
                )

                SettingSwitchRow(
                    title = "El Sallayarak Gelen Aramayı Sustur (Wave Gesture)",
                    subtitle = "Elinizi yakınlık sensörünün üzerinden geçirdiğinizde çalan zil sesini susturur.",
                    checked = settings.waveGestureSilenceCall,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(waveGestureSilenceCall = v) } }
                )

                SettingSwitchRow(
                    title = "Akıllı Cep Koruması (Cepteyken Arka Ekranı Tamamen Kapat / Uyut)",
                    subtitle = "Telefon cebe konulduğunda yakınlık sensörü algılar ve arka ekranı anında saf AMOLED siyahına alarak uyutur, dokunmaları kilitler ve pil harcamasını sıfırlar.",
                    checked = settings.pocketModeProtection,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(pocketModeProtection = v) } }
                )

                SettingSwitchRow(
                    title = "AMOLED Yanık Koruması (Pixel Shift)",
                    subtitle = "Ekran yanığını önler ve cepteyken yanlış dokunmaları engeller.",
                    checked = settings.amoledBurnInProtection,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(amoledBurnInProtection = v) } }
                )


            }
        }

        // Duvar Kağıdı & Arka Plan Temaları Kartı
        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                runCatching {
                    val input = context.contentResolver.openInputStream(uri)
                    val destFile = java.io.File(context.filesDir, "custom_rear_wallpaper.png")
                    destFile.outputStream().use { out -> input?.copyTo(out) }
                    RearStateRepository.updateSettings {
                        it.copy(
                            rearWallpaperTheme = com.mirearultra.pro.core.RearWallpaperTheme.CUSTOM_PHOTO,
                            customWallpaperPath = destFile.absolutePath
                        )
                    }
                    android.widget.Toast.makeText(context, "✅ Özel fotoğraf arka ekran duvar kağıdı yapıldı!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }



        // 2. Clock Face & Theme Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Arka Ekran Saat & Tema Stili",
                    color = AmberGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                ClockFaceStyle.entries.forEach { style ->
                    val selected = settings.clockFaceStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) LeicaRed.copy(alpha = 0.2f) else DarkBg)
                            .border(
                                width = 1.dp,
                                color = if (selected) LeicaRed else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                RearStateRepository.updateSettings { it.copy(clockFaceStyle = style) }
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(style.titleTr, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(style.subtitleTr, color = Color(0xFF9EA6BA), fontSize = 11.sp)
                        }
                        if (selected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LeicaRed)
                        }
                    }
                }
            }
        }



        // 4. Quick Reply Templates & Rear DPI
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Arka Ekran DPI (${settings.targetRearDpi}) & Hızlı Yanıt Şablonları",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = settings.targetRearDpi.toFloat(),
                    onValueChange = { v ->
                        RearStateRepository.updateSettings { it.copy(targetRearDpi = v.toInt()) }
                    },
                    valueRange = 160f..360f
                )
                Button(
                    onClick = { RearDisplayEngine.resetRearDisplayDpi(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Arka Ekran DPI Değerini Fabrika Ayarına Sıfırla", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTemplateText,
                        onValueChange = { newTemplateText = it },
                        placeholder = { Text("Yeni hızlı yanıt şablonu ekle...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newTemplateText.isNotBlank()) {
                                val added = newTemplateText.trim()
                                RearStateRepository.updateSettings {
                                    it.copy(quickReplyTemplates = listOf(added) + it.quickReplyTemplates)
                                }
                                newTemplateText = ""
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldGreen)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Ekle", tint = Color.Black)
                    }
                }

                settings.quickReplyTemplates.forEach { tpl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBg)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tpl,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                RearStateRepository.updateSettings { s ->
                                    s.copy(quickReplyTemplates = s.quickReplyTemplates.filterNot { it == tpl })
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = LeicaRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBg)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = Color(0xFF9BA3B8), fontSize = 11.sp, lineHeight = 15.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

