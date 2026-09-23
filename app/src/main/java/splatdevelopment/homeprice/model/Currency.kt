package splatdevelopment.homeprice.model

data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
)

data class ExchangeRate(
    val from: String,
    val to: String,
    val rate: Double,
    val updatedAt: String?,
)
