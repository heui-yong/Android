package com.example.pytorch_yolo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pytorch_yolo.databinding.ActivityMainBinding
import com.example.pytorch_yolo.processor.PrePostProcessor
import com.example.pytorch_yolo.processor.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : BaseModuleActivity(), CoroutineScope by MainScope() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val mTestImages = arrayOf("test1.png", "test2.jpg", "test3.png","test4.png")
    private var mImgScaleX = 0f
    private var mImgScaleY:Float = 0f
    private var mIvScaleX:Float = 0f
    private var mIvScaleY:Float = 0f
    private var mStartX:Float = 0f
    private var mStartY:Float = 0f
    private var mBitmap: Bitmap? = null
    private var mImageIndex = 0

    private var mModule: Module? = null

    @Throws(IOException::class)
    fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        context.assets.open(assetName).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (`is`.read(buffer).also { read = it } != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }

        setContentView(binding.root)

        try {
            mBitmap = BitmapFactory.decodeStream(assets.open(mTestImages[mImageIndex]))
        } catch (e: IOException) {
            Log.e("Object Detection", "Error reading assets", e)
            finish()
        }

        binding.testButton.text = "Test Image 1/4"

        binding.testButton.setOnClickListener {
            binding.resultView.visibility = View.INVISIBLE
            mImageIndex = (mImageIndex + 1) % mTestImages.size
            binding.testButton.text =
                String.format("Text Image %d/%d", mImageIndex + 1, mTestImages.size)

            try {
                mBitmap = BitmapFactory.decodeStream(assets.open(mTestImages[mImageIndex]))
                binding.imageView.setImageBitmap(mBitmap)
            } catch (e: IOException) {
                Log.e("Object Detection", "Error reading assets", e)
                finish()
            }
        }

        binding.selectButton.setOnClickListener {
            binding.resultView.visibility = View.INVISIBLE

            val options = arrayOf<CharSequence>("Choose from Photos", "Take Picture", "Cancel")
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("New Test Image")

            builder.setItems(
                options
            ) { dialog, item ->
                if (options[item] == "Take Picture") {
                    val takePicture =
                        Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(takePicture, 0)
                } else if (options[item] == "Choose from Photos") {
                    val pickPhoto = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.INTERNAL_CONTENT_URI
                    )
                    startActivityForResult(pickPhoto, 1)
                } else if (options[item] == "Cancel") {
                    dialog.dismiss()
                }
            }
            builder.show()
        }

        binding.liveButton.setOnClickListener {
            val intent = Intent(
                this@MainActivity,
                ObjectDetectionActivity::class.java
            )
            startActivity(intent)
        }

        binding.detectButton.setOnClickListener {
            binding.detectButton.isEnabled = false
            binding.progressBar.visibility = ProgressBar.VISIBLE
            binding.detectButton.text = getString(R.string.run_model)

            mImgScaleX = mBitmap!!.width.toFloat() / PrePostProcessor.mInputWidth
            mImgScaleY = mBitmap!!.height.toFloat() / PrePostProcessor.mInputHeight

            mIvScaleX = if (mBitmap!!.width > mBitmap!!.height) binding.imageView.width
                .toFloat() / mBitmap!!.width else binding.imageView.height
                .toFloat() / mBitmap!!.height
            mIvScaleY = if (mBitmap!!.height > mBitmap!!.width) binding.imageView.height
                .toFloat() / mBitmap!!.height else binding.imageView.width.toFloat() / mBitmap!!.width

            mStartX = (binding.imageView.width - mIvScaleX * mBitmap!!.width) / 2
            mStartY = (binding.imageView.height - mIvScaleY * mBitmap!!.height) / 2

            launch {
                runModel()
            }
        }

        try {
            mModule = LiteModuleLoader.load(assetFilePath(applicationContext, "yolov5s.torchscript.ptl"))
            val classes = assets.open("classes.txt").bufferedReader().readLines()
            Log.e("TAG", "classes = $classes")
            prePostProcessor.mClasses = classes.toTypedArray()
//            PrePostProcessor().mClasses.addAll(classes)
            prePostProcessor.mClasses.forEach {
                Log.e("TAG", "prePostProcessor.mClasses = $it")
            }

        } catch (e: IOException) {
            Log.e("Object Detection", "Error reading assets", e)
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_CANCELED) {
            when (requestCode) {
                0 -> {
                    if (resultCode == RESULT_OK && data != null) {
                        val bitmap = data.extras?.get("data") as Bitmap
                        val matrix = Matrix()
                        matrix.postRotate(90.0f)
                        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        binding.imageView.setImageBitmap(rotatedBitmap)
                    }
                }
                1 -> {
                    if (resultCode == RESULT_OK && data != null) {
                        val selectedImage = data.data
                        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                        selectedImage?.let { uri ->
                            contentResolver.query(uri, filePathColumn, null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                                    val picturePath = cursor.getString(columnIndex)
                                    val bitmap = BitmapFactory.decodeFile(picturePath)
                                    val matrix = Matrix()
                                    matrix.postRotate(90.0f)
                                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                    binding.imageView.setImageBitmap(rotatedBitmap)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun runModel() {
        withContext(Dispatchers.Default) {
            val resizedBitmap = Bitmap.createScaledBitmap(
                mBitmap!!, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true
            )
            val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                resizedBitmap,
                PrePostProcessor.NO_MEAN_RGB,
                PrePostProcessor.NO_STD_RGB
            )
            val outputTuple = mModule!!.forward(IValue.from(inputTensor)).toTuple()
            val outputTensor = outputTuple[0].toTensor()
            val outputs = outputTensor.dataAsFloatArray
            val results: ArrayList<Result> = PrePostProcessor().outputsToNMSPredictions(
                outputs,
                mImgScaleX,
                mImgScaleY,
                mIvScaleX,
                mIvScaleY,
                mStartX,
                mStartY
            )

            withContext(Dispatchers.Main) {
                Log.e("TAG", "runOnUiThread")
                binding.detectButton.isEnabled = true
                binding.detectButton.text = getString(R.string.detect)
                binding.progressBar.visibility = ProgressBar.INVISIBLE
                binding.resultView.setResults(results, prePostProcessor)
                binding.resultView.invalidate()
                binding.resultView.visibility = View.VISIBLE
            }
        }
    }
}