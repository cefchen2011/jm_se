package com.comicreader.ui.reader

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.comicreader.data.JmCrypto
import com.comicreader.ui.components.ErrorBox
import com.comicreader.ui.components.LoadingBox
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ReaderDark = Color(0xFF0F0F0F)

@Composable
fun ReaderScreen(navController: NavHostController) {
    val args = navController.currentBackStackEntry?.arguments ?: return
    val comicId = args.getString("comicId") ?: return
    val chapterId = args.getString("chapterId") ?: return
    val sort = args.getInt("sort")

    val vm: ReaderViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    var controlsVisible by remember { mutableStateOf(true) }

    // 全屏沉浸
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val originalStatusColor = window.statusBarColor
            val originalNavColor = window.navigationBarColor
            val originalLightStatus = controller.isAppearanceLightStatusBars
            val originalLightNav = controller.isAppearanceLightNavigationBars
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.rgb(15, 15, 15)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
                window.statusBarColor = originalStatusColor
                window.navigationBarColor = originalNavColor
                controller.isAppearanceLightStatusBars = originalLightStatus
                controller.isAppearanceLightNavigationBars = originalLightNav
            }
        }
    }

    LaunchedEffect(comicId, chapterId) { vm.load(comicId, chapterId, sort) }

    val chapters = state.chapters
    val idx = chapters.indexOfFirst { it.id == state.currentChapter?.id }
    val canPrev = idx > 0
    val canNext = idx >= 0 && idx < chapters.size - 1

    Box(Modifier.fillMaxSize().background(ReaderDark)) {
        when {
            state.loading -> LoadingBox()
            state.error != null -> ErrorBox(state.error) { vm.load(comicId, chapterId, sort) }
            else -> {
                val images = state.current?.images ?: emptyList()
                val photoId = state.current?.id ?: ""
                val scrambleId = state.current?.scrambleId ?: ""
                val cacheStore = remember { com.comicreader.data.ComicCacheStore(context) }
                val chapterDir = remember(comicId, chapterId) {
                    cacheStore.chapterDirOf(comicId, chapterId)
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { controlsVisible = !controlsVisible }
                        }
                ) {
                    val listState = rememberLazyListState()
                    val currentPage = listState.firstVisibleItemIndex
                    LaunchedEffect(currentPage) { vm.setCurrentPage(currentPage) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            kotlinx.coroutines.delay(3000)
                            vm.saveCurrentPage()
                        }
                    }
                    DisposableEffect(Unit) {
                        onDispose { vm.saveCurrentPage() }
                    }
                    LaunchedEffect(images) {
                        if (images.isNotEmpty()) {
                            val page = vm.getSavedPage(comicId, chapterId)
                            if (page in 0 until images.size) listState.scrollToItem(page)
                        }
                    }
                    LazyColumn(Modifier.fillMaxSize().background(ReaderDark), state = listState) {
                        items(images, key = { it.page }) { img ->
                            val localFile = chapterDir.listFiles { f -> f.isFile }?.firstOrNull {
                                val name = it.nameWithoutExtension
                                name == String.format("%04d", img.page) || name == img.page.toString()
                            }
                            Box {
                                ReaderImage(img.image, photoId, scrambleId, localFile)
                            }
                        }
                    }
                }
            }
        }

        // 顶部 / 底部覆盖层
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.saveCurrentPage(); navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                    Text(
                        text = state.currentChapter?.name ?: state.current?.name ?: "阅读",
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                        .navigationBarsPadding()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.prevNext(-1) }, enabled = canPrev) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "上一章", tint = Color.White)
                    }
                    if (chapters.isNotEmpty() && idx >= 0) {
                        Text(text = "${idx + 1} / ${chapters.size}", color = Color.White)
                    }
                    IconButton(onClick = { vm.prevNext(1) }, enabled = canNext) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "下一章", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderImage(url: String, photoId: String, scrambleId: String, localFile: java.io.File?) {
    val context = LocalContext.current
    val data: Any = localFile ?: url
    val bitmap by produceState<ImageBitmap?>(initialValue = null, data, photoId, scrambleId) {
        value = runCatching {
            val loader = Coil.imageLoader(context)
            val request = ImageRequest.Builder(context).data(data).allowHardware(false).build()
            val result = loader.execute(request)
            val drawable = (result as? SuccessResult)?.drawable ?: error("no drawable")
            val bmp = (drawable as? BitmapDrawable)?.bitmap ?: error("not bitmap")
            val final = withContext(Dispatchers.Default) {
                JmCrypto.unscramble(bmp, photoId, scrambleId, url) ?: bmp
            }
            final.asImageBitmap()
        }.getOrNull()
    }
    val img = bitmap
    if (img != null) {
        Image(bitmap = img, contentDescription = null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
    } else {
        Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}