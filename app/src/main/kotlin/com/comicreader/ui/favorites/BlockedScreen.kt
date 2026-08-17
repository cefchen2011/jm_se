package com.comicreader.ui.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.comicreader.data.model.Comic
import com.comicreader.ui.components.AppTopBar
import com.comicreader.ui.components.ComicGrid
import com.comicreader.ui.components.EmptyBox

@Composable
fun BlockedScreen(navController: NavHostController) {
    val vm: BlockedViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    var pendingUnblock by remember { mutableStateOf<Comic?>(null) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "屏蔽",
            onBack = { navController.popBackStack() }
        )
        if (state.blocked.isEmpty() && state.blockedAuthors.isEmpty()) {
            EmptyBox("暂无屏蔽的内容")
        } else {
            Box(Modifier.fillMaxSize()) {
                ComicGrid(
                    comics = state.blocked,
                    onClick = { pendingUnblock = it },
                    header = {
                        BlockedAuthorsSection(
                            authors = state.blockedAuthors,
                            onRemove = { vm.removeBlockedAuthor(it) }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    pendingUnblock?.let { comic ->
        AlertDialog(
            onDismissRequest = { pendingUnblock = null },
            title = { Text("取消屏蔽") },
            text = { Text("确定取消屏蔽《${comic.name}》吗？取消后将重新在列表中展示。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.removeBlocked(comic.id)
                    pendingUnblock = null
                }) { Text("取消屏蔽") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnblock = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun BlockedAuthorsSection(
    authors: List<String>,
    onRemove: (String) -> Unit
) {
    if (authors.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Text(
            text = "屏蔽的作者",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        authors.forEach { author ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = author,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = { onRemove(author) }) { Text("取消屏蔽") }
            }
        }
    }
}
