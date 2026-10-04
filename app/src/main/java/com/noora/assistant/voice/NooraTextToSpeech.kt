package com.noora.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class NooraTextToSpeech(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready=false
    private var status:((String)->Unit)?=null
    private var completed:(()->Unit)?=null
    private var speaking:((Boolean)->Unit)?=null

    override fun onInit(result:Int) {
        ready=result==TextToSpeech.SUCCESS
        if(ready){ tts.setSpeechRate(.95f); status?.invoke("Voice ready") }
        else status?.invoke("Voice engine unavailable")
    }
    fun setStatusListener(v:(String)->Unit){ status=v; if(ready)v("Voice ready") }
    fun setCompletionListener(v:()->Unit){ completed=v }
    fun setSpeakingListener(v:(Boolean)->Unit){ speaking=v }
    fun isReady()=ready

    fun speak(text:String, language:String="en") {
        if(!ready || text.isBlank()) return
        val requested=if(language.lowercase().startsWith("ur")) Locale("ur","PK") else Locale.US
        val locale=if(tts.isLanguageAvailable(requested)>=TextToSpeech.LANG_AVAILABLE) requested else Locale.US
        tts.setLanguage(locale)
        tts.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
            override fun onStart(id:String?){ if(id=="NOORA_RESPONSE") speaking?.invoke(true) }
            override fun onDone(id:String?){ if(id=="NOORA_RESPONSE"){speaking?.invoke(false);completed?.invoke()} }
            override fun onError(id:String?, code:Int){ if(id=="NOORA_RESPONSE"){speaking?.invoke(false);completed?.invoke()} }
        })
        tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"NOORA_RESPONSE")
    }
    fun stop(){tts.stop();speaking?.invoke(false)}
    fun shutdown(){tts.stop();tts.shutdown()}
}
