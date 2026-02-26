package com.strummer.practice.detection

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChordDetectionServiceFailureTest {
    @Test
    fun failsWhenAudioFileMissing() {
        val service = ChordDetectionService(rawDetector = FakeDetector(emptyList()))
        val outcome = runBlockingDetect(service, "/tmp/strummer-no-file.mp3")
        assertTrue(outcome is ChordDetectionOutcome.Failure)
    }

    @Test
    fun failsOnDetectorException() {
        val temp = File.createTempFile("strummer-detect", ".mp3")
        temp.writeBytes(ByteArray(4096) { 1 })
        val service = ChordDetectionService(rawDetector = ThrowingDetector())

        val outcome = runBlockingDetect(service, temp.absolutePath)

        assertTrue(outcome is ChordDetectionOutcome.Failure)
    }

    @Test
    fun failsOnEmptyDetectorOutput() {
        val temp = File.createTempFile("strummer-detect-empty", ".mp3")
        temp.writeBytes(ByteArray(4096) { 1 })
        val service = ChordDetectionService(rawDetector = FakeDetector(emptyList()))

        val outcome = runBlockingDetect(service, temp.absolutePath)

        assertTrue(outcome is ChordDetectionOutcome.Failure)
    }

    private fun runBlockingDetect(service: ChordDetectionService, path: String): ChordDetectionOutcome {
        val result = kotlinx.coroutines.runBlocking {
            service.detect(path)
        }
        return result
    }

    private class FakeDetector(private val events: List<DetectedChordEvent>) : RawChordDetector {
        override val detectorVersion: String = "fake-v1"

        override suspend fun analyze(
            audioFilePath: String,
            params: ChordDetectionParams,
            onProgress: (Float) -> Unit
        ): List<DetectedChordEvent> = events
    }

    private class ThrowingDetector : RawChordDetector {
        override val detectorVersion: String = "throw-v1"

        override suspend fun analyze(
            audioFilePath: String,
            params: ChordDetectionParams,
            onProgress: (Float) -> Unit
        ): List<DetectedChordEvent> {
            throw IllegalStateException("detector boom")
        }
    }
}
