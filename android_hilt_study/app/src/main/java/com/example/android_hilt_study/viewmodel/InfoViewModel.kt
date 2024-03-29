package com.example.android_hilt_study.viewmodel

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android_hilt_study.R
import com.example.android_hilt_study.data.Info
import com.example.android_hilt_study.provider.InfoProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor(
    private val infoProvider: InfoProvider
): ViewModel(){
    companion object {
        private val TAG = InfoViewModel::class.java.simpleName
    }

    private val _infoData =  MutableLiveData<Info>()
    val infoData: LiveData<Info> get() = _infoData

    fun setInfoData() {
        _infoData.postValue(Info("hee yong", 27, 72, 173))
    }

    fun subBtnClick(v: View) {
        Log.e(TAG, "subBtnClick")

        infoData.value?.let {
            when(v.id) {
                R.id.btn_age_sub -> updateAge(infoProvider.subInfoData(it.age))
                R.id.btn_weight_sub -> updateWeight(infoProvider.subInfoData(it.weight))
                R.id.btn_height_sub -> updateHeight(infoProvider.subInfoData(it.height))
                else -> 0
            }
        }
    }

    fun addBtnClick(v: View) {
        Log.e(TAG, "addBtnClick")

        infoData.value?.let {
            when(v.id) {
                R.id.btn_age_add -> updateAge(infoProvider.addInfoData(it.age))
                R.id.btn_weight_add -> updateWeight(infoProvider.addInfoData(it.weight))
                R.id.btn_height_add -> updateHeight(infoProvider.addInfoData(it.height))
                else -> 0
            }
        }
    }

    private fun updateAge(age: Int) {
        _infoData.value?.let { info ->
            _infoData.postValue(info.copy(age = age))
        }
    }

    private fun updateWeight(weight: Int) {
        _infoData.value?.let { info ->
            _infoData.postValue(info.copy(weight = weight))
        }
    }

    private fun updateHeight(height: Int) {
        _infoData.value?.let { info ->
            _infoData.postValue(info.copy(height = height))
        }
    }
}