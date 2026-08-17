package com.comicreader.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.comicreader.data.model.Comic
import com.comicreader.data.model.HistoryEntry
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "reader_store")

/** 本地收藏 + 阅读历史 + 屏蔽列表（DataStore + JSON） */
class FavoritesStore(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()

    private val keyFavorites = stringPreferencesKey("favorites")
    private val keyHistory = stringPreferencesKey("history")
    private val keyBlocked = stringPreferencesKey("blocked")

    private val comicType = object : TypeToken<List<Comic>>() {}.type
    private val historyType = object : TypeToken<List<HistoryEntry>>() {}.type

    fun favoritesFlow(): Flow<List<Comic>> =
        appContext.dataStore.data.map { p ->
            p[keyFavorites]?.let { runCatching { gson.fromJson<List<Comic>>(it, comicType) }.getOrNull() } ?: emptyList()
        }

    fun historyFlow(): Flow<List<HistoryEntry>> =
        appContext.dataStore.data.map { p ->
            p[keyHistory]?.let { runCatching { gson.fromJson<List<HistoryEntry>>(it, historyType) }.getOrNull() } ?: emptyList()
        }

    fun blockedFlow(): Flow<List<Comic>> =
        appContext.dataStore.data.map { p ->
            p[keyBlocked]?.let { runCatching { gson.fromJson<List<Comic>>(it, comicType) }.getOrNull() } ?: emptyList()
        }

    suspend fun addFavorite(comic: Comic) {
        appContext.dataStore.edit { p ->
            val cur = p[keyFavorites]?.let { runCatching { gson.fromJson<List<Comic>>(it, comicType) }.getOrNull() } ?: emptyList()
            if (cur.none { it.id == comic.id }) {
                p[keyFavorites] = gson.toJson(listOf(comic) + cur)
            }
        }
    }

    suspend fun removeFavorite(id: String) {
        appContext.dataStore.edit { p ->
            val cur = p[keyFavorites]?.let { runCatching { gson.fromJson<List<Comic>>(it, comicType) }.getOrNull() } ?: emptyList()
            p[keyFavorites] = gson.toJson(cur.filterNot { it.id == id })
        }
    }

    suspend fun recordProgress(entry: HistoryEntry) {
        appContext.dataStore.edit { p ->
            val cur = p[keyHistory]?.let { runCatching { gson.fromJson<List<HistoryEntry>>(it, historyType) }.getOrNull() } ?: emptyList()
            val next = listOf(entry) + cur.filterNot { it.comicId == entry.comicId }
            p[keyHistory] = gson.toJson(next.take(200))
        }
    }

    suspend fun clearHistory() {
        appContext.dataStore.edit { p -> p[keyHistory] = "[]" }
    }

    /** 删除单条阅读记录（按漫画 id） */
    suspend fun removeHistoryEntry(comicId: String) {
        appContext.dataStore.edit { p ->
            val cur = p[keyHistory]?.let { runCatching { gson.fromJson<List<HistoryEntry>>(it, historyType) }.getOrNull() } ?: emptyList()
            p[keyHistory] = gson.toJson(cur.filterNot { it.comicId == comicId })
        }
    }

    suspend fun addBlocked(comic: Comic) {
        appContext.dataStore.edit { p ->
            val cur = p[keyBlocked]?.let { runCatching { gson.fromJson<List<Comic>>(it, comicType) }.getOrNull() } ?: emptyList()
            if (cur.none { it.id == comic.id }) {
                p[keyBlocked] = gson.toJson(listOf(comic) + cur)
            }
        }
    }

    suspend fun removeBlocked(id: String) {
        appContext.dataStore.edit { p ->
            val cur = p[keyBlocked]?.let { runCatching { gson.fromJson<List<Comic>>(it, comicType) }.getOrNull() } ?: emptyList()
            p[keyBlocked] = gson.toJson(cur.filterNot { it.id == id })
        }
    }

    // ---------- 数据导出 / 导入 ----------

    /** 导出全部数据（收藏 + 历史 + 屏蔽）为 JSON 字符串 */
    suspend fun exportJson(): String {
        val favs = favoritesFlow().first()
        val hist = historyFlow().first()
        val blocked = blockedFlow().first()
        val obj = JsonObject()
        obj.addProperty("version", 1)
        obj.add("favorites", gson.toJsonTree(favs))
        obj.add("history", gson.toJsonTree(hist))
        obj.add("blocked", gson.toJsonTree(blocked))
        return gson.toJson(obj)
    }

    /** 从 JSON 字符串导入（覆盖当前数据） */
    suspend fun importJson(json: String) {
        val obj = try {
            JsonParser.parseString(json) as? JsonObject ?: return
        } catch (e: Exception) {
            return
        }
        val favs = obj["favorites"]?.let { runCatching { gson.fromJson<List<Comic>>(it, comicType) }.getOrNull() } ?: emptyList()
        val hist = obj["history"]?.let { runCatching { gson.fromJson<List<HistoryEntry>>(it, historyType) }.getOrNull() } ?: emptyList()
        val blocked = obj["blocked"]?.let { runCatching { gson.fromJson<List<Comic>>(it, comicType) }.getOrNull() } ?: emptyList()
        appContext.dataStore.edit { p ->
            p[keyFavorites] = gson.toJson(favs)
            p[keyHistory] = gson.toJson(hist)
            p[keyBlocked] = gson.toJson(blocked)
        }
    }
}
