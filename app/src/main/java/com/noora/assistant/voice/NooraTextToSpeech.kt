package com.noora.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class NooraTextToSpeech(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var onStatus: ((String) -> Unit)? = null
    private var onCompleted: (() -> Unit)? = null

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.setSpeechRate(0.95f)
            onStatus?.invoke("Voice ready")
        } else {
            onStatus?.invoke("Voice engine unavailable")
        }
    }

    fun setStatusListener(listener: (String) -> Unit) {
        onStatus = listener
        if (ready) listener("Voice ready")
    }

    fun isReady(): Boolean = ready

    fun setCompletionListener(listener: () -> Unit) {
        onCompleted = listener
    }

    fun speak(text: String, language: String = "en") {
        if (!ready || text.isBlank()) return

        val requested = if (language.lowercase().startsWith("ur")) {
            Locale("ur", "PK")
        } else {
            Locale.US
        }

        val support = tts.isLanguageAvailable(requested)
        val locale = if (support >= TextToSpeech.LANG_AVAILABLE) {
            requested
        } else {
            Locale.US
        }

        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.US)
            onStatus?.invoke("Requested voice language unavailable; using English voice")
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "NOORA_RESPONSE") onCompleted?.invoke()
                }
                override fun onError(utteranceId: String?) {
                    if (utteranceId == "NOORA_RESPONSE") onCompleted?.invoke()
                }
            })
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "NOORA_RESPONSE")
    }

    fun stop() = tts.stop()
    fun shutdown() = tts.shutdown()
}
