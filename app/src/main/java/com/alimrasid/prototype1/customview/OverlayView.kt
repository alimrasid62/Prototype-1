package com.alimrasid.prototype1.customview

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val callbacks = mutableListOf<DrawCallback>()

    fun addCallback(callback: DrawCallback) {
        callbacks.add(callback)
    }

    @Synchronized
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        for (callback in callbacks) {
            callback.drawCallback(canvas)
        }
    }

    /** Interface defining the callback for client classes. */
    interface DrawCallback {
        fun drawCallback(canvas: Canvas)
    }
}
