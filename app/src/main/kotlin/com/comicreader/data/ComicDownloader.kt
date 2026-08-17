package com.comicreader.data

import android.util.Log
import com.comicreader.data.model.ComicCacheInfo
import com.comicreader.data.model.ComicDetail
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/** 缓存下载进度 */
data class DownloadProgress(
    val comicId: String,
    val totalChapters: Int = 0,
    val completedChapters: Int = 0,
    val currentChapterName: String = "",
    val currentImage: Int = 0,
    val currentTotalImages: Int = 0,
    val finished: Boolean = false,
    val error: String? = null
) {
    /** 0f..1f 总进度（章节维度） */
    val fraction: Float get() = if (totalChapters <= 0) 0f else completedChapters.toFloat() / totalChapters
}

/**
 * 漫画离线下载器：
 * - 逐章获取图片清单，逐图下载到本地（保留原始文件，阅读时仍做扰码还原）
 * - 进度以 [progress] 全局状态暴露（key = comicId）
 * - 通过协程取消（Job.cancel）中断，已完成的章节保留
 */
object ComicDownloader {

    private val _progress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val progress: StateFlow<Map<String, DownloadProgress>> = _progress.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun downloadComic(detail: ComicDetail, store: ComicCacheStore) {
        val comicId = detail.id
        // 单章漫画（无 series）：把漫画 id 本身当作唯一章节来缓存
        val chapters = if (detail.chapters.isNotEmpty()) detail.chapters
        else listOf(com.comicreader.data.model.Chapter(comicId, "开始阅读", 0))
        _progress.update { m ->
            m + (comicId to DownloadProgress(comicId = comicId, totalChapters = chapters.size))
        }
        try {
            // 封面（可选，失败忽略）
            runCatching {
                if (detail.cover.isNotBlank()) {
                    downloadTo(detail.cover, store.coverFileOf(comicId))
                }
            }
            var completed = 0
            val doneIds = mutableListOf<String>()
            val failReasons = mutableListOf<String>()
            for (chapter in chapters) {
                coroutineContext.ensureActive()
                val images = try {
                    JmApiClient.repository.chapterImages(chapter.id)
                } catch (e: Exception) {
                    Log.e("ComicCache", "章节 ${chapter.id} 图片清单获取失败: ${e.message}")
                    failReasons += "第${chapter.sort}章清单失败: ${e.message}"
                    continue
                }
                val chapterDir = store.chapterDirOf(comicId, chapter.id)
                chapterDir.mkdirs()
                var okImages = 0
                for ((idx, img) in images.images.withIndex()) {
                    coroutineContext.ensureActive()
                    val imgImageBrief = img.image.take(80)
                    val ext = img.image.substringAfterLast('.', "webp").substringBefore('?')
                    val file = File(chapterDir, String.format("%04d.%s", img.page, ext))
                    // 已有有效文件（>1KB，排除接口返回的错误 JSON 占位）则跳过；无效则删除重下
                    if (file.exists() && file.length() > 1024) {
                        okImages++
                    } else {
                        file.delete()
                        if (downloadTo(img.image, file) && file.length() > 1024) {
                            okImages++
                        } else {
                            file.delete()
                            if (failReasons.size < 5) failReasons += "图片 ${img.page} 下载失败: $imgImageBrief"
                        }
                    }
                    _progress.update { m ->
                        m + (comicId to (_progress.value[comicId] ?: DownloadProgress(comicId)).copy(
                            currentChapterName = chapter.name,
                            currentImage = idx + 1,
                            currentTotalImages = images.images.size
                        ))
                    }
                }
                // 章节完成标准：无图章节视为完成；有图章节需全部图片下载成功
                val chapterDone = images.images.isEmpty() || okImages == images.images.size
                if (chapterDone) {
                    completed++
                    doneIds += chapter.id
                    _progress.update { m ->
                        m + (comicId to (_progress.value[comicId] ?: DownloadProgress(comicId)).copy(
                            completedChapters = completed,
                            currentChapterName = chapter.name
                        ))
                    }
                    store.upsertCache(
                        ComicCacheInfo(
                            comicId = comicId,
                            comicName = detail.name,
                            cover = detail.cover,
                            author = detail.author,
                            totalChapters = chapters.size,
                            cachedChapterIds = doneIds.toList()
                        )
                    )
                }
            }
            val finished = completed == chapters.size
            _progress.update { m ->
                m + (comicId to (_progress.value[comicId] ?: DownloadProgress(comicId)).copy(
                    completedChapters = completed,
                    finished = true,
                    error = if (finished) null
                    else "缓存未完成：$completed/${chapters.size} 章" +
                        if (failReasons.isNotEmpty()) "\n${failReasons.take(3).joinToString("\n")}" else ""
                ))
            }
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            _progress.update { m ->
                m + (comicId to (_progress.value[comicId] ?: DownloadProgress(comicId)).copy(
                    error = e.message ?: "下载失败"
                ))
            }
        }
    }

    fun clear(comicId: String) {
        _progress.update { it - comicId }
    }

    private fun downloadTo(url: String, file: File): Boolean {
        return try {
            val request = Request.Builder().url(url)
                .header("Referer", "https://18comic.vip/")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.e("ComicCache", "图片下载 HTTP ${resp.code}: ${url.take(100)}")
                    return false
                }
                val bytes = resp.body?.bytes() ?: return false
                file.parentFile?.mkdirs()
                file.writeBytes(bytes)
                true
            }
        } catch (e: Exception) {
            Log.e("ComicCache", "图片下载异常: ${e.message} url=${url.take(100)}")
            false
        }
    }
}
