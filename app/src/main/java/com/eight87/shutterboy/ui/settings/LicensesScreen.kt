package com.eight87.shutterboy.ui.settings

import android.app.Application
import android.content.res.AssetManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
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
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsDimens
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * oss-licenses Phase B — Open-source licenses sub-page.
 *
 * Reads the Licensee-generated `assets/licenses/artifacts.json` once at VM
 * construction, resolves each entry's SPDX id to a license-text asset, and
 * renders the inventory as a `LazyColumn` of [SettingsRow]s. Tapping a row
 * opens a Material3 `AlertDialog` showing the license body in monospaced
 * text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: LicensesViewModel = viewModel(factory = LicensesViewModel.factory(app.assets))
    val entries by vm.entries.collectAsStateWithLifecycle()

    LicensesScreenContent(
        entries = entries,
        onBack = { scope.backStack.pop() },
        modifier = modifier,
    )
}

/**
 * Stateless content for the licenses sub-page — split off from
 * [LicensesScreen] so [LicensesScreenTest] (Phase C.2) can mount the surface
 * over a synthetic entry list without dragging in the full [RouteScope] facet
 * bundle. Mirrors the `*Content` pattern already used by other screens in
 * this module (e.g. `FavoritesScreenContent`, `PhotoViewerContent`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreenContent(
    entries: List<LicenseEntry>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<LicenseEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.licenses_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_licenses_back),
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = SettingsDimens.PagePadding),
            verticalArrangement = Arrangement.spacedBy(SettingsDimens.CardSpacing),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = SettingsDimens.CardSpacing,
                bottom = SettingsDimens.CardSpacing,
            ),
        ) {
            items(entries, key = { "${it.groupId}:${it.artifactId}:${it.version}" }) { entry ->
                SettingsCard {
                    val unknownLabel = stringResource(R.string.licenses_unknown_spdx, entry.spdxId)
                    SettingsRow(
                        id = "licenses_${entry.groupId}_${entry.artifactId}",
                        icon = Icons.AutoMirrored.Outlined.Article,
                        label = "${entry.artifactId} ${entry.version}",
                        subtitle = if (entry.licenseText != null) {
                            "${entry.groupId} • ${entry.spdxId}"
                        } else {
                            "${entry.groupId} • $unknownLabel"
                        },
                        onClick = { selected = entry },
                    )
                }
            }
        }
    }

    val current = selected
    if (current != null) {
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(text = "${current.artifactId} ${current.version}") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Column(modifier = Modifier.padding(end = 4.dp)) {
                        Text(
                            text = "${current.groupId} • ${current.spdxId}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = current.licenseText
                                ?: stringResource(R.string.licenses_unknown_spdx, current.spdxId),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selected = null }) {
                    Text(text = stringResource(R.string.about_license_dialog_dismiss))
                }
            },
        )
    }
}

/**
 * One artifact entry surfaced on the screen. [licenseText] is resolved at VM
 * construction by reading the matching SPDX text asset from
 * `app/src/main/assets/licenses/<spdxId>.txt`; if no asset exists for the
 * SPDX id we leave it `null` and the row renders an "Unknown SPDX" warning
 * in its subtitle (per plan B.3).
 */
data class LicenseEntry(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val spdxId: String,
    val licenseText: String?,
)

/**
 * Parses the Licensee `artifacts.json` payload into the screen's
 * [LicenseEntry] shape. Pulled out as a top-level function (no Android
 * dependency) so it can be exercised in a pure-JUnit test.
 */
internal fun parseLicensesCatalog(
    json: String,
    licenseTextResolver: (spdxId: String) -> String?,
): List<LicenseEntry> {
    val raw = LicensesJson.json.decodeFromString<List<LicenseeArtifact>>(json)
    return raw
        .map { artifact ->
            val spdx = artifact.spdxLicenses.firstOrNull()?.identifier ?: "UNKNOWN"
            LicenseEntry(
                groupId = artifact.groupId,
                artifactId = artifact.artifactId,
                version = artifact.version,
                spdxId = spdx,
                licenseText = licenseTextResolver(spdx),
            )
        }
        .sortedBy { "${it.groupId}:${it.artifactId}" }
}

@Serializable
internal data class LicenseeArtifact(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val spdxLicenses: List<LicenseeSpdx> = emptyList(),
)

@Serializable
internal data class LicenseeSpdx(
    val identifier: String,
)

internal object LicensesJson {
    val json: Json = Json { ignoreUnknownKeys = true }
}

/**
 * Reads the assets catalog once at construction and exposes the
 * parsed/sorted [LicenseEntry] list as a `StateFlow`. SPDX-text resolution
 * caches the asset bytes per SPDX id so the same body isn't re-read for
 * every Apache-2.0 entry (which is ~95% of the catalog).
 */
class LicensesViewModel internal constructor(
    assetManager: AssetManager,
) : ViewModel() {
    private val licenseTextCache = mutableMapOf<String, String?>()

    private val _entries = MutableStateFlow<List<LicenseEntry>>(emptyList())
    val entries: StateFlow<List<LicenseEntry>> = _entries.asStateFlow()

    init {
        val catalogJson = runCatching {
            assetManager.open(CATALOG_ASSET).use { it.bufferedReader().readText() }
        }.getOrNull()
        _entries.value = catalogJson
            ?.let { parseLicensesCatalog(it) { spdx -> resolveLicenseText(assetManager, spdx) } }
            .orEmpty()
    }

    private fun resolveLicenseText(assets: AssetManager, spdxId: String): String? {
        return licenseTextCache.getOrPut(spdxId) {
            runCatching {
                assets.open("licenses/$spdxId.txt").use { it.bufferedReader().readText() }
            }.getOrNull()
        }
    }

    companion object {
        internal const val CATALOG_ASSET = "licenses/artifacts.json"

        fun factory(assetManager: AssetManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LicensesViewModel(assetManager) as T
            }
    }
}
