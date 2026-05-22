package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

    override fun observePrefetchSampleRateMs(): Flow<Int> = dataStore.data
        .map { prefs -> decodeSampleRate(prefs[SampleRateKey], DisplayPreferences.PREFETCH_SAMPLE_DEFAULT_MS) }

    override fun observePrefetchSampleRateBatterySaverMs(): Flow<Int> = dataStore.data
        .map { prefs -> decodeSampleRate(prefs[SampleRateBatterySaverKey], DisplayPreferences.PREFETCH_SAMPLE_BATTERY_SAVER_DEFAULT_MS) }

    override suspend fun setDefaultGridDensity(level: PhotosZoomLevel) {
        dataStore.edit { it[DensityKey] = level.name }
    }

    override suspend fun setThumbnailQuality(quality: ThumbnailQuality) {
        dataStore.edit { it[QualityKey] = quality.name }
    }

    override suspend fun setPrefetchSampleRateMs(ms: Int) {
        val clamped = ms.coerceIn(
            DisplayPreferences.PREFETCH_SAMPLE_MIN_MS,
            DisplayPreferences.PREFETCH_SAMPLE_MAX_MS,
        )
        dataStore.edit { it[SampleRateKey] = clamped }
    }

    override suspend fun setPrefetchSampleRateBatterySaverMs(ms: Int) {
        val clamped = ms.coerceIn(
            DisplayPreferences.PREFETCH_SAMPLE_MIN_MS,
            DisplayPreferences.PREFETCH_SAMPLE_MAX_MS,
        )
        dataStore.edit { it[SampleRateBatterySaverKey] = clamped }
    }

    companion object {
        private val DensityKey = stringPreferencesKey("display_default_density")
        private val QualityKey = stringPreferencesKey("display_thumbnail_quality")
        private val SampleRateKey = intPreferencesKey("display_prefetch_sample_ms")
        private val SampleRateBatterySaverKey = intPreferencesKey("display_prefetch_sample_battery_saver_ms")

        internal fun decodeDensity(raw: String?): PhotosZoomLevel =
            raw?.let { runCatching { PhotosZoomLevel.valueOf(it) }.getOrNull() }
                ?: PhotosZoomLevel.Items

        internal fun decodeQuality(raw: String?): ThumbnailQuality =
            raw?.let { runCatching { ThumbnailQuality.valueOf(it) }.getOrNull() }
                ?: ThumbnailQuality.Medium

        internal fun decodeSampleRate(raw: Int?, default: Int): Int =
            (raw ?: default).coerceIn(
                DisplayPreferences.PREFETCH_SAMPLE_MIN_MS,
                DisplayPreferences.PREFETCH_SAMPLE_MAX_MS,
            )
    }
}
