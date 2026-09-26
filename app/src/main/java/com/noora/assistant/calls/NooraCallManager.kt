package com.noora.assistant.calls

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import java.util.Locale

class NooraCallManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("noora_calls", Context.MODE_PRIVATE)

    fun call(target: String): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return "Please allow NOORA to make phone calls first."
        }
        val number = resolveNumber(target) ?: normalizeNumber(target)
        if (number.isNullOrBlank()) return "I could not find that contact or phone number."
        return try {
            context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "Calling $target"
        } catch (_: Exception) {
            "I could not start that call."
        }
    }

    fun answer(): String = if (telecom().acceptRingingCallSafely()) "Answering the call" else "I could not answer the call on this phone."

    fun end(): String = if (telecom().endCallSafely()) "Ending the call" else "I could not end the call on this phone."

    fun setAutoAnswer(enabled: Boolean, delaySeconds: Int = 10) {
        prefs.edit().putBoolean("auto_answer", enabled).putInt("auto_answer_delay", delaySeconds.coerceIn(1, 60)).apply()
    }

    fun isAutoAnswerEnabled(): Boolean = prefs.getBoolean("auto_answer", false)
    fun autoAnswerDelaySeconds(): Int = prefs.getInt("auto_answer_delay", 10)

    fun requestDefaultDialerIntent(): Intent? =
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(
                TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                context.packageName
            )
        } else null

    private fun resolveNumber(target: String): String? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%${target.trim()}%")
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection, selection, args,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    private fun normalizeNumber(value: String): String? {
        val raw = value.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        return if (raw.matches(Regex("\\+?[0-9]{7,15}"))) raw else null
    }

    private fun telecom() = TelecomCompat(context)
}

private class TelecomCompat(private val context: Context) {
    private val telecom = context.getSystemService(TelecomManager::class.java)

    fun acceptRingingCallSafely(): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                telecom?.acceptRingingCall()
                true
            } else false
        } catch (_: SecurityException) { false } catch (_: Exception) { false }
    }

    fun endCallSafely(): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= 28) telecom?.endCall() ?: false else false
        } catch (_: SecurityException) { false } catch (_: Exception) { false }
    }
}
