package ru.hdlabs.apibox.data

import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

data class HttpResult(
    val code: Int,
    val body: String,
    val message: String?,
)

class NetClient {
    fun request(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
    ): HttpResult {
        var current = url.trim()
        var currentMethod = method.uppercase()
        var currentBody = body
        repeat(MAX_REDIRECTS) {
            if (!current.startsWith("https://")) {
                return HttpResult(0, "", "Нужен адрес https://")
            }
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 8_000
                readTimeout = 12_000
                requestMethod = currentMethod
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ApiBox")
            }
            headers.forEach { (name, value) ->
                connection.setRequestProperty(name, value)
            }
            try {
                if (currentMethod == "POST") {
                    connection.doOutput = true
                    if (connection.getRequestProperty("Content-Type") == null) {
                        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    }
                    connection.outputStream.use { stream ->
                        stream.write(currentBody.orEmpty().toByteArray(Charsets.UTF_8))
                    }
                }
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (location.isNullOrBlank()) {
                        return HttpResult(code, "", "Сервер не сообщил новый адрес")
                    }
                    val next = if (location.startsWith("https://")) {
                        location
                    } else if (location.startsWith("http://")) {
                        return HttpResult(code, "", "Перенаправление отклонено")
                    } else {
                        URL(URL(current), location).toString()
                    }
                    val fromHost = URL(current).host
                    val toHost = URL(next).host
                    if (!next.startsWith("https://") || !fromHost.equals(toHost, ignoreCase = true)) {
                        return HttpResult(code, "", "Перенаправление на другой адрес отклонено")
                    }
                    current = next
                    if (code == 303 || code == 302) {
                        currentMethod = "GET"
                        currentBody = null
                    }
                    return@repeat
                }
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                connection.disconnect()
                return HttpResult(code, text.take(MAX_BODY), null)
            } catch (error: Exception) {
                connection.disconnect()
                return HttpResult(0, "", error.userMessage())
            }
        }
        return HttpResult(0, "", "Слишком много перенаправлений")
    }

    private fun Throwable.userMessage(): String = when (this) {
        is SocketTimeoutException -> "Сервер не ответил"
        is UnknownHostException -> "Нет сети"
        is SSLException -> "Не удалось проверить сертификат"
        else -> "Нет сети"
    }

    private companion object {
        const val MAX_REDIRECTS = 3
        const val MAX_BODY = 200_000
    }
}
