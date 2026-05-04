package com.eight87.shutterboy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.eight87.shutterboy.theme.MyApplicationTheme

/**
 * Phase B — placeholder body. The data layer is shipping under the hood; the UI
 * lands in Phase C (bottom-nav scaffold + Photos timeline). Until then,
 * MainActivity renders a single centered app-name surface so cold-boot stays
 * green.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Phase 0.6 — splash before super.onCreate so the launcher animation
        // hands off into Theme.SplashScreen cleanly.
        installSplashScreen()

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = stringResource(id = R.string.app_name))
                    }
                }
            }
        }
    }
}
