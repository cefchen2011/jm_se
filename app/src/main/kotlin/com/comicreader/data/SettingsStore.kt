package com.comicreader.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings_store")

/** 应用设置：主题色、界面风格、图片 CDN 等偏好 */
class SettingsStore(context: Context) {

    private val appContext = context.applicationContext

    private val keyThemeColor = stringPreferencesKey("theme_color")
    private val keyUiStyle = stringPreferencesKey("ui_style")
    private val keyImgCdn = intPreferencesKey("img_cdn_index")

    /** 主题色 key："system" 或具体色名 */
    fun themeColorFlow(): Flow<String> =
        appContext.settingsDataStore.data.map { p -> p[keyThemeColor] ?: "system" }

    suspend fun setThemeColor(color: String) {
        appContext.settingsDataStore.edit { p -> p[keyThemeColor] = color }
    }

    /** 界面风格 key："standard" / "miui" */
    fun uiStyleFlow(): Flow<String> =
        appContext.settingsDataStore.data.map { p -> p[keyUiStyle] ?: "standard" }

    suspend fun setUiStyle(style: String) {
        appContext.settingsDataStore.edit { p -> p[keyUiStyle] = style }
    }

    /** 图片 CDN 下标（对应 [ImgCdnConfig.domains]） */
    fun imgCdnIndexFlow(): Flow<Int> =
        appContext.settingsDataStore.data.map { p -> p[keyImgCdn] ?: 0 }

    suspend fun setImgCdnIndex(index: Int) {
        appContext.settingsDataStore.edit { p -> p[keyImgCdn] = index.coerceIn(0, ImgCdnConfig.domains.lastIndex) }
    }
}
