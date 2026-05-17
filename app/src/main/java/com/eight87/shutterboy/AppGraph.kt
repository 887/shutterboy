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
import com.eight87.shutterboy.data.settings.DataStoreSortPreferences
import com.eight87.shutterboy.data.settings.DataStoreThemePreferences
import com.eight87.shutterboy.data.settings.DisplayPreferences
import com.eight87.shutterboy.data.settings.RecentSearchesPreferences
import com.eight87.shutterboy.data.settings.SafSourcesPreferences
import com.eight87.shutterboy.data.settings.ScanGatePreferences
import com.eight87.shutterboy.data.settings.SortPreferences
import com.eight87.shutterboy.data.settings.ThemePreferences

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
}
