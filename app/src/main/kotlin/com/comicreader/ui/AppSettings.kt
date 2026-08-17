package com.comicreader.ui

import kotlinx.coroutines.flow.MutableStateFlow

/** 全局应用状态（由 MainActivity 从 DataStore 同步） */
object AppSettings {
    /** 主题色 key："system" 或具体色名 */
    val themeColor = MutableStateFlow("system")

    /** 界面风格 key："standard" / "miui" */
    val uiStyle = MutableStateFlow("standard")
}
