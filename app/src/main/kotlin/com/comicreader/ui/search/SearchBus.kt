package com.comicreader.ui.search

import kotlinx.coroutines.flow.MutableStateFlow

/** 跨 Tab 传递搜索词（首页热门标签 → 搜索页） */
object SearchBus {
    val pendingQuery = MutableStateFlow<String?>(null)
}
