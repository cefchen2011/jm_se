package com.comicreader.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.comicreader.data.FavoritesStore
import com.comicreader.data.model.Comic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BlockedUiState(
    val blocked: List<Comic> = emptyList()
)

class BlockedViewModel(app: Application) : AndroidViewModel(app) {
    private val store = FavoritesStore(app)

    val uiState: StateFlow<BlockedUiState> =
        store.blockedFlow()
            .map { BlockedUiState(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlockedUiState())

    fun removeBlocked(id: String) {
        viewModelScope.launch { store.removeBlocked(id) }
    }
}
