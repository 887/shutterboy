package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Phase incremental-scan A.2 + A.3 — concrete [ScanGatePreferences]
 * backed by the same `shutterboy_settings` DataStore the rest of the
 * preferences code uses.
 *
 * Wire format:
 * - `media_store_generation_<volume>` → `Long`. Absent = never scanned.
 * - `saf_fingerprint` → single `String` of `uri=count;uri=count`. URIs
 *   from SAF tree intents are URL-safe (no `;` or `=` in their content),
 *   so the wire format is unambiguous. Malformed entries are dropped on
 *   decode (cache, not durable user data).
 */
internal class DataStoreScanGatePreferences(
    private val dataStore: DataStore<Preferences>,
) : ScanGatePreferences {

    override fun observeMediaStoreGeneration(volume: String): Flow<Long?> = dataStore.data
        .map { prefs -> prefs[mediaStoreGenerationKey(volume)] }

    override suspend fun setMediaStoreGeneration(volume: String, token: Long) {
        dataStore.edit { it[mediaStoreGenerationKey(volume)] = token }
    }

    override fun observeSafFingerprint(): Flow<Map<String, Int>> = dataStore.data
        .map { prefs -> decodeSafFingerprint(prefs[SafFingerprintKey]) }

    override suspend fun setSafFingerprint(map: Map<String, Int>) {
        dataStore.edit { it[SafFingerprintKey] = encodeSafFingerprint(map) }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.asMap().keys
                .filter { it.name.startsWith(MediaStoreGenerationPrefix) || it == SafFingerprintKey }
                .forEach { prefs.remove(it) }
        }
    }

    private fun mediaStoreGenerationKey(volume: String) =
        longPreferencesKey("${MediaStoreGenerationPrefix}${volume}")

    companion object {
        internal const val MediaStoreGenerationPrefix = "media_store_generation_"
        internal val SafFingerprintKey = stringPreferencesKey("saf_fingerprint")

        internal fun encodeSafFingerprint(map: Map<String, Int>): String =
            map.entries.joinToString(separator = ";") { "${it.key}=${it.value}" }

        internal fun decodeSafFingerprint(encoded: String?): Map<String, Int> {
            if (encoded.isNullOrEmpty()) return emptyMap()
            val out = LinkedHashMap<String, Int>()
            for (entry in encoded.split(';')) {
                if (entry.isEmpty()) continue
                val eq = entry.lastIndexOf('=')
                if (eq <= 0 || eq >= entry.length - 1) continue
                val uri = entry.substring(0, eq)
                val count = entry.substring(eq + 1).toIntOrNull() ?: continue
                out[uri] = count
            }
            return out
        }
    }
}
