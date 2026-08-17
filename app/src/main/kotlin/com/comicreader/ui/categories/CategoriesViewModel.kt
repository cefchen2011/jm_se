package com.comicreader.ui.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.comicreader.data.FavoritesStore
import com.comicreader.data.JmApiClient
import com.comicreader.data.model.Category
import com.comicreader.data.model.Comic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val categories: List<Category> = emptyList(),
    val selected: Category? = null,
    val comics: List<Comic> = emptyList(),
    val loadingComics: Boolean = false,
    val loadingMore: Boolean = false,
    val comicsError: String? = null,
    val endReached: Boolean = false,
    /** 已收藏的漫画 id（长按菜单） */
    val favoriteIds: Set<String> = emptySet()
)

class CategoriesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = JmApiClient.repository
    private val store = FavoritesStore(app)
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()
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
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val cats = repo.categories()
                _uiState.update { it.copy(loading = false, categories = cats) }
                if (cats.isNotEmpty()) selectCategory(cats.first())
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun selectCategory(cat: Category) {
        if (_uiState.value.selected?.slug == cat.slug && _uiState.value.comics.isNotEmpty()) return
        _uiState.update { it.copy(selected = cat, comics = emptyList(), endReached = false) }
        page = 1
        viewModelScope.launch {
            _uiState.update { it.copy(loadingComics = true, comicsError = null) }
            try {
                val comics = repo.categoriesFilter(cat.slug, "mr", 1)
                _uiState.update {
                    it.copy(
                        loadingComics = false,
                        comics = comics.filterNot { c -> c.id in blockedIds.value || c.author in blockedAuthors.value },
                        endReached = comics.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loadingComics = false, comicsError = e.message) }
            }
        }
    }

    fun loadMore() {
        val s = _uiState.value
        val slug = s.selected?.slug ?: return
        if (s.loadingMore || s.endReached || s.loadingComics) return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            try {
                val next = repo.categoriesFilter(slug, "mr", page + 1)
                page++
                _uiState.update {
                    it.copy(
                        comics = it.comics + next.filterNot { c -> c.id in blockedIds.value || c.author in blockedAuthors.value },
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

    /** 屏蔽某作者的全部作品，立即从当前列表移除 */
    fun blockAuthor(author: String) {
        viewModelScope.launch {
            store.addBlockedAuthor(author)
            _uiState.update { it.copy(comics = it.comics.filterNot { c -> c.author == author }) }
        }
    }
}
