package com.noora.assistant.memory

import android.content.Context

class NooraMemoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("noora_memory", Context.MODE_PRIVATE)
    fun saveUserName(name: String) = prefs.edit().putString("user_name", name).apply()
    fun userName(): String? = prefs.getString("user_name", null)
    fun saveAssistantEnabled(enabled: Boolean) = prefs.edit().putBoolean("assistant_enabled", enabled).apply()
    fun assistantEnabled(): Boolean = prefs.getBoolean("assistant_enabled", true)
    fun remember(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun recentContext(): List<Pair<String, String>> = buildList {
        userName()?.let { add("system" to "The user's preferred name is $it.") }
        prefs.getString("last_user_text", null)?.let { add("user" to it) }
        prefs.getString("last_noora_reply", null)?.let { add("assistant" to it) }
    }
}
