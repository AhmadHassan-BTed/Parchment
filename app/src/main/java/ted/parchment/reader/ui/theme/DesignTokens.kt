package ted.parchment.reader.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens for the Parchment reading experience.
 *
 * Provides a warm, paper-like aesthetic in light mode and a comfortable
 * charcoal tone in dark mode — deliberately avoiding pure black (#000000)
 * to reduce eye strain during extended reading sessions.
 *
 * Usage: Access via [ReaderColorScheme] which resolves tokens based on
 * the current night mode state.
 */
object DesignTokens {

    // ── Light Mode — "printed paper" warmth ─────────────────────────────────
    val LightBackground = Color(0xFFF8F1E4)
    val LightText = Color(0xFF222222)
    val LightAccent = Color(0xFF8B6914)   // warm amber-gold
    val LightSurface = Color(0xFFF0E8D8)
    val LightHud = Color(0xEEF0E8D8)     // 93% opaque

    // ── Dark Mode — charcoal, NOT pure black ────────────────────────────────
    val DarkBackground = Color(0xFF1C1C1E)
    val DarkText = Color(0xFFEDE6D6)
    val DarkAccent = Color(0xFFC4974A)
    val DarkSurface = Color(0xFF2C2C2E)
    val DarkHud = Color(0xEE2C2C2E)

    // ── Timing Constants ────────────────────────────────────────────────────
    const val HUD_AUTO_HIDE_MS = 4_000L
    const val CHAPTER_OVERLAY_MS = 2_000L
    const val SWIPE_THRESHOLD = 80f
}

/**
 * Resolved color scheme based on the current reading mode.
 *
 * This is a lightweight alternative to MaterialTheme color schemes,
 * tailored specifically for the reader's warm paper aesthetic.
 */
data class ReaderColorScheme(
    val background: Color,
    val text: Color,
    val accent: Color,
    val surface: Color,
    val hud: Color
) {
    companion object {
        fun resolve(isNightMode: Boolean): ReaderColorScheme = if (isNightMode) {
            ReaderColorScheme(
                background = DesignTokens.DarkBackground,
                text = DesignTokens.DarkText,
                accent = DesignTokens.DarkAccent,
                surface = DesignTokens.DarkSurface,
                hud = DesignTokens.DarkHud
            )
        } else {
            ReaderColorScheme(
                background = DesignTokens.LightBackground,
                text = DesignTokens.LightText,
                accent = DesignTokens.LightAccent,
                surface = DesignTokens.LightSurface,
                hud = DesignTokens.LightHud
            )
        }
    }
}
