package com.alimrasid.prototype1.tflite

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import com.alimrasid.prototype1.env.Logger
import com.alimrasid.prototype1.env.Utils
import com.alimrasid.prototype1.ui.MainActivity
import org.tensorflow.lite.Delegate
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class YoloV4Classifier private constructor() : Classifier {

    companion object {
        @Throws(IOException::class)
        fun create(
            assetManager: AssetManager,
            modelFilename: String,
            labelFilename: String,
            isQuantized: Boolean
        ): Classifier {
            val yoloV4Classifier = YoloV4Classifier()

            val actualFilename = labelFilename.split("file:///android_asset/")[1]
            val labelsInput: InputStream = assetManager.open(actualFilename)
            val br = BufferedReader(InputStreamReader(labelsInput))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                LOGGER.w(line!!)
                yoloV4Classifier.labels.add(line!!)
            }
            br.close()

            try {
                val options = Interpreter.Options()
                options.setNumThreads(NUM_THREADS)
                if (isNNAPI) {
                    var nnApiDelegate: Delegate? = null
                    // Initialize interpreter with NNAPI delegate for Android Pie or above
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        nnApiDelegate = NnApiDelegate()
                        options.addDelegate(nnApiDelegate)
                        options.setNumThreads(NUM_THREADS)
                        options.setUseNNAPI(false)
                        options.setAllowFp16PrecisionForFp32(true)
                        options.setAllowBufferHandleOutput(true)
                        options.setUseNNAPI(true)
                    }
                }
                if (isGPU) {
                    val gpuDelegate = GpuDelegate()
                    options.addDelegate(gpuDelegate)
                }
                yoloV4Classifier.tfLite =
                    Interpreter(Utils.loadModelFile(assetManager, modelFilename), options)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

            yoloV4Classifier.isModelQuantized = isQuantized
            // Pre-allocate buffers.
            val numBytesPerChannel = if (isQuantized) 1 else 4
            yoloV4Classifier.imgData =
                ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * numBytesPerChannel)
            yoloV4Classifier.imgData.order(ByteOrder.nativeOrder())
            yoloV4Classifier.intValues = IntArray(INPUT_SIZE * INPUT_SIZE)

            return yoloV4Classifier
        }

        private const val IMAGE_MEAN = 0f
        private const val IMAGE_STD = 255.0f
        private const val INPUT_SIZE = 416
        private val OUTPUT_WIDTH_FULL = intArrayOf(10647, 10647)
        private val OUTPUT_WIDTH_TINY = intArrayOf(2535, 2535)
        private const val NUM_BOXES_PER_BLOCK = 3
        private const val NUM_THREADS = 4
        private var isNNAPI = false
        private var isGPU = true
        private var isTiny = true
        private const val BATCH_SIZE = 1
        private const val PIXEL_SIZE = 3
        private val LOGGER = Logger()

    }

    private var isModelQuantized: Boolean = false
    private lateinit var labels: Vector<String>
    private lateinit var intValues: IntArray
    private lateinit var imgData: ByteBuffer
    private lateinit var tfLite: Interpreter

    override fun enableStatLogging(logStats: Boolean) {}

    override fun getStatString(): String {
        return ""
    }

    override fun close() {}

    override fun setNumThreads(num_threads: Int) {
        if (tfLite != null) tfLite.setNumThreads(num_threads)
    }

    override fun setUseNNAPI(isChecked: Boolean) {
        if (tfLite != null) tfLite.setUseNNAPI(isChecked)
    }

    override val objThresh: Float
        get() = TODO("Not yet implemented")

    fun getObjThresh(): Float {
        return MainActivity.MINIMUM_CONFIDENCE_TF_OD_API
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer =
            ByteBuffer.allocateDirect(4 * BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(
            intValues,
            0,
            bitmap.width,
            0,
            0,
            bitmap.width,
            bitmap.height
        )
        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val `val` = intValues[pixel++]
                byteBuffer.putFloat(((`val` shr 16) and 0xFF) / 255.0f)
                byteBuffer.putFloat(((`val` shr 8) and 0xFF) / 255.0f)
                byteBuffer.putFloat((`val` and 0xFF) / 255.0f)
            }
        }
        return byteBuffer
    }

    private fun getDetectionsForFull(byteBuffer: ByteBuffer, bitmap: Bitmap): ArrayList<Classifier.Recognition> {
        val detections = ArrayList<Classifier.Recognition>()
        val outputMap: MutableMap<Int, Any> = HashMap()
        outputMap[0] = Array(1) { Array(OUTPUT_WIDTH_FULL[0]) { FloatArray(4) } }
        outputMap[1] = Array(1) { Array(OUTPUT_WIDTH_FULL[1]) { FloatArray(labels.size) } }
        val inputArray: Array<Any> = arrayOf(byteBuffer)
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap)

        val gridWidth = OUTPUT_WIDTH_FULL[0]
        val bboxes = outputMap[0] as Array<Array<FloatArray>>
        val out_score = outputMap[1] as Array<Array<FloatArray>>

        for (i in 0 until gridWidth) {
            var maxClass = 0f
            var detectedClass = -1
            val classes = FloatArray(labels.size)
            for (c in labels.indices) {
                classes[c] = out_score[0][i][c]
            }
            for (c in labels.indices) {
                if (classes[c] > maxClass) {
                    detectedClass = c
                    maxClass = classes[c]
                }
            }
            val score = maxClass
            if (score > getObjThresh()) {
                val xPos = bboxes[0][i][0]
                val yPos = bboxes[0][i][1]
                val w = bboxes[0][i][2]
                val h = bboxes[0][i][3]
                val rectF = RectF(
                    Math.max(0f, xPos - w / 2),
                    Math.max(0f, yPos - h / 2),
                    Math.min(bitmap.width - 1f, xPos + w / 2),
                    Math.min(bitmap.height - 1f, yPos + h / 2)
                )
                detections.add(
                    Classifier.Recognition(
                        "" + i,
                        labels[detectedClass],
                        score,
                        rectF,
                        detectedClass
                    )
                )
            }
        }
        return detections
    }

    private fun getDetectionsForTiny(byteBuffer: ByteBuffer, bitmap: Bitmap): ArrayList<Classifier.Recognition> {
        val detections = ArrayList<Classifier.Recognition>()
        val outputMap: MutableMap<Int, Any> = HashMap()
        outputMap[0] = Array(1) { Array(OUTPUT_WIDTH_TINY[0]) { FloatArray(4) } }
        outputMap[1] = Array(1) { Array(OUTPUT_WIDTH_TINY[1]) { FloatArray(labels.size) } }
        val inputArray: Array<Any> = arrayOf(byteBuffer)
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap)

        val gridWidth = OUTPUT_WIDTH_TINY[0]
        val bboxes = outputMap[0] as Array<Array<FloatArray>>
        val out_score = outputMap[1] as Array<Array<FloatArray>>

        for (i in 0 until gridWidth) {
            var maxClass = 0f
            var detectedClass = -1
            val classes = FloatArray(labels.size)
            for (c in labels.indices) {
                classes[c] = out_score[0][i][c]
            }
            for (c in labels.indices) {
                if (classes[c] > maxClass) {
                    detectedClass = c
                    maxClass = classes[c]
                }
            }
            val score = maxClass
            if (score > getObjThresh()) {
                val xPos = bboxes[0][i][0]
                val yPos = bboxes[0][i][1]
                val w = bboxes[0][i][2]
                val h = bboxes[0][i][3]
                val rectF = RectF(
                    Math.max(0f, xPos - w / 2),
                    Math.max(0f, yPos - h / 2),
                    Math.min(bitmap.width - 1f, xPos + w / 2),
                    Math.min(bitmap.height - 1f, yPos + h / 2)
                )
                detections.add(
                    Classifier.Recognition(
                        "" + i,
                        labels[detectedClass],
                        score,
                        rectF,
                        detectedClass
                    )
                )
            }
        }
        return detections
    }

    override fun recognizeImage(bitmap: Bitmap): ArrayList<Classifier.Recognition> {
        val byteBuffer = convertBitmapToByteBuffer(bitmap)

        val detections: ArrayList<Classifier.Recognition> = if (isTiny) {
            getDetectionsForTiny(byteBuffer, bitmap)
        } else {
            getDetectionsForFull(byteBuffer, bitmap)
        }
        return nms(detections)
    }

    private fun nms(list: ArrayList<Classifier.Recognition>): ArrayList<Classifier.Recognition> {
        val nmsList = ArrayList<Classifier.Recognition>()

        for (k in labels.indices) {
            val pq = PriorityQueue(
                50,
                Comparator { lhs: Classifier.Recognition, rhs: Classifier.Recognition ->
                    // Intentionally reversed to put high confidence at the head of the queue.
                    rhs.confidence?.let { java.lang.Float.compare(it, lhs.confidence!!) }!!
                })

            for (i in list.indices) {
                if (list[i].detectedClass == k) {
                    pq.add(list[i])
                }
            }

            while (pq.size > 0) {
                val a = pq.toTypedArray()
                val detections = pq.toTypedArray()
                val max = detections[0]
                nmsList.add(max)
                pq.clear()

                for (j in 1 until detections.size) {
                    val detection = detections[j]
                    val b: RectF = detection.location!!
                    if (box_iou(max.location!!, b) < mNmsThresh) {
                        pq.add(detection)
                    }
                }
            }
        }
        return nmsList
    }

    private val mNmsThresh = 0.6f

    private fun box_iou(a: RectF, b: RectF): Float {
        return box_intersection(a, b) / box_union(a, b)
    }

    private fun box_intersection(a: RectF, b: RectF): Float {
        val w: Float = overlap((a.left + a.right) / 2, a.right - a.left, (b.left + b.right) / 2, b.right - b.left)
        val h: Float = overlap((a.top + a.bottom) / 2, a.bottom - a.top, (b.top + b.bottom) / 2, b.bottom - b.top)
        return if (w < 0 || h < 0) 0f else w * h
    }

    private fun box_union(a: RectF, b: RectF): Float {
        val i: Float = box_intersection(a, b)
        val u: Float =
            (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i
        return u
    }

    private fun overlap(x1: Float, w1: Float, x2: Float, w2: Float): Float {
        val l1: Float = x1 - w1 / 2
        val l2: Float = x2 - w2 / 2
        val left: Float = if (l1 > l2) l1 else l2
        val r1: Float = x1 + w1 / 2
        val r2: Float = x2 + w2 / 2
        val right: Float = if (r1 < r2) r1 else r2
        return right - left
    }
}
