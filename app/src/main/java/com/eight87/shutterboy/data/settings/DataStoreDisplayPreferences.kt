package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eight87.shutterboy.ui.photos.grid.PhotosZoomLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete [DisplayPreferences] backed by `DataStore<Preferences>`.
 * Both knobs are encoded as their enum / sealed-variant name; unknown
 * tokens fall back to the documented default (silent downgrade-safe).
 */
internal class DataStoreDisplayPreferences(
    private val dataStore: DataStore<Preferences>,
) : DisplayPreferences {

    override fun observeDefaultGridDensity(): Flow<PhotosZoomLevel> = dataStore.data
        .map { prefs -> decodeDensity(prefs[DensityKey]) }

    override fun observeThumbnailQuality(): Flow<ThumbnailQuality> = dataStore.data
        .map { prefs -> decodeQuality(prefs[QualityKey]) }

    override suspend fun setDefaultGridDensity(level: PhotosZoomLevel) {
        dataStore.edit { it[DensityKey] = level.name }
    }

    override suspend fun setThumbnailQuality(quality: ThumbnailQuality) {
        dataStore.edit { it[QualityKey] = quality.name }
    }

    companion object {
        private val DensityKey = stringPreferencesKey("display_default_density")
        private val QualityKey = stringPreferencesKey("display_thumbnail_quality")

        internal fun decodeDensity(raw: String?): PhotosZoomLevel =
            raw?.let { runCatching { PhotosZoomLevel.valueOf(it) }.getOrNull() }
                ?: PhotosZoomLevel.Items

        internal fun decodeQuality(raw: String?): ThumbnailQuality =
            raw?.let { runCatching { ThumbnailQuality.valueOf(it) }.getOrNull() }
                ?: ThumbnailQuality.Medium
    }
}
