package com.comicreader.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.comicreader.data.model.Chapter
import com.comicreader.data.model.Comic
import com.comicreader.ui.Routes
import com.comicreader.ui.components.AppTopBar
import com.comicreader.ui.components.ErrorBox
import com.comicreader.ui.components.LoadingBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(navController: NavHostController) {
    val comicId = navController.currentBackStackEntry?.arguments?.getString("comicId") ?: return
    val vm: DetailViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    var confirmDeleteCache by remember { mutableStateOf(false) }

    LaunchedEffect(comicId) { vm.load(comicId) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = state.detail?.name ?: "详情",
            onBack = { navController.popBackStack() },
            actions = {
                if (state.detail != null) {
                    // 缓存按钮：未缓存→下载；下载中→取消；已缓存→删除
                    val downloading = state.download != null && !state.download!!.finished
                    val fullyCached = state.totalChapters > 0 && state.cachedChapters >= state.totalChapters
                    when {
                        downloading -> IconButton(onClick = vm::cancelDownload) {
                            Icon(Icons.Filled.Close, contentDescription = "取消缓存")
                        }
                        fullyCached -> IconButton(onClick = { confirmDeleteCache = true }) {
                            Icon(
                                Icons.Filled.DownloadDone,
                                contentDescription = "已缓存",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        else -> IconButton(onClick = vm::startDownload) {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = "缓存",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = vm::toggleFavorite) {
                        Icon(
                            if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (state.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )

        when {
            state.loading -> LoadingBox()
            state.error != null -> ErrorBox(state.error) { vm.load(comicId) }
            state.detail != null -> {
                val d = state.detail!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                ) {
                    // 下载进度条
                    state.download?.let { dp ->
                        if (!dp.finished && dp.totalChapters > 0) {
                            item {
                                Column(Modifier.fillMaxWidth().padding(16.dp, 8.dp)) {
                                    LinearProgressIndicator(
                                        progress = { dp.fraction },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "缓存中：${dp.completedChapters}/${dp.totalChapters} 章"
                                            + if (dp.currentChapterName.isNotBlank()) " · ${dp.currentChapterName}" else ""
                                            + if (dp.currentTotalImages > 0) "（${dp.currentImage}/${dp.currentTotalImages} 页）" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (dp.finished) {
                            item {
                                val text = when {
                                    dp.error != null -> "缓存失败：${dp.error}"
                                    dp.totalChapters > 0 && dp.completedChapters < dp.totalChapters ->
                                        "缓存未完成：${dp.completedChapters}/${dp.totalChapters} 章"
                                    else -> "缓存完成（${dp.completedChapters}/${dp.totalChapters} 章）"
                                }
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (dp.error != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(16.dp, 4.dp)
                                )
                            }
                        }
                    }
                    item { DetailHeader(d, state.history) { navController.navigate(Routes.reader(d.id, it.id, it.sort)) } }
                    if (d.chapters.isNotEmpty()) {
                        item {
                            Text(
                                text = "章节（${d.chapters.size}）",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)
                            )
                        }
                        items(d.chapters.sortedByDescending { it.sort }) { chapter ->
                            ChapterRow(chapter) {
                                navController.navigate(Routes.reader(d.id, chapter.id, chapter.sort))
                            }
                            HorizontalDivider()
                        }
                    }
                    if (d.related.isNotEmpty()) {
                        item {
                            Text(
                                text = "相关推荐",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)
                            )
                        }
                        item { RelatedRow(d.related) { navController.navigate(Routes.detail(it.id)) } }
                    }
                }
            }
        }
    }

    // 删除缓存确认
    if (confirmDeleteCache) {
        AlertDialog(
            onDismissRequest = { confirmDeleteCache = false },
            title = { Text("删除缓存") },
            text = { Text("确定删除《${state.detail?.name}》的离线缓存吗？删除后将无法离线阅读。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.removeCache()
                    confirmDeleteCache = false
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteCache = false }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeader(
    detail: com.comicreader.data.model.ComicDetail,
    history: com.comicreader.data.model.HistoryEntry?,
    onOpenChapter: (Chapter) -> Unit
) {
    Column(Modifier.padding(16.dp)) {
        Row {
            AsyncImage(
                model = detail.cover,
                contentDescription = detail.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                Text(detail.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = detail.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                // 始终提供阅读入口：有历史→继续阅读；多章→最新章节；单章→漫画 id 本身作为章节
                val targetChapter = history?.let {
                    Chapter(it.chapterId, it.chapterName, it.sort)
                } ?: if (detail.chapters.isNotEmpty()) {
                    detail.chapters.maxByOrNull { it.sort }
                        ?: Chapter(detail.id, "开始阅读", 0)
                } else {
                    Chapter(detail.id, "开始阅读", 0)
                }
                FilledTonalButton(
                    onClick = { onOpenChapter(targetChapter) }
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (history != null) "继续阅读" else "开始阅读", maxLines = 1)
                }
            }
        }

        if (detail.tags.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                detail.tags.forEach { tag ->
                    AssistChip(onClick = {}, label = { Text(tag) })
                }
            }
        }

        if (detail.description.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = detail.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChapterRow(chapter: Chapter, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chapter.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RelatedRow(related: List<Comic>, onClick: (Comic) -> Unit) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(related, key = { it.id }) { comic ->
            Column(Modifier.width(110.dp)) {
                AsyncImage(
                    model = comic.cover,
                    contentDescription = comic.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(110.dp)
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = comic.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onClick(comic) }
                )
            }
        }
    }
}
