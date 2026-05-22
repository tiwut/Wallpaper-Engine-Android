package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.WallpaperRepository
import com.example.ui.screens.MainScreen
import com.example.ui.theme.WallpaperEngineTheme
import com.example.viewmodel.WallpaperViewModel
import com.example.viewmodel.WallpaperViewModelFactory

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
