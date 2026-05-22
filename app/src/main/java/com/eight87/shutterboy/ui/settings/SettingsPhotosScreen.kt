package com.eight87.shutterboy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Slider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eight87.shutterboy.R
import com.eight87.shutterboy.data.settings.DisplayPreferences
import com.eight87.shutterboy.data.settings.ThumbnailQuality
import com.eight87.shutterboy.domain.sort.Direction
import com.eight87.shutterboy.domain.sort.PhotoSort
import com.eight87.shutterboy.ui.photos.grid.PhotosZoomLevel
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsDimens
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow
import com.eight87.shutterboy.ui.settings.catalog.SettingsRowDivider
import com.eight87.shutterboy.ui.sort.SortSheet
import kotlinx.coroutines.launch

/**
 * Settings → Photos sub-page (main.md I.4). Today: Default sort row that
 * opens the shared [SortSheet] and persists via `SortPreferences`.
 * Date-header style, year-scrubber visibility, density-on-launch defer
 * to follow-up commits once their underlying preferences land.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPhotosScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    val sort by scope.sortPreferences.observePhotosSort()
        .collectAsStateWithLifecycle(initialValue = PhotoSort.Default)
    val kenBurnsEnabled by scope.slideshowPreferences.observeKenBurnsEnabled()
        .collectAsStateWithLifecycle(initialValue = true)
    val density by scope.displayPreferences.observeDefaultGridDensity()
        .collectAsStateWithLifecycle(initialValue = PhotosZoomLevel.Items)
    val quality by scope.displayPreferences.observeThumbnailQuality()
        .collectAsStateWithLifecycle(initialValue = ThumbnailQuality.Medium)
    val prefetchRate by scope.displayPreferences.observePrefetchSampleRateMs()
        .collectAsStateWithLifecycle(initialValue = DisplayPreferences.PREFETCH_SAMPLE_DEFAULT_MS)
    val prefetchRateSaver by scope.displayPreferences.observePrefetchSampleRateBatterySaverMs()
        .collectAsStateWithLifecycle(initialValue = DisplayPreferences.PREFETCH_SAMPLE_BATTERY_SAVER_DEFAULT_MS)
    val coroutineScope = rememberCoroutineScope()
    var showSortSheet by remember { mutableStateOf(false) }
    var densityPickerOpen by remember { mutableStateOf(false) }
    var qualityPickerOpen by remember { mutableStateOf(false) }
    var prefetchPickerOpen by remember { mutableStateOf(false) }
    var prefetchSaverPickerOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_photos_title)) },
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
                    id = "settings_photos_default_sort",
                    icon = Icons.AutoMirrored.Outlined.Sort,
                    label = stringResource(R.string.settings_photos_default_sort_label),
                    subtitle = stringResource(sortSummaryRes(sort)),
                    onClick = { showSortSheet = true },
                )
                SettingsRowDivider()
                SettingsRow(
                    id = "settings_photos_density",
                    icon = Icons.Outlined.GridView,
                    label = stringResource(R.string.settings_lookfeel_density_label),
                    subtitle = stringResource(densityLabelRes(density)),
                    onClick = { densityPickerOpen = true },
                )
                SettingsRowDivider()
                SettingsRow(
                    id = "settings_photos_quality",
                    icon = Icons.Outlined.HighQuality,
                    label = stringResource(R.string.settings_lookfeel_quality_label),
                    subtitle = stringResource(qualityLabelRes(quality)),
                    onClick = { qualityPickerOpen = true },
                )
                SettingsRowDivider()
                SettingsRow(
                    id = "settings_photos_prefetch_rate",
                    icon = Icons.Outlined.Speed,
                    label = stringResource(R.string.settings_photos_prefetch_rate_label),
                    subtitle = stringResource(
                        R.string.settings_photos_prefetch_rate_subtitle,
                        prefetchRate,
                    ),
                    onClick = { prefetchPickerOpen = true },
                )
                SettingsRowDivider()
                SettingsRow(
                    id = "settings_photos_prefetch_rate_saver",
                    icon = Icons.Outlined.Speed,
                    label = stringResource(R.string.settings_photos_prefetch_rate_saver_label),
                    subtitle = stringResource(
                        R.string.settings_photos_prefetch_rate_saver_subtitle,
                        prefetchRateSaver,
                    ),
                    onClick = { prefetchSaverPickerOpen = true },
                )
                SettingsRowDivider()
                SettingsRow(
                    id = "settings_photos_ken_burns",
                    icon = Icons.Outlined.Slideshow,
                    label = stringResource(R.string.settings_photos_ken_burns_label),
                    subtitle = stringResource(R.string.settings_photos_ken_burns_subtitle),
                    onClick = {
                        coroutineScope.launch {
                            scope.slideshowPreferences.setKenBurnsEnabled(!kenBurnsEnabled)
                        }
                    },
                    trailing = {
                        Switch(
                            checked = kenBurnsEnabled,
                            onCheckedChange = { value ->
                                coroutineScope.launch {
                                    scope.slideshowPreferences.setKenBurnsEnabled(value)
                                }
                            },
                        )
                    },
                )
            }
        }
    }

    if (densityPickerOpen) {
        PhotosRadioPickerDialog(
            title = stringResource(R.string.settings_lookfeel_density_label),
            options = PhotosZoomLevel.entries,
            current = density,
            labelOf = { stringResource(densityLabelRes(it)) },
            onPick = {
                coroutineScope.launch { scope.displayPreferences.setDefaultGridDensity(it) }
                densityPickerOpen = false
            },
            onDismiss = { densityPickerOpen = false },
        )
    }
    if (prefetchPickerOpen) {
        PrefetchRateSliderDialog(
            title = stringResource(R.string.settings_photos_prefetch_rate_dialog_title),
            help = stringResource(R.string.settings_photos_prefetch_rate_help),
            current = prefetchRate,
            onApply = { value ->
                coroutineScope.launch { scope.displayPreferences.setPrefetchSampleRateMs(value) }
                prefetchPickerOpen = false
            },
            onDismiss = { prefetchPickerOpen = false },
        )
    }
    if (prefetchSaverPickerOpen) {
        PrefetchRateSliderDialog(
            title = stringResource(R.string.settings_photos_prefetch_rate_saver_dialog_title),
            help = stringResource(R.string.settings_photos_prefetch_rate_saver_help),
            current = prefetchRateSaver,
            onApply = { value ->
                coroutineScope.launch { scope.displayPreferences.setPrefetchSampleRateBatterySaverMs(value) }
                prefetchSaverPickerOpen = false
            },
            onDismiss = { prefetchSaverPickerOpen = false },
        )
    }
    if (qualityPickerOpen) {
        PhotosRadioPickerDialog(
            title = stringResource(R.string.settings_lookfeel_quality_label),
            options = ThumbnailQuality.entries,
            current = quality,
            labelOf = { stringResource(qualityLabelRes(it)) },
            onPick = {
                coroutineScope.launch { scope.displayPreferences.setThumbnailQuality(it) }
                qualityPickerOpen = false
            },
            onDismiss = { qualityPickerOpen = false },
        )
    }

    if (showSortSheet) {
        SortSheet(
            current = sort,
            onApply = { newSort ->
                coroutineScope.launch { scope.sortPreferences.setPhotosSort(newSort) }
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false },
        )
    }
}

private fun densityLabelRes(level: PhotosZoomLevel): Int = when (level) {
    PhotosZoomLevel.Items -> R.string.settings_lookfeel_density_items
    PhotosZoomLevel.Days -> R.string.settings_lookfeel_density_days
    PhotosZoomLevel.Months -> R.string.settings_lookfeel_density_months
    PhotosZoomLevel.Years -> R.string.settings_lookfeel_density_years
}

private fun qualityLabelRes(quality: ThumbnailQuality): Int = when (quality) {
    ThumbnailQuality.Low -> R.string.settings_lookfeel_quality_low
    ThumbnailQuality.Medium -> R.string.settings_lookfeel_quality_medium
    ThumbnailQuality.High -> R.string.settings_lookfeel_quality_high
}

@Composable
private fun PrefetchRateSliderDialog(
    title: String,
    help: String,
    current: Int,
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(current) { mutableStateOf(current.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(
                        R.string.settings_photos_prefetch_rate_value,
                        draft.toInt(),
                    ),
                )
                Slider(
                    value = draft,
                    onValueChange = { draft = it },
                    valueRange = DisplayPreferences.PREFETCH_SAMPLE_MIN_MS.toFloat()..
                        DisplayPreferences.PREFETCH_SAMPLE_MAX_MS.toFloat(),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Text(
                    text = help,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(draft.toInt()) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun <T> PhotosRadioPickerDialog(
    title: String,
    options: List<T>,
    current: T,
    labelOf: @Composable (T) -> String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEach { opt ->
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(opt) }
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = opt == current,
                            onClick = { onPick(opt) },
                        )
                        Text(
                            text = labelOf(opt),
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_lookfeel_close))
            }
        },
    )
}

private fun sortSummaryRes(sort: PhotoSort): Int = when (sort) {
    is PhotoSort.ByDateTaken -> when (sort.direction) {
        Direction.DESC -> R.string.settings_photos_sort_date_taken_desc
        Direction.ASC -> R.string.settings_photos_sort_date_taken_asc
    }
    is PhotoSort.ByDateAdded -> when (sort.direction) {
        Direction.DESC -> R.string.settings_photos_sort_date_added_desc
        Direction.ASC -> R.string.settings_photos_sort_date_added_asc
    }
    is PhotoSort.ByName -> when (sort.direction) {
        Direction.ASC -> R.string.settings_photos_sort_name_asc
        Direction.DESC -> R.string.settings_photos_sort_name_desc
    }
    is PhotoSort.BySize -> when (sort.direction) {
        Direction.DESC -> R.string.settings_photos_sort_size_desc
        Direction.ASC -> R.string.settings_photos_sort_size_asc
    }
}
