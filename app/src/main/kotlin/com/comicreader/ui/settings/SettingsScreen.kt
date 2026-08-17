package com.comicreader.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.comicreader.ui.components.AppTopBar
import com.comicreader.ui.theme.ThemeSeeds
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val vm: SettingsViewModel = viewModel()
    val themeColor by vm.themeColor.collectAsStateWithLifecycle()
    val uiStyle by vm.uiStyle.collectAsStateWithLifecycle()
    val imgCdnIndex by vm.imgCdnIndex.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 待导出的 JSON（点击导出时异步生成，SAF 回调里写入）
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    // 待导入的 JSON（读取后弹确认）
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val json = pendingExportJson
        if (uri != null && json != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                hint = "导出成功"
            }.onFailure {
                hint = "导出失败：${it.message}"
            }
        }
        pendingExportJson = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (!text.isNullOrBlank()) {
                pendingImportJson = text
            } else {
                hint = "文件读取失败或为空"
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "设置",
            onBack = { navController.popBackStack() }
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ---------- 主题色 ----------
            Text(
                text = "主题色",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.setThemeColor("system") }
                    .padding(vertical = 6.dp)
            ) {
                RadioButton(
                    selected = themeColor == "system",
                    onClick = { vm.setThemeColor("system") }
                )
                Text("跟随系统动态取色", modifier = Modifier.padding(start = 4.dp))
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeSeeds.forEach { (key, color) ->
                    ThemeSwatch(
                        color = color,
                        selected = themeColor == key,
                        onClick = { vm.setThemeColor(key) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ---------- 界面风格 ----------
            Text(
                text = "界面风格",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.setUiStyle("standard") }
                    .padding(vertical = 6.dp)
            ) {
                RadioButton(
                    selected = uiStyle == "standard",
                    onClick = { vm.setUiStyle("standard") }
                )
                Text("标准（Material 3）", modifier = Modifier.padding(start = 4.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.setUiStyle("miui") }
                    .padding(vertical = 6.dp)
            ) {
                RadioButton(
                    selected = uiStyle == "miui",
                    onClick = { vm.setUiStyle("miui") }
                )
                Text("MIUI 风格（Miuix 组件）", modifier = Modifier.padding(start = 4.dp))
            }
            Text(
                text = "切换后立即生效，顶部栏 / 底部导航 / 卡片等组件会切换为 MIUI 样式",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // ---------- 图片源（CDN） ----------
            Text(
                text = "图片源",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            com.comicreader.data.ImgCdnConfig.domains.forEachIndexed { i, host ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.setImgCdnIndex(i) }
                        .padding(vertical = 6.dp)
                ) {
                    RadioButton(
                        selected = imgCdnIndex == i,
                        onClick = { vm.setImgCdnIndex(i) }
                    )
                    Text(
                        text = "线路 ${i + 1}：$host",
                        modifier = Modifier.padding(start = 4.dp),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = "图片加载与缓存使用所选线路。若图片加载失败，可切换到其他线路后重试。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // ---------- 数据管理 ----------
            Text(
                text = "数据管理",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedButton(
                onClick = {
                    scope.launch {
                        pendingExportJson = runCatching { vm.exportJson() }.getOrNull()
                        if (pendingExportJson != null) {
                            exportLauncher.launch("comicreader-backup.json")
                        } else {
                            hint = "导出失败：数据生成失败"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text("导出数据（收藏 / 历史 / 屏蔽）")
            }
            OutlinedButton(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text("导入数据")
            }
            Text(
                text = "导入会覆盖当前全部收藏、历史与屏蔽记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            // 提示
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // 导入确认
    pendingImportJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingImportJson = null },
            title = { Text("导入数据") },
            text = { Text("确定导入该备份文件吗？当前收藏、历史、屏蔽记录将被覆盖。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.importJson(json)
                    pendingImportJson = null
                    hint = "导入成功"
                }) { Text("导入") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportJson = null }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick)
            .background(color, CircleShape)
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
