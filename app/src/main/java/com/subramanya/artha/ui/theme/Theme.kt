package com.subramanya.artha.ui.theme

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
import com.subramanya.artha.data.preferences.ThemeMode

/**
 * Artha colour scheme — dark theme is the primary surface per the design.
 * Light theme is a derived mirror for users on a bright display; both keep the
 * teal brand. Material You (dynamic colour) only kicks in if explicitly enabled
 * in Settings AND on Android 12+ — when off, the brand teal wins so the app's
 * identity stays consistent.
 *
 * Surfaces follow the design's deep-ink scale: surface=Surface1, surfaceVariant=
 * Surface3, surfaceContainer=Surface2. Hairlines (Line1/2/3) drive the
 * `outlineVariant`/`outline` slots so Card/Divider/Border pick them up
 * automatically — we don't need a separate "border" colour.
 */
private val ArthaDarkColors = darkColorScheme(
    primary = Teal300,
    onPrimary = Teal950,
    primaryContainer = Teal900,
    onPrimaryContainer = Teal300,

    secondary = OchreSoft,
    onSecondary = Color(0xFF1A1208),
    secondaryContainer = Color(0xFF3A2A14),
    onSecondaryContainer = OchreSoft,

    tertiary = Indigo,
    onTertiary = Color(0xFF0A1228),
    tertiaryContainer = IndigoDeep,
    onTertiaryContainer = Color(0xFFD4DBFA),

    error = Expense,
    onError = Color(0xFF2A1410),
    errorContainer = ExpenseSoft,
    onErrorContainer = Expense,

    background = Surface1,
    onBackground = Text1,
    surface = Surface1,
    onSurface = Text1,
    surfaceVariant = Surface3,
    onSurfaceVariant = Text2,
    surfaceContainer = Surface2,
    surfaceContainerLow = Surface1,
    surfaceContainerHigh = Surface3,
    surfaceContainerHighest = Surface4,
    surfaceTint = Teal500,

    outline = Line2,
    outlineVariant = Line1,
    scrim = Surface0,

    inverseSurface = Text1,
    inverseOnSurface = Surface1,
    inversePrimary = Teal700,
)

private val ArthaLightColors = lightColorScheme(
    primary = Teal700,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA7F0E5),
    onPrimaryContainer = Color(0xFF002019),

    secondary = Ochre,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5E2BD),
    onSecondaryContainer = Color(0xFF2A1D08),

    tertiary = IndigoDeep,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDE2F8),
    onTertiaryContainer = Color(0xFF0A1228),

    error = Color(0xFFB3361B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD3),
    onErrorContainer = Color(0xFF410001),

    background = Color(0xFFFBF8F0),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFFBF8F0),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDAE5E0),
    onSurfaceVariant = Color(0xFF3F4946),
    surfaceContainer = Color(0xFFF1ECDD),
    surfaceTint = Teal700,
    outline = Color(0xFF6F7976),
    outlineVariant = Color(0xFFBEC9C5),
)

@Composable
fun ArthaTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, useDynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> ArthaDarkColors
        else -> ArthaLightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = ArthaTypography,
        content = content,
    )
}
