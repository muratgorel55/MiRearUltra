package com.mirearultra.pro.core

import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle

object NotificationReplyController {

    /**
     * Dispatches a direct reply to any messaging notification (WhatsApp, Telegram, Instagram, SMS, etc.)
     * using its captured RemoteInput and PendingIntent, without needing to flip or unlock the front screen.
     */
    fun sendDirectReply(
        context: Context,
        item: RearNotificationItem,
        replyText: String
    ): Boolean {
        if (replyText.isBlank()) return false

        val pendingIntent = item.replyIntent
        val remoteInput = item.remoteInput

        if (pendingIntent != null && remoteInput != null) {
            return try {
                val localIntent = Intent()
                val bundle = Bundle().apply {
                    putCharSequence(remoteInput.resultKey, replyText)
                }
                RemoteInput.addResultsToIntent(arrayOf(remoteInput), localIntent, bundle)
                pendingIntent.send(context, 0, localIntent)
                RearStateRepository.markNotificationReplied(item.id, replyText)
                true
            } catch (e: Throwable) {
                RearStateRepository.appendLog("⚠️ Bildirim yanıtı gönderilirken hata: ${e.message}")
                false
            }
        } else {
            // Demo or notification without RemoteInput: mark replied in UI
            RearStateRepository.markNotificationReplied(item.id, replyText)
            return true
        }
    }
}
