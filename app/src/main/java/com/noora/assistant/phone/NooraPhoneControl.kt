package com.noora.assistant.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.Locale

class NooraPhoneControl(private val context: Context) {
    fun execute(command: String): String? {
        val c = command.trim().lowercase(Locale.ROOT)
        val whatsapp = parseWhatsAppMessage(command)
        if (whatsapp != null) return sendWhatsAppMessage(whatsapp.first, whatsapp.second)

        return when {
            c.contains("open settings") || c.contains("settings kholo") || c.contains("سیٹنگ") -> launch(Intent(Settings.ACTION_SETTINGS), "Settings opened")
            c.contains("open camera") || c.contains("camera kholo") || c.contains("کیمرہ") -> launch(Intent("android.media.action.IMAGE_CAPTURE"), "Camera opened")
            c.contains("open browser") || c.contains("browser kholo") || c.contains("chrome kholo") -> launch(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("https://www.google.com") }, "Browser opened")
            c.contains("open whatsapp") || c.contains("whatsapp kholo") || c.contains("واٹس ایپ") -> launchPackage("com.whatsapp", "WhatsApp opened")
            c.contains("open youtube") || c.contains("youtube kholo") -> launchPackage("com.google.android.youtube", "YouTube opened")
            c.contains("open phone") || c.contains("dialer kholo") || c.contains("phone kholo") -> launch(Intent(Intent.ACTION_DIAL), "Phone opened")
            c.contains("open messages") || c.contains("messages kholo") || c.contains("sms kholo") -> launch(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING), "Messages opened")
            c.contains("volume up") || c.contains("volume barhao") || c.contains("آواز تیز") -> volume(AudioManager.ADJUST_RAISE, "Volume increased")
            c.contains("volume down") || c.contains("volume kam") || c.contains("آواز کم") -> volume(AudioManager.ADJUST_LOWER, "Volume decreased")
            c == "mute" || c.contains("mute phone") || c.contains("phone mute") -> volume(AudioManager.ADJUST_MUTE, "Phone muted")
            c.contains("ringer loud") || c.contains("ring loud") -> volume(AudioManager.ADJUST_UNMUTE, "Phone unmuted")
            c.contains("open wifi") || c.contains("wifi settings") || c.contains("wifi kholo") -> launch(Intent(Settings.ACTION_WIFI_SETTINGS), "Wi-Fi settings opened")
            c.contains("open bluetooth") || c.contains("bluetooth settings") || c.contains("bluetooth kholo") -> launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS), "Bluetooth settings opened")
            c == "back" || c.contains("go back") || c.contains("wapas") -> accessibility("back", "Going back")
            c == "home" || c.contains("go home") || c.contains("home screen") -> accessibility("home", "Home screen opened")
            c.contains("recent apps") || c.contains("recent applications") -> accessibility("recent", "Recent apps opened")
            else -> null
        }
    }

    private fun parseWhatsAppMessage(command: String): Pair<String, String>? {
        val raw = command.trim()
        val normalized = raw.lowercase(Locale.ROOT)
        val hasWhatsApp = normalized.contains("whatsapp") || normalized.contains("واٹس ایپ")
        val sendWords = "(?:send|send karo|bhejo|bhej do|bhej dena|message karo|msg karo|بھیجو|بھیج دو|پیغام بھیجو)"

        val patterns = listOf(
            Regex("(?:whatsapp|واٹس ایپ)(?:\\s+(?:open|kholo|کھولو))?(?:\\s+par)?(?:\\s+aur)?\\s+(.+?)\\s+(?:ko|to)\\s+(.+?)\\s+$sendWords$", RegexOption.IGNORE_CASE),
            Regex("(?:whatsapp|واٹس ایپ).*?(?:send|message|msg)\\s+(.+?)\\s+(?:to|ko)\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("(.+?)\\s+ko\\s+(.+?)\\s+$sendWords$", RegexOption.IGNORE_CASE)
        )

        if (!hasWhatsApp && !normalized.contains("bhejo") && !normalized.contains("send")) return null
        for ((index, pattern) in patterns.withIndex()) {
            val m = pattern.find(raw) ?: continue
            val name: String
            val message: String
            if (index == 1) {
                message = m.groupValues[1].trim()
                name = m.groupValues[2].trim()
            } else {
                name = m.groupValues[1].trim()
                message = m.groupValues[2].trim()
            }
            if (name.isNotBlank() && message.isNotBlank()) return name to message
        }
        return null
    }

    private fun sendWhatsAppMessage(contactName: String, message: String): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return "Please allow NOORA to read contacts first."
        }
        val number = resolveContactNumber(contactName) ?: return "I could not find $contactName in your contacts."
        return try {
            val service = NooraAccessibilityService.instance
                ?: return "Please enable NOORA Phone Control in Android Accessibility settings first."
            val cleanNumber = normalizeWhatsAppNumber(number)
            if (cleanNumber.isBlank()) return "I found $contactName, but the phone number is not valid for WhatsApp."
            val uri = Uri.parse("whatsapp://send?phone=${Uri.encode(cleanNumber)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            service.queueWhatsAppMessage(message)
            "Opening WhatsApp chat for $contactName and sending your message."
        } catch (_: Exception) {
            "I could not open WhatsApp for $contactName."
        }
    }

    private fun resolveContactNumber(name: String): String? {
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val exactSelection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ?"
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection, exactSelection, arrayOf(name.trim()), null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }

        val partialSelection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection, partialSelection, arrayOf("%${name.trim()}%"),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    private fun normalizeWhatsAppNumber(value: String): String {
        val raw = value.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        val digits = raw.removePrefix("+")
        return when {
            digits.startsWith("00") -> digits.removePrefix("00")
            digits.startsWith("92") -> digits
            digits.startsWith("0") && digits.length in 10..12 -> "92" + digits.drop(1)
            digits.all { it.isDigit() } -> digits
            else -> ""
        }
    }

    private fun accessibility(action: String, reply: String): String {
        val service = NooraAccessibilityService.instance
        return if (service?.performNooraAction(action) == true) reply
        else "Please enable NOORA Phone Control in Android Accessibility settings first."
    }

    private fun volume(direction: Int, reply: String): String {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.adjustVolume(direction, AudioManager.FLAG_SHOW_UI)
        return reply
    }

    private fun launch(intent: Intent, reply: String): String {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            reply
        } catch (_: Exception) {
            Toast.makeText(context, "NOORA could not open this action", Toast.LENGTH_SHORT).show()
            "I could not open that on this phone."
        }
    }

    private fun launchPackage(packageName: String, reply: String): String {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return "That app is not installed on this phone."
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            reply
        } catch (_: Exception) {
            "I could not open that app."
        }
    }
}
