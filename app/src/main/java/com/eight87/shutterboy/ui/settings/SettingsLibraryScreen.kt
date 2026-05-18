package com.eight87.shutterboy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil3.SingletonImageLoader
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.SettingsManageSources
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsDimens
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow
import com.eight87.shutterboy.ui.settings.catalog.SettingsRowDivider
import kotlinx.coroutines.launch

/**
 * Settings → Library sub-page (main.md I.3). Rescan + Clear thumbnails +
 * Reset library cache + Manage sources. Each destructive action has its
 * own confirm dialog: Rescan is a light one-liner (I.3.d), Clear cache
 * + Reset get heavier bodies explaining what's wiped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLibraryScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showClearConfirm by remember { mutableStateOf(false) }
    var showRescanConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val rescanInProgress = stringResource(R.string.settings_library_rescan_in_progress)
    val rescanCompleteFormat = stringResource(R.string.settings_library_rescan_complete)
    val clearComplete = stringResource(R.string.settings_library_clear_cache_complete)
    val resetInProgress = stringResource(R.string.settings_library_reset_in_progress)
    val resetCompleteFormat = stringResource(R.string.settings_library_reset_complete)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_library_title)) },
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
            SettingsCard {
                SettingsRow(
                    id = "settings_library_rescan",
                    icon = Icons.Filled.Refresh,
                    label = stringResource(R.string.settings_library_rescan),
                    subtitle = stringResource(R.string.settings_library_rescan_subtitle),
                    onClick = { showRescanConfirm = true },
                )
                SettingsRowDivider()
                SettingsRow(
                    id = "settings_library_manage_sources",
                    icon = Icons.Outlined.Folder,
                    label = stringResource(R.string.settings_library_manage_sources),
                    subtitle = stringResource(R.string.settings_library_manage_sources_subtitle),
                    onClick = { scope.backStack.push(SettingsManageSources) },
                )
                SettingsRowDivider()
                SettingsRow(
                    id = "settings_library_clear_cache",
                    icon = Icons.Outlined.CleaningServices,
                    label = stringResource(R.string.settings_library_clear_cache),
                    subtitle = stringResource(R.string.settings_library_clear_cache_subtitle),
                    onClick = { showClearConfirm = true },
                )
                SettingsRowDivider()
                SettingsRow(
                    id = "settings_library_reset",
                    icon = Icons.Outlined.DeleteForever,
                    label = stringResource(R.string.settings_library_reset),
                    subtitle = stringResource(R.string.settings_library_reset_subtitle),
                    onClick = { showResetConfirm = true },
                )
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.settings_library_clear_cache_dialog_title)) },
            text = { Text(stringResource(R.string.settings_library_clear_cache_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    val loader = SingletonImageLoader.get(context)
                    loader.memoryCache?.clear()
                    loader.diskCache?.clear()
                    coroutineScope.launch { scope.snackbar.showSnackbar(clearComplete) }
                }) {
                    Text(stringResource(R.string.settings_library_clear_cache_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.settings_library_clear_cache_cancel))
                }
            },
        )
    }

    if (showRescanConfirm) {
        AlertDialog(
            onDismissRequest = { showRescanConfirm = false },
            title = { Text(stringResource(R.string.settings_library_rescan_dialog_title)) },
            text = { Text(stringResource(R.string.settings_library_rescan_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showRescanConfirm = false
                    coroutineScope.launch { scope.snackbar.showSnackbar(rescanInProgress) }
                    coroutineScope.launch {
                        val snapshot = scope.libraryScanner.forceRescan()
                        scope.snackbar.showSnackbar(
                            rescanCompleteFormat.format(snapshot.photos.size),
                        )
                    }
                }) {
                    Text(stringResource(R.string.settings_library_rescan_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRescanConfirm = false }) {
                    Text(stringResource(R.string.settings_library_rescan_cancel))
                }
            },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.settings_library_reset_dialog_title)) },
            text = { Text(stringResource(R.string.settings_library_reset_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    coroutineScope.launch { scope.snackbar.showSnackbar(resetInProgress) }
                    coroutineScope.launch {
                        val snapshot = scope.libraryScanner.resetAndRescan()
                        scope.snackbar.showSnackbar(
                            resetCompleteFormat.format(snapshot.photos.size),
                        )
                    }
                }) {
                    Text(
                        text = stringResource(R.string.settings_library_reset_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.settings_library_reset_cancel))
                }
            },
        )
    }
}
