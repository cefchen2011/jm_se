package com.comicreader.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.comicreader.ui.Routes
import com.comicreader.ui.components.AppTopBar
import com.comicreader.ui.components.ComicGrid
import com.comicreader.ui.components.ErrorBox
import com.comicreader.ui.components.LoadingBox

@Composable
fun AuthorScreen(navController: NavHostController) {
    val author = navController.currentBackStackEntry?.arguments?.getString("author")
        ?.let { android.net.Uri.decode(it) }
        ?: return
    val vm: AuthorViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(author) { vm.load(author) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = author,
            onBack = { navController.popBackStack() }
        )
        when {
            state.loading -> LoadingBox()
            state.error != null && state.comics.isEmpty() -> ErrorBox(state.error) { vm.load(author) }
            else -> ComicGrid(
                comics = state.comics,
                onClick = { navController.navigate(Routes.detail(it.id)) },
                loadingMore = state.loadingMore,
                endReached = state.endReached,
                onLoadMore = vm::loadMore,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
