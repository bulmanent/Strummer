package com.strummer.practice.repo

import com.strummer.practice.model.ChordShape

object ChordLibraryRepository {
    val openChords: List<ChordShape> = listOf(
        // F-family (non-barre first)
        ChordShape("F (mini)", listOf(-1, -1, 3, 2, 1, 1)),
        ChordShape("Fmaj7", listOf(-1, -1, 3, 2, 1, 0)),
        ChordShape("Fsus2", listOf(-1, -1, 3, 0, 1, 1)),
        ChordShape("Fadd9", listOf(-1, -1, 3, 2, 1, 3)),
        ChordShape("F/C", listOf(-1, 3, 3, 2, 1, 0)),
        ChordShape("F6", listOf(-1, -1, 3, 2, 3, 1)),
        ChordShape("F (barre)", listOf(1, 3, 3, 2, 1, 1)),

        // B-family (non-barre/common shapes)
        ChordShape("B7", listOf(-1, 2, 1, 2, 0, 2)),
        ChordShape("Bm7", listOf(-1, 2, 0, 2, 0, 2)),
        ChordShape("B7sus2", listOf(-1, 2, 1, 2, 0, 0)),
        ChordShape("B7sus4", listOf(-1, 2, 2, 2, 0, 2)),
        ChordShape("Badd11", listOf(-1, 2, 1, 3, 0, 2)),
        ChordShape("B5", listOf(-1, 2, 4, 4, -1, -1)),

        // Additional non-typical non-barre variations (requested)
        ChordShape("A6", listOf(-1, 0, 2, 2, 2, 2)),
        ChordShape("Asus2", listOf(-1, 0, 2, 2, 0, 0)),
        ChordShape("A7sus4", listOf(-1, 0, 2, 0, 3, 0)),

        ChordShape("Cadd9", listOf(-1, 3, 2, 0, 3, 0)),
        ChordShape("Cmaj7", listOf(-1, 3, 2, 0, 0, 0)),
        ChordShape("Csus2", listOf(-1, 3, 0, 0, 1, 3)),

        ChordShape("Dsus2", listOf(-1, -1, 0, 2, 3, 0)),
        ChordShape("Dadd9", listOf(-1, -1, 0, 2, 3, 0)),
        ChordShape("D6", listOf(-1, -1, 0, 2, 0, 2)),

        ChordShape("E6", listOf(0, 2, 2, 1, 2, 0)),
        ChordShape("E7sus4", listOf(0, 2, 2, 2, 0, 0)),
        ChordShape("Esus2", listOf(0, 2, 4, 4, 0, 0)),

        ChordShape("G6", listOf(3, 2, 0, 0, 0, 0)),
        ChordShape("Gadd9", listOf(3, 0, 0, 2, 0, 3)),
        ChordShape("Gsus2", listOf(3, 0, 0, 0, 3, 3))
    )
}
