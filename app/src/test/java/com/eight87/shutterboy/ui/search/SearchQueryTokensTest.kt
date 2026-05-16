package com.eight87.shutterboy.ui.search

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase G — pure-JUnit coverage for [SearchQueryTokens]. No Android, no
 * Robolectric: the helper is plain Kotlin and the suite stays cheap.
 */
class SearchQueryTokensTest {

    @Test
    fun empty_input_yields_no_tokens() {
        assertEquals(emptyList<String>(), SearchQueryTokens.tokenize(""))
        assertEquals(emptyList<String>(), SearchQueryTokens.tokenize("   "))
    }

    @Test
    fun single_token_lowercased_and_trimmed() {
        assertEquals(listOf("vacation"), SearchQueryTokens.tokenize("  Vacation  "))
    }

    @Test
    fun multiple_tokens_split_on_whitespace_runs() {
        assertEquals(
            listOf("beach", "2024", "sunset"),
            SearchQueryTokens.tokenize("  Beach   2024\tSunset"),
        )
    }

    @Test
    fun fts_reserved_characters_are_stripped() {
        assertEquals(
            listOf("hello", "world"),
            SearchQueryTokens.tokenize("\"hello\" *world^"),
        )
    }

    @Test
    fun all_reserved_characters_collapse_to_empty_dropped() {
        assertEquals(emptyList<String>(), SearchQueryTokens.tokenize("\"\" ^^ **"))
    }

    @Test
    fun mixed_case_normalises_to_lowercase() {
        assertEquals(
            listOf("camera", "lens"),
            SearchQueryTokens.tokenize("CAMERA Lens"),
        )
    }
}
