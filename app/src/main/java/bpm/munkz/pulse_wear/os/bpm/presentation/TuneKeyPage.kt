package bpm.munkz.pulse_wear.os.bpm.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
internal fun TuneKeyPage(
    appText: AppText,
    audioAnalysisState: AudioAnalysisState,
    micPermissionGranted: Boolean,
    showSaveKey: Boolean = true,
    onSaveKey: (String) -> Unit,
    onRequestMicPermission: () -> Unit,
) {
    val guessedKey = audioAnalysisState.guessedKey

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val watchSClass = minOf(maxWidth, maxHeight) <= 200.dp
        val titleFontSize = if (watchSClass) 15.sp else 17.sp
        val keyFontSize = if (watchSClass) 36.sp else 42.sp
        val labelFontSize = if (watchSClass) 10.sp else 11.sp
        val chordFontSize = if (watchSClass) 13.sp else 15.sp
        val notesFontSize = if (watchSClass) 9.sp else 10.sp

        if (!micPermissionGranted) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = appText.keyGuess,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                MicPermissionButton(appText = appText, onClick = onRequestMicPermission)
            }
            return@BoxWithConstraints
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = appText.keyGuess,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(if (watchSClass) 6.dp else 8.dp))

            Text(
                text = guessedKey ?: "--",
                fontSize = keyFontSize,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(if (watchSClass) 4.dp else 6.dp))

            Text(
                text = "Likely chords",
                fontSize = labelFontSize,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val chords = audioAnalysisState.likelyChords.ifEmpty { listOf("--", "--", "--") }
                chords.take(3).forEach { chord ->
                    Text(
                        text = chord,
                        modifier = Modifier.width(if (watchSClass) 38.dp else 42.dp),
                        fontSize = chordFontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (watchSClass) 5.dp else 7.dp))

            Text(
                text = audioAnalysisState.recentNotes.joinToString(" ").ifBlank { "--" },
                modifier = Modifier.width(if (watchSClass) 132.dp else 150.dp),
                fontSize = notesFontSize,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )

            if (showSaveKey) {
                Spacer(modifier = Modifier.height(if (watchSClass) 7.dp else 9.dp))

                ChoicePillButton(
                    text = appText.saveKey,
                    selected = false,
                    enabled = guessedKey != null,
                    modifier = Modifier
                        .width(if (watchSClass) 92.dp else 104.dp)
                        .height(if (watchSClass) 27.dp else 30.dp),
                    fontSize = if (watchSClass) 10.sp else 11.sp,
                    onClick = {
                        guessedKey?.let(onSaveKey)
                    },
                )
            }
        }
    }
}
