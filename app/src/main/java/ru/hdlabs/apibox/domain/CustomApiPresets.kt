package ru.hdlabs.apibox.domain

enum class PresetCategory(val title: String, val about: String) {
    FINANCE("Финансы", "Курсы ЦБ и криптовалюты"),
    MOSCOW("Москва", "Погода и воздух, ключи не нужны"),
    SPB("Санкт-Петербург", "Погода и воздух, ключи не нужны"),
}

data class CustomApiPreset(
    val id: String,
    val category: PresetCategory,
    val name: String,
    val url: String,
    val jsonPath: String,
    val hint: String,
    val source: String,
    val unit: String,
)

object CustomApiPresets {
    private const val CBR_DAILY = "https://www.cbr-xml-daily.ru/daily_json.js"
    private const val COINGECKO_BTC = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd"

    private const val MOSCOW_LAT = 55.7558
    private const val MOSCOW_LON = 37.6173
    private const val SPB_LAT = 59.9311
    private const val SPB_LON = 30.3609

    private fun weatherUrl(lat: Double, lon: Double): String =
        "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,wind_speed_10m&timezone=Europe%2FMoscow&forecast_days=1"

    private fun aqiUrl(lat: Double, lon: Double): String =
        "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon" +
            "&current=european_aqi&timezone=Europe%2FMoscow"

    val all: List<CustomApiPreset> = listOf(
        CustomApiPreset(
            id = "preset-cbr-usd",
            category = PresetCategory.FINANCE,
            name = "Доллар США",
            url = CBR_DAILY,
            jsonPath = "Valute.USD.Value",
            hint = "Официальный курс ЦБ за 1 USD",
            source = "ЦБ РФ",
            unit = "₽",
        ),
        CustomApiPreset(
            id = "preset-cbr-eur",
            category = PresetCategory.FINANCE,
            name = "Евро",
            url = CBR_DAILY,
            jsonPath = "Valute.EUR.Value",
            hint = "Официальный курс ЦБ за 1 EUR",
            source = "ЦБ РФ",
            unit = "₽",
        ),
        CustomApiPreset(
            id = "preset-cbr-cny",
            category = PresetCategory.FINANCE,
            name = "Юань (CNY)",
            url = CBR_DAILY,
            jsonPath = "Valute.CNY.Value",
            hint = "Курс ЦБ за 1 CNY (номинал в ответе банка)",
            source = "ЦБ РФ",
            unit = "₽",
        ),
        CustomApiPreset(
            id = "preset-btc-usd",
            category = PresetCategory.FINANCE,
            name = "Bitcoin",
            url = COINGECKO_BTC,
            jsonPath = "bitcoin.usd",
            hint = "Spot-цена в USD",
            source = "CoinGecko",
            unit = "$",
        ),
        CustomApiPreset(
            id = "preset-msk-temp",
            category = PresetCategory.MOSCOW,
            name = "Температура",
            url = weatherUrl(MOSCOW_LAT, MOSCOW_LON),
            jsonPath = "current.temperature_2m",
            hint = "Сейчас на улице, Open-Meteo",
            source = "Open-Meteo",
            unit = "°C",
        ),
        CustomApiPreset(
            id = "preset-msk-wind",
            category = PresetCategory.MOSCOW,
            name = "Ветер",
            url = weatherUrl(MOSCOW_LAT, MOSCOW_LON),
            jsonPath = "current.wind_speed_10m",
            hint = "Скорость ветра у земли",
            source = "Open-Meteo",
            unit = "км/ч",
        ),
        CustomApiPreset(
            id = "preset-msk-aqi",
            category = PresetCategory.MOSCOW,
            name = "Качество воздуха",
            url = aqiUrl(MOSCOW_LAT, MOSCOW_LON),
            jsonPath = "current.european_aqi",
            hint = "Индекс EAQI: до 20 — хорошо, выше 100 — плохо",
            source = "Open-Meteo",
            unit = "EAQI",
        ),
        CustomApiPreset(
            id = "preset-spb-temp",
            category = PresetCategory.SPB,
            name = "Температура",
            url = weatherUrl(SPB_LAT, SPB_LON),
            jsonPath = "current.temperature_2m",
            hint = "Сейчас на улице, Open-Meteo",
            source = "Open-Meteo",
            unit = "°C",
        ),
        CustomApiPreset(
            id = "preset-spb-wind",
            category = PresetCategory.SPB,
            name = "Ветер",
            url = weatherUrl(SPB_LAT, SPB_LON),
            jsonPath = "current.wind_speed_10m",
            hint = "Скорость ветра у земли",
            source = "Open-Meteo",
            unit = "км/ч",
        ),
        CustomApiPreset(
            id = "preset-spb-aqi",
            category = PresetCategory.SPB,
            name = "Качество воздуха",
            url = aqiUrl(SPB_LAT, SPB_LON),
            jsonPath = "current.european_aqi",
            hint = "Индекс EAQI: до 20 — хорошо, выше 100 — плохо",
            source = "Open-Meteo",
            unit = "EAQI",
        ),
    )

    private val byId = all.associateBy { it.id }

    fun find(id: String): CustomApiPreset? = byId[id]

    fun byCategory(): List<Pair<PresetCategory, List<CustomApiPreset>>> =
        PresetCategory.entries.mapNotNull { category ->
            val items = all.filter { it.category == category }
            if (items.isEmpty()) null else category to items
        }
}
