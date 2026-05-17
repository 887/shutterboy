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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.eight87.shutterboy.domain.sort.Direction
import com.eight87.shutterboy.domain.sort.PhotoSort
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsDimens
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow
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
    val coroutineScope = rememberCoroutineScope()
    var showSortSheet by remember { mutableStateOf(false) }

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
            }
        }
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
