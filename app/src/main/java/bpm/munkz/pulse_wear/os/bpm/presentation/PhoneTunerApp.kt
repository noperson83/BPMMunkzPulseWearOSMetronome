package bpm.munkz.pulse_wear.os.bpm.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.wear.compose.material3.Text
import bpm.munkz.pulse_wear.os.bpm.presentation.theme.BPMMunkzPulseTheme
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

private const val PHONE_TUNER_PREFS = "bpm_munkz_phone_tuner"
private const val PHONE_TUNER_A4_KEY = "a4_reference"
private const val PHONE_TUNER_PROFILE_KEY = "profile"
private const val PHONE_TUNER_READER_MODE_KEY = "reader_mode"
private const val PHONE_TUNER_TUNING_KEY = "tuning_choice"

@Composable
fun PhoneTunerApp() {
    val context = LocalContext.current
    var micPermissionGranted by remember {
        mutableStateOf(context.hasRecordAudioPermission())
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        micPermissionGranted = granted
    }
    var a4ReferenceHz by rememberSaveable {
        mutableIntStateOf(context.phoneTunerLoadInt(PHONE_TUNER_A4_KEY, DEFAULT_A4_REFERENCE_HZ))
    }
    var selectedProfile by rememberSaveable {
        mutableStateOf(
            context.phoneTunerLoadString(PHONE_TUNER_PROFILE_KEY)
                ?.let { persisted -> TunerListenProfile.entries.firstOrNull { it.name == persisted } }
                ?: TunerListenProfile.Guitar,
        )
    }
    var readerMode by rememberSaveable {
        mutableStateOf(
            context.phoneTunerLoadString(PHONE_TUNER_READER_MODE_KEY)
                ?.let { persisted -> SpectrumReaderMode.entries.firstOrNull { it.name == persisted } }
                ?: SpectrumReaderMode.Instrument,
        )
    }
    var tuningChoice by rememberSaveable {
        mutableStateOf(
            context.phoneTunerLoadString(PHONE_TUNER_TUNING_KEY)
                ?.let { persisted -> SpectrumTuningChoice.entries.firstOrNull { it.name == persisted } }
                ?: TunerListenProfile.Guitar.defaultSpectrumTuningChoice(),
        )
    }
    val effectiveTuningChoice = tuningChoice?.takeIf { choice ->
        readerMode == SpectrumReaderMode.Instrument && choice.profile == selectedProfile
    }
    val audioAnalysisState = rememberAudioAnalysisState(
        enabled = micPermissionGranted,
        listenProfile = selectedProfile,
        readerMode = readerMode,
        tuningChoice = effectiveTuningChoice,
        a4ReferenceHz = a4ReferenceHz,
        includeSpectrum = true,
    )

    LaunchedEffect(a4ReferenceHz, selectedProfile, readerMode, tuningChoice) {
        context.phoneTunerSaveInt(PHONE_TUNER_A4_KEY, a4ReferenceHz)
        context.phoneTunerSaveString(PHONE_TUNER_PROFILE_KEY, selectedProfile.name)
        context.phoneTunerSaveString(PHONE_TUNER_READER_MODE_KEY, readerMode.name)
        context.phoneTunerSaveString(PHONE_TUNER_TUNING_KEY, tuningChoice?.name.orEmpty())
    }

    BPMMunkzPulseTheme {
        PhoneTunerScreen(
            audioAnalysisState = audioAnalysisState,
            a4ReferenceHz = a4ReferenceHz.coerceIn(MIN_A4_REFERENCE_HZ, MAX_A4_REFERENCE_HZ),
            selectedProfile = selectedProfile,
            readerMode = readerMode,
            tuningChoice = effectiveTuningChoice,
            micPermissionGranted = micPermissionGranted,
            onRequestMicPermission = {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onA4ReferenceChange = { value ->
                a4ReferenceHz = value.coerceIn(MIN_A4_REFERENCE_HZ, MAX_A4_REFERENCE_HZ)
            },
            onProfileChoice = { profile ->
                selectedProfile = profile
                tuningChoice = profile.defaultSpectrumTuningChoice()
            },
            onReaderModeChoice = { mode ->
                readerMode = mode
            },
            onTuningChoice = { choice ->
                tuningChoice = choice
            },
        )
    }
}

@Composable
private fun PhoneTunerScreen(
    audioAnalysisState: AudioAnalysisState,
    a4ReferenceHz: Int,
    selectedProfile: TunerListenProfile,
    readerMode: SpectrumReaderMode,
    tuningChoice: SpectrumTuningChoice?,
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit,
    onA4ReferenceChange: (Int) -> Unit,
    onProfileChoice: (TunerListenProfile) -> Unit,
    onReaderModeChoice: (SpectrumReaderMode) -> Unit,
    onTuningChoice: (SpectrumTuningChoice?) -> Unit,
) {
    val green = Color(0xFF9BFF00)
    val background = Color(0xFF050604)
    val panel = Color(0xFF141812)
    val displayFrequency = audioAnalysisState.frequencyHz
        ?.takeIf { frequency -> frequency in selectedProfile.minHz..selectedProfile.maxHz }
    val noteReading = displayFrequency?.toNoteReading(a4ReferenceHz)
    val noteName = noteReading?.first ?: "--"
    val cents = noteReading?.second ?: 0
    val peakReading = audioAnalysisState.spectrum.peakSpectrumReading()
    val phrase = audioAnalysisState.songPhraseState()
    val tuningChoices = remember(selectedProfile) {
        spectrumTuningChoicesFor(selectedProfile)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BPM Munkz Tuner",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (micPermissionGranted) {
                        "${selectedProfile.frequencyRangeLabel()} : ${readerMode.label()}"
                    } else {
                        "Mic permission needed"
                    },
                    color = Color.White.copy(alpha = 0.64f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PhoneTunerLevelBadge(
                level = audioAnalysisState.level,
                active = micPermissionGranted,
            )
        }

        if (!micPermissionGranted) {
            PhoneTunerPanel {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Microphone access",
                        color = green,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "The phone tuner listens through the mic for pitch, spectrum, BPM, and key reads.",
                        color = Color.White.copy(alpha = 0.76f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                    PhoneTunerCommandButton(
                        label = "Enable mic",
                        prominent = true,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRequestMicPermission,
                    )
                }
            }
        }

        PhoneTunerReadoutPanel(
            noteName = noteName,
            frequencyHz = displayFrequency,
            cents = cents,
            level = audioAnalysisState.level,
        )

        PhoneTunerPanel {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PhoneTunerMetric(
                        label = "BPM",
                        value = audioAnalysisState.detectedTempoBpm?.toString() ?: "--",
                        modifier = Modifier.weight(1f),
                    )
                    PhoneTunerMetric(
                        label = "Key",
                        value = audioAnalysisState.guessedKey ?: "--",
                        modifier = Modifier.weight(1f),
                    )
                    PhoneTunerMetric(
                        label = "Peak",
                        value = peakReading?.let { "${it.frequencyHz.roundToInt()} Hz" } ?: "--",
                        modifier = Modifier.weight(1f),
                    )
                }
                PhoneTunerSpectrumBars(
                    spectrum = audioAnalysisState.spectrum,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp),
                )
                Text(
                    text = listOfNotNull(
                        peakReading?.let { "${it.bandLabel} ${it.frequencyHz.roundToInt()} Hz" },
                        audioAnalysisState.likelyChords.take(2).takeIf { it.isNotEmpty() }?.joinToString(" / "),
                        phrase.compactLabel().takeIf { it != "Learn" },
                    ).joinToString(" : ").ifBlank { "Listening for spectrum and key" },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        PhoneTunerPanel {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PhoneTunerSectionTitle("Reference")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PhoneTunerCommandButton("-1", Modifier.weight(1f)) {
                        onA4ReferenceChange(a4ReferenceHz - 1)
                    }
                    PhoneTunerMetric(
                        label = "A4",
                        value = "$a4ReferenceHz Hz",
                        modifier = Modifier.weight(1.35f),
                    )
                    PhoneTunerCommandButton("+1", Modifier.weight(1f)) {
                        onA4ReferenceChange(a4ReferenceHz + 1)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PhoneTunerCommandButton("-5", Modifier.weight(1f)) {
                        onA4ReferenceChange(a4ReferenceHz - 5)
                    }
                    PhoneTunerCommandButton("440", Modifier.weight(1f)) {
                        onA4ReferenceChange(DEFAULT_A4_REFERENCE_HZ)
                    }
                    PhoneTunerCommandButton("+5", Modifier.weight(1f)) {
                        onA4ReferenceChange(a4ReferenceHz + 5)
                    }
                }
            }
        }

        PhoneTunerPanel {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PhoneTunerSectionTitle("Mode")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PhoneTunerChoiceButton(
                        label = "Instrument",
                        selected = readerMode == SpectrumReaderMode.Instrument,
                        modifier = Modifier.weight(1f),
                    ) {
                        onReaderModeChoice(SpectrumReaderMode.Instrument)
                    }
                    PhoneTunerChoiceButton(
                        label = "Song",
                        selected = readerMode == SpectrumReaderMode.Song,
                        modifier = Modifier.weight(1f),
                    ) {
                        onReaderModeChoice(SpectrumReaderMode.Song)
                    }
                }
                SpectrumTunerListenProfiles.chunked(3).forEach { rowProfiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowProfiles.forEach { profile ->
                            PhoneTunerChoiceButton(
                                label = profile.label,
                                selected = profile == selectedProfile,
                                modifier = Modifier.weight(1f),
                            ) {
                                onProfileChoice(profile)
                            }
                        }
                        repeat(3 - rowProfiles.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (readerMode == SpectrumReaderMode.Instrument && tuningChoices.isNotEmpty()) {
                    tuningChoices.chunked(3).forEach { rowChoices ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowChoices.forEach { choice ->
                                PhoneTunerChoiceButton(
                                    label = choice.label,
                                    selected = choice == tuningChoice,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    onTuningChoice(choice)
                                }
                            }
                            repeat(3 - rowChoices.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneTunerReadoutPanel(
    noteName: String,
    frequencyHz: Float?,
    cents: Int,
    level: Float,
) {
    PhoneTunerPanel {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = noteName,
                color = Color(0xFF9BFF00),
                fontSize = 76.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = frequencyHz?.let { "${it.roundToInt()} Hz" } ?: "-- Hz",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            PhoneTunerNeedle(
                cents = cents,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${cents.coerceIn(-99, 99)} cents",
                    color = if (abs(cents) <= 5) Color(0xFF9BFF00) else Color.White.copy(alpha = 0.72f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Signal ${(level.coerceIn(0f, 1f) * 100f).roundToInt()}%",
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun PhoneTunerPanel(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141812))
            .border(1.dp, Color(0xFF9BFF00).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
    ) {
        content()
    }
}

@Composable
private fun PhoneTunerLevelBadge(
    level: Float,
    active: Boolean,
) {
    val green = Color(0xFF9BFF00)
    Box(
        modifier = Modifier
            .width(54.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(5.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(level.coerceIn(0.04f, 1f))
                .clip(RoundedCornerShape(5.dp))
                .background(if (active) green else Color.White.copy(alpha = 0.22f)),
        )
    }
}

@Composable
private fun PhoneTunerMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF11140F))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.58f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = Color(0xFF9BFF00),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhoneTunerSectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun PhoneTunerCommandButton(
    label: String,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    onClick: () -> Unit,
) {
    val green = Color(0xFF9BFF00)
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(if (prominent) green else Color(0xFF20271A))
            .border(1.dp, green.copy(alpha = if (prominent) 0f else 0.36f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (prominent) Color.Black else Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun PhoneTunerChoiceButton(
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PhoneTunerNeedle(
    cents: Int,
    modifier: Modifier = Modifier,
) {
    val green = Color(0xFF9BFF00)
    Canvas(modifier = modifier) {
        val centerY = size.height * 0.48f
        val startX = 14.dp.toPx()
        val endX = size.width - 14.dp.toPx()
        val centerX = size.width / 2f
        val clampedCents = cents.coerceIn(-100, 100)
        val needleX = startX + (endX - startX) * ((clampedCents + 100f) / 200f)
        drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = green,
            start = Offset(centerX, centerY - 22.dp.toPx()),
            end = Offset(centerX, centerY + 22.dp.toPx()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        listOf(startX, centerX, endX).forEach { tickX ->
            drawLine(
                color = Color.White.copy(alpha = 0.36f),
                start = Offset(tickX, centerY - 11.dp.toPx()),
                end = Offset(tickX, centerY + 11.dp.toPx()),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
        val tuned = abs(cents) <= 5
        drawCircle(
            color = if (tuned) green.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.13f),
            radius = if (tuned) 17.dp.toPx() else 13.dp.toPx(),
            center = Offset(needleX, centerY),
        )
        drawCircle(
            color = if (tuned) green else Color(0xFFFFC857),
            radius = if (tuned) 9.dp.toPx() else 7.dp.toPx(),
            center = Offset(needleX, centerY),
        )
    }
}

@Composable
private fun PhoneTunerSpectrumBars(
    spectrum: List<Float>,
    modifier: Modifier = Modifier,
) {
    val bands = spectrumBands()
    Canvas(modifier = modifier) {
        if (spectrum.isEmpty()) return@Canvas
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (spectrum.size - 1)) / spectrum.size).coerceAtLeast(2f)
        spectrum.forEachIndexed { index, level ->
            val left = index * (barWidth + gap)
            val frequency = 30f * (10_000f / 30f).pow(index.toFloat() / (spectrum.size - 1))
            val color = spectrumBandForFrequency(frequency, bands).color
            val barHeight = (size.height * level.coerceIn(0.03f, 1f)).coerceAtLeast(3.dp.toPx())
            drawLine(
                color = color.copy(alpha = 0.9f),
                start = Offset(left + barWidth / 2f, size.height),
                end = Offset(left + barWidth / 2f, size.height - barHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun SpectrumReaderMode.label(): String {
    return when (this) {
        SpectrumReaderMode.Instrument -> "Instrument"
        SpectrumReaderMode.Song -> "Song"
    }
}

private fun Context.hasRecordAudioPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun Context.phoneTunerLoadInt(key: String, fallback: Int): Int {
    return getSharedPreferences(PHONE_TUNER_PREFS, Context.MODE_PRIVATE).getInt(key, fallback)
}

private fun Context.phoneTunerSaveInt(key: String, value: Int) {
    getSharedPreferences(PHONE_TUNER_PREFS, Context.MODE_PRIVATE).edit {
        putInt(key, value)
    }
}

private fun Context.phoneTunerLoadString(key: String): String? {
    return getSharedPreferences(PHONE_TUNER_PREFS, Context.MODE_PRIVATE)
        .getString(key, null)
        ?.takeIf { it.isNotBlank() }
}

private fun Context.phoneTunerSaveString(key: String, value: String) {
    getSharedPreferences(PHONE_TUNER_PREFS, Context.MODE_PRIVATE).edit {
        putString(key, value)
    }
}
