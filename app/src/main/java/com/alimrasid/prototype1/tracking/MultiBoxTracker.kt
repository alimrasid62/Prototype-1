package com.alimrasid.prototype1.tracking

import com.alimrasid.prototype1.env.BorderedText
import com.alimrasid.prototype1.env.ImageUtils
import com.alimrasid.prototype1.env.Logger
import com.alimrasid.prototype1.tflite.Classifier
import java.util.*

import android.content.Context
import android.graphics.*
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import android.util.TypedValue
import java.util.*

class MultiBoxTracker(private val context: Context) {
    private val TEXT_SIZE_DIP = 18f
    private val MIN_SIZE = 16.0f
    private val COLORS = intArrayOf(
        Color.BLUE,
        Color.RED,
        Color.GREEN,
        Color.YELLOW,
        Color.CYAN,
        Color.MAGENTA,
        Color.WHITE,
        Color.parseColor("#55FF55"),
        Color.parseColor("#FFA500"),
        Color.parseColor("#FF8888"),
        Color.parseColor("#AAAAFF"),
        Color.parseColor("#FFFFAA"),
        Color.parseColor("#55AAAA"),
        Color.parseColor("#AA33AA"),
        Color.parseColor("#0D0068")
    )
    private val screenRects: MutableList<Pair<Float, RectF>> = LinkedList()
    private val logger = Logger()
    private val availableColors: Queue<Int> = LinkedList()
    private val trackedObjects: MutableList<TrackedRecognition> = LinkedList()
    private val boxPaint = Paint()
    private val textSizePx: Float
    private val borderedText: BorderedText
    private var frameToCanvasMatrix: Matrix? = null
    private var frameWidth = 0
    private var frameHeight = 0
    private var sensorOrientation = 0

    init {
        for (color in COLORS) {
            availableColors.add(color)
        }

        boxPaint.color = Color.RED
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = 10.0f
        boxPaint.strokeCap = Paint.Cap.ROUND
        boxPaint.strokeJoin = Paint.Join.ROUND
        boxPaint.strokeMiter = 100f

        textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.resources.displayMetrics
        )
        borderedText = BorderedText(textSizePx)
    }

    @Synchronized
    fun setFrameConfiguration(width: Int, height: Int, sensorOrientation: Int) {
        frameWidth = width
        frameHeight = height
        this.sensorOrientation = sensorOrientation
    }

    @Synchronized
    fun drawDebug(canvas: Canvas) {
        val textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.textSize = 60.0f

        val boxPaint = Paint()
        boxPaint.color = Color.RED
        boxPaint.alpha = 200
        boxPaint.style = Paint.Style.STROKE

        for (detection in screenRects) {
            val rect = detection.second
            canvas.drawRect(rect, boxPaint)
            canvas.drawText("" + detection.first, rect.left, rect.top, textPaint)
            borderedText.drawText(canvas, rect.centerX(), rect.centerY(), "" + detection.first)
        }
    }

    @Synchronized
    fun trackResults(results: List<Classifier.Recognition>, timestamp: Long) {
        logger.i("Processing %d results from %d", results.size, timestamp)
        processResults(results)
    }

    private fun getFrameToCanvasMatrix(): Matrix? {
        return frameToCanvasMatrix
    }

    @Synchronized
    fun draw(canvas: Canvas) {
        val rotated = sensorOrientation % 180 == 90
        val multiplier = Math.min(
            canvas.height / (frameWidth.toFloat() * if (rotated) 1 else frameHeight.toFloat()),
            canvas.width / (frameHeight.toFloat() * if (rotated) 1 else frameWidth.toFloat())
        )
        frameToCanvasMatrix = ImageUtils.getTransformationMatrix(
            frameWidth,
            frameHeight,
            (multiplier * (if (rotated) frameHeight else frameWidth)).toInt(),
            (multiplier * (if (rotated) frameWidth else frameHeight)).toInt(),
            sensorOrientation,
            false
        )
        for (recognition in trackedObjects) {
            val trackedPos = RectF(recognition.location)

            getFrameToCanvasMatrix()?.mapRect(trackedPos)
            boxPaint.color = recognition.color

            val cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f
            canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint)

            val labelString = if (!TextUtils.isEmpty(recognition.title)) {
                String.format(
                    "%s %.2f",
                    recognition.title,
                    (100 * recognition.detectionConfidence)
                )
            } else {
                String.format("%.2f", (100 * recognition.detectionConfidence))
            }
            borderedText.drawText(
                canvas,
                trackedPos.left + cornerSize,
                trackedPos.top,
                labelString + "%",
                boxPaint
            )
        }
    }

    private fun processResults(results: List<Classifier.Recognition>) {
        val rectsToTrack: MutableList<Pair<Float, Classifier.Recognition>> = LinkedList()

        screenRects.clear()
        val rgbFrameToScreen = Matrix(getFrameToCanvasMatrix())

        for (result in results) {
            if (result.location == null) {
                continue
            }
            val detectionFrameRect = RectF(result.location)

            val detectionScreenRect = RectF()
            rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect)

            logger.v(
                "Result! Frame: " + result.location + " mapped to screen:" + detectionScreenRect
            )

            screenRects.add(Pair(result.confidence, detectionScreenRect))

            if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
                logger.w("Degenerate rectangle! " + detectionFrameRect)
                continue
            }

            rectsToTrack.add(Pair(result.confidence, result))
        }

        trackedObjects.clear()
        if (rectsToTrack.isEmpty()) {
            logger.v("Nothing to track, aborting.")
            return
        }

        for (potential in rectsToTrack) {
            val trackedRecognition = TrackedRecognition()
            trackedRecognition.detectionConfidence = potential.first
            trackedRecognition.location = RectF(potential.second.location)
            trackedRecognition.title = potential.second.title
            trackedRecognition.color = COLORS[trackedObjects.size]
            trackedObjects.add(trackedRecognition)

            if (trackedObjects.size >= COLORS.size) {
                break
            }
        }
    }

    private class TrackedRecognition {
        var location: RectF? = null
        var detectionConfidence = 0f
        var color = 0
        var title: String? = null
    }
}

