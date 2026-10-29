package com.mirearultra.pro.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mirearultra.pro.core.RearDisplayEngine

class RearSwitchTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "Arka Ekrana Aktar"
            subtitle = "MiRearUltra"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        RearDisplayEngine.switchForegroundTaskToRear(this)
    }
}
