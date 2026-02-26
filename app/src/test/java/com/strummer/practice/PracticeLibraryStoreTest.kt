package com.strummer.practice

import com.strummer.practice.library.ChordEvent
import com.strummer.practice.library.PracticeLibraryState
import com.strummer.practice.library.PracticeProfile
import com.strummer.practice.library.Song
import com.strummer.practice.repo.PracticeLibraryStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class PracticeLibraryStoreTest {
    @Test
    fun roundTripsSongsAndChordEvents() {
        val dir = createTempDirectory(prefix = "strummer-store-").toFile()
        val store = PracticeLibraryStore(
            storageFile = File(dir, "library_state.json"),
            backupFile = File(dir, "library_state.json.bak")
        )

        val state = PracticeLibraryState(
            songs = listOf(
                Song(
                    id = "song-1",
                    title = "Test Song",
                    artist = "Artist",
                    audioFilePath = "/tmp/test.mp3",
                    durationMs = 12_345L,
                    createdAt = 1L,
                    updatedAt = 2L
                )
            ),
            chordEvents = listOf(
                ChordEvent(id = "evt-1", songId = "song-1", timestampMs = 1000L, chordName = "G")
            ),
            practiceProfiles = listOf(
                PracticeProfile(songId = "song-1", startSpeed = 0.6f, stepSize = 0.05f, targetSpeed = 1.0f)
            )
        )

        store.write(state)
        val restored = store.read()

        assertEquals(1, restored.songs.size)
        assertEquals("Test Song", restored.songs.first().title)
        assertEquals(1, restored.chordEvents.size)
        assertEquals("G", restored.chordEvents.first().chordName)
        assertEquals(1, restored.practiceProfiles.size)
        assertTrue(File(dir, "library_state.json").exists())
    }
}
