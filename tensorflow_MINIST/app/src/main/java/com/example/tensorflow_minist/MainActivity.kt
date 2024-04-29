package com.example.tensorflow_minist

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tensorflow_minist.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val classifier: Classifier by lazy {
        Classifier(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()
    }

    private fun initView() {
        setDrawView()
        setBtnClassify()
        setBtnClear()
    }

    private fun setDrawView() {
        binding.drawView.apply {
            setStrokeWidth(40.0f)
            setBackgroundColor(Color.BLACK)
            setColor(Color.WHITE)
        }
    }

    private fun setBtnClassify() {
        binding.btnClassify.setOnClickListener {
//            val bitmap = BitmapFactory.decodeStream(assets.open("test1.png"))
            val bitmap = binding.drawView.getBitmap()

            val res = classifier.classify(bitmap)
            val outStr = String.format(Locale.ENGLISH, "예측 숫자 : %d, \n 맞을 확률 : %.0f%%", res.first, res.second * 100.0f)
            binding.textView.text = outStr
        }
    }

    private fun setBtnClear() {
        binding.btnClear.setOnClickListener {
            binding.drawView.clearCanvas()
        }
    }
}