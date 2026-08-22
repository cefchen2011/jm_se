package com.comicreader.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.comicreader.data.FavoritesStore
import com.comicreader.data.model.Comic
import com.comicreader.data.model.HistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favorites: List<Comic> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    /** 已收藏的漫画 id（用于长按菜单显示"加入/取消收藏"） */
    val favoriteIds: Set<String> = emptySet(),
    /** 关注的作者名 */
    val followedAuthors: List<String> = emptyList()
)

class FavoritesViewModel(app: Application) : AndroidViewModel(app) {
    private val store = FavoritesStore(app)
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            store.favoritesFlow().collect { favs ->
                _uiState.update { it.copy(
                    favorites = favs,
                    favoriteIds = favs.map { c -> c.id }.toSet()
                ) }
            }
        }
        viewModelScope.launch {
            store.historyFlow().collect { hist ->
                _uiState.update { it.copy(history = hist) }
            }
        }
        viewModelScope.launch {
            store.followedAuthorsFlow().collect { a ->
                _uiState.update { it.copy(followedAuthors = a) }
            }
        }
    }

    fun removeFavorite(id: String) {
        viewModelScope.launch { store.removeFavorite(id) }
    }

    fun removeHistoryEntry(comicId: String) {
        viewModelScope.launch { store.removeHistoryEntry(comicId) }
    }

    fun clearHistory() {
        viewModelScope.launch { store.clearHistory() }
    }

    fun toggleFavorite(comic: Comic) {
        viewModelScope.launch {
            if (_uiState.value.favoriteIds.contains(comic.id)) store.removeFavorite(comic.id)
            else store.addFavorite(comic)
        }
    }

    fun blockComic(comic: Comic) {
        viewModelScope.launch {
            store.addBlocked(comic)
            store.removeFavorite(comic.id)
        }
    }

    fun blockAuthor(author: String) {
        viewModelScope.launch {
            store.addBlockedAuthor(author)
        }
    }

    fun unfollowAuthor(author: String) {
        viewModelScope.launch { store.removeFollowedAuthor(author) }
    }

    fun toggleFollowAuthor(author: String) {
        viewModelScope.launch {
            if (_uiState.value.followedAuthors.contains(author)) {
                store.removeFollowedAuthor(author)
            } else {
                store.addFollowedAuthor(author)
            }
        }
    }
}