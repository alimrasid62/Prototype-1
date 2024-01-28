package com.alimrasid.prototype1.env

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    const val kMaxChannelValue = 262143

    @Suppress("unused")
    private val LOGGER = Logger()

    fun getYUVByteSize(width: Int, height: Int): Int {
        val ySize = width * height
        val uvSize = ((width + 1) / 2) * ((height + 1) / 2) * 2
        return ySize + uvSize
    }

    fun saveBitmap(bitmap: Bitmap) {
        saveBitmap(bitmap, "preview.png")
    }

    fun saveBitmap(bitmap: Bitmap, filename: String) {
        val root =
            Environment.getExternalStorageDirectory().absolutePath + File.separator + "tensorflow"
        LOGGER.i("Saving %dx%d bitmap to %s.", bitmap.width, bitmap.height, root)
        val myDir = File(root)

        if (!myDir.mkdirs()) {
            LOGGER.i("Make dir failed")
        }

        val fname = filename
        val file = File(myDir, fname)
        if (file.exists()) {
            file.delete()
        }
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 99, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            LOGGER.e(e, "Exception!")
        }
    }

    fun convertYUV420SPToARGB8888(input: ByteArray, width: Int, height: Int, output: IntArray) {
        val frameSize = width * height
        for (j in 0 until height) {
            var uvp = frameSize + j / 2 * width
            var u = 0
            var v = 0

            for (i in 0 until width) {
                val y = 0xff and input[j * width + i].toInt()
                if (i and 1 == 0) {
                    v = 0xff and input[uvp++].toInt()
                    u = 0xff and input[uvp++].toInt()
                }

                output[j * width + i] = YUV2RGB(y, u, v)
            }
        }
    }

    private fun YUV2RGB(y: Int, u: Int, v: Int): Int {
        var y = y
        var u = u
        var v = v

        y = if (y - 16 < 0) 0 else y - 16
        u -= 128
        v -= 128

        val y1192 = 1192 * y
        var r = y1192 + 1634 * v
        var g = y1192 - 833 * v - 400 * u
        var b = y1192 + 2066 * u

        r = if (r > kMaxChannelValue) kMaxChannelValue else if (r < 0) 0 else r
        g = if (g > kMaxChannelValue) kMaxChannelValue else if (g < 0) 0 else g
        b = if (b > kMaxChannelValue) kMaxChannelValue else if (b < 0) 0 else b

        return 0xff000000.toInt() or (r shl 6 and 0xff0000) or (g shr 2 and 0xff00) or (b shr 10 and 0xff)
    }

    fun convertYUV420ToARGB8888(
        yData: ByteArray,
        uData: ByteArray,
        vData: ByteArray,
        width: Int,
        height: Int,
        yRowStride: Int,
        uvRowStride: Int,
        uvPixelStride: Int,
        out: IntArray
    ) {
        var yp = 0
        for (j in 0 until height) {
            val pY = yRowStride * j
            val pUV = uvRowStride * (j / 2)

            for (i in 0 until width) {
                val uv_offset = pUV + i / 2 * uvPixelStride

                out[yp++] =
                    YUV2RGB(0xff and yData[pY + i].toInt(), 0xff and uData[uv_offset].toInt(), 0xff and vData[uv_offset].toInt())
            }
        }
    }

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
            if (applyRotation % 90 != 0) {
                LOGGER.w("Rotation of %d % 90 != 0", applyRotation)
            }
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f)

            matrix.postRotate(applyRotation.toFloat())
        }

        val transpose = (Math.abs(applyRotation) + 90) % 180 == 0
        val inWidth = if (transpose) srcHeight else srcWidth
        val inHeight = if (transpose) srcWidth else srcHeight

        if (inWidth != dstWidth || inHeight != dstHeight) {
            val scaleFactorX = dstWidth / inWidth.toFloat()
            val scaleFactorY = dstHeight / inHeight.toFloat()

            if (maintainAspectRatio) {

                val scaleFactor = scaleFactorX.coerceAtLeast(scaleFactorY)
                matrix.postScale(scaleFactor, scaleFactor)
            } else {
                matrix.postScale(scaleFactorX, scaleFactorY)
            }
        }

        if (applyRotation != 0) {
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f)
        }

        return matrix
    }
}

