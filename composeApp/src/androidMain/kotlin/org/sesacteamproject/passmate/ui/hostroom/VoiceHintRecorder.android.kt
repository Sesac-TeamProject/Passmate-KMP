package org.sesacteamproject.passmate.ui.hostroom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.time.TimeSource

// MediaRecorder(m4a/AAC) 녹음 — 권한 없으면 start()가 요청만 트리거하고 false 반환(허용 후 다시 누름)
private class AndroidVoiceHintRecorder(
    private val context: Context,
    private val requestPermission: () -> Unit
) : VoiceHintRecorderController {

    private var recorder: MediaRecorder? = null

    private var outputFile: File? = null

    private var startMark: TimeSource.Monotonic.ValueTimeMark? = null

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun releaseRecorder() {
        try {
            recorder?.release()
        } catch (e: Exception) {
            // 이미 해제된 레코더 정리 실패는 무시
        }
        recorder = null
        outputFile?.delete()
        outputFile = null
        startMark = null
    }

    override fun start(): Boolean {
        if (!hasPermission()) {
            requestPermission()
            return false
        }
        if (recorder != null) {
            return false
        }
        val file = File.createTempFile("ptt_hint_", ".m4a", context.cacheDir)

        return try {
            @Suppress("DEPRECATION")
            val mediaRecorder = MediaRecorder()

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioEncodingBitRate(ENCODING_BIT_RATE)
            mediaRecorder.setAudioSamplingRate(SAMPLING_RATE)
            mediaRecorder.setOutputFile(file.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            recorder = mediaRecorder
            outputFile = file
            startMark = TimeSource.Monotonic.markNow()
            true
        } catch (e: Exception) {
            file.delete()
            releaseRecorder()
            false
        }
    }

    override fun stop(): RecordedVoiceHint? {
        val mediaRecorder = recorder
        val file = outputFile
        val durationMs = startMark?.elapsedNow()?.inWholeMilliseconds ?: 0L

        if (mediaRecorder == null || file == null) {
            return null
        }
        return try {
            mediaRecorder.stop()
            // 너무 짧은 클립은 무효(비어 있거나 stop 실패 소지) — 화면이 "너무 짧아요" 안내
            if (durationMs < MIN_DURATION_MS || !file.exists()) {
                null
            } else {
                RecordedVoiceHint(
                    audioBytes = file.readBytes(),
                    mimeType = "audio/mp4",
                    fileName = file.name,
                    durationMs = durationMs
                )
            }
        } catch (e: Exception) {
            null
        } finally {
            releaseRecorder()
        }
    }

    override fun cancel() {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            // 시작 직후 취소는 stop 실패 가능 — 정리만 한다
        }
        releaseRecorder()
    }

    companion object {
        private const val ENCODING_BIT_RATE = 64_000
        private const val SAMPLING_RATE = 44_100
        private const val MIN_DURATION_MS = 500L
    }
}

@Composable
actual fun rememberVoiceHintRecorder(): VoiceHintRecorderController? {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    return remember(context) {
        AndroidVoiceHintRecorder(
            context = context.applicationContext,
            requestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        )
    }
}
