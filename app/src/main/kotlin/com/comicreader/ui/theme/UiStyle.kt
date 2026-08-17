package com.comicreader.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/** 界面风格 */
enum class UiStyle(val displayName: String) {
    STANDARD("标准"),
    MIUI("MIUI 风格")
}

/** 当前界面风格（由 Theme 注入，组件按需调整视觉） */
val LocalUiStyle = staticCompositionLocalOf { UiStyle.STANDARD }

/** MIUI 经典橙色 */
val MiuiOrange = androidx.compose.ui.graphics.Color(0xFFFF6900)
