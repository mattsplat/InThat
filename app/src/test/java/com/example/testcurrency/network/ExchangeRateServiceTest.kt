package com.example.testcurrency.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExchangeRateServiceTest {

    @Test
    fun `fetchRate returns correct rate when API call is successful`() = runTest {
        // Given
        val mockEngine = MockEngine { _ ->
            respond(
                content = """
                    {
                        "time_last_update_utc": "Mon, 01 Jan 2024 00:00:01 +0000",
                        "rates": {
                            "EUR": 0.91,
                            "GBP": 0.78
                        }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val service = ExchangeRateService(client)

        // When
        val result = service.fetchRate("USD", "EUR")

        // Then
        assertEquals(0.91, result.rate, 0.0001)
        assertEquals("USD", result.from)
        assertEquals("EUR", result.to)
        assertEquals("Mon, 01 Jan 2024 00:00:01 +0000", result.updatedAt)
    }

    @Test
    fun `fetchRate returns 1_0 when from and to currencies are the same`() = runTest {
        // Given
        val service = ExchangeRateService(HttpClient(MockEngine { respond("") }))

        // When
        val result = service.fetchRate("USD", "USD")

        // Then
        assertEquals(1.0, result.rate, 0.0001)
        assertEquals(null, result.updatedAt)
    }

    @Test(expected = Exception::class)
    fun `fetchRate throws exception when API returns error`() = runTest {
        // Given
        val mockEngine = MockEngine { _ ->
            respond(
                content = "Error",
                status = HttpStatusCode.InternalServerError
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val service = ExchangeRateService(client)

        // When
        service.fetchRate("USD", "EUR")
    }

    @Test
    fun `fetchRate actually fetches data from the real API`() = runTest {
        // Given
        val service = ExchangeRateService() // Uses real OkHttp client by default

        // When
        val result = service.fetchRate("USD", "EUR")

        // Then
        assertTrue("Rate should be positive", result.rate > 0)
        assertEquals("USD", result.from)
        assertEquals("EUR", result.to)
        assertTrue("Updated date should not be null", result.updatedAt != null)
    }
}
