package com.comicreader.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

@Composable
fun AuthorScreen(navController: NavHostController) {
    val author = navController.currentBackStackEntry?.arguments?.getString("author")
        ?.let { android.net.Uri.decode(it) }
        ?: return
    val vm: AuthorViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    var longPressComic by remember { mutableStateOf<Comic?>(null) }

    LaunchedEffect(author) { vm.load(author) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = author,
            onBack = { navController.popBackStack() }
        )
        when {
            state.loading -> LoadingBox()
            state.error != null && state.comics.isEmpty() -> ErrorBox(state.error) { vm.load(author) }
            state.comics.isEmpty() -> EmptyBox("暂无作品")
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
