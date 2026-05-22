package org.tiwut.wallpaperengine.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {
    @Query("SELECT * FROM wallpaper_images ORDER BY dateAdded DESC")
    fun getAllImages(): Flow<List<WallpaperImage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: WallpaperImage): Long

    @Query("DELETE FROM wallpaper_images WHERE id = :id")
    suspend fun deleteImageById(id: Int)

    @Query("SELECT * FROM wallpaper_rules")
    fun getAllRules(): Flow<List<WallpaperRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: WallpaperRule)

    @androidx.room.Update
    suspend fun updateRule(rule: WallpaperRule)

    @Query("DELETE FROM wallpaper_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Int)

    @Query("UPDATE wallpaper_rules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateRuleStatus(id: Int, isEnabled: Boolean)
    
    @Query("SELECT * FROM wallpaper_images WHERE id = :id LIMIT 1")
    suspend fun getImageById(id: Int): WallpaperImage?

    @Query("SELECT * FROM active_wallpaper WHERE id = 1")
    fun getActiveWallpaper(): Flow<ActiveWallpaper?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setActiveWallpaper(active: ActiveWallpaper)
}
