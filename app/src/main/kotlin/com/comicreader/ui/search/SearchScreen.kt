package com.comicreader.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.comicreader.ui.components.ErrorBox
import com.comicreader.ui.components.LoadingBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavHostController) {
    val vm: SearchViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    var longPressComic by remember { mutableStateOf<Comic?>(null) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "搜索")
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("输入关键词") },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = vm::submit) {
                    Icon(Icons.Filled.Search, contentDescription = "搜索")
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.submit() })
        )

        when {
            state.loading -> LoadingBox()
            state.error != null && state.comics.isEmpty() -> ErrorBox(state.error, onRetry = vm::submit)
            !state.hasSearched -> {
                if (state.hotTags.isNotEmpty()) {
                    Column(Modifier.padding(top = 16.dp)) {
                        Text(
                            text = "热门标签",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.hotTags) { tag ->
                                SuggestionChip(
                                    onClick = { vm.searchTag(tag) },
                                    label = { Text("#$tag") }
                                )
                            }
                        }
                    }
                }
            }
            state.hasSearched && state.comics.isEmpty() -> EmptyBox("没有找到相关漫画")
            else -> Box(Modifier.fillMaxSize()) {
                ComicGrid(
                    comics = state.comics,
                    onClick = { navController.navigate(Routes.detail(it.id)) },
                    onLongClick = { longPressComic = it },
                    loadingMore = state.loadingMore,
                    endReached = state.endReached,
                    onLoadMore = vm::loadMore,
                    modifier = Modifier.fillMaxSize()
                )
                longPressComic?.let { comic ->
                    ComicContextMenu(
                        comic = comic,
                        isFavorite = comic.id in state.favoriteIds,
                        onDismiss = { longPressComic = null },
                        onToggleFavorite = { vm.toggleFavorite(comic) },
                        onBlock = { vm.blockComic(comic); longPressComic = null },
                        onBlockAuthor = { vm.blockAuthor(comic.author); longPressComic = null },
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
