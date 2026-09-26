package com.noora.assistant.calls

import android.telecom.Call
import android.telecom.InCallService
import android.os.Handler
import android.os.Looper

class NooraInCallService : InCallService() {
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Call? = null

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        if (call.state == Call.STATE_RINGING) {
            pending = call
            val prefs = getSharedPreferences("noora_calls", MODE_PRIVATE)
            if (prefs.getBoolean("auto_answer", false)) {
                val delay = prefs.getInt("auto_answer_delay", 10).coerceIn(1, 60) * 1000L
                handler.postDelayed({
                    if (pending === call && call.state == Call.STATE_RINGING) {
                        try { call.answer(0) } catch (_: Exception) { }
                    }
                }, delay)
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        if (pending === call) pending = null
        super.onCallRemoved(call)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        pending = null
        super.onDestroy()
    }
}
