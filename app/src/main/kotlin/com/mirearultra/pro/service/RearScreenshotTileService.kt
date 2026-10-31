package com.mirearultra.pro.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.mirearultra.pro.core.RearDisplayEngine

class RearScreenshotTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "Arka Ekran Görüntüsü"
            subtitle = "MiRearUltra"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        unlockAndRun {
            RearDisplayEngine.takeRearScreenshot(this) { success, _ ->
                qsTile?.apply {
                    subtitle = if (success) "Kaydedildi" else "Hata"
                    updateTile()
                }
                Toast.makeText(
                    this,
                    if (success) "📸 Arka ekran görüntüsü Galeri'ye kaydedildi!" else "❌ Ekran görüntüsü alınamadı (Shizuku gerekli).",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
