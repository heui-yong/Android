package com.example.pytorch_yolo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.TextureView
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.camera.core.CameraX
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysisConfig
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.pytorch.LiteModuleLoader
import java.io.IOException

abstract class AbstractCameraXActivity<R> : BaseModuleActivity() {

    private var lastAnalysisResultTime: Long = 0

    protected abstract val contentViewLayoutId: Int

    protected abstract fun getCameraPreviewTextureView(): TextureView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentViewLayoutId)

        startBackgroundThread()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_CAMERA_PERMISSION)
        } else {
            setupCameraX()
        }

        try {
            val classes = assets.open("classes.txt").bufferedReader().readLines()
            prePostProcessor.mClasses = classes.toTypedArray()

        } catch (e: IOException) {
            Log.e("Object Detection", "Error reading assets", e)
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "You can't use object detection example without granting CAMERA permission", Toast.LENGTH_LONG).show()
                finish()
            } else {
                setupCameraX()
            }
        }
    }

    private fun setupCameraX() {
        val textureView = getCameraPreviewTextureView()
        val previewConfig = PreviewConfig.Builder().build()
        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener { output -> textureView.setSurfaceTexture(output.surfaceTexture) }

        val imageAnalysisConfig = mBackgroundHandler?.let {
            ImageAnalysisConfig.Builder()
                .setTargetResolution(Size(480, 640))
                .setCallbackHandler(it)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .build()
        }
        val imageAnalysis = ImageAnalysis(imageAnalysisConfig)
        imageAnalysis.setAnalyzer { image, rotationDegrees ->
            if (SystemClock.elapsedRealtime() - lastAnalysisResultTime < 500) {
                return@setAnalyzer
            }

            val result = analyzeImage(image, rotationDegrees)
            if (result != null) {
                lastAnalysisResultTime = SystemClock.elapsedRealtime()
                runOnUiThread { applyToUiAnalyzeImageResult(result) }
            }
        }

        CameraX.bindToLifecycle(this, preview, imageAnalysis)
    }

    @WorkerThread
    protected abstract fun analyzeImage(image: ImageProxy, rotationDegrees: Int): R?

    @UiThread
    protected abstract fun applyToUiAnalyzeImageResult(result: R)

    companion object {
        private const val REQUEST_CODE_CAMERA_PERMISSION = 200
    }
}