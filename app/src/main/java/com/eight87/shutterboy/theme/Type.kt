package com.eight87.shutterboy.theme

import androidx.compose.material3.Typography

// m3-expressive C.1 — defer to M3E's expressive scale entirely.
// MaterialExpressiveTheme overlays the expressive font sizes / weights /
// letter-spacing on top of this default Typography instance, so we don't
// need to hand-roll any style overrides at this stage.
val Typography = Typography()
