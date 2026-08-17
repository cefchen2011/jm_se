package com.comicreader.data

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 图片 CDN 配置：
 * 接口返回的图片域名（*.jmdanjonproxy.xyz 等）在国内网络常不可达，
 * 以下列表为实测可达的图片 CDN（均可直接返回 /media/photos/ 与 /media/albums/ 资源）。
 * 客户端把图片 URL 域名统一重写为当前选中的 CDN。
 */
object ImgCdnConfig {

    /** 可选 CDN 域名（index 对应设置项顺序） */
    val domains: List<String> = listOf(
        "cdn-msp.jmdanjonproxy.vip", // 已验证可达（image/webp 正常返回）
        "cdn-msp2.jmapiproxy3.cc"    // 已验证可达（image/webp 正常返回）
    )

    /** 当前选中 index（由 MainActivity 从 DataStore 同步） */
    val selectedIndex = MutableStateFlow(0)

    fun currentHost(): String =
        domains[selectedIndex.value.coerceIn(0, domains.lastIndex)]
}
