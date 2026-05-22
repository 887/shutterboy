package com.eight87.shutterboy.ui.viewer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.eight87.shutterboy.R

/**
 * Minimal full-screen viewer for an externally-supplied media URI
 * (ACTION_VIEW from the system chooser, camera app review intents).
 * Independent of Room — the URI may point to a photo that hasn't been
 * scanned yet, so we don't try to look anything up; we just render.
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ExternalPhotoViewerScreen(
    uriString: String,
    mime: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uri = remember(uriString) { Uri.parse(uriString) }
    val isVideo = mime?.startsWith("video/") == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.viewer_loading_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_viewer_back),
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_STREAM, uri)
                            type = mime ?: if (isVideo) "video/*" else "image/*"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(
                            share,
                            context.getString(R.string.viewer_share_chooser_title),
                        )
                        context.startActivity(chooser)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.cd_viewer_share),
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.35f),
                    titleContentColor = Color.White,
                ),
            )
        },
        containerColor = Color.Black,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            if (isVideo) {
                VideoPlayerSurface(
                    contentUri = uri,
                    isActive = true,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ZoomableImage(uri = uri, modifier = Modifier.fillMaxSize())
            }
            // The Scaffold padding gives us the inset under the top bar;
            // we don't apply it to the image so it stays full-bleed.
            @Suppress("UNUSED_EXPRESSION") padding
        }
    }
}

@Composable
private fun ZoomableImage(uri: Uri, modifier: Modifier = Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val state = rememberTransformableState { panChange, zoomChange, _, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 6f)
        if (scale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }
    Box(
        modifier = modifier
            .background(Color.Black)
            .clipToBounds()
            .transformable(state = state),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                ),
        )
    }
}
