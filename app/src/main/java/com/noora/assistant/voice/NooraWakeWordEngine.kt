package com.noora.assistant.voice

import android.content.Context
import java.util.Locale

/**
 * Practical wake-word foundation using the existing Android speech recognizer.
 * This is intentionally not advertised as a low-power hardware hotword engine.
 */
class NooraWakeWordEngine(context: Context) {
    private val recognizer = NooraSpeechRecognizer(context)
    private var active = false
    private var listening = false

    fun isAvailable(): Boolean = recognizer.isAvailable()

    fun start(
        onWake: (String) -> Unit,
        onState: (String) -> Unit = {},
        onError: (Int) -> Unit = {}
    ) {
        active = true
        listenForWake(onWake, onState, onError)
    }

    fun stop() {
        active = false
        listening = false
        recognizer.cancel()
    }

    private fun listenForWake(
        onWake: (String) -> Unit,
        onState: (String) -> Unit,
        onError: (Int) -> Unit
    ) {
        if (!active || listening || !recognizer.isAvailable()) return
        listening = true
        recognizer.listen(
            languageTag = "en-US",
            onText = { transcript ->
                listening = false
                val command = extractWakeCommand(transcript)
                if (command != null) onWake(command)
                if (active) listenForWake(onWake, onState, onError)
            },
            onError = { error ->
                listening = false
                onError(error)
                if (active) listenForWake(onWake, onState, onError)
            },
            onState = onState
        )
    }

    private fun extractWakeCommand(text: String): String? {
        val normalized = text.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N} ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized == "noora" || normalized == "نورا") return ""
        val wake = Regex("(?:^|\\s)(noora|نورا)(?:\\s|$)")
        val match = wake.find(normalized) ?: return null
        return normalized.removeRange(match.range).trim()
    }

    fun destroy() {
        stop()
        recognizer.destroy()
    }
}
