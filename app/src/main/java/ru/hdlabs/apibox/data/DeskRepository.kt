package ru.hdlabs.apibox.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.hdlabs.apibox.data.local.CustomApiDao
import ru.hdlabs.apibox.data.local.DeskDao
import ru.hdlabs.apibox.data.local.DeskItemEntity
import ru.hdlabs.apibox.data.local.QuoteDao
import ru.hdlabs.apibox.data.local.QuoteEntity
import ru.hdlabs.apibox.domain.KIND_CUSTOM
import ru.hdlabs.apibox.domain.KIND_MOEX
import ru.hdlabs.apibox.domain.MoexCatalog
import ru.hdlabs.apibox.domain.MoexQuotes
import ru.hdlabs.apibox.domain.formatPlain
import ru.hdlabs.apibox.domain.formatSigned
import ru.hdlabs.apibox.domain.shortClock
import java.net.URLEncoder
import java.util.UUID

class DeskRepository(
    private val deskDao: DeskDao,
    private val quoteDao: QuoteDao,
    private val customDao: CustomApiDao,
    private val secrets: SecretStore,
    private val net: NetClient,
) {
    fun observe(): Flow<List<DeskItemEntity>> = deskDao.observe()

    fun observeQuotes(): Flow<List<QuoteEntity>> = quoteDao.observe()

    suspend fun ensureSeed() {
        if (deskDao.count() == 0) {
            deskDao.insert(
                DeskItemEntity(
                    id = UUID.randomUUID().toString(),
                    kind = KIND_MOEX,
                    ref = "IMOEX",
                    sortOrder = 0,
                ),
            )
        }
    }

    suspend fun setMoexEnabled(secId: String, enabled: Boolean) {
        if (!MoexCatalog.isSecId(secId)) return
        val existing = deskDao.find(KIND_MOEX, secId)
        if (enabled && existing == null) {
            deskDao.insert(
                DeskItemEntity(
                    id = UUID.randomUUID().toString(),
                    kind = KIND_MOEX,
                    ref = secId,
                    sortOrder = deskDao.maxOrder() + 1,
                ),
            )
        } else if (!enabled && existing != null) {
            deskDao.delete(existing.id)
            quoteDao.delete(existing.id)
        }
    }

    suspend fun setCustomOnDesk(customId: String, enabled: Boolean) {
        val existing = deskDao.find(KIND_CUSTOM, customId)
        if (enabled && existing == null) {
            deskDao.insert(
                DeskItemEntity(
                    id = UUID.randomUUID().toString(),
                    kind = KIND_CUSTOM,
                    ref = customId,
                    sortOrder = deskDao.maxOrder() + 1,
                ),
            )
        } else if (!enabled && existing != null) {
            deskDao.delete(existing.id)
            quoteDao.delete(existing.id)
        }
    }

    suspend fun removeCustom(customId: String) {
        val existing = deskDao.find(KIND_CUSTOM, customId)
        if (existing != null) {
            deskDao.delete(existing.id)
            quoteDao.delete(existing.id)
        }
    }

    suspend fun persistOrder(ids: List<String>) {
        val byId = deskDao.snapshot().associateBy { it.id }
        val updated = ids.mapIndexedNotNull { index, id -> byId[id]?.copy(sortOrder = index) }
        if (updated.isNotEmpty()) deskDao.updateAll(updated)
    }

    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        coroutineScope {
            deskDao.snapshot().map { item ->
                async {
                    when (item.kind) {
                        KIND_MOEX -> refreshMoex(item)
                        KIND_CUSTOM -> refreshCustom(item)
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun refreshCustomById(customId: String): String = withContext(Dispatchers.IO) {
        val item = deskDao.find(KIND_CUSTOM, customId)
        val api = customDao.get(customId) ?: return@withContext "API не найден"
        val outcome = readCustom(api.id, api.url, api.method, api.jsonPath)
        if (item != null) {
            if (outcome.ok) {
                quoteDao.upsert(
                    QuoteEntity(
                        deskItemId = item.id,
                        valueText = outcome.value,
                        deltaText = "",
                        up = null,
                        footer = outcome.footer,
                    ),
                )
            } else {
                rememberFailure(item.id, outcome.footer)
            }
        }
        if (outcome.ok) outcome.value else outcome.footer
    }

    private suspend fun refreshMoex(item: DeskItemEntity) {
        if (!MoexCatalog.isSecId(item.ref)) {
            rememberFailure(item.id, "Неизвестный индекс")
            return
        }
        val secId = URLEncoder.encode(item.ref, Charsets.UTF_8.name())
        val url = "https://iss.moex.com/iss/engines/stock/markets/index/securities/$secId.json" +
            "?iss.meta=off&iss.only=marketdata,securities"
        val result = try {
            net.request(url, "GET", emptyMap(), null)
        } catch (_: Exception) {
            rememberFailure(item.id, "Нет сети")
            return
        }
        if (result.code !in 200..299) {
            rememberFailure(item.id, result.message ?: "Биржа ответила ${result.code}")
            return
        }
        val quote = MoexQuotes.parse(result.body)
        val value = quote?.value
        if (quote == null || value == null) {
            rememberFailure(item.id, "Биржа не вернула значение")
            return
        }
        val parts = buildList {
            if (quote.change != null) add(formatSigned(quote.change, quote.decimals))
            if (quote.changePercent != null) add(formatSigned(quote.changePercent, 2) + "%")
        }
        val direction = quote.changePercent ?: quote.change
        quoteDao.upsert(
            QuoteEntity(
                deskItemId = item.id,
                valueText = formatPlain(value, quote.decimals),
                deltaText = parts.joinToString("  "),
                up = when {
                    direction == null || direction == 0.0 -> null
                    direction > 0 -> true
                    else -> false
                },
                footer = shortClock(quote.time)?.let { "биржа $it" } ?: "обновлено",
            ),
        )
    }

    private suspend fun refreshCustom(item: DeskItemEntity) {
        val api = customDao.get(item.ref)
        if (api == null) {
            rememberFailure(item.id, "API удалён")
            return
        }
        val outcome = readCustom(api.id, api.url, api.method, api.jsonPath)
        if (outcome.ok) {
            quoteDao.upsert(
                QuoteEntity(
                    deskItemId = item.id,
                    valueText = outcome.value,
                    deltaText = "",
                    up = null,
                    footer = outcome.footer,
                ),
            )
        } else {
            rememberFailure(item.id, outcome.footer)
        }
    }

    private fun readCustom(id: String, url: String, method: String, jsonPath: String): CustomRead {
        val headers = decodeHeaders(secrets.get(SecretKeys.headers(id))).toMap()
        val body = secrets.get(SecretKeys.body(id))
        val result = try {
            net.request(url, method, headers, if (method == "POST") body else null)
        } catch (_: Exception) {
            return CustomRead(false, "—", "Нет сети")
        }
        if (!result.message.isNullOrBlank() && result.code == 0) {
            return CustomRead(false, "—", result.message)
        }
        if (result.code !in 200..299) {
            return CustomRead(false, "—", connectionText(result))
        }
        if (jsonPath.isBlank()) {
            val preview = result.body.replace(Regex("\\s+"), " ").trim().take(80)
            return CustomRead(true, preview.ifBlank { "пусто" }, "ответ без пути")
        }
        val value = ru.hdlabs.apibox.domain.JsonPath.read(result.body, jsonPath)
            ?: return CustomRead(false, "—", "По пути ничего нет")
        return CustomRead(true, value.take(80), "свой API")
    }

    private suspend fun rememberFailure(id: String, message: String) {
        val previous = quoteDao.get(id)
        quoteDao.upsert(
            (previous ?: QuoteEntity(id, "—", "", null, "")).copy(footer = message),
        )
    }

    private data class CustomRead(val ok: Boolean, val value: String, val footer: String)
}
