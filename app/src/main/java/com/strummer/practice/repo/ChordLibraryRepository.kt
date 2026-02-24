package com.strummer.practice.repo

import com.strummer.practice.model.ChordShape

object ChordLibraryRepository {
    val openChords: List<ChordShape> = listOf(
        ChordShape("A", listOf(-1, 0, 2, 2, 2, 0)),
        ChordShape("Am", listOf(-1, 0, 2, 2, 1, 0)),
        ChordShape("C", listOf(-1, 3, 2, 0, 1, 0)),
        ChordShape("D", listOf(-1, -1, 0, 2, 3, 2)),
        ChordShape("Dm", listOf(-1, -1, 0, 2, 3, 1)),
        ChordShape("E", listOf(0, 2, 2, 1, 0, 0)),
        ChordShape("Em", listOf(0, 2, 2, 0, 0, 0)),
        ChordShape("F", listOf(1, 3, 3, 2, 1, 1)),
        ChordShape("G", listOf(3, 2, 0, 0, 0, 3))
    )
}
