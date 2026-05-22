package org.tiwut.wallpaperengine.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import org.tiwut.wallpaperengine.data.WallpaperImage
import org.tiwut.wallpaperengine.data.WallpaperRepository
import org.tiwut.wallpaperengine.data.WallpaperRule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class WallpaperViewModel(private val repository: WallpaperRepository) : ViewModel() {
    val images: StateFlow<List<WallpaperImage>> = repository.allImages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rules: StateFlow<List<WallpaperRule>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val fileName = "wallpaper_${UUID.randomUUID()}.jpg"
                val file = File(context.filesDir, fileName)
                
                inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                val newImage = WallpaperImage(localUri = file.absolutePath)
                repository.insertImage(newImage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteImage(id: Int, localPath: String) {
         viewModelScope.launch {
             try {
                val file = File(localPath)
                if (file.exists()) file.delete()
             } catch (e: Exception) { e.printStackTrace() }
             
             repository.deleteImage(id)
         }
    }

    fun addRule(rule: WallpaperRule) {
        viewModelScope.launch {
            repository.insertRule(rule)
        }
    }

    fun deleteRule(id: Int) {
        viewModelScope.launch {
            repository.deleteRule(id)
        }
    }

    fun toggleRule(id: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateRuleStatus(id, isEnabled)
        }
    }
}

class WallpaperViewModelFactory(private val repository: WallpaperRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WallpaperViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WallpaperViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
