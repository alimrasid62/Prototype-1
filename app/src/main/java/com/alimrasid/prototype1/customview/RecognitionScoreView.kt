package com.alimrasid.prototype1.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.alimrasid.prototype1.tflite.Classifier

class RecognitionScoreView @JvmOverloads constructor(
    context: Context,
    set: AttributeSet? = null
) : View(context, set), ResultsView {

    private val TEXT_SIZE_DIP = 14f
    private val textSizePx: Float
    private val fgPaint: Paint
    private val bgPaint: Paint
    private var results: List<Classifier.Recognition>? = null

    init {
        textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            TEXT_SIZE_DIP,
            resources.displayMetrics
        )
        fgPaint = Paint()
        fgPaint.textSize = textSizePx

        bgPaint = Paint()
        bgPaint.color = 0xcc4285f4.toInt()
    }

    override fun setResults(results: List<Classifier.Recognition>) {
        this.results = results
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val x = 10f
        var y = fgPaint.textSize * 1.5f

        canvas.drawPaint(bgPaint)

        results?.let {
            for (recog in it) {
                canvas.drawText("${recog.title}: ${recog.confidence}", x, y, fgPaint)
                y += fgPaint.textSize * 1.5f
            }
        }
    }
}
