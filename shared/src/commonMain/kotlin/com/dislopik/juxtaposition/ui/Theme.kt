package com.dislopik.juxtaposition.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Juxtaposition's own palette, taken from the site's stylesheets
 * (`webfiles/web/css/web.scss` and `login.css`). The website has no light mode, so neither
 * does the app; it stays on this dark navy scheme whatever the system theme is.
 */
object JuxtColors {
    /** `--background` */
    val Background = Color(0xFF1B1F3B)

    /** `--background-alt`: post cards, the selected nav item. */
    val Surface = Color(0xFF2A2F50)

    /** `--background-dark`: removed posts. */
    val SurfaceDark = Color(0xFF121527)

    /** `--background-alt-alt`: dividers and hairlines. */
    val SurfaceEdge = Color(0xFF383F6B)

    /** `--btn` / `--theme`: primary buttons and selected tabs. */
    val Purple = Color(0xFF673DB6)

    /** `--theme-light`: the selected nav item's label. */
    val PurpleLight = Color(0xFFA185D6)

    /** `--btn-secondary` */
    val PurpleMuted = Color(0xFF333960)

    /** `--text-secondary` */
    val TextSecondary = Color(0xFFA1A8D9)

    /** `--text-secondary-2` */
    val TextMuted = Color(0xFF8990C1)

    /** The login form card (`form.account`). */
    val LoginCard = Color(0xFF292E53)

    /** Login inputs, and their focused state. */
    val InputField = Color(0xFF353C6A)
    val InputFieldFocused = Color(0xFF4B5595)

    /** The site's error toast. */
    val ErrorToast = Color(0xFFA9375B)

    /**
     * A Yeah'd post fills its heart with `red`. Lightened just enough to stay legible on
     * the navy card without reading as a different colour.
     */
    val Yeah = Color(0xFFFF4D4D)
}

private val JuxtColorScheme = darkColorScheme(
    // Material uses `primary` for accent text such as tab indicators, text buttons and links,
    // so it has to be the light purple. The site's darker `--btn` is a fill colour and would
    // be unreadable as text on the navy background; it lives in `primaryContainer`, which is
    // what filled buttons and FABs draw with.
    primary = JuxtColors.PurpleLight,
    onPrimary = JuxtColors.Background,
    primaryContainer = JuxtColors.Purple,
    onPrimaryContainer = Color.White,

    secondary = JuxtColors.PurpleLight,
    onSecondary = JuxtColors.Background,
    // Selected tabs and chips use `background: var(--btn)` on the web.
    secondaryContainer = JuxtColors.Purple,
    onSecondaryContainer = Color.White,

    tertiary = JuxtColors.PurpleLight,
    onTertiary = JuxtColors.Background,
    tertiaryContainer = JuxtColors.PurpleMuted,
    onTertiaryContainer = Color.White,

    background = JuxtColors.Background,
    onBackground = Color.White,

    surface = JuxtColors.Surface,
    onSurface = Color.White,
    surfaceVariant = JuxtColors.SurfaceEdge,
    onSurfaceVariant = JuxtColors.TextSecondary,

    // Keep tonal elevation from washing cards out: tinting with the surface colour is a no-op.
    surfaceTint = JuxtColors.Surface,
    surfaceContainerLowest = JuxtColors.SurfaceDark,
    surfaceContainerLow = JuxtColors.Background,
    surfaceContainer = JuxtColors.LoginCard,
    surfaceContainerHigh = JuxtColors.Surface,
    surfaceContainerHighest = JuxtColors.InputField,

    outline = JuxtColors.SurfaceEdge,
    outlineVariant = JuxtColors.PurpleMuted,

    // Snackbars invert the surface; without this they would come out light.
    inverseSurface = JuxtColors.InputField,
    inverseOnSurface = Color.White,
    inversePrimary = JuxtColors.PurpleLight,

    error = JuxtColors.ErrorToast,
    onError = Color.White,
    errorContainer = JuxtColors.ErrorToast,
    onErrorContainer = Color.White,

    scrim = Color(0xCC121527)
)

@Composable
fun JuxtTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JuxtColorScheme,
        content = content
    )
}
