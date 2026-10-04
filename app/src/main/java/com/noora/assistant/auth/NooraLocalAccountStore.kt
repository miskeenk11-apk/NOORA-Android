package com.noora.assistant.auth

import android.content.Context
import java.security.MessageDigest

class NooraLocalAccountStore(context: Context) {
    private val prefs = context.getSharedPreferences("noora_account", Context.MODE_PRIVATE)

    fun signUp(email: String, password: String): Boolean {
        if (!email.contains("@") || password.length < 6) return false
        prefs.edit()
            .putString("email", email.trim().lowercase())
            .putString("password_hash", hash(password))
            .putBoolean("verified", false)
            .putBoolean("first_greeting_done", false)
            .apply()
        return true
    }

    fun login(email: String, password: String): Boolean =
        email.trim().lowercase() == prefs.getString("email", null) &&
            hash(password) == prefs.getString("password_hash", null)

    fun verifyDevice() { prefs.edit().putBoolean("verified", true).apply() }
    fun isVerified(): Boolean = prefs.getBoolean("verified", false)
    fun hasAccount(): Boolean = prefs.getString("email", null) != null
    fun firstGreetingDone(): Boolean = prefs.getBoolean("first_greeting_done", false)
    fun markFirstGreetingDone() { prefs.edit().putBoolean("first_greeting_done", true).apply() }
    fun logout() { prefs.edit().putBoolean("verified", false).apply() }

    private fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
