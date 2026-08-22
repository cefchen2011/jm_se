package com.comicreader.ui.favorites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.comicreader.data.model.Comic
import com.comicreader.data.model.HistoryEntry
import com.comicreader.ui.Routes
import com.comicreader.ui.components.AppTopBar
import com.comicreader.ui.components.ComicContextMenu
import com.comicreader.ui.components.EmptyBox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(navController: NavHostController) {
    val vm: FavoritesViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<HistoryEntry?>(null) }
    var confirmClearAll by remember { mutableStateOf(false) }
    var longPressEntry by remember { mutableStateOf<HistoryEntry?>(null) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "历史",
            onBack = { navController.popBackStack() }
        )
        if (state.history.isEmpty()) {
            EmptyBox("暂无阅读记录")
        } else {
            val filtered = if (searchQuery.isBlank()) {
                state.history
            } else {
                val q = searchQuery.trim()
                state.history.filter {
                    it.comicName.contains(q, ignoreCase = true) ||
                        it.chapterName.contains(q, ignoreCase = true) ||
                        it.author.contains(q, ignoreCase = true)
                }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("搜索漫画名 / 章节 / 作者") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "清空搜索")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "最近阅读" else "搜索结果（${filtered.size}）",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { confirmClearAll = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "清空全部")
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    item { EmptyBox("没有匹配的记录", Modifier.padding(vertical = 48.dp)) }
                } else {
                    val unfinished = filtered.filter { !it.finished }
                    val finished = filtered.filter { it.finished }
                    if (unfinished.isNotEmpty()) {
                        item {
                            Text(
                                text = "未读完（${unfinished.size}）",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        items(unfinished, key = { it.comicId + it.chapterId }) { h ->
                            HistoryRow(
                                entry = h,
                                onClick = { navController.navigate(Routes.reader(h.comicId, h.chapterId, h.sort)) },
                                onLongClick = { longPressEntry = h },
                                onDelete = { pendingDelete = h }
                            )
                            HorizontalDivider()
                        }
                    }
                    if (finished.isNotEmpty()) {
                        item {
                            Text(
                                text = "已读完（${finished.size}）",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        items(finished, key = { it.comicId + "f" + it.chapterId }) { h ->
                            HistoryRow(
                                entry = h,
                                onClick = { navController.navigate(Routes.reader(h.comicId, h.chapterId, h.sort)) },
                                onLongClick = { longPressEntry = h },
                                onDelete = { pendingDelete = h }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    // 长按菜单
    longPressEntry?.let { entry ->
        val author = entry.author.trim().removeSurrounding("[\"", "\"]").removeSurrounding("[", "]")
        val comic = Comic(entry.comicId, entry.comicName, author, cover = entry.cover)
        Box(Modifier.fillMaxSize()) {
            ComicContextMenu(
                comic = comic,
                isFavorite = comic.id in state.favoriteIds,
                isFollowed = author in state.followedAuthors,
                onDismiss = { longPressEntry = null },
                onToggleFavorite = { vm.toggleFavorite(comic); longPressEntry = null },
                onBlock = { vm.blockComic(comic); longPressEntry = null },
                onBlockAuthor = { vm.blockAuthor(author); longPressEntry = null },
                onToggleFollow = { vm.toggleFollowAuthor(author); longPressEntry = null },
                onViewDetail = {
                    longPressEntry = null
                    navController.navigate(Routes.detail(comic.id))
                },
                onViewAuthor = {
                    longPressEntry = null
                    navController.navigate(Routes.author(author))
                }
            )
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除记录") },
            text = { Text("确定删除《${entry.comicName}》的阅读记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.removeHistoryEntry(entry.comicId)
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("清空历史") },
            text = { Text("确定清空全部阅读记录吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearHistory()
                    confirmClearAll = false
                }) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = entry.cover,
            contentDescription = entry.comicName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(52.dp)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(6.dp))
        )
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                text = entry.comicName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "看到：${entry.chapterName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val chapNum = entry.sort + 1
                val pageNum = entry.currentPage + 1
                val singleChapter = entry.totalChapters <= 1
                val pageStr = if (entry.totalPages > 0) "章 ${pageNum}/${entry.totalPages}" else "章 ${chapNum}/${entry.totalChapters}"
                val suffix = if (entry.finished) "  · ✓ 已读完"
                    else if (!singleChapter) "  · 书 ${chapNum}/${entry.totalChapters}"
                    else ""
                Text(
                    text = "${pageStr}${suffix}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry.finished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 相对时间显示：刚刚 / x分钟前 / x小时前 / x天前 / 具体日期 */
private fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "${minutes}分钟前"
        hours < 24 -> "${hours}小时前"
        days < 7 -> "${days}天前"
        days < 30 -> "${days / 7}周前"
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }
}