package com.comicreader.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络层：
 * 1. 通过 BytePlus CDN 的 newsvr-2025.txt 动态解析 API 域名与图片 CDN 域名
 * 2. 每个请求附加 Tokenparam/Token 鉴权头（时间戳绑定）
 * 3. 响应 data 字段 AES-256-ECB 解密后回写，供 Retrofit/Gson 直接解析
 */
object JmApiClient {

    private const val APP_VERSION = "2.0.30"
    private const val APP_TOKEN = "185Hcomic3PAPP7R"
    private const val REFERER = "https://18comic.vip/"

    private val newsvrUrls = listOf(
        "https://rup4a04-c01.tos-ap-southeast-1.bytepluses.com/newsvr-2025.txt",
        "https://rup4a04-c02.tos-cn-hongkong.bytepluses.com/newsvr-2025.txt"
    )
    private val fallbackServers = listOf(
        "www.cdnhjk.net", "www.cdngwc.cc", "www.cdngwc.net", "www.cdngwc.club", "www.cdnutc.me"
    )
    private val fallbackCdns = listOf(
        "www.cdnhjk.net", "www.cdngwc.cc", "www.cdngwc.net", "www.cdngwc.club"
    )

    @Volatile private var apiHost = "www.cdnhjk.net"
    @Volatile private var cdnHost = "www.cdnhjk.net"
    @Volatile private var hostsResolved = false

    private val gson = Gson()

    private val plainClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val authDecryptInterceptor = Interceptor { chain ->
        val b = System.currentTimeMillis() / 1000
        val original = chain.request()
        val url = original.url.newBuilder()
            .host(apiHost)
            .addQueryParameter("lang", "TW")
            .build()
        val request: Request = original.newBuilder()
            .url(url)
            .header("Tokenparam", "$b,$APP_VERSION")
            .header("Token", JmCrypto.md5Hex("$b$APP_TOKEN"))
            .header("Referer", REFERER)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
            )
            .build()
        val response: Response = chain.proceed(request)
        val body = response.body
        if (body == null) {
            response
        } else {
            val contentType = body.contentType()
            val raw = body.string()
            val transformed = transformBody(raw, b)
            response.newBuilder().body(transformed.toResponseBody(contentType)).build()
        }
    }

    private val apiClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(authDecryptInterceptor)
        .build()

    private val api: JmApi = Retrofit.Builder()
        .baseUrl("https://www.cdnhjk.net/")
        .client(apiClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(JmApi::class.java)

    val repository: JmRepository = JmRepository(api)

    fun cdnHost(): String = cdnHost

    fun coverUrl(id: String, updateAt: String = ""): String {
        val q = if (updateAt.isNotBlank()) "?v=$updateAt" else ""
        return "https://$cdnHost/media/albums/${id}_3x4.jpg$q"
    }

    /**
     * 把图片 URL 的域名重写为当前配置的图片 CDN。
     * 接口返回的图片中转域名（*.jmdanjonproxy.xyz 等）在国内网络常不可达，
     * 而 [ImgCdnConfig.domains] 中为实测可达的图片 CDN（路径可直接访问）。
     */
    fun imageUrl(url: String): String {
        if (url.isBlank()) return url
        return try {
            val u = java.net.URI(url)
            if (u.host == null) url
            else {
                val query = u.rawQuery?.let { "?$it" } ?: ""
                "https://${ImgCdnConfig.currentHost()}${u.rawPath}$query"
            }
        } catch (e: Exception) {
            url
        }
    }

    private fun transformBody(raw: String, b: Long): String {
        val json = try {
            JsonParser.parseString(raw) as? JsonObject
        } catch (e: Exception) {
            null
        } ?: return raw
        val dataEl = json["data"] ?: return raw
        if (!dataEl.isJsonPrimitive || !dataEl.asJsonPrimitive.isString) return raw
        val enc = dataEl.asString
        if (enc.isBlank()) return raw
        val dec = JmCrypto.decryptResponseData(enc, b) ?: return raw
        val out = JsonObject()
        for ((k, v) in json.entrySet()) {
            if (k != "data") out.add(k, v)
        }
        try {
            out.add("data", JsonParser.parseString(dec))
        } catch (e: Exception) {
            out.addProperty("data", dec)
        }
        return gson.toJson(out)
    }

    suspend fun ensureHosts() {
        if (hostsResolved) return
        for (url in newsvrUrls) {
            try {
                val resp = plainClient.newCall(Request.Builder().url(url).build()).execute()
                val text = resp.body?.string() ?: continue
                val clean = text.removePrefix("\uFEFF").trim()
                val dec = JmCrypto.decryptNewsvr(clean) ?: continue
                val obj = JsonParser.parseString(dec) as? JsonObject ?: continue
                val servers = obj["Server"]?.asJsonArray
                    ?.mapNotNull { it.takeIf { e -> !e.isJsonNull }?.asString }
                    ?.filter { it.isNotBlank() }
                val cdns = obj["Setting"]?.asJsonArray
                    ?.mapNotNull { it.takeIf { e -> !e.isJsonNull }?.asString }
                    ?.filter { it.isNotBlank() }
                if (!servers.isNullOrEmpty()) {
                    apiHost = servers.first()
                    cdnHost = (cdns ?: servers).first()
                    hostsResolved = true
                    return
                }
            } catch (e: Exception) {
                // try next mirror
            }
        }
        apiHost = fallbackServers.first()
        cdnHost = fallbackCdns.first()
        hostsResolved = true
    }
}
