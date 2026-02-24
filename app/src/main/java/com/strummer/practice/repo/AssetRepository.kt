package com.strummer.practice.repo

import android.content.Context
import com.strummer.practice.model.PatternsAsset
import com.strummer.practice.model.Song
import com.strummer.practice.model.SongsAsset
import com.strummer.practice.model.StrumPattern
import kotlinx.serialization.json.Json

class AssetRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadSongs(): List<Song> {
        val raw = context.assets.open("songs.json").bufferedReader().use { it.readText() }
        return json.decodeFromString<SongsAsset>(raw).songs
    }

    suspend fun loadPatterns(): List<StrumPattern> {
        val raw = context.assets.open("patterns.json").bufferedReader().use { it.readText() }
        return json.decodeFromString<PatternsAsset>(raw).patterns
    }
}
