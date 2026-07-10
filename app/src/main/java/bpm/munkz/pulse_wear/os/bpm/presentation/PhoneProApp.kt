package bpm.munkz.pulse_wear.os.bpm.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import bpm.munkz.pulse_wear.os.bpm.presentation.theme.BPMMunkzPulseTheme

private enum class PhoneProTool(
    val label: String,
    val detail: String,
) {
    Metronome(
        label = "Metronome",
        detail = "Tap tempo, tempo controls, beat feel, and timing settings.",
    ),
    Rhythm(
        label = "Rhythm",
        detail = "Patterns, subdivisions, accents, drone, and performance feel.",
    ),
    Tuner(
        label = "Tuner",
        detail = "Instrument tuner, spectrum, key tools, and A4 reference.",
    ),
    Setlist(
        label = "Setlist",
        detail = "Songs, notes, BPM, keys, and set navigation.",
    ),
}

@Composable
fun PhoneProApp() {
    var selectedTool by rememberSaveable { mutableStateOf<PhoneProTool?>(null) }

    when (selectedTool) {
        PhoneProTool.Metronome -> PhoneProToolFrame(
            title = "Metronome",
            onBack = { selectedTool = null },
        ) {
            PhoneMetronomeApp()
        }
        PhoneProTool.Rhythm -> PhoneProToolFrame(
            title = "Rhythm",
            onBack = { selectedTool = null },
        ) {
            PhoneRhythmApp()
        }
        PhoneProTool.Tuner -> PhoneProToolFrame(
            title = "Tuner",
            onBack = { selectedTool = null },
        ) {
            PhoneTunerApp()
        }
        PhoneProTool.Setlist -> PhoneProToolFrame(
            title = "Setlist",
            onBack = { selectedTool = null },
        ) {
            PhonePlaylistApp()
        }
        null -> PhoneProHome(
            onToolSelected = { selectedTool = it },
        )
    }
}

@Composable
private fun PhoneProHome(
    onToolSelected: (PhoneProTool) -> Unit,
) {
    val green = Color(0xFF9BFF00)
    val background = Color(0xFF050604)

    BPMMunkzPulseTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "BPM Munkz Pulse Pro",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Phone suite",
                    color = green,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            PhoneProHero()

            PhoneProTool.entries.forEach { tool ->
                PhoneProToolCard(
                    tool = tool,
                    onClick = { onToolSelected(tool) },
                )
            }
        }
    }
}

@Composable
private fun PhoneProHero() {
    val green = Color(0xFF9BFF00)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151812))
            .border(1.dp, green.copy(alpha = 0.24f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "4",
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "TOOLS READY",
                color = green,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PhoneProToolCard(
    tool: PhoneProTool,
    onClick: () -> Unit,
) {
    val green = Color(0xFF9BFF00)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151812))
            .border(1.dp, green.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .height(54.dp)
                .weight(0.42f)
                .clip(RoundedCornerShape(8.dp))
                .background(green.copy(alpha = 0.18f))
                .border(1.dp, green.copy(alpha = 0.65f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tool.label.first().toString(),
                color = green,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier.weight(1.8f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = tool.label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = tool.detail,
                color = Color.White.copy(alpha = 0.66f),
                fontSize = 12.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun PhoneProToolFrame(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 14.dp, top = 10.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.72f))
                .border(1.dp, Color(0xFF9BFF00).copy(alpha = 0.44f), RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "< $title",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
