package com.comicreader.ui.reader

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.comicreader.data.JmCrypto
import com.comicreader.ui.components.AppTopBar
import com.comicreader.ui.components.ErrorBox
import com.comicreader.ui.components.LoadingBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ReaderDark = Color(0xFF0F0F0F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(navController: NavHostController) {
    val args = navController.currentBackStackEntry?.arguments
    val comicId = args?.getString("comicId") ?: return
    val chapterId = args?.getString("chapterId") ?: return
    val sort = args?.getInt("sort") ?: 0

    val vm: ReaderViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(comicId, chapterId) { vm.load(comicId, chapterId, sort) }

    val chapters = state.chapters
    val idx = chapters.indexOfFirst { it.id == state.currentChapter?.id }
    val canPrev = idx > 0
    val canNext = idx >= 0 && idx < chapters.size - 1

    Column(Modifier.fillMaxSize().background(ReaderDark)) {
        AppTopBar(
            title = state.currentChapter?.name ?: state.current?.name ?: "阅读",
            dark = true,
            onBack = { navController.popBackStack() },
            actions = {
                IconButton(onClick = { vm.prevNext(-1) }, enabled = canPrev) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "上一章")
                }
                IconButton(onClick = { vm.prevNext(1) }, enabled = canNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "下一章")
                }
            }
        )

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
                LazyColumn(Modifier.fillMaxSize().background(ReaderDark)) {
                    items(images, key = { it.page }) { img ->
                        // 优先读取本地缓存（离线可用）
                        val localFile = chapterDir.listFiles { f -> f.isFile }?.firstOrNull {
                            it.nameWithoutExtension == String.format("%04d", img.page)
                        }
                        ReaderImage(img.image, photoId, scrambleId, localFile)
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
            val request = ImageRequest.Builder(context)
                .data(data)
                .allowHardware(false)
                .build()
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
        Image(
            bitmap = img,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Box(
            modifier = Modifier.fillMaxWidth().height(320.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}
