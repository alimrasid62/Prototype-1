package com.alimrasid.prototype1.ui.detection

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.ImageReader
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.widget.Toast
import com.alimrasid.prototype1.R
import com.alimrasid.prototype1.customview.OverlayView
import com.alimrasid.prototype1.env.BorderedText
import com.alimrasid.prototype1.env.ImageUtils
import com.alimrasid.prototype1.env.Logger
import com.alimrasid.prototype1.tflite.Classifier
import com.alimrasid.prototype1.tflite.YoloV4Classifier
import com.alimrasid.prototype1.tracking.MultiBoxTracker
import java.io.IOException
import java.util.LinkedList

class DetectorActivity : CameraActivity(), ImageReader.OnImageAvailableListener {
    var trackingOverlay: OverlayView? = null
    private var sensorOrientation: Int? = null
    private var detector: Classifier? = null
    private var lastProcessingTimeMs: Long = 0
    private var rgbFrameBitmap: Bitmap? = null
    private var croppedBitmap: Bitmap? = null
    private var cropCopyBitmap: Bitmap? = null
    private var computingDetection = false
    private var timestamp: Long = 0
    private var frameToCropTransform: Matrix? = null
    private var cropToFrameTransform: Matrix? = null
    private var tracker: MultiBoxTracker? = null
    private var borderedText: BorderedText? = null
    override fun onPreviewSizeChosen(size: Size, rotation: Int) {
        val textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, resources.displayMetrics
        )
        borderedText = BorderedText(textSizePx)
        borderedText!!.setTypeface(Typeface.MONOSPACE)
        tracker = MultiBoxTracker(this)
        var cropSize = TF_OD_API_INPUT_SIZE
        try {
            detector = YoloV4Classifier.create(
                assets,
                TF_OD_API_MODEL_FILE,
                TF_OD_API_LABELS_FILE,
                TF_OD_API_IS_QUANTIZED
            )
            cropSize = TF_OD_API_INPUT_SIZE
        } catch (e: IOException) {
            e.printStackTrace()
            LOGGER.e(e, "Exception initializing classifier!")
            val toast = Toast.makeText(
                applicationContext, "Classifier could not be initialized", Toast.LENGTH_SHORT
            )
            toast.show()
            finish()
        }
        previewWidth = size.width
        previewHeight = size.height
        sensorOrientation = rotation - getScreenOrientation()
        LOGGER.i(
            "Camera orientation relative to screen canvas: %d",
            sensorOrientation!!
        )
        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight)
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
        frameToCropTransform = ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation!!, MAINTAIN_ASPECT
        )
        cropToFrameTransform = Matrix()
        frameToCropTransform!!.invert(cropToFrameTransform)
        trackingOverlay = findViewById(R.id.tracking_overlay) as OverlayView
        trackingOverlay!!.addCallback(
            object : OverlayView.DrawCallback {

                override fun drawCallback(canvas: Canvas) {
                    tracker!!.draw(canvas!!)
                    if (isDebug()) {
                        tracker!!.drawDebug(canvas)
                    }
                }
            })
        tracker!!.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation!!)
    }

    override fun processImage() {
        ++timestamp
        val currTimestamp = timestamp
        trackingOverlay!!.postInvalidate()

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage()
            return
        }
        computingDetection = true
        LOGGER.i("Preparing image $currTimestamp for detection in bg thread.")
        rgbFrameBitmap!!.setPixels(
            getRgbBytes(),
            0,
            previewWidth,
            0,
            0,
            previewWidth,
            previewHeight
        )
        readyForNextImage()
        val canvas = Canvas(croppedBitmap!!)
        canvas.drawBitmap(rgbFrameBitmap!!, frameToCropTransform!!, null)
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap!!)
        }
        runInBackground {
            LOGGER.i("Running detection on image $currTimestamp")
            val startTime = SystemClock.uptimeMillis()
            val results: List<Classifier.Recognition> =
                detector!!.recognizeImage(croppedBitmap!!)
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime
            Log.e("CHECK", "run: " + results.size)
            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap!!)
            val canvas = Canvas(cropCopyBitmap)


            val paint = Paint()
            paint.color = Color.RED
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.0f
            var minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API
            minimumConfidence = when (MODE) {
                DetectorMode.TF_OD_API -> MINIMUM_CONFIDENCE_TF_OD_API
            }
            val mappedRecognitions: MutableList<Classifier.Recognition> =
                LinkedList<Classifier.Recognition>()
            for (result in results) {
                val location: RectF? = result.location
                if (location != null && result.confidence!! >= minimumConfidence) {
                    canvas.drawRect(location, paint)
                    cropToFrameTransform!!.mapRect(location)
                    result.location
                    mappedRecognitions.add(result)
                }
            }
            tracker!!.trackResults(mappedRecognitions, currTimestamp)
            trackingOverlay!!.postInvalidate()
            computingDetection = false
            runOnUiThread {
                showFrameInfo(previewWidth + "x" + previewHeight)
                showCropInfo(
                    cropCopyBitmap.getWidth().toString() + "x" + cropCopyBitmap.getHeight()
                )
                showInference(lastProcessingTimeMs.toString() + "ms")
            }
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.tfe_od_camera_connection_fragment_tracking
    }

    override fun getDesiredPreviewFrameSize(): Size {
        return DESIRED_PREVIEW_SIZE
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum class DetectorMode {
        TF_OD_API
    }

    override fun setUseNNAPI(isChecked: Boolean) {
        runInBackground { detector!!.setUseNNAPI(isChecked) }
    }

    override fun setNumThreads(numThreads: Int) {
        runInBackground { detector!!.setNumThreads(numThreads) }
    }

    override fun setUseKata(isChecked: Boolean) {
        runInBackground {
            TF_OD_API_LABELS_FILE =
                "file:///android_asset/custom_kata.txt"
            TF_OD_API_MODEL_FILE = "custom-416.tflite"
            try {
                detector = YoloV4Classifier.create(
                    assets,
                    TF_OD_API_MODEL_FILE,
                    TF_OD_API_LABELS_FILE,
                    TF_OD_API_IS_QUANTIZED
                )
            } catch (e: IOException) {
                e.printStackTrace()
                LOGGER.e(e, "Exception initializing classifier!")
                val toast = Toast.makeText(
                    applicationContext,
                    "Classifier could not be initialized",
                    Toast.LENGTH_SHORT
                )
                toast.show()
                finish()
            }
        }
    }

    companion object {
        private val LOGGER = Logger()
        private const val TF_OD_API_INPUT_SIZE = 416
        private const val TF_OD_API_IS_QUANTIZED = false
        private var TF_OD_API_MODEL_FILE = "custom-416.tflite"
        private var TF_OD_API_LABELS_FILE = "file:///android_asset/custom.txt"
        private val MODE = DetectorMode.TF_OD_API
        private const val MINIMUM_CONFIDENCE_TF_OD_API = 0.5f
        private const val MAINTAIN_ASPECT = false
        private val DESIRED_PREVIEW_SIZE = Size(640, 480)
        private const val SAVE_PREVIEW_BITMAP = false
        private const val TEXT_SIZE_DIP = 10f
    }
}
