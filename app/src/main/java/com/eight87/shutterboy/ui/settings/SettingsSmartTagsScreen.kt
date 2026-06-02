package com.eight87.shutterboy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import com.eight87.shutterboy.R
import com.eight87.shutterboy.data.tagging.TaggerAvailability
import com.eight87.shutterboy.data.tagging.TaggingRunState
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsDimens
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow
import com.eight87.shutterboy.ui.settings.catalog.SettingsRowDivider
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Settings → Smart Tags sub-page. On-device AI photo tagging: a master
 * toggle, a GenAI-captions toggle, coverage + approximate storage size,
 * and re-tag / clear actions. When the device supports neither labeling
 * (Play Services) nor captions (AICore), an info banner explains the
 * controls are inert.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSmartTagsScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val enabled by scope.taggingPreferences.observeEnabled()
        .collectAsStateWithLifecycle(initialValue = true)
    val captionsEnabled by scope.taggingPreferences.observeCaptionsEnabled()
        .collectAsStateWithLifecycle(initialValue = false)
    val stats by scope.aiTagging.observeStats()
        .collectAsStateWithLifecycle(initialValue = null)
    val runState by scope.aiTagging.runState.collectAsStateWithLifecycle()

    var availability by remember { mutableStateOf<TaggerAvailability?>(null) }
    LaunchedEffect(Unit) { availability = scope.aiTagging.probeAvailability() }

    var showRetagConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val retagStarted = stringResource(R.string.settings_smarttags_retag_started)
    val clearDone = stringResource(R.string.settings_smarttags_clear_done)

    val captionsSupported = availability?.captionsAvailable ?: false
    val deviceUnsupported = availability?.any == false

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_smarttags_title)) },
                navigationIcon = {
                    IconButton(onClick = { scope.backStack.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_settings_back),
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = SettingsDimens.PagePadding,
                    end = SettingsDimens.PagePadding,
                    top = SettingsDimens.CardSpacing,
                    bottom = SettingsDimens.CardSpacing,
                ),
            verticalArrangement = Arrangement.spacedBy(SettingsDimens.CardSpacing),
        ) {
            if (deviceUnsupported) {
                SettingsCard(title = stringResource(R.string.settings_smarttags_unavailable_title)) {
                    Text(
                        text = stringResource(R.string.settings_smarttags_unavailable_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = SettingsDimens.PagePadding,
                            vertical = 12.dp,
                        ),
                    )
                }
            }

            SettingsCard {
                SettingsRow(
                    id = "settings_smarttags_enable",
                    icon = Icons.Outlined.Sell,
                    label = stringResource(R.string.settings_smarttags_enable_label),
                    subtitle = stringResource(R.string.settings_smarttags_enable_subtitle),
                    trailing = {
                        Switch(
                            checked = enabled,
                            enabled = !deviceUnsupported,
                            onCheckedChange = { value ->
                                coroutineScope.launch { scope.taggingPreferences.setEnabled(value) }
                            },
                        )
                    },
                )
                SettingsRowDivider()
                SettingsRow(
                    id = "settings_smarttags_captions",
                    icon = Icons.Outlined.Description,
                    label = stringResource(R.string.settings_smarttags_captions_label),
                    subtitle = if (captionsSupported) {
                        stringResource(R.string.settings_smarttags_captions_subtitle)
                    } else {
                        stringResource(R.string.settings_smarttags_captions_unavailable)
                    },
                    trailing = {
                        Switch(
                            checked = captionsEnabled && captionsSupported,
                            enabled = enabled && captionsSupported,
                            onCheckedChange = { value ->
                                coroutineScope.launch { scope.taggingPreferences.setCaptionsEnabled(value) }
                            },
                        )
                    },
                )
            }

            SettingsCard(title = stringResource(R.string.settings_smarttags_stats_title)) {
                val s = stats
                SettingsRow(
                    icon = Icons.Outlined.AutoAwesome,
                    label = stringResource(R.string.settings_smarttags_stats_label),
                    subtitle = if (s == null) {
                        stringResource(R.string.settings_smarttags_stats_loading)
                    } else {
                        stringResource(
                            R.string.settings_smarttags_stats_subtitle,
                            s.processedCount,
                            s.totalPhotos,
                            formatBytes(s.approxBytes),
                        )
                    },
                )
                val running = runState
                if (running is TaggingRunState.Running) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SettingsDimens.PagePadding, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.settings_smarttags_progress,
                                running.processed,
                                running.total,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (running.total > 0) {
                            LinearProgressIndicator(
                                progress = {
                                    (running.processed.toFloat() / running.total).coerceIn(0f, 1f)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.Refresh,
                    label = stringResource(R.string.settings_smarttags_retag),
                    subtitle = stringResource(R.string.settings_smarttags_retag_subtitle),
                    onClick = { showRetagConfirm = true },
                )
                SettingsRowDivider()
                SettingsRow(
                    icon = Icons.Outlined.DeleteForever,
                    label = stringResource(R.string.settings_smarttags_clear),
                    subtitle = stringResource(R.string.settings_smarttags_clear_subtitle),
                    onClick = { showClearConfirm = true },
                )
            }
        }
    }

    if (showRetagConfirm) {
        AlertDialog(
            onDismissRequest = { showRetagConfirm = false },
            title = { Text(stringResource(R.string.settings_smarttags_retag_dialog_title)) },
            text = { Text(stringResource(R.string.settings_smarttags_retag_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showRetagConfirm = false
                    scope.aiTagging.retagAll()
                    coroutineScope.launch { scope.snackbar.showSnackbar(retagStarted) }
                }) {
                    Text(stringResource(R.string.settings_smarttags_retag_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRetagConfirm = false }) {
                    Text(stringResource(R.string.settings_smarttags_dialog_cancel))
                }
            },
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.settings_smarttags_clear_dialog_title)) },
            text = { Text(stringResource(R.string.settings_smarttags_clear_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    scope.aiTagging.wipe()
                    coroutineScope.launch { scope.snackbar.showSnackbar(clearDone) }
                }) {
                    Text(
                        text = stringResource(R.string.settings_smarttags_clear_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.settings_smarttags_dialog_cancel))
                }
            },
        )
    }
}

/** Compact, non-localised byte formatter (B / KB / MB) for the storage line. */
internal fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.0f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB", mb)
}
