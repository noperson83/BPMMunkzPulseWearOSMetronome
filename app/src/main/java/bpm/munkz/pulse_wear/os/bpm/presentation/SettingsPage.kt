package bpm.munkz.pulse_wear.os.bpm.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlin.math.abs
import kotlin.math.max
@Composable
private fun ColorPickerRow(
    selectedColorArgb: Int,
    onColorChoice: (Int) -> Unit,
    colorOptions: List<Int> = PulseColorOptions,
    enabled: Boolean = true,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        colorOptions.chunked(4).forEach { colorRow ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                colorRow.forEach { colorArgb ->
                    ColorSwatchButton(
                        colorArgb = colorArgb,
                        selected = selectedColorArgb == colorArgb,
                        enabled = enabled,
                        onClick = { onColorChoice(colorArgb) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchButton(
    colorArgb: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(9.dp)
    val enabledAlpha = if (enabled) 1f else 0.28f
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = enabledAlpha)
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.28f * enabledAlpha)
    }
    val baseModifier = Modifier
        .width(36.dp)
        .height(22.dp)
        .clip(shape)
        .then(
            if (isRainbowColor(colorArgb)) {
                Modifier.background(Brush.horizontalGradient(RainbowColors.map { it.copy(alpha = enabledAlpha) }), shape)
            } else {
                Modifier.background(colorFromChoice(colorArgb).copy(alpha = enabledAlpha), shape)
            },
        )
        .border(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor,
            shape = shape,
        )
        .clickable(enabled = enabled, onClick = onClick)

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Text(
                text = "*",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = selectedSwatchMarkColor(colorArgb),
            )
        }
    }
}

@Composable
private fun ClockImagePicker(
    selectedIndex: Int,
    appLanguage: AppLanguage,
    onClockImageChoice: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ClockImageChoices.chunked(4).forEachIndexed { rowIndex, choices ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                choices.forEachIndexed { columnIndex, choice ->
                    val choiceIndex = rowIndex * 4 + columnIndex
                    ChoicePillButton(
                        text = choice.labelFor(appLanguage),
                        selected = selectedIndex == choiceIndex,
                        modifier = Modifier
                            .width(42.dp)
                            .height(26.dp),
                        onClick = { onClockImageChoice(choiceIndex) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BigRingModePicker(
    selectedMode: BigRingFlashMode,
    appLanguage: AppLanguage,
    onModeChoice: (BigRingFlashMode) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BigRingModeChoices.forEach { choice ->
            ChoicePillButton(
                text = choice.labelFor(appLanguage),
                selected = selectedMode == choice.mode,
                modifier = Modifier
                    .width(42.dp)
                    .height(26.dp),
                enabled = enabled,
                onClick = { onModeChoice(choice.mode) },
            )
        }
    }
}

@Composable
private fun LanguagePicker(
    selectedLanguage: AppLanguage,
    onLanguageChoice: (AppLanguage) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppLanguages.forEach { language ->
            ChoicePillButton(
                text = when (language) {
                    AppLanguage.English -> "EN"
                    AppLanguage.Spanish -> "ES"
                },
                selected = selectedLanguage == language,
                modifier = Modifier
                    .width(42.dp)
                    .height(26.dp),
                enabled = enabled,
                onClick = { onLanguageChoice(language) },
            )
        }
    }
}

@Composable
internal fun SimpleSettingsPage(
    appText: AppText,
    hapticsEnabled: Boolean,
    beepEnabled: Boolean,
    beatSoundMode: BeatSoundMode,
    keepScreenMode: KeepScreenMode,
    mainColorArgb: Int,
    backgroundColorArgb: Int,
    ringColorArgb: Int,
    bigRingFlashMode: BigRingFlashMode,
    appLanguage: AppLanguage,
    appCpuUsagePercent: Float?,
    showBuyNowButton: Boolean,
    settingsEnabled: Boolean = true,
    trialStatusText: String = "Settings locked",
    trialButtonText: String = "30 Day Trial",
    trialButtonEnabled: Boolean = true,
    onStartTrial: () -> Unit = {},
    onHapticsToggle: () -> Unit,
    onBeepToggle: () -> Unit,
    onBeatSoundModeChoice: (BeatSoundMode) -> Unit,
    onKeepScreenModeChoice: (KeepScreenMode) -> Unit,
    onMainColorChoice: (Int) -> Unit,
    onBackgroundColorChoice: (Int) -> Unit,
    onRingColorChoice: (Int) -> Unit,
    onBigRingModeChoice: (BigRingFlashMode) -> Unit,
    onLanguageChoice: (AppLanguage) -> Unit,
) {
    var buyNowPopupOpen by rememberSaveable { mutableStateOf(false) }
    val settingsScrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val watchSClass = minOf(maxWidth, maxHeight) <= 200.dp
        val contentHorizontalPadding = if (watchSClass) 8.dp else 12.dp
        val contentVerticalPadding = if (watchSClass) 12.dp else 18.dp
        val titleFontSize = if (watchSClass) 15.sp else 17.sp
        val sectionTitleFontSize = if (watchSClass) 11.sp else 12.sp
        val labelFontSize = if (watchSClass) 9.sp else 10.sp
        val choiceWidth = if (watchSClass) 38.dp else 42.dp
        val choiceHeight = if (watchSClass) 24.dp else 26.dp
        val tightSpacing = if (watchSClass) 3.dp else 4.dp
        val sectionSpacing = if (watchSClass) 7.dp else 9.dp
        val scrollIndicatorHeight = if (watchSClass) 104.dp else 118.dp

        Column(
            modifier = Modifier
                .verticalScroll(settingsScrollState)
                .padding(horizontal = contentHorizontalPadding, vertical = contentVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showBuyNowButton) {
                SettingsCommandButton(
                    text = "BUY NOW",
                    prominent = true,
                    modifier = Modifier
                        .width(if (watchSClass) 116.dp else 130.dp)
                        .height(if (watchSClass) 28.dp else 30.dp),
                    fontSize = if (watchSClass) 12.sp else 13.sp,
                    onClick = {
                        buyNowPopupOpen = true
                    },
                )

                Spacer(modifier = Modifier.height(if (watchSClass) 5.dp else 7.dp))
            }

            Text(
                text = appText.settings,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChoicePillButton(
                    text = appText.haptics,
                    selected = hapticsEnabled,
                    modifier = Modifier
                        .width(choiceWidth)
                        .height(choiceHeight),
                    enabled = settingsEnabled,
                    onClick = onHapticsToggle,
                )
                ChoicePillButton(
                    text = appText.beep,
                    selected = beepEnabled,
                    modifier = Modifier
                        .width(choiceWidth)
                        .height(choiceHeight),
                    enabled = settingsEnabled,
                    onClick = onBeepToggle,
                )
                ChoicePillButton(
                    text = appText.wood,
                    selected = beatSoundMode == BeatSoundMode.Wood,
                    modifier = Modifier
                        .width(choiceWidth)
                        .height(choiceHeight),
                    enabled = settingsEnabled,
                    onClick = { onBeatSoundModeChoice(BeatSoundMode.Wood) },
                )
                ChoicePillButton(
                    text = appText.bell,
                    selected = beatSoundMode == BeatSoundMode.Bell,
                    modifier = Modifier
                        .width(choiceWidth)
                        .height(choiceHeight),
                    enabled = settingsEnabled,
                    onClick = { onBeatSoundModeChoice(BeatSoundMode.Bell) },
                )
            }

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = appText.keepScreenOn,
                fontSize = labelFontSize,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(tightSpacing))

            KeepScreenModeButtons(
                selectedMode = keepScreenMode,
                appText = appText,
                enabled = settingsEnabled,
                onModeChoice = onKeepScreenModeChoice,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = appText.theme,
                fontSize = sectionTitleFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(tightSpacing))

            Text(
                text = appText.mainColor,
                fontSize = labelFontSize,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(tightSpacing))
            ColorPickerRow(
                selectedColorArgb = mainColorArgb,
                onColorChoice = onMainColorChoice,
                colorOptions = ThemeMainColorOptions,
                enabled = settingsEnabled,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = appText.backgroundColor,
                fontSize = labelFontSize,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(tightSpacing))
            ColorPickerRow(
                selectedColorArgb = backgroundColorArgb,
                onColorChoice = onBackgroundColorChoice,
                colorOptions = ThemeBackgroundColorOptions,
                enabled = settingsEnabled,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = appText.bigRing,
                fontSize = labelFontSize,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(tightSpacing))
            ColorPickerRow(
                selectedColorArgb = ringColorArgb,
                onColorChoice = onRingColorChoice,
                colorOptions = PulseColorOptions,
                enabled = settingsEnabled,
            )

            Spacer(modifier = Modifier.height(tightSpacing))

            BigRingModePicker(
                selectedMode = bigRingFlashMode,
                appLanguage = appLanguage,
                enabled = settingsEnabled,
                onModeChoice = onBigRingModeChoice,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = appText.language,
                fontSize = labelFontSize,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(tightSpacing))
            LanguagePicker(
                selectedLanguage = appLanguage,
                onLanguageChoice = onLanguageChoice,
                enabled = settingsEnabled,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            SettingsDiagnosticsSection(
                appText = appText,
                appCpuUsagePercent = appCpuUsagePercent,
                titleFontSize = sectionTitleFontSize,
                spacing = tightSpacing,
            )
        }

        SettingsScrollIndicator(
            scrollState = settingsScrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .width(4.dp)
                .height(scrollIndicatorHeight),
        )

        if (buyNowPopupOpen) {
            BuyNowChoicePopup(
                trialStatusText = trialStatusText,
                trialButtonText = trialButtonText,
                trialButtonEnabled = trialButtonEnabled,
                onStartTrial = {
                    onStartTrial()
                    buyNowPopupOpen = false
                },
                onDismiss = {
                    buyNowPopupOpen = false
                },
            )
        }
    }
}

@Composable
internal fun TuneSettingsPage(
    appText: AppText,
    a4ReferenceHz: Int,
    mainColorArgb: Int,
    backgroundColorArgb: Int,
    appLanguage: AppLanguage,
    appCpuUsagePercent: Float?,
    keepScreenMode: KeepScreenMode,
    showBuyNowButton: Boolean,
    settingsEnabled: Boolean,
    trialStatusText: String,
    trialButtonText: String,
    trialButtonEnabled: Boolean,
    onStartTrial: () -> Unit,
    onA4ReferenceHzChange: (Int) -> Unit,
    onKeepScreenModeChoice: (KeepScreenMode) -> Unit,
    onMainColorChoice: (Int) -> Unit,
    onBackgroundColorChoice: (Int) -> Unit,
    onLanguageChoice: (AppLanguage) -> Unit,
) {
    val settingsScrollState = rememberScrollState()
    var buyNowPopupOpen by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val watchSClass = minOf(maxWidth, maxHeight) <= 200.dp
        val contentHorizontalPadding = if (watchSClass) 8.dp else 12.dp
        val contentVerticalPadding = if (watchSClass) 14.dp else 20.dp
        val titleFontSize = if (watchSClass) 15.sp else 17.sp
        val sectionTitleFontSize = if (watchSClass) 11.sp else 12.sp
        val labelFontSize = if (watchSClass) 9.sp else 10.sp
        val tightSpacing = if (watchSClass) 3.dp else 4.dp
        val sectionSpacing = if (watchSClass) 8.dp else 11.dp
        val scrollIndicatorHeight = if (watchSClass) 104.dp else 118.dp

        Column(
            modifier = Modifier
                .verticalScroll(settingsScrollState)
                .padding(horizontal = contentHorizontalPadding, vertical = contentVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showBuyNowButton) {
                SettingsCommandButton(
                    text = "BUY NOW",
                    prominent = true,
                    modifier = Modifier
                        .width(if (watchSClass) 116.dp else 130.dp)
                        .height(if (watchSClass) 28.dp else 30.dp),
                    fontSize = if (watchSClass) 12.sp else 13.sp,
                    onClick = {
                        buyNowPopupOpen = true
                    },
                )

                Spacer(modifier = Modifier.height(if (watchSClass) 5.dp else 7.dp))
            }

            Text(
                text = appText.settings,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            A4ReferenceControl(
                label = appText.a4Reference,
                referenceHz = a4ReferenceHz,
                enabled = settingsEnabled,
                onReferenceHzChange = onA4ReferenceHzChange,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = appText.keepScreenOn,
                fontSize = labelFontSize,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (settingsEnabled) 1f else 0.34f),
            )

            Spacer(modifier = Modifier.height(tightSpacing))

            KeepScreenModeButtons(
                selectedMode = keepScreenMode,
                appText = appText,
                enabled = settingsEnabled,
                onModeChoice = onKeepScreenModeChoice,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = appText.theme,
                fontSize = sectionTitleFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(tightSpacing))

            Text(
                text = appText.mainColor,
                fontSize = labelFontSize,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(tightSpacing))
            ColorPickerRow(
                selectedColorArgb = mainColorArgb,
                onColorChoice = onMainColorChoice,
                colorOptions = ThemeMainColorOptions,
                enabled = settingsEnabled,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = appText.backgroundColor,
                fontSize = labelFontSize,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(tightSpacing))
            ColorPickerRow(
                selectedColorArgb = backgroundColorArgb,
                onColorChoice = onBackgroundColorChoice,
                colorOptions = ThemeBackgroundColorOptions,
                enabled = settingsEnabled,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = appText.language,
                fontSize = labelFontSize,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(tightSpacing))
            LanguagePicker(
                selectedLanguage = appLanguage,
                onLanguageChoice = onLanguageChoice,
                enabled = settingsEnabled,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            SettingsDiagnosticsSection(
                appText = appText,
                appCpuUsagePercent = appCpuUsagePercent,
                titleFontSize = sectionTitleFontSize,
                spacing = tightSpacing,
            )
        }

        SettingsScrollIndicator(
            scrollState = settingsScrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .width(4.dp)
                .height(scrollIndicatorHeight),
        )

        if (buyNowPopupOpen) {
            BuyNowChoicePopup(
                trialStatusText = trialStatusText,
                trialButtonText = trialButtonText,
                trialButtonEnabled = trialButtonEnabled,
                onStartTrial = {
                    onStartTrial()
                    buyNowPopupOpen = false
                },
                onDismiss = {
                    buyNowPopupOpen = false
                },
            )
        }
    }
}

@Composable
internal fun SettingsPage(
    appText: AppText,
    hapticsEnabled: Boolean,
    beepEnabled: Boolean,
    beatSoundMode: BeatSoundMode,
    keyDroneEnabled: Boolean,
    keyDroneVolumePercent: Int,
    tempoNudgeMs: Int,
    accentIntensityMode: AccentIntensityMode,
    accentIntensityRanges: List<AccentIntensityRange>,
    a4ReferenceHz: Int,
    keepScreenMode: KeepScreenMode,
    mainColorArgb: Int,
    backgroundColorArgb: Int,
    clockColorArgb: Int,
    clockImageIndex: Int,
    ringColorArgb: Int,
    bigRingFlashMode: BigRingFlashMode,
    appLanguage: AppLanguage,
    appCpuUsagePercent: Float?,
    compactSettings: Boolean = false,
    showBuyNowButton: Boolean = false,
    onHapticsToggle: () -> Unit,
    onBeepToggle: () -> Unit,
    onBeatSoundModeChoice: (BeatSoundMode) -> Unit,
    onKeyDroneToggle: () -> Unit,
    onKeyDroneVolumeChange: (Int) -> Unit,
    onTempoNudgeChange: (Int) -> Unit,
    onAccentIntensityModeChoice: (AccentIntensityMode) -> Unit,
    onAccentIntensityRangesChange: (List<AccentIntensityRange>) -> Unit,
    onA4ReferenceHzChange: (Int) -> Unit,
    onKeepScreenModeChoice: (KeepScreenMode) -> Unit,
    onMainColorChoice: (Int) -> Unit,
    onBackgroundColorChoice: (Int) -> Unit,
    onClockColorChoice: (Int) -> Unit,
    onClockImageChoice: (Int) -> Unit,
    onRingColorChoice: (Int) -> Unit,
    onBigRingModeChoice: (BigRingFlashMode) -> Unit,
    onLanguageChoice: (AppLanguage) -> Unit,
) {
    if (compactSettings) {
        SimpleSettingsPage(
            appText = appText,
            hapticsEnabled = hapticsEnabled,
            beepEnabled = beepEnabled,
            beatSoundMode = beatSoundMode,
            keepScreenMode = keepScreenMode,
            mainColorArgb = mainColorArgb,
            backgroundColorArgb = backgroundColorArgb,
            ringColorArgb = ringColorArgb,
            bigRingFlashMode = bigRingFlashMode,
            appLanguage = appLanguage,
            appCpuUsagePercent = appCpuUsagePercent,
            showBuyNowButton = showBuyNowButton,
            settingsEnabled = true,
            trialStatusText = "Settings unlocked",
            trialButtonText = "30 Day Trial",
            trialButtonEnabled = false,
            onHapticsToggle = onHapticsToggle,
            onBeepToggle = onBeepToggle,
            onBeatSoundModeChoice = onBeatSoundModeChoice,
            onKeepScreenModeChoice = onKeepScreenModeChoice,
            onMainColorChoice = onMainColorChoice,
            onBackgroundColorChoice = onBackgroundColorChoice,
            onRingColorChoice = onRingColorChoice,
            onBigRingModeChoice = onBigRingModeChoice,
            onLanguageChoice = onLanguageChoice,
        )
        return
    }

    var intensityPickerOpen by rememberSaveable { mutableStateOf(false) }
    val settingsScrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val watchSClass = minOf(maxWidth, maxHeight) <= 200.dp
        val contentHorizontalPadding = if (watchSClass) 8.dp else 12.dp
        val contentVerticalPadding = if (watchSClass) 14.dp else 20.dp
        val titleFontSize = if (watchSClass) 16.sp else 18.sp
        val sectionTitleFontSize = if (watchSClass) 12.sp else 13.sp
        val labelFontSize = if (watchSClass) 10.sp else 11.sp
        val choiceWidth = if (watchSClass) 38.dp else 42.dp
        val choiceHeight = if (watchSClass) 24.dp else 26.dp
        val tightSpacing = if (watchSClass) 3.dp else 4.dp
        val smallSpacing = if (watchSClass) 4.dp else 5.dp
        val sectionSpacing = if (watchSClass) 7.dp else 10.dp
        val scrollIndicatorHeight = if (watchSClass) 104.dp else 118.dp

        Column(
            modifier = Modifier
                .verticalScroll(settingsScrollState)
                .padding(horizontal = contentHorizontalPadding, vertical = contentVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = appText.settings,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(smallSpacing))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChoicePillButton(
                    text = appText.haptics,
                    selected = hapticsEnabled,
                    modifier = Modifier
                        .width(choiceWidth)
                        .height(choiceHeight),
                    onClick = onHapticsToggle,
                )

                ChoicePillButton(
                    text = appText.beep,
                    selected = beepEnabled,
                    modifier = Modifier
                        .width(choiceWidth)
                        .height(choiceHeight),
                    onClick = onBeepToggle,
                )

                ChoicePillButton(
                    text = appText.wood,
                    selected = beatSoundMode == BeatSoundMode.Wood,
                    modifier = Modifier
                        .width(choiceWidth)
                        .height(choiceHeight),
                    onClick = { onBeatSoundModeChoice(BeatSoundMode.Wood) },
                )

                ChoicePillButton(
                    text = appText.bell,
                    selected = beatSoundMode == BeatSoundMode.Bell,
                    modifier = Modifier
                        .width(choiceWidth)
                        .height(choiceHeight),
                    onClick = { onBeatSoundModeChoice(BeatSoundMode.Bell) },
                )
            }

            Spacer(modifier = Modifier.height(if (watchSClass) 5.dp else 7.dp))

            Text(
                text = appText.intensityTitle.replace('\n', ' '),
                fontSize = labelFontSize,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(tightSpacing))

            AccentIntensityPicker(
                accentIntensityRanges = accentIntensityRanges,
                appLanguage = appLanguage,
                onClick = {
                    intensityPickerOpen = true
                },
            )

            Spacer(modifier = Modifier.height(if (watchSClass) 5.dp else 7.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                PercentStepperControl(
                    label = appText.droneVolume,
                    valuePercent = keyDroneVolumePercent,
                    onValueChange = onKeyDroneVolumeChange,
                )

                ChoicePillButton(
                    text = appText.drone,
                    selected = keyDroneEnabled,
                    modifier = Modifier
                        .width(42.dp)
                        .width(choiceWidth)
                        .height(choiceHeight),
                    onClick = onKeyDroneToggle,
                )
            }

            Spacer(modifier = Modifier.height(sectionSpacing))

            A4ReferenceControl(
                label = appText.a4Reference,
                referenceHz = a4ReferenceHz,
                onReferenceHzChange = onA4ReferenceHzChange,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            MillisecondStepperControl(
                label = appText.visualNudge,
                valueMs = tempoNudgeMs,
                onValueChange = onTempoNudgeChange,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = appText.keepScreenOn,
                fontSize = labelFontSize,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(tightSpacing))

            KeepScreenModeButtons(
                selectedMode = keepScreenMode,
                appText = appText,
                onModeChoice = onKeepScreenModeChoice,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = appText.theme,
                fontSize = sectionTitleFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(smallSpacing))

            Text(
                text = appText.mainColor,
                fontSize = labelFontSize,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(tightSpacing))

            ColorPickerRow(
                selectedColorArgb = mainColorArgb,
                onColorChoice = onMainColorChoice,
                colorOptions = ThemeMainColorOptions,
            )

            Spacer(modifier = Modifier.height(if (watchSClass) 6.dp else 8.dp))

            Text(
                text = appText.backgroundColor,
                fontSize = labelFontSize,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(tightSpacing))

            ColorPickerRow(
                selectedColorArgb = backgroundColorArgb,
                onColorChoice = onBackgroundColorChoice,
                colorOptions = ThemeBackgroundColorOptions,
            )

            Spacer(modifier = Modifier.height(if (watchSClass) 6.dp else 8.dp))

            Text(
                text = appText.bigRing,
                fontSize = labelFontSize,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(tightSpacing))

            ColorPickerRow(
                selectedColorArgb = ringColorArgb,
                onColorChoice = onRingColorChoice,
            )

            Spacer(modifier = Modifier.height(smallSpacing))

            BigRingModePicker(
                selectedMode = bigRingFlashMode,
                appLanguage = appLanguage,
                onModeChoice = onBigRingModeChoice,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            Text(
                text = appText.clock,
                fontSize = sectionTitleFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(smallSpacing))

            Text(
                text = appText.handColor,
                fontSize = labelFontSize,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(tightSpacing))

            ColorPickerRow(
                selectedColorArgb = clockColorArgb,
                onColorChoice = onClockColorChoice,
            )

            Spacer(modifier = Modifier.height(if (watchSClass) 6.dp else 8.dp))

            Text(
                text = appText.clockImage,
                fontSize = labelFontSize,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(tightSpacing))

            ClockImagePicker(
                selectedIndex = clockImageIndex,
                appLanguage = appLanguage,
                onClockImageChoice = onClockImageChoice,
            )

            Spacer(modifier = Modifier.height(if (watchSClass) 6.dp else 8.dp))

            Text(
                text = appText.language,
                fontSize = labelFontSize,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(tightSpacing))

            LanguagePicker(
                selectedLanguage = appLanguage,
                onLanguageChoice = onLanguageChoice,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            SettingsDiagnosticsSection(
                appText = appText,
                appCpuUsagePercent = appCpuUsagePercent,
                titleFontSize = sectionTitleFontSize,
                spacing = tightSpacing,
            )
        }

        SettingsScrollIndicator(
            scrollState = settingsScrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .width(4.dp)
                .height(scrollIndicatorHeight),
        )

        if (intensityPickerOpen) {
            AccentIntensityChoicePopup(
                title = appText.intensityTitle,
                selectedMode = accentIntensityMode,
                accentIntensityRanges = accentIntensityRanges,
                appLanguage = appLanguage,
                dismissText = appText.done,
                onModeChoice = { mode ->
                    onAccentIntensityModeChoice(mode)
                },
                onValueChange = { mode, value ->
                    onAccentIntensityRangesChange(
                        accentIntensityRanges.withRangeFor(
                            mode,
                            accentIntensityRanges.rangeFor(mode).copy(valuePercent = value),
                        ),
                    )
                },
                onDismiss = {
                    intensityPickerOpen = false
                },
            )
        }
    }
}

@Composable
internal fun SettingsScrollIndicator(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val maxScroll = scrollState.maxValue
    val trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = if (maxScroll > 0) 0.18f else 0f)
    val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = if (maxScroll > 0) 0.78f else 0f)

    Canvas(modifier = modifier) {
        if (maxScroll <= 0) return@Canvas

        val strokeWidth = size.width
        val thumbHeight = max(22.dp.toPx(), size.height * 0.26f)
        val scrollProgress = (scrollState.value.toFloat() / maxScroll.toFloat()).coerceIn(0f, 1f)
        val thumbTop = (size.height - thumbHeight) * scrollProgress
        val centerX = size.width / 2f

        drawLine(
            color = trackColor,
            start = Offset(centerX, 0f),
            end = Offset(centerX, size.height),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = thumbColor,
            start = Offset(centerX, thumbTop),
            end = Offset(centerX, thumbTop + thumbHeight),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun CpuUsageReadout(
    label: String,
    cpuUsagePercent: Float?,
) {
    Row(
        modifier = Modifier
            .width(118.dp)
            .height(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = cpuUsagePercent.formatCpuUsagePercent(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SettingsDiagnosticsSection(
    appText: AppText,
    appCpuUsagePercent: Float?,
    titleFontSize: TextUnit,
    spacing: androidx.compose.ui.unit.Dp,
) {
    Text(
        text = appText.diagnostics,
        fontSize = titleFontSize,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(spacing))

    CpuUsageReadout(
        label = appText.appCpu,
        cpuUsagePercent = appCpuUsagePercent,
    )
}

@Composable
private fun A4ReferenceControl(
    label: String,
    referenceHz: Int,
    enabled: Boolean = true,
    onReferenceHzChange: (Int) -> Unit,
) {
    val enabledAlpha = if (enabled) 1f else 0.34f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = enabledAlpha),
        )

        Spacer(modifier = Modifier.height(3.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsCommandButton(
                text = "-",
                modifier = Modifier
                    .width(24.dp)
                    .height(22.dp),
                fontSize = 11.sp,
                enabled = enabled,
                onClick = {
                    onReferenceHzChange((referenceHz - 1).coerceAtLeast(MIN_A4_REFERENCE_HZ))
                },
            )

            Text(
                text = "A $referenceHz Hz",
                modifier = Modifier.width(72.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = enabledAlpha),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            SettingsCommandButton(
                text = "+",
                modifier = Modifier
                    .width(24.dp)
                    .height(22.dp),
                fontSize = 11.sp,
                enabled = enabled,
                onClick = {
                    onReferenceHzChange((referenceHz + 1).coerceAtMost(MAX_A4_REFERENCE_HZ))
                },
            )
        }
    }
}

@Composable
private fun PercentStepperControl(
    label: String,
    valuePercent: Int,
    onValueChange: (Int) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.height(3.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsCommandButton(
                text = "-",
                modifier = Modifier
                    .width(24.dp)
                    .height(22.dp),
                fontSize = 11.sp,
                onClick = { onValueChange(-5) },
            )

            Text(
                text = "$valuePercent%",
                modifier = Modifier.width(54.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            SettingsCommandButton(
                text = "+",
                modifier = Modifier
                    .width(24.dp)
                    .height(22.dp),
                fontSize = 11.sp,
                onClick = { onValueChange(5) },
            )
        }
    }
}

@Composable
private fun MillisecondStepperControl(
    label: String,
    valueMs: Int,
    onValueChange: (Int) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.height(3.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsCommandButton(
                text = "-",
                modifier = Modifier
                    .width(24.dp)
                    .height(22.dp),
                fontSize = 11.sp,
                onClick = {
                    if (valueMs > MIN_TEMPO_NUDGE_MS) {
                        onValueChange(-TEMPO_NUDGE_STEP_MS)
                    }
                },
            )

            Text(
                text = "${valueMs}ms",
                modifier = Modifier.width(62.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            SettingsCommandButton(
                text = "+",
                modifier = Modifier
                    .width(24.dp)
                    .height(22.dp),
                fontSize = 11.sp,
                onClick = {
                    if (valueMs < MAX_TEMPO_NUDGE_MS) {
                        onValueChange(TEMPO_NUDGE_STEP_MS)
                    }
                },
            )
        }
    }
}

@Composable
private fun AccentIntensityPicker(
    accentIntensityRanges: List<AccentIntensityRange>,
    appLanguage: AppLanguage,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)

    Box(
        modifier = Modifier
            .width(166.dp)
            .height(36.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.84f), shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIntensityChoices.forEach { choice ->
                val value = accentIntensityRanges.rangeFor(choice.mode).valuePercent
                Column(
                    modifier = Modifier.width(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = choice.labelFor(appLanguage),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Text(
                        text = value.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AccentIntensityChoicePopup(
    title: String,
    selectedMode: AccentIntensityMode,
    accentIntensityRanges: List<AccentIntensityRange>,
    appLanguage: AppLanguage,
    dismissText: String,
    onModeChoice: (AccentIntensityMode) -> Unit,
    onValueChange: (AccentIntensityMode, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var horizontalDrag by remember { mutableFloatStateOf(0f) }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .pointerInput(onDismiss) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        horizontalDrag = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        horizontalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (abs(horizontalDrag) > 60f) {
                            onDismiss()
                        }
                        horizontalDrag = 0f
                    },
                    onDragCancel = {
                        horizontalDrag = 0f
                    },
                )
            }
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        SettingsDoneButton(
            text = dismissText,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 0.dp, end = 0.dp),
            onClick = onDismiss,
        )

        val selectedRange = accentIntensityRanges.rangeFor(selectedMode)

        Column(
            modifier = Modifier
                .width(160.dp)
                .padding(top = 13.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                modifier = Modifier.width(112.dp),
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(7.dp))

            AccentIntensityChoices.forEach { choice ->
                AccentIntensityTypePill(
                    choice = choice,
                    range = accentIntensityRanges.rangeFor(choice.mode),
                    selected = selectedMode == choice.mode,
                    appLanguage = appLanguage,
                    onClick = { onModeChoice(choice.mode) },
                )

                Spacer(modifier = Modifier.height(3.dp))
            }

            Spacer(modifier = Modifier.height(1.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsCommandButton(
                    text = "-",
                    modifier = Modifier
                        .width(34.dp)
                        .height(26.dp),
                    fontSize = 14.sp,
                    onClick = {
                        onValueChange(
                            selectedMode,
                            (selectedRange.valuePercent - 1).coerceAtLeast(selectedRange.minPercent),
                        )
                    },
                )

                SettingsCommandButton(
                    text = "+",
                    modifier = Modifier
                        .width(34.dp)
                        .height(26.dp),
                    fontSize = 14.sp,
                    onClick = {
                        onValueChange(
                            selectedMode,
                            (selectedRange.valuePercent + 1).coerceAtMost(selectedRange.maxPercent),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AccentIntensityTypePill(
    choice: AccentIntensityChoice,
    range: AccentIntensityRange,
    selected: Boolean,
    appLanguage: AppLanguage,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
    } else {
        Color.Black.copy(alpha = 0.34f)
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.24f)
    }

    Column(
        modifier = Modifier
            .width(142.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .width(138.dp)
                .height(26.dp)
                .clip(shape)
                .background(backgroundColor, shape)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = shape,
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = choice.labelFor(appLanguage),
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.84f)
                },
                maxLines = 1,
            )

            Text(
                text = "${range.valuePercent}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )

            Text(
                text = "${range.maxPercent}-${range.minPercent}",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun KeepScreenModeButtons(
    selectedMode: KeepScreenMode,
    appText: AppText,
    onModeChoice: (KeepScreenMode) -> Unit,
    enabled: Boolean = true,
) {
    val choices = listOf(
        KeepScreenMode.AppOpen to appText.keepScreenAppOpen,
        KeepScreenMode.Playing to appText.keepScreenPlaying,
        KeepScreenMode.WatchTimeout to appText.keepScreenWatchTimeout,
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        choices.forEach { (mode, label) ->
            ChoicePillButton(
                text = label,
                selected = selectedMode == mode,
                modifier = Modifier
                    .width(42.dp)
                    .height(26.dp),
                enabled = enabled,
                onClick = { onModeChoice(mode) },
            )
        }
    }
}

@Composable
private fun BuyNowChoicePopup(
    trialStatusText: String,
    trialButtonText: String,
    trialButtonEnabled: Boolean,
    onStartTrial: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Choose Upgrade",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Text(
                text = trialStatusText,
                modifier = Modifier.width(144.dp),
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            SettingsCommandButton(
                text = trialButtonText,
                modifier = Modifier
                    .width(136.dp)
                    .height(30.dp),
                fontSize = 11.sp,
                prominent = true,
                enabled = trialButtonEnabled,
                onClick = onStartTrial,
            )

            SettingsCommandButton(
                text = "Buy this app",
                modifier = Modifier
                    .width(136.dp)
                    .height(30.dp),
                fontSize = 11.sp,
                prominent = true,
                onClick = onDismiss,
            )

            SettingsCommandButton(
                text = "Pulse Pro",
                modifier = Modifier
                    .width(136.dp)
                    .height(30.dp),
                fontSize = 11.sp,
                prominent = true,
                onClick = onDismiss,
            )

            SettingsCommandButton(
                text = "Done",
                modifier = Modifier
                    .width(72.dp)
                    .height(26.dp),
                fontSize = 10.sp,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun SettingsDoneButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCommandButton(
        text = text,
        modifier = modifier
            .width(56.dp)
            .height(24.dp),
        fontSize = 9.sp,
        onClick = onClick,
    )
}

@Composable
private fun SettingsCommandButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp,
    selected: Boolean = false,
    prominent: Boolean = false,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    val enabledAlpha = if (enabled) 1f else 0.34f
    val backgroundColor = when {
        prominent -> MaterialTheme.colorScheme.primary.copy(alpha = enabledAlpha)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.34f * enabledAlpha)
        else -> Color.Black.copy(alpha = 0.42f * enabledAlpha)
    }
    val borderColor = if (selected || prominent) {
        MaterialTheme.colorScheme.primary.copy(alpha = enabledAlpha)
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.24f * enabledAlpha)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(
                width = if (selected || prominent) 2.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = if (prominent) {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = enabledAlpha)
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = enabledAlpha)
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
