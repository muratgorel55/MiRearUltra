package com.mirearultra.pro.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.mirearultra.pro.core.RearDisplayEngine
import com.mirearultra.pro.core.RearStateRepository

class RearSwitchTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val isProjecting = RearDisplayEngine.isProjectionActive
        qsTile?.apply {
            state = if (isProjecting) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Arka Ekrana Aktar"
            subtitle = if (isProjecting) "Aktif" else "Kapalı"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        unlockAndRun {
            if (RearDisplayEngine.isProjectionActive) {
                RearDisplayEngine.stopProjection(this)
                Toast.makeText(this, "⏹️ Yansıtma durduruldu, saate dönüldü.", Toast.LENGTH_SHORT).show()
            } else {
                RearDisplayEngine.switchForegroundTaskToRear(this)
                Toast.makeText(this, "📲 Uygulama arka ekrana aktarılıyor...", Toast.LENGTH_SHORT).show()
            }
            updateTileState()
        }
    }
}
