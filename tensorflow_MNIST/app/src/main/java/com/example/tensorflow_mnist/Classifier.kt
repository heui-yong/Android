package com.example.tensorflow_mnist

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class Classifier(context: Context) {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    private val interpreter: Interpreter
    private var modelInputWidth = 0
    private var modelInputHeight = 0
    private var modelInputChannel = 0
    private var modelOutputClasses = 0

    init {
        val am = context.assets
        val afd = am.openFd("tensor_model.tflite")
        val fis = FileInputStream(afd.fileDescriptor)

        val model = fis.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
        model.order(ByteOrder.nativeOrder())

        interpreter = Interpreter(model)

        initModelShape()
    }

    //입력 및 출력 텐서 형상 초기화
    private fun initModelShape() {
        val inputTensor = interpreter.getInputTensor(0)
        val inputShape = inputTensor.shape()

        //입력 텐서의 형상 [1,28,28,1]
        modelInputChannel = inputShape[0]
        modelInputWidth = inputShape[1]
        modelInputHeight = inputShape[2]

        val outputTensor = interpreter.getOutputTensor(0)
        val outputShape = outputTensor.shape()

        //출력 텐서의 형상 [1,10]
        modelOutputClasses = outputShape[1]
    }

    fun classify(bitmap: Bitmap) : Pair<Int, Float> {
        val buffer = convertBitmapToGrayByteBuffer(resizeBitmap(bitmap))
        val result = arrayOf(FloatArray(modelOutputClasses))

        interpreter.run(buffer, result)

        return argmax(result[0])
    }

    //Bitmap 이미지를 TensorFlow Lite 모델의 입력 텐서 크기에 맞게 리사이징
    private fun resizeBitmap(bitmap: Bitmap): Bitmap =
        Bitmap.createScaledBitmap(bitmap, modelInputWidth, modelInputHeight, false)

    private fun convertBitmapToGrayByteBuffer(bitmap: Bitmap) : ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(bitmap.byteCount)
        byteBuffer.order(ByteOrder.nativeOrder())

        //Bitmap의 모든 픽셀 값을 pixels 배열에 로드
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            //RGB 값을 추출
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            //평균 픽셀 값 계산
            val avgPixelValue = (r + g + b) / 3.0f

            //평균 픽셀 값을 [0, 1] 범위로 정규화
            val normalizedPixelValue = avgPixelValue / 255.0f

            byteBuffer.putFloat(normalizedPixelValue)
        }

        return byteBuffer
    }

    private fun argmax(array: FloatArray): Pair<Int, Float> =
        array.withIndex().maxByOrNull { it.value }?.let { it.index to it.value }
            ?: error("Array is empty")
}