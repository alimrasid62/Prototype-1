package com.alimrasid.prototype1.env

import android.graphics.Bitmap
import android.text.TextUtils
import java.io.Serializable

data class Size(val width: Int, val height: Int) : Comparable<Size>, Serializable {

    companion object {
        private const val serialVersionUID: Long = 7689808733290872361L

        fun getRotatedSize(size: Size, rotation: Int): Size {
            return if (rotation % 180 != 0) {
                // The phone is portrait, therefore the camera is sideways and frame should be rotated.
                Size(size.height, size.width)
            } else {
                size
            }
        }

        fun parseFromString(sizeString: String?): Size? {
            if (TextUtils.isEmpty(sizeString)) {
                return null
            }

            val trimmedSizeString = sizeString!!.trim()

            // The expected format is "<width>x<height>".
            val components = trimmedSizeString.split("x")
            if (components.size == 2) {
                try {
                    val width = components[0].toInt()
                    val height = components[1].toInt()
                    return Size(width, height)
                } catch (e: NumberFormatException) {
                    return null
                }
            } else {
                return null
            }
        }

        fun sizeStringToList(sizes: String?): List<Size> {
            val sizeList = mutableListOf<Size>()
            if (!sizes.isNullOrBlank()) {
                val pairs = sizes!!.split(",")
                for (pair in pairs) {
                    val size = parseFromString(pair)
                    size?.let {
                        sizeList.add(it)
                    }
                }
            }
            return sizeList
        }

        fun sizeListToString(sizes: List<Size>): String {
            return sizes.joinToString(",") { it.toString() }
        }

        fun dimensionsAsString(width: Int, height: Int): String {
            return "$width$height"
        }
    }

    fun aspectRatio(): Float {
        return width.toFloat() / height.toFloat()
    }

    override fun compareTo(other: Size): Int {
        return width * height - other.width * other.height
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Size) return false

        return width == other.width && height == other.height
    }

    override fun hashCode(): Int {
        return width * 32713 + height
    }

    override fun toString(): String {
        return dimensionsAsString(width, height)
    }
}
