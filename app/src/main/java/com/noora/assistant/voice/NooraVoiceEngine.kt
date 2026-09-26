package com.noora.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class NooraVoiceEngine(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context, this)
    private var ready = false
    override fun onInit(status: Int) { ready = status == TextToSpeech.SUCCESS }
    fun speak(text: String, language: Locale = Locale.US) {
        if (!ready) return
        tts.language = language
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "NOORA_RESPONSE")
    }
    fun stop() = tts.stop()
    fun release() = tts.shutdown()
}
