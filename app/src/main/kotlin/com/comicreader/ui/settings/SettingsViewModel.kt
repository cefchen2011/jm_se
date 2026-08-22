package com.comicreader.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.comicreader.data.ComicCacheStore
import com.comicreader.data.FavoritesStore
import com.comicreader.data.SettingsStore
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsStore = SettingsStore(app)
    private val dataStore = FavoritesStore(app)
    private val cacheStore = ComicCacheStore(app)

    val themeColor: StateFlow<String> = settingsStore.themeColorFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val uiStyle: StateFlow<String> = settingsStore.uiStyleFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "standard")

    val imgCdnIndex: StateFlow<Int> = settingsStore.imgCdnIndexFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setThemeColor(color: String) {
        viewModelScope.launch { settingsStore.setThemeColor(color) }
    }

    fun setUiStyle(style: String) {
        viewModelScope.launch { settingsStore.setUiStyle(style) }
    }

    fun setImgCdnIndex(index: Int) {
        viewModelScope.launch { settingsStore.setImgCdnIndex(index) }
    }

    /** 导出全部数据为 JSON（由 UI 写文件），含缓存清单 */
    suspend fun exportJson(): String {
        val json = dataStore.exportJson()
        val obj = try {
            com.google.gson.JsonParser.parseString(json) as? JsonObject ?: JsonObject()
        } catch (e: Exception) { JsonObject() }
        // 附加缓存清单
        val cached = cacheStore.cachedComicsFlow().first()
        obj.add("cache", Gson().toJsonTree(cached))
        return Gson().toJson(obj)
    }

    /** 从 JSON 导入（覆盖） */
    fun importJson(json: String) {
        viewModelScope.launch { dataStore.importJson(json) }
    }

    /** 清除全部数据 */
    fun clearAllData() {
        viewModelScope.launch { dataStore.clearAll() }
    }

    fun cacheSize(): Long = cacheStore.totalSize()

    fun clearCache() {
        viewModelScope.launch { cacheStore.clearAll() }
    }

    suspend fun exportCacheZip(outFile: File) = withContext(Dispatchers.IO) {
        val cached = cacheStore.cachedComicsFlow().first()
        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            for (info in cached) {
                val comicDir = cacheStore.cacheDirOf(info.comicId)
                if (!comicDir.isDirectory) continue
                val prefix = "${info.comicName}"
                comicDir.walkTopDown().filter { it.isFile }.forEach { f ->
                    val relative = f.relativeTo(comicDir).path
                    zip.putNextEntry(ZipEntry("$prefix/$relative"))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }
}
