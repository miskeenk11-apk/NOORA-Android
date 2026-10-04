package com.noora.assistant.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.noora.assistant.R

class NooraAvatarView(context: Context, private val onAction: (Action) -> Unit = {}) : View(context) {
    enum class Action { VOICE, NOORA, SETTINGS, EXIT }
    private val image: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.noora_main)
    private val rect = RectF()
    private val phone = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var state = "Ready"
    private var phase = 0f
    private var animator: ValueAnimator? = null

    fun setState(value: String) {
        state = value
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = when (value.lowercase()) {
                "speaking" -> 260L
                "listening" -> 850L
                "thinking", "reading" -> 1050L
                "salute" -> 700L
                else -> 1800L
            }
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { phase = it.animatedValue as Float; invalidate() }
            start()
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        if (width <= 0 || height <= 0 || image.width <= 0) return
        val scale = maxOf(width.toFloat() / image.width, height.toFloat() / image.height)
        val w = image.width * scale
        val h = image.height * scale
        val p = kotlin.math.sin(phase * Math.PI).toFloat()
        val yMove = when (state.lowercase()) {
            "speaking" -> 5f * p
            "listening" -> 3f * p
            "thinking", "reading" -> 2f * p
            "salute" -> -7f * p
            else -> 1.5f * p
        }
        rect.set((width-w)/2f, (height-h)/2f+yMove, (width+w)/2f, (height+h)/2f+yMove)
        canvas.drawBitmap(image, null, rect, paint)

        // NOORA holds/uses a smartphone as part of the main character interaction.\n        run {
            val cx = width * .52f
            val cy = height * .70f + 5f*p
            phone.set(cx-42f, cy-72f, cx+42f, cy+72f)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(235,18,22,30)
            canvas.drawRoundRect(phone,18f,18f,paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = Color.argb(210,120,190,255)
            canvas.drawRoundRect(phone,18f,18f,paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            canvas.drawCircle(cx,cy+59f,4f,paint)
            paint.color = Color.argb(180,120,190,255)
            canvas.drawRoundRect(cx-28f,cy-38f,cx+28f,cy-30f,4f,4f,paint)
            canvas.drawRoundRect(cx-28f,cy-16f,cx+18f,cy-8f,4f,4f,paint)
        }

        // Small state label keeps the voice-first UI understandable without becoming a chat screen.\n        paint.style = Paint.Style.FILL\n        paint.textSize = 24f\n        paint.color = Color.argb(210,235,245,255)\n        canvas.drawText(state, 28f, 52f, paint)\n\n        drawControl(canvas, "EXIT", 0.03f, 0.90f)\n        drawControl(canvas, "NOORA", 0.03f, 0.82f)\n        drawControl(canvas, "SETTINGS", 0.03f, 0.74f)\n\n        if (state.equals("Speaking", true)) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.argb(120,150,210,255)
            canvas.drawCircle(width*.5f,height*.48f,70f+8f*p,paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawControl(canvas: Canvas, label: String, x: Float, y: Float) {\n        val left=width*x; val top=height*y; val right=left+150f; val bottom=top+52f\n        paint.style=Paint.Style.FILL; paint.color=Color.argb(150,8,18,28)\n        canvas.drawRoundRect(left,top,right,bottom,18f,18f,paint)\n        paint.style=Paint.Style.STROKE; paint.strokeWidth=2f; paint.color=Color.argb(130,150,210,255)\n        canvas.drawRoundRect(left,top,right,bottom,18f,18f,paint)\n        paint.style=Paint.Style.FILL; paint.textSize=18f; paint.color=Color.WHITE\n        canvas.drawText(label,left+16f,top+33f,paint)\n    }\n\n    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val x=event.x/width; val y=event.y/height
        when {
            x in .02f.. .22f && y in .62f.. .74f -> onAction(Action.VOICE)
            x in .02f.. .22f && y in .74f.. .83f -> onAction(Action.NOORA)
            x in .02f.. .22f && y in .83f.. .91f -> onAction(Action.SETTINGS)
            x in .02f.. .22f && y > .91f -> onAction(Action.EXIT)
        }
        return true
    }

    override fun onDetachedFromWindow() { animator?.cancel(); animator=null; super.onDetachedFromWindow() }
}
