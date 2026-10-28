package ru.hdlabs.apibox.ui.dashboard

import ru.hdlabs.apibox.data.local.CustomApiEntity
import ru.hdlabs.apibox.data.local.DeskItemEntity
import ru.hdlabs.apibox.data.local.QuoteEntity
import ru.hdlabs.apibox.domain.CustomApiPresets
import ru.hdlabs.apibox.domain.KIND_CUSTOM
import ru.hdlabs.apibox.domain.KIND_MOEX
import ru.hdlabs.apibox.domain.MoexCatalog

fun buildDeskCards(
    items: List<DeskItemEntity>,
    quotes: List<QuoteEntity>,
    customs: List<CustomApiEntity>,
): List<DeskCard> {
    val quoteById = quotes.associateBy { it.deskItemId }
    val customById = customs.associateBy { it.id }
    return items.mapNotNull { item -> item.toDeskCard(quoteById[item.id], customById) }
}

private fun DeskItemEntity.toDeskCard(
    quote: QuoteEntity?,
    customs: Map<String, CustomApiEntity>,
): DeskCard? {
    val title: String
    val subtitle: String
    when (kind) {
        KIND_MOEX -> {
            val index = MoexCatalog.find(ref)
            title = ref
            subtitle = index?.title ?: "Индекс Мосбиржи"
        }
        KIND_CUSTOM -> {
            val api = customs[ref] ?: return null
            title = api.name
            subtitle = CustomApiPresets.find(api.id)?.source ?: "Свой HTTPS"
        }
        else -> return null
    }
    return DeskCard(
        id = id,
        title = title,
        subtitle = subtitle,
        value = quote?.valueText ?: "…",
        delta = quote?.deltaText.orEmpty(),
        up = quote?.up,
        footer = quote?.footer ?: "загрузка",
    )
}

fun DeskCard.statusOk(): Boolean {
    if (value == "…") return false
    if (value == "—") return false
    val lower = footer.lowercase()
    if (lower.contains("ошиб") || lower.contains("нет сети") || lower.contains("не ответил")) return false
    if (lower.contains("не принят") || lower.contains("ничего нет")) return false
    return true
}
