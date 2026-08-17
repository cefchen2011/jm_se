package com.comicreader.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.comicreader.data.ComicCacheStore
import com.comicreader.data.ComicDownloader
import com.comicreader.data.DownloadProgress
import com.comicreader.data.FavoritesStore
import com.comicreader.data.JmApiClient
import com.comicreader.data.model.Comic
import com.comicreader.data.model.ComicDetail
import com.comicreader.data.model.ComicCacheInfo
import com.comicreader.data.model.HistoryEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val detail: ComicDetail? = null,
    val isFavorite: Boolean = false,
    val history: HistoryEntry? = null,
    /** 该漫画已缓存章节数 */
    val cachedChapters: Int = 0,
    val totalChapters: Int = 0,
    /** 下载进度（进行中） */
    val download: DownloadProgress? = null
)

class DetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = JmApiClient.repository
    private val store = FavoritesStore(app)
    private val cacheStore = ComicCacheStore(app)
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    private var comicId: String = ""
    private var downloadJob: Job? = null

    fun load(id: String) {
        if (comicId == id && _uiState.value.detail != null) return
        comicId = id
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val detail = repo.detail(id)
                _uiState.update {
                    it.copy(
                        loading = false,
                        detail = detail,
                        totalChapters = detail.chapters.size
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
        viewModelScope.launch {
            store.favoritesFlow().collect { favs ->
                _uiState.update { it.copy(isFavorite = favs.any { f -> f.id == id }) }
            }
        }
        viewModelScope.launch {
            store.historyFlow().collect { hist ->
                _uiState.update { it.copy(history = hist.firstOrNull { h -> h.comicId == id }) }
            }
        }
        viewModelScope.launch {
            cacheStore.cachedComicsFlow().collect { list ->
                val info = list.firstOrNull { it.comicId == id }
                _uiState.update {
                    it.copy(
                        cachedChapters = info?.cachedCount ?: 0,
                        totalChapters = info?.totalChapters ?: it.totalChapters
                    )
                }
            }
        }
        viewModelScope.launch {
            ComicDownloader.progress.collect { map ->
                _uiState.update { it.copy(download = map[id]) }
            }
        }
    }

    /** 开始缓存全部章节 */
    fun startDownload() {
        val d = _uiState.value.detail ?: return
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            ComicDownloader.downloadComic(d, cacheStore)
        }
    }

    /** 取消/停止下载（已完成的章节保留） */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        ComicDownloader.clear(comicId)
        _uiState.update { it.copy(download = null) }
    }

    /** 删除已缓存内容 */
    fun removeCache() {
        viewModelScope.launch {
            downloadJob?.cancel()
            downloadJob = null
            ComicDownloader.clear(comicId)
            cacheStore.removeCache(comicId)
            _uiState.update { it.copy(cachedChapters = 0, download = null) }
        }
    }

    fun toggleFavorite() {
        val d = _uiState.value.detail ?: return
        viewModelScope.launch {
            if (_uiState.value.isFavorite) store.removeFavorite(d.id)
            else store.addFavorite(Comic(d.id, d.name, d.author, cover = d.cover))
        }
    }

    override fun onCleared() {
        downloadJob?.cancel()
        super.onCleared()
    }
}
