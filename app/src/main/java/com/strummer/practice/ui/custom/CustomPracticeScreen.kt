package com.strummer.practice.ui.custom

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
import com.strummer.practice.util.PatternDslParser

@Composable
fun CustomPracticeScreen(
    viewModel: CustomPracticeViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var patternsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Custom Practice", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.chordSequenceInput,
                    onValueChange = viewModel::setChordSequence,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Chord sequence (comma/space separated)") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 4).forEach { beat ->
                        FilterChip(
                            selected = state.beatsPerBar == beat,
                            onClick = { viewModel.setBeatsPerBar(beat) },
                            label = { Text("$beat/4") }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.useDslPattern,
                        onClick = { viewModel.setPatternMode(true) },
                        label = { Text("DSL Pattern") }
                    )
                    FilterChip(
                        selected = !state.useDslPattern,
                        onClick = { viewModel.setPatternMode(false) },
                        label = { Text("Preset Pattern") }
                    )
                }

                if (state.useDslPattern) {
                    OutlinedTextField(
                        value = state.patternDsl,
                        onValueChange = viewModel::setPatternDsl,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Pattern DSL") }
                    )
                } else {
                    OutlinedTextField(
                        value = state.availablePatterns.firstOrNull { it.id == state.selectedPatternId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Pattern") }
                    )
                    TextButton(onClick = { patternsExpanded = true }) { Text("Choose pattern") }
                    androidx.compose.material3.DropdownMenu(
                        expanded = patternsExpanded,
                        onDismissRequest = { patternsExpanded = false }
                    ) {
                        state.availablePatterns.forEach { pattern ->
                            DropdownMenuItem(
                                text = { Text(pattern.name) },
                                onClick = {
                                    viewModel.setPatternId(pattern.id)
                                    patternsExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = state.currentChord.ifBlank { "-" },
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )
        if (state.errorMessage.isNotBlank()) {
            Text(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        PatternStepsRow(
            pattern = if (state.useDslPattern) {
                runCatching {
                    PatternDslParser.parse(
                        id = "preview",
                        name = "Preview",
                        subdivision = 8,
                        dsl = state.patternDsl
                    )
                }.getOrNull()
            } else {
                state.availablePatterns.firstOrNull { it.id == state.selectedPatternId }
            },
            currentStepIndex = state.currentStepIndex
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tempo ${state.tempoBpm} BPM")
                Slider(
                    value = state.tempoBpm.toFloat(),
                    onValueChange = { viewModel.setTempo(it.toInt()) },
                    valueRange = 40f..160f
                )
            }
        }
    }
}
