package com.comicreader.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.comicreader.data.SettingsStore
import com.comicreader.ui.theme.ComicReaderTheme
import com.comicreader.ui.theme.LocalUiStyle
import com.comicreader.ui.theme.UiStyle
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = SettingsStore(applicationContext)
        lifecycleScope.launch {
            settings.themeColorFlow().collect { AppSettings.themeColor.value = it }
        }
        lifecycleScope.launch {
            settings.uiStyleFlow().collect { AppSettings.uiStyle.value = it }
        }
        lifecycleScope.launch {
            settings.imgCdnIndexFlow().collect { com.comicreader.data.ImgCdnConfig.selectedIndex.value = it }
        }
        setContent {
            val themeColor by AppSettings.themeColor.collectAsState()
            val uiStyleName by AppSettings.uiStyle.collectAsState()
            val uiStyle = if (uiStyleName == "miui") UiStyle.MIUI else UiStyle.STANDARD
            ComicReaderTheme(themeColor = themeColor) {
                CompositionLocalProvider(LocalUiStyle provides uiStyle) {
                    if (uiStyle == UiStyle.MIUI) {
                        MiuixTheme { AppRoot() }
                    } else {
                        AppRoot()
                    }
                }
            }
        }
    }
}
