package com.comicreader.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.comicreader.ui.theme.LocalUiStyle
import com.comicreader.ui.theme.UiStyle
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

/** 底部导航数据 */
data class NavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/** 顶部栏：标准（M3）/ MIUI（Miuix）风格自适应 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    dark: Boolean = false
) {
    val backIcon: (@Composable () -> Unit)? = if (onBack != null) {
        { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") } }
    } else null

    when (LocalUiStyle.current) {
        UiStyle.STANDARD -> {
            TopAppBar(
                title = { Text(title) },
                modifier = modifier,
                navigationIcon = { backIcon?.invoke() },
                actions = actions ?: {},
                colors = if (dark) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0F0F0F),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                } else TopAppBarDefaults.topAppBarColors()
            )
        }
        UiStyle.MIUI -> {
            MiuixTopAppBar(
                title = title,
                modifier = modifier,
                color = if (dark) Color(0xFF0F0F0F) else MiuixColors.surface,
                titleColor = if (dark) Color.White else MiuixColors.onSurface,
                navigationIcon = { backIcon?.invoke() },
                actions = actions ?: {}
            )
        }
    }
}

/** 底部导航：标准（M3）/ MIUI（Miuix）风格自适应 */
@Composable
fun AppNavigationBar(
    tabs: List<NavTab>,
    currentRoute: String?,
    onSelect: (String) -> Unit
) {
    when (LocalUiStyle.current) {
        UiStyle.STANDARD -> {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { onSelect(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
        UiStyle.MIUI -> {
            MiuixNavigationBar {
                tabs.forEach { tab ->
                    MiuixNavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { onSelect(tab.route) },
                        icon = tab.icon,
                        label = tab.label
                    )
                }
            }
        }
    }
}

/** Miuix 主题色（AppTopBar 的 MIUI 分支必然处于 MiuixTheme 之内） */
private object MiuixColors {
    val surface: Color
        @Composable get() = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surface
    val onSurface: Color
        @Composable get() = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface
}
