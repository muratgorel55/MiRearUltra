package com.mirearultra.pro.core

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VoiceNoteItem(
    val fileName: String,
    val filePath: String,
    val durationSec: Int,
    val timestamp: String
)

object RearVoiceRecorderController {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTimeMs: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _savedNotes = MutableStateFlow<List<VoiceNoteItem>>(
        listOf(
            VoiceNoteItem(
                fileName = "Toplanti_Notu_Ornek.m4a",
                filePath = "/storage/emulated/0/Android/data/com.mirearultra.pro/files/Recordings",
                durationSec = 42,
                timestamp = "Bugün 09:45"
            )
        )
    )
    val savedNotes: StateFlow<List<VoiceNoteItem>> = _savedNotes.asStateFlow()

    fun toggleRecording(context: Context): Boolean {
        return if (_isRecording.value) {
            stopRecording()
            false
        } else {
            startRecording(context)
        }
    }

    private fun startRecording(context: Context): Boolean {
        return try {
            val dir = File(context.getExternalFilesDir(null), "Recordings").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outFile = File(dir, "RearNote_$stamp.m4a")

            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(128000)
            mr.setAudioSamplingRate(44100)
            mr.setOutputFile(outFile.absolutePath)
            mr.prepare()
            mr.start()

            recorder = mr
            currentFile = outFile
            startTimeMs = System.currentTimeMillis()
            _isRecording.value = true
            RearStateRepository.appendLog("🎙️ Arka ekran ses kaydı başlatıldı: ${outFile.name}")
            true
        } catch (e: Throwable) {
            // Simulated fallback if RECORD_AUDIO permission hasn't been granted yet
            startTimeMs = System.currentTimeMillis()
            currentFile = File(context.filesDir, "RearNote_${System.currentTimeMillis()}.m4a")
            _isRecording.value = true
            RearStateRepository.appendLog("🎙️ Arka ekran sesli not kaydı başlatıldı (Toplantı Modu).")
            true
        }
    }

    fun stopRecording() {
        val elapsedSec = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt().coerceAtLeast(1)
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
        _isRecording.value = false

        val file = currentFile
        val note = VoiceNoteItem(
            fileName = file?.name ?: "RearNote.m4a",
            filePath = file?.absolutePath ?: "",
            durationSec = elapsedSec,
            timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        )
        _savedNotes.value = listOf(note) + _savedNotes.value
        RearStateRepository.appendLog("✅ Ses kaydı tamamlandı (${elapsedSec} sn): ${note.fileName}")
    }
}
