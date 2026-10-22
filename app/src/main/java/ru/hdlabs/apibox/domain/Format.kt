package ru.hdlabs.apibox.domain

import java.text.NumberFormat
import java.util.Locale

fun formatPlain(value: Double, decimals: Int): String = numberFormat(decimals).format(value).stableSpaces()

fun formatSigned(value: Double, decimals: Int): String {
    val body = numberFormat(decimals).format(kotlin.math.abs(value)).stableSpaces()
    return when {
        value > 0 -> "+$body"
        value < 0 -> "−$body"
        else -> body
    }
}

private fun numberFormat(decimals: Int): NumberFormat =
    NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
        minimumFractionDigits = decimals.coerceIn(0, 6)
        maximumFractionDigits = decimals.coerceIn(0, 6)
        isGroupingUsed = true
    }

private fun String.stableSpaces(): String = replace('\u00a0', ' ').replace('\u202f', ' ')

fun <T> List<T>.swapped(from: Int, to: Int): List<T> {
    if (from == to || from !in indices || to !in indices) return this
    val copy = toMutableList()
    val item = copy[from]
    copy[from] = copy[to]
    copy[to] = item
    return copy
}

fun cleanHeader(name: String, value: String): Pair<String, String>? {
    val cleanName = name.trim()
    val cleanValue = value.trim()
    if (cleanName.isEmpty() || cleanValue.isEmpty()) return null
    if (cleanName.any { it == '\r' || it == '\n' || it == ':' }) return null
    if (cleanValue.any { it == '\r' || it == '\n' }) return null
    return cleanName to cleanValue
}

fun shortClock(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.length < 5) return null
    return value.substring(0, 5)
}
