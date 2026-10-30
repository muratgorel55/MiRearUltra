package com.mirearultra.pro.core

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.speech.tts.TextToSpeech
import android.telecom.TelecomManager
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

object CallActionController {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var ttsEngine: TextToSpeech? = null

    fun answerCall(context: Context, enableSpeakerphone: Boolean) {
        val current = RearStateRepository.currentCall.value
        var handled = false

        // 1. PendingIntent üzerinden cevaplama (HyperOS InCallUI, WhatsApp & Google Dialer)
        current?.answerIntent?.let { pendingIntent ->
            runCatching {
                pendingIntent.send(context, 0, null)
                handled = true
            }.onFailure {
                runCatching {
                    pendingIntent.send()
                    handled = true
                }
            }
        }

        // 2. TelecomManager üzerinden cevaplama (GSM)
        if (!handled && ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
            runCatching {
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                @Suppress("DEPRECATION")
                tm.acceptRingingCall()
                handled = true
            }
        }

        // 3. Shizuku ve Tuş Emülasyonu (GSM aramasını terminalden açma)
        if (!handled && RearDisplayEngine.checkShizukuStatus()) {
            scope.launch(Dispatchers.IO) {
                RearDisplayEngine.executeShizukuShell("cmd telecom accept-ringing-call || input keyevent 5 || input keyevent 79")
            }
            handled = true
        }

        // 4. Doğrudan AudioManager üzerinden kulaklık/arama tuşu tetikleme yedeği
        if (!handled) {
            runCatching {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK)
                val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK)
                audioManager?.dispatchMediaKeyEvent(downEvent)
                audioManager?.dispatchMediaKeyEvent(upEvent)
            }
        }

        // Kullanıcı isteği: Arama arka ekrandan açıldığı AN arama ekranı hemen kapanır ve saate döner!
        RearStateRepository.onCallEnded()
        RearStateRepository.setMode(RearScreenMode.HOME_LAUNCHER)

        if (enableSpeakerphone) {
            scope.launch {
                delay(400)
                setSpeakerphone(context, true)
            }
        }
        RearStateRepository.appendLog(
            if (enableSpeakerphone) "📞 Arama arka ekrandan cevaplandı (Hoparlör Açıldı ve Arka Ekran Saate Döndü)."
            else "📞 Arama arka ekrandan cevaplandı (Arka Ekran Saate Döndü)."
        )
    }

    /**
     * Initiates an outgoing call directly from the Xiaomi 17 / 18 Pro Max rear screen (Speed Dial / Dialpad)
     * and automatically switches audio to Speakerphone so the user can talk hands-free.
     */
    fun placeCallFromRear(context: Context, nameLabel: String, phoneNumber: String) {
        if (phoneNumber.isBlank()) return
        val cleanUri = Uri.parse("tel:${Uri.encode(phoneNumber)}")
        val canDirectCall = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

        // 0. Arka ekranda dış arama animasyonunu ve ekranını anında başlat
        RearStateRepository.onOutgoingCall(name = nameLabel, phone = phoneNumber)
        scope.launch {
            delay(25_000L)
            if (RearStateRepository.currentCall.value?.status == CallStatus.OUTGOING) {
                RearStateRepository.onCallEnded()
            }
        }

        // 1. Doğrudan veya Shizuku üzerinden aramayı başlat
        if (RearDisplayEngine.checkShizukuStatus()) {
            scope.launch(Dispatchers.IO) {
                RearDisplayEngine.executeShizukuShell("am start -a android.intent.action.CALL -d tel:${Uri.encode(phoneNumber)}")
            }
        } else {
            val intent = Intent(if (canDirectCall) Intent.ACTION_CALL else Intent.ACTION_DIAL, cleanUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching {
                context.startActivity(intent)
            }
        }

        // 2. Hoparlörü aç (eller serbest görüşme için)
        if (RearStateRepository.settings.value.autoSpeakerOnRearAnswer) {
            scope.launch {
                delay(800)
                setSpeakerphone(context, true)
            }
        }
        // NOT: onIncomingCall ÇAĞRILMAZ! Sahte arama overlay'i başlatıp arka ekranı dondurmasını engeller.
        RearStateRepository.appendLog("☎️ Hızlı arama başlatıldı: $nameLabel ($phoneNumber)")
    }

    fun rejectOrEndCall(context: Context) {
        val current = RearStateRepository.currentCall.value
        var handled = false

        current?.declineIntent?.let { pendingIntent ->
            runCatching {
                pendingIntent.send(context, 0, null)
                handled = true
            }.onFailure {
                runCatching {
                    pendingIntent.send()
                    handled = true
                }
            }
        }

        if (!handled && ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
            runCatching {
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    @Suppress("DEPRECATION")
                    tm.endCall()
                    handled = true
                }
            }
        }

        if (!handled && RearDisplayEngine.checkShizukuStatus()) {
            scope.launch(Dispatchers.IO) {
                RearDisplayEngine.executeShizukuShell("cmd telecom end-call || input keyevent 6")
            }
        }

        restoreAudioAfterCall(context)
        RearStateRepository.onCallEnded()
    }

    fun silenceRinger(context: Context) {
        runCatching {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            tm.silenceRinger()
        }
        // STREAM_RING'i kalıcı ADJUST_MUTE yapmıyoruz, çünkü telefon sessize düşüp öyle kalıyordu!
        RearStateRepository.appendLog("🔕 Gelen arama zil sesi susturuldu.")
    }

    fun toggleSpeakerphone(context: Context) {
        val current = RearStateRepository.currentCall.value ?: return
        val target = !current.isSpeakerOn
        setSpeakerphone(context, target)
        RearStateRepository.updateCallState { it.copy(isSpeakerOn = target) }
    }

    fun toggleMute(context: Context) {
        val current = RearStateRepository.currentCall.value ?: return
        val target = !current.isMuted
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.isMicrophoneMute = target
        }
        RearStateRepository.updateCallState { it.copy(isMuted = target) }
    }

    /**
     * Google Play Policy Compliant Quick SMS Reply:
     * Uses standard ACTION_SENDTO intent (does not require restricted SEND_SMS manifest permission).
     */
    fun rejectWithQuickSms(context: Context, smsText: String) {
        val current = RearStateRepository.currentCall.value
        val number = current?.phoneNumber.orEmpty()
        rejectOrEndCall(context)
        if (number.isNotBlank()) {
            runCatching {
                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")).apply {
                    putExtra("sms_body", smsText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(smsIntent)
            }
        }
        RearStateRepository.appendLog("✉️ Hızlı mesaj yanıtı hazırlandı ($number): \"$smsText\"")
    }

    /**
     * Reads any incoming notification aloud in Turkish via Android TextToSpeech (TTS Spiker).
     */
    fun speakNotificationAloud(context: Context, item: RearNotificationItem) {
        val speechText = "${item.appName} uygulamasından ${item.sender} yazdı: ${item.message}"
        if (ttsEngine == null) {
            ttsEngine = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsEngine?.language = Locale("tr", "TR")
                    ttsEngine?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "rear_tts_${item.id}")
                }
            }
        } else {
            ttsEngine?.language = Locale("tr", "TR")
            ttsEngine?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "rear_tts_${item.id}")
        }
        RearStateRepository.appendLog("🔊 Mesaj sesli okunuyor (TTS): ${item.sender}")
    }

    private fun setSpeakerphone(context: Context, enabled: Boolean) {
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (enabled) {
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val speakerDevice = am.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speakerDevice != null) {
                        am.setCommunicationDevice(speakerDevice)
                    }
                }
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = true
            } else {
                restoreAudioAfterCall(context)
            }
        }
    }

    /** Görüşme bittiğinde ses ayarlarını normale döndürür; telefonun zil sesi bozuk kalmasın. */
    fun restoreAudioAfterCall(context: Context) {
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.clearCommunicationDevice()
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn = false
            am.isMicrophoneMute = false
            am.mode = AudioManager.MODE_NORMAL
        }
    }

    /** Eski sürümlerin bıraktığı kalıcı sessiz/ses modu bozukluğunu düzeltir. */
    fun repairRingerState(context: Context) {
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (am.mode == AudioManager.MODE_IN_COMMUNICATION && RearStateRepository.currentCall.value == null) {
                am.mode = AudioManager.MODE_NORMAL
            }
            if (am.ringerMode == AudioManager.RINGER_MODE_NORMAL && am.isStreamMute(AudioManager.STREAM_RING)) {
                am.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
            }
        }
    }
}
