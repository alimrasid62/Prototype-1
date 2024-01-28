package com.alimrasid.prototype1.env

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.Paint.Align
import android.graphics.Paint.Style

class BorderedText(private val textSize: Float) {
    private val interiorPaint: Paint = Paint()
    private val exteriorPaint: Paint = Paint()

    init {
        interiorPaint.textSize = textSize
        interiorPaint.color = Color.WHITE
        interiorPaint.style = Style.FILL
        interiorPaint.isAntiAlias = false
        interiorPaint.alpha = 255

        exteriorPaint.textSize = textSize
        exteriorPaint.style = Style.FILL_AND_STROKE
        exteriorPaint.strokeWidth = textSize / 8
        exteriorPaint.isAntiAlias = false
        exteriorPaint.alpha = 255
    }

    constructor(interiorColor: Int, exteriorColor: Int, textSize: Float) : this(textSize) {
        interiorPaint.color = interiorColor
        exteriorPaint.color = exteriorColor
    }

    fun setTypeface(typeface: Typeface) {
        interiorPaint.typeface = typeface
        exteriorPaint.typeface = typeface
    }

    fun drawText(canvas: Canvas, posX: Float, posY: Float, text: String) {
        canvas.drawText(text, posX, posY, exteriorPaint)
        canvas.drawText(text, posX, posY, interiorPaint)
    }

    fun drawText(canvas: Canvas, posX: Float, posY: Float, text: String, bgPaint: Paint) {
        val width = exteriorPaint.measureText(text)
        val textSize = exteriorPaint.textSize
        val paint = Paint(bgPaint)
        paint.style = Style.FILL
        paint.alpha = 160
        canvas.drawRect(posX, posY + textSize, posX + width, posY, paint)
        canvas.drawText(text, posX, posY + textSize, interiorPaint)
    }

    fun drawLines(canvas: Canvas, posX: Float, posY: Float, lines: List<String>) {
        var lineNum = 0
        for (line in lines) {
            drawText(canvas, posX, posY - textSize * (lines.size - lineNum - 1), line)
            lineNum++
        }
    }

    fun setInteriorColor(color: Int) {
        interiorPaint.color = color
    }

    fun setExteriorColor(color: Int) {
        exteriorPaint.color = color
    }

    fun getTextSize(): Float {
        return textSize
    }

    fun setAlpha(alpha: Int) {
        interiorPaint.alpha = alpha
        exteriorPaint.alpha = alpha
    }

    fun getTextBounds(line: String, index: Int, count: Int, lineBounds: Rect) {
        interiorPaint.getTextBounds(line, index, count, lineBounds)
    }

    fun setTextAlign(align: Align) {
        interiorPaint.textAlign = align
        exteriorPaint.textAlign = align
    }
}

