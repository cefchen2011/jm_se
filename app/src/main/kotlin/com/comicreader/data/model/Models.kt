package com.comicreader.data.model

/** 漫画列表条目 */
data class Comic(
    val id: String,
    val name: String,
    val author: String,
    val updateAt: String = "",
    val cover: String = ""
)

/** 章节条目 */
data class Chapter(
    val id: String,
    val name: String,
    val sort: Int = 0
)

/** 漫画详情 */
data class ComicDetail(
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val cover: String,
    val tags: List<String>,
    val chapters: List<Chapter>,
    val related: List<Comic>
)

/** 章节内单页图片 */
data class PageImage(
    val image: String,
    val page: Int = 0
)

/** 章节图片数据（用于阅读器） */
data class ChapterImages(
    val id: String,
    val name: String,
    val scrambleId: String,
    val images: List<PageImage>
)

/** 分类标签 */
data class Category(
    val slug: String,
    val name: String
)

/**
 * 阅读进度记录
 * @param totalChapters 该漫画总章节数（用于计算书进度百分比）
 * @param currentPage 当前阅读到的页码（0-based），-1 表示整章已读完
 * @param totalPages 当前章节总页数
 */
data class HistoryEntry(
    val comicId: String,
    val comicName: String,
    val cover: String,
    val author: String,
    val chapterId: String,
    val chapterName: String,
    val sort: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    /** 是否已读完最后一章 */
    val finished: Boolean = false,
    val totalChapters: Int = 0,
    val currentPage: Int = 0,
    val totalPages: Int = 0
)

/** 缓存漫画元数据 */
data class ComicCacheInfo(
    val comicId: String,
    val comicName: String,
    val cover: String = "",
    val author: String = "",
    val totalChapters: Int = 0,
    /** 已成功缓存的章节 id 列表 */
    val cachedChapterIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val cachedCount: Int get() = cachedChapterIds.size
}
