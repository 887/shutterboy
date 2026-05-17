package com.eight87.shutterboy.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.SourceType
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsDimens
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Phase I.3.b — Settings → Library → Manage sources. Lists the persisted
 * SAF tree URIs with per-source photo counts. The "+ Add source" row
 * launches `ACTION_OPEN_DOCUMENT_TREE`; on a successful pick we take
 * persistable read permission, persist the URI through
 * [com.eight87.shutterboy.data.settings.SafSourcesPreferences], and force
 * a rescan so the new tree is picked up immediately. Per-row delete
 * releases the permission and removes the URI from prefs (also rescans).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsManageSourcesScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val sources by scope.safSourcesPreferences.observeSources()
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val safFoldersFlow = remember(scope.folderSource) {
        scope.folderSource.observeFolders()
            .map { list -> list.filter { it.sourceType == SourceType.SAF } }
    }
    val safFolders by safFoldersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val addedMessage = stringResource(R.string.settings_manage_sources_added)
    val removedMessage = stringResource(R.string.settings_manage_sources_removed)

    val pickDirectory = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        coroutineScope.launch {
            scope.safSourcesPreferences.add(uri.toString())
            scope.snackbar.showSnackbar(addedMessage)
            scope.libraryScanner.forceRescan()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_manage_sources_title)) },
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
                    id = "settings_manage_sources_add",
                    icon = Icons.Outlined.Add,
                    label = stringResource(R.string.settings_manage_sources_add),
                    subtitle = stringResource(R.string.settings_manage_sources_add_subtitle),
                    onClick = { pickDirectory.launch(null) },
                )
                if (sources.isEmpty()) {
                    SettingsRow(
                        id = "settings_manage_sources_empty",
                        icon = Icons.Outlined.Folder,
                        label = stringResource(R.string.settings_manage_sources_empty),
                    )
                } else {
                    val safCountsByUri: Map<String, Int> = safFolders
                        .mapNotNull { f -> f.safTreeUri?.toString()?.let { it to f.photoCount } }
                        .toMap()
                    sources.forEach { uriStr ->
                        val displayName = remember(uriStr, context) {
                            displayNameForTreeUri(context, uriStr)
                        }
                        val count = safCountsByUri[uriStr] ?: 0
                        SettingsRow(
                            id = "settings_manage_sources_row_${uriStr.hashCode()}",
                            icon = Icons.Outlined.Folder,
                            label = displayName,
                            subtitle = stringResource(
                                R.string.settings_manage_sources_photo_count,
                                count,
                            ),
                            trailing = {
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        runCatching {
                                            context.contentResolver.releasePersistableUriPermission(
                                                Uri.parse(uriStr),
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION,
                                            )
                                        }
                                        scope.safSourcesPreferences.remove(uriStr)
                                        scope.snackbar.showSnackbar(removedMessage)
                                        scope.libraryScanner.forceRescan()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(
                                            R.string.cd_settings_manage_sources_remove,
                                        ),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Best-effort display name for a SAF tree URI. `DocumentFile.fromTreeUri`
 * resolves the friendly `DISPLAY_NAME` via the content provider; if that
 * fails (revoked permission / inaccessible provider) we fall back to the
 * decoded last path segment.
 */
private fun displayNameForTreeUri(
    context: android.content.Context,
    uriStr: String,
): String {
    val uri = runCatching { Uri.parse(uriStr) }.getOrNull() ?: return uriStr
    val viaDocumentFile = runCatching { DocumentFile.fromTreeUri(context, uri)?.name }
        .getOrNull()
    if (!viaDocumentFile.isNullOrBlank()) return viaDocumentFile
    val last = uri.lastPathSegment ?: return uriStr
    val decoded = runCatching { Uri.decode(last) }.getOrNull() ?: last
    val sep = decoded.indexOf(':')
    val tail = if (sep < 0) decoded else decoded.substring(sep + 1)
    return when {
        tail.isBlank() && sep >= 0 -> decoded.substring(0, sep)
        tail.isBlank() -> decoded
        else -> tail
    }
}

