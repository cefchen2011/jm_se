package com.comicreader.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.comicreader.data.FavoritesStore
import com.comicreader.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsStore = SettingsStore(app)
    private val dataStore = FavoritesStore(app)

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

    /** 导出全部数据为 JSON（由 UI 写文件） */
    suspend fun exportJson(): String = dataStore.exportJson()

    /** 从 JSON 导入（覆盖） */
    fun importJson(json: String) {
        viewModelScope.launch { dataStore.importJson(json) }
    }
}
