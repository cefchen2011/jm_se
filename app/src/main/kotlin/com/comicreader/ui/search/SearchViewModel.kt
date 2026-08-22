package com.comicreader.ui.search

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

data class SearchUiState(
    val query: String = "",
    val submitted: String = "",
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val comics: List<Comic> = emptyList(),
    val hotTags: List<String> = emptyList(),
    val endReached: Boolean = false,
    val hasSearched: Boolean = false,
    /** 已收藏的漫画 id（长按菜单） */
    val favoriteIds: Set<String> = emptySet()
)

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = JmApiClient.repository
    private val store = FavoritesStore(app)
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var page = 1

    /** 已屏蔽的漫画 id */
    private val blockedIds = MutableStateFlow<Set<String>>(emptySet())

    /** 已屏蔽的作者名 */
    private val blockedAuthors = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            store.favoritesFlow().collect { favs ->
                _uiState.update { it.copy(favoriteIds = favs.map { c -> c.id }.toSet()) }
            }
        }
        viewModelScope.launch {
            store.blockedFlow().collect { b ->
                blockedIds.value = b.map { it.id }.toSet()
                _uiState.update { it.copy(comics = it.comics.filterNot { c -> c.id in blockedIds.value }) }
            }
        }
        viewModelScope.launch {
            store.blockedAuthorsFlow().collect { a ->
                blockedAuthors.value = a.toSet()
                _uiState.update { it.copy(comics = it.comics.filterNot { c -> c.author in blockedAuthors.value }) }
            }
        }
        viewModelScope.launch {
            runCatching { repo.hotTags() }
                .onSuccess { _uiState.update { s -> s.copy(hotTags = it) } }
        }
        viewModelScope.launch {
            SearchBus.pendingQuery.collect { q ->
                if (!q.isNullOrBlank()) {
                    SearchBus.pendingQuery.value = null
                    search(q.trim())
                }
            }
        }
    }

    fun setQuery(q: String) {
        _uiState.update { it.copy(query = q) }
    }

    fun submit() {
        val q = _uiState.value.query.trim()
        if (q.isNotEmpty()) search(q)
    }

    fun searchTag(tag: String) {
        search(tag)
    }

    private fun search(q: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    query = q,
                    submitted = q,
                    loading = true,
                    hasSearched = true,
                    error = null,
                    comics = emptyList(),
                    endReached = false
                )
            }
            page = 1
            try {
                val comics = repo.search(q, "mr", 1)
                _uiState.update {
                    it.copy(
                        loading = false,
                        comics = comics.filterNot { c -> c.id in blockedIds.value || c.author in blockedAuthors.value },
                        endReached = comics.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun loadMore() {
        val s = _uiState.value
        if (s.loadingMore || s.endReached || s.loading || s.submitted.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            try {
                val next = repo.search(s.submitted, "mr", page + 1)
                page++
                val existingIds = _uiState.value.comics.map { c -> c.id }.toSet()
                val newComics = next.filterNot { c -> c.id in existingIds || c.id in blockedIds.value || c.author in blockedAuthors.value }
                _uiState.update {
                    it.copy(
                        comics = it.comics + newComics,
                        loadingMore = false,
                        endReached = next.isEmpty() || newComics.isEmpty()
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

    /** 屏蔽某作者的全部作品，立即从当前列表移除 */
    fun blockAuthor(author: String) {
        viewModelScope.launch {
            store.addBlockedAuthor(author)
            _uiState.update { it.copy(comics = it.comics.filterNot { c -> c.author == author }) }
        }
    }
}
