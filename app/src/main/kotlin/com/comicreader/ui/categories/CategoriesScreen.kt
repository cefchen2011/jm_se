package com.comicreader.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(navController: NavHostController) {
    val vm: CategoriesViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    var longPressComic by remember { mutableStateOf<Comic?>(null) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "分类")
        when {
            state.loading -> LoadingBox()
            state.error != null -> ErrorBox(state.error, onRetry = vm::loadCategories)
            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.categories) { cat ->
                        FilterChip(
                            selected = state.selected?.slug == cat.slug,
                            onClick = { vm.selectCategory(cat) },
                            label = { Text(cat.name) }
                        )
                    }
                }
                when {
                    state.loadingComics -> LoadingBox()
                    state.comicsError != null && state.comics.isEmpty() ->
                        ErrorBox(state.comicsError) { state.selected?.let(vm::selectCategory) }
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
