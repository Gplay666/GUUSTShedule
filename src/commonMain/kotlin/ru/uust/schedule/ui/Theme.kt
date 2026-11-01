package ru.uust.schedule.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

enum class ThemeMode {
    Light,
    Dark,
    System,
}

/**
 * The colour combinations from the UUST brand guide (pages 17–18).
 *
 * [swatches] contains the exact colours used by the guidebook. The generated
 * Material colour scheme adjusts their tones only where needed for contrast.
 */
enum class ThemePalette(
    val displayName: String,
    val swatches: List<Color>,
    internal val outer: Color? = null,
    internal val inner: Color? = null,
) {
    Current(
        displayName = "Текущая палитра",
        swatches = listOf(
            Color(0xFF4B3FD6),
            Color(0xFFE25B75),
            Color(0xFF00796B),
        ),
    ),
    Brand1(
        displayName = "Палитра 1",
        swatches = listOf(Color(0xFF2A1468), Color(0xFF4D19CC)),
        outer = Color(0xFF2A1468),
        inner = Color(0xFF4D19CC),
    ),
    Brand2(
        displayName = "Палитра 2",
        swatches = listOf(Color(0xFFE8E8E8), Color(0xFFCBB8FF)),
        outer = Color(0xFFE8E8E8),
        inner = Color(0xFFCBB8FF),
    ),
    Brand3(
        displayName = "Палитра 3",
        swatches = listOf(Color(0xFF4D19CC), Color(0xFFAA92FF)),
        outer = Color(0xFF4D19CC),
        inner = Color(0xFFAA92FF),
    ),
    Brand4(
        displayName = "Палитра 4",
        swatches = listOf(Color(0xFFCBB8FF), Color(0xFF8264FF)),
        outer = Color(0xFFCBB8FF),
        inner = Color(0xFF8264FF),
    ),
    Brand5(
        displayName = "Палитра 5",
        swatches = listOf(Color(0xFFF9D84B), Color(0xFFEA591F)),
        outer = Color(0xFFF9D84B),
        inner = Color(0xFFEA591F),
    ),
    Brand6(
        displayName = "Палитра 6",
        swatches = listOf(Color(0xFFFFA039), Color(0xFFEA591F)),
        outer = Color(0xFFFFA039),
        inner = Color(0xFFEA591F),
    ),
    Brand7(
        displayName = "Палитра 7",
        swatches = listOf(Color(0xFF00BC7D), Color(0xFFC1F26B)),
        outer = Color(0xFF00BC7D),
        inner = Color(0xFFC1F26B),
    ),
    System(
        displayName = "Цвета устройства",
        swatches = emptyList(),
    ),
}

private val CurrentLightColors = lightColorScheme(
    primary = Color(0xFF4B3FD6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5DEFF),
    onPrimaryContainer = Color(0xFF14005E),
    inversePrimary = Color(0xFFC7BFFF),
    secondary = Color(0xFFE25B75),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DF),
    onSecondaryContainer = Color(0xFF3F0012),
    tertiary = Color(0xFF00796B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA7F2E4),
    onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFE7E0EB),
    onSurfaceVariant = Color(0xFF49454E),
    surfaceTint = Color(0xFF4B3FD6),
    inverseSurface = Color(0xFF313036),
    inverseOnSurface = Color(0xFFF4EFF7),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF7A757F),
    outlineVariant = Color(0xFFCBC4CF),
    surfaceBright = Color(0xFFFCF8FF),
    surfaceContainer = Color(0xFFF1ECF5),
    surfaceContainerHigh = Color(0xFFEBE6EF),
    surfaceContainerHighest = Color(0xFFE5E0E9),
    surfaceContainerLow = Color(0xFFF6F2FA),
    surfaceContainerLowest = Color.White,
    surfaceDim = Color(0xFFDDD8E0),
)

private val CurrentDarkColors = darkColorScheme(
    primary = Color(0xFFC7BFFF),
    onPrimary = Color(0xFF2B1B8F),
    primaryContainer = Color(0xFF4132C9),
    onPrimaryContainer = Color(0xFFE5DEFF),
    inversePrimary = Color(0xFF4B3FD6),
    secondary = Color(0xFFFFB2C0),
    onSecondary = Color(0xFF5E1130),
    secondaryContainer = Color(0xFF7B2946),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = Color(0xFF8BD5C8),
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF005048),
    onTertiaryContainer = Color(0xFFA7F2E4),
    background = Color(0xFF141318),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF141318),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCBC4CF),
    surfaceTint = Color(0xFFC7BFFF),
    inverseSurface = Color(0xFFE6E1E9),
    inverseOnSurface = Color(0xFF313036),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF958F99),
    outlineVariant = Color(0xFF49454E),
    surfaceBright = Color(0xFF3A383F),
    surfaceContainer = Color(0xFF201F25),
    surfaceContainerHigh = Color(0xFF2B292F),
    surfaceContainerHighest = Color(0xFF36343A),
    surfaceContainerLow = Color(0xFF1C1B20),
    surfaceContainerLowest = Color(0xFF0F0E13),
    surfaceDim = Color(0xFF141318),
)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(38.dp),
)

private val ExpressiveTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 57.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1.6).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 45.sp,
        lineHeight = 49.sp,
        letterSpacing = (-1.2).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 37.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp,
    ),
)

object AppMotion {
    fun <T> spatial() = spring<T>(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMediumLow,
    )

    fun <T> effects() = spring<T>(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMedium,
    )
}

@Composable
fun UustScheduleTheme(
    darkTheme: Boolean,
    palette: ThemePalette = ThemePalette.Current,
    systemAccent: Color? = null,
    content: @Composable () -> Unit,
) {
    // Generating a Material scheme runs contrast calculations and allocates a
    // full ColorScheme. Reuse it until the theme inputs actually change.
    val colorScheme = remember(darkTheme, palette, systemAccent) {
        when {
            palette == ThemePalette.System && systemAccent != null -> {
                val companion = mix(
                    systemAccent,
                    if (darkTheme) Color.Black else Color.White,
                    0.28f,
                )
                brandColorScheme(systemAccent, companion, darkTheme)
            }

            palette.outer != null && palette.inner != null -> {
                brandColorScheme(palette.outer, palette.inner, darkTheme)
            }

            else -> if (darkTheme) CurrentDarkColors else CurrentLightColors
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ExpressiveShapes,
        typography = ExpressiveTypography,
        content = content,
    )
}

/**
 * Builds a complete Material 3 scheme from a two-colour brand combination.
 *
 * The exact guidebook colours are kept for the palette swatches. For actual
 * surfaces and text we pick the darker/lighter member of the pair and nudge
 * it only as far as necessary to stay readable.
 */
private fun brandColorScheme(
    first: Color,
    second: Color,
    dark: Boolean,
): ColorScheme {
    val darker = if (first.relativeLuminance() <= second.relativeLuminance()) first else second
    val lighter = if (darker == first) second else first

    val background = if (dark) {
        mix(Color(0xFF121116), lighter, 0.055f)
    } else {
        mix(Color.White, darker, 0.022f)
    }
    val onBackground = if (dark) Color(0xFFE6E1E9) else Color(0xFF1C1B20)

    val primary = ensureContrast(if (dark) lighter else darker, background)
    val secondary = ensureContrast(if (dark) darker else lighter, background)
    val tertiary = ensureContrast(mix(primary, secondary, 0.45f), background)

    val primaryContainer = if (dark) {
        mix(primary, Color.Black, 0.56f)
    } else {
        mix(primary, Color.White, 0.84f)
    }
    val secondaryContainer = if (dark) {
        mix(secondary, Color.Black, 0.56f)
    } else {
        mix(secondary, Color.White, 0.84f)
    }
    val tertiaryContainer = if (dark) {
        mix(tertiary, Color.Black, 0.56f)
    } else {
        mix(tertiary, Color.White, 0.84f)
    }

    val onPrimaryContainer = ensureContrast(
        if (dark) mix(primary, Color.White, 0.82f) else mix(primary, Color.Black, 0.72f),
        primaryContainer,
    )
    val onSecondaryContainer = ensureContrast(
        if (dark) mix(secondary, Color.White, 0.82f) else mix(secondary, Color.Black, 0.72f),
        secondaryContainer,
    )
    val onTertiaryContainer = ensureContrast(
        if (dark) mix(tertiary, Color.White, 0.82f) else mix(tertiary, Color.Black, 0.72f),
        tertiaryContainer,
    )

    val surfaceVariant = if (dark) {
        mix(background, primary, 0.12f)
    } else {
        mix(background, primary, 0.08f)
    }
    val onSurfaceVariant = ensureContrast(
        if (dark) Color(0xFFCBC4CF) else Color(0xFF49454E),
        surfaceVariant,
    )
    val outline = if (dark) {
        mix(onBackground, background, 0.55f)
    } else {
        mix(onBackground, background, 0.45f)
    }
    val outlineVariant = if (dark) {
        mix(onBackground, background, 0.30f)
    } else {
        mix(onBackground, background, 0.20f)
    }

    val surface = background
    val surfaceContainerLowest = if (dark) mix(background, Color.Black, 0.15f) else Color.White
    val surfaceContainerLow = if (dark) {
        mix(background, Color.White, 0.03f)
    } else {
        mix(background, primary, 0.03f)
    }
    val surfaceContainer = if (dark) {
        mix(background, Color.White, 0.06f)
    } else {
        mix(background, primary, 0.06f)
    }
    val surfaceContainerHigh = if (dark) {
        mix(background, Color.White, 0.09f)
    } else {
        mix(background, primary, 0.10f)
    }
    val surfaceContainerHighest = if (dark) {
        mix(background, Color.White, 0.12f)
    } else {
        mix(background, primary, 0.14f)
    }
    val surfaceBright = if (dark) mix(background, Color.White, 0.20f) else Color.White
    val surfaceDim = if (dark) background else mix(background, onBackground, 0.12f)

    return if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = readableOn(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = mix(primary, Color.Black, 0.55f),
            secondary = secondary,
            onSecondary = readableOn(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = readableOn(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onBackground,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = primary,
            inverseSurface = Color(0xFFE6E1E9),
            inverseOnSurface = Color(0xFF313036),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            outline = outline,
            outlineVariant = outlineVariant,
            surfaceBright = surfaceBright,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceDim = surfaceDim,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = readableOn(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = mix(primary, Color.White, 0.60f),
            secondary = secondary,
            onSecondary = readableOn(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = readableOn(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onBackground,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = primary,
            inverseSurface = Color(0xFF313036),
            inverseOnSurface = Color(0xFFF4EFF7),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            outline = outline,
            outlineVariant = outlineVariant,
            surfaceBright = surfaceBright,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceDim = surfaceDim,
        )
    }
}

private fun mix(first: Color, second: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = first.red + (second.red - first.red) * t,
        green = first.green + (second.green - first.green) * t,
        blue = first.blue + (second.blue - first.blue) * t,
        alpha = first.alpha + (second.alpha - first.alpha) * t,
    )
}

private fun Color.relativeLuminance(): Float {
    fun channel(value: Float): Float {
        return if (value <= 0.03928f) {
            value / 12.92f
        } else {
            ((value + 0.055f) / 1.055f).pow(2.4f)
        }
    }
    return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}

private fun contrastRatio(first: Color, second: Color): Float {
    val firstLuminance = first.relativeLuminance()
    val secondLuminance = second.relativeLuminance()
    val lighter = maxOf(firstLuminance, secondLuminance)
    val darker = minOf(firstLuminance, secondLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun readableOn(background: Color): Color {
    return if (contrastRatio(Color.White, background) >= contrastRatio(Color.Black, background)) {
        Color.White
    } else {
        Color.Black
    }
}

private fun ensureContrast(
    color: Color,
    background: Color,
    minimum: Float = 4.5f,
): Color {
    if (contrastRatio(color, background) >= minimum) return color
    val target = if (background.relativeLuminance() > 0.5f) Color.Black else Color.White
    var result = color
    repeat(28) {
        result = mix(result, target, 0.08f)
        if (contrastRatio(result, background) >= minimum) return result
    }
    return target
}
