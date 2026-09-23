package splatdevelopment.homeprice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import splatdevelopment.homeprice.domain.ConverterController
import splatdevelopment.homeprice.ui.PriceScannerScreen
import splatdevelopment.homeprice.ui.theme.HomePriceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Share the same controller across screens for consistent currency selection
        val controller = ConverterController()

        setContent {
            HomePriceTheme {
                var isScanning by remember { mutableStateOf(false) }

                if (isScanning) {
                    PriceScannerScreen(
                        controller = controller,
                        onClose = { isScanning = false }
                    )
                } else {
                    ConverterScreen(
                        controller = controller,
                        onOpenScanner = { isScanning = true }
                    )
                }
            }
        }
    }
}
