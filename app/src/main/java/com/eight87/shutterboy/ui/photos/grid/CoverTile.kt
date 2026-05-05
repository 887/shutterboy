package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.eight87.shutterboy.domain.Photo

/**
 * Aggregate-tile used at [PhotosZoomLevel.Days] / `.Months` / `.Years`. Renders
 * the cover [photo] under a vertical gradient scrim with the label + photo
 * count stacked at the bottom-left. Aspect ratio is square at Days/Months and
 * 16:9 at Years (the hero variant, set by the caller via [aspectRatio]).
 */
@Composable
internal fun CoverTile(
    cover: Photo,
    label: String,
    photoCount: Int,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1f,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = cover.contentUri,
            contentDescription = cover.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Bottom scrim so the label reads cleanly over a busy photo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        startY = 0f,
                    ),
                ),
        )
        // Label + count, bottom-left.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(
                text = if (photoCount > 1) "$label · $photoCount" else label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
