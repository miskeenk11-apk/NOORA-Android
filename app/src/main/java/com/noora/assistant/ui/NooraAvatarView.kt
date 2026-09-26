package com.noora.assistant.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import kotlin.math.min

/** Lightweight local avatar foundation. No network, camera, or external assets required. */
class NooraAvatarView(context: Context) : View(context) {
    private val face = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eye = Paint(Paint.ANTI_ALIAS_FLAG)
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        textSize = 34f
    }
    private var state = "Ready"

    fun setState(value: String) {
        state = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = min(width, height).toFloat()
        val cx = width / 2f
        val cy = s * .48f

        face.style = Paint.Style.FILL
        face.color = 0xFFF1D2BE.toInt()
        canvas.drawOval(cx - s*.31f, cy - s*.37f, cx + s*.31f, cy + s*.37f, face)

        face.color = 0xFF3A241C.toInt()
        canvas.drawArc(cx - s*.33f, cy - s*.40f, cx + s*.33f, cy + s*.18f, 180f, 180f, true, face)

        eye.color = 0xFF4F8FD8.toInt()
        canvas.drawCircle(cx - s*.105f, cy - s*.02f, s*.038f, eye)
        canvas.drawCircle(cx + s*.105f, cy - s*.02f, s*.038f, eye)

        face.color = 0xFF9B4B55.toInt()
        face.style = Paint.Style.STROKE
        face.strokeWidth = s*.018f
        canvas.drawArc(cx - s*.09f, cy + s*.08f, cx + s*.09f, cy + s*.18f, 15f, 150f, false, face)

        label.color = 0xFF333333.toInt()
        canvas.drawText("NOORA", cx, s*.92f, label)
        label.textSize = 22f
        canvas.drawText(state, cx, s*.99f, label)
    }
}
