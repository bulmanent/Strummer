package com.strummer.practice

import android.content.Context
import com.strummer.practice.audio.PlaybackService
import com.strummer.practice.audio.PracticeAudioEngine
import com.strummer.practice.data.SettingsRepository
import com.strummer.practice.library.BarLoopTimelineService
import com.strummer.practice.repo.AssetRepository
import com.strummer.practice.repo.SongRepository

class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(context)
    val assetRepository = AssetRepository(context)
    val audioEngine = PracticeAudioEngine()

    val songRepository = SongRepository(context)
    val playbackService = PlaybackService()
    val barLoopTimelineService = BarLoopTimelineService()
}
