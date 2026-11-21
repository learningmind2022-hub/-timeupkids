package com.amolbangde.timeupkids

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.sin

/**
 * Ek udaas, thaka hua, rota hua laal dimaag (cartoon style).
 * Canvas par draw hota hai (200 x 190 ke virtual grid me), isliye koi image file nahi chahiye.
 */
class CryingBrainView(context: Context) : View(context) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private var t = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1400
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { t = it.animatedValue as Float; invalidate() }
    }

    // cx, cy, r (outer outline radius)
    private val blobs = arrayOf(
        floatArrayOf(60f, 70f, 41f), floatArrayOf(100f, 55f, 45f), floatArrayOf(142f, 70f, 41f),
        floatArrayOf(50f, 105f, 37f), floatArrayOf(150f, 105f, 37f),
        floatArrayOf(80f, 125f, 39f), floatArrayOf(120f, 125f, 39f), floatArrayOf(100f, 100f, 53f)
    )

    // tear x position, phase offset
    private val tears = arrayOf(
        floatArrayOf(68f, 0f), floatArrayOf(84f, 0.32f),
        floatArrayOf(116f, 0.64f), floatArrayOf(132f, 0.32f)
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val s = minOf(width / 200f, height / 190f)
        canvas.save()
        canvas.translate((width - 200f * s) / 2f, (height - 190f * s) / 2f)
        canvas.scale(s, s)

        // sobbing shake
        val sob = sin(t * 2f * PI.toFloat() * 3f)
        canvas.rotate(sob * 1.5f, 100f, 100f)
        canvas.translate(0f, (sob + 1f) * 1.5f)

        drawBrain(canvas)
        drawFace(canvas)
        drawTears(canvas)
        canvas.restore()
    }

    private fun drawBrain(c: Canvas) {
        fill.color = Color.parseColor("#7A0F22")
        for (b in blobs) c.drawCircle(b[0], b[1], b[2], fill)
        fill.color = Color.parseColor("#E53957")
        for (b in blobs) c.drawCircle(b[0], b[1], b[2] - 3f, fill)

        line.color = Color.parseColor("#A81C37")
        line.strokeWidth = 3f
        val p = Path()
        p.moveTo(100f, 14f); p.cubicTo(94f, 38f, 106f, 52f, 100f, 62f)
        p.moveTo(42f, 62f); p.quadTo(58f, 48f, 76f, 58f)
        p.moveTo(124f, 58f); p.quadTo(142f, 48f, 158f, 62f)
        p.moveTo(28f, 108f); p.quadTo(40f, 96f, 52f, 106f)
        p.moveTo(148f, 106f); p.quadTo(160f, 96f, 172f, 108f)
        p.moveTo(62f, 150f); p.quadTo(76f, 140f, 90f, 150f)
        p.moveTo(110f, 150f); p.quadTo(124f, 140f, 138f, 150f)
        c.drawPath(p, line)
    }

    private fun drawFace(c: Canvas) {
        val dark = Color.parseColor("#5A0A18")
        for (cx in floatArrayOf(76f, 124f)) {
            // eye white (pinkish)
            val oval = RectF(cx - 16f, 80f, cx + 16f, 116f)
            fill.color = Color.parseColor("#FFDEDE")
            c.drawOval(oval, fill)
            line.color = dark; line.strokeWidth = 2.5f
            c.drawOval(oval, line)

            // red veins
            line.color = Color.parseColor("#E60000"); line.strokeWidth = 1.3f
            c.drawLine(cx - 15f, 94f, cx - 7f, 96f, line)
            c.drawLine(cx - 15f, 102f, cx - 7f, 100f, line)
            c.drawLine(cx + 15f, 94f, cx + 7f, 96f, line)
            c.drawLine(cx + 15f, 102f, cx + 7f, 100f, line)
            c.drawLine(cx - 6f, 82f, cx - 3f, 88f, line)
            c.drawLine(cx + 6f, 114f, cx + 3f, 108f, line)
            c.drawLine(cx, 80f, cx, 86f, line)

            // red iris, pupil, shine
            fill.color = Color.parseColor("#D50000"); c.drawCircle(cx, 102f, 8.5f, fill)
            fill.color = Color.parseColor("#1A0000"); c.drawCircle(cx, 102f, 4.5f, fill)
            fill.color = Color.WHITE; c.drawCircle(cx - 3f, 99f, 1.8f, fill)
        }

        // sad eyebrows (inner ends raised)
        line.color = dark; line.strokeWidth = 4f
        c.drawLine(58f, 72f, 92f, 62f, line)
        c.drawLine(108f, 62f, 142f, 72f, line)

        // open, wailing mouth
        val mouth = Path().apply {
            moveTo(82f, 138f); quadTo(100f, 114f, 118f, 138f); close()
        }
        fill.color = Color.parseColor("#3A0610"); c.drawPath(mouth, fill)
        line.color = dark; line.strokeWidth = 3f; c.drawPath(mouth, line)
    }

    private fun drawTears(c: Canvas) {
        val drop = Path().apply {
            moveTo(0f, -7f)
            cubicTo(5f, 0f, 6f, 5f, 0f, 8f)
            cubicTo(-6f, 5f, -5f, 0f, 0f, -7f)
        }
        fill.color = Color.parseColor("#6FD0FF")
        for (tr in tears) {
            val p = (t + tr[1]) % 1f
            val alpha = if (p < 0.15f) p / 0.15f else 1f - (p - 0.15f) / 0.85f
            fill.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
            c.save()
            c.translate(tr[0], 118f + p * 55f)
            c.drawPath(drop, fill)
            c.restore()
        }
        fill.alpha = 255
    }
}
