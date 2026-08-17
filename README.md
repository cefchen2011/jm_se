# 😘 jm_se

如题，这是 JMComic 的 **Material Design 3 原生纯净客户端**，基于 Kotlin + Jetpack Compose 从零重构，彻底告别 WebView 壳与广告。

真正的纯净版喵 🐾

**请不要将项目文件用于盈利哦**

---

## ✨ 特性说明

- 🚫 **完全去除广告** — 原生重构，无任何广告组件（banner / 闪屏 / 插屏 / 文字链接）
- 🎨 **Material Design 3** — 动态取色 + 7 种可选主题色，随系统明暗切换
- 🥰 **MIUI 风格一键切换** — 基于 [Miuix](https://compose-miuix-ui.github.io/miuix/zh_CN/) 组件库，顶部栏/底部导航/卡片全量切换
- 🎮 **移除无关板块** — 无游戏/电影/小说/博客等模块，只有漫画，清爽纯净
- 📚 **完整阅读体验** — 首页 / 分类 / 搜索 / 详情 / 阅读器，无限滚动
- ⭐ **收藏 + 历史** — 本地存储，支持搜索、单条删除、相对时间显示、读完自动隐藏
- 🚫 **屏蔽功能** — 长按漫画弹菜单：加入收藏 / 屏蔽 / 查看作者其他作品
- 📥 **离线缓存** — 多 CDN 线路可切换，缓存后可离线阅读
- 💾 **数据备份** — 收藏 / 历史 / 屏蔽一键导出导入（JSON）

---

## 📥 如何使用 / 下载

👉 直接构建 Debug APK 安装（见下方「自己构建」），或等待 Releases 发布。

> 当前接口协议基于 JMComic3 v2.0.30 逆向，域名自动轮换解析，国内网络可直连。

---

## 🛠️ 自己构建

需要 **JDK 21** + **Android SDK**（compileSdk 36，build-tools 36.0.0）。

```bash
git clone https://github.com/cefchen2011/jm_se.git
cd jm_se
./gradlew assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

国内网络构建提示：Gradle 发行版与 Maven 依赖已配置阿里云镜像，SDK 组件（platform-36）可从腾讯镜像手动安装。

---

## 📦 源码说明

本仓库是基于 JMComic3 APK 逆向出的接口协议实现的原生客户端：

- **接口**：动态域名分发（AES-256-ECB 解密）+ 时间戳 Token 鉴权 + 响应解密，全部在 `data/` 层实现
- **图片**：章节图片做切片扰码还原；图片 CDN 多线路可切换（`data/ImgCdnConfig.kt`）
- **架构**：Jetpack Compose（M3）+ Retrofit/OkHttp + Coil + DataStore + ViewModel

> 逆向协议参考：`assets/public/static/js/` 中 Webpack 产物分析（广告链路 adKey、模块、路由等），本项目只实现漫画阅读功能，**不含任何广告代码**。

---

## ⚠️ 免责声明

本项目**仅用于学习和研究目的**，请勿用于盈利或商业分发。请遵守所在地法律法规，尊重版权。

---

## 🥰 相关项目

- [Tom6814/JMComic3-APK-NO-Ads](https://github.com/Tom6814/JMComic3-APK-NO-Ads) — 原版去广告/优化 APK 项目
- [Tom6814/jmcomic-apk-mod-skill](https://github.com/Tom6814/jmcomic-apk-mod-skill) — 逆向修改 AI Skill
- [Sakura-TWT/JMComicX](https://github.com/Sakura-TWT/JMComicX) — 同技术栈第三方客户端参考

---

## 📜 许可

[MIT](LICENSE)
