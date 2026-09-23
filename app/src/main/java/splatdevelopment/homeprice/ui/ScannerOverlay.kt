package splatdevelopment.homeprice.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import splatdevelopment.homeprice.analyzer.PriceTagDetector
import splatdevelopment.homeprice.data.CurrencyCatalog
import splatdevelopment.homeprice.domain.ConverterState
import splatdevelopment.homeprice.domain.DetectedPrice
import splatdevelopment.homeprice.domain.ScanMode
import java.util.Locale
import kotlin.math.roundToInt

// Grow each box so small numbers are still easy to tap
private const val TAP_PADDING_PX = 12f

/**
 * The centered region the user aims at a price tag. Price tags are wide, so the box is
 * about twice as wide as it is tall, capped so it fits between the top bar and buttons.
 */
internal fun aimBoxRect(size: Size): Rect {
    val width = size.width * 0.85f
    val height = minOf(width * 0.55f, size.height * 0.4f)
    return Rect(
        left = (size.width - width) / 2,
        top = (size.height - height) / 2,
        right = (size.width + width) / 2,
        bottom = (size.height + height) / 2,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerOverlay(
    state: ConverterState,
    onSelectPrice: (String) -> Unit,
    onScanModeChange: (ScanMode) -> Unit,
    onResumeScanning: () -> Unit,
    tagDetections: PriceTagDetector.Result? = null,
) {
    val isAuto = state.scanMode == ScanMode.Auto
    val selected = state.selectedPriceId?.let(state.detectedPrices::get)
    // A paused Auto frame keeps just the price the user tapped
    val highlighted = if (isAuto && selected != null) listOf(selected) else state.detectedPrices.values.toList()
    val cards = if (isAuto) highlighted else listOfNotNull(selected)

    Box(modifier = Modifier.fillMaxSize()) {
        AimBox()
        tagDetections?.let { TagOutlines(it.tags) }

        highlighted.forEach { detected ->
            PriceHighlight(
                detected = detected,
                isSelected = detected.id == state.selectedPriceId,
                onClick = if (isAuto) null else { { onSelectPrice(detected.id) } },
            )
        }

        cards.forEach { price ->
            ConversionLabel(
                price = price,
                state = state,
                // Tapping a card pauses on it; tapping the paused card again resumes
                onClick = { if (price.id == state.selectedPriceId) onResumeScanning() else onSelectPrice(price.id) },
            )
        }

        // Top bar shows the current conversion setting
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Scanning Prices",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${state.fromCurrency} ➔ ${state.toCurrency}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                tagDetections?.let {
                    Text(
                        text = "Tag detector (trial): ${it.tags.size} found · convert ${it.conversionMs} ms + model ${it.inferenceMs} ms",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(vertical = 8.dp)) {
                    ScanMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.scanMode == mode,
                            onClick = { onScanModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ScanMode.entries.size),
                        ) {
                            Text(mode.name)
                        }
                    }
                }
                Text(
                    text = when {
                        isAuto && selected != null -> "Paused · tap the price again to resume"
                        isAuto -> "Fit a price tag in the box · tap a price to pause"
                        selected == null -> "Fit a price tag in the box, then tap the price"
                        else -> "Tap another price, or tap the card to resume"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Dims everything outside the aim box so it's clear where to point the camera. */
@Composable
private fun AimBox() {
    val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
    val outline = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val box = RoundRect(aimBoxRect(size), CornerRadius(16.dp.toPx()))
        val outside = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addRoundRect(box)
        }
        drawPath(outside, scrim)
        drawPath(Path().apply { addRoundRect(box) }, outline, style = Stroke(width = 3.dp.toPx()))
    }
}

/** Outlines the tags found by the trial detector, so its quality can be judged live. */
@Composable
private fun TagOutlines(tags: List<Rect>) {
    val color = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx()))
        tags.forEach { tag ->
            drawRect(color, topLeft = tag.topLeft, size = tag.size, style = Stroke(width = 2.dp.toPx(), pathEffect = dash))
        }
    }
}

@Composable
private fun PriceHighlight(
    detected: DetectedPrice,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
) {
    val density = LocalDensity.current
    val bounds = detected.bounds.inflate(TAP_PADDING_PX)
    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inversePrimary
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = Modifier
            .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
            .size(with(density) { bounds.width.toDp() }, with(density) { bounds.height.toDp() })
            .clip(shape)
            .background(color.copy(alpha = if (isSelected) 0.35f else 0.15f))
            .border(if (isSelected) 3.dp else 2.dp, color, shape)
            .semantics { contentDescription = "Price ${formatAmount(detected.value)}" }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    )
}

/** Places the conversion card next to its price and keeps it on screen. */
@Composable
private fun ConversionLabel(price: DetectedPrice, state: ConverterState, onClick: () -> Unit) {
    val density = LocalDensity.current
    val margin = with(density) { 12.dp.roundToPx() }
    val gap = with(density) { 6.dp.roundToPx() }
    // Clear the highlight drawn around the price, not just the digits
    val anchor = price.bounds.inflate(TAP_PADDING_PX)

    Layout(content = { ConversionCard(price.value, state, onClick) }, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val card = measurables.single().measure(
            Constraints(maxWidth = (constraints.maxWidth - 2 * margin).coerceAtLeast(0), maxHeight = constraints.maxHeight)
        )
        val position = labelPosition(
            anchor = anchor,
            label = IntSize(card.width, card.height),
            container = IntSize(constraints.maxWidth, constraints.maxHeight),
            margin = margin,
            gap = gap,
        )
        layout(constraints.maxWidth, constraints.maxHeight) { card.place(position) }
    }
}

/**
 * Puts the label centered above [anchor], or below it when there's no room above, and
 * shifts it sideways so it stays [margin] px inside the container.
 */
internal fun labelPosition(anchor: Rect, label: IntSize, container: IntSize, margin: Int, gap: Int): IntOffset {
    val maxX = (container.width - label.width - margin).coerceAtLeast(margin)
    val x = (anchor.center.x - label.width / 2f).roundToInt().coerceIn(margin, maxX)

    val above = (anchor.top - gap - label.height).roundToInt()
    val below = (anchor.bottom + gap).roundToInt()
    val y = if (above >= margin) above else below.coerceAtMost((container.height - label.height - margin).coerceAtLeast(0))
    return IntOffset(x, y)
}

/** The converted price large, with the original price underneath for reference. */
@Composable
private fun ConversionCard(amount: Double, state: ConverterState, onClick: () -> Unit) {
    val rate = state.rateAB

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 8.dp,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            if (rate == null) {
                Text("Loading rate…", style = MaterialTheme.typography.titleMedium)
            } else {
                Row {
                    Text(
                        text = formatMoney(amount * rate, state.toCurrency),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = state.toCurrency,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
            Text(
                text = "${formatMoney(amount, state.fromCurrency)} ${state.fromCurrency}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Formats money with the currency's symbol and usual decimals: "C$12.63", "¥1,180". */
internal fun formatMoney(value: Double, currencyCode: String): String {
    val decimals = runCatching { java.util.Currency.getInstance(currencyCode).defaultFractionDigits }
        .getOrDefault(2)
        .coerceAtLeast(0)
    val symbol = CurrencyCatalog.supported.find { it.code == currencyCode }?.symbol.orEmpty()
    return symbol + String.format(Locale.US, "%,.${decimals}f", value)
}

private fun formatAmount(value: Double): String = String.format(Locale.US, "%,.2f", value)
