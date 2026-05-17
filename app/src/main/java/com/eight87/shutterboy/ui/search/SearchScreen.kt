package com.eight87.shutterboy.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.eight87.shutterboy.R
import com.eight87.shutterboy.data.repo.FavoriteCommands
import com.eight87.shutterboy.data.repo.FolderSource
import com.eight87.shutterboy.data.repo.PhotoSearch
import com.eight87.shutterboy.domain.Folder
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.ui.nav.PhotoViewer
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.Slideshow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf

internal const val SEARCH_FIELD_TAG = "search_field"
internal const val SEARCH_RESULTS_GRID_TAG = "search_results_grid"
internal const val SEARCH_SLIDESHOW_BUTTON_TAG = "search_slideshow_button"

/**
 * Phase G.5 — milliseconds the trimmed query must stay stable before we
 * commit it to recent-searches. Long enough that mid-word typing doesn't
 * spam DataStore writes, short enough that pause-and-tap-a-result feels
 * like "the search happened".
 */
private const val RECORD_SETTLE_DELAY_MS: Long = 800L

/**
 * Phase G — fullscreen search route. Pulls [PhotoSearch] via the RouteScope
 * facet; result-tap pushes a [PhotoViewer] with the search-result id list
 * as the pager backing.
 */
@Composable
fun SearchScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    SearchScreenContent(
        photoSearch = scope.photoSearch,
        favoriteCommands = scope.favoriteCommands,
        folderSource = scope.folderSource,
        onBack = { scope.backStack.pop() },
        onResultTap = { photoId, backingIds ->
            scope.backStack.push(PhotoViewer(photoId, backingIds))
        },
        onStartSlideshow = { backingIds ->
            scope.backStack.push(Slideshow(backingIds = backingIds))
        },
        modifier = modifier,
    )
}

@Composable
internal fun SearchScreenContent(
    photoSearch: PhotoSearch,
    favoriteCommands: FavoriteCommands? = null,
    folderSource: FolderSource? = null,
    onBack: () -> Unit,
    onResultTap: (Long, List<Long>) -> Unit,
    onStartSlideshow: (List<Long>) -> Unit = {},
    modifier: Modifier = Modifier,
    nowMs: Long = System.currentTimeMillis(),
) {
    // R.F.21 — query / activeFilters / selectedFolder survive rotation +
    // process-death via rememberSaveable. Custom Savers round-trip the
    // filter list as its enum-name strings and the folder selection as its
    // FolderId.value Long (re-resolved against the folders flow below).
    var query by rememberSaveable { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val activeFilters = rememberSaveable(saver = SearchFilterListSaver) {
        mutableStateListOf<SearchFilter>()
    }
    var savedFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showFolderSheet by remember { mutableStateOf(false) }

    // G.3 — favorite-id set powers both the Favorites chip filter and the
    // "no favorites yet → chip disabled" affordance check; folder list
    // feeds the bottom-sheet.
    val favoriteIds by produceState(initialValue = emptySet<Long>(), favoriteCommands) {
        favoriteCommands?.observeFavoriteIds()?.collect { value = it }
    }
    val folders by produceState(initialValue = emptyList<Folder>(), folderSource) {
        folderSource?.observeFolders()?.collect { value = it }
    }
    // Re-resolve persisted folder id against the live folder list.
    val selectedFolder = remember(savedFolderId, folders) {
        savedFolderId?.let { id -> folders.firstOrNull { it.id.value == id } }
    }

    // Live results — re-collect whenever the trimmed query changes.
    val trimmedQuery by remember { derivedStateOf { query.trim() } }
    val rawResults by produceState(initialValue = emptyList<Photo>(), trimmedQuery, photoSearch) {
        val flow = if (trimmedQuery.isEmpty()) flowOf(emptyList()) else photoSearch.searchPhotos(trimmedQuery)
        flow.collect { value = it }
    }
    val filtered by remember(
        rawResults,
        activeFilters.toList(),
        nowMs,
        favoriteIds,
        selectedFolder,
    ) {
        derivedStateOf {
            applyFilters(
                photos = rawResults,
                active = activeFilters.toSet(),
                nowMs = nowMs,
                favoriteIds = favoriteIds,
                selectedFolderId = selectedFolder?.id,
            )
        }
    }
    val recents by produceState(initialValue = emptyList<String>(), photoSearch) {
        photoSearch.recentSearches().collect { value = it }
    }

    // G.5 — record the query after a short settle delay so we persist the
    // query the user actually typed, not every intermediate keystroke. The
    // DataStore impl dedupe-promotes, so re-recording an existing entry just
    // moves it to the head; we still skip blanks here.
    LaunchedEffect(trimmedQuery, photoSearch) {
        if (trimmedQuery.isEmpty()) return@LaunchedEffect
        delay(RECORD_SETTLE_DELAY_MS)
        photoSearch.recordSearch(trimmedQuery)
    }

    if (showFolderSheet && folderSource != null) {
        FolderFilterSheet(
            folders = folders,
            onPick = { folder ->
                savedFolderId = folder.id.value
                showFolderSheet = false
            },
            onDismiss = { showFolderSheet = false },
        )
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            SearchHeader(
                query = query,
                onQueryChange = { query = it },
                onBack = onBack,
                onFocusChange = { focused = it },
                onStartSlideshow = if (filtered.isNotEmpty()) {
                    { onStartSlideshow(filtered.map { it.id.value }) }
                } else null,
            )
            FilterChipRow(
                active = activeFilters.toSet(),
                onToggle = { filter ->
                    if (filter == SearchFilter.Videos) return@FilterChipRow
                    if (activeFilters.contains(filter)) activeFilters.remove(filter)
                    else activeFilters.add(filter)
                },
                selectedFolder = selectedFolder,
                onFolderChipTap = {
                    if (selectedFolder != null) {
                        savedFolderId = null
                    } else if (folderSource != null) {
                        showFolderSheet = true
                    }
                },
                onClearFolder = { savedFolderId = null },
                folderChipEnabled = folderSource != null,
                favoritesChipEnabled = favoriteCommands != null,
            )
            when {
                trimmedQuery.isEmpty() && focused && recents.isNotEmpty() ->
                    RecentSearchesList(recents = recents, onPick = { query = it })
                trimmedQuery.isEmpty() ->
                    Box(modifier = Modifier.fillMaxSize())
                filtered.isEmpty() ->
                    SearchEmptyState()
                else ->
                    ResultsGrid(photos = filtered, onResultTap = onResultTap)
            }
        }
    }
}

@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onStartSlideshow: (() -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_search_back),
            )
        }
        Surface(
            modifier = Modifier
                .padding(start = 4.dp, end = 4.dp)
                .weight(1f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChange(it.isFocused) }
                    .testTag(SEARCH_FIELD_TAG),
                placeholder = { Text(stringResource(R.string.search_field_hint)) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.cd_search_field),
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = stringResource(R.string.search_clear_cd),
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* live-search via state */ }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
        }
        if (onStartSlideshow != null) {
            IconButton(
                onClick = onStartSlideshow,
                modifier = Modifier.testTag(SEARCH_SLIDESHOW_BUTTON_TAG),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Slideshow,
                    contentDescription = stringResource(R.string.slideshow_start_menu_label),
                )
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    active: Set<SearchFilter>,
    onToggle: (SearchFilter) -> Unit,
    selectedFolder: Folder?,
    onFolderChipTap: () -> Unit,
    onClearFolder: () -> Unit,
    folderChipEnabled: Boolean,
    favoritesChipEnabled: Boolean,
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // R.F.2 — chip row iterates the per-variant ConditionUi registry
        // instead of `when`-ing over the enum at two call sites (label +
        // enabled). Adding a SearchFilter variant means one enum case + one
        // SearchFilterUi entry; this row needs no edit.
        val chipCtx = ChipEnabledContext(favoritesWired = favoritesChipEnabled)
        SearchFilter.entries.forEach { filter ->
            val ui = SearchFilterUi.getValue(filter)
            FilterChip(
                selected = active.contains(filter),
                onClick = { onToggle(filter) },
                enabled = ui.enabled(chipCtx),
                label = { Text(text = stringResource(ui.labelRes)) },
                modifier = Modifier.testTag("search_chip_${filter.name.lowercase()}"),
            )
        }
        // G.3 — folder picker chip. Label is the picked folder's display
        // name once one is selected; tapping a selected chip clears it
        // (via the trailing close icon affordance), tapping an unselected
        // chip opens the folder bottom sheet.
        FilterChip(
            selected = selectedFolder != null,
            onClick = onFolderChipTap,
            enabled = folderChipEnabled,
            label = {
                Text(
                    text = selectedFolder?.displayName
                        ?: stringResource(R.string.search_chip_folder_default),
                )
            },
            trailingIcon = if (selectedFolder != null) {
                {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription =
                            stringResource(R.string.search_folder_chip_clear_cd),
                        modifier = Modifier.clickable(onClick = onClearFolder),
                    )
                }
            } else null,
            modifier = Modifier.testTag("search_chip_folder"),
        )
    }
}

@Composable
private fun RecentSearchesList(
    recents: List<String>,
    onPick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        recents.take(10).forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(entry) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = entry,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun ResultsGrid(
    photos: List<Photo>,
    onResultTap: (Long, List<Long>) -> Unit,
) {
    val backingIds = remember(photos) { photos.map { it.id.value } }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .testTag(SEARCH_RESULTS_GRID_TAG),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(
            items = photos,
            key = { p -> "search-${p.id.value}" },
            contentType = { "photo_thumbnail" },
        ) { photo ->
            SearchPhotoThumbnail(
                photo = photo,
                onTap = { onResultTap(photo.id.value, backingIds) },
            )
        }
    }
}

@Composable
private fun SearchPhotoThumbnail(
    photo: Photo,
    onTap: () -> Unit,
) {
    AsyncImage(
        model = photo.contentUri,
        contentDescription = photo.displayName,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onTap)
            .testTag("search_thumb_${photo.id.value}"),
    )
}

@Composable
private fun SearchEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp),
        )
        Text(
            text = stringResource(R.string.search_empty_state),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
