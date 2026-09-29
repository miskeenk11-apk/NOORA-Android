package com.noora.assistant.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.Locale

/**
 * Practical wake-word foundation using the existing Android speech recognizer.
 * This is intentionally not advertised as a low-power hardware hotword engine.
 */
class NooraWakeWordEngine(context: Context) {

    private val recognizer = NooraSpeechRecognizer(context)
    private val handler = Handler(Looper.getMainLooper())

    private var active = false
    private var listening = false
    private var restartPending = false
    private var destroyed = false

    fun isAvailable(): Boolean =
        !destroyed && recognizer.isAvailable()

    fun start(
        onWake: (String) -> Unit,
        onState: (String) -> Unit = {},
        onError: (Int) -> Unit = {}
    ) {
        if (destroyed) return

        active = true
        restartPending = false
        handler.removeCallbacksAndMessages(null)

        listenForWake(onWake, onState, onError)
    }

    fun stop() {
        active = false
        listening = false
        restartPending = false

        handler.removeCallbacksAndMessages(null)
        recognizer.cancel()
    }

    private fun listenForWake(
        onWake: (String) -> Unit,
        onState: (String) -> Unit,
        onError: (Int) -> Unit
    ) {
        if (
            destroyed ||
            !active ||
            listening ||
            restartPending ||
            !recognizer.isAvailable()
        ) {
            return
        }

        listening = true

        recognizer.listen(
            languageTag = "en-US",

            onText = { transcript ->
                listening = false

                val command = extractWakeCommand(transcript)

                if (command != null) {
                    onWake(command)
                } else {
                    scheduleWakeRestart(
                        onWake,
                        onState,
                        onError,
                        700L
                    )
                }
            },

            onError = { error ->
                listening = false

                /*
                 * Android may briefly report the recognizer as busy
                 * while the previous microphone session is closing.
                 * Do not immediately start another session.
                 */
                if (error != android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    onError(error)
                }

                if (active) {
                    scheduleWakeRestart(
                        onWake,
                        onState,
                        onError,
                        1500L
                    )
                }
            },

            onState = onState
        )
    }

    private fun scheduleWakeRestart(
        onWake: (String) -> Unit,
        onState: (String) -> Unit,
        onError: (Int) -> Unit,
        delayMs: Long
    ) {
        if (
            destroyed ||
            !active ||
            restartPending
        ) {
            return
        }

        restartPending = true

        handler.postDelayed({
            restartPending = false

            if (active && !listening && !destroyed) {
                listenForWake(
                    onWake,
                    onState,
                    onError
                )
            }
        }, delayMs)
    }

    private fun extractWakeCommand(text: String): String? {
        val normalized = text.lowercase(Locale.ROOT)
            .replace(Regex("""[^\p{L}\p{N} ]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (
            normalized == "noora" ||
            normalized == "nora" ||
            normalized == "noorah" ||
            normalized == "noor" ||
            normalized == "نورا" ||
            normalized == "نور" ||
            normalized == "hello" ||
            normalized == "hello noora" ||
            normalized == "hello nora" ||
            normalized == "hey noora" ||
            normalized == "hey nora"
        ) {
            return ""
        }

        val wake = Regex("""(?:^|\s)(noora|nora|noorah|noor|نورا|نور)(?:\s|$)""")
        val match = wake.find(normalized) ?: return null

        return normalized.removeRange(match.range).trim()
    }

    fun destroy() {
        if (destroyed) return

        destroyed = true
        active = false
        listening = false
        restartPending = false

        handler.removeCallbacksAndMessages(null)
        recognizer.destroy()
    }
}
