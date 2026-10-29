package com.mirearultra.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mirearultra.pro.core.RearDisplayEngine
import com.mirearultra.pro.service.RearEngineForegroundService
import com.mirearultra.pro.ui.theme.MiRearUltraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RearDisplayEngine.init(this)
        RearEngineForegroundService.start(this)
        requestRuntimePermissionsIfNeeded()
        // Uygulama açılır açılmaz Shizuku izni gerekiyorsa iste; hazırsa servis arka ekranı otomatik devralır.
        if (RearDisplayEngine.readShizukuState(this) == com.mirearultra.pro.core.ShizukuConnectionState.NOT_ALLOWED) {
            RearDisplayEngine.requestShizukuPermission(this)
        }

        enableEdgeToEdge()
        setContent {
            MiRearUltraTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // İzinler verildiyse servis adım sensörünü yeniden bağlasın
        RearEngineForegroundService.start(this)
    }

    /** Adım verisi (ACTIVITY_RECOGNITION), arayan/mesaj ismi eşleme (READ_CONTACTS) ve bildirimler için izinleri ister. */
    private fun requestRuntimePermissionsIfNeeded() {
        val needed = buildList {
            add(android.Manifest.permission.ACTIVITY_RECOGNITION)
            add(android.Manifest.permission.READ_CONTACTS)
            add(android.Manifest.permission.READ_CALL_LOG)
            add(android.Manifest.permission.READ_PHONE_STATE)
            if (android.os.Build.VERSION.SDK_INT >= 33) add(android.Manifest.permission.POST_NOTIFICATIONS)
        }.filter {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }
}
