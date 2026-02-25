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
        ChordShape("G", listOf(3, 2, 0, 0, 0, 3)),

        // F-family non-barre/common variants
        ChordShape("F (barre)", listOf(1, 3, 3, 2, 1, 1)),
        ChordShape("F (mini)", listOf(-1, -1, 3, 2, 1, 1)),
        ChordShape("Fmaj7", listOf(-1, -1, 3, 2, 1, 0)),
        ChordShape("Fsus2", listOf(-1, -1, 3, 0, 1, 1)),
        ChordShape("Fadd9", listOf(-1, -1, 3, 2, 1, 3)),
        ChordShape("F/C", listOf(-1, 3, 3, 2, 1, 0)),
        ChordShape("F6", listOf(-1, -1, 3, 2, 3, 1)),

        // B-family non-barre/common variants
        ChordShape("B7", listOf(-1, 2, 1, 2, 0, 2)),
        ChordShape("Bm7", listOf(-1, 2, 0, 2, 0, 2)),
        ChordShape("B7sus2", listOf(-1, 2, 1, 2, 0, 0)),
        ChordShape("B7sus4", listOf(-1, 2, 2, 2, 0, 2)),
        ChordShape("Badd11", listOf(-1, 2, 1, 3, 0, 2)),
        ChordShape("B5", listOf(-1, 2, 4, 4, -1, -1))
    )
}
