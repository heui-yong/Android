package com.example.pytorch_yolo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import androidx.camera.core.ImageProxy
import com.example.pytorch_yolo.databinding.ActivityObjectDetectionBinding
import com.example.pytorch_yolo.processor.PrePostProcessor
import com.example.pytorch_yolo.processor.Result
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.ByteArrayOutputStream
import java.io.IOException


class ObjectDetectionActivity(
    override val contentViewLayoutId: Int =  R.layout.activity_object_detection,
) : AbstractCameraXActivity<ObjectDetectionActivity.AnalysisResult>() {
    private val binding: ActivityObjectDetectionBinding by lazy {
        ActivityObjectDetectionBinding.inflate(layoutInflater)
    }
    private var mModule: Module? = null
    private var mainActivity = MainActivity()

    inner class AnalysisResult(results: ArrayList<Result>) {
        val mResults: ArrayList<Result>

        init {
            mResults = results
        }
    }

    override fun getCameraPreviewTextureView(): TextureView {
        return binding.objectDetectionTextureViewStub
            .inflate()
            .findViewById(R.id.object_detection_texture_view)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

    override fun applyToUiAnalyzeImageResult(result: AnalysisResult) {
        binding.resultView.setResults(result.mResults, prePostProcessor)
        binding.resultView.invalidate()
    }

    private fun imgToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]
        uBuffer[nv21, ySize + vSize, uSize]
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    override fun analyzeImage(image: ImageProxy, rotationDegrees: Int): AnalysisResult? {
        try {
            if (mModule == null) {
                mModule = LiteModuleLoader.load(
                    mainActivity.assetFilePath(
                        applicationContext,
                        "yolov5s.torchscript.ptl"
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("Object Detection", "Error reading assets", e)
            return null
        }
        var bitmap: Bitmap = imgToBitmap(image.image!!)
        val matrix = Matrix()
        matrix.postRotate(90.0f)
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            PrePostProcessor.mInputWidth,
            PrePostProcessor.mInputHeight,
            true
        )

        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            resizedBitmap,
            PrePostProcessor.NO_MEAN_RGB,
            PrePostProcessor.NO_STD_RGB
        )
        val outputTuple = mModule!!.forward(IValue.from(inputTensor)).toTuple()
        val outputTensor = outputTuple[0].toTensor()
        val outputs = outputTensor.dataAsFloatArray

        val imgScaleX: Float = bitmap.width.toFloat() / PrePostProcessor.mInputWidth
        val imgScaleY: Float = bitmap.height.toFloat() / PrePostProcessor.mInputHeight
        val ivScaleX = binding.resultView.width.toFloat() / bitmap.width
        val ivScaleY = binding.resultView.height.toFloat() / bitmap.height

        val results: java.util.ArrayList<Result> = prePostProcessor.outputsToNMSPredictions(
            outputs,
            imgScaleX,
            imgScaleY,
            ivScaleX,
            ivScaleY,
            0F,
            0F
        )
        return AnalysisResult(results)
    }
}