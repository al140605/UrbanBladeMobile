package com.urbanblade.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.urbanblade.mobile.core.push.PushDeepLink
import com.urbanblade.mobile.ui.navigation.UrbanBladeRoot
import com.urbanblade.mobile.ui.theme.UrbanBladeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PushDeepLink.handle(intent)
        setContent {
            UrbanBladeTheme {
                UrbanBladeRoot()
            }
        }
    }

    /** Con la app ya abierta, tocar una notificación llega aquí (la actividad es singleTop vía CLEAR_TOP). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PushDeepLink.handle(intent)
    }
}
