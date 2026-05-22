package com.eight87.shutterboy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.lifecycleScope
import com.eight87.shutterboy.data.settings.BaseTheme
import com.eight87.shutterboy.data.settings.ThemeMode
import com.eight87.shutterboy.theme.ShutterboyTheme
import com.eight87.shutterboy.ui.nav.ShutterboyApp
import com.eight87.shutterboy.ui.permission.RequireMediaPermission
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val graph = (application as ShutterboyApplication).graph
        routeExternalOpen(intent, graph)
    }

    private fun routeExternalOpen(intent: Intent?, graph: AppGraph) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        val mime = intent.type ?: contentResolver.getType(uri)
        lifecycleScope.launch {
            graph.externalOpenIntent.emit(uri.toString() to mime)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val graph = (application as ShutterboyApplication).graph

        // External VIEW intent on cold-start. onNewIntent handles the
        // warm-instance case.
        routeExternalOpen(intent, graph)

        setContent {
            val baseTheme by graph.themePreferences.observeBaseTheme()
                .collectAsStateWithLifecycle(initialValue = BaseTheme.Default)
            val themeMode by graph.themePreferences.observeThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.Default)
            val tintColor by graph.themePreferences.observeTintColor()
                .collectAsStateWithLifecycle(initialValue = null)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.System -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            ShutterboyTheme(
                darkTheme = darkTheme,
                baseTheme = baseTheme,
                tintColor = tintColor,
            ) {
                RequireMediaPermission(
                    onGranted = {
                        // `lifecycleScope` cancels on activity-destroy so a rotation
                        // mid-scan doesn't leak the coroutine. The single-flight
                        // mutex in `RoomGalleryRepository.executeScan` (R.F.25) is
                        // the companion fix that prevents the rotation-then-resume
                        // case from spawning a second concurrent scan.
                        //
                        // `scanIfChanged` (not `forceRescan`) — the MediaStore
                        // generation token + SAF fingerprint gate short-circuits
                        // in <10 ms when nothing changed since the last successful
                        // scan, so cold-launch is free in the common case. The
                        // explicit "Rescan" button in Settings is the only path
                        // that should bypass the gate.
                        lifecycleScope.launch { graph.libraryScanner.scanIfChanged() }
                    },
                ) {
                    ShutterboyApp(graph = graph)
                }
            }
        }
    }
}
