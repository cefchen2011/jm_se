package com.comicreader.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.comicreader.data.model.Comic

/**
 * 长按漫画弹出的上下文菜单：
 * - 加入收藏 / 取消收藏（根据状态）
 * - 屏蔽（从所有列表隐藏该作品）
 * - 屏蔽该作者作品（隐藏该作者的全部作品）
 * - 查看作者其他作品
 */
@Composable
fun ComicContextMenu(
    comic: Comic,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBlock: () -> Unit,
    onBlockAuthor: () -> Unit,
    onViewAuthor: () -> Unit
) {
    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(if (isFavorite) "取消收藏" else "加入收藏") },
            leadingIcon = {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null
                )
            },
            onClick = { onToggleFavorite() }
        )
        DropdownMenuItem(
            text = { Text("屏蔽") },
            leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null) },
            onClick = { onBlock() }
        )
        if (comic.author.isNotBlank()) {
            DropdownMenuItem(
                text = { Text("屏蔽该作者作品") },
                leadingIcon = { Icon(Icons.Filled.PersonOff, contentDescription = null) },
                onClick = { onBlockAuthor() }
            )
        }
        if (comic.author.isNotBlank()) {
            DropdownMenuItem(
                text = { Text("查看作者其他作品") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                onClick = { onViewAuthor() }
            )
        }
    }
}
