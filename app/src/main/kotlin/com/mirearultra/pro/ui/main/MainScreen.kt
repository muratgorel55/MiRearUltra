package com.mirearultra.pro.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SendToMobile
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavKey
import com.mirearultra.pro.core.CallEvent
import com.mirearultra.pro.core.CallStatus
import com.mirearultra.pro.core.ClockFaceStyle
import com.mirearultra.pro.core.RearDisplayEngine
import com.mirearultra.pro.core.RearNotificationItem
import com.mirearultra.pro.core.RearScreenMode
import com.mirearultra.pro.core.RearStateRepository
import com.mirearultra.pro.core.RearWallpaperTheme
import com.mirearultra.pro.core.ShizukuConnectionState
import com.mirearultra.pro.core.SpeedDialContact
import com.mirearultra.pro.rear.RearScreenInteractiveView
import java.io.File

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
                                text = "Arka Ekran",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Xiaomi 17 Pro Max • Ultra Denetim",
                                color = Color(0xFF9BA3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        color = if (shizukuReady) EmeraldGreen.copy(alpha = 0.18f) else if (notifConnected) CyberCyan.copy(alpha = 0.18f) else AmberGold.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (shizukuReady) "SHIZUKU AKTİF" else if (notifConnected) "SERVİS AKTİF" else "KURULUM",
                            color = if (shizukuReady) EmeraldGreen else if (notifConnected) CyberCyan else AmberGold,
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
                    Triple(0, Icons.Default.PhoneAndroid, "Kontrol"),
                    Triple(1, Icons.Default.Tune, "Modüller"),
                    Triple(2, Icons.Default.Security, "Koruma & Pil"),
                    Triple(3, Icons.Default.Build, "Sistem & İzin")
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
                2 -> ProtectionAndPowerTab()
                else -> SetupAndSystemTab()
            }
        }
    }
}

@Composable
private fun LiveRearOverviewTab(
    onOpenModulesTab: () -> Unit
) {
    val context = LocalContext.current
    val settings by RearStateRepository.settings.collectAsState()
    val isProjecting by RearStateRepository.isProjectingTaskToRear.collectAsState()
    val shizukuReadyNow by RearStateRepository.shizukuReady.collectAsState()
    val shizukuState = remember(shizukuReadyNow) { RearDisplayEngine.readShizukuState(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Canlı Arka Ekran Çerçevesi & Tema Seçimi
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CANLI ARKA EKRAN",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Surface(
                        color = LeicaRed.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onOpenModulesTab() }
                    ) {
                        Text(
                            text = "Modüller ›",
                            color = LeicaRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                RearScreenInteractiveView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tema Seçim Çipleri
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    RearWallpaperTheme.entries.forEach { th ->
                        val sel = settings.rearWallpaperTheme == th
                        val lbl = when (th) {
                            RearWallpaperTheme.MINIMAL_OBSIDIAN -> "Obsidyen"
                            RearWallpaperTheme.COSMIC_NEBULA -> "Nebula"
                            RearWallpaperTheme.CYBER_LEICA -> "Leica"
                            RearWallpaperTheme.NORDIC_AURORA -> "Aurora"
                            RearWallpaperTheme.CARBON_FIBER -> "Karbon"
                            RearWallpaperTheme.SUNSET_AMBER -> "Gün Batımı"
                            RearWallpaperTheme.CUSTOM_PHOTO -> "Foto"
                        }
                        Surface(
                            color = if (sel) CyberCyan else DarkBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (sel) CyberCyan else Color(0xFF333333), RoundedCornerShape(8.dp))
                                .clickable {
                                    RearStateRepository.updateSettings { it.copy(rearWallpaperTheme = th) }
                                }
                        ) {
                            Text(
                                text = lbl,
                                color = if (sel) Color.Black else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 2. Ana Kontrol & Eylemler Kartı
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Hızlı Kontrol & Eylemler",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Shizuku Durum Çubuğu
                val shizukuBannerText = when (shizukuState) {
                    ShizukuConnectionState.READY -> "✅ Shizuku Köprüsü Hazır"
                    ShizukuConnectionState.NOT_ALLOWED -> "⚠️ MiRearUltra İznini Onaylayın"
                    ShizukuConnectionState.BLOCKED -> "❌ Shizuku İzni Reddedilmiş"
                    ShizukuConnectionState.NOT_RUNNING -> "⚠️ Shizuku Servisini Başlatın"
                    ShizukuConnectionState.NOT_INSTALLED -> "❌ Shizuku Uygulaması Yüklü Değil"
                }
                val shizukuBannerColor = when (shizukuState) {
                    ShizukuConnectionState.READY -> EmeraldGreen
                    ShizukuConnectionState.NOT_ALLOWED -> AmberGold
                    else -> LeicaRed
                }

                Surface(
                    color = shizukuBannerColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, shizukuBannerColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable { RearDisplayEngine.requestShizukuPermission(context) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = shizukuBannerText,
                            color = shizukuBannerColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (shizukuState == ShizukuConnectionState.READY) "HAZIR" else "DÜZELT ›",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // 📲 Ana Eylem: Arka Ekrana Aktar / Saate Dön
                Button(
                    onClick = {
                        if (isProjecting) {
                            RearDisplayEngine.stopProjection(context)
                            Toast.makeText(context, "⏹️ Yansıtma durduruldu, saate dönüldü.", Toast.LENGTH_SHORT).show()
                        } else {
                            RearDisplayEngine.switchForegroundTaskToRear(context)
                            Toast.makeText(context, "📲 Ön ekrandaki uygulama arka ekrana aktarılıyor...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isProjecting) Color(0xFFD32F2F) else LeicaRed
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(
                        imageVector = if (isProjecting) Icons.Default.Close else Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isProjecting) "⏹️ Yansıtmayı Durdur (Saate Dön)" else "📲 Ön Ekrandaki Uygulamayı Arka Ekrana Aktar",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 2 İkincil Buton: Ekran Görüntüsü & Orijinal Xiaomi Arayüzü
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            RearDisplayEngine.takeRearScreenshot(context) { success, _ ->
                                Toast.makeText(
                                    context,
                                    if (success) "📸 Arka ekran görüntüsü Galeri'ye kaydedildi!" else "❌ Ekran görüntüsü alınamadı (Shizuku gerekli).",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📸 Ekran Görüntüsü Al", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            RearDisplayEngine.restoreXiaomiOriginalRearDisplay(context)
                            Toast.makeText(context, "🔄 Xiaomi orijinal arka ekranına dönülüyor...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Orijinal Arayüz", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Ekran Yönü (Rotation) & DPI Ölçekleme Kartı
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ekran Yönlendirme & DPI Ayarı",
                    color = CyberCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Döndürme Butonları (0°, 90°, 180°, 270°)
                Text(
                    text = "Arka Ekran Döndürme (Yön):",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0 to "0° (Doğal)", 1 to "90°", 2 to "180°", 3 to "270°").forEach { (rot, lbl) ->
                        val sel = settings.rearDisplayRotation == rot
                        Surface(
                            color = if (sel) LeicaRed else DarkBg,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (sel) LeicaRed else Color(0xFF333333), RoundedCornerShape(10.dp))
                                .clickable {
                                    RearDisplayEngine.setRearDisplayRotation(context, rot)
                                }
                        ) {
                            Text(
                                text = lbl,
                                color = if (sel) Color.White else Color(0xFFB0B8C8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // DPI Ayarı Slider & Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Arka Ekran DPI:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${settings.targetRearDpi} DPI",
                        color = EmeraldGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Slider(
                    value = settings.targetRearDpi.toFloat(),
                    onValueChange = { v ->
                        RearStateRepository.updateSettings { it.copy(targetRearDpi = v.toInt()) }
                    },
                    onValueChangeFinished = {
                        RearDisplayEngine.setRearDisplayDpi(context, settings.targetRearDpi)
                    },
                    valueRange = 200f..380f
                )

                Button(
                    onClick = { RearDisplayEngine.resetRearDisplayDpi(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("DPI Değerini Varsayılana Sıfırla (240 DPI)", color = Color(0xFF8C94A8), fontSize = 11.sp)
                }
            }
        }

        // 4. Hızlı Simülasyon Testleri Kartı
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
                    text = "Hızlı Test Simülasyonu",
                    color = AmberGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📞 Arama Testi", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("💬 Mesaj Testi", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

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
        // Hızlı Profil Kartı
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
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Kullanmak istediğiniz modülleri tek dokunuşla seçin.",
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
                                    modMediaLyrics = true,
                                    modLiveActivities = true,
                                    modCyberTools = true,
                                    modMiniGame = true
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
                                    modMediaLyrics = true,
                                    modLiveActivities = false,
                                    modCyberTools = false,
                                    modMiniGame = false
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
                                    modMediaLyrics = false,
                                    modLiveActivities = false,
                                    modCyberTools = false,
                                    modMiniGame = false
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

        // Kategori 1: İletişim & Arama Modülleri
        ModuleCategoryCard(
            categoryTitle = "📞 İLETİŞİM & ARAMA",
            accentColor = EmeraldGreen
        ) {
            SettingSwitchRow(
                title = "1. Gelen Arama Ekranı (Gizlilik Korumalı)",
                subtitle = "Çalarken kimin aradığını gösterir ve cevaplama butonları sunar. Arama açıldığı an arka ekran hemen saat moduna döner.",
                checked = settings.modIncomingCall,
                onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(modIncomingCall = v) } }
            )
            SettingSwitchRow(
                title = "2. Hızlı Arama (En Sık Aranan 6 Kişi)",
                subtitle = "Rehberinizden belirlediğiniz 6 kişiyi arka ekranda listeler; tek dokunuşla arama başlatır.",
                checked = settings.modSpeedDial,
                onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(modSpeedDial = v) } }
            )
            SettingSwitchRow(
                title = "3. Üstten Çekilen Bildirim Perdesi",
                subtitle = "Ekranın tam tepesinden aşağı çekildiğinde tüm bildirimleri listeler; yeni bildirimler kayarak okunur.",
                checked = settings.modNotifications,
                onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(modNotifications = v) } }
            )
        }

        // Akıllı Bildirim Seçenekleri (MRSS'ten İlham Alan Geliştirmeler)
        ModuleCategoryCard(
            categoryTitle = "🔔 AKILLI BİLDİRİM & GİZLİLİK AYARLARI",
            accentColor = CyberCyan
        ) {
            SettingSwitchRow(
                title = "Yalnızca Telefon Yüzüstü Dururken Bildir",
                subtitle = "Telefon masada veya zeminde yüzüstü dururken arka ekran bildirim gösterir; telefon önü açıksa arka ekran boşuna uyanmaz.",
                checked = settings.onlyWhenFaceDown,
                onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(onlyWhenFaceDown = v) } }
            )
            SettingSwitchRow(
                title = "Rahatsız Etmeyin (DND) Modunu Takip Et",
                subtitle = "Sistem Rahatsız Etmeyin modu aktifken arka ekranın bildirimlerle uyanmasını engeller.",
                checked = settings.followDndMode,
                onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(followDndMode = v) } }
            )
            SettingSwitchRow(
                title = "Bildirim Gizlilik Maskesi",
                subtitle = "Arka ekranda mesaj içeriğini gizler; yalnızca kimin gönderdiğini ve yeni mesaj ikonunu gösterir.",
                checked = settings.privacyMaskNotifications,
                onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(privacyMaskNotifications = v) } }
            )
        }

        // Hızlı Aranacak Kişiler Kartı
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
                            text = "👥 Hızlı Aranacak Kişiler (Maks. 6)",
                            color = EmeraldGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Arka ekranda tek tıkla aramak istediğiniz kişileri belirleyin.",
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
                        text = "Henüz kişi seçilmedi. Rehberinizden kişi ekleyebilirsiniz.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    settings.speedDialContacts.forEachIndexed { index, contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkBg)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 1}. ${contact.name}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = contact.phone,
                                    color = Color(0xFF8C94A8),
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    val updated = settings.speedDialContacts.toMutableList()
                                    updated.removeAt(index)
                                    RearStateRepository.updateSettings { it.copy(speedDialContacts = updated) }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = LeicaRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

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
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
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

        // Kategori 2: Medya & Canlı Takip
        ModuleCategoryCard(
            categoryTitle = "🎵 MEDYA & CANLI TAKİP",
            accentColor = CyberCyan
        ) {
            SettingSwitchRow(
                title = "4. YouTube Music / Medya Çalar Kontrolleri",
                subtitle = "Şarkı bilgileri, albüm kapağı, parça geçiş butonları ve dokunulmadığında otomatik ambient karartma.",
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

        // Kategori 3: Hızlı Araçlar & Mini Oyun
        ModuleCategoryCard(
            categoryTitle = "🛠️ HIZLI ARAÇLAR & OYUN",
            accentColor = AmberGold
        ) {
            SettingSwitchRow(
                title = "6. Hızlı Araçlar (El Feneri, Ses Kaydı, Su Terazisi)",
                subtitle = "Flaş fener anahtarı, toplantı ses kaydedici ve donanımsal su terazisi.",
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
private fun ProtectionAndPowerTab() {
    val context = LocalContext.current
    val settings by RearStateRepository.settings.collectAsState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val input = context.contentResolver.openInputStream(uri)
                val destFile = File(context.filesDir, "custom_rear_wallpaper.png")
                destFile.outputStream().use { out -> input?.copyTo(out) }
                RearStateRepository.updateSettings {
                    it.copy(
                        rearWallpaperTheme = RearWallpaperTheme.CUSTOM_PHOTO,
                        customWallpaperPath = destFile.absolutePath
                    )
                }
                Toast.makeText(context, "✅ Özel fotoğraf duvar kağıdı yapıldı!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Akıllı Korumalar & Gizlilik
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
                    text = "🛡️ Akıllı Korumalar & Gizlilik",
                    color = CyberCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                SettingSwitchRow(
                    title = "Oyun Koruması (Ön Ekranda Oyun Açıkken Bölme)",
                    subtitle = "Ön ekranda oyun oynarken arka ekran uyandırılmaz ve yeniden başlatılmaz; oyununuz kesintiye uğramaz. Gelen arama ve alarm istisnadır.",
                    checked = settings.gameModeProtection,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(gameModeProtection = v) } }
                )

                SettingSwitchRow(
                    title = "Akıllı Cep Koruması",
                    subtitle = "Telefon cebe girdiğinde arka ekran anında saf AMOLED siyahına döner, dokunmatik kilitlenir ve pil harcaması durur.",
                    checked = settings.pocketModeProtection,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(pocketModeProtection = v) } }
                )

                SettingSwitchRow(
                    title = "Kulak Yakınlık Gizliliği",
                    subtitle = "Görüşme sırasında telefon kulağınıza gittiğinde arka ekran tamamen kararır.",
                    checked = settings.earProximityCallPrivacy,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(earProximityCallPrivacy = v) } }
                )

                SettingSwitchRow(
                    title = "Arayan Numara Maskeleme",
                    subtitle = "Gelen aramalarda telefon numarasını maskeleyerek etraftakilerin görmesini engeller.",
                    checked = settings.maskCallerPhoneNumber,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(maskCallerPhoneNumber = v) } }
                )

                SettingSwitchRow(
                    title = "Arka Ekrandan Cevaplayınca Otomatik Hoparlörü Aç",
                    subtitle = "Telefon masadayken arka ekrandan cevap verdiğinizde hoparlörü otomatik açar.",
                    checked = settings.autoSpeakerOnRearAnswer,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(autoSpeakerOnRearAnswer = v) } }
                )

                SettingSwitchRow(
                    title = "El Sallayarak Gelen Aramayı Sustur (Wave Gesture)",
                    subtitle = "Yakınlık sensörünün üzerinden elinizi geçirdiğinizde zil sesini susturur.",
                    checked = settings.waveGestureSilenceCall,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(waveGestureSilenceCall = v) } }
                )

                SettingSwitchRow(
                    title = "AMOLED Yanık Koruması (Pixel Shift)",
                    subtitle = "Sabit pikselleri mikroskobik düzeyde kaydırarak LTPO AMOLED panelin ömrünü korur.",
                    checked = settings.amoledBurnInProtection,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(amoledBurnInProtection = v) } }
                )
            }
        }

        // 2. Pil & LTPO Güç Optimizasyonu
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
                    text = "🔋 Pil & LTPO Güç Optimizasyonu",
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
                    listOf(15 to "15 sn", 30 to "30 sn", 60 to "60 sn", 0 to "Sürekli").forEach { (sec, lbl) ->
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
                                modifier = Modifier.padding(vertical = 7.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                SettingSwitchRow(
                    title = "LTPO 1Hz Eko Saat Modu",
                    subtitle = "Saniye yenilemesini kapatarak Xiaomi LTPO ekran panelinin 1Hz'e inmesini sağlar (%80 ekran tasarrufu).",
                    checked = settings.ltpo1HzClockMode,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(ltpo1HzClockMode = v) } }
                )

                SettingSwitchRow(
                    title = "Ön Ekran Kullanımdaysa Arka Ekranı Uyandırma",
                    subtitle = "Telefonun kilidi açıkken ve ön ekrana bakıyorken normal bildirimlerde arka ekranı boşuna açmaz.",
                    checked = settings.skipRearWakeWhenFrontActive,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(skipRearWakeWhenFrontActive = v) } }
                )

                SettingSwitchRow(
                    title = "%20 Pil Altında Otomatik Ultra Tasarruf Modu",
                    subtitle = "Şarj %20 altına düştüğünde arka ekran parlaklığını ve efektlerini kısar.",
                    checked = settings.autoEcoBelow20Percent,
                    onCheckedChange = { v -> RearStateRepository.updateSettings { it.copy(autoEcoBelow20Percent = v) } }
                )
            }
        }

        // 3. Saat & Tema Stili
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
                    text = "Saat & Kadran Stili",
                    color = AmberGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                ClockFaceStyle.entries.forEach { style ->
                    val selected = settings.clockFaceStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) LeicaRed.copy(alpha = 0.2f) else DarkBg)
                            .border(
                                width = 1.dp,
                                color = if (selected) LeicaRed else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
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

                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Text("🖼️ Galeriden Özel Fotoğraf Seç", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SetupAndSystemTab() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val shizukuReady by RearStateRepository.shizukuReady.collectAsState()
    val notifConnectedState by RearStateRepository.notificationListenerConnected.collectAsState()
    val statusLog by RearStateRepository.statusLog.collectAsState()
    val settings by RearStateRepository.settings.collectAsState()
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
    ) {
        phoneAndMicGranted = checkPhoneAndMicGranted(context)
    }

    if (showProminentDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showProminentDisclosureDialog = false },
            title = {
                Text("Bildirim Erişimi & Gizlilik", fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Text(
                    "MiRearUltra, gelen aramaları ve mesaj bildirimlerini Xiaomi 17 Pro Max arka ekranında göstermek için Bildirim Erişimi izni kullanır.\n\n" +
                    "🔒 GİZLİLİK GARANTİSİ: Uygulama İNTERNET İZNİ İSTEMEZ. Tüm verileriniz %100 yerel olarak cihaz içinde işlenir, hiçbir yere gönderilmez."
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
                    Text("Ayarları Aç", color = Color.Black, fontWeight = FontWeight.Bold)
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
        // Kontrol Merkezi Kısayolları Rehberi
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF192538)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "💡 Kontrol Merkezi Kısayollarını Ekleyin",
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ekranın üstünden Kontrol Merkezini indirin, Düzenle'ye dokunun ve şu kısayolları ekleyin:\n" +
                        "• 📲 Arka Ekrana Aktar: Açık uygulamayı tek dokunuşla arka ekrana atar.\n" +
                        "• 📸 Arka Ekran Görüntüsü: Arka ekran görüntüsünü anında Galeri'ye kaydeder.",
                    color = Color(0xFFD0D8E8),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // İzin Adımları
        PermissionStepCard(
            stepNumber = "1",
            title = "Diğer Uygulamaların Üzerinde Göster",
            description = "Xiaomi HyperOS arka ekranına doğrudan pencere yansıtmak için gereklidir.",
            isGranted = overlayGranted,
            buttonText = if (overlayGranted) "✓ İzin Açık" else "Pencere İznini Aç",
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

        PermissionStepCard(
            stepNumber = "2",
            title = "Bildirim & Gelen Çağrı Erişimi",
            description = "Gelen aramaları ve WhatsApp/SMS bildirimlerini arka ekranda göstermek için gereklidir.",
            isGranted = notifGranted,
            buttonText = if (notifGranted) "✓ İzin Açık" else "Bildirim İznini Aç",
            onClick = { showProminentDisclosureDialog = true }
        )

        PermissionStepCard(
            stepNumber = "3",
            title = "Telefon & Mikrofon İzinleri",
            description = "Arka ekrandan aramayı cevaplamak, hoparlöre almak ve sesli not kaydetmek için gereklidir.",
            isGranted = phoneAndMicGranted,
            buttonText = if (phoneAndMicGranted) "✓ İzinler Onaylandı" else "Telefon İzinlerini Onayla",
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

        PermissionStepCard(
            stepNumber = "4",
            title = "Shizuku Köprüsü (Root Gerektirmez)",
            description = "Xiaomi 17 Pro Max arka ekranını uyandırmak ve uygulamaları arka ekrana taşımak için kullanılır.",
            isGranted = shizukuReady,
            buttonText = if (shizukuReady) "✓ Shizuku Aktif" else "Shizuku İznini İste",
            onClick = { RearDisplayEngine.requestShizukuPermission(context) }
        )

        PermissionStepCard(
            stepNumber = "5",
            title = "Pil Optimizasyon Muafiyeti",
            description = "Telefon kilitliyken arka ekran servisinin kesintisiz çalışmasını sağlar.",
            isGranted = ignoringBattery,
            buttonText = if (ignoringBattery) "✓ Pil Optimize Edildi" else "Pil Ayarını Aç",
            onClick = {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            }
        )

        // Sistem Günlüğü Kartı
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
                    text = "Canlı Sistem Günlüğü",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                statusLog.take(6).forEach { logLine ->
                    Text(
                        text = "• $logLine",
                        color = Color(0xFFC2C9DB),
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
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
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isGranted) EmeraldGreen else LeicaRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber,
                            color = if (isGranted) Color.Black else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.5.sp,
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
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) Color(0xFF22352C) else LeicaRed
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = buttonText,
                    color = if (isGranted) EmeraldGreen else Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
