package com.urbanblade.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.urbanblade.mobile.ui.navigation.UrbanBladeRoot
import com.urbanblade.mobile.ui.theme.UrbanBladeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UrbanBladeTheme {
                UrbanBladeRoot()
            }
        }
    }
}
