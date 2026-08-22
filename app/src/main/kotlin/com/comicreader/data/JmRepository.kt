package com.comicreader.data

import com.comicreader.data.model.Category
import com.comicreader.data.model.Chapter
import com.comicreader.data.model.ChapterImages
import com.comicreader.data.model.Comic
import com.comicreader.data.model.ComicDetail
import com.comicreader.data.model.PageImage
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/** JsonElement -> 字符串（兼容字符串/数字两种类型） */
fun JsonElement?.opt(): String = when {
    this == null || isJsonNull -> ""
    isJsonPrimitive ->
        if (asJsonPrimitive.isString) asJsonPrimitive.asString
        else asJsonPrimitive.asNumber.toString()
    isJsonArray -> asJsonArray.joinToString(", ") { it.opt() }
    else -> toString()
}

class JmRepository(private val api: JmApi) {

    private suspend fun execute(block: suspend () -> JsonObject): JsonObject {
        JmApiClient.ensureHosts()
        val json = block()
        val code = json["code"]?.opt()?.toIntOrNull() ?: -1
        if (code != 200) {
            val msg = json["msg"]?.opt()?.ifBlank { "请求失败 ($code)" } ?: "请求失败 ($code)"
            throw Exception(msg)
        }
        return json
    }

    // ---------- 漫画列表 ----------

    suspend fun latest(page: Int): List<Comic> =
        toComicList(execute { api.latest(page) }["data"])

    suspend fun search(keyword: String, order: String, page: Int): List<Comic> =
        toComicList(execute { api.search(keyword, order, page) }["data"])

    suspend fun categoriesFilter(slug: String, order: String, page: Int): List<Comic> =
        toComicList(execute { api.categoriesFilter(page, order, slug) }["data"])

    suspend fun hotTags(): List<String> {
        val el = execute { api.hotTags() }["data"]
        return when {
            el == null || el.isJsonNull -> emptyList()
            el.isJsonArray -> el.asJsonArray.mapNotNull { it.takeIf { e -> !e.isJsonNull }?.opt() }
            el.isJsonObject -> emptyList()
            else -> listOf(el.opt())
        }
    }

    suspend fun categories(): List<Category> {
        val el = execute { api.categories() }["data"]
        val cats = el?.asJsonObject?.get("categories") ?: el
        return if (cats != null && cats.isJsonArray) {
            cats.asJsonArray.mapNotNull { it as? JsonObject }.map {
                Category(it["slug"].opt(), it["name"].opt().ifBlank { it["title"].opt() })
            }
        } else emptyList()
    }

    suspend fun randomRecommend(): String? {
        val el = execute { api.randomRecommend() }["data"]
        return when {
            el == null || el.isJsonNull -> null
            el.isJsonPrimitive -> el.opt()
            el.isJsonObject -> el.asJsonObject["id"].opt().ifBlank { null }
            el.isJsonArray -> el.asJsonArray.firstOrNull()?.opt()
            else -> null
        }
    }

    // ---------- 详情 / 章节 ----------

    suspend fun detail(id: String): ComicDetail {
        val obj = execute { api.album(id) }["data"]?.asJsonObject ?: throw Exception("数据为空")
        return toDetail(obj)
    }

    suspend fun chapterImages(chapterId: String): ChapterImages {
        val obj = execute { api.comicRead(chapterId) }["data"]?.asJsonObject
            ?: throw Exception("数据为空")
        val images = obj["images"]?.asJsonArray
            ?.mapNotNull { it as? JsonObject }
            ?.map { img ->
                // 图片 URL 域名重写为配置的可达 CDN（接口返回的中转域名在国内不可达）
                PageImage(
                    image = JmApiClient.imageUrl(img["image"].opt()),
                    page = img["page"].opt().toIntOrNull() ?: 0
                )
            }
            ?: emptyList()
        return ChapterImages(
            id = obj["id"].opt(),
            name = obj["name"].opt(),
            scrambleId = obj["scramble_id"].opt(),
            images = images
        )
    }

    // ---------- 解析工具 ----------

    private fun toComicList(el: JsonElement?): List<Comic> {
        if (el == null || el.isJsonNull) return emptyList()
        if (el.isJsonArray) return el.asJsonArray.mapNotNull { it as? JsonObject }.map { toComic(it) }
        if (el.isJsonObject) {
            val obj = el.asJsonObject
            for (key in listOf("list", "content", "data", "comics", "items")) {
                val v = obj[key]
                if (v != null && v.isJsonArray) {
                    return v.asJsonArray.mapNotNull { it as? JsonObject }.map { toComic(it) }
                }
            }
        }
        return emptyList()
    }

    private fun toComic(obj: JsonObject): Comic {
        val id = obj["id"].opt()
        val name = obj["name"].opt().ifBlank { obj["title"].opt() }
        val author = obj["author"].opt()
        val updateAt = obj["update_at"].opt().ifBlank { obj["addtime"].opt() }
        return Comic(
            id = id,
            name = name,
            author = author,
            updateAt = updateAt,
            cover = JmApiClient.coverUrl(id, updateAt)
        )
    }

    private fun toDetail(obj: JsonObject): ComicDetail {
        val id = obj["id"].opt()
        val addtime = obj["addtime"].opt()
        val chapters = obj["series"]?.asJsonArray
            ?.mapNotNull { it as? JsonObject }
            ?.map { Chapter(it["id"].opt(), it["name"].opt(), it["sort"].opt().toIntOrNull() ?: 0) }
            ?: emptyList()
        val tags = toTags(obj["tag_list"] ?: obj["tags"])
        val related = obj["related_list"]?.asJsonArray
            ?.mapNotNull { it as? JsonObject }
            ?.map { toComic(it) }
            ?: emptyList()
        return ComicDetail(
            id = id,
            name = obj["name"].opt().ifBlank { obj["title"].opt() },
            author = obj["author"].opt(),
            description = obj["description"].opt().ifBlank { obj["intro"].opt() },
            cover = JmApiClient.coverUrl(id, addtime),
            tags = tags,
            chapters = chapters,
            related = related
        )
    }

    private fun toTags(el: JsonElement?): List<String> {
        if (el == null || el.isJsonNull) return emptyList()
        if (el.isJsonArray) {
            return el.asJsonArray.mapNotNull { e ->
                when {
                    e.isJsonPrimitive -> e.opt()
                    e.isJsonObject -> e.asJsonObject["name"].opt().ifBlank { e.asJsonObject["tag"].opt() }
                    else -> null
                }
            }.filter { it.isNotBlank() }
        }
        if (el.isJsonPrimitive) return listOf(el.opt())
        return emptyList()
    }
}
