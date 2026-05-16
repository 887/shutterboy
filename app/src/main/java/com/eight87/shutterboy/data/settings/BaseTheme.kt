package com.eight87.shutterboy.data.settings

/**
 * Base theme picker — ported from tonearmboy. Four variants:
 *
 *  - [DefaultAndroid] — Material You / dynamic colour on API 31+,
 *    falls back to the brand palette on older devices.
 *  - [DefaultColors] — the static shutterboy brand palette regardless
 *    of API.
 *  - [PureBlack] — true-black surface family for AMOLED screens. The
 *    primary / secondary / tertiary still come from the dynamic or
 *    brand palette underneath; only `surface` / `background` go black.
 *  - [Custom] — user picked a seed colour via the in-app HSV picker;
 *    `lightColorScheme` / `darkColorScheme` are derived from it.
 *
 * Persisted as a string. The first three serialise as their class
 * names ("DefaultAndroid" / "DefaultColors" / "PureBlack"); [Custom]
 * serialises as `Custom:0xRRGGBB`. Unknown / malformed strings fall
 * back to [Default].
 */
sealed class BaseTheme {
    data object DefaultAndroid : BaseTheme()
    data object DefaultColors : BaseTheme()
    data object PureBlack : BaseTheme()

    /**
     * Custom seed-colour theme. [seedRgb] is a 24-bit RGB value
     * (alpha is implied 0xFF). Stored as a `Long` rather than `Int` so
     * the high bit doesn't sign-extend round-tripping through DataStore.
     */
    data class Custom(val seedRgb: Long) : BaseTheme()

    /** Storage form. Inverse of [fromStored]. */
    fun toStored(): String = when (this) {
        is DefaultAndroid -> "DefaultAndroid"
        is DefaultColors -> "DefaultColors"
        is PureBlack -> "PureBlack"
        is Custom -> "Custom:0x${(seedRgb and 0xFFFFFFL).toString(16).padStart(6, '0').uppercase()}"
    }

    companion object {
        val Default: BaseTheme = DefaultAndroid

        fun fromStored(raw: String?): BaseTheme {
            if (raw == null) return Default
            if (raw.startsWith("Custom:")) {
                val hex = raw.removePrefix("Custom:").removePrefix("0x").removePrefix("0X")
                val parsed = runCatching { hex.toLong(16) }.getOrNull() ?: return Default
                return Custom(parsed and 0xFFFFFFL)
            }
            return when (raw) {
                "DefaultAndroid" -> DefaultAndroid
                "DefaultColors" -> DefaultColors
                "PureBlack" -> PureBlack
                else -> Default
            }
        }
    }
}
