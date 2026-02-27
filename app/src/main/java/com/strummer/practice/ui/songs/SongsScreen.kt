package com.strummer.practice.ui.songs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strummer.practice.library.BarChordStep
import kotlin.math.roundToInt

@Composable
fun SongsScreen(
    viewModel: SongsViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var songsExpanded by remember { mutableStateOf(false) }
    var pendingAddSongUri by remember { mutableStateOf<Uri?>(null) }
    var addChordName by remember { mutableStateOf("") }
    var addBarsInput by remember { mutableStateOf("4") }
    var setStepInput by remember { mutableStateOf("1") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingAddSongUri = uri
        if (uri != null) {
            viewModel.addSong(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Practice Library", style = MaterialTheme.typography.headlineSmall)

        if (state.errorMessage != null) {
            Text(state.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
        }
        if (state.infoMessage != null) {
            Text(state.infoMessage ?: "", color = MaterialTheme.colorScheme.primary)
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.songTitleInput,
                    onValueChange = viewModel::setSongTitleInput,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { picker.launch(arrayOf("audio/mpeg", "audio/mp3", "audio/*")) }) {
                        Text("Add")
                    }
                    Button(onClick = { songsExpanded = true }, enabled = state.songs.isNotEmpty()) {
                        Text("Select")
                    }
                }
                DropdownMenu(expanded = songsExpanded, onDismissRequest = { songsExpanded = false }) {
                    state.songs.forEach { song ->
                        DropdownMenuItem(
                            text = { Text(song.title) },
                            onClick = {
                                viewModel.selectSong(song.id)
                                songsExpanded = false
                            }
                        )
                    }
                }

                if (state.songs.isEmpty()) {
                    Text("No songs yet. Enter title and tap Add.")
                }
                if (state.missingFileMessage != null) {
                    Text(state.missingFileMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
                if (pendingAddSongUri != null) {
                    Text("Imported: ${pendingAddSongUri.toString()}")
                }
            }
        }

        if (state.selectedSong != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Song Metadata", style = MaterialTheme.typography.titleMedium)
                    Text("Title: ${state.selectedSong?.title.orEmpty()}")
                    Text("Source path: ${state.selectedSong?.audioFilePath.orEmpty()}")
                    Text("Duration: ${formatMs(state.durationMs)}")
                    Button(onClick = viewModel::deleteSelectedSong) { Text("Delete") }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Playback", style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = viewModel::playPause,
                            enabled = state.selectedSong != null && state.missingFileMessage == null
                        ) {
                            Text(if (state.isPlaying) "Pause" else "Play")
                        }
                    }
                    Text(
                        text = state.currentChord,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Current Step: ${state.currentStepNumber ?: "-"}")
                    Text("Next: ${state.nextChord} (${formatBarValue(state.barsUntilNextChange)} bars)")
                    Text(
                        "Bar ${formatBarValue(state.absoluteBar)} (Loop ${formatBarValue(state.loopBar)}/${formatBarValue(state.totalLoopBars.coerceAtLeast(1.0))})"
                    )

                    val safeDuration = state.durationMs.coerceAtLeast(1L)
                    Slider(
                        value = state.positionMs.coerceAtMost(safeDuration).toFloat(),
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..safeDuration.toFloat()
                    )
                    Text("${formatMs(state.positionMs)} / ${formatMs(state.durationMs)}")

                    Text("Speed ${"%.2f".format(state.speed)}x")
                    Slider(
                        value = state.speed,
                        onValueChange = viewModel::setSpeed,
                        valueRange = 0.5f..1.25f
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.6f, 0.7f, 0.8f, 0.9f, 1.0f).forEach { speed ->
                            FilterChip(
                                selected = kotlin.math.abs(state.speed - speed) < 0.005f,
                                onClick = { viewModel.setSpeed(speed) },
                                label = { Text("${"%.1f".format(speed)}x") }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.seekByBars(-1) }) { Text("-1 Bar") }
                        Button(onClick = { viewModel.seekByBars(1) }) { Text("+1 Bar") }
                        OutlinedTextField(
                            value = setStepInput,
                            onValueChange = { setStepInput = it },
                            label = { Text("Step #") },
                            singleLine = true,
                            modifier = Modifier.width(88.dp)
                        )
                        Button(onClick = {
                            val stepNumber = setStepInput.toIntOrNull()
                            val resolvedStepNumber = stepNumber ?: state.currentStepNumber
                            val didSet = viewModel.setStepStartToCurrentLoopBar(stepNumber)
                            if (didSet && resolvedStepNumber != null && state.barSteps.isNotEmpty()) {
                                val nextStepNumber = (resolvedStepNumber % state.barSteps.size) + 1
                                setStepInput = nextStepNumber.toString()
                            }
                        }) {
                            Text("Set Step")
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Chord Timeline", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.tempoBpm.toString(),
                            onValueChange = { it.toIntOrNull()?.let(viewModel::setTempoBpm) },
                            label = { Text("Tempo (BPM)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.timeSignatureTop.toString(),
                            onValueChange = { it.toIntOrNull()?.let(viewModel::setTimeSignatureTop) },
                            label = { Text("Time Sig Top") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = "${state.timeSignatureBottom}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Bottom") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = addChordName,
                            onValueChange = { addChordName = it },
                            label = { Text("Chord") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = addBarsInput,
                            onValueChange = { addBarsInput = it },
                            label = { Text("Bars") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            val bars = addBarsInput.toDoubleOrNull() ?: return@Button
                            viewModel.addStep(addChordName, bars)
                            addChordName = ""
                        }) {
                            Text("Add Step")
                        }
                    }

                    if (state.barSteps.isEmpty()) {
                        Text("No steps yet. Example: G 4, D 4, Am 8...")
                    } else {
                        BarStepList(state.barSteps, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun BarStepList(steps: List<BarChordStep>, viewModel: SongsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.sortedBy { it.displayOrder }.forEach { step ->
            val stepNumber = step.displayOrder + 1
            key(step.id) {
                var isEditing by remember(step.id) { mutableStateOf(false) }
                var chord by remember(step.id, step.chordName) { mutableStateOf(step.chordName) }
                var bars by remember(step.id, step.barCount) { mutableStateOf(barValueToInput(step.barCount)) }
                var startBar by remember(step.id, step.startBar) { mutableStateOf(barValueToInput(step.startBar)) }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Step $stepNumber")
                        if (!isEditing) {
                            Text(
                                "Starts at bar ${formatBarValue(step.startBar)} • ${step.chordName} for ${formatBarValue(step.barCount)} bars"
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    startBar = barValueToInput(step.startBar)
                                    chord = step.chordName
                                    bars = barValueToInput(step.barCount)
                                    isEditing = true
                                }) {
                                    Text("Edit")
                                }
                                Button(onClick = { viewModel.deleteStep(step.id) }) {
                                    Text("Delete Step")
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = startBar,
                                    onValueChange = { startBar = it },
                                    label = { Text("Start Bar") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = chord,
                                    onValueChange = { chord = it },
                                    label = { Text("Chord") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = bars,
                                    onValueChange = { bars = it },
                                    label = { Text("Bars") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val parsedStartBar = startBar.toDoubleOrNull() ?: return@Button
                                    val count = bars.toDoubleOrNull() ?: return@Button
                                    val normalizedChord = chord.trim()
                                    if (normalizedChord.isBlank()) return@Button
                                    viewModel.updateStep(step.id, parsedStartBar, normalizedChord, count)
                                    isEditing = false
                                }) {
                                    Text("Save")
                                }
                                Button(onClick = {
                                    startBar = barValueToInput(step.startBar)
                                    chord = step.chordName
                                    bars = barValueToInput(step.barCount)
                                    isEditing = false
                                }) {
                                    Text("Cancel")
                                }
                                Button(onClick = { viewModel.deleteStep(step.id) }) {
                                    Text("Delete Step")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(value: Long): String {
    if (value <= 0L) return "00:00.0"
    val totalSeconds = value / 1000.0
    val minutes = (totalSeconds / 60.0).toInt()
    val seconds = (totalSeconds % 60.0)
    val roundedTenths = (seconds * 10.0).roundToInt() / 10.0
    return "%02d:%04.1f".format(minutes, roundedTenths)
}

private fun formatBarValue(value: Double): String {
    if (kotlin.math.abs(value - value.roundToInt().toDouble()) < 1e-6) {
        return value.roundToInt().toString()
    }
    return "%.2f".format(value).trimEnd('0').trimEnd('.')
}

private fun barValueToInput(value: Double): String = formatBarValue(value)
