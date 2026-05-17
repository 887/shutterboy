package com.eight87.shutterboy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eight87.shutterboy.data.settings.BaseTheme
import com.eight87.shutterboy.theme.ShutterboyTheme
import com.eight87.shutterboy.ui.nav.ShutterboyApp
import com.eight87.shutterboy.ui.permission.RequireMediaPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val graph = (application as ShutterboyApplication).graph
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        setContent {
            val baseTheme by graph.themePreferences.observeBaseTheme()
                .collectAsStateWithLifecycle(initialValue = BaseTheme.Default)
            ShutterboyTheme(baseTheme = baseTheme) {
                // Permission gate — first launch on a real device walks the
                // system grant flow for READ_MEDIA_IMAGES + READ_MEDIA_VIDEO.
                // Without this the library scan returns 0 rows silently and
                // the user is stuck on "No photos yet" forever. On grant,
                // kick a fresh full rescan immediately so the user doesn't
                // need to find Settings → Library → Rescan to populate.
                RequireMediaPermission(
                    onGranted = {
                        appScope.launch { graph.libraryScanner.forceRescan() }
                    },
                ) {
                    ShutterboyApp(graph = graph)
                }
            }
        }
    }
}
