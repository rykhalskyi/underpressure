package com.example.underpressure

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class UnderPressureApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "OpenCV successfully loaded")
        } else {
            Log.e("OpenCV", "OpenCV load failed")
        }
    }
}
