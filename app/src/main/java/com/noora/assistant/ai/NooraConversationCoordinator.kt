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
    private val executor=Executors.newSingleThreadExecutor()

    fun handleTranscript(text:String) {
        val clean=text.trim()
        if(clean.isEmpty()) return
        memory.remember("last_user_text",clean)
        phoneControl?.execute(clean)?.let{reply->speakAndRemember(reply,clean);return}
        if(!connectivity.isOnline()){ speakAndRemember("میں اس وقت offline ہوں۔ Internet available ہونے پر میں online AI سے جواب دوں گی۔",clean); return }
        if(!gateway.isConfigured()){ speakAndRemember("NOORA AI gateway ابھی configure نہیں کیا گیا۔ Settings میں gateway address set کریں۔",clean); return }
        onStatus("Thinking")
        executor.execute{
            val result=gateway.chat(memory.recentContext()+listOf("user" to clean))
            result.fold(
                onSuccess={reply->speakAndRemember(reply,clean)},
                onFailure={speakAndRemember("معذرت، ابھی NOORA AI سے رابطہ نہیں ہو سکا۔",clean)}
            )
        }
    }

    fun shutdown()=executor.shutdownNow()

    private fun speakAndRemember(reply:String,userText:String){
        memory.remember("last_noora_reply",reply)
        onStatus("Speaking")
        speech.speak(reply,if(containsUrdu(reply)||containsUrdu(userText))"ur" else "en")
    }
    private fun containsUrdu(value:String)=value.any{it.code in 0x0600..0x06FF}
}
