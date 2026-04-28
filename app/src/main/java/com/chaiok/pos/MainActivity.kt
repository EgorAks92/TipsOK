package com.chaiok.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.chaiok.pos.app.ChaiOkApp
import com.chaiok.pos.presentation.navigation.ChaiOkNavHost
import com.chaiok.pos.presentation.theme.ChaiOkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        val container = (application as ChaiOkApp).container
        setContent {
            ChaiOkTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ChaiOkNavHost(container)
                }
            }
        }
    }
}
