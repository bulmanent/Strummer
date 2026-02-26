package com.strummer.practice

import com.strummer.practice.library.AudioFileChecker
import com.strummer.practice.library.AudioFileStatus
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AudioFileCheckerTest {
    private val checker = AudioFileChecker()

    @Test
    fun reportsMissingFile() {
        val status = checker.status("/tmp/does-not-exist-strummer-audio.mp3")
        assertTrue(status is AudioFileStatus.Missing)
    }

    @Test
    fun reportsAvailableFile() {
        val temp = File.createTempFile("strummer-audio", ".mp3")
        temp.deleteOnExit()

        val status = checker.status(temp.absolutePath)
        assertTrue(status is AudioFileStatus.Available)
    }
}
