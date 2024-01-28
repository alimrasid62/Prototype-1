package com.alimrasid.prototype1.ui.detection

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.Camera
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alimrasid.prototype1.R
import com.alimrasid.prototype1.env.ImageUtils
import com.alimrasid.prototype1.env.Logger
import com.alimrasid.prototype1.ui.MainActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.switchmaterial.SwitchMaterial
import java.nio.ByteBuffer


abstract class CameraActivity : AppCompatActivity(), CompoundButton.OnCheckedChangeListener, View.OnClickListener {
    private val LOGGER = Logger()

    private val PERMISSIONS_REQUEST = 1

    private val PERMISSION_CAMERA = Manifest.permission.CAMERA
    protected var previewWidth = 0
    protected var previewHeight = 0
    private var debug = false
    private lateinit var handler: Handler
    private lateinit var handlerThread: HandlerThread
    private var useCamera2API = false
    private var isProcessingFrame = false
    private var yuvBytes = arrayOfNulls<ByteArray>(3)
    private var rgbBytes: IntArray? = null
    private var yRowStride = 0
    private lateinit var postInferenceCallback: Runnable
    private lateinit var imageConverter: Runnable

    private lateinit var bottomSheetLayout: LinearLayout
    private lateinit var gestureLayout: LinearLayout
    private lateinit var sheetBehavior: BottomSheetBehavior<LinearLayout>

    protected lateinit var frameValueTextView: TextView
    protected lateinit var cropValueTextView: TextView
    protected lateinit var inferenceTimeTextView: TextView
    protected lateinit var bottomSheetArrowImageView: ImageView
    private lateinit var plusImageView: ImageView
    private lateinit var minusImageView: ImageView
    private lateinit var apiSwitchCompat: SwitchMaterial
    private lateinit var modelSwitchCompat: SwitchMaterial
    private lateinit var threadsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        LOGGER.d("onCreate $this")
        super.onCreate(null)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.tfe_od_activity_camera)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        if (hasPermission()) {
            setFragment()
        } else {
            requestPermission()
        }

        threadsTextView = findViewById(R.id.threads)
        plusImageView = findViewById(R.id.plus)
        minusImageView = findViewById(R.id.minus)
        apiSwitchCompat = findViewById(R.id.api_info_switch)
        modelSwitchCompat = findViewById(R.id.model_switch)
        bottomSheetLayout = findViewById(R.id.bottom_sheet_layout)
        gestureLayout = findViewById(R.id.gesture_layout)
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
        bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow)

        modelSwitchCompat.setOnCheckedChangeListener { _, isChecked ->
            setUseKata(isChecked)
            Toast.makeText(this@CameraActivity, "Deketksi per kata aktif!", Toast.LENGTH_SHORT).show()
        }

        val vto = gestureLayout.viewTreeObserver
        vto.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        gestureLayout.viewTreeObserver.removeGlobalOnLayoutListener(this)
                    } else {
                        gestureLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                    val height = gestureLayout.measuredHeight

                    sheetBehavior.peekHeight = height
                }
            })
        sheetBehavior.isHideable = false

        sheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> {
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        bottomSheetArrowImageView.setImageResource(R.drawable.ic_arrow_down)
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        bottomSheetArrowImageView.setImageResource(R.drawable.ic_arrow_up)
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {
                    }
                    BottomSheetBehavior.STATE_SETTLING -> bottomSheetArrowImageView.setImageResource(R.drawable.ic_arrow_up)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        frameValueTextView = findViewById(R.id.frame_info)
        cropValueTextView = findViewById(R.id.crop_info)
        inferenceTimeTextView = findViewById(R.id.inference_info)

        apiSwitchCompat.setOnCheckedChangeListener(this)

        plusImageView.setOnClickListener(this)
        minusImageView.setOnClickListener(this)
        val btnBack: ImageButton = findViewById(R.id.btnBack)
        btnBack.setOnClickListener(this)
    }

    protected fun getRgbBytes(): IntArray? {
        imageConverter.run()
        return rgbBytes
    }

    protected fun getLuminanceStride(): Int {
        return yRowStride
    }

    protected fun getLuminance(): ByteArray? {
        return yuvBytes[0]
    }

    /** Callback for android.hardware.Camera API */
    fun onPreviewFrame(bytes: ByteArray, camera: Camera) {
        if (isProcessingFrame) {
            LOGGER.w("Dropping frame!")
            return
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                val parameters = camera.parameters
                val previewSize = parameters.previewSize
                previewHeight = previewSize.height
                previewWidth = previewSize.width
                rgbBytes = IntArray(previewWidth * previewHeight)
                onPreviewSizeChosen(Size(previewSize.width, previewSize.height), 90)
            }
        } catch (e: Exception) {
            LOGGER.e(e, "Exception!")
            return
        }

        isProcessingFrame = true
        yuvBytes[0] = bytes
        yRowStride = previewWidth

        imageConverter = Runnable {
            ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes!!)
        }

        postInferenceCallback = Runnable {
            camera.addCallbackBuffer(bytes)
            isProcessingFrame = false
        }
        processImage()
    }

    /** Callback for Camera2 API */
    fun onImageAvailable(reader: ImageReader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return
        }
        if (rgbBytes == null) {
            rgbBytes = IntArray(previewWidth * previewHeight)
        }
        try {
            val image: Image? = reader.acquireLatestImage()

            if (image == null) {
                return
            }

            if (isProcessingFrame) {
                image.close()
                return
            }
            isProcessingFrame = true
            val planes: Array<Image.Plane> = image.planes
            fillBytes(planes, yuvBytes)
            yRowStride = planes[0].rowStride
            val uvRowStride = planes[1].rowStride
            val uvPixelStride = planes[1].pixelStride

            imageConverter = Runnable {
                ImageUtils.convertYUV420ToARGB8888(
                    yuvBytes[0]!!,
                    yuvBytes[1]!!,
                    yuvBytes[2]!!,
                    previewWidth,
                    previewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    rgbBytes!!
                )
            }

            postInferenceCallback = Runnable {
                image.close()
                isProcessingFrame = false
            }

            processImage()
        } catch (e: Exception) {
            LOGGER.e(e, "Exception!")
            return
        }
    }

    override fun onStart() {
        LOGGER.d("onStart $this")
        super.onStart()
    }

    override fun onResume() {
        LOGGER.d("onResume $this")
        super.onResume()

        handlerThread = HandlerThread("inference")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    override fun onPause() {
        LOGGER.d("onPause $this")

        handlerThread.quitSafely()
        try {
            handlerThread.join()
            handlerThread = null
            handler = null
        } catch (e: InterruptedException) {
            LOGGER.e(e, "Exception!")
        }

        super.onPause()
    }

    override fun onStop() {
        LOGGER.d("onStop $this")
        super.onStop()
    }

    override fun onDestroy() {
        LOGGER.d("onDestroy $this")
        super.onDestroy()
    }

    protected fun runInBackground(r: Runnable) {
        handler.post(r)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                setFragment()
            } else {
                requestPermission()
            }
        }
    }

    private fun allPermissionsGranted(grantResults: IntArray): Boolean {
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_CAMERA)) {
            Toast.makeText(
                this@CameraActivity,
                "Camera permission is required for this demo",
                Toast.LENGTH_LONG
            ).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(PERMISSION_CAMERA), PERMISSIONS_REQUEST)
    }

    // Returns true if the device supports the required hardware level, or better.
    private fun isHardwareLevelSupported(characteristics: CameraCharacteristics, requiredLevel: Int): Boolean {
        val deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return requiredLevel <= deviceLevel!!
    }

    private fun chooseCamera(): String? {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // We don't use a front facing camera in this sample.
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }

                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

                if (map == null) {
                    continue
                }

                // Fallback to camera1 API for internal cameras that don't have full support.
                // This should help with legacy situations where using the camera2 API causes
                // distorted or otherwise broken previews.
                useCamera2API =
                    (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                            || isHardwareLevelSupported(
                        characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
                    )
                LOGGER.i("Camera API lv2?: $useCamera2API")
                return cameraId
            }
        } catch (e: CameraAccessException) {
            LOGGER.e(e, "Not allowed to access camera")
        }

        return null
    }

    protected fun setFragment() {
        val cameraId = chooseCamera()

        val fragment: androidx.fragment.app.Fragment
        if (useCamera2API) {
            val camera2Fragment = CameraConnectionFragment.newInstance(
                object : CameraConnectionFragment.ConnectionCallback {
//                    override fun onPreviewSizeChosen(size: Size, rotation: Int) {
//                        previewHeight = size.height
//                        previewWidth = size.width
//                        this@CameraActivity.onPreviewSizeChosen(size, rotation)
//                    }

                    override fun onPreviewSizeChosen(size: Size, cameraRotation: Int) {
                        previewHeight = size.height
                        previewWidth = size.width
                        this@CameraActivity.onPreviewSizeChosen(size, cameraRotation)
                    }
                },
                this,
                getLayoutId(),
                getDesiredPreviewFrameSize()
            )

            if (cameraId != null) {
                camera2Fragment.setCamera(cameraId)
            }
            fragment = camera2Fragment
        } else {
            fragment = LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize())
        }

        supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
    }

    protected fun fillBytes(planes: Array<Image.Plane>, yuvBytes: Array<ByteArray?>) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (i in planes.indices) {
            val buffer: ByteBuffer = planes[i].buffer
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer $i at size ${buffer.capacity()}")
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
            buffer.get(yuvBytes[i])
        }
    }

    fun isDebug(): Boolean {
        return debug
    }

    protected fun readyForNextImage() {
        postInferenceCallback.run()
    }

    protected fun getScreenOrientation(): Int {
        return when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_270 -> 270
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_90 -> 90
            else -> 0
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        setUseNNAPI(isChecked)
        if (isChecked) apiSwitchCompat.text = "NNAPI" else apiSwitchCompat.text = "TFLITE"
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.plus -> {
                val threads = threadsTextView.text.toString().trim()
                var numThreads = Integer.parseInt(threads)
                if (numThreads >= 9) return
                numThreads++
                threadsTextView.text = numThreads.toString()
                setNumThreads(numThreads)
            }
            R.id.minus -> {
                val threads = threadsTextView.text.toString().trim()
                var numThreads = Integer.parseInt(threads)
                if (numThreads == 1) {
                    return
                }
                numThreads--
                threadsTextView.text = numThreads.toString()
                setNumThreads(numThreads)
            }
            R.id.btnBack -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    protected fun showFrameInfo(frameInfo: String) {
        frameValueTextView.text = frameInfo
    }

    protected fun showCropInfo(cropInfo: String) {
        cropValueTextView.text = cropInfo
    }

    protected fun showInference(inferenceTime: String) {
        inferenceTimeTextView.text = inferenceTime
    }

    protected abstract fun processImage()

    protected abstract fun onPreviewSizeChosen(size: Size, rotation: Int)

    protected abstract fun getLayoutId(): Int

    protected abstract fun getDesiredPreviewFrameSize(): Size

    protected abstract fun setNumThreads(numThreads: Int)

    protected abstract fun setUseNNAPI(isChecked: Boolean)

    protected abstract fun setUseKata(isChecked: Boolean)
}
