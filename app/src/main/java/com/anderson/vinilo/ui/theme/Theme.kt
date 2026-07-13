package com.anderson.vinilo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun ViniloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    coverAccentColors: CoverAccentColors? = null,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val spec = tween<Color>(durationMillis = 600)
    val primary by animateColorAsState(coverAccentColors?.primary ?: baseColorScheme.primary, spec)
    val onPrimary by
        animateColorAsState(coverAccentColors?.onPrimary ?: baseColorScheme.onPrimary, spec)
    val primaryContainer by
        animateColorAsState(
            coverAccentColors?.primaryContainer ?: baseColorScheme.primaryContainer,
            spec,
        )
    val onPrimaryContainer by
        animateColorAsState(
            coverAccentColors?.onPrimaryContainer ?: baseColorScheme.onPrimaryContainer,
            spec,
        )
    val secondary by
        animateColorAsState(coverAccentColors?.secondary ?: baseColorScheme.secondary, spec)
    val onSecondary by
        animateColorAsState(coverAccentColors?.onSecondary ?: baseColorScheme.onSecondary, spec)
    val tertiary by
        animateColorAsState(coverAccentColors?.tertiary ?: baseColorScheme.tertiary, spec)
    val onTertiary by
        animateColorAsState(coverAccentColors?.onTertiary ?: baseColorScheme.onTertiary, spec)

    val colorScheme =
        baseColorScheme.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            tertiary = tertiary,
            onTertiary = onTertiary,
        )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}