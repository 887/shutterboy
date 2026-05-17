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
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import kotlinx.coroutines.launch

/**
 * Settings → Library sub-page (main.md I.3). Rescan + Clear cache today;
 * Manage sources lands when the SAF source manager UI lands. Both
 * actions surface a snackbar via the shared host. Clear-cache prompts
 * for confirmation since it's destructive of thumbnails (regenerated on
 * next view).
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

    val rescanInProgress = stringResource(R.string.settings_library_rescan_in_progress)
    val rescanCompleteFormat = stringResource(R.string.settings_library_rescan_complete)
    val clearComplete = stringResource(R.string.settings_library_clear_cache_complete)

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
                    onClick = {
                        coroutineScope.launch { scope.snackbar.showSnackbar(rescanInProgress) }
                        coroutineScope.launch {
                            val snapshot = scope.libraryScanner.forceRescan()
                            scope.snackbar.showSnackbar(
                                rescanCompleteFormat.format(snapshot.photos.size),
                            )
                        }
                    },
                )
                SettingsRow(
                    id = "settings_library_manage_sources",
                    icon = Icons.Outlined.Folder,
                    label = stringResource(R.string.settings_library_manage_sources),
                    subtitle = stringResource(R.string.settings_library_manage_sources_subtitle),
                    onClick = { scope.backStack.push(SettingsManageSources) },
                )
                SettingsRow(
                    id = "settings_library_clear_cache",
                    icon = Icons.Outlined.CleaningServices,
                    label = stringResource(R.string.settings_library_clear_cache),
                    subtitle = stringResource(R.string.settings_library_clear_cache_subtitle),
                    onClick = { showClearConfirm = true },
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
}
