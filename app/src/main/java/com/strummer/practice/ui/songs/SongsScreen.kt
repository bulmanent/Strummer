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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strummer.practice.library.ChordEvent
import kotlin.math.roundToInt

@Composable
fun SongsScreen(
    viewModel: SongsViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var songsExpanded by remember { mutableStateOf(false) }
    var addChordName by remember { mutableStateOf("") }
    var addChordNote by remember { mutableStateOf("") }
    var addTimestampInput by remember { mutableStateOf("0") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pickedUri = uri
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

        ImportCard(
            title = state.importTitleInput,
            artist = state.importArtistInput,
            selectedUri = pickedUri,
            onTitleChange = viewModel::setImportTitle,
            onArtistChange = viewModel::setImportArtist,
            onPick = { picker.launch(arrayOf("audio/mpeg", "audio/mp3", "audio/*")) },
            onImport = {
                val uri = pickedUri ?: return@ImportCard
                viewModel.importSong(uri)
                pickedUri = null
            }
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.selectedSong?.title ?: "",
                    onValueChange = {},
                    label = { Text("Selected Song") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { songsExpanded = true }, enabled = state.songs.isNotEmpty()) {
                    Text("Choose Song")
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
                    Text("Library is empty. Import an MP3 to start.")
                }
                if (state.missingFileMessage != null) {
                    Text(state.missingFileMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (state.selectedSong != null) {
            SongEditorCard(state = state, viewModel = viewModel)
            CueCard(state = state)
            PlaybackCard(state = state, viewModel = viewModel)
            SteppedModeCard(state = state, viewModel = viewModel)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Chord Timeline", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = addChordName,
                            onValueChange = { addChordName = it },
                            label = { Text("Chord") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = addChordNote,
                            onValueChange = { addChordNote = it },
                            label = { Text("Note") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            viewModel.addChordAtCurrentTime(addChordName, addChordNote)
                            addChordName = ""
                            addChordNote = ""
                        }) {
                            Text("Add @ Current")
                        }
                        OutlinedTextField(
                            value = addTimestampInput,
                            onValueChange = { addTimestampInput = it },
                            label = { Text("Timestamp ms") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            val timestamp = addTimestampInput.toLongOrNull() ?: return@Button
                            viewModel.addChordAtTimestamp(timestamp, addChordName, addChordNote)
                            addChordName = ""
                            addChordNote = ""
                        }) {
                            Text("Add")
                        }
                    }

                    if (state.chordEvents.isEmpty()) {
                        Text("No chord events yet.")
                    } else {
                        ChordEventList(events = state.chordEvents, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportCard(
    title: String,
    artist: String,
    selectedUri: Uri?,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onPick: () -> Unit,
    onImport: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Import Song", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = artist,
                onValueChange = onArtistChange,
                label = { Text("Artist (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPick) { Text("Pick MP3") }
                Button(onClick = onImport, enabled = selectedUri != null) { Text("Import") }
            }
            Text(selectedUri?.toString() ?: "No file selected")
        }
    }
}

@Composable
private fun SongEditorCard(state: SongsUiState, viewModel: SongsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Song Metadata", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.selectedSongTitleInput,
                onValueChange = viewModel::setSelectedSongTitle,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.selectedSongArtistInput,
                onValueChange = viewModel::setSelectedSongArtist,
                label = { Text("Artist") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Source path: ${state.selectedSong?.audioFilePath.orEmpty()}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::saveSongEdits) { Text("Save") }
                Button(onClick = viewModel::deleteSelectedSong) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun CueCard(state: SongsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Chord Cues", style = MaterialTheme.typography.titleMedium)
            Text(
                text = state.currentCue?.chordName ?: "-",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            val nextLabel = state.nextCue?.chordName ?: "-"
            val nextTime = state.timeToNextMs?.let { "in ${formatMs(it)}" } ?: ""
            Text("Next: $nextLabel $nextTime")
        }
    }
}

@Composable
private fun PlaybackCard(state: SongsUiState, viewModel: SongsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Playback", style = MaterialTheme.typography.titleMedium)
            Button(onClick = viewModel::playPause, enabled = state.missingFileMessage == null) {
                Text(if (state.isPlaying) "Pause" else "Play")
            }

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
            Text(state.pitchStatus)
        }
    }
}

@Composable
private fun SteppedModeCard(state: SongsUiState, viewModel: SongsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Stepped Speed Practice", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enabled")
                Switch(
                    checked = state.steppedModeEnabled,
                    onCheckedChange = viewModel::setSteppedModeEnabled
                )
                Button(onClick = viewModel::resetSteppedMode) { Text("Reset") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.steppedStartSpeed.toString(),
                    onValueChange = { it.toFloatOrNull()?.let(viewModel::setSteppedStartSpeed) },
                    label = { Text("Start") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.steppedStepSize.toString(),
                    onValueChange = { it.toFloatOrNull()?.let(viewModel::setSteppedStepSize) },
                    label = { Text("Step") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.steppedTargetSpeed.toString(),
                    onValueChange = { it.toFloatOrNull()?.let(viewModel::setSteppedTargetSpeed) },
                    label = { Text("Target") },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.steppedLoopsPerSpeed.toString(),
                    onValueChange = { it.toIntOrNull()?.let(viewModel::setSteppedLoopsPerSpeed) },
                    label = { Text("Loops/Speed") },
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text("Loop")
                    Switch(checked = state.loopEnabled, onCheckedChange = viewModel::setLoopEnabled)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.loopStartMs?.toString() ?: "",
                    onValueChange = { viewModel.setLoopStartMs(it.toLongOrNull()) },
                    label = { Text("Loop Start ms") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.loopEndMs?.toString() ?: "",
                    onValueChange = { viewModel.setLoopEndMs(it.toLongOrNull()) },
                    label = { Text("Loop End ms") },
                    modifier = Modifier.weight(1f)
                )
            }
            Text("Current: ${"%.2f".format(state.speed)}x")
            Text("Next: ${state.nextSteppedSpeed?.let { "%.2f".format(it) } ?: "-"}x")
        }
    }
}

@Composable
private fun ChordEventList(events: List<ChordEvent>, viewModel: SongsViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        events.forEach { event ->
            var timestamp by remember(event.id, event.timestampMs) { mutableStateOf(event.timestampMs.toString()) }
            var chord by remember(event.id, event.chordName) { mutableStateOf(event.chordName) }
            var note by remember(event.id, event.note) { mutableStateOf(event.note.orEmpty()) }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${formatMs(event.timestampMs)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = timestamp,
                            onValueChange = { timestamp = it },
                            label = { Text("Timestamp ms") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = chord,
                            onValueChange = { chord = it },
                            label = { Text("Chord") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val ts = timestamp.toLongOrNull() ?: return@Button
                            viewModel.updateChordEvent(event.id, ts, chord, note)
                        }) {
                            Text("Save")
                        }
                        Button(onClick = { viewModel.deleteChordEvent(event.id) }) {
                            Text("Delete")
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
