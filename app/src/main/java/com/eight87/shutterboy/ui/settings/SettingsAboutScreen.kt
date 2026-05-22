package com.eight87.shutterboy.ui.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.eight87.shutterboy.ui.nav.Licenses
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsDimens
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow
import com.eight87.shutterboy.ui.settings.catalog.SettingsRowDivider

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
                .padding(horizontal = SettingsDimens.PagePadding)
                // Match tonearmboy / whisperboy / strictlykeptboy About — make sure the last
                // card row clears the gesture-nav inset. Scaffold's innerPadding already adds
                // the system-bar top; the bottom needs its own pad because the scroll Column
                // doesn't consume insets internally.
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(SettingsDimens.CardSpacing),
        ) {
            // App-icon header. ic_launcher_foreground at 96dp + app name in
            // headlineSmall — the visual identity anchor every sibling app shares.
            // Tagline moves underneath so the heading reads "icon → name → tagline".
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
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
                SettingsRowDivider()
                SettingsRow(
                    id = "settings_about_github",
                    icon = Icons.Filled.Code,
                    label = stringResource(R.string.about_github_row_label),
                    subtitle = stringResource(R.string.about_github_row_value),
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, githubUrl.toUri()))
                    },
                )
                SettingsRowDivider()
                SettingsRow(
                    id = "settings_about_licenses",
                    icon = Icons.AutoMirrored.Outlined.Article,
                    label = stringResource(R.string.licenses_row_label),
                    subtitle = stringResource(R.string.licenses_row_supporting),
                    onClick = { scope.backStack.push(Licenses) },
                )
                SettingsRowDivider()
                SettingsRow(
                    id = "settings_about_oss",
                    icon = Icons.Filled.Favorite,
                    label = stringResource(R.string.about_oss_row_label),
                    subtitle = stringResource(R.string.about_oss_row_subtitle),
                    onClick = null,
                )
            }

            // Credits card — same three-row shape every sibling app uses
            // (cleanroom attribution + sibling-apps link + stack credit).
            SettingsCard(title = stringResource(R.string.about_credits_card_title)) {
                SettingsRow(
                    id = "settings_about_credits_cleanroom",
                    icon = Icons.Filled.Info,
                    label = stringResource(R.string.about_credits_cleanroom_label),
                    subtitle = stringResource(R.string.about_credits_cleanroom_subtitle),
                    onClick = null,
                )
                SettingsRowDivider()
                SettingsRow(
                    id = "settings_about_credits_siblings",
                    icon = Icons.Filled.Favorite,
                    label = stringResource(R.string.about_credits_siblings_label),
                    subtitle = stringResource(R.string.about_credits_siblings_subtitle),
                    onClick = null,
                )
                SettingsRowDivider()
                SettingsRow(
                    id = "settings_about_credits_stack",
                    icon = Icons.Filled.Code,
                    label = stringResource(R.string.about_credits_stack_label),
                    subtitle = stringResource(R.string.about_credits_stack_subtitle),
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
