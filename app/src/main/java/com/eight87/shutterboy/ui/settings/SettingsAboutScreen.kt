package com.eight87.shutterboy.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Numbers
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
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsDimens
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow

/**
 * Phase C.x — About sub-page. Renders inside the same M3 Expressive
 * [SettingsCard] / [SettingsRow] primitives every other settings surface uses
 * (mirrors tonearmboy D.16.4) so chrome lines up across the app.
 *
 * Two cards:
 *  - **Build** — version + SHA + date row (the easter-egg tap target).
 *  - **Source** — License row (opens MIT alert dialog), GitHub row (opens
 *    repo in the browser), Open-source acknowledgments row (placeholder
 *    until Phase L).
 *
 * The build-version row hosts the tap counter; three taps within
 * [EASTER_EGG_WINDOW_MS] reveal the fullscreen tiger artwork via
 * [EasterEggDialog].
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
    val githubUrl = stringResource(R.string.about_github_url)

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
                .padding(horizontal = SettingsDimens.PagePadding),
            verticalArrangement = Arrangement.spacedBy(SettingsDimens.CardSpacing),
        ) {
            // Identity heading — sits above the first card, aligned to the
            // page padding so it lines up with the card's title-column.
            Column(
                modifier = Modifier.padding(
                    top = SettingsDimens.GroupTitleTopPadding,
                    bottom = 4.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.about_app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Build card — version row is the easter-egg tap target.
            SettingsCard {
                SettingsRow(
                    id = "settings_about_version",
                    icon = Icons.Filled.Numbers,
                    label = stringResource(R.string.about_version_label),
                    subtitle = stringResource(
                        R.string.about_version_format,
                        versionName(context),
                        BuildConfig.GIT_SHA,
                        BuildConfig.BUILD_DATE,
                    ),
                    onClick = easterEgg.onVersionTap,
                )
            }

            // Source card — License + GitHub + OSS acknowledgments.
            SettingsCard {
                SettingsRow(
                    id = "settings_about_license",
                    icon = Icons.AutoMirrored.Filled.Article,
                    label = stringResource(R.string.about_license_row_label),
                    subtitle = stringResource(R.string.about_license_row_value),
                    onClick = { showLicenseDialog = true },
                )
                SettingsRow(
                    id = "settings_about_github",
                    icon = Icons.Filled.Code,
                    label = stringResource(R.string.about_github_row_label),
                    subtitle = stringResource(R.string.about_github_row_value),
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, githubUrl.toUri()))
                    },
                )
                SettingsRow(
                    id = "settings_about_oss",
                    icon = Icons.Filled.Favorite,
                    label = stringResource(R.string.about_oss_row_label),
                    subtitle = stringResource(R.string.about_oss_row_subtitle),
                    onClick = null,
                )
            }
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
