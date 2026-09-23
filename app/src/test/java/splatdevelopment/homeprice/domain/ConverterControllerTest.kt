package splatdevelopment.homeprice.domain

import androidx.compose.ui.geometry.Rect
import splatdevelopment.homeprice.network.ExchangeRateService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConverterControllerTest {

    private var now = 1_000L
    private val controller = ConverterController(
        ExchangeRateService(HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) })),
        clock = { now },
    )

    private val price = DetectedPrice(3.79, Rect(100f, 100f, 200f, 150f))
    private val otherPrice = DetectedPrice(6.00, Rect(100f, 300f, 300f, 400f))

    @Test
    fun `selecting a price freezes detections`() {
        controller.updateDetectedPrices(listOf(price))
        controller.selectPrice(price.id)
        controller.updateDetectedPrices(listOf(otherPrice))

        val state = controller.state.value
        assertEquals(price.id, state.selectedPriceId)
        assertEquals(setOf(price.id), state.detectedPrices.keys)
    }

    @Test
    fun `resuming scanning clears selection and accepts new detections`() {
        controller.updateDetectedPrices(listOf(price))
        controller.selectPrice(price.id)
        controller.resumeScanning()
        controller.updateDetectedPrices(listOf(otherPrice))

        val state = controller.state.value
        assertNull(state.selectedPriceId)
        assertEquals(setOf(otherPrice.id), state.detectedPrices.keys)
    }

    @Test
    fun `switching scan mode clears the selection so detections resume`() {
        controller.updateDetectedPrices(listOf(price))
        controller.selectPrice(price.id)
        controller.setScanMode(ScanMode.Auto)
        controller.updateDetectedPrices(listOf(otherPrice))

        val state = controller.state.value
        assertEquals(ScanMode.Auto, state.scanMode)
        assertNull(state.selectedPriceId)
        assertEquals(setOf(otherPrice.id), state.detectedPrices.keys)
    }

    @Test
    fun `a detection missing from one frame survives, but not from two`() {
        controller.updateDetectedPrices(listOf(price, otherPrice))
        now += 300
        controller.updateDetectedPrices(listOf(otherPrice))
        assertEquals(setOf(price.id, otherPrice.id), controller.state.value.detectedPrices.keys)

        now += 300
        controller.updateDetectedPrices(listOf(otherPrice))
        assertEquals(setOf(otherPrice.id), controller.state.value.detectedPrices.keys)
    }
}
