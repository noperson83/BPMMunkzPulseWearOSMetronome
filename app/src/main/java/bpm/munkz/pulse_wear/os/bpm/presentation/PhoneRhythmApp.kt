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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

private const val PHONE_RHYTHM_PREFS = "bpm_munkz_phone_rhythm"
private const val PHONE_RHYTHM_BPM_KEY = "bpm"
private const val PHONE_RHYTHM_BEATS_KEY = "beats_per_measure"
private const val PHONE_RHYTHM_SUBDIVISION_KEY = "subdivision"
private const val PHONE_RHYTHM_ACCENTS_KEY = "accents"
private const val PHONE_RHYTHM_BEEP_KEY = "beep"
private const val PHONE_RHYTHM_HAPTICS_KEY = "haptics"
private const val PHONE_RHYTHM_SOUND_MODE_KEY = "sound_mode"
private const val PHONE_RHYTHM_TEMPO_NUDGE_KEY = "tempo_nudge"
private const val PHONE_RHYTHM_DRONE_KEY = "drone"
private const val PHONE_RHYTHM_DRONE_VOLUME_KEY = "drone_volume"
private const val PHONE_RHYTHM_KEY_KEY = "musical_key"

@Composable
fun PhoneRhythmApp() {
    val context = LocalContext.current
    var bpm by rememberSaveable { mutableIntStateOf(context.phoneRhythmLoadInt(PHONE_RHYTHM_BPM_KEY, 96)) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var beatsPerMeasure by rememberSaveable {
        mutableIntStateOf(context.phoneRhythmLoadInt(PHONE_RHYTHM_BEATS_KEY, 4).coerceIn(2, 16))
    }
    var subdivisionCount by rememberSaveable {
        mutableIntStateOf(context.phoneRhythmLoadInt(PHONE_RHYTHM_SUBDIVISION_KEY, 1).toPhoneRhythmSubdivision())
    }
    var beatAccentTypes by remember {
        mutableStateOf(context.phoneRhythmLoadAccentTypes(beatsPerMeasure))
    }
    var beepEnabled by rememberSaveable {
        mutableStateOf(context.phoneRhythmLoadBoolean(PHONE_RHYTHM_BEEP_KEY, true))
    }
    var hapticsEnabled by rememberSaveable {
        mutableStateOf(context.phoneRhythmLoadBoolean(PHONE_RHYTHM_HAPTICS_KEY, true))
    }
    var beatSoundMode by rememberSaveable {
        mutableStateOf(
            BeatSoundMode.fromPersistedValue(
                context.phoneRhythmLoadInt(PHONE_RHYTHM_SOUND_MODE_KEY, BeatSoundMode.Clicks.persistedValue),
            ),
        )
    }
    var tempoNudgeMs by rememberSaveable {
        mutableIntStateOf(context.phoneRhythmLoadInt(PHONE_RHYTHM_TEMPO_NUDGE_KEY, 150).coerceIn(50, 250))
    }
    var keyDroneEnabled by rememberSaveable {
        mutableStateOf(context.phoneRhythmLoadBoolean(PHONE_RHYTHM_DRONE_KEY, false))
    }
    var keyDroneVolumePercent by rememberSaveable {
        mutableIntStateOf(context.phoneRhythmLoadInt(PHONE_RHYTHM_DRONE_VOLUME_KEY, 18).coerceIn(0, 100))
    }
    var musicalKey by rememberSaveable {
        mutableStateOf(context.phoneRhythmLoadString(PHONE_RHYTHM_KEY_KEY, "C"))
    }
    var tapTimes by remember { mutableStateOf(emptyList<Long>()) }
    var pulseOn by remember { mutableStateOf(false) }

    LaunchedEffect(beatsPerMeasure) {
        beatAccentTypes = beatAccentTypes.phoneRhythmNormalizedAccents(beatsPerMeasure)
    }

    val metronomeState = remember(
        bpm,
        beatsPerMeasure,
        subdivisionCount,
        beatAccentTypes,
        beepEnabled,
        hapticsEnabled,
        beatSoundMode,
        tempoNudgeMs,
        keyDroneEnabled,
        keyDroneVolumePercent,
        musicalKey,
        isRunning,
    ) {
        MetronomeState(
            bpm = bpm.coerceIn(MIN_BPM, MAX_BPM),
            beatsPerMeasure = beatsPerMeasure.coerceIn(2, 16),
            accentBeat = beatAccentTypes.phoneRhythmPrimaryAccentBeat(),
            subdivisionCount = subdivisionCount.toPhoneRhythmSubdivision(),
            beatAccentTypes = beatAccentTypes.phoneRhythmNormalizedAccents(beatsPerMeasure),
            hapticsEnabled = hapticsEnabled,
            beepEnabled = beepEnabled,
            beatSoundMode = beatSoundMode,
            keyDroneEnabled = keyDroneEnabled,
            keyDroneVolumePercent = keyDroneVolumePercent.coerceIn(0, 100),
            musicalKey = musicalKey,
            tempoNudgeMs = tempoNudgeMs.coerceIn(50, 250),
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

    LaunchedEffect(metronomeState) {
        context.phoneRhythmSaveInt(PHONE_RHYTHM_BPM_KEY, metronomeState.bpm)
        context.phoneRhythmSaveInt(PHONE_RHYTHM_BEATS_KEY, metronomeState.beatsPerMeasure)
        context.phoneRhythmSaveInt(PHONE_RHYTHM_SUBDIVISION_KEY, metronomeState.subdivisionCount)
        context.phoneRhythmSaveString(PHONE_RHYTHM_ACCENTS_KEY, metronomeState.beatAccentTypes.phoneRhythmPersistedString())
        context.phoneRhythmSaveBoolean(PHONE_RHYTHM_BEEP_KEY, metronomeState.beepEnabled)
        context.phoneRhythmSaveBoolean(PHONE_RHYTHM_HAPTICS_KEY, metronomeState.hapticsEnabled)
        context.phoneRhythmSaveInt(PHONE_RHYTHM_SOUND_MODE_KEY, metronomeState.beatSoundMode.persistedValue)
        context.phoneRhythmSaveInt(PHONE_RHYTHM_TEMPO_NUDGE_KEY, metronomeState.tempoNudgeMs)
        context.phoneRhythmSaveBoolean(PHONE_RHYTHM_DRONE_KEY, metronomeState.keyDroneEnabled)
        context.phoneRhythmSaveInt(PHONE_RHYTHM_DRONE_VOLUME_KEY, metronomeState.keyDroneVolumePercent)
        context.phoneRhythmSaveString(PHONE_RHYTHM_KEY_KEY, metronomeState.musicalKey)
    }

    LaunchedEffect(isRunning, bpm, subdivisionCount) {
        pulseOn = false
        while (isRunning) {
            pulseOn = true
            delay(90L)
            pulseOn = false
            delay((60_000L / bpm.coerceAtLeast(1) / subdivisionCount.toPhoneRhythmSubdivision() - 90L).coerceAtLeast(45L))
        }
    }

    BPMMunkzPulseTheme {
        PhoneRhythmScreen(
            state = metronomeState,
            pulseOn = pulseOn,
            onBpmChange = { bpm = it.coerceIn(MIN_BPM, MAX_BPM) },
            onRunningChange = { isRunning = it },
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
            onBeatsPerMeasureChange = { beatsPerMeasure = it.coerceIn(2, 16) },
            onSubdivisionChange = { subdivisionCount = it.toPhoneRhythmSubdivision() },
            onAccentClick = { beat ->
                beatAccentTypes = beatAccentTypes
                    .phoneRhythmNormalizedAccents(beatsPerMeasure)
                    .mapIndexed { index, accentType ->
                        if (index + 1 == beat) accentType.next() else accentType
                    }
            },
            onPreset = { nextAccents, nextSubdivision ->
                beatAccentTypes = nextAccents.phoneRhythmNormalizedAccents(beatsPerMeasure)
                subdivisionCount = nextSubdivision.toPhoneRhythmSubdivision()
            },
            onBeepChange = { beepEnabled = it },
            onHapticsChange = { hapticsEnabled = it },
            onBeatSoundModeChange = { beatSoundMode = it },
            onTempoNudgeChange = { tempoNudgeMs = it.coerceIn(50, 250) },
            onKeyDroneChange = { keyDroneEnabled = it },
            onKeyDroneVolumeChange = { keyDroneVolumePercent = it.coerceIn(0, 100) },
            onMusicalKeyChange = { musicalKey = it },
        )
    }
}

@Composable
private fun PhoneRhythmScreen(
    state: MetronomeState,
    pulseOn: Boolean,
    onBpmChange: (Int) -> Unit,
    onRunningChange: (Boolean) -> Unit,
    onTapTempo: () -> Unit,
    onBeatsPerMeasureChange: (Int) -> Unit,
    onSubdivisionChange: (Int) -> Unit,
    onAccentClick: (Int) -> Unit,
    onPreset: (List<BeatAccentType>, Int) -> Unit,
    onBeepChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onBeatSoundModeChange: (BeatSoundMode) -> Unit,
    onTempoNudgeChange: (Int) -> Unit,
    onKeyDroneChange: (Boolean) -> Unit,
    onKeyDroneVolumeChange: (Int) -> Unit,
    onMusicalKeyChange: (String) -> Unit,
) {
    val green = Color(0xFF9BFF00)
    val amber = Color(0xFFFFB020)
    val background = Color(0xFF050604)
    val panel = Color(0xFF151812)
    val surface = Color(0xFF202619)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
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
                    text = "Munkz Rhythm",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${state.beatsPerMeasure}/4  x${state.subdivisionCount}  ${state.beatSoundMode.name}",
                    color = if (state.isRunning) green else Color.White.copy(alpha = 0.64f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            PhoneRhythmStatusPill(active = state.isRunning)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(panel)
                .border(1.dp, green.copy(alpha = 0.24f), RoundedCornerShape(8.dp))
                .clickable(onClick = onTapTempo),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(if (pulseOn) 230.dp else 204.dp)) {
                drawCircle(color = green.copy(alpha = if (pulseOn) 0.25f else 0.12f))
                drawCircle(
                    color = green.copy(alpha = if (pulseOn) 0.88f else 0.56f),
                    radius = size.minDimension * 0.36f,
                )
                drawCircle(color = background, radius = size.minDimension * 0.25f)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.bpm.toString(),
                    color = Color.White,
                    fontSize = 78.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "TAP TEMPO",
                    color = green,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PhoneRhythmButton("-5", Modifier.weight(1f)) { onBpmChange(state.bpm - 5) }
            PhoneRhythmButton("-1", Modifier.weight(1f)) { onBpmChange(state.bpm - 1) }
            PhoneRhythmButton("+1", Modifier.weight(1f)) { onBpmChange(state.bpm + 1) }
            PhoneRhythmButton("+5", Modifier.weight(1f)) { onBpmChange(state.bpm + 5) }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhoneRhythmButton(
                label = if (state.isRunning) "Stop" else "Start",
                modifier = Modifier.weight(1.3f),
                prominent = true,
            ) {
                onRunningChange(!state.isRunning)
            }
            PhoneRhythmButton("Tap", Modifier.weight(1f), onClick = onTapTempo)
        }

        PhoneRhythmPanel {
            PhoneRhythmPanelHeader("Rhythm")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhoneRhythmStepper(
                    label = "Meter",
                    value = "${state.beatsPerMeasure}/4",
                    modifier = Modifier.weight(1f),
                    onDecrease = { onBeatsPerMeasureChange(state.beatsPerMeasure - 1) },
                    onIncrease = { onBeatsPerMeasureChange(state.beatsPerMeasure + 1) },
                )
                PhoneRhythmStepper(
                    label = "Subdivision",
                    value = "x${state.subdivisionCount}",
                    modifier = Modifier.weight(1f),
                    onDecrease = { onSubdivisionChange(state.subdivisionCount - 1) },
                    onIncrease = { onSubdivisionChange(state.subdivisionCount + 1) },
                )
                PhoneRhythmStepper(
                    label = "Nudge",
                    value = "${state.tempoNudgeMs}ms",
                    modifier = Modifier.weight(1f),
                    onDecrease = { onTempoNudgeChange(state.tempoNudgeMs - 25) },
                    onIncrease = { onTempoNudgeChange(state.tempoNudgeMs + 25) },
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhoneRhythmButton(
                    label = "On 1",
                    modifier = Modifier.weight(1f),
                    compact = true,
                ) {
                    onPreset(defaultBeatAccentTypes(state.beatsPerMeasure, 1), 1)
                }
                PhoneRhythmButton(
                    label = "Backbeat",
                    modifier = Modifier.weight(1f),
                    compact = true,
                ) {
                    onPreset(state.beatsPerMeasure.phoneRhythmBackbeatAccents(), 1)
                }
                PhoneRhythmButton(
                    label = "All",
                    modifier = Modifier.weight(1f),
                    compact = true,
                ) {
                    onPreset(List(state.beatsPerMeasure) { BeatAccentType.Medium }, 1)
                }
            }
            PhoneRhythmAccentRow(
                beatAccentTypes = state.beatAccentTypes,
                beatsPerMeasure = state.beatsPerMeasure,
                onAccentClick = onAccentClick,
            )
        }

        PhoneRhythmPanel {
            PhoneRhythmPanelHeader("Feel")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhoneRhythmToggle("Beep", state.beepEnabled, Modifier.weight(1f), onBeepChange)
                PhoneRhythmToggle("Haptic", state.hapticsEnabled, Modifier.weight(1f), onHapticsChange)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhoneRhythmMode("Clicks", state.beatSoundMode == BeatSoundMode.Clicks, Modifier.weight(1f)) {
                    onBeatSoundModeChange(BeatSoundMode.Clicks)
                }
                PhoneRhythmMode("Wood", state.beatSoundMode == BeatSoundMode.Wood, Modifier.weight(1f)) {
                    onBeatSoundModeChange(BeatSoundMode.Wood)
                }
                PhoneRhythmMode("Bell", state.beatSoundMode == BeatSoundMode.Bell, Modifier.weight(1f)) {
                    onBeatSoundModeChange(BeatSoundMode.Bell)
                }
            }
        }

        PhoneRhythmPanel {
            PhoneRhythmPanelHeader("Drone")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("C", "D", "E", "F", "G", "A", "B").forEach { key ->
                    PhoneRhythmMode(
                        label = key,
                        selected = state.musicalKey == key,
                        modifier = Modifier.weight(1f),
                        compact = true,
                    ) {
                        onMusicalKeyChange(key)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhoneRhythmToggle("Drone", state.keyDroneEnabled, Modifier.weight(1f), onKeyDroneChange)
                PhoneRhythmStepper(
                    label = "Volume",
                    value = "${state.keyDroneVolumePercent}%",
                    modifier = Modifier.weight(1.4f),
                    onDecrease = { onKeyDroneVolumeChange(state.keyDroneVolumePercent - 5) },
                    onIncrease = { onKeyDroneVolumeChange(state.keyDroneVolumePercent + 5) },
                )
            }
        }

        Text(
            text = "Tap a beat below to cycle Big, Medium, Small, Silent.",
            color = amber.copy(alpha = 0.9f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PhoneRhythmPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151812))
            .border(1.dp, Color(0xFF9BFF00).copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        content = content,
    )
}

@Composable
private fun PhoneRhythmPanelHeader(label: String) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun PhoneRhythmStatusPill(active: Boolean) {
    val green = Color(0xFF9BFF00)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) green.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f))
            .border(1.dp, if (active) green else Color.White.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (active) green else Color.White.copy(alpha = 0.38f)),
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = if (active) "Live" else "Ready",
            color = if (active) green else Color.White.copy(alpha = 0.72f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PhoneRhythmButton(
    label: String,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val green = Color(0xFF9BFF00)
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(if (compact) 42.dp else 54.dp)
            .clip(shape)
            .background(if (prominent) green else Color(0xFF202619))
            .border(1.dp, if (prominent) Color.Transparent else green.copy(alpha = 0.34f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (prominent) Color.Black else Color.White,
            fontSize = if (compact) 13.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhoneRhythmToggle(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelectedChange: (Boolean) -> Unit,
) {
    PhoneRhythmMode(label, selected, modifier) {
        onSelectedChange(!selected)
    }
}

@Composable
private fun PhoneRhythmMode(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val green = Color(0xFF9BFF00)
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(if (compact) 36.dp else 44.dp)
            .clip(shape)
            .background(if (selected) green.copy(alpha = 0.2f) else Color(0xFF10130E))
            .border(1.dp, if (selected) green else Color.White.copy(alpha = 0.14f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) green else Color.White.copy(alpha = 0.72f),
            fontSize = if (compact) 12.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhoneRhythmStepper(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF10130E))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PhoneRhythmMiniButton("-", onDecrease)
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
                color = Color(0xFF9BFF00),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        PhoneRhythmMiniButton("+", onIncrease)
    }
}

@Composable
private fun PhoneRhythmMiniButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhoneRhythmAccentRow(
    beatAccentTypes: List<BeatAccentType>,
    beatsPerMeasure: Int,
    onAccentClick: (Int) -> Unit,
) {
    val normalized = beatAccentTypes.phoneRhythmNormalizedAccents(beatsPerMeasure)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        normalized.forEachIndexed { index, accentType ->
            PhoneRhythmAccentButton(
                beat = index + 1,
                accentType = accentType,
                modifier = Modifier.weight(1f),
                onClick = { onAccentClick(index + 1) },
            )
        }
    }
}

@Composable
private fun PhoneRhythmAccentButton(
    beat: Int,
    accentType: BeatAccentType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val color = when (accentType) {
        BeatAccentType.Big -> Color(0xFF9BFF00)
        BeatAccentType.Medium -> Color(0xFFFFB020)
        BeatAccentType.Small -> Color(0xFF62D9FF)
        BeatAccentType.Silent -> Color.White.copy(alpha = 0.28f)
    }
    val label = when (accentType) {
        BeatAccentType.Big -> "Big"
        BeatAccentType.Medium -> "Med"
        BeatAccentType.Small -> "Low"
        BeatAccentType.Silent -> "Mute"
    }
    Column(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color.copy(alpha = 0.82f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = beat.toString(),
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun Context.phoneRhythmPrefs() = getSharedPreferences(PHONE_RHYTHM_PREFS, Context.MODE_PRIVATE)

private fun Context.phoneRhythmLoadInt(key: String, fallback: Int): Int {
    return phoneRhythmPrefs().getInt(key, fallback)
}

private fun Context.phoneRhythmSaveInt(key: String, value: Int) {
    phoneRhythmPrefs().edit { putInt(key, value) }
}

private fun Context.phoneRhythmLoadBoolean(key: String, fallback: Boolean): Boolean {
    return phoneRhythmPrefs().getBoolean(key, fallback)
}

private fun Context.phoneRhythmSaveBoolean(key: String, value: Boolean) {
    phoneRhythmPrefs().edit { putBoolean(key, value) }
}

private fun Context.phoneRhythmLoadString(key: String, fallback: String): String {
    return phoneRhythmPrefs().getString(key, fallback) ?: fallback
}

private fun Context.phoneRhythmSaveString(key: String, value: String) {
    phoneRhythmPrefs().edit { putString(key, value) }
}

private fun Context.phoneRhythmLoadAccentTypes(beatsPerMeasure: Int): List<BeatAccentType> {
    val persisted = phoneRhythmLoadString(PHONE_RHYTHM_ACCENTS_KEY, "")
    return persisted
        .split(",")
        .mapNotNull { it.toIntOrNull() }
        .map { BeatAccentType.fromPersistedValue(it) }
        .phoneRhythmNormalizedAccents(beatsPerMeasure)
}

private fun List<BeatAccentType>.phoneRhythmPersistedString(): String {
    return joinToString(",") { it.persistedValue.toString() }
}

private fun List<BeatAccentType>.phoneRhythmNormalizedAccents(beatsPerMeasure: Int): List<BeatAccentType> {
    val safeBeats = beatsPerMeasure.coerceIn(2, 16)
    if (isEmpty()) return defaultBeatAccentTypes(safeBeats, 1)
    return take(safeBeats) + List((safeBeats - size).coerceAtLeast(0)) { BeatAccentType.Medium }
}

private fun List<BeatAccentType>.phoneRhythmPrimaryAccentBeat(): Int {
    return indexOfFirst { it == BeatAccentType.Big }
        .takeIf { it >= 0 }
        ?.plus(1)
        ?: 1
}

private fun Int.toPhoneRhythmSubdivision(): Int {
    return when {
        this <= 1 -> 1
        this == 2 -> 2
        this == 3 -> 3
        this == 4 -> 4
        else -> 6
    }
}

private fun Int.phoneRhythmBackbeatAccents(): List<BeatAccentType> {
    return List(coerceIn(2, 16)) { index ->
        when (index + 1) {
            1 -> BeatAccentType.Big
            2, 4 -> BeatAccentType.Medium
            else -> BeatAccentType.Small
        }
    }
}
