package com.eight87.shutterboy.data.settings

/**
 * Base theme — picks ONLY the surface/background scheme. Tint
 * (primary / secondary / tertiary) is a separate
 * [ThemePreferences.observeTintColor] pref so the user can pick an
 * accent colour without disturbing the surface scheme. Shared across
 * all four boy apps (shutterboy / tonearmboy / whisperboy /
 * strictlykeptboy).
 *
 *  - [DefaultAndroid] — Material You / dynamic colour on API 31+;
 *    falls back to a neutral scheme on older devices.
 *  - [PureBlack] — true-black surface family for AMOLED screens.
 *
 * Legacy stored values "DefaultColors" (the shutterboy-only brand
 * palette) and "Custom:0xRRGGBB" (the old combined base+tint) both
 * fall back to [Default]; the seed previously stored in Custom moves
 * to the new tint-colour pref on first read.
 */
sealed class BaseTheme {
    data object DefaultAndroid : BaseTheme()
    data object PureBlack : BaseTheme()

    /** Storage form. Inverse of [fromStored]. */
    fun toStored(): String = when (this) {
        is DefaultAndroid -> "DefaultAndroid"
        is PureBlack -> "PureBlack"
    }

    companion object {
        val Default: BaseTheme = DefaultAndroid

        fun fromStored(raw: String?): BaseTheme = when (raw) {
            "DefaultAndroid" -> DefaultAndroid
            "PureBlack" -> PureBlack
            else -> Default
        }

        /**
         * Parse a seed colour out of the legacy "Custom:0xRRGGBB"
         * stored value. Used on first migration to seed the new
         * tint-colour pref so users don't lose their custom accent.
         */
        fun extractLegacyCustomSeed(raw: String?): Long? {
            if (raw == null || !raw.startsWith("Custom:")) return null
            val hex = raw.removePrefix("Custom:").removePrefix("0x").removePrefix("0X")
            return runCatching { hex.toLong(16) }.getOrNull()?.and(0xFFFFFFL)
        }
    }
}
