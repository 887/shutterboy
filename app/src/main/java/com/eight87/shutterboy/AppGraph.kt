package com.eight87.shutterboy

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.eight87.shutterboy.data.db.ShutterboyDatabase
import com.eight87.shutterboy.data.repo.FavoriteCommands
import com.eight87.shutterboy.data.repo.FolderSource
import com.eight87.shutterboy.data.repo.LibraryScanner
import com.eight87.shutterboy.data.repo.MediaChangeSource
import com.eight87.shutterboy.data.repo.PhotoDeleter
import com.eight87.shutterboy.data.repo.PhotoMover
import com.eight87.shutterboy.data.repo.PhotoSearch
import com.eight87.shutterboy.data.repo.PhotoSource
import com.eight87.shutterboy.data.repo.RoomGalleryRepository
import com.eight87.shutterboy.data.saf.SafSourceManager
import com.eight87.shutterboy.data.scan.ExifEnricher
import com.eight87.shutterboy.data.scan.MediaStoreScanner
import com.eight87.shutterboy.data.settings.CustomOrderPreferences
import com.eight87.shutterboy.data.settings.DataStoreCustomOrderPreferences
import com.eight87.shutterboy.data.settings.DataStoreDisplayPreferences
import com.eight87.shutterboy.data.settings.DataStoreRecentSearchesPreferences
import com.eight87.shutterboy.data.settings.DataStoreSafSourcesPreferences
import com.eight87.shutterboy.data.settings.DataStoreScanGatePreferences
import com.eight87.shutterboy.data.settings.DataStoreSlideshowPreferences
import com.eight87.shutterboy.data.settings.DataStoreSortPreferences
import com.eight87.shutterboy.data.settings.DataStoreThemePreferences
import com.eight87.shutterboy.data.settings.DisplayPreferences
import com.eight87.shutterboy.data.settings.RecentSearchesPreferences
import com.eight87.shutterboy.data.settings.SlideshowPreferences
import com.eight87.shutterboy.data.settings.SafSourcesPreferences
import com.eight87.shutterboy.data.settings.ScanGatePreferences
import com.eight87.shutterboy.data.settings.SortPreferences
import com.eight87.shutterboy.data.settings.ThemePreferences
import com.eight87.shutterboy.domain.Photo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.shutterboyPrefs: DataStore<Preferences> by preferencesDataStore(
    name = "shutterboy_settings",
)

/**
 * Composition root — the only place that knows concrete types.
 * Phase B.5 ships eight narrow facets (R.A locked) so UI consumers depend
 * only on what they read. `RoomGalleryRepository` happens to implement all
 * eight; the AppGraph exposes them as the eight separate properties below.
 *
 * `ScanConfigSource` here is a placeholder that always returns an empty set
 * of SAF tree URIs. Phase I lands the real `SettingsRepository` that
 * implements [ScanConfigSource] and reads from DataStore. The repo is
 * constructed against the interface, so swapping in the real impl is a
 * one-line change in this file.
 */
class AppGraph(applicationContext: Context) {

    private val appCtx: Context = applicationContext.applicationContext

    private val database: ShutterboyDatabase = Room.databaseBuilder(
        appCtx,
        ShutterboyDatabase::class.java,
        "shutterboy.db",
    ).build()

    private val mediaStoreScanner = MediaStoreScanner(appCtx)
    private val exifEnricher = ExifEnricher(appCtx)
    private val safSourceManager = SafSourceManager(appCtx)

    /**
     * Phase I.3.b — DataStore-backed SAF tree URI set. Exposed through the
     * narrow [SafSourcesPreferences] facet for the Manage-sources UI, and
     * through `ScanConfigSource.safSourceUris` for the data-layer scanner.
     */
    val safSourcesPreferences: SafSourcesPreferences =
        DataStoreSafSourcesPreferences(appCtx.shutterboyPrefs)
    private val scanConfig = safSourcesPreferences as DataStoreSafSourcesPreferences

    /** Phase incremental-scan A.2 + A.3 — cold-start gate persistence. */
    private val scanGate: ScanGatePreferences =
        DataStoreScanGatePreferences(appCtx.shutterboyPrefs)

    /** Phase G.5 — DataStore-backed last-10 recent search queries. */
    val recentSearchesPreferences: RecentSearchesPreferences =
        DataStoreRecentSearchesPreferences(appCtx.shutterboyPrefs)

    private val repository = RoomGalleryRepository(
        context = appCtx,
        photoDao = database.photos(),
        folderDao = database.folders(),
        favoriteDao = database.favorites(),
        searchDao = database.search(),
        mediaStoreScanner = mediaStoreScanner,
        exifEnricher = exifEnricher,
        safSourceManager = safSourceManager,
        scanConfig = scanConfig,
        scanGate = scanGate,
        recentSearchesPrefs = recentSearchesPreferences,
    )

    // Eight narrow facets — UI consumes whichever it needs, never the wholesale repo.
    val photoSource: PhotoSource = repository
    val folderSource: FolderSource = repository
    val photoSearch: PhotoSearch = repository
    val libraryScanner: LibraryScanner = repository
    val favoriteCommands: FavoriteCommands = repository
    val photoDeleter: PhotoDeleter = repository
    val photoMover: PhotoMover = repository
    val mediaChangeSource: MediaChangeSource = repository

    /** Phase E.2 — DataStore-backed sort persistence. */
    val sortPreferences: SortPreferences = DataStoreSortPreferences(appCtx.shutterboyPrefs)

    /** Phase E.4 — DataStore-backed user-pinned ordering of chip row + folder grid. */
    val customOrderPreferences: CustomOrderPreferences =
        DataStoreCustomOrderPreferences(appCtx.shutterboyPrefs)

    /** BaseTheme picker — Material You / brand / pure black / custom seed. */
    val themePreferences: ThemePreferences =
        DataStoreThemePreferences(appCtx.shutterboyPrefs)

    /** Look-and-Feel I.2 — default grid density + thumbnail quality. */
    val displayPreferences: DisplayPreferences =
        DataStoreDisplayPreferences(appCtx.shutterboyPrefs)

    /** Phase J.2 — DataStore-backed slideshow toggles (Ken Burns, future dwell). */
    val slideshowPreferences: SlideshowPreferences =
        DataStoreSlideshowPreferences(appCtx.shutterboyPrefs)

    /**
     * App-scoped photo feed. Stays hot for the process lifetime so the
     * gallery survives navigation away & back instantly — re-mounting
     * PhotosScreen after a viewer round-trip immediately sees the
     * cached list instead of a black-screen gap while Room re-queries.
     * Replays the current value to any new subscriber (StateFlow).
     */
    private val graphScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @OptIn(ExperimentalCoroutinesApi::class)
    val photosFeed: StateFlow<List<Photo>?> =
        sortPreferences.observePhotosSort()
            .flatMapLatest { sort -> photoSource.observePhotos(sort) }
            .stateIn(graphScope, SharingStarted.Eagerly, null)

    init {
        // Auto-rescan when MediaStore notifies of a change (delete /
        // edit / new). Without this, deleting photos through our
        // selection toolbar gets the file off disk but our Room table
        // still has the row — the gallery keeps showing the deleted
        // photo until something else triggers a rescan.
        mediaChangeSource.observeChanges()
            .onEach { graphScope.launch { libraryScanner.scanIfChanged() } }
            .launchIn(graphScope)
    }

    /**
     * Process-lifetime stash of backing photo-id lists for the viewer
     * pager. The PhotoViewer / Slideshow routes can't carry the full
     * id list as a route argument — 26k longs gets to ~600 KB once
     * kotlinx-serialization stringifies them, which blows past the
     * Binder transaction limit on activity stop
     * (TransactionTooLargeException). Instead, the caller stashes the
     * list here under a UUID key and the route only carries the key.
     *
     * Surviving process death is intentionally NOT supported — the
     * stash is in-process. On cold restart the viewer falls back to a
     * one-element pager containing just the tapped photo.
     */
    private val backingIdsStash = java.util.concurrent.ConcurrentHashMap<String, List<Long>>()

    fun stashBackingIds(ids: List<Long>): String {
        val key = java.util.UUID.randomUUID().toString()
        backingIdsStash[key] = ids
        return key
    }

    fun takeBackingIds(key: String): List<Long>? = backingIdsStash[key]
}
