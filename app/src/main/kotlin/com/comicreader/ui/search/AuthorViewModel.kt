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

data class AuthorUiState(
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val comics: List<Comic> = emptyList(),
    val endReached: Boolean = false,
    /** 已收藏的漫画 id（长按菜单） */
    val favoriteIds: Set<String> = emptySet()
)

/** 作者其他作品列表（通过搜索接口按作者名检索） */
class AuthorViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = JmApiClient.repository
    private val store = FavoritesStore(app)
    private val _uiState = MutableStateFlow(AuthorUiState())
    val uiState: StateFlow<AuthorUiState> = _uiState.asStateFlow()
    private var page = 1
    private var author: String = ""

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
    }

    fun load(author: String) {
        if (this.author == author && _uiState.value.comics.isNotEmpty()) return
        this.author = author
        _uiState.update { it.copy(loading = true, error = null, comics = emptyList()) }
        page = 1
        viewModelScope.launch {
            try {
                val comics = repo.search(author, "mr", 1)
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
        if (s.loadingMore || s.endReached || s.loading || author.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            try {
                val next = repo.search(author, "mr", page + 1)
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
