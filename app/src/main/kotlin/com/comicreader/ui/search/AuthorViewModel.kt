package com.comicreader.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    val endReached: Boolean = false
)

/** 作者其他作品列表（通过搜索接口按作者名检索） */
class AuthorViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = JmApiClient.repository
    private val _uiState = MutableStateFlow(AuthorUiState())
    val uiState: StateFlow<AuthorUiState> = _uiState.asStateFlow()
    private var page = 1
    private var author: String = ""

    fun load(author: String) {
        if (this.author == author && _uiState.value.comics.isNotEmpty()) return
        this.author = author
        _uiState.update { it.copy(loading = true, error = null, comics = emptyList()) }
        page = 1
        viewModelScope.launch {
            try {
                val comics = repo.search(author, "mr", 1)
                _uiState.update { it.copy(loading = false, comics = comics, endReached = comics.isEmpty()) }
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
                _uiState.update {
                    it.copy(
                        comics = it.comics + next,
                        loadingMore = false,
                        endReached = next.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loadingMore = false) }
            }
        }
    }
}
