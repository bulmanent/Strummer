package com.strummer.practice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strummer.practice.repo.ChordLibraryRepository
import com.strummer.practice.ui.custom.CustomPracticeScreen
import com.strummer.practice.ui.custom.CustomPracticeViewModel
import com.strummer.practice.ui.library.ChordLibraryScreen
import com.strummer.practice.ui.songs.SongsScreen
import com.strummer.practice.ui.songs.SongsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = AppContainer(applicationContext)

        setContent {
            MaterialTheme {
                StrummerApp(container)
                DisposableEffect(Unit) {
                    onDispose {
                        container.audioEngine.release()
                        container.playbackService.release()
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun StrummerApp(container: AppContainer) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val songsViewModel: SongsViewModel = viewModel(
        factory = SongsViewModel.factory(
            container.songRepository,
            container.settingsRepository,
            container.playbackService,
            container.barLoopTimelineService
        )
    )

    val customViewModel: CustomPracticeViewModel = viewModel(
        factory = CustomPracticeViewModel.factory(
            container.assetRepository,
            container.settingsRepository,
            container.audioEngine
        )
    )
    val songsState by songsViewModel.uiState.collectAsStateWithLifecycle()
    val localView = LocalView.current
    DisposableEffect(localView, songsState.isPlaying) {
        localView.keepScreenOn = songsState.isPlaying
        onDispose {
            localView.keepScreenOn = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Strummer") })
        },
        bottomBar = {
            NavigationBar {
                listOf("Songs", "Custom Practice", "Chord Library").forEachIndexed { idx, label ->
                    NavigationBarItem(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        icon = { Text(if (idx == 0) "S" else if (idx == 1) "C" else "L") },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Top
        ) {
            when (selectedTab) {
                0 -> SongsScreen(songsViewModel, Modifier.padding(horizontal = 12.dp), PaddingValues(bottom = 8.dp))
                1 -> CustomPracticeScreen(customViewModel, Modifier.padding(horizontal = 12.dp), PaddingValues(bottom = 8.dp))
                2 -> ChordLibraryScreen(ChordLibraryRepository.openChords, Modifier.padding(horizontal = 12.dp), PaddingValues(bottom = 8.dp))
            }
        }
    }
}
