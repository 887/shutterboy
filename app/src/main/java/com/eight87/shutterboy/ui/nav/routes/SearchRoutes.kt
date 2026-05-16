package com.eight87.shutterboy.ui.nav.routes

import androidx.compose.runtime.Composable
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.Search
import com.eight87.shutterboy.ui.search.SearchScreen

/**
 * Phase G — per-destination Register extension for the Search surface.
 * Adding the Search destination is one new file with one new `Register`
 * extension; the navigation dispatcher in `ShutterboyApp` is closed
 * against modification (R.E pattern).
 */
@Composable
fun Search.Register(scope: RouteScope) {
    SearchScreen(scope = scope)
}
