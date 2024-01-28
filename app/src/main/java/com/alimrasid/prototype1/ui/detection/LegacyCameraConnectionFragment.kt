package com.alimrasid.prototype1.ui.detection

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Bundle
import android.os.HandlerThread
import android.util.Size
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.alimrasid.prototype1.R
import com.alimrasid.prototype1.customview.AutoFitTextureView
import com.alimrasid.prototype1.env.ImageUtils
import com.alimrasid.prototype1.env.Logger
import java.io.IOException

class LegacyCameraConnectionFragment(
    private val imageListener: CameraActivity,
    private val layout: Int,
    private val desiredSize: Size
) : Fragment() {

    private val LOGGER = Logger()
    private val ORIENTATIONS = SparseIntArray()

    private var camera: Camera? = null
    private lateinit var textureView: AutoFitTextureView
    private var backgroundThread: HandlerThread? = null

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            val index = getCameraId()
            camera = Camera.open(index)

            try {
                val parameters = camera!!.parameters
                val focusModes: MutableList<String>? = parameters.supportedFocusModes
                if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                }
                val cameraSizes: MutableList<Camera.Size>? = parameters.supportedPreviewSizes
                val sizes = cameraSizes?.let { arrayOfNulls<Size>(it.size) }
                var i = 0
                if (cameraSizes != null) {
                    for (size in cameraSizes) {
                        sizes?.set(i++, Size(size.width, size.height))
                    }
                }
                val previewSize = sizes?.let {
                    CameraConnectionFragment.chooseOptimalSize(
                        it.requireNoNulls(), desiredSize.width, desiredSize.height
                    )
                }
                if (previewSize != null) {
                    parameters.setPreviewSize(previewSize.width, previewSize.height)
                }
                camera!!.setDisplayOrientation(90)
                camera!!.parameters = parameters
                camera!!.setPreviewTexture(texture)
            } catch (exception: IOException) {
                camera!!.release()
            }

            camera!!.setPreviewCallbackWithBuffer(imageListener)
            val s = camera!!.parameters.previewSize
            camera!!.addCallbackBuffer(ByteArray(ImageUtils.getYUVByteSize(s.height, s.width)))

            textureView.setAspectRatio(s.height, s.width)

            camera!!.startPreview()
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {}

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textureView = view.findViewById(R.id.texture)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        if (textureView.isAvailable) {
            camera?.startPreview()
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        stopCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread?.start()
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
        } catch (e: InterruptedException) {
            LOGGER.e(e, "Exception!")
        }
    }

    private fun stopCamera() {
        camera?.let {
            it.stopPreview()
            it.setPreviewCallback(null)
            it.release()
            camera = null
        }
    }

    private fun getCameraId(): Int {
        val ci = CameraInfo()
        for (i in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(i, ci)
            if (ci.facing == CameraInfo.CAMERA_FACING_BACK) return i
        }
        return -1
    }
}

