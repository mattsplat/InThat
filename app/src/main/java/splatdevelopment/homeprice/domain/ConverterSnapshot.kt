package splatdevelopment.homeprice.domain

data class ConverterSnapshot(
    val fromCurrency: String,
    val toCurrency: String,
    val amountInput: String,
    val rateAB: Double?,
    val rateBA: Double?,
    val convertedAmount: Double?,
    val isLoading: Boolean,
    val errorMessage: String?,
    val updatedAt: String?,
)

fun ConverterState.toSnapshot(): ConverterSnapshot = ConverterSnapshot(
    fromCurrency = fromCurrency,
    toCurrency = toCurrency,
    amountInput = amountInput,
    rateAB = rateAB,
    rateBA = rateBA,
    convertedAmount = convertedAmount,
    isLoading = isLoading,
    errorMessage = errorMessage,
    updatedAt = updatedAt,
)
