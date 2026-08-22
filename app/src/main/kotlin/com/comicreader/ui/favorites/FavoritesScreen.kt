package com.comicreader.ui.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.comicreader.data.model.Comic
import com.comicreader.ui.Routes
import com.comicreader.ui.components.AppTopBar
import com.comicreader.ui.components.ComicContextMenu
import com.comicreader.ui.components.ComicGrid
import com.comicreader.ui.components.EmptyBox

@Composable
fun FavoritesScreen(navController: NavHostController) {
    val vm: FavoritesViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    var favoritesQuery by rememberSaveable { mutableStateOf("") }
    var longPressComic by remember { mutableStateOf<Comic?>(null) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "收藏",
            onBack = { navController.popBackStack() }
        )
        if (state.favorites.isEmpty()) {
            EmptyBox("暂无收藏")
        } else {
            val filtered = if (favoritesQuery.isBlank()) {
                state.favorites
            } else {
                val q = favoritesQuery.trim()
                state.favorites.filter {
                    it.name.contains(q, ignoreCase = true) ||
                        it.author.contains(q, ignoreCase = true)
                }
            }
            Column(Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = favoritesQuery,
                    onValueChange = { favoritesQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("搜索漫画名 / 作者") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (favoritesQuery.isNotEmpty()) {
                            IconButton(onClick = { favoritesQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "清空搜索")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
                if (filtered.isEmpty()) {
                    EmptyBox("没有匹配的收藏", Modifier.weight(1f))
                } else {
                    Box(Modifier.weight(1f)) {
                        ComicGrid(
                            comics = filtered,
                            onClick = { navController.navigate(Routes.detail(it.id)) },
                            onLongClick = { longPressComic = it },
                            modifier = Modifier.fillMaxSize()
                        )
                        longPressComic?.let { comic ->
                            ComicContextMenu(
                                comic = comic,
                                isFavorite = true,
                                isFollowed = comic.author in state.followedAuthors,
                                onDismiss = { longPressComic = null },
                                onToggleFavorite = { vm.removeFavorite(comic.id); longPressComic = null },
                                onBlock = { vm.blockComic(comic); longPressComic = null },
                                onBlockAuthor = { vm.blockAuthor(comic.author); longPressComic = null },
                                onToggleFollow = { vm.toggleFollowAuthor(comic.author); longPressComic = null },
                                onViewDetail = {
                                    longPressComic = null
                                    navController.navigate(Routes.detail(comic.id))
                                },
                                onViewAuthor = {
                                    longPressComic = null
                                    navController.navigate(Routes.author(comic.author))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}