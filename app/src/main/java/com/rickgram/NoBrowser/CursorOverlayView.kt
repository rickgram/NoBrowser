package com.rickgram.NoBrowser

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class CursorOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val density = resources.displayMetrics.density
    private val movementStep = 8f * density
    private val edgeInset = 2f * density
    private var positioned = false
    private val cursorPath = Path()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.25f * density
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    var cursorX: Float = 0f
        private set
    var cursorY: Float = 0f
        private set

    init {
        isClickable = false
        isFocusable = false
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (!positioned) {
            cursorX = width / 2f
            cursorY = height / 2f
            positioned = true
        } else {
            cursorX = cursorX.coerceIn(edgeInset, (width - edgeInset).coerceAtLeast(edgeInset))
            cursorY = cursorY.coerceIn(edgeInset, (height - edgeInset).coerceAtLeast(edgeInset))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        cursorPath.reset()
        cursorPath.moveTo(cursorX, cursorY)
        cursorPath.lineTo(cursorX + 26f * density, cursorY + 8f * density)
        cursorPath.lineTo(cursorX + 13f * density, cursorY + 11f * density)
        cursorPath.lineTo(cursorX + 10f * density, cursorY + 23f * density)
        cursorPath.close()
        canvas.drawPath(cursorPath, fillPaint)
        canvas.drawPath(cursorPath, outlinePaint)
    }

    fun move(direction: CursorDirection): CursorMovement {
        val movement = CursorMotion.move(
            x = cursorX,
            y = cursorY,
            direction = direction,
            viewportWidth = width.toFloat(),
            viewportHeight = height.toFloat(),
            step = movementStep,
            edgeInset = edgeInset
        )
        cursorX = movement.x
        cursorY = movement.y
        invalidate()
        return movement
    }
}
