package com.noora.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class NooraSpeechRecognizer(context: Context) {
    private val appContext = context.applicationContext
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
    private var onText: ((String) -> Unit)? = null
    private var onError: ((Int) -> Unit)? = null
    private var onState: ((String) -> Unit)? = null

    init {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty().trim()
                if (text.isNotBlank()) onText?.invoke(text)
                else onError?.invoke(SpeechRecognizer.ERROR_NO_MATCH)
            }

            override fun onError(error: Int) {
                onState?.invoke(errorLabel(error))
                onError?.invoke(error)
            }

            override fun onReadyForSpeech(params: Bundle?) { onState?.invoke("Listening…") }
            override fun onBeginningOfSpeech() { onState?.invoke("Listening…") }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { onState?.invoke("Processing voice…") }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun isOnDeviceAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)

    fun listen(
        languageTag: String = "en-US",
        onText: (String) -> Unit,
        onError: (Int) -> Unit = {},
        onState: (String) -> Unit = {}
    ) {
        this.onText = onText
        this.onError = onError
        this.onState = onState

        if (!isAvailable()) {
            onState("Speech recognition unavailable")
            onError(SpeechRecognizer.ERROR_CLIENT)
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            // Android 14+ can switch between Urdu and English during the same utterance
            // when the installed recognition service supports language switching.
            if (Build.VERSION.SDK_INT >= 34) {
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH, true)
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES,
                    arrayListOf("en-US", "ur-PK")
                )
            }
        }

        onState("Listening…")
        try {
            recognizer.startListening(intent)
        } catch (_: RuntimeException) {
            onState("Could not start microphone")
            onError(SpeechRecognizer.ERROR_CLIENT)
        }
    }

    fun stop() = recognizer.stopListening()
    fun cancel() = recognizer.cancel()
    fun destroy() = recognizer.destroy()

    private fun errorLabel(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error"
        SpeechRecognizer.ERROR_CLIENT -> "Voice input client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice network unavailable"
        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice input is busy"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Requested language unavailable"
        else -> "Voice input error"
    }
}
