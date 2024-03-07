package com.example.android_hilt_study.provider

import javax.inject.Inject

class InfoProvider @Inject constructor() {
    fun addInfoData(data: Int): Int = data + 1
}