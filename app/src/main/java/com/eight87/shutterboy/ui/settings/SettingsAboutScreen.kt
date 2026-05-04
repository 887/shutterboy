package com.eight87.shutterboy.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.eight87.shutterboy.BuildConfig
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.RouteScope

/**
 * Phase C.x — About sub-page. Single page hosting build identity, license,
 * GitHub link, OSS-acknowledgments placeholder, and the build-version
 * easter-egg tap counter (3 taps within 2 s reveals
 * `R.drawable.easter_egg_tiger`). Mirrors tonearmboy D.16.4 + D.16.5.
 *
 * The 'Settings' tab placeholder navigates here on a single tap until Phase I
 * builds the M3 Expressive grouped-cards root with this page underneath
 * Settings → Library → About.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAboutScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val easterEgg = rememberEasterEggBindings()
    var showLicenseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.about_title)) },
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // App identity block.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.about_app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Version row — the tap target for the easter egg.
            AboutRow(
                label = stringResource(R.string.about_version_label),
                value = stringResource(
                    R.string.about_version_format,
                    versionName(context),
                    BuildConfig.GIT_SHA,
                    BuildConfig.BUILD_DATE,
                ),
                onTap = easterEgg.onVersionTap,
            )

            // License row — opens a Material 3 alert dialog with the MIT body.
            AboutRow(
                label = stringResource(R.string.about_license_row_label),
                value = stringResource(R.string.about_license_row_value),
                onTap = { showLicenseDialog = true },
            )

            // GitHub row — fires an ACTION_VIEW intent for the repo URL.
            val githubUrl = stringResource(R.string.about_github_url)
            AboutRow(
                label = stringResource(R.string.about_github_row_label),
                value = stringResource(R.string.about_github_row_value),
                onTap = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, githubUrl.toUri()))
                },
            )

            // OSS acknowledgments — placeholder until Phase L.
            AboutRow(
                label = stringResource(R.string.about_oss_row_label),
                value = stringResource(R.string.about_oss_row_subtitle),
                onTap = null,
            )
        }
    }

    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            title = { Text(text = stringResource(R.string.about_license_dialog_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = stringResource(R.string.about_license_dialog_body),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicenseDialog = false }) {
                    Text(stringResource(R.string.about_license_dialog_dismiss))
                }
            },
        )
    }

    if (easterEgg.revealed) {
        EasterEggDialog(onDismiss = easterEgg.onDismiss)
    }
}

@Composable
private fun AboutRow(
    label: String,
    value: String,
    onTap: (() -> Unit)?,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .let { if (onTap != null) it.clickable(onClick = onTap) else it }
        .padding(vertical = 8.dp)
    Column(
        modifier = rowModifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Project the install-time `versionName` out of `PackageInfo`. Pinned in code
 * because BuildConfig only carries the manifest-baked sha + date; the version
 * name comes from the package manager.
 */
private fun versionName(context: android.content.Context): String =
    runCatching {
        val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
        pkg.versionName ?: "0.0"
    }.getOrDefault("0.0")
