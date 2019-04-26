package com.damianf.paintmobile.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel(){
    fun savePaths(bitmap: Bitmap) {
        drawing.value = bitmap
    }

    val drawing = MutableLiveData<Bitmap>()
    val color = MutableLiveData<Int>()
    val stroke = MutableLiveData<Float>()
}