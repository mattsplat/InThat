package splatdevelopment.homeprice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import splatdevelopment.homeprice.domain.ConverterController
import splatdevelopment.homeprice.data.CurrencyCatalog
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    controller: ConverterController,
    onOpenScanner: () -> Unit
) {
    val state by controller.state.collectAsState()

    val fromCurrency = remember(state.fromCurrency) {
        CurrencyCatalog.supported.find { it.code == state.fromCurrency }
    }
    val toCurrency = remember(state.toCurrency) {
        CurrencyCatalog.supported.find { it.code == state.toCurrency }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .safeDrawingPadding() // Ensures UI stays clear of system bars/notches
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.size(8.dp))
            
            Text(
                text = "HomePrice",
                style = MaterialTheme.typography.headlineMedium
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(end = 56.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CurrencyDropdown(
                        label = "From",
                        selected = state.fromCurrency,
                        onSelected = controller::updateFromCurrency,
                    )
                    CurrencyDropdown(
                        label = "To",
                        selected = state.toCurrency,
                        onSelected = controller::updateToCurrency,
                    )
                }
                IconButton(
                    onClick = controller::swapCurrencies,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Swap currencies",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            OutlinedTextField(
                value = state.amountInput,
                onValueChange = controller::updateAmount,
                label = { Text("Amount") },
                prefix = fromCurrency?.symbol?.let { { Text("$it ") } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onOpenScanner,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Scan Price Tag")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    // contentColorFor() can't match an alpha-modified color, so set it explicitly
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.isLoading) {
                        Text("Loading latest exchange rate…")
                    }
                    state.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    
                    state.rateAB?.let {
                        val fromSym = fromCurrency?.symbol ?: ""
                        val toSym = toCurrency?.symbol ?: ""
                        Text("${fromSym}1 ${state.fromCurrency} = ${toSym}${formatRate(it, 4)} ${state.toCurrency}")
                    }
                    state.rateBA?.let {
                        val fromSym = fromCurrency?.symbol ?: ""
                        val toSym = toCurrency?.symbol ?: ""
                        Text("${toSym}1 ${state.toCurrency} = ${fromSym}${formatRate(it, 4)} ${state.fromCurrency}")
                    }
                    state.convertedAmount?.let {
                        Text(
                            text = "${toCurrency?.symbol ?: ""}${formatRate(it, 2)} ${state.toCurrency}",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    }
                    state.updatedAt?.let {
                        Text("Updated: $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Text(
                text = "Choose two currencies and enter an amount to compare them. Use the camera to scan prices directly from labels.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.size(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    label: String,
    selected: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCurrency = remember(selected) { 
        CurrencyCatalog.supported.find { it.code == selected } 
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = "${selectedCurrency?.symbol ?: ""} ${selectedCurrency?.code ?: ""} - ${selectedCurrency?.name ?: ""}",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                CurrencyCatalog.favorites.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text("${currency.symbol} ${currency.code} - ${currency.name}") },
                        onClick = {
                            onSelected(currency.code)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
                
                HorizontalDivider()
                
                Text(
                    text = "All Currencies",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.secondary
                )
                CurrencyCatalog.all.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text("${currency.symbol} ${currency.code} - ${currency.name}") },
                        onClick = {
                            onSelected(currency.code)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

private fun formatRate(value: Double, decimals: Int): String {
    val pattern = "%,.${decimals}f"
    return String.format(Locale.US, pattern, value)
}
