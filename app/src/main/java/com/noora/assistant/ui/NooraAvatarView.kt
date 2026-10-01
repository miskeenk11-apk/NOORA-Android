package com.noora.assistant.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.view.View
import com.noora.assistant.R

/**
 * Full-screen NOORA visual avatar.
 * The existing setState() interface is preserved so voice/wake-word logic remains unchanged.
 */
class NooraAvatarView(context: Context) : View(context) {
    private val nooraImage: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.noora_main)
    private val destination = RectF()
    private var state = "Ready"

    fun setState(value: String) {
        state = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width <= 0 || height <= 0) return

        val scale = maxOf(
            width.toFloat() / nooraImage.width.toFloat(),
            height.toFloat() / nooraImage.height.toFloat()
        )

        val drawWidth = nooraImage.width * scale
        val drawHeight = nooraImage.height * scale
        val left = (width - drawWidth) / 2f
        val top = (height - drawHeight) / 2f

        destination.set(left, top, left + drawWidth, top + drawHeight)
        canvas.drawBitmap(nooraImage, null, destination, null)
    }
}
