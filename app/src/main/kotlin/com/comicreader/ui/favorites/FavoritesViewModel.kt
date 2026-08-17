package com.comicreader.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.comicreader.data.FavoritesStore
import com.comicreader.data.model.Comic
import com.comicreader.data.model.HistoryEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favorites: List<Comic> = emptyList(),
    val history: List<HistoryEntry> = emptyList()
)

class FavoritesViewModel(app: Application) : AndroidViewModel(app) {
    private val store = FavoritesStore(app)

    val uiState: StateFlow<FavoritesUiState> =
        combine(store.favoritesFlow(), store.historyFlow()) { favs, hist ->
            FavoritesUiState(favs, hist)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FavoritesUiState())

    fun removeFavorite(id: String) {
        viewModelScope.launch { store.removeFavorite(id) }
    }

    fun removeHistoryEntry(comicId: String) {
        viewModelScope.launch { store.removeHistoryEntry(comicId) }
    }

    fun clearHistory() {
        viewModelScope.launch { store.clearHistory() }
    }
}
