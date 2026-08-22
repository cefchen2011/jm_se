package com.comicreader.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.comicreader.ui.Routes
import com.comicreader.ui.components.AppTopBar
import com.comicreader.ui.components.EmptyBox

@Composable
fun FollowedAuthorsScreen(navController: NavHostController) {
    val vm: FavoritesViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "关注作者", onBack = { navController.popBackStack() })
        if (state.followedAuthors.isEmpty()) {
            EmptyBox("暂无关注的作者")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.followedAuthors) { author ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Routes.author(author)) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = author,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { vm.unfollowAuthor(author) }) {
                            androidx.compose.material3.Text("取消关注")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}