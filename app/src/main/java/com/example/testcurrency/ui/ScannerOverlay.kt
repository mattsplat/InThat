package com.example.testcurrency.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.testcurrency.domain.ConverterState
import java.util.Locale

@Composable
fun ScannerOverlay(
    state: ConverterState,
    previewWidth: Int,
    previewHeight: Int,
    screenWidth: Int,
    screenHeight: Int
) {
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {
        
        // Add a top bar to show current conversion setting
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
            }
        }

        state.detectedPrices.values.forEach { detected ->
            val rate = state.rateAB ?: 1.0
            val convertedValue = detected.value * rate
            val fromCurrency = state.fromCurrency
            val toCurrency = state.toCurrency

            // Simple scaling from preview coordinates to screen coordinates
            val scaleX = screenWidth.toFloat() / previewWidth
            val scaleY = screenHeight.toFloat() / previewHeight

            val left = detected.bounds.left * scaleX
            val top = detected.bounds.top * scaleY

            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { left.toDp() },
                        y = with(density) { top.toDp() }
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${String.format(Locale.US, "%,.2f", detected.value)} $fromCurrency ➔ ${String.format(Locale.US, "%,.2f", convertedValue)} $toCurrency",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
