package com.alimrasid.prototype1.env

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Environment
import android.util.Log
import android.content.res.AssetManager
import android.graphics.Canvas
import android.widget.Toast
import java.io.*
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object Utils {

    /**
     * Memory-map the model file in Assets.
     */
    @Throws(IOException::class)
    fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun softmax(vals: FloatArray) {
        var max = Float.NEGATIVE_INFINITY
        for (value in vals) {
            max = max.coerceAtLeast(value)
        }
        var sum = 0.0f
        for (i in vals.indices) {
            vals[i] = kotlin.math.exp(vals[i] - max)
            sum += vals[i]
        }
        for (i in vals.indices) {
            vals[i] = vals[i] / sum
        }
    }

    fun expit(x: Float): Float {
        return 1.0f / (1.0f + kotlin.math.exp(-x))
    }

    fun getBitmapFromAsset(context: Context, filePath: String): Bitmap? {
        val assetManager = context.assets
        var istr: InputStream? = null
        var bitmap: Bitmap? = null
        try {
            istr = assetManager.open(filePath)
            bitmap = BitmapFactory.decodeStream(istr)
        } catch (e: IOException) {
            // handle exception
            Log.e("getBitmapFromAsset", "getBitmapFromAsset: ${e.message}")
        } finally {
            try {
                istr?.close()
            } catch (e: IOException) {
                // handle exception
            }
        }
        return bitmap
    }

    /**
     * Returns a transformation matrix from one reference frame into another.
     * Handles cropping (if maintaining aspect ratio is desired) and rotation.
     *
     * @param srcWidth Width of source frame.
     * @param srcHeight Height of source frame.
     * @param dstWidth Width of destination frame.
     * @param dstHeight Height of destination frame.
     * @param applyRotation Amount of rotation to apply from one frame to another.
     * Must be a multiple of 90.
     * @param maintainAspectRatio If true, will ensure that scaling in x and y remains constant,
     * cropping the image if necessary.
     * @return The transformation fulfilling the desired requirements.
     */
    fun getTransformationMatrix(
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int,
        applyRotation: Int,
        maintainAspectRatio: Boolean
    ): Matrix {
        val matrix = Matrix()

        if (applyRotation != 0) {
            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f)

            // Rotate around origin.
            matrix.postRotate(applyRotation.toFloat())
        }

        // Account for the already applied rotation, if any, and then determine how
        // much scaling is needed for each axis.
        val transpose = (kotlin.math.abs(applyRotation) + 90) % 180 == 0
        val inWidth = if (transpose) srcHeight else srcWidth
        val inHeight = if (transpose) srcWidth else srcHeight

        // Apply scaling if necessary.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            val scaleFactorX = dstWidth / inWidth.toFloat()
            val scaleFactorY = dstHeight / inHeight.toFloat()

            if (maintainAspectRatio) {
                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                val scaleFactor = kotlin.math.max(scaleFactorX, scaleFactorY)
                matrix.postScale(scaleFactor, scaleFactor)
            } else {
                // Scale exactly to fill dst from src.
                matrix.postScale(scaleFactorX, scaleFactorY)
            }
        }

        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f)
        }

        return matrix
    }

    fun processBitmap(source: Bitmap, size: Int): Bitmap {
        val imageHeight = source.height
        val imageWidth = source.width
        val croppedBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val frameToCropTransformations =
            getTransformationMatrix(imageWidth, imageHeight, size, size, 0, false)
        val cropToFrameTransformations = Matrix()
        frameToCropTransformations.invert(cropToFrameTransformations)

        val canvas = Canvas(croppedBitmap)
        canvas.drawBitmap(source, frameToCropTransformations, null)

        return croppedBitmap
    }

    fun writeToFile(data: String, context: Context) {
        try {
            val baseDir = Environment.getExternalStorageDirectory().absolutePath
            val fileName = "myFile.txt"

            val file = File(baseDir + File.separator + fileName)

            val stream = FileOutputStream(file)
            try {
                stream.write(data.toByteArray())
            } finally {
                stream.close()
            }
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: ${e.toString()}")
        }
    }
}
