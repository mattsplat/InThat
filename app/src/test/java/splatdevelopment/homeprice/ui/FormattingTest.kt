package splatdevelopment.homeprice.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import splatdevelopment.homeprice.model.Currency
import java.time.ZoneOffset
import java.util.Locale

class FormattingTest {

    // Java formats some locales with non-breaking spaces; compare with plain spaces
    private fun plain(text: String) = text.replace(' ', ' ').replace(' ', ' ')

    @Test
    fun `money follows the user's language`() {
        assertEquals("CA$12.63", formatMoney(12.634, "CAD", Locale.US))
        assertEquals("12,63 €", plain(formatMoney(12.634, "EUR", Locale.GERMANY)))
        assertEquals("1 299,50 €", plain(formatMoney(1299.5, "EUR", Locale.FRANCE)))
    }

    @Test
    fun `money uses the currency's decimals, not the language's`() {
        assertEquals("¥1,180", formatMoney(1180.0, "JPY", Locale.US))
        // Japanese locale defaults to no decimals; Canadian dollars still need cents
        assertEquals("CA$12.63", formatMoney(12.634, "CAD", Locale.JAPAN))
    }

    @Test
    fun `exchange rates can ask for more decimals`() {
        assertEquals("CA$1.4054", formatMoney(1.40543, "CAD", Locale.US, decimals = 4))
    }

    @Test
    fun `currency names are translated, with the catalog name as fallback`() {
        assertEquals("Kanadischer Dollar", Currency("CAD", "Canadian Dollar", "C$").localizedName(Locale.GERMANY))
        assertEquals("Euro", Currency("EUR", "Euro", "€").localizedName(Locale.FRANCE))
        assertEquals("Made-up Money", Currency("XQQ", "Made-up Money", "?").localizedName(Locale.US))
    }

    @Test
    fun `update time is shown in the local format`() {
        val formatted = formatUpdatedAt("Wed, 23 Sep 2026 00:02:31 +0000", Locale.GERMANY, ZoneOffset.UTC)
        assertEquals("23.09.2026, 00:02", plain(formatted))
        assertEquals("not a date", formatUpdatedAt("not a date"))
    }
}
