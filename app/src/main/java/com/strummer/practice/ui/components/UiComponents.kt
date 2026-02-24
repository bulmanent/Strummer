package com.strummer.practice.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strummer.practice.model.ChordShape
import com.strummer.practice.model.StrumPattern

@Composable
fun PatternStepsRow(pattern: StrumPattern?, currentStepIndex: Int, modifier: Modifier = Modifier) {
    if (pattern == null) return

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(pattern.steps) { index, step ->
            val highlighted = index == currentStepIndex
            Box(
                modifier = Modifier
                    .background(
                        color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = step.kindRaw,
                    color = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun UpcomingBarsRow(chords: List<String>, currentBar: Int, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(chords) { idx, chord ->
            val active = idx == currentBar
            Box(
                modifier = Modifier
                    .background(
                        color = if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = chord,
                    color = if (active) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChordDiagram(shape: ChordShape, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = shape.name, style = MaterialTheme.typography.titleLarge)
            Canvas(modifier = Modifier.size(200.dp)) {
                val strings = 6
                val frets = 5
                val left = size.width * 0.1f
                val right = size.width * 0.9f
                val top = size.height * 0.1f
                val bottom = size.height * 0.9f
                val stringGap = (right - left) / (strings - 1)
                val fretGap = (bottom - top) / frets

                for (s in 0 until strings) {
                    val x = left + s * stringGap
                    drawLine(
                        color = Color.Black,
                        start = Offset(x, top),
                        end = Offset(x, bottom),
                        strokeWidth = 2f
                    )
                }

                for (f in 0..frets) {
                    val y = top + f * fretGap
                    drawLine(
                        color = Color.Black,
                        start = Offset(left, y),
                        end = Offset(right, y),
                        strokeWidth = if (f == 0) 4f else 2f
                    )
                }

                shape.frets.forEachIndexed { stringIndex, fret ->
                    val x = left + stringIndex * stringGap
                    when {
                        fret < 0 -> {
                            drawLine(
                                color = Color.Red,
                                start = Offset(x - 8f, top - 20f),
                                end = Offset(x + 8f, top - 4f),
                                strokeWidth = 3f
                            )
                            drawLine(
                                color = Color.Red,
                                start = Offset(x + 8f, top - 20f),
                                end = Offset(x - 8f, top - 4f),
                                strokeWidth = 3f
                            )
                        }

                        fret == 0 -> {
                            drawCircle(
                                color = Color.Black,
                                radius = 7f,
                                center = Offset(x, top - 12f),
                                style = Stroke(width = 2f)
                            )
                        }

                        else -> {
                            val y = top + (fret - 0.5f) * fretGap
                            drawCircle(
                                color = Color.Black,
                                radius = 10f,
                                center = Offset(x, y)
                            )
                        }
                    }
                }
            }
        }
    }
}
