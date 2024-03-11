package com.example.android_hilt_study

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.example.android_hilt_study.databinding.ActivityMainBinding
import com.example.android_hilt_study.viewmodel.InfoViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: InfoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setContentView(binding.root)

        initView()
        initObserve()
    }

    private fun initView() {
        binding.layoutInfoEdit.viewModel = viewModel
        viewModel.setInfoData()
    }

    private fun initObserve() {
        viewModel.infoData.observe(this) {
            binding.apply {
                layoutInfoEdit.data = it
                name = it.name
            }
        }
    }
}