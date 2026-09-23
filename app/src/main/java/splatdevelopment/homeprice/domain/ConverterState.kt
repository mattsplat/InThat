package splatdevelopment.homeprice.domain

import androidx.compose.ui.geometry.Rect
import kotlin.math.abs

data class DetectedPrice(
    val value: Double,
    val bounds: Rect, // In camera preview pixels
    val timestamp: Long = System.currentTimeMillis(),
    val id: String = "${value}_${(bounds.center.x / 50).toInt()}_${(bounds.center.y / 50).toInt()}" // Group by value and approximate location
)

enum class ScanMode {
    // Show a conversion on every detected price
    Auto,
    // Highlight detected prices and convert only the one the user taps
    Manual,
}

data class ConverterState(
    val fromCurrency: String = "USD",
    val toCurrency: String = "EUR",
    val amountInput: String = "1",
    val rateAB: Double? = null,
    val ratesMap: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val updatedAt: String? = null,
    val detectedPrices: Map<String, DetectedPrice> = emptyMap(),
    // Set when the user taps a detected price; detections stay frozen until cleared
    val selectedPriceId: String? = null,
    val scanMode: ScanMode = ScanMode.Manual,
) {
    val amount: Double?
        get() = amountInput.toDoubleOrNull()

    val rateBA: Double?
        get() = rateAB?.takeIf { abs(it) > 0.0 }?.let { 1 / it }

    val convertedAmount: Double?
        get() = amount?.let { value -> rateAB?.let { value * it } }
}
