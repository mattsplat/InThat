package com.example.testcurrency.domain

import com.example.testcurrency.network.ExchangeRateService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConverterController(
    private val exchangeRateService: ExchangeRateService = ExchangeRateService(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(ConverterState())
    val state: StateFlow<ConverterState> = _state.asStateFlow()

    init {
        refreshRate()
        startCleanupTimer()
    }

    fun updateAmount(value: String) {
        _state.update { it.copy(amountInput = value) }
    }

    fun updateFromCurrency(value: String) {
        _state.update { it.copy(fromCurrency = value) }
        refreshRate()
    }

    fun updateToCurrency(value: String) {
        _state.update { it.copy(toCurrency = value) }
        refreshRate()
    }

    fun swapCurrencies() {
        _state.update {
            it.copy(
                fromCurrency = it.toCurrency,
                toCurrency = it.fromCurrency,
            )
        }
        refreshRate()
    }

    /**
     * Merges new detections into the state, updating timestamps for existing ones
     * to prevent flickering.
     */
    fun updateDetectedPrices(newPrices: List<DetectedPrice>) {
        _state.update { current ->
            val updatedMap = current.detectedPrices.toMutableMap()
            val now = System.currentTimeMillis()
            
            newPrices.forEach { price ->
                // Overwrite or add the detection with a fresh timestamp
                updatedMap[price.id] = price.copy(timestamp = now)
            }
            
            current.copy(detectedPrices = updatedMap)
        }
    }

    /**
     * Periodically removes detections that haven't been seen for a while (e.g., 1 second).
     */
    private fun startCleanupTimer() {
        scope.launch {
            while (true) {
                delay(500)
                val expiryThreshold = System.currentTimeMillis() - 1000
                _state.update { current ->
                    val filtered = current.detectedPrices.filterValues { it.timestamp > expiryThreshold }
                    if (filtered.size != current.detectedPrices.size) {
                        current.copy(detectedPrices = filtered)
                    } else {
                        current
                    }
                }
            }
        }
    }

    /**
     * Calculates the rate between two currencies using a base-relative rates map.
     * Formula: Rate(A -> B) = Rate(Base -> B) / Rate(Base -> A)
     */
    private fun calculateInferredRate(from: String, to: String, rates: Map<String, Double>): Double? {
        val rateToBaseA = rates[from] ?: return null
        val rateToBaseB = rates[to] ?: return null
        
        if (rateToBaseA == 0.0) return null
        return rateToBaseB / rateToBaseA
    }

    fun refreshRate() {
        val snapshot = _state.value
        val from = snapshot.fromCurrency
        val to = snapshot.toCurrency

        // Step 1: Check if from and to are the same
        if (from == to) {
            _state.update {
                it.copy(
                    rateAB = 1.0,
                    isLoading = false,
                    errorMessage = null,
                    updatedAt = null,
                )
            }
            return
        }

        // Step 2: Immediate offline calculation using current ratesMap
        val cachedRate = calculateInferredRate(from, to, snapshot.ratesMap)
        if (cachedRate != null) {
            _state.update {
                it.copy(
                    rateAB = cachedRate,
                    isLoading = false,
                    errorMessage = null,
                )
            }
        } else {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
        }

        // Step 3: Background network refresh (using USD as consistent base for inference)
        scope.launch {
            runCatching {
                exchangeRateService.fetchLatestRates("USD")
            }.onSuccess { response ->
                val newRates = response.rates
                // Update with USD = 1.0 if not present, to ensure self-consistency
                val finalRates = if (!newRates.containsKey("USD")) newRates + ("USD" to 1.0) else newRates
                
                val newInferredRate = calculateInferredRate(from, to, finalRates)
                
                _state.update {
                    it.copy(
                        ratesMap = finalRates,
                        rateAB = newInferredRate ?: it.rateAB,
                        isLoading = false,
                        errorMessage = null,
                        updatedAt = response.timeLastUpdateUtc,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        // Step 5: Silent failure if we have cached rates, otherwise show error
                        errorMessage = if (it.ratesMap.isEmpty()) (error.message ?: "Unable to load rates") else null,
                    )
                }
            }
        }
    }
}
