package com.comicreader.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.comicreader.ui.cache.CacheScreen
import com.comicreader.ui.categories.CategoriesScreen
import com.comicreader.ui.components.AppNavigationBar
import com.comicreader.ui.components.NavTab
import com.comicreader.ui.detail.DetailScreen
import com.comicreader.ui.favorites.BlockedScreen
import com.comicreader.ui.favorites.FavoritesScreen
import com.comicreader.ui.favorites.HistoryScreen
import com.comicreader.ui.favorites.MeScreen
import com.comicreader.ui.home.HomeScreen
import com.comicreader.ui.reader.ReaderScreen
import com.comicreader.ui.search.AuthorScreen
import com.comicreader.ui.search.SearchScreen
import com.comicreader.ui.settings.SettingsScreen

private sealed class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : Tab("home", "首页", Icons.Filled.Home)
    data object Categories : Tab("categories", "分类", Icons.Filled.Category)
    data object Search : Tab("search", "搜索", Icons.Filled.Search)
    data object Me : Tab("me", "我的", Icons.Filled.Person)
}

object Routes {
    const val DETAIL = "detail/{comicId}"
    const val READER = "reader/{comicId}/{chapterId}/{sort}"
    const val AUTHOR = "author/{author}"
    const val ME_FAVORITES = "me/favorites"
    const val ME_HISTORY = "me/history"
    const val ME_BLOCKED = "me/blocked"
    const val ME_CACHE = "me/cache"
    const val ME_SETTINGS = "me/settings"

    fun detail(comicId: String) = "detail/$comicId"
    fun reader(comicId: String, chapterId: String, sort: Int) = "reader/$comicId/$chapterId/$sort"
    fun author(author: String) = "author/${android.net.Uri.encode(author)}"
}

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val tabs = listOf(Tab.Home, Tab.Categories, Tab.Search, Tab.Me)
    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppNavigationBar(
                    tabs = tabs.map { NavTab(it.route, it.label, it.icon) },
                    currentRoute = currentRoute,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tab.Home.route) { HomeScreen(navController) }
            composable(Tab.Categories.route) { CategoriesScreen(navController) }
            composable(Tab.Search.route) { SearchScreen(navController) }
            composable(Tab.Me.route) { MeScreen(navController) }
            composable(Routes.ME_FAVORITES) { FavoritesScreen(navController) }
            composable(Routes.ME_HISTORY) { HistoryScreen(navController) }
            composable(Routes.ME_BLOCKED) { BlockedScreen(navController) }
            composable(Routes.ME_CACHE) { CacheScreen(navController) }
            composable(Routes.ME_SETTINGS) { SettingsScreen(navController) }

            composable(
                route = Routes.AUTHOR,
                arguments = listOf(navArgument("author") { type = NavType.StringType })
            ) {
                AuthorScreen(navController)
            }

            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("comicId") { type = NavType.StringType })
            ) {
                DetailScreen(navController)
            }

            composable(
                route = Routes.READER,
                arguments = listOf(
                    navArgument("comicId") { type = NavType.StringType },
                    navArgument("chapterId") { type = NavType.StringType },
                    navArgument("sort") { type = NavType.IntType }
                )
            ) {
                ReaderScreen(navController)
            }
        }
    }
}
