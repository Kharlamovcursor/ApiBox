package ru.hdlabs.apibox.domain

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

object JsonPath {
    fun read(json: String, path: String): String? {
        if (path.isBlank()) return null
        val root = try {
            JSONTokener(json).nextValue()
        } catch (_: Exception) {
            return null
        }
        var current: Any? = root
        for (token in tokenize(path)) {
            current = when (token) {
                is Key -> {
                    val obj = current as? JSONObject ?: return null
                    if (!obj.has(token.name) || obj.isNull(token.name)) return null
                    obj.get(token.name)
                }
                is Index -> {
                    val array = current as? JSONArray ?: return null
                    if (token.index !in 0 until array.length() || array.isNull(token.index)) return null
                    array.get(token.index)
                }
            }
        }
        return when (current) {
            null, JSONObject.NULL -> null
            is String -> current
            is Number, is Boolean -> current.toString()
            else -> null
        }
    }

    private fun tokenize(path: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val buffer = StringBuilder()
        fun flush() {
            if (buffer.isEmpty()) return
            val part = buffer.toString()
            buffer.clear()
            val asIndex = part.toIntOrNull()
            if (asIndex != null && asIndex >= 0) tokens += Index(asIndex) else tokens += Key(part)
        }
        var cursor = 0
        val source = path.trim()
        while (cursor < source.length) {
            when (source[cursor]) {
                '.' -> {
                    flush()
                    cursor += 1
                }
                '[' -> {
                    flush()
                    val end = source.indexOf(']', cursor)
                    if (end < 0) return emptyList()
                    val index = source.substring(cursor + 1, end).trim().toIntOrNull() ?: return emptyList()
                    if (index < 0) return emptyList()
                    tokens += Index(index)
                    cursor = end + 1
                }
                else -> {
                    buffer.append(source[cursor])
                    cursor += 1
                }
            }
        }
        flush()
        return tokens
    }

    private sealed interface Token
    private data class Key(val name: String) : Token
    private data class Index(val index: Int) : Token
}

data class MoexQuote(
    val secId: String,
    val name: String,
    val value: Double?,
    val change: Double?,
    val changePercent: Double?,
    val time: String?,
    val decimals: Int,
)

object MoexQuotes {
    fun parse(json: String): MoexQuote? {
        val root = try {
            JSONObject(json)
        } catch (_: Exception) {
            return null
        }
        val market = root.optJSONObject("marketdata") ?: return null
        val row = firstRow(market) ?: return null
        val columns = columnIndex(market)
        val securities = root.optJSONObject("securities")
        val info = securities?.let { firstRow(it) }
        val infoColumns = securities?.let { columnIndex(it) }.orEmpty()
        val secId = textAt(row, columns, "SECID").orEmpty()
        if (secId.isBlank()) return null
        val decimals = info?.let { numberAt(it, infoColumns, "DECIMALS") }?.toInt() ?: 2
        return MoexQuote(
            secId = secId,
            name = info?.let { textAt(it, infoColumns, "SHORTNAME") }.orEmpty().ifBlank { secId },
            value = numberAt(row, columns, "CURRENTVALUE") ?: numberAt(row, columns, "LASTVALUE"),
            change = numberAt(row, columns, "LASTCHANGE"),
            changePercent = numberAt(row, columns, "LASTCHANGEPRC"),
            time = textAt(row, columns, "UPDATETIME"),
            decimals = decimals.coerceIn(0, 6),
        )
    }

    private fun columnIndex(block: JSONObject): Map<String, Int> {
        val columns = block.optJSONArray("columns") ?: return emptyMap()
        return buildMap {
            for (index in 0 until columns.length()) {
                put(columns.optString(index), index)
            }
        }
    }

    private fun firstRow(block: JSONObject): JSONArray? {
        val data = block.optJSONArray("data") ?: return null
        if (data.length() == 0) return null
        return data.optJSONArray(0)
    }

    private fun numberAt(row: JSONArray, columns: Map<String, Int>, name: String): Double? {
        val index = columns[name] ?: return null
        if (index !in 0 until row.length() || row.isNull(index)) return null
        return when (val value = row.get(index)) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun textAt(row: JSONArray, columns: Map<String, Int>, name: String): String? {
        val index = columns[name] ?: return null
        if (index !in 0 until row.length() || row.isNull(index)) return null
        return row.optString(index).takeIf { it.isNotBlank() }
    }
}
