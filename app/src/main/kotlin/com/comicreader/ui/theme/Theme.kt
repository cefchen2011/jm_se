package com.comicreader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/** 可选主题色：key -> 种子色 */
val ThemeSeeds: Map<String, Color> = linkedMapOf(
    "purple" to Color(0xFF6750A4),
    "blue" to Color(0xFF2962FF),
    "green" to Color(0xFF2E7D32),
    "teal" to Color(0xFF00695C),
    "orange" to Color(0xFFE65100),
    "pink" to Color(0xFFC2185B),
    "red" to Color(0xFFC62828)
)

@Composable
fun ComicReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: String = "system",
    content: @Composable () -> Unit
) {
    val seed = ThemeSeeds[themeColor]
    val colorScheme = when {
        seed != null -> if (darkTheme) {
            darkColorScheme(primary = seed)
        } else {
            lightColorScheme(primary = seed)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
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
