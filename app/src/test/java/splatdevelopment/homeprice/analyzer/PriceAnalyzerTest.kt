package splatdevelopment.homeprice.analyzer

import androidx.compose.ui.geometry.Rect
import splatdevelopment.homeprice.domain.DetectedPrice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceAnalyzerTest {

    @Test
    fun `detects price without currency symbol`() {
        assertEquals(listOf(12.99), PriceAnalyzer.extractPrices("12.99"))
        assertEquals(listOf(450.0), PriceAnalyzer.extractPrices("Price 450"))
    }

    @Test
    fun `still detects price with symbol before or after`() {
        assertEquals(listOf(5000.0), PriceAnalyzer.extractPrices("¥5, 000"))
        assertEquals(listOf(12.99), PriceAnalyzer.extractPrices("12.99 €"))
        assertEquals(listOf(3.5), PriceAnalyzer.extractPrices("$3,50"))
    }

    @Test
    fun `ignores barcode and sku numbers`() {
        assertEquals(emptyList<Double>(), PriceAnalyzer.extractPrices("0001111091046"))
        assertEquals(emptyList<Double>(), PriceAnalyzer.extractPrices("4901234567894"))
    }

    @Test
    fun `ignores sizes and unit prices`() {
        assertEquals(emptyList<Double>(), PriceAnalyzer.extractPrices("10 OZ"))
        assertEquals(emptyList<Double>(), PriceAnalyzer.extractPrices("37.9¢ PER OUNCE"))
        assertEquals(emptyList<Double>(), PriceAnalyzer.extractPrices("2.0L"))
        assertEquals(emptyList<Double>(), PriceAnalyzer.extractPrices("500 ml"))
    }

    @Test
    fun `ignores dates`() {
        assertEquals(emptyList<Double>(), PriceAnalyzer.extractPrices("11-26-24"))
        assertEquals(emptyList<Double>(), PriceAnalyzer.extractPrices("11/26/2024"))
    }

    @Test
    fun `ignores lone digits unless marked with a symbol`() {
        assertEquals(emptyList<Double>(), PriceAnalyzer.extractPrices("2 1"))
        assertEquals(listOf(5.0), PriceAnalyzer.extractPrices("$5"))
    }

    @Test
    fun `keeps real prices from a US sale tag`() {
        assertEquals(listOf(3.79), PriceAnalyzer.extractPrices("KRO LEAFY ROMAINE 37.9¢ PERÖUNCE 3.79"))
        assertEquals(listOf(6.0), PriceAnalyzer.extractPrices("2/6.00"))
        assertEquals(listOf(3.0), PriceAnalyzer.extractPrices("3.00 each"))
    }

    @Test
    fun `keeps both yen prices from a Japanese tag`() {
        assertEquals(listOf(498.0, 538.0), PriceAnalyzer.extractPrices("498 (538)"))
    }

    @Test
    fun `treats a space before two digits as a missed decimal point`() {
        assertEquals(listOf(2.99), PriceAnalyzer.extractPrices("2 99"))
        assertEquals(listOf(14.95), PriceAnalyzer.extractPrices("14 95€"))
    }

    @Test
    fun `still treats a space before three digits as a thousands separator`() {
        assertEquals(listOf(5000.0), PriceAnalyzer.extractPrices("5 000"))
    }

    @Test
    fun `detects hidden decimal from a wide gap before the cents`() {
        // "2 . 9 9" with the dot missing: 40px digits, 8px normal gaps, 30px gap where the dot was
        val boxes = listOf(digit(0f), digit(70f), digit(118f))
        assertTrue(PriceAnalyzer.hasHiddenDecimalBeforeCents(boxes))
    }

    @Test
    fun `detects hidden decimal from superscript cents`() {
        val boxes = listOf(digit(0f), digit(48f, height = 40f), digit(96f, height = 40f))
        assertTrue(PriceAnalyzer.hasHiddenDecimalBeforeCents(boxes))
    }

    @Test
    fun `evenly spaced digits have no hidden decimal`() {
        val boxes = listOf(digit(0f), digit(48f), digit(96f))
        assertFalse(PriceAnalyzer.hasHiddenDecimalBeforeCents(boxes))
    }

    private fun digit(left: Float, height: Float = 80f) = Rect(left, 80f - height, left + 40f, 80f)

    private val aimBox = Rect(0f, 0f, 1000f, 500f)
    private val tag = Rect(100f, 100f, 400f, 300f)
    private val headline = DetectedPrice(8.99, Rect(200f, 200f, 320f, 260f)) // tall, inside the tag
    private val barcodeDigits = DetectedPrice(62017.0, Rect(150f, 120f, 210f, 135f)) // short, inside the tag
    private val outsideTag = DetectedPrice(100.0, Rect(600f, 100f, 660f, 140f)) // e.g. "100 TABLETS" on the box
    private val outsideAimBox = DetectedPrice(11.99, Rect(200f, 700f, 300f, 760f))
    private val all = listOf(headline, barcodeDigits, outsideTag, outsideAimBox)

    @Test
    fun `only prices inside detected tags count when tags are found`() {
        val selected = PriceAnalyzer.selectPrices(all, aimBox, listOf(tag), largestPerTag = false)
        assertEquals(listOf(headline, barcodeDigits), selected)
    }

    @Test
    fun `falls back to the aim box when no tags are found`() {
        val selected = PriceAnalyzer.selectPrices(all, aimBox, emptyList(), largestPerTag = false)
        assertEquals(listOf(headline, barcodeDigits, outsideTag), selected)
    }

    @Test
    fun `largest per tag keeps just the headline price`() {
        val selected = PriceAnalyzer.selectPrices(all, aimBox, listOf(tag), largestPerTag = true)
        assertEquals(listOf(headline), selected)
    }
}
