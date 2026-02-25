package com.strummer.practice.ui.songs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strummer.practice.ui.components.PatternStepsRow
import com.strummer.practice.ui.components.UpcomingBarsRow

@Composable
fun SongsScreen(viewModel: SongsViewModel, modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Songs", style = MaterialTheme.typography.headlineSmall)

        SongPickers(
            state = state,
            onSongSelected = viewModel::selectSong,
            onSectionSelected = viewModel::selectSection
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Current Chord", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = state.currentChord.ifBlank { state.selectedSection?.bars?.firstOrNull()?.chord.orEmpty() },
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                UpcomingBarsRow(
                    chords = state.selectedSection?.bars?.map { it.chord }.orEmpty(),
                    currentBar = state.currentBarIndex
                )
            }
        }

        PatternStepsRow(
            pattern = state.selectedSection
                ?.bars
                ?.getOrNull(state.currentBarIndex)
                ?.let { bar -> state.patterns.firstOrNull { it.id == bar.patternId } },
            currentStepIndex = state.currentStepIndex
        )

        TempoControls(
            bpm = state.tempoBpm,
            effectiveBpm = state.effectiveBpm,
            onBpmChange = viewModel::setTempo,
            onDelta = viewModel::adjustTempo
        )

        RampPanel(state = state, viewModel = viewModel)

    }
}

@Composable
private fun SongPickers(
    state: SongsUiState,
    onSongSelected: (Int) -> Unit,
    onSectionSelected: (Int) -> Unit
) {
    var songExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.selectedSong?.title ?: "",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Song") }
        )
        TextButton(onClick = { songExpanded = true }) { Text("Choose Song") }
        androidx.compose.material3.DropdownMenu(expanded = songExpanded, onDismissRequest = { songExpanded = false }) {
            state.songs.forEachIndexed { idx, song ->
                DropdownMenuItem(
                    text = { Text(song.title) },
                    onClick = {
                        onSongSelected(idx)
                        songExpanded = false
                    }
                )
            }
        }

        OutlinedTextField(
            value = state.selectedSection?.name ?: "",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Section") }
        )
        TextButton(onClick = { sectionExpanded = true }) { Text("Choose Section") }
        androidx.compose.material3.DropdownMenu(expanded = sectionExpanded, onDismissRequest = { sectionExpanded = false }) {
            state.selectedSong?.sections?.forEachIndexed { idx, section ->
                DropdownMenuItem(
                    text = { Text(section.name) },
                    onClick = {
                        onSectionSelected(idx)
                        sectionExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TempoControls(
    bpm: Int,
    effectiveBpm: Int,
    onBpmChange: (Int) -> Unit,
    onDelta: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tempo", style = MaterialTheme.typography.titleMedium)
            Text("Target: $bpm BPM   Current: $effectiveBpm BPM")
            Slider(
                value = bpm.toFloat(),
                onValueChange = { onBpmChange(it.toInt()) },
                valueRange = 40f..160f
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(-5, -1, +1, +5).forEach { delta ->
                    FilterChip(
                        selected = false,
                        onClick = { onDelta(delta) },
                        label = { Text(if (delta > 0) "+$delta" else "$delta") }
                    )
                }
            }
        }
    }
}

@Composable
private fun RampPanel(state: SongsUiState, viewModel: SongsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Practice Ramp", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.rampEnabled,
                    onClick = { viewModel.setRampEnabled(!state.rampEnabled) },
                    label = { Text(if (state.rampEnabled) "Enabled" else "Disabled") }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.rampStartBpm.toString(),
                    onValueChange = { it.toIntOrNull()?.let(viewModel::setRampStart) },
                    label = { Text("Start") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.rampEndBpm.toString(),
                    onValueChange = { it.toIntOrNull()?.let(viewModel::setRampEnd) },
                    label = { Text("End") },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.rampIncrement.toString(),
                    onValueChange = { it.toIntOrNull()?.let(viewModel::setRampIncrement) },
                    label = { Text("Increment") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.barsPerIncrement.toString(),
                    onValueChange = { it.toIntOrNull()?.let(viewModel::setBarsPerIncrement) },
                    label = { Text("Bars/Step") },
                    modifier = Modifier.weight(1f)
                )
            }
            Text("Ramp status: ${if (state.rampEnabled) "running" else "off"}")
            if (state.rampEnabled) {
                Text("Bars until next increment: ${state.rampBarsUntilIncrement}")
            }
        }
    }
}
