package splatdevelopment.homeprice.analyzer

import android.graphics.Rect
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.toComposeRect
import splatdevelopment.homeprice.domain.DetectedPrice
import com.google.mlkit.vision.text.Text

/**
 * Finds every number in recognized text that could be a price. The user picks the real
 * one on screen, so this only filters out numbers that are clearly not prices.
 */
object PriceAnalyzer {

    internal data class PriceMatch(val value: Double, val range: IntRange)

    // Matches any number, with or without a currency symbol next to it.
    // e.g. "¥5, 000" captures "5, 000", "12.99 €" captures "12.99", "450" captures "450".
    // A space before exactly two digits counts as a decimal point OCR missed: "2 99" captures "2 99".
    private val priceRegex = Regex("""(?<!\d)(\d{1,3}(?:[.,\s]+\d{3})*(?:[.,]\d{1,2}|\s\d{2}(?!\d))?|\d+)(?!\d)""")

    // Dates like "11-26-24" or "11/26/2024" (same separator twice, so "2/6.00" is not a date)
    private val dateRegex = Regex("""(?<!\d)\d{1,2}([-/])\d{1,2}\1\d{2,4}(?!\d)""")

    // Units and unit prices right after the number: "10 OZ", "37.9¢", "2L", "500 ml", "PER OUNCE"
    private val unitSuffixRegex = Regex("""^\s*(?:¢|c\b|oz\b|lbs?\b|kg\b|g\b|ml\b|l\b|per\b|%)""", RegexOption.IGNORE_CASE)

    private const val CURRENCY_SYMBOLS = "¥$£€₹₽₩"

    // UPC/JAN/SKU codes are long runs of digits with no separators
    private const val MIN_CODE_DIGITS = 7

    // Digits that could hide a missed decimal point: "299" for "2.99", up to "9999" for "99.99"
    private val HIDDEN_DECIMAL_LENGTHS = 3..4

    private class Word(val text: String, val range: IntRange, val bounds: Rect?)

    /** Returns candidate prices with bounds covering just the number's words. */
    fun detectPrices(text: Text): List<DetectedPrice> =
        text.textBlocks.flatMap { it.lines }.flatMap { line ->
            // Rebuild the line from its words so restored decimal points keep each word's position in sync
            val words = mutableListOf<Word>()
            var start = 0
            line.elements.forEach { element ->
                val wordText = restoreDecimalPoint(element)
                words += Word(wordText, start until start + wordText.length, element.boundingBox)
                start += wordText.length + 1
            }
            val lineText = words.joinToString(" ") { it.text }

            findPriceCandidates(lineText).mapNotNull { match ->
                val boxes = words.filter { it.range.first <= match.range.last && it.range.last >= match.range.first }
                    .mapNotNull { it.bounds }
                val bounds = if (boxes.isEmpty()) line.boundingBox else Rect(boxes.first()).apply { boxes.drop(1).forEach(::union) }
                bounds?.let { DetectedPrice(match.value, it.toComposeRect()) }
            }
        }

    /** Puts back a decimal point OCR dropped, e.g. a tiny dot on an e-ink tag read as "299" instead of "2.99". */
    private fun restoreDecimalPoint(element: Text.Element): String {
        val text = element.text
        if (text.length !in HIDDEN_DECIMAL_LENGTHS || !text.all(Char::isDigit)) return text

        val digitBoxes = element.symbols.mapNotNull { it.boundingBox?.toComposeRect() }
        if (digitBoxes.size != text.length || !hasHiddenDecimalBeforeCents(digitBoxes)) return text
        return text.dropLast(2) + "." + text.takeLast(2)
    }

    /** Looks at the digit boxes of a number for signs that the last two digits are cents. */
    internal fun hasHiddenDecimalBeforeCents(digitBoxes: List<ComposeRect>): Boolean {
        if (digitBoxes.size < 3) return false
        val cents = digitBoxes.takeLast(2)
        val units = digitBoxes.dropLast(2)

        // Superscript cents are noticeably shorter than the main digits: "2⁹⁹"
        if (cents.maxOf { it.height } < units.maxOf { it.height } * 0.75f) return true

        // A skipped dot leaves a wider gap before the cents than between the other digits
        val gaps = digitBoxes.zipWithNext { a, b -> b.left - a.right }
        val centsGapIndex = gaps.size - 2
        val widestOtherGap = gaps.filterIndexed { i, _ -> i != centsGapIndex }.max().coerceAtLeast(0f)
        val digitWidth = digitBoxes.map { it.width }.average().toFloat()
        return gaps[centsGapIndex] > widestOtherGap + digitWidth * 0.3f
    }

    // Tag boxes can be a little tight around the edge digits
    private const val TAG_MARGIN_PX = 16f

    /**
     * Keeps the prices the user is aiming at. When the tag detector found tags in the aim
     * box, only prices inside those tags count; otherwise the aim box alone decides, since
     * the detector misses some tags. With [largestPerTag], keeps just the tallest number in
     * each tag (or in the box, without tags), which is usually the headline price.
     */
    fun selectPrices(
        prices: List<DetectedPrice>,
        aimBox: ComposeRect,
        tags: List<ComposeRect>,
        largestPerTag: Boolean,
    ): List<DetectedPrice> {
        val inBox = prices.filter { aimBox.contains(it.bounds.center) }
        val tagsInBox = tags.filter { it.overlaps(aimBox) }.map { it.inflate(TAG_MARGIN_PX) }
        val groups = if (tagsInBox.isEmpty()) {
            listOf(inBox)
        } else {
            tagsInBox.map { tag -> inBox.filter { tag.contains(it.bounds.center) } }
        }

        return if (largestPerTag) {
            groups.mapNotNull { group -> group.maxByOrNull { it.bounds.height } }.distinctBy { it.id }
        } else {
            groups.flatten().distinctBy { it.id }
        }
    }

    internal fun extractPrices(text: String): List<Double> = findPriceCandidates(text).map { it.value }

    internal fun findPriceCandidates(text: String): List<PriceMatch> {
        val dateRanges = dateRegex.findAll(text).map { it.range }.toList()

        return priceRegex.findAll(text).mapNotNull { match ->
            val raw = match.value
            val isDate = dateRanges.any { match.range.first in it }
            val isCode = raw.length >= MIN_CODE_DIGITS && raw.all(Char::isDigit)
            val hasUnit = unitSuffixRegex.containsMatchIn(text.substring(match.range.last + 1))
            // Lone digits like "2 pieces" or "1 pack" are noise unless a symbol marks them as money
            val isBareDigit = raw.length == 1 &&
                text.substring(0, match.range.first).trimEnd().lastOrNull()?.let { it in CURRENCY_SYMBOLS } != true

            if (isDate || isCode || hasUnit || isBareDigit) return@mapNotNull null
            parsePrice(raw)?.let { PriceMatch(it, match.range) }
        }.toList()
    }

    private fun parsePrice(raw: String): Double? {
        // A space before exactly two final digits was a missed decimal point: "2 99" -> "2.99"
        // Then remove all other spaces to normalize: "5, 000" -> "5,000"
        val s = raw.replace("\\s+(?=\\d{2}$)".toRegex(), ".").replace("\\s+".toRegex(), "")

        val lastComma = s.lastIndexOf(',')
        val lastDot = s.lastIndexOf('.')
        val lastSeparatorIdx = maxOf(lastComma, lastDot)

        if (lastSeparatorIdx == -1) {
            return s.toDoubleOrNull()
        }

        val digitsAfterSeparator = s.length - lastSeparatorIdx - 1

        return if (digitsAfterSeparator == 3) {
            // Thousands separator: "5,000" or "5.000"
            s.replace(",", "").replace(".", "").toDoubleOrNull()
        } else {
            // Decimal separator: "5.99" or "5,99"
            val integerPart = s.substring(0, lastSeparatorIdx).replace(",", "").replace(".", "")
            val decimalPart = s.substring(lastSeparatorIdx + 1)
            "${integerPart}.${decimalPart}".toDoubleOrNull()
        }
    }
}
