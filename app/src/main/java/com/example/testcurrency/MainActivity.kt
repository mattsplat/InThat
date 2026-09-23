package com.example.testcurrency

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.testcurrency.domain.ConverterController
import com.example.testcurrency.ui.PriceScannerScreen
import com.example.testcurrency.ui.theme.InThatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Share the same controller across screens for consistent currency selection
        val controller = ConverterController()

        setContent {
            InThatTheme {
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
