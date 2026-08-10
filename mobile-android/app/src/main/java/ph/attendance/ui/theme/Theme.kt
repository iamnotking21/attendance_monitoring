package ph.attendance.ui.theme

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

/**
 * The same palette the web app uses, so the two products look like one system.
 *
 * Present, late, and absent are exposed separately from the Material scheme: they carry meaning
 * rather than emphasis, and mapping them onto primary/secondary/tertiary would make the colour
 * of an absence depend on the theme rather than on what it means.
 */
private val Indigo = Color(0xFF4338CA)
private val IndigoLight = Color(0xFF818CF8)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF0FF),
    onPrimaryContainer = Color(0xFF1B1650),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF10131C),
    surface = Color.White,
    onSurface = Color(0xFF10131C),
    surfaceVariant = Color(0xFFEDEFF6),
    onSurfaceVariant = Color(0xFF5B6479),
    outline = Color(0xFFCBD1E0),
    error = Color(0xFFBE123C),
)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Color(0xFF10131C),
    primaryContainer = Color(0xFF1C2039),
    onPrimaryContainer = Color(0xFFE0E3FF),
    background = Color(0xFF0B0D14),
    onBackground = Color(0xFFEEF1F8),
    surface = Color(0xFF12151F),
    onSurface = Color(0xFFEEF1F8),
    surfaceVariant = Color(0xFF1D2130),
    onSurfaceVariant = Color(0xFF9AA3B8),
    outline = Color(0xFF363D51),
    error = Color(0xFFFB7185),
)

data class StatusColors(
    val present: Color,
    val presentContainer: Color,
    val late: Color,
    val lateContainer: Color,
    val absent: Color,
    val absentContainer: Color,
)

private val LightStatus = StatusColors(
    present = Color(0xFF047857),
    presentContainer = Color(0xFFE7F6EF),
    late = Color(0xFFB45309),
    lateContainer = Color(0xFFFDF2E3),
    absent = Color(0xFFBE123C),
    absentContainer = Color(0xFFFDEAEF),
)

private val DarkStatus = StatusColors(
    present = Color(0xFF34D399),
    presentContainer = Color(0xFF10241D),
    late = Color(0xFFFBBF24),
    lateContainer = Color(0xFF2A2113),
    absent = Color(0xFFFB7185),
    absentContainer = Color(0xFF2C1520),
)

val LocalStatusColors = androidx.compose.runtime.staticCompositionLocalOf { LightStatus }

@Composable
fun AttendanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You is honoured where the platform offers it: a school's phones are personal
    // devices, and matching the wallpaper costs nothing.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatus else LightStatus,
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
