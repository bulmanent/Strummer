package com.strummer.practice.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strummer.practice.model.ChordShape
import com.strummer.practice.ui.components.ChordDiagram

@Composable
fun ChordLibraryScreen(
    chords: List<ChordShape>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    var selected by remember { mutableStateOf(chords.firstOrNull()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Chord Library", style = MaterialTheme.typography.headlineSmall)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            items(chords) { chord ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = chord }
                ) {
                    Text(
                        text = chord.name,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        selected?.let { ChordDiagram(it) }
    }
}
