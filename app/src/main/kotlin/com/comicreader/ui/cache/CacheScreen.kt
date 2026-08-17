package com.comicreader.ui.cache

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.comicreader.data.model.ComicCacheInfo
import com.comicreader.ui.Routes
import com.comicreader.ui.components.AppTopBar
import com.comicreader.ui.components.EmptyBox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CacheScreen(navController: NavHostController) {
    val vm: CacheViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ComicCacheInfo?>(null) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "缓存",
            onBack = { navController.popBackStack() }
        )
        if (state.cached.isEmpty()) {
            EmptyBox("暂无缓存，去漫画详情页点下载按钮开始缓存")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = "共 ${state.cached.size} 部 · 占用 ${formatSize(state.totalSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(state.cached, key = { it.comicId }) { info ->
                    CacheRow(
                        info = info,
                        progress = state.downloads[info.comicId],
                        onClick = { navController.navigate(Routes.detail(info.comicId)) },
                        onDelete = { pendingDelete = info }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    pendingDelete?.let { info ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除缓存") },
            text = { Text("确定删除《${info.comicName}》的离线缓存（${info.cachedCount}/${info.totalChapters} 章）吗？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.removeCache(info.comicId)
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun CacheRow(
    info: ComicCacheInfo,
    progress: com.comicreader.data.DownloadProgress?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = info.cover,
            contentDescription = info.comicName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(52.dp)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(6.dp))
        )
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                text = info.comicName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            val active = progress != null && !progress!!.finished
            if (active) {
                Text(
                    text = "缓存中：${progress!!.completedChapters}/${info.totalChapters} 章"
                        + if (progress!!.currentChapterName.isNotBlank()) " · ${progress!!.currentChapterName}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LinearProgressIndicator(
                    progress = { progress!!.fraction },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            } else {
                Text(
                    text = "${info.cachedCount}/${info.totalChapters} 章 · ${formatDate(info.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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

private fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    return when {
        kb < 1024 -> "%.0f KB".format(kb)
        else -> "%.1f MB".format(kb / 1024)
    }
}

private fun formatDate(ts: Long): String =
    SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(ts))
