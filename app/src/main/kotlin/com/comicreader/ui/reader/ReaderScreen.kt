package com.comicreader.ui.reader

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
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
import com.comicreader.data.ComicCacheStore
import com.comicreader.data.JmCrypto
import com.comicreader.data.model.PageImage
import com.comicreader.ui.components.ErrorBox
import com.comicreader.ui.components.LoadingBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

private val ReaderDark = Color(0xFF0F0F0F)

/** 拼接前单页降采样的最大宽度（px） */
private const val MAX_PAGE_WIDTH = 1080

/** 单段长图的最大高度（px），避免超过 GPU 纹理尺寸上限导致黑屏 */
private const val MAX_SEGMENT_HEIGHT = 4096

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

    // 全屏沉浸：隐藏系统状态栏 + 导航栏，离开阅读页时恢复
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

    val current = state.current
    val images = current?.images ?: emptyList()
    val photoId = current?.id ?: ""
    val scrambleId = current?.scrambleId ?: ""
    val chapterKey = current?.id ?: ""

    // 拼接结果：多段长图（每段高度受限），共享缩放实现同步缩放
    var segments by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var stripWidth by remember { mutableStateOf(1) }
    var stripTotalHeight by remember { mutableStateOf(0) }
    var stitchError by remember { mutableStateOf<String?>(null) }
    var stitchRetry by remember { mutableStateOf(0) }

    LaunchedEffect(chapterKey, stitchRetry) {
        if (images.isEmpty()) return@LaunchedEffect
        segments = emptyList()
        stitchError = null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val cacheStore = ComicCacheStore(context)
                val chapterDir = cacheStore.chapterDirOf(comicId, chapterId)
                buildStrip(context, images, photoId, scrambleId, chapterDir)
            }
        }
        result.fold(
            onSuccess = { strip ->
                if (strip != null && strip.segments.isNotEmpty()) {
                    segments = strip.segments.map { it.asImageBitmap() }
                    stripWidth = strip.width
                    stripTotalHeight = strip.totalHeight
                } else {
                    stitchError = "图片加载失败"
                }
            },
            onFailure = { e -> stitchError = e.message }
        )
    }

    // 缩放 / 平移状态（用稳定的 state，避免 pointerInput 捕获到旧引用；切换章节时复位）
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(chapterKey) {
        scale = 1f
        offset = Offset.Zero
    }

    Box(Modifier.fillMaxSize().background(ReaderDark)) {
        when {
            state.loading -> LoadingBox()
            state.error != null -> ErrorBox(state.error) { vm.load(comicId, chapterId, sort) }
            stitchError != null -> ErrorBox(stitchError) { stitchRetry++ }
            segments.isEmpty() -> LoadingBox()
            else -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .onSizeChanged { containerSize = it }
                        .clipToBounds()
                        .background(ReaderDark)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var pastTouchSlop = false
                                var totalPan = Offset.Zero
                                var totalZoom = 1f
                                val touchSlop = viewConfiguration.touchSlop

                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.any { it.isConsumed }) break
                                    val panChange = event.calculatePan()
                                    val zoomChange = event.calculateZoom()
                                    totalPan += panChange
                                    totalZoom *= zoomChange

                                    if (!pastTouchSlop) {
                                        val multiTouch = event.changes.count { it.pressed } >= 2
                                        if (totalPan.getDistance() > touchSlop ||
                                            (multiTouch && abs(1f - totalZoom) > 0.02f)
                                        ) {
                                            pastTouchSlop = true
                                        }
                                    }

                                    if (pastTouchSlop) {
                                        val centroid = event.calculateCentroid(useCurrent = false)
                                        val oldScale = scale
                                        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                        // 以捏合中心为锚点缩放，再叠加平移，最后钳制在边界内
                                        offset = centroid + panChange - (centroid - offset) * (newScale / oldScale)
                                        scale = newScale
                                        offset = clampOffset(offset, scale, containerSize, stripWidth, stripTotalHeight)
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })

                                // 没有越过阈值 => 是一次点击，切换控制条显隐
                                if (!pastTouchSlop) {
                                    controlsVisible = !controlsVisible
                                }
                            }
                        }
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        var cumulative = 0
                        segments.forEach { seg ->
                            val stripY = containerSize.width.toFloat() / stripWidth * cumulative
                            Image(
                                bitmap = seg,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        transformOrigin = TransformOrigin(0f, 0f)
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offset.x
                                        translationY = stripY * (scale - 1f) + offset.y
                                    }
                            )
                            cumulative += seg.height
                        }
                    }
                }
            }
        }

        // 顶部 / 底部覆盖层：点击屏幕切换显隐
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                // 顶部：返回 + 章节标题
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                    Text(
                        text = state.currentChapter?.name ?: current?.name ?: "阅读",
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                // 底部：上一章 / 下一章 + 章节指示
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
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

/** 偏移钳制：让长图始终铺满视口、不露出空白边缘（左上对齐，初始即顶部） */
private fun clampOffset(
    offset: Offset,
    scale: Float,
    container: IntSize,
    stripWidth: Int,
    stripTotalHeight: Int
): Offset {
    if (container.width <= 0 || container.height <= 0 || stripWidth <= 0 || stripTotalHeight <= 0) {
        return Offset.Zero
    }
    val baseH = container.width.toFloat() / stripWidth * stripTotalHeight
    val minX = (container.width - container.width * scale).coerceAtMost(0f)
    val minY = (container.height - baseH * scale).coerceAtMost(0f)
    return Offset(offset.x.coerceIn(minX, 0f), offset.y.coerceIn(minY, 0f))
}

/** 把整章所有页拼成多段竖向长图，逐页加载后立即回收，控制每段高度与内存 */
private suspend fun buildStrip(
    context: android.content.Context,
    images: List<PageImage>,
    photoId: String,
    scrambleId: String,
    chapterDir: File
): StitchedStrip? {
    if (images.isEmpty()) return null
    val bitmaps = ArrayList<Bitmap>(images.size)
    try {
        for (img in images) {
            val localFile = chapterDir.listFiles { f -> f.isFile }?.firstOrNull {
                it.nameWithoutExtension == String.format("%04d", img.page)
            }
            val bmp = loadPageBitmap(context, img.image, photoId, scrambleId, localFile)
            if (bmp != null) bitmaps.add(bmp)
        }
        if (bitmaps.isEmpty()) return null

        val width = bitmaps.maxOf { it.width }
        val segments = ArrayList<Bitmap>()
        var current = ArrayList<Bitmap>()
        var currentHeight = 0
        for (bmp in bitmaps) {
            if (current.isNotEmpty() && currentHeight + bmp.height > MAX_SEGMENT_HEIGHT) {
                segments.add(mergeSegment(current, width))
                current = ArrayList()
                currentHeight = 0
            }
            current.add(bmp)
            currentHeight += bmp.height
        }
        if (current.isNotEmpty()) segments.add(mergeSegment(current, width))
        bitmaps.forEach { if (!it.isRecycled) it.recycle() }
        return StitchedStrip(segments, width, segments.sumOf { it.height })
    } catch (e: Exception) {
        bitmaps.forEach { if (!it.isRecycled) it.recycle() }
        return null
    }
}

/** 把若干页合并为一段（同一宽度，居中对齐），并回收各页位图 */
private fun mergeSegment(bitmaps: List<Bitmap>, width: Int): Bitmap {
    val totalHeight = bitmaps.sumOf { it.height }
    val segment = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.RGB_565)
    val canvas = Canvas(segment)
    var y = 0
    for (bmp in bitmaps) {
        canvas.drawBitmap(bmp, (width - bmp.width) / 2f, y.toFloat(), null)
        y += bmp.height
        bmp.recycle()
    }
    return segment
}

private data class StitchedStrip(
    val segments: List<Bitmap>,
    val width: Int,
    val totalHeight: Int
)

/** 加载单页图片（优先本地缓存，必要时去扰码），返回 null 表示该页加载失败 */
private suspend fun loadPageBitmap(
    context: android.content.Context,
    url: String,
    photoId: String,
    scrambleId: String,
    localFile: File?
): Bitmap? {
    val data: Any = localFile ?: url
    return runCatching {
        val loader = Coil.imageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(data)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        val drawable = (result as? SuccessResult)?.drawable ?: return null
        val bmp = (drawable as? BitmapDrawable)?.bitmap ?: return null
        var out = withContext(Dispatchers.Default) {
            JmCrypto.unscramble(bmp, photoId, scrambleId, url)
        } ?: bmp
        if (out !== bmp) bmp.recycle()
        // 降采样，同时限制宽高，降低内存峰值、避免单页过高
        if (out.width > MAX_PAGE_WIDTH || out.height > MAX_SEGMENT_HEIGHT) {
            val ratio = minOf(
                MAX_PAGE_WIDTH.toFloat() / out.width,
                MAX_SEGMENT_HEIGHT.toFloat() / out.height,
                1f
            )
            val newW = (out.width * ratio).toInt().coerceAtLeast(1)
            val newH = (out.height * ratio).toInt().coerceAtLeast(1)
            val scaled = withContext(Dispatchers.Default) {
                Bitmap.createScaledBitmap(out, newW, newH, true)
            }
            if (scaled !== out) out.recycle()
            out = scaled
        }
        out
    }.getOrNull()
}
