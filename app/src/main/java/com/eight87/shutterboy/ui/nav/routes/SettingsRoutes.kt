package com.eight87.shutterboy.ui.nav.routes

import androidx.compose.runtime.Composable
import com.eight87.shutterboy.ui.nav.Licenses
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.Settings
import com.eight87.shutterboy.ui.nav.SettingsAbout
import com.eight87.shutterboy.ui.nav.SettingsPhotos
import com.eight87.shutterboy.ui.settings.LicensesScreen
import com.eight87.shutterboy.ui.settings.SettingsAboutScreen
import com.eight87.shutterboy.ui.settings.SettingsPhotosScreen
import com.eight87.shutterboy.ui.settings.SettingsScreen

@Composable
fun Settings.Register(scope: RouteScope) {
    SettingsScreen(scope = scope)
}

@Composable
fun SettingsAbout.Register(scope: RouteScope) {
    SettingsAboutScreen(scope = scope)
}

@Composable
fun SettingsPhotos.Register(scope: RouteScope) {
    SettingsPhotosScreen(scope = scope)
}

@Composable
fun Licenses.Register(scope: RouteScope) {
    LicensesScreen(scope = scope)
}
