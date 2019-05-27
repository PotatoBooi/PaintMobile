package com.damianf.paintmobile.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.damianf.paintmobile.utils.DEFAULT_STROKE
import com.divyanshu.draw.widget.MyPath
import com.divyanshu.draw.widget.PaintOptions

class MainViewModel : ViewModel(){
    fun savePaths(paths: LinkedHashMap<MyPath,PaintOptions>) {
        drawing.value = paths
    }

    val drawing = MutableLiveData<LinkedHashMap<MyPath,PaintOptions>>()
    val color = MutableLiveData<Int>()
    val backgroudColor = MutableLiveData<Int>()
    val stroke = MutableLiveData<Float>().apply {
        if (value == null ) value = DEFAULT_STROKE
    }
}