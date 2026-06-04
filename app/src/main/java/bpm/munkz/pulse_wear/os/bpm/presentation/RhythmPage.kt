package bpm.munkz.pulse_wear.os.bpm.presentation

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
@Composable
internal fun TapTempoFree(
    appText: AppText,
    bpm: Int,
    musicalKey: String,
    beatsPerMeasure: Int,
    subdivisionCount: Int,
    beatFlash: Boolean,
    isAccentFlash: Boolean,
    isRunning: Boolean,
    showAudioTools: Boolean = true,
    showRhythmChoices: Boolean = true,
    onOpenTuner: () -> Unit,
    onOpenSpectrum: () -> Unit,
    onTapTempo: () -> Unit,
    onDecrease: () -> Unit,
    onDecreaseLarge: () -> Unit,
    onIncrease: () -> Unit,
    onIncreaseLarge: () -> Unit,
    onToggleRunning: () -> Unit,
    onTimeSignatureClick: () -> Unit,
    onKeyClick: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val watchSClass = minOf(maxWidth, maxHeight) <= 200.dp
        val audioToolOffsetY = if (watchSClass) (-32).dp else (-36).dp
        val bottomChoiceHorizontalPadding = if (watchSClass) 14.dp else 20.dp
        val bottomChoiceBottomPadding = if (watchSClass) 50.dp else 57.dp
        val bottomChoiceWidth = if (watchSClass) 50.dp else 54.dp
        val bottomChoiceHeight = if (watchSClass) 22.dp else 24.dp
        val bottomChoiceFontSize = if (watchSClass) 9.sp else 10.sp
        val startButtonWidth = if (watchSClass) 94.dp else 104.dp
        val startButtonHeight = if (watchSClass) 27.dp else 30.dp
        val startButtonFontSize = if (watchSClass) 13.sp else 15.sp

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            TapTempoControls(
                appText = appText,
                bpm = bpm,
                beatFlash = beatFlash,
                isAccentFlash = isAccentFlash,
                onTapTempo = onTapTempo,
                onDecrease = onDecrease,
                onDecreaseLarge = onDecreaseLarge,
                onIncrease = onIncrease,
                onIncreaseLarge = onIncreaseLarge,
                readoutNumberFontSize = if (watchSClass) 32.sp else 38.sp,
                readoutLabelFontSize = if (watchSClass) 10.sp else 12.sp,
                readoutPulseBoxSize = if (watchSClass) 30.dp else 34.dp,
                readoutSpacing = if (watchSClass) 5.dp else 7.dp,
                readoutBottomSpacing = if (watchSClass) 6.dp else 8.dp,
                adjustRowSpacing = if (watchSClass) 8.dp else 12.dp,
                tapButtonSize = if (watchSClass) 76.dp else 88.dp,
                tapButtonFontSize = if (watchSClass) 13.sp else 15.sp,
                adjustButtonWidth = if (watchSClass) 36.dp else 42.dp,
                adjustButtonHeight = if (watchSClass) 27.dp else 30.dp,
                adjustButtonFontSize = if (watchSClass) 13.sp else 15.sp,
                footerTopSpacing = if (watchSClass) 4.dp else 6.dp,
                prominentTempoButtons = true,
            ) {
                GlassCommandButton(
                    text = if (isRunning) appText.stopUpper else appText.startUpper,
                    modifier = Modifier
                        .width(startButtonWidth)
                        .height(startButtonHeight),
                    fontSize = startButtonFontSize,
                    selected = isRunning,
                    prominent = true,
                    onClick = onToggleRunning,
                )
            }

            if (showAudioTools) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = audioToolOffsetY),
                    contentAlignment = Alignment.Center,
                ) {
                    AudioToolButtons(
                        appText = appText,
                        onOpenTuner = onOpenTuner,
                        onOpenSpectrum = onOpenSpectrum,
                        modifier = if (watchSClass) Modifier
                            .width(156.dp)
                            .height(21.dp) else Modifier,
                    )
                }
            }

            if (showRhythmChoices) {
                GlassCommandButton(
                    text = "$beatsPerMeasure/4 x$subdivisionCount",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = bottomChoiceHorizontalPadding, bottom = bottomChoiceBottomPadding)
                        .width(bottomChoiceWidth)
                        .height(bottomChoiceHeight),
                    fontSize = bottomChoiceFontSize,
                    selected = false,
                    prominent = false,
                    onClick = onTimeSignatureClick,
                )

                GlassCommandButton(
                    text = musicalKey,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = bottomChoiceHorizontalPadding, bottom = bottomChoiceBottomPadding)
                        .width(bottomChoiceWidth)
                        .height(bottomChoiceHeight),
                    fontSize = bottomChoiceFontSize,
                    selected = false,
                    prominent = false,
                    onClick = onKeyClick,
                )
            }
        }
    }
}

@Composable
private fun TapTempoControls(
    appText: AppText,
    bpm: Int,
    beatFlash: Boolean,
    isAccentFlash: Boolean,
    onTapTempo: () -> Unit,
    onDecrease: () -> Unit,
    onDecreaseLarge: () -> Unit,
    onIncrease: () -> Unit,
    onIncreaseLarge: () -> Unit,
    readoutOffsetY: Dp = 0.dp,
    readoutBottomSpacing: Dp = 8.dp,
    readoutNumberFontSize: TextUnit = 38.sp,
    readoutLabelFontSize: TextUnit = 12.sp,
    readoutPulseBoxSize: Dp = 34.dp,
    readoutSpacing: Dp = 7.dp,
    adjustRowOffsetY: Dp = 0.dp,
    adjustRowSpacing: Dp = 12.dp,
    prominentTempoButtons: Boolean = false,
    tapButtonSize: Dp = if (prominentTempoButtons) 88.dp else 98.dp,
    tapButtonFontSize: TextUnit = if (prominentTempoButtons) 15.sp else 20.sp,
    adjustButtonWidth: Dp = 42.dp,
    adjustButtonHeight: Dp = 30.dp,
    adjustButtonFontSize: TextUnit = 15.sp,
    footerTopSpacing: Dp = 6.dp,
    header: @Composable () -> Unit = {},
    footer: @Composable () -> Unit,
) {
    BeatPulsePage {
        BeatTempoReadout(
            bpm = bpm,
            beatFlash = beatFlash,
            isAccentFlash = isAccentFlash,
            numberFontSize = readoutNumberFontSize,
            labelFontSize = readoutLabelFontSize,
            pulseBoxSize = readoutPulseBoxSize,
            spacing = readoutSpacing,
            modifier = Modifier.offset(y = readoutOffsetY),
        )

        Spacer(modifier = Modifier.height(readoutBottomSpacing))

        header()

        Row(
            modifier = Modifier.offset(y = adjustRowOffsetY),
            horizontalArrangement = Arrangement.spacedBy(adjustRowSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TempoAdjustButton(
                text = "-",
                onClick = onDecrease,
                onLongClick = onDecreaseLarge,
                onLongClickLabel = appText.decreaseBpmBy5,
                prominent = prominentTempoButtons,
                width = adjustButtonWidth,
                height = adjustButtonHeight,
                fontSize = adjustButtonFontSize,
            )

            FastTapTempoButton(
                text = appText.tap,
                onTap = onTapTempo,
                prominent = prominentTempoButtons,
                size = tapButtonSize,
                fontSize = tapButtonFontSize,
            )

            TempoAdjustButton(
                text = "+",
                onClick = onIncrease,
                onLongClick = onIncreaseLarge,
                onLongClickLabel = appText.increaseBpmBy5,
                prominent = prominentTempoButtons,
                width = adjustButtonWidth,
                height = adjustButtonHeight,
                fontSize = adjustButtonFontSize,
            )
        }

        Spacer(modifier = Modifier.height(footerTopSpacing))

        footer()
    }
}

@Composable
internal fun TapTempoPopup(
    appText: AppText,
    bpm: Int,
    beatFlash: Boolean,
    isAccentFlash: Boolean,
    isRunning: Boolean,
    onOpenTuner: () -> Unit,
    onOpenSpectrum: () -> Unit,
    onTapTempo: () -> Unit,
    onDecrease: () -> Unit,
    onDecreaseLarge: () -> Unit,
    onIncrease: () -> Unit,
    onIncreaseLarge: () -> Unit,
    onToggleRunning: () -> Unit,
    onDismiss: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        val watchSClass = minOf(maxWidth, maxHeight) <= 200.dp
        TapTempoControls(
            appText = appText,
            bpm = bpm,
            beatFlash = beatFlash,
            isAccentFlash = isAccentFlash,
            onTapTempo = onTapTempo,
            onDecrease = onDecrease,
            onDecreaseLarge = onDecreaseLarge,
            onIncrease = onIncrease,
            onIncreaseLarge = onIncreaseLarge,
            readoutOffsetY = if (watchSClass) (-2).dp else 0.dp,
            readoutBottomSpacing = 0.dp,
            readoutNumberFontSize = if (watchSClass) 32.sp else 38.sp,
            readoutLabelFontSize = if (watchSClass) 10.sp else 12.sp,
            readoutPulseBoxSize = if (watchSClass) 30.dp else 34.dp,
            readoutSpacing = if (watchSClass) 5.dp else 7.dp,
            adjustRowOffsetY = if (watchSClass) (-5).dp else (-6).dp,
            adjustRowSpacing = if (watchSClass) 8.dp else 12.dp,
            tapButtonSize = if (watchSClass) 74.dp else 88.dp,
            tapButtonFontSize = if (watchSClass) 13.sp else 15.sp,
            adjustButtonWidth = if (watchSClass) 36.dp else 42.dp,
            adjustButtonHeight = if (watchSClass) 27.dp else 30.dp,
            adjustButtonFontSize = if (watchSClass) 13.sp else 15.sp,
            footerTopSpacing = if (watchSClass) 4.dp else 6.dp,
            prominentTempoButtons = true,
            header = {
                AudioToolButtons(
                    appText = appText,
                    onOpenTuner = onOpenTuner,
                    onOpenSpectrum = onOpenSpectrum,
                    modifier = Modifier
                        .width(if (watchSClass) 154.dp else 164.dp)
                        .height(if (watchSClass) 21.dp else 22.dp),
                )

                Spacer(modifier = Modifier.height(0.dp))

            },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(if (watchSClass) 3.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassCommandButton(
                    text = if (isRunning) appText.stopUpper else appText.startUpper,
                    modifier = Modifier
                        .width(if (watchSClass) 94.dp else 104.dp)
                        .height(if (watchSClass) 27.dp else 30.dp),
                    fontSize = if (watchSClass) 13.sp else 15.sp,
                    selected = isRunning,
                    prominent = true,
                    onClick = onToggleRunning,
                )

                GlassCommandButton(
                    text = appText.done,
                    modifier = Modifier
                        .width(if (watchSClass) 56.dp else 62.dp)
                        .height(if (watchSClass) 27.dp else 30.dp),
                    fontSize = if (watchSClass) 9.sp else 10.sp,
                    selected = false,
                    prominent = false,
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
internal fun RhythmSetupPage(
    appText: AppText,
    bpm: Int,
    beatsPerMeasure: Int,
    subdivisionCount: Int,
    beatAccentTypes: List<BeatAccentType>,
    currentBeatIndex: Int,
    currentSubdivisionIndex: Int,
    beatFlash: Boolean,
    isRunning: Boolean,
    beatClockStartedAtMs: Long,
    tempoNudgeMs: Int,
    playbackStartedAtMs: Long,
    beatVisualsEnabled: Boolean,
    beatRingVisible: Boolean,
    onEditRhythm: () -> Unit,
    onToggleRunning: () -> Unit,
    onBpmClick: () -> Unit,
    onTempoNudge: (Int) -> Unit,
    onOpenTuner: () -> Unit,
    onOpenSpectrum: () -> Unit,
) {
    var tempoNudgeAckStep by remember { mutableIntStateOf(0) }
    var tempoNudgeAckSerial by remember { mutableIntStateOf(0) }
    val acknowledgeTempoNudge = { step: Int ->
        onTempoNudge(step)
        tempoNudgeAckStep = step
        tempoNudgeAckSerial += 1
    }

    LaunchedEffect(tempoNudgeAckSerial) {
        if (tempoNudgeAckSerial > 0) {
            val activeSerial = tempoNudgeAckSerial
            delay(650L)
            if (tempoNudgeAckSerial == activeSerial) {
                tempoNudgeAckStep = 0
            }
        }
    }

    val beatVisualState = rememberRhythmBeatVisualState(
        isRunning = isRunning,
        animationEnabled = beatVisualsEnabled,
        bpm = bpm,
        beatsPerMeasure = beatsPerMeasure,
        subdivisionCount = subdivisionCount,
        beatClockStartedAtMs = beatClockStartedAtMs,
        fallbackBeatIndex = currentBeatIndex,
        fallbackSubdivisionIndex = currentSubdivisionIndex,
        fallbackBeatFlash = beatFlash,
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        val watchSClass = minOf(maxWidth, maxHeight) <= 200.dp
        RhythmLiveCanvas(
            appText = appText,
            bpm = bpm,
            beatsPerMeasure = beatsPerMeasure,
            subdivisionCount = subdivisionCount,
            beatAccentTypes = beatAccentTypes,
            currentBeatIndex = beatVisualState.currentBeatIndex,
            currentSubdivisionIndex = beatVisualState.currentSubdivisionIndex,
            beatFlash = beatVisualState.beatFlash,
            isRunning = isRunning,
            playbackStartedAtMs = playbackStartedAtMs,
            beatRingVisible = beatRingVisible,
            tempoNudgeMs = tempoNudgeMs,
            modifier = Modifier.fillMaxSize(),
            centerContentWidth = if (watchSClass) 204.dp else 236.dp,
            bpmFontSize = if (watchSClass) 28.sp else 32.sp,
            controlRowWidth = if (watchSClass) 120.dp else 132.dp,
            beatTextWidth = if (watchSClass) 50.dp else 58.dp,
            beatTextFontSize = if (watchSClass) 17.sp else 20.sp,
            audioButtonHeight = if (watchSClass) 20.dp else 21.dp,
            audioButtonFontSize = if (watchSClass) 9.sp else 10.sp,
            startButtonWidth = if (watchSClass) 100.dp else 112.dp,
            startButtonHeight = if (watchSClass) 31.dp else 36.dp,
            startButtonFontSize = if (watchSClass) 13.sp else 15.sp,
            editButtonWidth = if (watchSClass) 54.dp else 60.dp,
            editButtonHeight = if (watchSClass) 22.dp else 24.dp,
            editButtonFontSize = if (watchSClass) 10.sp else 12.sp,
            centerVerticalSpacing = if (watchSClass) 6.dp else 8.dp,
            bottomButtonSpacing = if (watchSClass) 4.dp else 5.dp,
            onBpmClick = onBpmClick,
            onEdit = onEditRhythm,
            onToggleRunning = onToggleRunning,
            onTempoNudge = acknowledgeTempoNudge,
            onOpenTuner = onOpenTuner,
            onOpenSpectrum = onOpenSpectrum,
        )

        if (tempoNudgeAckStep != 0) {
            Text(
                text = if (tempoNudgeAckStep > 0) {
                    "+${tempoNudgeAckStep}ms"
                } else {
                    "${tempoNudgeAckStep}ms"
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.86f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun RhythmLiveCanvas(
    appText: AppText,
    bpm: Int,
    beatsPerMeasure: Int,
    subdivisionCount: Int,
    beatAccentTypes: List<BeatAccentType>,
    currentBeatIndex: Int,
    currentSubdivisionIndex: Int,
    beatFlash: Boolean,
    isRunning: Boolean,
    playbackStartedAtMs: Long,
    tempoNudgeMs: Int,
    modifier: Modifier = Modifier,
    beatRingVisible: Boolean = true,
    centerContentWidth: Dp = 236.dp,
    bpmFontSize: TextUnit = 32.sp,
    controlRowWidth: Dp = 132.dp,
    beatTextWidth: Dp = 58.dp,
    beatTextFontSize: TextUnit = 20.sp,
    audioButtonHeight: Dp = 21.dp,
    audioButtonFontSize: TextUnit = 10.sp,
    startButtonWidth: Dp = 112.dp,
    startButtonHeight: Dp = 36.dp,
    startButtonFontSize: TextUnit = 15.sp,
    editButtonWidth: Dp = 60.dp,
    editButtonHeight: Dp = 24.dp,
    editButtonFontSize: TextUnit = 12.sp,
    centerVerticalSpacing: Dp = 8.dp,
    bottomButtonSpacing: Dp = 5.dp,
    onBpmClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleRunning: () -> Unit,
    onTempoNudge: (Int) -> Unit,
    onOpenTuner: () -> Unit,
    onOpenSpectrum: () -> Unit,
) {
    BigPulseCircleSelector(
        beatsPerMeasure = beatsPerMeasure,
        beatAccentTypes = beatAccentTypes,
        currentBeatIndex = currentBeatIndex,
        beatFlash = beatRingVisible && beatFlash,
        modifier = modifier,
        centerContentWidth = centerContentWidth,
        beatRingVisible = beatRingVisible,
        onBeatAccentTypeCycle = null,
        tempoNudgeMs = tempoNudgeMs,
        onTempoNudge = onTempoNudge,
        bottomContent = {
            RhythmElapsedTimer(
                isRunning = isRunning,
                playbackStartedAtMs = playbackStartedAtMs,
            )
        },
    ) {
        Box(
            modifier = Modifier.width(centerContentWidth),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "$bpm",
                    modifier = Modifier.clickable(onClick = onBpmClick),
                    fontSize = bpmFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(centerVerticalSpacing))

                Row(
                    modifier = Modifier.width(controlRowWidth),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassCommandButton(
                        text = appText.tuner,
                        modifier = Modifier
                            .width(38.dp)
                            .height(audioButtonHeight),
                        fontSize = audioButtonFontSize,
                        selected = false,
                        prominent = false,
                        onClick = onOpenTuner,
                    )

                    Text(
                        text = "$currentBeatIndex/$beatsPerMeasure",
                        modifier = Modifier.width(beatTextWidth),
                        fontSize = beatTextFontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )

                    GlassCommandButton(
                        text = "Spect",
                        modifier = Modifier
                            .width(42.dp)
                            .height(audioButtonHeight),
                        fontSize = audioButtonFontSize,
                        selected = false,
                        prominent = false,
                        onClick = onOpenSpectrum,
                    )
                }

                TempoPushSubdivisionRow(
                    subdivisionCount = subdivisionCount,
                    currentSubdivisionIndex = currentSubdivisionIndex,
                    tempoNudgeMs = tempoNudgeMs,
                    onTempoNudge = onTempoNudge,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(bottomButtonSpacing),
                ) {
                    GlassCommandButton(
                        text = if (isRunning) appText.stopUpper else appText.startUpper,
                        modifier = Modifier
                            .width(startButtonWidth)
                            .height(startButtonHeight),
                        fontSize = startButtonFontSize,
                        selected = isRunning,
                        prominent = true,
                        onClick = onToggleRunning,
                    )

                    GlassCommandButton(
                        text = appText.edit,
                        modifier = Modifier
                            .width(editButtonWidth)
                            .height(editButtonHeight),
                        fontSize = editButtonFontSize,
                        onClick = onEdit,
                    )
                }
            }
        }
    }
}

@Composable
internal fun RhythmElapsedTimer(
    isRunning: Boolean,
    playbackStartedAtMs: Long,
    modifier: Modifier = Modifier,
) {
    var elapsedMs by remember(isRunning, playbackStartedAtMs) {
        mutableLongStateOf(elapsedTimerMs(isRunning, playbackStartedAtMs))
    }

    LaunchedEffect(isRunning, playbackStartedAtMs) {
        if (!isRunning || playbackStartedAtMs <= 0L) {
            elapsedMs = 0L
            return@LaunchedEffect
        }

        while (true) {
            elapsedMs = elapsedTimerMs(isRunning = true, playbackStartedAtMs = playbackStartedAtMs)
            delay(250)
        }
    }

    if (!isRunning && playbackStartedAtMs <= 0L) {
        return
    }

    Box(
        modifier = modifier
            .width(58.dp)
            .height(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = formatElapsedTimer(elapsedMs),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isRunning) 0.94f else 0.48f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun TempoPushSubdivisionRow(
    subdivisionCount: Int,
    currentSubdivisionIndex: Int,
    tempoNudgeMs: Int,
    onTempoNudge: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(236.dp)
            .height(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        TempoPushInlineHint(
            rotationDegrees = -90f,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-0).dp),
            onVerticalNudge = { isSwipeUp ->
                onTempoNudge(if (isSwipeUp) tempoNudgeMs else -tempoNudgeMs)
            },
        )

        SubdivisionDots(
            subdivisionCount = subdivisionCount,
            currentSubdivisionIndex = currentSubdivisionIndex,
            activeSize = 18.dp,
            inactiveSize = 10.dp,
            spacing = 3.dp,
        )

        TempoPushInlineHint(
            rotationDegrees = 90f,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 0.dp),
            onVerticalNudge = { isSwipeUp ->
                onTempoNudge(if (isSwipeUp) -tempoNudgeMs else tempoNudgeMs)
            },
        )
    }
}

@Composable
private fun TempoPushInlineHint(
    rotationDegrees: Float,
    modifier: Modifier = Modifier,
    onVerticalNudge: ((Boolean) -> Unit)? = null,
) {
    var verticalDrag by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
            .width(58.dp)
            .height(12.dp)
            .then(
                if (onVerticalNudge != null) {
                    Modifier.pointerInput(onVerticalNudge) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                verticalDrag = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                verticalDrag += dragAmount
                            },
                            onDragEnd = {
                                when {
                                    verticalDrag < -12f -> onVerticalNudge(true)
                                    verticalDrag > 12f -> onVerticalNudge(false)
                                }
                                verticalDrag = 0f
                            },
                            onDragCancel = {
                                verticalDrag = 0f
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .rotate(rotationDegrees),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TempoPushArrowIcon(pointLeft = true)

        Text(
            text = "Pull - Push",
            fontSize = 6.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            maxLines = 1,
        )

        TempoPushArrowIcon(pointLeft = false)
    }
}

@Composable
private fun TempoPushArrowIcon(
    pointLeft: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)

    Canvas(
        modifier = modifier.size(width = 5.dp, height = 7.dp),
    ) {
        val path = Path().apply {
            if (pointLeft) {
                moveTo(0f, size.height / 2f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
            } else {
                moveTo(size.width, size.height / 2f)
                lineTo(0f, 0f)
                lineTo(0f, size.height)
            }
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Suppress("unused")
@Composable
private fun TempoPushSideHint(
    rotationDegrees: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(68.dp)
            .height(12.dp)
            .rotate(rotationDegrees),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "↕",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            maxLines = 1,
        )

        Text(
            text = "<- Pull - Push ->",
            fontSize = 6.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            maxLines = 1,
        )
    }
}

@Composable
private fun RhythmEditDetails(
    appText: AppText,
    bpm: Int,
    beatsPerMeasure: Int,
    subdivisionCount: Int,
    beatAccentTypes: List<BeatAccentType>,
    currentBeatIndex: Int,
    currentSubdivisionIndex: Int,
    beatFlash: Boolean,
    modifier: Modifier = Modifier,
    onTimeSignatureChoice: (Int) -> Unit,
    onBeatAccentTypeCycle: (Int) -> Unit,
    onSubdivisionChoice: (Int) -> Unit,
    onTimeSignaturePickerClick: () -> Unit,
    onSubdivisionPickerClick: () -> Unit,
    onBpmClick: () -> Unit,
    onIntensityClick: () -> Unit,
    onDone: () -> Unit,
) {
    BigPulseCircleSelector(
        beatsPerMeasure = beatsPerMeasure,
        beatAccentTypes = beatAccentTypes,
        currentBeatIndex = currentBeatIndex,
        beatFlash = beatFlash,
        modifier = modifier,
        centerContentOffsetY = (-18).dp,
        bottomContentOffsetY = (-18).dp,
        onBeatAccentTypeCycle = onBeatAccentTypeCycle,
        bottomContent = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                SmallCommandButton(
                    text = appText.intensity,
                    modifier = Modifier
                        .width(82.dp)
                        .height(22.dp),
                    fontSize = 8.sp,
                    onClick = onIntensityClick,
                )

                SmallCommandButton(
                    text = appText.done,
                    modifier = Modifier
                        .width(64.dp)
                        .height(22.dp),
                    fontSize = 9.sp,
                    onClick = onDone,
                )
            }
        },
    ) {
        SmallCommandButton(
            text = "$bpm BPM",
            modifier = Modifier
                .width(78.dp)
                .height(26.dp),
            fontSize = 12.sp,
            onClick = onBpmClick,
        )

        Spacer(modifier = Modifier.height(1.dp))

        Text(
            text = appText.timeSignature,
            fontSize = 8.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(1.dp))

        RhythmValueStepper(
            valueText = "$beatsPerMeasure/4",
            onDecrease = {
                onTimeSignatureChoice(steppedOption(TimeSignatureBeatOptions, beatsPerMeasure, -1))
            },
            onValueClick = onTimeSignaturePickerClick,
            onIncrease = {
                onTimeSignatureChoice(steppedOption(TimeSignatureBeatOptions, beatsPerMeasure, 1))
            },
        )

        Spacer(modifier = Modifier.height(1.dp))

        Text(
            text = appText.subdivision,
            fontSize = 8.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(1.dp))

        RhythmValueStepper(
            valueText = "$subdivisionCount",
            onDecrease = {
                onSubdivisionChoice(steppedOption(SubdivisionOptions, subdivisionCount, -1))
            },
            onValueClick = onSubdivisionPickerClick,
            onIncrease = {
                onSubdivisionChoice(steppedOption(SubdivisionOptions, subdivisionCount, 1))
            },
        )

        Spacer(modifier = Modifier.height(1.dp))

        SubdivisionDots(
            subdivisionCount = subdivisionCount,
            currentSubdivisionIndex = currentSubdivisionIndex,
        )
    }
}

@Composable
internal fun RhythmEditorPopup(
    appText: AppText,
    bpm: Int,
    beatsPerMeasure: Int,
    subdivisionCount: Int,
    beatAccentTypes: List<BeatAccentType>,
    currentBeatIndex: Int,
    currentSubdivisionIndex: Int,
    beatFlash: Boolean,
    accentIntensityMode: AccentIntensityMode,
    accentIntensityRanges: List<AccentIntensityRange>,
    appLanguage: AppLanguage,
    onTimeSignatureChoice: (Int) -> Unit,
    onBeatAccentTypeCycle: (Int) -> Unit,
    onSubdivisionChoice: (Int) -> Unit,
    onAccentIntensityModeChoice: (AccentIntensityMode) -> Unit,
    onAccentIntensityRangesChange: (List<AccentIntensityRange>) -> Unit,
    onBpmClick: () -> Unit,
    onDone: () -> Unit,
) {
    var activeChoicePicker by rememberSaveable { mutableStateOf<RhythmChoicePicker?>(null) }
    var intensityPickerOpen by rememberSaveable { mutableStateOf(false) }

    DismissibleEditorPopup(onDone = onDone) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            RhythmEditDetails(
                appText = appText,
                bpm = bpm,
                beatsPerMeasure = beatsPerMeasure,
                subdivisionCount = subdivisionCount,
                beatAccentTypes = beatAccentTypes,
                currentBeatIndex = currentBeatIndex,
                currentSubdivisionIndex = currentSubdivisionIndex,
                beatFlash = beatFlash,
                modifier = Modifier.fillMaxSize(),
                onTimeSignatureChoice = onTimeSignatureChoice,
                onBeatAccentTypeCycle = onBeatAccentTypeCycle,
                onSubdivisionChoice = onSubdivisionChoice,
                onTimeSignaturePickerClick = {
                    activeChoicePicker = RhythmChoicePicker.TimeSignature
                },
                onSubdivisionPickerClick = {
                    activeChoicePicker = RhythmChoicePicker.Subdivision
                },
                onBpmClick = onBpmClick,
                onIntensityClick = {
                    intensityPickerOpen = true
                },
                onDone = onDone,
            )

            activeChoicePicker?.let { picker ->
                RhythmTimingChoicePopup(
                    appText = appText,
                    initialPicker = picker,
                    beatsPerMeasure = beatsPerMeasure,
                    subdivisionCount = subdivisionCount,
                    onTimeSignatureChoice = { option ->
                        onTimeSignatureChoice(option)
                    },
                    onSubdivisionChoice = { option ->
                        onSubdivisionChoice(option)
                    },
                    onDismiss = {
                        activeChoicePicker = null
                    },
                )
            }

            if (intensityPickerOpen) {
                AccentIntensityChoicePopup(
                    title = appText.intensityTitle,
                    selectedMode = accentIntensityMode,
                    accentIntensityRanges = accentIntensityRanges,
                    appLanguage = appLanguage,
                    dismissText = appText.done,
                    onModeChoice = onAccentIntensityModeChoice,
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
}

@Composable
private fun RhythmValueStepper(
    valueText: String,
    onDecrease: () -> Unit,
    onValueClick: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallCommandButton(
            text = "-",
            modifier = Modifier
                .width(26.dp)
                .height(24.dp),
            fontSize = 12.sp,
            onClick = onDecrease,
        )

        SmallCommandButton(
            text = valueText,
            modifier = Modifier
                .width(48.dp)
                .height(24.dp),
            fontSize = 11.sp,
            onClick = onValueClick,
        )

        SmallCommandButton(
            text = "+",
            modifier = Modifier
                .width(26.dp)
                .height(24.dp),
            fontSize = 12.sp,
            onClick = onIncrease,
        )
    }
}

@Composable
internal fun RhythmTimingChoicePopup(
    appText: AppText,
    initialPicker: RhythmChoicePicker,
    beatsPerMeasure: Int,
    subdivisionCount: Int,
    onTimeSignatureChoice: (Int) -> Unit,
    onSubdivisionChoice: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialPage = if (initialPicker == RhythmChoicePicker.TimeSignature) 0 else 1
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2 })
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(184.dp)
                .padding(top = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChoicePillButton(
                    text = appText.timeSignature,
                    selected = pagerState.currentPage == 0,
                    modifier = Modifier
                        .width(82.dp)
                        .height(24.dp),
                    fontSize = 8.sp,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    },
                )

                ChoicePillButton(
                    text = appText.subdivision,
                    selected = pagerState.currentPage == 1,
                    modifier = Modifier
                        .width(82.dp)
                        .height(24.dp),
                    fontSize = 8.sp,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .width(184.dp)
                    .height(130.dp),
            ) { page ->
                if (page == 0) {
                    RhythmTimingOptionGrid(
                        options = TimeSignatureBeatOptions,
                        selectedOption = beatsPerMeasure,
                        optionLabel = { option -> "$option/4" },
                        onOptionChoice = onTimeSignatureChoice,
                    )
                } else {
                    RhythmTimingOptionGrid(
                        options = SubdivisionOptions,
                        selectedOption = subdivisionCount,
                        optionLabel = { option -> "$option" },
                        onOptionChoice = onSubdivisionChoice,
                    )
                }
            }
        }

        RhythmBottomActionButton(
            text = "Back",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(y = (-12).dp),
            mirrored = true,
            onClick = onDismiss,
        )

        RhythmBottomActionButton(
            text = appText.done,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = (-12).dp),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun RhythmTimingOptionGrid(
    options: List<Int>,
    selectedOption: Int,
    optionLabel: (Int) -> String,
    onOptionChoice: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        options.chunked(4).forEach { optionRow ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                optionRow.forEach { option ->
                    ChoicePillButton(
                        text = optionLabel(option),
                        selected = selectedOption == option,
                        modifier = Modifier
                            .width(40.dp)
                            .height(26.dp),
                        fontSize = 9.sp,
                        onClick = { onOptionChoice(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RhythmBottomActionButton(
    text: String,
    modifier: Modifier = Modifier,
    mirrored: Boolean = false,
    onClick: () -> Unit,
) {
    SmallCommandButton(
        text = text,
        modifier = modifier
            .padding(
                start = if (mirrored) 2.dp else 0.dp,
                end = if (mirrored) 0.dp else 2.dp,
                bottom = 16.dp,
            )
            .rotate(if (mirrored) 38f else -38f)
            .width(64.dp)
            .height(24.dp),
        fontSize = 7.sp,
        onClick = onClick,
    )
}

@Composable
private fun BigPulseCircleSelector(
    beatsPerMeasure: Int,
    beatAccentTypes: List<BeatAccentType>,
    currentBeatIndex: Int,
    beatFlash: Boolean,
    modifier: Modifier = Modifier,
    centerContentWidth: Dp = 128.dp,
    centerContentOffsetY: Dp = 0.dp,
    bottomContentOffsetY: Dp = 0.dp,
    beatRingVisible: Boolean = true,
    onBeatAccentTypeCycle: ((Int) -> Unit)?,
    tempoNudgeMs: Int = DEFAULT_TEMPO_NUDGE_MS,
    onTempoNudge: ((Int) -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val beatCount = beatsPerMeasure.coerceIn(2, 16)
    val rightCount = (beatCount + 1) / 2
    val leftCount = beatCount - rightCount
    val hitSize = when {
        beatCount > 12 -> 26.dp
        beatCount > 8 -> 30.dp
        else -> 34.dp
    }
    val circleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val ringSize = if (maxWidth < maxHeight) maxWidth else maxHeight
        val radius = (ringSize / 2) - (hitSize / 2) - 2.dp

        if (beatRingVisible) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 1.dp.toPx()
                val ringRadius = ((size.minDimension / 2f) - (hitSize.toPx() / 2f) - 2.dp.toPx())
                    .coerceAtLeast(0f)
                drawCircle(
                    color = circleColor,
                    radius = ringRadius,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = strokeWidth),
                )
            }
        }

        Column(
            modifier = Modifier
                .width(centerContentWidth)
                .offset(y = centerContentOffsetY),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }

        if (beatRingVisible) {
            (1..rightCount).forEach { index ->
                val angle = splitCircleAngle(index, rightCount, rightSide = true)
                BeatAccentDotButton(
                    accentType = beatAccentTypes.typeForBeat(index),
                    beatFlash = beatFlash && currentBeatIndex == index,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = (maxWidth / 2) + (radius * cos(angle).toFloat()) - (hitSize / 2),
                            y = (maxHeight / 2) + (radius * sin(angle).toFloat()) - (hitSize / 2),
                        )
                        .size(hitSize),
                    onClick = onBeatAccentTypeCycle?.let { onBeatChoice ->
                        { onBeatChoice(index) }
                    },
                    onVerticalNudge = onTempoNudge?.let { nudge ->
                        { isSwipeUp -> nudge(if (isSwipeUp) -tempoNudgeMs else tempoNudgeMs) }
                    },
                )
            }

            (1..leftCount).forEach { index ->
                val beat = rightCount + index
                val angle = splitCircleAngle(index, leftCount, rightSide = false)
                BeatAccentDotButton(
                    accentType = beatAccentTypes.typeForBeat(beat),
                    beatFlash = beatFlash && currentBeatIndex == beat,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = (maxWidth / 2) + (radius * cos(angle).toFloat()) - (hitSize / 2),
                            y = (maxHeight / 2) + (radius * sin(angle).toFloat()) - (hitSize / 2),
                        )
                        .size(hitSize),
                    onClick = onBeatAccentTypeCycle?.let { onBeatChoice ->
                        { onBeatChoice(beat) }
                    },
                    onVerticalNudge = onTempoNudge?.let { nudge ->
                        { isSwipeUp -> nudge(if (isSwipeUp) tempoNudgeMs else -tempoNudgeMs) }
                    },
                )
            }
        }

        bottomContent?.let { footer ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .offset(y = bottomContentOffsetY),
                contentAlignment = Alignment.Center,
            ) {
                footer()
            }
        }
    }
}

private fun splitCircleAngle(
    index: Int,
    count: Int,
    rightSide: Boolean,
): Double {
    val startDegrees = if (rightSide) -58.0 else 122.0
    val endDegrees = if (rightSide) 58.0 else 238.0
    val progress = if (count <= 1) 0.0 else (index - 1).toDouble() / (count - 1).toDouble()
    val angleDegrees = startDegrees + ((endDegrees - startDegrees) * progress)

    return angleDegrees * PI / 180.0
}

@Composable
private fun BeatAccentDotButton(
    accentType: BeatAccentType,
    beatFlash: Boolean,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    onVerticalNudge: ((Boolean) -> Unit)? = null,
) {
    val baseDotSize = when (accentType) {
        BeatAccentType.Big -> 20.dp
        BeatAccentType.Medium -> 16.dp
        BeatAccentType.Small -> 13.dp
        BeatAccentType.Silent -> 20.dp
    }
    val dotRadius = (if (beatFlash) baseDotSize + 6.dp else baseDotSize) / 2
    val primaryColor = MaterialTheme.colorScheme.primary
    val dotAlpha = when (accentType) {
        BeatAccentType.Big -> if (beatFlash) 1f else 0.9f
        BeatAccentType.Medium -> if (beatFlash) 1f else 0.72f
        BeatAccentType.Small -> if (beatFlash) 0.95f else 0.56f
        BeatAccentType.Silent -> 0f
    }
    val borderAlpha = if (beatFlash) 0.95f else 0.58f
    val borderWidth = if (beatFlash) 3.dp else 2.dp
    var verticalDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (onVerticalNudge != null) {
                    Modifier.pointerInput(onVerticalNudge) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                verticalDrag = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                verticalDrag += dragAmount
                            },
                            onDragEnd = {
                                when {
                                    verticalDrag < -12f -> onVerticalNudge(true)
                                    verticalDrag > 12f -> onVerticalNudge(false)
                                }
                                verticalDrag = 0f
                            },
                            onDragCancel = {
                                verticalDrag = 0f
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            if (accentType == BeatAccentType.Silent) {
                drawCircle(
                    color = primaryColor.copy(alpha = borderAlpha),
                    radius = dotRadius.toPx(),
                    center = center,
                    style = Stroke(width = borderWidth.toPx()),
                )
            } else {
                drawCircle(
                    color = primaryColor.copy(alpha = dotAlpha),
                    radius = dotRadius.toPx(),
                    center = center,
                )
            }
        }
    }
}

private fun steppedOption(
    options: List<Int>,
    current: Int,
    step: Int,
): Int {
    val currentIndex = options.indexOf(current).takeIf { it >= 0 }
        ?: options.indexOf(options.minBy { abs(it - current) })
    return options[(currentIndex + step).coerceIn(0, options.lastIndex)]
}

@Composable
private fun SubdivisionDots(
    subdivisionCount: Int,
    currentSubdivisionIndex: Int,
    activeSize: Dp = 7.dp,
    inactiveSize: Dp = 5.dp,
    spacing: Dp = 5.dp,
) {
    val slotSize = if (activeSize > inactiveSize) activeSize else inactiveSize
    val dotColor = MaterialTheme.colorScheme.onBackground
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (1..subdivisionCount).forEach { subdivisionIndex ->
            val active = subdivisionIndex == currentSubdivisionIndex
            Canvas(
                modifier = Modifier
                    .size(slotSize),
            ) {
                drawCircle(
                    color = dotColor.copy(
                        alpha = if (active) 0.72f else 0.3f,
                    ),
                    radius = (if (active) activeSize else inactiveSize).toPx() / 2f,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
            }
        }
    }
}

private fun elapsedTimerMs(
    isRunning: Boolean,
    playbackStartedAtMs: Long,
): Long {
    return if (isRunning && playbackStartedAtMs > 0L) {
        (SystemClock.elapsedRealtime() - playbackStartedAtMs).coerceAtLeast(0L)
    } else {
        0L
    }
}

private fun formatElapsedTimer(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds / 60L) % 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0L) {
        "$hours:${minutes.twoDigitTimerPart()}:${seconds.twoDigitTimerPart()}"
    } else {
        "$minutes:${seconds.twoDigitTimerPart()}"
    }
}

private fun Long.twoDigitTimerPart(): String {
    return toString().padStart(2, '0')
}

@Composable
private fun rememberRhythmBeatVisualState(
    isRunning: Boolean,
    animationEnabled: Boolean,
    bpm: Int,
    beatsPerMeasure: Int,
    subdivisionCount: Int,
    beatClockStartedAtMs: Long,
    fallbackBeatIndex: Int,
    fallbackSubdivisionIndex: Int,
    fallbackBeatFlash: Boolean,
): BeatVisualState {
    var beatVisualState by remember {
        mutableStateOf(
            BeatVisualState(
                currentBeatIndex = fallbackBeatIndex,
                currentSubdivisionIndex = fallbackSubdivisionIndex,
                beatFlash = fallbackBeatFlash,
            ),
        )
    }
    val latestFallbackBeatIndex by rememberUpdatedState(fallbackBeatIndex)
    val latestFallbackSubdivisionIndex by rememberUpdatedState(fallbackSubdivisionIndex)
    val latestFallbackBeatFlash by rememberUpdatedState(fallbackBeatFlash)

    LaunchedEffect(
        isRunning,
        animationEnabled,
        bpm,
        beatsPerMeasure,
        subdivisionCount,
        beatClockStartedAtMs,
    ) {
        if (!isRunning || beatClockStartedAtMs <= 0L) {
            beatVisualState = BeatVisualState(
                currentBeatIndex = latestFallbackBeatIndex.coerceIn(1, beatsPerMeasure.coerceAtLeast(1)),
                currentSubdivisionIndex = latestFallbackSubdivisionIndex.coerceAtLeast(1),
                beatFlash = latestFallbackBeatFlash,
            )
            return@LaunchedEffect
        }

        if (!animationEnabled) {
            beatVisualState = currentRhythmBeatVisualState(
                bpm = bpm,
                beatsPerMeasure = beatsPerMeasure,
                subdivisionCount = subdivisionCount,
                beatClockStartedAtMs = beatClockStartedAtMs,
            )
            return@LaunchedEffect
        }

        while (true) {
            val nextBeatVisualState = currentRhythmBeatVisualState(
                bpm = bpm,
                beatsPerMeasure = beatsPerMeasure,
                subdivisionCount = subdivisionCount,
                beatClockStartedAtMs = beatClockStartedAtMs,
            )
            if (nextBeatVisualState.beatFlash && isRhythmVisualTraceWindow(
                    bpm = bpm,
                    beatClockStartedAtMs = beatClockStartedAtMs,
                )
            ) {
                BeatTimingTrace.markForBeat(
                    label = "rhythm visual loop",
                    beat = nextBeatVisualState.currentBeatIndex,
                )
            }
            beatVisualState = nextBeatVisualState
            delay(
                rhythmVisualDelayMs(
                    bpm = bpm,
                    subdivisionCount = subdivisionCount,
                    beatClockStartedAtMs = beatClockStartedAtMs,
                ),
            )
        }
    }

    return beatVisualState
}

private fun currentRhythmBeatVisualState(
    bpm: Int,
    beatsPerMeasure: Int,
    subdivisionCount: Int,
    beatClockStartedAtMs: Long,
): BeatVisualState {
    val normalizedBpm = bpm.coerceIn(MIN_BPM, MAX_BPM)
    val normalizedBeatsPerMeasure = beatsPerMeasure.coerceIn(2, 16)
    val normalizedSubdivisionCount = subdivisionCount.toSupportedPulseSubdivisionCount()
    val intervalMs = (60_000L / normalizedBpm).coerceAtLeast(1L)
    val subdivisionIntervalMs = (intervalMs / normalizedSubdivisionCount).coerceAtLeast(1L)
    val elapsedMs = (SystemClock.elapsedRealtime() - beatClockStartedAtMs).coerceAtLeast(0L)
    val beatElapsedMs = elapsedMs % intervalMs

    return BeatVisualState(
        currentBeatIndex = (((elapsedMs / intervalMs) % normalizedBeatsPerMeasure) + 1L).toInt(),
        currentSubdivisionIndex = ((beatElapsedMs / subdivisionIntervalMs) + 1L)
            .toInt()
            .coerceIn(1, normalizedSubdivisionCount),
        beatFlash = beatElapsedMs < BEAT_FLASH_DURATION_MS,
    )
}

private fun isRhythmVisualTraceWindow(
    bpm: Int,
    beatClockStartedAtMs: Long,
): Boolean {
    val normalizedBpm = bpm.coerceIn(MIN_BPM, MAX_BPM)
    val intervalMs = (60_000L / normalizedBpm).coerceAtLeast(1L)
    val elapsedMs = (SystemClock.elapsedRealtime() - beatClockStartedAtMs).coerceAtLeast(0L)
    val beatElapsedMs = elapsedMs % intervalMs
    return beatElapsedMs < BEAT_FLASH_DURATION_MS
}

private fun rhythmVisualDelayMs(
    bpm: Int,
    subdivisionCount: Int,
    beatClockStartedAtMs: Long,
): Long {
    val normalizedBpm = bpm.coerceIn(MIN_BPM, MAX_BPM)
    val normalizedSubdivisionCount = subdivisionCount.toSupportedPulseSubdivisionCount()
    val intervalMs = (60_000L / normalizedBpm).coerceAtLeast(1L)
    val subdivisionIntervalMs = (intervalMs / normalizedSubdivisionCount).coerceAtLeast(1L)
    val elapsedMs = (SystemClock.elapsedRealtime() - beatClockStartedAtMs).coerceAtLeast(0L)
    val beatElapsedMs = elapsedMs % intervalMs
    val subdivisionElapsedMs = beatElapsedMs % subdivisionIntervalMs
    val untilNextBeatMs = intervalMs - beatElapsedMs
    val untilNextSubdivisionMs = subdivisionIntervalMs - subdivisionElapsedMs
    val untilFlashEndsMs = if (beatElapsedMs < BEAT_FLASH_DURATION_MS) {
        BEAT_FLASH_DURATION_MS - beatElapsedMs
    } else {
        Long.MAX_VALUE
    }
    val untilNextVisualBoundaryMs = minOf(
        untilNextBeatMs,
        untilNextSubdivisionMs,
        untilFlashEndsMs,
    )

    return (untilNextVisualBoundaryMs - RHYTHM_VISUAL_WAKE_AHEAD_MS)
        .coerceAtLeast(1L)
        .coerceAtMost(RHYTHM_VISUAL_MAX_DELAY_MS)
}

@Composable
private fun FastTapTempoButton(
    text: String,
    onTap: () -> Unit,
    prominent: Boolean = false,
    size: Dp = if (prominent) 88.dp else 98.dp,
    fontSize: TextUnit = if (prominent) 15.sp else 20.sp,
) {
    val shape = CircleShape

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (prominent) 0.78f else 1f), shape)
            .border(
                width = if (prominent) 1.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (prominent) 0.95f else 0f),
                shape = shape,
            )
            .pointerInput(onTap) {
                detectTapGestures(
                    onPress = {
                        onTap()
                        tryAwaitRelease()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TempoAdjustButton(
    text: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLongClickLabel: String,
    prominent: Boolean = false,
    width: Dp = 42.dp,
    height: Dp = 30.dp,
    fontSize: TextUnit = 15.sp,
) {
    if (prominent) {
        val shape = RoundedCornerShape(50)
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.78f), shape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                    shape = shape,
                )
                .pointerInput(onClick, onLongClick) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onLongClick() },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        return
    }

    Button(
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        modifier = Modifier.size(36.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        CenteredButtonLabel(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun AudioToolButtons(
    appText: AppText,
    onOpenTuner: () -> Unit,
    onOpenSpectrum: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassCommandButton(
            text = appText.tuner,
            modifier = Modifier
                .width(50.dp)
                .height(22.dp),
            fontSize = 10.sp,
            selected = false,
            prominent = false,
            onClick = onOpenTuner,
        )

        GlassCommandButton(
            text = "Spect",
            modifier = Modifier
                .width(50.dp)
                .height(22.dp),
            fontSize = 10.sp,
            selected = false,
            prominent = false,
            onClick = onOpenSpectrum,
        )
    }
}

@Composable
internal fun BigPulseRingOverlay(
    beatFlash: Boolean,
    flashingAccentType: BeatAccentType,
    bigRingFlashMode: BigRingFlashMode,
    colorArgb: Int,
    modifier: Modifier = Modifier,
) {
    val shouldFlash = bigRingFlashMode.shouldFlashRing(
        beatFlash = beatFlash,
        flashingAccentType = flashingAccentType,
    )

    if (shouldFlash) {
        BeatTimingTrace.mark("big ring draw")
    }

    BigPulseRing(
        colorArgb = colorArgb,
        alpha = if (shouldFlash) {
            0.95f
        } else {
            0f
        },
        modifier = modifier,
    )
}

@Composable
private fun BigPulseRing(
    colorArgb: Int,
    modifier: Modifier = Modifier,
    alpha: Float = 0.95f,
) {
    Canvas(modifier = modifier) {
        if (alpha <= 0f) return@Canvas

        val strokeWidth = 6.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        if (isRainbowColor(colorArgb)) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = RainbowColors.map { color -> color.copy(alpha = alpha.coerceIn(0f, 1f)) },
                    center = center,
                ),
                radius = (size.minDimension - strokeWidth) / 2f,
                center = center,
                style = Stroke(width = strokeWidth),
            )
        } else {
            drawCircle(
                color = colorFromChoice(colorArgb).copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = (size.minDimension - strokeWidth) / 2f,
                center = center,
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

@Composable
internal fun PulsePagerIndicator(
    currentPage: Int,
    modifier: Modifier = Modifier,
    pageCount: Int = PULSE_PAGE_COUNT,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { page ->
            Box(
                modifier = Modifier
                    .size(if (page == currentPage) 5.dp else 3.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = if (page == currentPage) 0.8f else 0.34f,
                        ),
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun BeatPulsePage(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun BeatTempoReadout(
    bpm: Int,
    beatFlash: Boolean,
    isAccentFlash: Boolean,
    modifier: Modifier = Modifier,
    numberFontSize: TextUnit = 38.sp,
    labelFontSize: TextUnit = 12.sp,
    pulseBoxSize: Dp = 34.dp,
    spacing: Dp = 7.dp,
) {
    val pulseSize = when {
        beatFlash && isAccentFlash -> 28.dp
        beatFlash -> 22.dp
        else -> 11.dp
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(pulseBoxSize),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(pulseSize)
                    .background(
                        color = if (beatFlash) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        shape = CircleShape,
                    ),
            )
        }

        Text(
            text = "$bpm",
            fontSize = numberFontSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = "BPM",
            fontSize = labelFontSize,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

