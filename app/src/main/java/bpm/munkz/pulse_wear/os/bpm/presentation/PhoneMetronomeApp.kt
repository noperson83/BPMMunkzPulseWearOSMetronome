package bpm.munkz.pulse_wear.os.bpm.presentation

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.wear.compose.material3.Text
import bpm.munkz.pulse_wear.os.bpm.presentation.theme.BPMMunkzPulseTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val PHONE_PREFS = "bpm_munkz_phone_metronome"
private const val PHONE_BPM_KEY = "bpm"
private const val PHONE_BEEP_KEY = "beep"
private const val PHONE_HAPTICS_KEY = "haptics"
private const val PHONE_ACCENT_KEY = "accent"
private const val PHONE_BEATS_PER_MEASURE_KEY = "beats_per_measure"
private const val PHONE_SUBDIVISION_KEY = "subdivision"
private const val PHONE_TEMPO_NUDGE_KEY = "tempo_nudge"
private const val PHONE_SOUND_MODE_KEY = "sound_mode"

@Composable
fun PhoneMetronomeApp() {
    val context = LocalContext.current
    var bpm by rememberSaveable { mutableIntStateOf(context.phoneLoadInt(PHONE_BPM_KEY, 96)) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var beepEnabled by rememberSaveable { mutableStateOf(context.phoneLoadBoolean(PHONE_BEEP_KEY, true)) }
    var hapticsEnabled by rememberSaveable { mutableStateOf(context.phoneLoadBoolean(PHONE_HAPTICS_KEY, true)) }
    var accentEnabled by rememberSaveable { mutableStateOf(context.phoneLoadBoolean(PHONE_ACCENT_KEY, true)) }
    var beatsPerMeasure by rememberSaveable {
        mutableIntStateOf(context.phoneLoadInt(PHONE_BEATS_PER_MEASURE_KEY, 4).coerceIn(2, 16))
    }
    var subdivisionCount by rememberSaveable {
        mutableIntStateOf(context.phoneLoadInt(PHONE_SUBDIVISION_KEY, 1).coerceIn(1, 8))
    }
    var tempoNudgeMs by rememberSaveable {
        mutableIntStateOf(context.phoneLoadInt(PHONE_TEMPO_NUDGE_KEY, 200).coerceIn(0, 600))
    }
    var beatSoundMode by rememberSaveable {
        mutableStateOf(
            BeatSoundMode.fromPersistedValue(
                context.phoneLoadInt(PHONE_SOUND_MODE_KEY, BeatSoundMode.Clicks.persistedValue),
            ),
        )
    }
    var pulseOn by remember { mutableStateOf(false) }
    var tapTimes by remember { mutableStateOf(emptyList<Long>()) }

    val metronomeState = remember(
        bpm,
        beepEnabled,
        hapticsEnabled,
        accentEnabled,
        beatsPerMeasure,
        subdivisionCount,
        tempoNudgeMs,
        beatSoundMode,
        isRunning,
    ) {
        MetronomeState(
            bpm = bpm,
            beatsPerMeasure = beatsPerMeasure,
            subdivisionCount = subdivisionCount,
            beatAccentTypes = if (accentEnabled) {
                defaultBeatAccentTypes(beatsPerMeasure = beatsPerMeasure, accentBeat = 1)
            } else {
                List(beatsPerMeasure) { BeatAccentType.Medium }
            },
            hapticsEnabled = hapticsEnabled,
            beepEnabled = beepEnabled,
            beatSoundMode = beatSoundMode,
            tempoNudgeMs = tempoNudgeMs,
            isRunning = isRunning,
        )
    }

    LaunchedEffect(isRunning, metronomeState) {
        if (isRunning) {
            MetronomeService.start(context, metronomeState)
        } else {
            MetronomeService.stop(context)
        }
    }

    LaunchedEffect(
        bpm,
        beepEnabled,
        hapticsEnabled,
        accentEnabled,
        beatsPerMeasure,
        subdivisionCount,
        tempoNudgeMs,
        beatSoundMode,
    ) {
        context.phoneSaveInt(PHONE_BPM_KEY, bpm)
        context.phoneSaveBoolean(PHONE_BEEP_KEY, beepEnabled)
        context.phoneSaveBoolean(PHONE_HAPTICS_KEY, hapticsEnabled)
        context.phoneSaveBoolean(PHONE_ACCENT_KEY, accentEnabled)
        context.phoneSaveInt(PHONE_BEATS_PER_MEASURE_KEY, beatsPerMeasure)
        context.phoneSaveInt(PHONE_SUBDIVISION_KEY, subdivisionCount)
        context.phoneSaveInt(PHONE_TEMPO_NUDGE_KEY, tempoNudgeMs)
        context.phoneSaveInt(PHONE_SOUND_MODE_KEY, beatSoundMode.persistedValue)
    }

    LaunchedEffect(isRunning, bpm) {
        pulseOn = false
        while (isRunning) {
            pulseOn = true
            delay(90L)
            pulseOn = false
            delay((60_000L / bpm.coerceAtLeast(1) - 90L).coerceAtLeast(80L))
        }
    }

    BPMMunkzPulseTheme {
        PhoneMetronomeScreen(
            bpm = bpm,
            isRunning = isRunning,
            beepEnabled = beepEnabled,
            hapticsEnabled = hapticsEnabled,
            accentEnabled = accentEnabled,
            beatsPerMeasure = beatsPerMeasure,
            subdivisionCount = subdivisionCount,
            tempoNudgeMs = tempoNudgeMs,
            beatSoundMode = beatSoundMode,
            pulseOn = pulseOn,
            onBpmChange = { bpm = it.coerceIn(MIN_BPM, MAX_BPM) },
            onRunningChange = { isRunning = it },
            onBeepChange = { beepEnabled = it },
            onHapticsChange = { hapticsEnabled = it },
            onAccentChange = { accentEnabled = it },
            onBeatsPerMeasureChange = { beatsPerMeasure = it.coerceIn(2, 16) },
            onSubdivisionChange = { subdivisionCount = it.coerceIn(1, 8) },
            onTempoNudgeChange = { tempoNudgeMs = it.coerceIn(0, 600) },
            onBeatSoundModeChange = { beatSoundMode = it },
            onTapTempo = {
                val now = SystemClock.elapsedRealtime()
                val recentTaps = (tapTimes + now)
                    .filter { now - it <= 2_000L }
                    .takeLast(5)
                tapTimes = recentTaps
                if (recentTaps.size >= 2) {
                    val intervals = recentTaps.zipWithNext { first, second -> second - first }
                    val averageInterval = intervals.average()
                    if (averageInterval > 0.0) {
                        bpm = (60_000.0 / averageInterval).roundToInt().coerceIn(MIN_BPM, MAX_BPM)
                    }
                }
            },
        )
    }
}

@Composable
private fun PhoneMetronomeScreen(
    bpm: Int,
    isRunning: Boolean,
    beepEnabled: Boolean,
    hapticsEnabled: Boolean,
    accentEnabled: Boolean,
    beatsPerMeasure: Int,
    subdivisionCount: Int,
    tempoNudgeMs: Int,
    beatSoundMode: BeatSoundMode,
    pulseOn: Boolean,
    onBpmChange: (Int) -> Unit,
    onRunningChange: (Boolean) -> Unit,
    onBeepChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onAccentChange: (Boolean) -> Unit,
    onBeatsPerMeasureChange: (Int) -> Unit,
    onSubdivisionChange: (Int) -> Unit,
    onTempoNudgeChange: (Int) -> Unit,
    onBeatSoundModeChange: (BeatSoundMode) -> Unit,
    onTapTempo: () -> Unit,
) {
    val green = Color(0xFF9BFF00)
    val background = Color(0xFF050604)
    val panel = Color(0xFF141812)
    val text = Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
            .padding(top = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "BPM Munkz Pulse",
                    color = text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (isRunning) "Running" else "Ready",
                    color = if (isRunning) green else text.copy(alpha = 0.62f),
                    fontSize = 14.sp,
                )
            }
            PhoneStatusDot(active = isRunning, color = green)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(panel)
                .border(1.dp, green.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .clickable(onClick = onTapTempo),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(if (pulseOn) 286.dp else 254.dp)) {
                drawCircle(color = green.copy(alpha = if (pulseOn) 0.22f else 0.12f))
                drawCircle(
                    color = green.copy(alpha = if (pulseOn) 0.92f else 0.58f),
                    radius = size.minDimension * 0.38f,
                )
                drawCircle(color = background, radius = size.minDimension * 0.27f)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = bpm.toString(),
                    color = text,
                    fontSize = 82.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "BPM",
                    color = green,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PhoneCommandButton("-5", modifier = Modifier.weight(1f)) { onBpmChange(bpm - 5) }
            PhoneCommandButton("-1", modifier = Modifier.weight(1f)) { onBpmChange(bpm - 1) }
            PhoneCommandButton("+1", modifier = Modifier.weight(1f)) { onBpmChange(bpm + 1) }
            PhoneCommandButton("+5", modifier = Modifier.weight(1f)) { onBpmChange(bpm + 5) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PhoneCommandButton(
                label = if (isRunning) "Stop" else "Start",
                modifier = Modifier.weight(1.25f),
                prominent = true,
            ) {
                onRunningChange(!isRunning)
            }
            PhoneCommandButton(
                label = "Tap",
                modifier = Modifier.weight(1f),
            ) {
                onTapTempo()
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PhoneToggleButton("Beep", beepEnabled, Modifier.weight(1f), onBeepChange)
            PhoneToggleButton("Haptic", hapticsEnabled, Modifier.weight(1f), onHapticsChange)
            PhoneToggleButton("Accent", accentEnabled, Modifier.weight(1f), onAccentChange)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(panel)
                .border(1.dp, green.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PhoneStepperSetting(
                    label = "Beats",
                    value = "$beatsPerMeasure/4",
                    modifier = Modifier.weight(1f),
                    onDecrease = { onBeatsPerMeasureChange(beatsPerMeasure - 1) },
                    onIncrease = { onBeatsPerMeasureChange(beatsPerMeasure + 1) },
                )
                PhoneStepperSetting(
                    label = "Sub",
                    value = "x$subdivisionCount",
                    modifier = Modifier.weight(1f),
                    onDecrease = { onSubdivisionChange(subdivisionCount - 1) },
                    onIncrease = { onSubdivisionChange(subdivisionCount + 1) },
                )
                PhoneStepperSetting(
                    label = "Nudge",
                    value = "${tempoNudgeMs}ms",
                    modifier = Modifier.weight(1f),
                    onDecrease = { onTempoNudgeChange(tempoNudgeMs - 50) },
                    onIncrease = { onTempoNudgeChange(tempoNudgeMs + 50) },
                )
            }

            PhoneSoundModeSetting(
                selected = beatSoundMode,
                onSelected = onBeatSoundModeChange,
            )
        }
    }
}

@Composable
private fun PhoneStatusDot(active: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(if (active) color else Color.White.copy(alpha = 0.18f)),
    )
}

@Composable
private fun PhoneCommandButton(
    label: String,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    onClick: () -> Unit,
) {
    val green = Color(0xFF9BFF00)
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(if (prominent) green else Color(0xFF20271A))
            .border(1.dp, green.copy(alpha = if (prominent) 0f else 0.36f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (prominent) Color.Black else Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhoneToggleButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelectedChange: (Boolean) -> Unit,
) {
    val green = Color(0xFF9BFF00)
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(if (selected) green.copy(alpha = 0.22f) else Color(0xFF11140F))
            .border(1.dp, if (selected) green else Color.White.copy(alpha = 0.16f), shape)
            .clickable { onSelectedChange(!selected) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) green else Color.White.copy(alpha = 0.72f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhoneStepperSetting(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val green = Color(0xFF9BFF00)
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(Color(0xFF11140F))
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PhoneMiniButton("-", onDecrease)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = value,
                color = green,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        PhoneMiniButton("+", onIncrease)
    }
}

@Composable
private fun PhoneSoundModeSetting(
    selected: BeatSoundMode,
    onSelected: (BeatSoundMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PhoneModeButton("Clicks", selected == BeatSoundMode.Clicks, Modifier.weight(1f)) {
            onSelected(BeatSoundMode.Clicks)
        }
        PhoneModeButton("Wood", selected == BeatSoundMode.Wood, Modifier.weight(1f)) {
            onSelected(BeatSoundMode.Wood)
        }
        PhoneModeButton("Bell", selected == BeatSoundMode.Bell, Modifier.weight(1f)) {
            onSelected(BeatSoundMode.Bell)
        }
    }
}

@Composable
private fun PhoneMiniButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhoneModeButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val green = Color(0xFF9BFF00)
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(if (selected) green.copy(alpha = 0.22f) else Color(0xFF11140F))
            .border(1.dp, if (selected) green else Color.White.copy(alpha = 0.14f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) green else Color.White.copy(alpha = 0.72f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

private fun Context.phoneLoadInt(key: String, fallback: Int): Int {
    return getSharedPreferences(PHONE_PREFS, Context.MODE_PRIVATE).getInt(key, fallback)
}

private fun Context.phoneSaveInt(key: String, value: Int) {
    getSharedPreferences(PHONE_PREFS, Context.MODE_PRIVATE).edit {
        putInt(key, value)
    }
}

private fun Context.phoneLoadBoolean(key: String, fallback: Boolean): Boolean {
    return getSharedPreferences(PHONE_PREFS, Context.MODE_PRIVATE).getBoolean(key, fallback)
}

private fun Context.phoneSaveBoolean(key: String, value: Boolean) {
    getSharedPreferences(PHONE_PREFS, Context.MODE_PRIVATE).edit {
        putBoolean(key, value)
    }
}
