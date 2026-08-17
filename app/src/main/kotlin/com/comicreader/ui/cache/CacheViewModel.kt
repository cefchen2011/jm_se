package com.comicreader.ui.cache

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.comicreader.data.ComicCacheStore
import com.comicreader.data.ComicDownloader
import com.comicreader.data.DownloadProgress
import com.comicreader.data.model.ComicCacheInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CacheUiState(
    val cached: List<ComicCacheInfo> = emptyList(),
    val totalSize: Long = 0L,
    val downloads: Map<String, DownloadProgress> = emptyMap()
)

class CacheViewModel(app: Application) : AndroidViewModel(app) {
    private val cacheStore = ComicCacheStore(app)

    val uiState: StateFlow<CacheUiState> =
        combine(cacheStore.cachedComicsFlow(), ComicDownloader.progress) { list, prog ->
            CacheUiState(
                cached = list,
                totalSize = cacheStore.totalSize(),
                downloads = prog
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CacheUiState())

    fun removeCache(id: String) {
        viewModelScope.launch {
            ComicDownloader.clear(id)
            cacheStore.removeCache(id)
        }
    }
}
