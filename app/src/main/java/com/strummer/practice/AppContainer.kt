package com.strummer.practice

import android.content.Context
import com.strummer.practice.audio.PlaybackService
import com.strummer.practice.audio.PracticeAudioEngine
import com.strummer.practice.data.SettingsRepository
import com.strummer.practice.detection.ChordDetectionService
import com.strummer.practice.detection.ChordDraftMergeService
import com.strummer.practice.detection.ChordDetectionPostProcessor
import com.strummer.practice.detection.DetectedChordMapper
import com.strummer.practice.library.ChordTimelineService
import com.strummer.practice.repo.AssetRepository
import com.strummer.practice.repo.SongRepository

class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository(context)
    val assetRepository = AssetRepository(context)
    val audioEngine = PracticeAudioEngine()

    val songRepository = SongRepository(context)
    val playbackService = PlaybackService()
    val chordTimelineService = ChordTimelineService()
    val chordDetectionService = ChordDetectionService(postProcessor = ChordDetectionPostProcessor())
    val detectedChordMapper = DetectedChordMapper()
    val chordDraftMergeService = ChordDraftMergeService()
}
