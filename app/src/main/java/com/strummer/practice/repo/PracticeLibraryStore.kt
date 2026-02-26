package com.strummer.practice.repo

import android.util.Log
import com.strummer.practice.library.PracticeLibraryState
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File

class PracticeLibraryStore(
    private val storageFile: File,
    private val backupFile: File
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun read(): PracticeLibraryState {
        if (!storageFile.exists()) return PracticeLibraryState()

        return runCatching {
            val raw = storageFile.readText()
            json.decodeFromString<PracticeLibraryState>(raw)
        }.getOrElse { err ->
            Log.e(TAG, "Failed to parse library file: ${storageFile.absolutePath}", err)
            recoverFromCorruption(err)
        }
    }

    fun write(state: PracticeLibraryState) {
        storageFile.parentFile?.mkdirs()
        if (storageFile.exists()) {
            runCatching { storageFile.copyTo(backupFile, overwrite = true) }
                .onFailure { Log.w(TAG, "Failed to write backup file", it) }
        }

        val tmp = File(storageFile.parentFile, "${storageFile.name}.tmp")
        tmp.writeText(json.encodeToString(PracticeLibraryState.serializer(), state))
        if (!tmp.renameTo(storageFile)) {
            tmp.copyTo(storageFile, overwrite = true)
            tmp.delete()
        }
    }

    private fun recoverFromCorruption(cause: Throwable): PracticeLibraryState {
        if (!backupFile.exists()) return PracticeLibraryState()

        return runCatching {
            val raw = backupFile.readText()
            json.decodeFromString<PracticeLibraryState>(raw)
        }.onFailure {
            Log.e(TAG, "Failed to parse backup library file: ${backupFile.absolutePath}", it)
        }.getOrElse {
            if (cause is SerializationException) {
                PracticeLibraryState()
            } else {
                PracticeLibraryState()
            }
        }
    }

    private companion object {
        const val TAG = "PracticeLibraryStore"
    }
}
