package com.comicreader.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * JMComic (18comic.vip) 接口的加密工具：
 * - MD5 摘要
 * - AES-256-ECB 解密（域名分发文件 + 响应 data 字段）
 * - 阅读器逐图横向切片去扰码
 */
object JmCrypto {

    fun md5Hex(input: String): String =
        MessageDigest.getInstance("MD5")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    fun aes256EcbDecrypt(b64: String, keyBytes: ByteArray): String? = try {
        val cipher = Cipher.getInstance("AES/ECB/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"))
        val plain = cipher.doFinal(Base64.decode(b64.trim(), Base64.DEFAULT))
        String(plain, Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }

    /** newsvr-2025.txt 的密钥：utf8(md5("diosfjckwpqpdfjkvnqQjsik"))，32 字节 => AES-256 */
    private val newsvrKey: ByteArray by lazy {
        md5Hex("diosfjckwpqpdfjkvnqQjsik").toByteArray(Charsets.UTF_8)
    }

    fun decryptNewsvr(b64: String): String? = aes256EcbDecrypt(b64, newsvrKey)

    /** 响应 data 字段密钥：utf8(md5(ts + key))，逐次尝试两个 key */
    private val dataKeys = listOf("185Hcomic3PAPP7R", "18comicAPPContent")

    fun decryptResponseData(b64: String, tsSeconds: Long): String? {
        for (k in dataKeys) {
            val key = md5Hex("$tsSeconds$k").toByteArray(Charsets.UTF_8)
            val plain = aes256EcbDecrypt(b64, key)
            if (plain != null) return plain
        }
        return null
    }

    private fun webpFilename(url: String): String? =
        Regex("/([^/]+)\\.webp").find(url)?.groupValues?.getOrNull(1)

    /** 计算切片数：md5(photo_id + 文件名) 末位字符 charCode 取模后映射到 2..20 */
    private fun stripCount(photoId: String, filename: String): Int {
        var r = md5Hex(photoId + filename).last().code
        val pid = photoId.toLongOrNull() ?: 0L
        when {
            pid in 268850L..421925L -> r %= 10
            pid >= 421926L -> r %= 8
        }
        return when (r) {
            0 -> 2; 1 -> 4; 2 -> 6; 3 -> 8; 4 -> 10
            5 -> 12; 6 -> 14; 7 -> 16; 8 -> 18; 9 -> 20
            else -> 10
        }
    }

    /**
     * 还原扰码图片。返回 null 表示无需还原（gif / photo_id < scramble_id）。
     * 算法与官方 Web 端一致：把源图按 n 条横向切片自下而上重排回正确顺序。
     */
    fun unscramble(bitmap: Bitmap, photoId: String, scrambleId: String, imageUrl: String): Bitmap? {
        if (imageUrl.contains(".gif", ignoreCase = true)) return null
        val pid = photoId.toLongOrNull() ?: return null
        val sid = scrambleId.toLongOrNull() ?: return null
        if (pid < sid) return null
        val filename = webpFilename(imageUrl) ?: return null
        val n = stripCount(photoId, filename)
        if (n <= 1) return null

        val w = bitmap.width
        val h = bitmap.height
        val u = h / n
        if (u == 0) return null
        val r = h % n

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val src = Rect()
        val dst = Rect()
        for (c in 0 until n) {
            var stripH = u
            var dstY = u * c
            val srcY = h - u * (c + 1) - r
            if (c == 0) stripH = u + r else dstY += r
            if (srcY < 0 || stripH <= 0) continue
            src.set(0, srcY, w, srcY + stripH)
            dst.set(0, dstY, w, dstY + stripH)
            canvas.drawBitmap(bitmap, src, dst, null)
        }
        return out
    }
}
