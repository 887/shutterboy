package com.eight87.shutterboy.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eight87.shutterboy.R

/**
 * Phase C.x — full-screen modal that reveals the easter-egg tiger artwork
 * (`R.drawable.easter_egg_tiger`) over a 70 % black scrim. Mirrors
 * tonearmboy's D.16.5.3.
 *
 * Tap-outside on the scrim dismisses; system back-button also dismisses via
 * [Dialog]'s default. Image stays unclipped — the dialog uses
 * `usePlatformDefaultWidth = false` so it expands to fill the screen instead
 * of getting boxed into a default-width Card.
 */
@Composable
fun EasterEggDialog(
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val noRipple = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = noRipple,
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.easter_egg_tiger),
                contentDescription = stringResource(R.string.cd_easter_egg_tiger),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            )
        }
    }
}
