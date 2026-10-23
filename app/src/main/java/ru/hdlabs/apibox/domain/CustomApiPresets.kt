package ru.hdlabs.apibox.domain

data class CustomApiPreset(
    val id: String,
    val name: String,
    val url: String,
    val jsonPath: String,
    val hint: String,
)

object CustomApiPresets {
    private const val CBR_DAILY = "https://www.cbr-xml-daily.ru/daily_json.js"
    private const val COINGECKO_BTC = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd"

    val all: List<CustomApiPreset> = listOf(
        CustomApiPreset(
            id = "preset-cbr-usd",
            name = "Доллар США (ЦБ РФ)",
            url = CBR_DAILY,
            jsonPath = "Valute.USD.Value",
            hint = "Курс за 1 USD, обновление ЦБ",
        ),
        CustomApiPreset(
            id = "preset-cbr-eur",
            name = "Евро (ЦБ РФ)",
            url = CBR_DAILY,
            jsonPath = "Valute.EUR.Value",
            hint = "Курс за 1 EUR, обновление ЦБ",
        ),
        CustomApiPreset(
            id = "preset-btc-usd",
            name = "Bitcoin / USD",
            url = COINGECKO_BTC,
            jsonPath = "bitcoin.usd",
            hint = "CoinGecko, цена в долларах",
        ),
    )

    private val byId = all.associateBy { it.id }

    fun find(id: String): CustomApiPreset? = byId[id]
}
