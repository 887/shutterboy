package com.eight87.shutterboy.ui.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eight87.shutterboy.R
import com.eight87.shutterboy.data.scan.MediaImagesPermission

/**
 * Runtime-permission gate for `READ_MEDIA_IMAGES` + `READ_MEDIA_VIDEO`
 * (API 33+) or `READ_EXTERNAL_STORAGE` (API ≤ 32). Auto-prompts on
 * first composition; shows a rationale + retry card if denied with a
 * fallback "Open settings" deep-link for the "don't ask again" case.
 *
 * On grant, [onGranted] fires once so the caller can kick a fresh
 * library scan — the first-collect hook in `ShutterboyApp` will
 * eventually pick the change up, but firing immediately avoids the
 * dead-loop where the user grants permission and still has to find
 * a "rescan" button to see their photos.
 *
 * Ported from tonearmboy's `RequireAudioPermission` with the media
 * permission set swapped to images + video and the gate icon flipped
 * to a photo glyph.
 */
@Composable
fun RequireMediaPermission(
    onGranted: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val permissions = remember { MediaImagesPermission.manifestNames.toTypedArray() }

    var granted by remember {
        mutableStateOf(MediaImagesPermission.isGranted(context))
    }
    var asked by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val allGranted = result.values.all { it }
        granted = allGranted
        asked = true
        if (allGranted) onGranted()
    }

    LaunchedEffect(Unit) {
        if (!granted && !asked) {
            launcher.launch(permissions)
        }
    }

    if (granted) {
        content()
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Outlined.Image,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.permission_media_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.permission_media_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { launcher.launch(permissions) }) {
                Text(
                    if (asked) stringResource(R.string.permission_media_retry_button)
                    else stringResource(R.string.permission_media_grant_button),
                )
            }
            if (asked) {
                OutlinedButton(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                }) {
                    Text(stringResource(R.string.permission_media_open_settings_button))
                }
            }
        }
    }
}
