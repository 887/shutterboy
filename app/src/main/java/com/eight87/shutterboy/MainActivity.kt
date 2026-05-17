package com.eight87.shutterboy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.eight87.shutterboy.data.settings.BaseTheme
import com.eight87.shutterboy.theme.ShutterboyTheme
import com.eight87.shutterboy.ui.nav.ShutterboyApp
import com.eight87.shutterboy.ui.permission.RequireMediaPermission
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val graph = (application as ShutterboyApplication).graph

        setContent {
            val baseTheme by graph.themePreferences.observeBaseTheme()
                .collectAsStateWithLifecycle(initialValue = BaseTheme.Default)
            ShutterboyTheme(baseTheme = baseTheme) {
                RequireMediaPermission(
                    onGranted = {
                        // R.F.24 — `lifecycleScope` cancels on activity-destroy so a
                        // rotation mid-scan doesn't leak the coroutine. The single-flight
                        // mutex in `RoomGalleryRepository.executeScan` (R.F.25) is the
                        // companion fix that prevents the rotation-then-resume case
                        // from spawning a second concurrent scan.
                        lifecycleScope.launch { graph.libraryScanner.forceRescan() }
                    },
                ) {
                    ShutterboyApp(graph = graph)
                }
            }
        }
    }
}
