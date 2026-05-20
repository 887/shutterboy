package com.eight87.shutterboy

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
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val graph = (application as ShutterboyApplication).graph

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
