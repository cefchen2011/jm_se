package com.comicreader.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.comicreader.data.model.Comic
import com.comicreader.ui.Routes
import com.comicreader.ui.components.AppTopBar
import com.comicreader.ui.components.ComicContextMenu
import com.comicreader.ui.components.ComicGrid
import com.comicreader.ui.components.ErrorBox
import com.comicreader.ui.components.LoadingBox
import com.comicreader.ui.search.SearchBus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val vm: HomeViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    var longPressComic by remember { mutableStateOf<Comic?>(null) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "漫画",
            actions = {
                IconButton(onClick = vm::refresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                }
            }
        )
        when {
            state.loading -> LoadingBox()
            state.error != null && state.comics.isEmpty() -> ErrorBox(state.error, onRetry = vm::refresh)
            else -> Box(Modifier.fillMaxSize()) {
                ComicGrid(
                    comics = state.comics,
                    onClick = { navController.navigate(Routes.detail(it.id)) },
                    onLongClick = { longPressComic = it },
                    loadingMore = state.loadingMore,
                    endReached = state.endReached,
                    onLoadMore = vm::loadMore,
                    modifier = Modifier.fillMaxSize(),
                    header = {
                        if (state.hotTags.isNotEmpty()) {
                            HotTagsRow(state.hotTags) { tag ->
                                SearchBus.pendingQuery.value = tag
                                navController.navigate("search") { launchSingleTop = true }
                            }
                        }
                    }
                )
                longPressComic?.let { comic ->
                    ComicContextMenu(
                        comic = comic,
                        isFavorite = comic.id in state.favoriteIds,
                        isFollowed = comic.author in state.followedAuthorIds,
                        onDismiss = { longPressComic = null },
                        onToggleFavorite = { vm.toggleFavorite(comic) },
                        onBlock = { vm.blockComic(comic); longPressComic = null },
                        onBlockAuthor = { vm.blockAuthor(comic.author); longPressComic = null },
                        onToggleFollow = { vm.toggleFollowAuthor(comic.author); longPressComic = null },
                        onViewAuthor = {
                            longPressComic = null
                            navController.navigate(Routes.author(comic.author)) { launchSingleTop = true }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HotTagsRow(tags: List<String>, onTagClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tags) { tag ->
            SuggestionChip(onClick = { onTagClick(tag) }, label = { Text("#$tag") })
        }
    }
}
