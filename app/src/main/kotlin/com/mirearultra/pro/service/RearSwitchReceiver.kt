package com.mirearultra.pro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mirearultra.pro.core.RearDisplayEngine

/**
 * Bildirim çubuğundan veya kısayollardan tek dokunuşla ön ekrandaki
 * aktif uygulamayı anında arka ekrana taşır.
 */
class RearSwitchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            RearDisplayEngine.switchForegroundTaskToRear(it)
        }
    }
}
