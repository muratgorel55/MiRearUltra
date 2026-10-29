package com.mirearultra.pro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mirearultra.pro.core.RearStateRepository

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            RearStateRepository.init(context)
            if (RearStateRepository.settings.value.autoStartOnBoot) {
                RearEngineForegroundService.start(context)
                RearStateRepository.appendLog("🚀 Cihaz açılışında MiRearUltra servisi otomatik başlatıldı.")
            }
        }
    }
}
