package splatdevelopment.homeprice.ui

import splatdevelopment.homeprice.model.Currency
import java.text.NumberFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Formats money the way the user's language writes it: "CA$12.63" in English, "12,63 CA$"
 * in German. Uses the currency's usual decimals ("¥1,180") unless [decimals] is given,
 * e.g. for exchange rates.
 */
fun formatMoney(value: Double, currencyCode: String, locale: Locale = Locale.getDefault(), decimals: Int? = null): String {
    val format = NumberFormat.getCurrencyInstance(locale)
    val currency = runCatching { java.util.Currency.getInstance(currencyCode) }.getOrNull()
        ?: return String.format(locale, "%,.2f %s", value, currencyCode)
    format.currency = currency
    // NumberFormat takes decimals from the locale, not the currency, so set them explicitly
    val digits = decimals ?: currency.defaultFractionDigits.coerceAtLeast(0)
    format.minimumFractionDigits = digits
    format.maximumFractionDigits = digits
    return format.format(value)
}

/** The currency's name in the user's language, falling back to the catalog's English name. */
fun Currency.localizedName(locale: Locale = Locale.getDefault()): String =
    runCatching { java.util.Currency.getInstance(code).getDisplayName(locale) }
        .getOrNull()
        ?.takeIf { it != code }
        ?.replaceFirstChar { it.titlecase(locale) }
        ?: name

/** Turns the API's "Wed, 23 Sep 2026 00:02:31 +0000" into a local date and time, or returns it unchanged. */
fun formatUpdatedAt(rfc1123: String, locale: Locale = Locale.getDefault(), zone: ZoneId = ZoneId.systemDefault()): String =
    runCatching {
        ZonedDateTime.parse(rfc1123, DateTimeFormatter.RFC_1123_DATE_TIME)
            .withZoneSameInstant(zone)
            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale))
    }.getOrDefault(rfc1123)
