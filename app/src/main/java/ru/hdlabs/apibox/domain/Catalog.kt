package ru.hdlabs.apibox.domain

const val KIND_MOEX = "MOEX"
const val KIND_CUSTOM = "CUSTOM"

const val PLATFORM_OZON = "OZON"
const val PLATFORM_WB = "WILDBERRIES"
const val PLATFORM_YANDEX = "YANDEX"

fun platformTitle(platform: String): String = when (platform) {
    PLATFORM_OZON -> "Ozon"
    PLATFORM_WB -> "Wildberries"
    PLATFORM_YANDEX -> "Яндекс Маркет"
    else -> platform
}

data class MoexIndex(
    val secId: String,
    val title: String,
    val hint: String,
)

object MoexCatalog {
    val all: List<MoexIndex> = listOf(
        MoexIndex("IMOEX", "Индекс МосБиржи", "Основной рублёвый индекс"),
        MoexIndex("RTSI", "Индекс РТС", "Долларовый индекс"),
        MoexIndex("RGBI", "RGBI", "Индекс гособлигаций"),
        MoexIndex("MOEXBC", "Голубые фишки", "Крупнейшие компании"),
        MoexIndex("MOEXBMI", "Широкий рынок", "Broad Market"),
        MoexIndex("MCFTR", "Полная доходность", "Индекс МосБиржи с дивидендами"),
        MoexIndex("MOEXIT", "Технологии", "Сектор IT"),
        MoexIndex("MOEXFN", "Финансы", "Банки и финансы"),
        MoexIndex("MOEXOG", "Нефть и газ", "Сектор"),
        MoexIndex("MOEXMM", "Металлы", "Металлы и добыча"),
        MoexIndex("MOEXCN", "Потребители", "Потребительский сектор"),
        MoexIndex("MOEXTL", "Телеком", "Телекоммуникации"),
    )

    private val byId = all.associateBy { it.secId }

    fun find(secId: String): MoexIndex? = byId[secId]

    fun isSecId(value: String): Boolean = value.matches(Regex("^[A-Z0-9_]{1,16}$"))
}
