package com.strummer.practice.library

import java.io.File

class AudioFileChecker {
    fun status(path: String): AudioFileStatus {
        return if (File(path).exists()) {
            AudioFileStatus.Available
        } else {
            AudioFileStatus.Missing("Audio file not found at $path. Re-import this song.")
        }
    }
}

sealed class AudioFileStatus {
    data object Available : AudioFileStatus()
    data class Missing(val message: String) : AudioFileStatus()
}
