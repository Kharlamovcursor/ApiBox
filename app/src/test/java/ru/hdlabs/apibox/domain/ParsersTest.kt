package ru.hdlabs.apibox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.hdlabs.apibox.data.connectionText
import ru.hdlabs.apibox.data.HttpResult

class ParsersTest {
    @Test
    fun readsJsonPath() {
        val json = """{"data":{"price":12.5},"items":[{"value":"ok"}]}"""
        assertEquals("12.5", JsonPath.read(json, "data.price"))
        assertEquals("ok", JsonPath.read(json, "items[0].value"))
        assertNull(JsonPath.read(json, "items[3].value"))
        assertNull(JsonPath.read(json, ""))
    }

    @Test
    fun readsPresetRatePaths() {
        assertEquals("81.2345", JsonPath.read(CBR_SNIPPET, "Valute.USD.Value"))
        assertEquals("92.1", JsonPath.read(CBR_SNIPPET, "Valute.EUR.Value"))
        assertEquals("11.5", JsonPath.read(CBR_SNIPPET, "Valute.CNY.Value"))
        assertEquals("81603", JsonPath.read(COINGECKO_BTC, "bitcoin.usd"))
        assertEquals("-2.5", JsonPath.read(WEATHER_SNIPPET, "current.temperature_2m"))
        assertEquals("18", JsonPath.read(AQI_SNIPPET, "current.european_aqi"))
    }

    @Test
    fun parsesImoex() {
        val quote = MoexQuotes.parse(IMOEX)
        requireNotNull(quote)
        assertEquals("IMOEX", quote.secId)
        assertEquals("Индекс МосБиржи", quote.name)
        assertEquals(2438.19, quote.value!!, 0.001)
        assertEquals(-8.68, quote.change!!, 0.001)
        assertEquals(-0.35, quote.changePercent!!, 0.001)
        assertEquals("16:10:19", quote.time)
        assertEquals(2, quote.decimals)
        assertEquals("2 438,19", formatPlain(quote.value!!, quote.decimals))
        assertEquals("−8,68", formatSigned(quote.change!!, quote.decimals))
    }

    @Test
    fun swapsNeighbors() {
        assertEquals(listOf("b", "a", "c"), listOf("a", "b", "c").swapped(0, 1))
        assertEquals(listOf("c", "b", "a"), listOf("a", "b", "c").swapped(0, 2))
    }

    @Test
    fun dropsBrokenHeaders() {
        assertNull(cleanHeader("Auth\nX", "1"))
        assertNull(cleanHeader("Name", "bad\rvalue"))
        assertEquals("Authorization" to "secret", cleanHeader(" Authorization ", " secret "))
    }

    @Test
    fun explainsConnection() {
        assertEquals("Связь есть", connectionText(HttpResult(200, "{}", null)))
        assertEquals("Ключ не принят", connectionText(HttpResult(401, "", null)))
        assertEquals("Нет сети", connectionText(HttpResult(0, "", "Нет сети")))
    }

    private companion object {
        const val CBR_SNIPPET = """{"Valute":{"USD":{"Value":81.2345},"EUR":{"Value":92.1},"CNY":{"Value":11.5}}}"""
        const val COINGECKO_BTC = """{"bitcoin":{"usd":81603}}"""
        const val WEATHER_SNIPPET = """{"current":{"temperature_2m":-2.5,"wind_speed_10m":12.3}}"""
        const val AQI_SNIPPET = """{"current":{"european_aqi":18}}"""

        val IMOEX = """
            {
              "marketdata": {
                "columns": ["SECID","CURRENTVALUE","LASTCHANGE","UPDATETIME","LASTCHANGEPRC"],
                "data": [["IMOEX",2438.19,-8.68,"16:10:19",-0.35]]
              },
              "securities": {
                "columns": ["SECID","SHORTNAME","DECIMALS"],
                "data": [["IMOEX","Индекс МосБиржи",2]]
              }
            }
        """.trimIndent()
    }
}
