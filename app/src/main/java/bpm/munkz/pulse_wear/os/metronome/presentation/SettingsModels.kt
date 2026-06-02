package bpm.munkz.pulse_wear.os.metronome.presentation

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

internal const val RAINBOW_COLOR = 0x00ABCDEF

internal val PulseColorOptions = listOf(
    -47872,
    -16715777,
    -32512,
    -7667457,
    NEON_GREEN_COLOR,
    -1,
    -65281,
    RAINBOW_COLOR,
)

internal val ThemeMainColorOptions = listOf(
    -47872,
    -16715777,
    -32512,
    -7667457,
    NEON_GREEN_COLOR,
    -1,
    -65281,
)

internal val RainbowColors = listOf(
    Color(0xFFFF3B30),
    Color(0xFFFFD60A),
    Color(0xFF32D74B),
    Color(0xFF64D2FF),
    Color(0xFFBF5AF2),
    Color(0xFFFF2D55),
)

internal val ThemeBackgroundColorOptions = listOf(
    0xFF000000.toInt(),
    0xFF111827.toInt(),
    0xFF001F24.toInt(),
    0xFF1A1028.toInt(),
    0xFF24120A.toInt(),
    0xFF102016.toInt(),
)

internal data class ClockImageChoice(
    val label: String,
    val spanishLabel: String = label,
)

internal data class BigRingModeChoice(
    val mode: BigRingFlashMode,
    val label: String,
    val spanishLabel: String = label,
)

internal data class AccentIntensityChoice(
    val mode: AccentIntensityMode,
    val label: String,
    val spanishLabel: String = label,
)

internal enum class BigRingFlashMode(val persistedValue: Int) {
    All(0),
    Big(1),
    Off(2);

    companion object {
        fun fromPersistedValue(value: Int): BigRingFlashMode {
            return entries.firstOrNull { it.persistedValue == value } ?: Big
        }
    }
}

internal enum class KeepScreenMode(val persistedValue: Int) {
    AppOpen(0),
    Playing(1),
    WatchTimeout(2);

    companion object {
        fun fromPersistedValue(value: Int): KeepScreenMode {
            return entries.firstOrNull { it.persistedValue == value } ?: Playing
        }
    }
}

internal val ClockImageChoices = listOf(
    ClockImageChoice("Rainb", "Arco"),
    ClockImageChoice("Blue", "Azul"),
    ClockImageChoice("Green", "Verde"),
    ClockImageChoice("Orange", "Naran"),
    ClockImageChoice("Purple", "Morad"),
    ClockImageChoice("White", "Blanc"),
    ClockImageChoice("Munk"),
    ClockImageChoice("Sax"),
    ClockImageChoice("Piano"),
    ClockImageChoice("Gtr", "Guit"),
    ClockImageChoice("Trum", "Trom"),
    ClockImageChoice("Rock"),
)

internal val BigRingModeChoices = listOf(
    BigRingModeChoice(BigRingFlashMode.All, "All", "Todo"),
    BigRingModeChoice(BigRingFlashMode.Big, "Big", "Gran"),
    BigRingModeChoice(BigRingFlashMode.Off, "Off", "Off"),
)

internal val AccentIntensityChoices = listOf(
    AccentIntensityChoice(AccentIntensityMode.Big, "Big", "Gran"),
    AccentIntensityChoice(AccentIntensityMode.Medium, "Mid", "Med"),
    AccentIntensityChoice(AccentIntensityMode.Little, "Lil", "Peq"),
    AccentIntensityChoice(AccentIntensityMode.Silent, "Sil", "Sil"),
)

internal fun ClockImageChoice.labelFor(language: AppLanguage): String {
    return when (language) {
        AppLanguage.English -> label
        AppLanguage.Spanish -> spanishLabel
    }
}

internal fun BigRingModeChoice.labelFor(language: AppLanguage): String {
    return when (language) {
        AppLanguage.English -> label
        AppLanguage.Spanish -> spanishLabel
    }
}

internal fun AccentIntensityChoice.labelFor(language: AppLanguage): String {
    return when (language) {
        AppLanguage.English -> label
        AppLanguage.Spanish -> spanishLabel
    }
}

internal fun isRainbowColor(colorArgb: Int): Boolean {
    return colorArgb == RAINBOW_COLOR
}

internal fun colorFromChoice(colorArgb: Int): Color {
    return if (isRainbowColor(colorArgb)) {
        Color(NEON_GREEN_COLOR)
    } else {
        Color(colorArgb)
    }
}

internal fun selectedSwatchMarkColor(colorArgb: Int): Color {
    return if (isRainbowColor(colorArgb) || relativeLuminance(colorArgb) > 0.46) {
        Color.Black
    } else {
        Color.White
    }
}

internal fun hasSafeThemeContrast(
    mainColorArgb: Int,
    backgroundColorArgb: Int,
): Boolean {
    return colorDistanceSquared(mainColorArgb, backgroundColorArgb) >= 0.16
}

internal fun readableTextColorFor(colorArgb: Int): Color {
    return if (relativeLuminance(colorArgb) > 0.46) {
        Color.Black
    } else {
        Color.White
    }
}

private fun colorDistanceSquared(
    firstColorArgb: Int,
    secondColorArgb: Int,
): Double {
    val redDifference = colorChannel(firstColorArgb, 16) - colorChannel(secondColorArgb, 16)
    val greenDifference = colorChannel(firstColorArgb, 8) - colorChannel(secondColorArgb, 8)
    val blueDifference = colorChannel(firstColorArgb, 0) - colorChannel(secondColorArgb, 0)
    return redDifference * redDifference +
        greenDifference * greenDifference +
        blueDifference * blueDifference
}

private fun relativeLuminance(colorArgb: Int): Double {
    val red = linearColorChannel(colorArgb, 16)
    val green = linearColorChannel(colorArgb, 8)
    val blue = linearColorChannel(colorArgb, 0)
    return 0.2126 * red + 0.7152 * green + 0.0722 * blue
}

private fun linearColorChannel(
    colorArgb: Int,
    shift: Int,
): Double {
    val channel = colorChannel(colorArgb, shift)
    return if (channel <= 0.03928) {
        channel / 12.92
    } else {
        ((channel + 0.055) / 1.055).pow(2.4)
    }
}

private fun colorChannel(
    colorArgb: Int,
    shift: Int,
): Double {
    return ((colorArgb shr shift) and 0xFF) / 255.0
}
