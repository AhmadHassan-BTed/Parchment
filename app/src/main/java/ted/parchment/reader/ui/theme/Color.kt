package ted.parchment.reader.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Material 3 color palette for the Parchment app shell.
 *
 * These are used by [ParchmentTheme] as the MaterialTheme fallback when
 * dynamic colors are unavailable (Android < 12). The actual reader UI
 * uses [DesignTokens] / [ReaderColorScheme] for its warm paper aesthetic.
 */

// ── Warm tones derived from the Parchment brand ────────────────────────────

// Light scheme
val ParchmentPrimary = Color(0xFF8B6914)       // warm amber-gold
val ParchmentOnPrimary = Color(0xFFFFFFFF)
val ParchmentPrimaryContainer = Color(0xFFF5E6C8)
val ParchmentSecondary = Color(0xFF6B5E4F)      // warm grey-brown
val ParchmentTertiary = Color(0xFF5A6340)        // muted olive

// Dark scheme
val ParchmentPrimaryDark = Color(0xFFC4974A)     // lighter amber for dark bg
val ParchmentOnPrimaryDark = Color(0xFF1C1C1E)
val ParchmentPrimaryContainerDark = Color(0xFF3D2E10)
val ParchmentSecondaryDark = Color(0xFFCDC0B0)
val ParchmentTertiaryDark = Color(0xFFB3C48E)
