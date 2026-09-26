package com.noora.assistant.ai

import com.noora.assistant.core.NooraConnectivity
import com.noora.assistant.memory.NooraMemoryStore
import com.noora.assistant.phone.NooraPhoneControl
import com.noora.assistant.voice.NooraTextToSpeech
import java.util.concurrent.Executors

class NooraConversationCoordinator(
    private val connectivity: NooraConnectivity,
    private val memory: NooraMemoryStore,
    private val speech: NooraTextToSpeech,
    private val gateway: GatewayAiClient,
    private val phoneControl: NooraPhoneControl? = null,
    private val onStatus: (String) -> Unit = {}
) {
    private val executor = Executors.newSingleThreadExecutor()

    fun handleTranscript(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        memory.remember("last_user_text", clean)
        phoneControl?.execute(clean)?.let { reply ->
            speakAndRemember(reply, clean)
            return
        }
        if (!connectivity.isOnline()) {
            speakAndRemember("میں اس وقت offline ہوں، لیکن میں آپ کی supported local commands میں مدد کر سکتی ہوں۔", clean)
            return
        }
        onStatus("Thinking online…")
        executor.execute {
            val context = memory.recentContext()
            val result = gateway.chat(context + listOf("user" to clean))
            result.fold(
                onSuccess = { reply -> speakAndRemember(reply, clean) },
                onFailure = { error ->
                    onStatus("Gateway unavailable — offline fallback")
                    speakAndRemember("معذرت، ابھی online AI سے رابطہ نہیں ہو سکا۔ میں offline mode میں ہوں۔", clean)
                }
            )
        }
    }

    fun shutdown() = executor.shutdownNow()

    private fun speakAndRemember(reply: String, userText: String) {
        memory.remember("last_noora_reply", reply)
        onStatus("Speaking")
        speech.speak(reply, if (containsUrdu(reply) || containsUrdu(userText)) "ur" else "en")
    }

    private fun containsUrdu(value: String): Boolean = value.any { it.code in 0x0600..0x06FF }
}
