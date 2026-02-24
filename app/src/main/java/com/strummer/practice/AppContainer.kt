package com.strummer.practice

import android.content.Context
import com.strummer.practice.audio.PracticeAudioEngine
import com.strummer.practice.data.SettingsRepository
import com.strummer.practice.repo.AssetRepository

class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(context)
    val assetRepository = AssetRepository(context)
    val audioEngine = PracticeAudioEngine()
}
