package org.tiwut.wallpaperengine.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallpaper_images")
data class WallpaperImage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val localUri: String,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallpaper_rules")
data class WallpaperRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val triggerType: TriggerType,
    val triggerValue: String,
    val imageIds: String,
    val transitionAnimation: String = "FADE",
    val isEnabled: Boolean = true,
    val currentImageIndex: Int = 0
)

@Entity(tableName = "active_wallpaper")
data class ActiveWallpaper(
    @PrimaryKey val id: Int = 1,
    val localUri: String,
    val transitionAnimation: String
)

enum class TriggerType {
    TIME,
    SHAKE,
    UNLOCK,
    LIGHT
}
