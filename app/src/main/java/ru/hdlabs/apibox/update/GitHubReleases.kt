package ru.hdlabs.apibox.update

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val changelog: String,
    val apkUrl: String,
    val apkFileName: String,
)

object GitHubReleases {
    fun checkLatest(owner: String, repo: String, currentVersionCode: Int): UpdateInfo? {
        if (!SAFE_NAME.matches(owner) || !SAFE_NAME.matches(repo)) {
            throw IllegalStateException("Репозиторий обновлений указан неверно")
        }
        val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "ApiBox")
        }
        try {
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (code == 404) throw IllegalStateException("Релиз ещё не опубликован")
            if (code !in 200..299) throw IllegalStateException("GitHub ответил: HTTP $code")
            return parseLatest(text, currentVersionCode)
        } finally {
            connection.disconnect()
        }
    }

    fun parseLatest(json: String, currentVersionCode: Int): UpdateInfo? {
        val root = JSONObject(json)
        val tag = root.optString("tag_name")
        val remoteCode = parseVersionCode(tag)
            ?: throw IllegalStateException("Не удалось прочитать номер версии")
        if (remoteCode <= currentVersionCode) return null
        val assets = root.optJSONArray("assets")
            ?: throw IllegalStateException("В последнем релизе нет APK")
        var chosenName = ""
        var chosenUrl = ""
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name")
            val url = asset.optString("browser_download_url")
            if (!name.endsWith(".apk", ignoreCase = true)) continue
            if (!isTrustedUpdateUrl(url)) continue
            if (chosenUrl.isEmpty() || name.equals("app-release.apk", ignoreCase = true)) {
                chosenName = name
                chosenUrl = url
            }
            if (name.equals("app-release.apk", ignoreCase = true)) break
        }
        if (chosenUrl.isEmpty()) throw IllegalStateException("В последнем релизе нет APK")
        val releaseName = root.optString("name").trim()
        return UpdateInfo(
            versionName = releaseName.ifBlank { tag.trim().removePrefix("v").removePrefix("V") },
            versionCode = remoteCode,
            changelog = root.optString("body").trim(),
            apkUrl = chosenUrl,
            apkFileName = safeFileName(chosenName),
        )
    }

    fun parseVersionCode(tag: String): Int? {
        val stripped = tag.trim().removePrefix("v").removePrefix("V")
        return stripped.toIntOrNull()?.takeIf { it > 0 }
    }

    fun isTrustedUpdateUrl(url: String): Boolean {
        return try {
            val parsed = URL(url)
            val host = parsed.host.lowercase()
            parsed.protocol.equals("https", ignoreCase = true) &&
                (host == "github.com" || host.endsWith(".githubusercontent.com"))
        } catch (_: Exception) {
            false
        }
    }

    fun safeFileName(name: String): String {
        val base = name.substringAfterLast('/').substringAfterLast('\\')
        val clean = base.filter { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }
        return clean.ifBlank { "app-release.apk" }.take(80)
    }

    fun downloadApk(url: String, target: File) {
        var current = url
        repeat(MAX_REDIRECTS) {
            if (!isTrustedUpdateUrl(current)) {
                throw IllegalStateException("Перенаправление обновления отклонено")
            }
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 20_000
                readTimeout = 120_000
                setRequestProperty("User-Agent", "ApiBox")
                setRequestProperty("Accept", "application/vnd.android.package-archive")
            }
            try {
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IllegalStateException("Сервер не сообщил адрес файла")
                    current = URL(URL(current), location).toString()
                    return@repeat
                }
                if (code !in 200..299) throw IllegalStateException("Не удалось скачать APK: HTTP $code")
                target.outputStream().use { output ->
                    connection.inputStream.use { input -> input.copyTo(output) }
                }
                verifyApk(target)
                return
            } catch (error: Exception) {
                if (target.exists()) target.delete()
                throw error
            } finally {
                connection.disconnect()
            }
        }
        throw IllegalStateException("Слишком много перенаправлений")
    }

    fun verifyApk(file: File) {
        if (!file.exists() || file.length() <= 10_000) {
            throw IllegalStateException("Загруженный файл повреждён или пуст")
        }
        val header = ByteArray(4)
        val read = file.inputStream().use { it.read(header) }
        val zip = read == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
        if (!zip) {
            file.delete()
            throw IllegalStateException("Файл не является APK")
        }
    }

    private val SAFE_NAME = Regex("^[A-Za-z0-9_.-]{1,80}$")

    private const val MAX_REDIRECTS = 4
}
