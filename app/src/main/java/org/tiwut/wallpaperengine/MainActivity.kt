package org.tiwut.wallpaperengine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import org.tiwut.wallpaperengine.data.AppDatabase
import org.tiwut.wallpaperengine.data.WallpaperRepository
import org.tiwut.wallpaperengine.ui.screens.MainScreen
import org.tiwut.wallpaperengine.ui.theme.WallpaperEngineTheme
import org.tiwut.wallpaperengine.viewmodel.WallpaperViewModel
import org.tiwut.wallpaperengine.viewmodel.WallpaperViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val database = AppDatabase.getDatabase(this)
    val repository = WallpaperRepository(database.wallpaperDao())
    val factory = WallpaperViewModelFactory(repository)
    val viewModel = ViewModelProvider(this, factory)[WallpaperViewModel::class.java]

    setContent {
      WallpaperEngineTheme(darkTheme = true) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainScreen(viewModel)
        }
      }
    }
  }
}
