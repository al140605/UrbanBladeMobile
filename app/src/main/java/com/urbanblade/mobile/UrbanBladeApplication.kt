package com.urbanblade.mobile

import android.app.Application
import com.urbanblade.mobile.core.network.AppContainer

class UrbanBladeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
