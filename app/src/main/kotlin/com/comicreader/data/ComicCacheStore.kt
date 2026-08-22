package com.comicreader.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.comicreader.data.model.ComicCacheInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.cacheDataStore by preferencesDataStore(name = "cache_store")

/**
 * 漫画离线缓存：
 * - 元数据存 DataStore（JSON）
 * - 图片文件存 filesDir/comic_cache/{comicId}/{chapterId}/{page}.ext
 * - 封面存 filesDir/comic_cache/{comicId}/cover.jpg
 */
class ComicCacheStore(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val root = File(appContext.filesDir, "comic_cache")

    private val keyCacheList = stringPreferencesKey("cache_list")
    private val cacheType = object : TypeToken<List<ComicCacheInfo>>() {}.type

    fun cachedComicsFlow(): Flow<List<ComicCacheInfo>> =
        appContext.cacheDataStore.data.map { p ->
            p[keyCacheList]?.let { runCatching { gson.fromJson<List<ComicCacheInfo>>(it, cacheType) }.getOrNull() } ?: emptyList()
        }

    fun cacheDirOf(comicId: String): File = File(root, comicId)

    fun chapterDirOf(comicId: String, chapterId: String): File =
        File(cacheDirOf(comicId), chapterId)

    fun coverFileOf(comicId: String): File =
        File(cacheDirOf(comicId), "cover.jpg")

    fun isChapterCached(comicId: String, chapterId: String): Boolean {
        val dir = chapterDirOf(comicId, chapterId)
        return dir.isDirectory && (dir.listFiles()?.isNotEmpty() == true)
    }

    /** 章节内页面文件（按页码排序） */
    fun chapterImages(comicId: String, chapterId: String): List<File> =
        chapterDirOf(comicId, chapterId)
            .listFiles { f -> f.isFile }
            ?.sortedBy { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }
            ?: emptyList()

    fun exists(comicId: String): Boolean = cacheDirOf(comicId).isDirectory

    suspend fun upsertCache(info: ComicCacheInfo) {
        appContext.cacheDataStore.edit { p ->
            val cur = p[keyCacheList]?.let { runCatching { gson.fromJson<List<ComicCacheInfo>>(it, cacheType) }.getOrNull() } ?: emptyList()
            val next = listOf(info) + cur.filterNot { it.comicId == info.comicId }
            p[keyCacheList] = gson.toJson(next)
        }
    }

    suspend fun removeCache(comicId: String) {
        cacheDirOf(comicId).deleteRecursively()
        appContext.cacheDataStore.edit { p ->
            val cur = p[keyCacheList]?.let { runCatching { gson.fromJson<List<ComicCacheInfo>>(it, cacheType) }.getOrNull() } ?: emptyList()
            p[keyCacheList] = gson.toJson(cur.filterNot { it.comicId == comicId })
        }
    }

    fun totalSize(): Long = if (root.isDirectory) root.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L

    suspend fun clearAll() {
        root.deleteRecursively()
        appContext.cacheDataStore.edit { p ->
            p[keyCacheList] = "[]"
        }
    }
}
