package com.comicreader.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.comicreader.data.FavoritesStore
import com.comicreader.data.JmApiClient
import com.comicreader.data.model.Comic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val comics: List<Comic> = emptyList(),
    val hotTags: List<String> = emptyList(),
    val endReached: Boolean = false,
    /** 已收藏的漫画 id（用于长按菜单显示"加入/取消收藏"） */
    val favoriteIds: Set<String> = emptySet()
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = JmApiClient.repository
    private val store = FavoritesStore(app)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var page = 1

    /** 已读完 + 已屏蔽的漫画 id（主页不再展示） */
    private val hiddenIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            store.favoritesFlow().collect { favs ->
                _uiState.update { it.copy(favoriteIds = favs.map { c -> c.id }.toSet()) }
            }
        }
        viewModelScope.launch {
            val finished = store.historyFlow()
            val blocked = store.blockedFlow()
            // 合并两个流为隐藏 id 集合
            kotlinx.coroutines.flow.combine(finished, blocked) { h, b ->
                h.filter { it.finished }.map { it.comicId }.toSet() +
                    b.map { it.id }.toSet()
            }.collect { hiddenIds.value = it }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true, error = null) }
            try {
                val comics = repo.latest(1)
                val tags = runCatching { repo.hotTags() }.getOrDefault(emptyList())
                page = 1
                _uiState.update {
                    it.copy(
                        comics = comics.filterNot { c -> c.id in hiddenIds.value },
                        hotTags = tags,
                        refreshing = false,
                        loading = false,
                        endReached = comics.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(refreshing = false, loading = false, error = e.message) }
            }
        }
    }

    fun loadMore() {
        val s = _uiState.value
        if (s.loadingMore || s.endReached || s.loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            try {
                val next = repo.latest(page + 1)
                page++
                _uiState.update {
                    it.copy(
                        comics = it.comics + next.filterNot { c -> c.id in hiddenIds.value },
                        loadingMore = false,
                        endReached = next.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loadingMore = false) }
            }
        }
    }

    fun toggleFavorite(comic: Comic) {
        viewModelScope.launch {
            if (_uiState.value.favoriteIds.contains(comic.id)) store.removeFavorite(comic.id)
            else store.addFavorite(comic)
        }
    }

    /** 屏蔽后立即从当前列表移除 */
    fun blockComic(comic: Comic) {
        viewModelScope.launch {
            store.addBlocked(comic)
            _uiState.update { it.copy(comics = it.comics.filterNot { c -> c.id == comic.id }) }
        }
    }
}
