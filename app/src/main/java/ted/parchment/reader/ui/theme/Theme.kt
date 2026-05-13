package ted.parchment.reader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 theme wrapper for Parchment.
 *
 * On Android 12+ devices, dynamic colour is used by default. On older devices,
 * the warm Parchment palette from [Color.kt] provides a branded fallback.
 *
 * Note: The PDF reader itself bypasses MaterialTheme colours and uses
 * [ReaderColorScheme] for its warm paper aesthetic. This theme is primarily
 * for the app shell (MainActivity, HomeScreen, dialogs).
 */

private val DarkColorScheme = darkColorScheme(
    primary = ParchmentPrimaryDark,
    onPrimary = ParchmentOnPrimaryDark,
    primaryContainer = ParchmentPrimaryContainerDark,
    secondary = ParchmentSecondaryDark,
    tertiary = ParchmentTertiaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = ParchmentPrimary,
    onPrimary = ParchmentOnPrimary,
    primaryContainer = ParchmentPrimaryContainer,
    secondary = ParchmentSecondary,
    tertiary = ParchmentTertiary
)

@Composable
fun ParchmentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
