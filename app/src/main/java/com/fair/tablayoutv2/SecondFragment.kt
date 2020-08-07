package com.fair.tablayoutv2

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_DCIM
import android.os.Environment.getExternalStoragePublicDirectory
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import com.fair.tablayoutv2.databinding.SecondFragmentBinding
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias LumaListener = (luma: Double) -> Unit

class SecondFragment : Fragment(R.layout.second_fragment)  {

    private var _binding: SecondFragmentBinding? = null
    private val viewBinding get() = _binding!!


    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var preview: Preview? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(permission.CAMERA)
        private const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = SecondFragmentBinding.bind(view)
        outputDirectory = MainActivity.getOutputDirectory(requireContext())
        cameraExecutor = Executors.newSingleThreadExecutor()

        viewBinding.apply {

            startCamera()
            cameraCaptureBtn.setOnClickListener { takePhoto() }


        }


    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.createSurfaceProvider())
                }
            imageCapture = ImageCapture.Builder().build()

            imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer {

                })
            }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture, imageAnalyzer,preview)

            } catch(exc: Exception) {

            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }
    private fun takePhoto() {

        val thisImageCapture = imageCapture?: return

        val photoFile = File (
            outputDirectory, SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis())+PHOTO_EXTENSION)

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(photoFile)
            .build()

        thisImageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(context, exception.toString(), Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val saveUri = Uri.fromFile(photoFile)
                    val msg = "Success: $saveUri"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private class LuminosityAnalyzer(listener: LumaListener? = null): ImageAnalysis.Analyzer {
        private val frameRateWindow = 8
        private val frameTimestamps = ArrayDeque<Long>(5)
        private val listeners = ArrayList<LumaListener>().apply { listener?.let { add(it) } }
        private var lastAnalyzedTimestamp = 0L
        var framesPerSecond: Double = -1.0
            private set

        fun onFrameAnalyzed(listener: LumaListener) = listeners.add(listener)

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()
            val data = ByteArray(remaining())
            get(data)
            return data
        }

        override fun analyze(image: ImageProxy) {
            if (listeners.isEmpty()){
                image.close()
                return
            }

            val currentTime = System.currentTimeMillis()
            frameTimestamps.push(currentTime)

            while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
            val timeStampFirst = frameTimestamps.peekFirst() ?: currentTime
            val timeStampLast = frameTimestamps.peekLast() ?: currentTime
            framesPerSecond = 1.0 / ((timeStampFirst - timeStampLast) /
                    frameTimestamps.size.coerceAtLeast(1).toDouble()) * 1000.0

            lastAnalyzedTimestamp = frameTimestamps.first

            val buffer = image.planes[0].buffer

            val data = buffer.toByteArray()

            val pixels = data.map { it.toInt() and 0XFF }

            val luma = pixels.average()

            listeners.forEach { it(luma) }

            image.close()

        }

    }

    /**
    private fun startCamera() {
        viewBinding.apply {
            val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
            val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
            val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

            val previewConfig = Preview.Builder().apply {
                setTargetResolution(screenSize)
                setTargetAspectRatio(screenAspectRatio)
                setTargetRotation(windowManager.defaultDisplay.rotation)
                setTargetRotation(viewFinder.display.rotation)
            }.build()

            val preview = Preview(previewConfig)
            preview.setOnPreviewOutputUpdateListener {
                viewFinder.surfaceTexture = it.surfaceTexture
                updateTransform()
            }


            // Create configuration object for the image capture use case
            val imageCaptureConfig = ImageCaptureConfig.Builder()
                .apply {
                    setLensFacing(lensFacing)
                    setTargetAspectRatio(screenAspectRatio)
                    setTargetRotation(texture.display.rotation)
                    setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                }.build()

            // Build the image capture use case and attach button click listener
            val imageCapture = ImageCapture(imageCaptureConfig)
            btn_take_picture.setOnClickListener {

                val file = File(
                    Environment.getExternalStorageDirectory().toString() +
                            "${MainActivity.folderPath}${System.currentTimeMillis()}.jpg"
                )

                imageCapture.takePicture(file,
                    object : ImageCapture.OnImageSavedListener {
                        override fun onError(
                            error: ImageCapture.UseCaseError,
                            message: String, exc: Throwable?
                        ) {
                            val msg = "Photo capture failed: $message"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()

                        }

                        override fun onImageSaved(file: File) {
                            val msg = "Photo capture successfully: ${file.absolutePath}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        }
                    })

            }

            CameraX.bindToLifecycle(this, preview, imageCapture)
        }
    }

    private fun updateTransform() {

        viewBinding.apply {

            val matrix = Matrix()
            val centerX = viewFinder.width / 2f
            val centerY = viewFinder.height / 2f

            val rotationDegrees = when (viewFinder.display.rotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> return
            }
            matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)
            viewFinder.transformMatrixToLocal(matrix)
        }

    }

    **/
    /**
    private fun takePhoto() {}
    private fun hasBackCamera():Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)?: false
    }
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)?: false
    }
    private fun updateCameraSwitchButton () {}
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()

            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw  IllegalStateException("Back and front camera are unavailable")
            } 

            updateCameraSwitchButton()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun aspectRatio(width: Int, height: Int):Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if(abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return  AspectRatio.RATIO_4_3
        }
        return  AspectRatio.RATIO_16_9
    }
    
    private fun cameraUseCases() {
        val metrics = DisplayMetrics().also { viewBinding.viewFinder.display.getRealMetrics(it)}
        Toast.makeText(context, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}", Toast.LENGTH_SHORT).show()

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)

        val rotation = viewBinding.viewFinder.display.rotation

        val cameraProvider = cameraProvider?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->


                })
            }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
            preview?.setSurfaceProvider(viewBinding.viewFinder.createSurfaceProvider())

        } catch (e: Exception) {

        }

    }

    private class LuminosityAnalyzer(listener: LumaListener? = null): ImageAnalysis.Analyzer {
        private val frameRateWindow = 8
        private val frameTimestamps = ArrayDeque<Long>(5)
        private val listeners = ArrayList<LumaListener>().apply { listener?.let { add(it) } }
        private var lastAnalyzedTimestamp = 0L
        var framesPerSecond: Double = -1.0
            private set

        fun onFrameAnalyzed(listener: LumaListener) = listeners.add(listener)

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()
            val data = ByteArray(remaining())
            get(data)
            return data
        }

        override fun analyze(image: ImageProxy) {
            if (listeners.isEmpty()){
                image.close()
                return
            }

            val currentTime = System.currentTimeMillis()
            frameTimestamps.push(currentTime)

            while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
            val timeStampFirst = frameTimestamps.peekFirst() ?: currentTime
            val timeStampLast = frameTimestamps.peekLast() ?: currentTime
            framesPerSecond = 1.0 / ((timeStampFirst - timeStampLast) /
                    frameTimestamps.size.coerceAtLeast(1).toDouble()) * 1000.0

            lastAnalyzedTimestamp = frameTimestamps.first

            val buffer = image.planes[0].buffer

            val data = buffer.toByteArray()

            val pixels = data.map { it.toInt() and 0XFF }

            val luma = pixels.average()

            listeners.forEach { it(luma) }

            image.close()

        }

    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        checkSelfPermission(context as Context, it) == PERMISSION_GRANTED
    }

    /**
    private fun getOutputDirectory(baseFolder: File, format: String, extension: String) =
        File(baseFolder, SimpleDateFormat(format, Locale.US)
            .format(System.currentTimeMillis()) + extension)

    */
    /**
     *
     *     private fun getOutputDirectory(): File {
    val mediaDir = externalMediaDirs.firstOrNull()?.let{
    File(it, resources.getString(R.string.app_name)).apply {
    mkdirs() }}
    return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir

    }
     *
     * */

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
*/
    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null

    }
}