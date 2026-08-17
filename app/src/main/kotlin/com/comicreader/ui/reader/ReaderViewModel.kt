package com.comicreader.ui.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.comicreader.data.FavoritesStore
import com.comicreader.data.JmApiClient
import com.comicreader.data.model.Chapter
import com.comicreader.data.model.ChapterImages
import com.comicreader.data.model.ComicDetail
import com.comicreader.data.model.HistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val comicId: String = "",
    val detail: ComicDetail? = null,
    val chapters: List<Chapter> = emptyList(),
    val current: ChapterImages? = null,
    val currentChapter: Chapter? = null
)

class ReaderViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = JmApiClient.repository
    private val store = FavoritesStore(app)
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    fun load(comicId: String, chapterId: String, sort: Int) {
        if (_uiState.value.comicId == comicId && _uiState.value.currentChapter?.id == chapterId && _uiState.value.current != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, comicId = comicId) }
            try {
                val images = repo.chapterImages(chapterId)
                val detail = runCatching { repo.detail(comicId) }.getOrNull()
                val chapters = detail?.chapters ?: emptyList()
                val idx = chapters.indexOfFirst { it.id == chapterId || it.sort == sort }
                val chapter = chapters.getOrNull(idx) ?: Chapter(chapterId, images.name, sort)
                _uiState.update {
                    it.copy(
                        loading = false,
                        detail = detail,
                        chapters = chapters,
                        current = images,
                        currentChapter = chapter
                    )
                }
                record(detail, images, chapter, comicId)
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun openChapter(chapter: Chapter) {
        val s = _uiState.value
        if (chapter.id.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val images = repo.chapterImages(chapter.id)
                _uiState.update {
                    it.copy(loading = false, current = images, currentChapter = chapter)
                }
                record(s.detail, images, chapter, s.comicId)
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun prevNext(delta: Int) {
        val s = _uiState.value
        val idx = s.chapters.indexOfFirst { it.id == s.currentChapter?.id }
        if (idx < 0) return
        val target = s.chapters.getOrNull(idx + delta) ?: return
        openChapter(target)
    }

    private suspend fun record(detail: ComicDetail?, images: ChapterImages, chapter: Chapter, comicId: String) {
        val chapters = detail?.chapters ?: emptyList()
        val idx = chapters.indexOfFirst { it.id == chapter.id }
        // 当前章节是最后一章 => 已读完（单章漫画看完也标记）
        val finished = chapters.isNotEmpty() && (idx >= 0 && idx == chapters.lastIndex ||
            chapter.sort >= (chapters.maxOfOrNull { it.sort } ?: 0))
        store.recordProgress(
            HistoryEntry(
                comicId = comicId,
                comicName = detail?.name ?: "",
                cover = detail?.cover ?: "",
                author = detail?.author ?: "",
                chapterId = chapter.id,
                chapterName = chapter.name.ifBlank { images.name },
                sort = chapter.sort,
                finished = finished
            )
        )
    }
}
