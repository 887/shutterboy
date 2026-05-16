package com.eight87.shutterboy.ui.settings.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.eight87.shutterboy.theme.CategoryAccent

/**
 * m3-expressive Phase D — the coloured circular row-icon avatar that
 * gives Settings rows their per-category identity. 40-dp circle in
 * [CategoryAccent.container], centred 24-dp filled icon tinted
 * [CategoryAccent.onContainer]. Pair with `Icons.Filled.*` glyphs;
 * outlined glyphs read weak inside a coloured circle.
 */
@Composable
fun CategoryAvatar(
    icon: ImageVector,
    accent: CategoryAccent,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(accent.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = accent.onContainer,
            modifier = Modifier.size(24.dp),
        )
    }
}
