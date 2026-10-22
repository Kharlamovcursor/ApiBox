package ru.hdlabs.apibox.data

import org.json.JSONArray
import org.json.JSONObject
import ru.hdlabs.apibox.domain.cleanHeader

fun encodeHeaders(headers: List<Pair<String, String>>): String {
    val array = JSONArray()
    headers.forEach { (name, value) ->
        val clean = cleanHeader(name, value) ?: return@forEach
        array.put(JSONObject().put("name", clean.first).put("value", clean.second))
    }
    return array.toString()
}

fun decodeHeaders(raw: String): List<Pair<String, String>> {
    if (raw.isBlank()) return emptyList()
    return try {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val clean = cleanHeader(item.optString("name"), item.optString("value")) ?: continue
                add(clean)
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun connectionText(result: HttpResult): String {
    if (result.code in 200..299) return "Связь есть"
    if (result.code == 401 || result.code == 403) return "Ключ не принят"
    val hint = jsonMessage(result.body)
    if (hint.isNotBlank()) return hint.take(140)
    if (!result.message.isNullOrBlank()) return result.message
    if (result.code == 0) return "Нет сети"
    return "Ответ сервера ${result.code}"
}

private fun jsonMessage(body: String): String {
    return try {
        val obj = JSONObject(body)
        obj.optString("message").ifBlank { obj.optString("error") }
            .replace(Regex("\\s+"), " ")
            .trim()
    } catch (_: Exception) {
        ""
    }
}
