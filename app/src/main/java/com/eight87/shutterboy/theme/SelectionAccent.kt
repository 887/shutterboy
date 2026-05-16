package com.eight87.shutterboy.theme

import androidx.compose.ui.graphics.Color

/**
 * Phase H + m3-expressive F.3 pattern 1 — pinned active-state accent for the
 * multi-select chrome.
 *
 * The selected-tile border + the selection top bar tint must NOT pull from
 * `MaterialTheme.colorScheme.primary`: a Custom-seed theme (or wallpaper-derived
 * dynamic palette) can land that primary on orange / pink, which clashes with
 * the photo content and weakens the "this tile is selected" read. Stable blue
 * keeps the affordance unambiguous regardless of theme.
 *
 * Mirrors the tonearmboy `7876789` fix where the queue-active-row hijack picked
 * `primaryContainer` and went orange under the user's seed.
 */
val SelectionAccent: Color = Color(0xFF1565C0)
