package com.urbanblade.mobile

import android.app.Application
import com.urbanblade.mobile.core.analytics.UrbanAnalytics
import com.urbanblade.mobile.core.config.UrbanRemoteConfig
import com.urbanblade.mobile.core.network.AppContainer

class UrbanBladeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
        UrbanAnalytics.init(this)
        UrbanRemoteConfig.init(this)
    }
}
