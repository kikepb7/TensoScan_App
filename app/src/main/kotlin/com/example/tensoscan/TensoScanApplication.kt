package com.example.tensoscan

import android.app.Application
import com.example.tensoscan.di.initKoin

class TensoScanApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}