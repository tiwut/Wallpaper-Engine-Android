package org.tiwut.wallpaperengine.data

import kotlinx.coroutines.flow.Flow

class WallpaperRepository(private val dao: WallpaperDao) {
    val allImages: Flow<List<WallpaperImage>> = dao.getAllImages()
    val allRules: Flow<List<WallpaperRule>> = dao.getAllRules()

    suspend fun insertImage(image: WallpaperImage): Long = dao.insertImage(image)
    suspend fun deleteImage(id: Int) = dao.deleteImageById(id)

    suspend fun insertRule(rule: WallpaperRule) = dao.insertRule(rule)
    suspend fun updateRule(rule: WallpaperRule) = dao.updateRule(rule)
    suspend fun deleteRule(id: Int) = dao.deleteRuleById(id)
    suspend fun updateRuleStatus(id: Int, isEnabled: Boolean) = dao.updateRuleStatus(id, isEnabled)
    
    suspend fun getImageById(id: Int) = dao.getImageById(id)

    val activeWallpaper: Flow<ActiveWallpaper?> = dao.getActiveWallpaper()
    suspend fun setActiveWallpaper(active: ActiveWallpaper) = dao.setActiveWallpaper(active)
}
