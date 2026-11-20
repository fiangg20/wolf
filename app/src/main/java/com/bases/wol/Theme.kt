package com.bases.wol

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Palette(val name: String, val seed: Color)

val Palettes = listOf(
    Palette("Violet", Color(0xFF6750A4)),
    Palette("Ocean", Color(0xFF00677D)),
    Palette("Mint", Color(0xFF1B7F5C)),
    Palette("Sunset", Color(0xFFC2410C)),
    Palette("Rose", Color(0xFFB4235A)),
    Palette("Lime", Color(0xFF5C7A00)),
    Palette("Slate", Color(0xFF475569)),
)

/** A rounded blob with wavy edges: the app's signature shape. */
class ScallopShape(private val lobes: Int = 8, private val depth: Float = 0.07f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = minOf(cx, cy)
        val steps = 180
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * (2f * PI.toFloat())
            val rad = r * (1f - depth + depth * cos(lobes * t))
            val x = cx + rad * cos(t)
            val y = cy + rad * sin(t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

private fun hsvOf(c: Color): FloatArray {
    val a = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), a)
    return a
}

private fun fromHsv(h: Float, s: Float, v: Float): Color {
    val hh = ((h % 360f) + 360f) % 360f
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hh, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))
}

private fun buildScheme(seed: Color, dark: Boolean, pureBlack: Boolean): ColorScheme {
    val hsv = hsvOf(seed)
    val sec = fromHsv(hsv[0], hsv[1] * 0.45f, hsv[2] * 0.75f)
    val ter = fromHsv(hsv[0] + 55f, hsv[1] * 0.6f, hsv[2] * 0.85f)
    val white = Color.White
    val black = Color.Black
    return if (!dark) {
        lightColorScheme(
            primary = seed,
            onPrimary = white,
            primaryContainer = lerp(seed, white, 0.78f),
            onPrimaryContainer = lerp(seed, black, 0.65f),
            secondary = sec,
            onSecondary = white,
            secondaryContainer = lerp(sec, white, 0.8f),
            onSecondaryContainer = lerp(sec, black, 0.6f),
            tertiary = ter,
            onTertiary = white,
            tertiaryContainer = lerp(ter, white, 0.78f),
            onTertiaryContainer = lerp(ter, black, 0.6f),
            background = lerp(white, seed, 0.03f),
            onBackground = Color(0xFF1C1B1F),
            surface = lerp(white, seed, 0.03f),
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = lerp(white, seed, 0.12f),
            onSurfaceVariant = Color(0xFF49454F),
            outline = Color(0xFF79747E),
            outlineVariant = Color(0xFFCAC4D0),
            surfaceContainerLowest = white,
            surfaceContainerLow = lerp(white, seed, 0.05f),
            surfaceContainer = lerp(white, seed, 0.08f),
            surfaceContainerHigh = lerp(white, seed, 0.11f),
            surfaceContainerHighest = lerp(white, seed, 0.15f),
        )
    } else {
        val base = if (pureBlack) black else lerp(Color(0xFF111113), seed, 0.08f)
        val up = { f: Float -> lerp(base, white, f) }
        darkColorScheme(
            primary = lerp(seed, white, 0.5f),
            onPrimary = lerp(seed, black, 0.7f),
            primaryContainer = lerp(seed, black, 0.5f),
            onPrimaryContainer = lerp(seed, white, 0.82f),
            secondary = lerp(sec, white, 0.55f),
            onSecondary = lerp(sec, black, 0.7f),
            secondaryContainer = lerp(sec, black, 0.5f),
            onSecondaryContainer = lerp(sec, white, 0.82f),
            tertiary = lerp(ter, white, 0.55f),
            onTertiary = lerp(ter, black, 0.7f),
            tertiaryContainer = lerp(ter, black, 0.5f),
            onTertiaryContainer = lerp(ter, white, 0.82f),
            background = base,
            onBackground = Color(0xFFE6E1E5),
            surface = base,
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = up(0.12f),
            onSurfaceVariant = Color(0xFFCAC4D0),
            outline = Color(0xFF938F99),
            outlineVariant = Color(0xFF49454F),
            surfaceContainerLowest = lerp(base, black, 0.3f),
            surfaceContainerLow = up(0.03f),
            surfaceContainer = up(0.06f),
            surfaceContainerHigh = up(0.09f),
            surfaceContainerHighest = up(0.13f),
        )
    }
}

private val WolfShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private fun wolfTypography(): Typography {
    val b = Typography()
    return b.copy(
        headlineMedium = b.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = b.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = b.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = b.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        displaySmall = b.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
        labelLarge = b.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun WolfTheme(settings: AppSettings, dark: Boolean, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val useDynamic = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme = if (useDynamic) {
        val base = if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        if (dark && settings.pureBlack) {
            base.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = lerp(Color.Black, base.surfaceContainerLow, 0.5f),
                surfaceContainer = lerp(Color.Black, base.surfaceContainer, 0.6f),
                surfaceContainerHigh = lerp(Color.Black, base.surfaceContainerHigh, 0.7f),
                surfaceContainerHighest = lerp(Color.Black, base.surfaceContainerHighest, 0.8f),
            )
        } else base
    } else {
        val p = Palettes[settings.palette.coerceIn(0, Palettes.lastIndex)]
        buildScheme(p.seed, dark, settings.pureBlack)
    }
    MaterialTheme(
        colorScheme = scheme,
        shapes = WolfShapes,
        typography = wolfTypography(),
        content = content,
    )
}
