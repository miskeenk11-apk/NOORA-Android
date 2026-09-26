package com.noora.assistant.phone

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.widget.Toast
import java.util.Locale

class NooraPhoneControl(private val context: Context) {
    fun execute(command: String): String? {
        val c = command.trim().lowercase(Locale.ROOT)
        return when {
            c.contains("open settings") || c.contains("settings kholo") || c.contains("سیٹنگ") -> launch(Intent(Settings.ACTION_SETTINGS), "Settings opened")
            c.contains("open camera") || c.contains("camera kholo") || c.contains("کیمرہ") -> launch(Intent("android.media.action.IMAGE_CAPTURE"), "Camera opened")
            c.contains("open browser") || c.contains("browser kholo") || c.contains("chrome kholo") -> launch(Intent(Intent.ACTION_VIEW).apply { data = android.net.Uri.parse("https://www.google.com") }, "Browser opened")
            c.contains("open whatsapp") || c.contains("whatsapp kholo") -> launchPackage("com.whatsapp", "WhatsApp opened")
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
