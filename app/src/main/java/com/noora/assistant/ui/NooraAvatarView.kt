package com.noora.assistant.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.noora.assistant.R

/**
 * Full-screen NOORA visual surface.
 * The NOORA artwork remains the visual base. Existing setState() behavior is preserved.
 * Visible Voice / Settings / Exit artwork is backed by real touch zones.
 */
class NooraAvatarView(
    context: Context,
    private val onAction: (Action) -> Unit = {}
) : View(context) {

    enum class Action { VOICE, SETTINGS, EXIT }

    private val nooraImage: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.noora_main)
    private val destination = RectF()
    private var state = "Ready"
    private var motion = 0f
    private var animator: ValueAnimator? = null

    fun setState(value: String) {
        state = value
        startMotionForState(value)
        invalidate()
    }

    private fun startMotionForState(value: String) {
        animator?.cancel()

        val amplitude = when {
            value.equals("Speaking", true) -> 0.012f
            value.equals("Listening", true) -> 0.008f
            value.equals("Thinking", true) -> 0.006f
            value.equals("Salute", true) -> 0.014f
            else -> 0.003f
        }

        animator = ValueAnimator.ofFloat(-amplitude, amplitude).apply {
            duration = if (value.equals("Speaking", true)) 420L else 1600L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                motion = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width <= 0 || height <= 0 || nooraImage.width <= 0 || nooraImage.height <= 0) return

        val baseScale = maxOf(
            width.toFloat() / nooraImage.width.toFloat(),
            height.toFloat() / nooraImage.height.toFloat()
        )
        val scale = baseScale * (1f + motion)
        val drawWidth = nooraImage.width * scale
        val drawHeight = nooraImage.height * scale
        val left = (width - drawWidth) / 2f
        val top = (height - drawHeight) / 2f

        destination.set(left, top, left + drawWidth, top + drawHeight)
        canvas.drawBitmap(nooraImage, null, destination, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true

        val x = event.x / width.toFloat()
        val y = event.y / height.toFloat()

        when {
            x in 0.02f..0.20f && y in 0.62f..0.77f -> {
                onAction(Action.VOICE)
                return true
            }
            x in 0.02f..0.20f && y in 0.76f..0.88f -> {
                onAction(Action.SETTINGS)
                return true
            }
            x in 0.02f..0.20f && y >= 0.88f -> {
                onAction(Action.EXIT)
                return true
            }
        }

        return true
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }
}
