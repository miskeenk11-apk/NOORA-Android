package com.noora.assistant.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.openwakeword.OpenWakeWord

/**
 * Dedicated on-device wake-word engine.
 *
 * The assistant name remains NOORA.
 * The activation phrase is the bundled "Hey Jarvis" model.
 */
class NooraWakeWordEngine(context: Context) {

    private val appContext = context.applicationContext
    private var detector: OpenWakeWord? = null
    private var active = false
    private var destroyed = false

    init {
        detector = try {
            OpenWakeWord.Builder(appContext)
                .setModel(OpenWakeWord.BuiltInModel.HEY_JARVIS)
                .setThreshold(0.5f)
                .setDebounceMs(2000L)
                .build()
        } catch (_: Exception) {
            null
        }
    }

    fun isAvailable(): Boolean =
        !destroyed &&
            detector != null &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

    fun start(
        onWake: (String) -> Unit,
        onState: (String) -> Unit = {},
        onError: (Int) -> Unit = {}
    ) {
        if (destroyed || active) return

        if (!isAvailable()) {
            onState("Microphone permission required")
            return
        }

        val currentDetector = detector ?: run {
            onState("Wake word engine unavailable")
            return
        }

        active = true
        onState("Say: Hey JARVIS")

        try {
            currentDetector.start {
                if (!active || destroyed) return@start

                active = false
                onState("Wake word detected")
                onWake("")
            }
        } catch (_: Exception) {
            active = false
            onState("Wake word could not start")
        }
    }

    fun stop() {
        active = false

        if (destroyed) return

        try {
            detector?.stop()
        } catch (_: Exception) {
        }
    }

    fun destroy() {
        if (destroyed) return

        destroyed = true
        active = false

        try {
            detector?.stop()
        } catch (_: Exception) {
        }

        try {
            detector?.release()
        } catch (_: Exception) {
        }

        detector = null
    }
}
