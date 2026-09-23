package com.example.testcurrency.domain

import android.graphics.Rect
import kotlin.math.abs

data class DetectedPrice(
    val value: Double,
    val bounds: Rect,
    val timestamp: Long = System.currentTimeMillis(),
    val id: String = "${value}_${bounds.centerX() / 50}_${bounds.centerY() / 50}" // Group by value and approximate location
)

data class ConverterState(
    val fromCurrency: String = "USD",
    val toCurrency: String = "EUR",
    val amountInput: String = "1",
    val rateAB: Double? = null,
    val ratesMap: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val updatedAt: String? = null,
    val detectedPrices: Map<String, DetectedPrice> = emptyMap()
) {
    val amount: Double?
        get() = amountInput.toDoubleOrNull()

    val rateBA: Double?
        get() = rateAB?.takeIf { abs(it) > 0.0 }?.let { 1 / it }

    val convertedAmount: Double?
        get() = amount?.let { value -> rateAB?.let { value * it } }
}
