package com.noora.assistant.phone

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class NooraAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var pendingWhatsAppMessage: String? = null
    private var whatsappAttempts = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val message = pendingWhatsAppMessage ?: return
        if (event?.packageName?.toString() != WHATSAPP_PACKAGE) return
        automateWhatsAppMessage(message)
    }

    override fun onInterrupt() {
        pendingWhatsAppMessage = null
        handler.removeCallbacksAndMessages(null)
    }

    override fun onServiceConnected() {
        instance = this
    }

    fun queueWhatsAppMessage(message: String) {
        pendingWhatsAppMessage = message.trim()
        whatsappAttempts = 0
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ automateWhatsAppMessage(message.trim()) }, 900L)
    }

    private fun automateWhatsAppMessage(message: String) {
        val root = rootInActiveWindow ?: return retryWhatsApp(message)
        val input = findMessageInput(root)
        if (input == null) return retryWhatsApp(message)

        val set = input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT)
        if (!set) {
            input.recycle()
            return retryWhatsApp(message)
        }

        handler.postDelayed({
            val currentRoot = rootInActiveWindow ?: return@postDelayed retryWhatsApp(message)
            val send = findSendButton(currentRoot)
            if (send != null) {
                val clicked = send.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                send.recycle()
                if (clicked) {
                    pendingWhatsAppMessage = null
                    whatsappAttempts = 0
                    return@postDelayed
                }
            }
            retryWhatsApp(message)
        }, 350L)
        input.recycle()
    }

    private fun retryWhatsApp(message: String) {
        if (pendingWhatsAppMessage == null) return
        whatsappAttempts++
        if (whatsappAttempts > 15) {
            pendingWhatsAppMessage = null
            whatsappAttempts = 0
            return
        }
        handler.postDelayed({ automateWhatsAppMessage(message) }, 500L)
    }

    private fun findMessageInput(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val nodes = ArrayList<AccessibilityNodeInfo>()
        collectNodes(root, nodes)
        val result = nodes.firstOrNull { node ->
            val className = node.className?.toString().orEmpty()
            val hint = node.hintText?.toString().orEmpty().lowercase(Locale.ROOT)
            val desc = node.contentDescription?.toString().orEmpty().lowercase(Locale.ROOT)
            className.contains("EditText", true) &&
                (hint.contains("message") || desc.contains("message") || node.isEditable)
        }
        nodes.filter { it !== result }.forEach { it.recycle() }
        return result
    }

    private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val nodes = ArrayList<AccessibilityNodeInfo>()
        collectNodes(root, nodes)
        val result = nodes.firstOrNull { node ->
            val text = node.text?.toString().orEmpty().lowercase(Locale.ROOT)
            val desc = node.contentDescription?.toString().orEmpty().lowercase(Locale.ROOT)
            (text == "send" || desc == "send" || desc.contains("send")) && node.isClickable
        }
        nodes.filter { it !== result }.forEach { it.recycle() }
        return result
    }

    private fun collectNodes(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        out.add(AccessibilityNodeInfo.obtain(node))
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectNodes(child, out)
                child.recycle()
            }
        }
    }

    fun performNooraAction(action: String): Boolean = when (action.lowercase()) {
        "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
        "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
        "recent", "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        else -> false
    }

    fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 350): Boolean {
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration.coerceIn(80, 2000)))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    companion object {
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        @Volatile var instance: NooraAccessibilityService? = null
    }
}
