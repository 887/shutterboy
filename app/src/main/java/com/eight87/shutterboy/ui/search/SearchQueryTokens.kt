package com.eight87.shutterboy.ui.search

/**
 * Phase G — pure helper that tokenizes a raw query string into substring
 * tokens used by the case-insensitive fallback path (G.2). Lives separately
 * from the FTS-prefix expression that `RoomGalleryRepository.searchPhotos`
 * already builds, so the pure-JUnit suite can exercise this without Room.
 *
 * Rules:
 *   - Trim leading / trailing whitespace.
 *   - Split on any run of whitespace.
 *   - Drop empty tokens (consecutive whitespace, leading/trailing).
 *   - Lowercase every token.
 *   - Strip the FTS-reserved characters that `searchPhotos` already filters
 *     out, so the two paths share the same effective alphabet.
 */
internal object SearchQueryTokens {

    private val WHITESPACE = Regex("\\s+")
    private val FTS_RESERVED = Regex("[\"\\^*]")

    fun tokenize(raw: String): List<String> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return emptyList()
        return trimmed.split(WHITESPACE)
            .map { it.replace(FTS_RESERVED, "") }
            .map { it.lowercase() }
            .filter { it.isNotEmpty() }
    }
}
