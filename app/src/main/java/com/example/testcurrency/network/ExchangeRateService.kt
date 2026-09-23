package com.example.testcurrency.network

import com.example.testcurrency.model.ExchangeRate
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ExchangeRateService(
    private val client: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.INFO
        }
    }
) {
    suspend fun fetchLatestRates(base: String): ExchangeRateResponse {
        return client
            .get("https://open.er-api.com/v6/latest/$base")
            .body<ExchangeRateResponse>()
    }

    suspend fun fetchRate(from: String, to: String): ExchangeRate {
        if (from == to) {
            return ExchangeRate(from = from, to = to, rate = 1.0, updatedAt = null)
        }

        val response = fetchLatestRates(from)

        val rate = response.rates[to] ?: error("Missing rate for $to")
        return ExchangeRate(
            from = from,
            to = to,
            rate = rate,
            updatedAt = response.timeLastUpdateUtc,
        )
    }
}

@Serializable
data class ExchangeRateResponse(
    @SerialName("time_last_update_utc")
    val timeLastUpdateUtc: String? = null,
    val rates: Map<String, Double>,
)
