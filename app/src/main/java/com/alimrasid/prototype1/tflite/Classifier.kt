package com.alimrasid.prototype1.tflite

import android.graphics.Bitmap
import android.graphics.RectF

interface Classifier {
    fun recognizeImage(bitmap: Bitmap): List<Recognition>

    fun enableStatLogging(debug: Boolean)

    fun getStatString(): String

    fun close()

    fun setNumThreads(numThreads: Int)

    fun setUseNNAPI(isChecked: Boolean)

    val objThresh: Float

    /**
     * An immutable result returned by a Classifier describing what was recognized.
     */
    data class Recognition(
        /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        val id: String,
        /**
         * Display name for the recognition.
         */
        val title: String,
        /**
         * A sortable score for how good the recognition is relative to others. Higher should be better.
         */
        val confidence: Float?,
        /**
         * Optional location within the source image for the location of the recognized object.
         */
        var location: RectF?,
        val detectedClass: Int
    ) {
        override fun toString(): String {
            var resultString = ""
            if (id != null) {
                resultString += "[$id] "
            }

            if (title != null) {
                resultString += "$title "
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f)
            }

            if (location != null) {
                resultString += "$location "
            }

            return resultString.trim()
        }
    }
}

