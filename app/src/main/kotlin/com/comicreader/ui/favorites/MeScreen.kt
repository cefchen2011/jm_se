package com.comicreader.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.comicreader.ui.Routes
import com.comicreader.ui.components.AppTopBar

@Composable
fun MeScreen(navController: NavHostController) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "我的",
            actions = {
                IconButton(onClick = { navController.navigate(Routes.ME_SETTINGS) }) {
                    Icon(Icons.Filled.Settings, contentDescription = "设置")
                }
            }
        )
        MeItem(
            icon = Icons.Filled.Favorite,
            label = "收藏",
            onClick = { navController.navigate(Routes.ME_FAVORITES) }
        )
        HorizontalDivider()
        MeItem(
            icon = Icons.Filled.History,
            label = "历史",
            onClick = { navController.navigate(Routes.ME_HISTORY) }
        )
        HorizontalDivider()
        MeItem(
            icon = Icons.Filled.Download,
            label = "缓存",
            onClick = { navController.navigate(Routes.ME_CACHE) }
        )
        HorizontalDivider()
        MeItem(
            icon = Icons.Filled.Star,
            label = "关注作者",
            onClick = { navController.navigate(Routes.ME_FOLLOWED) }
        )
        HorizontalDivider()
        MeItem(
            icon = Icons.Filled.Block,
            label = "屏蔽",
            onClick = { navController.navigate(Routes.ME_BLOCKED) }
        )
    }
}

@Composable
private fun MeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp).weight(1f)
        )
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
