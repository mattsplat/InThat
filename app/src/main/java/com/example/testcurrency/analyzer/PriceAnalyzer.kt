package com.example.testcurrency.analyzer

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.testcurrency.domain.DetectedPrice
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class PriceAnalyzer(
    private val onPricesDetected: (List<DetectedPrice>) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    // Matches a currency symbol before OR after the number.
    // Group 1: Number if symbol is BEFORE
    // Group 2: Number if symbol is AFTER
    // e.g. "¥5, 000" captures "5, 000" in Group 1
    // e.g. "12.99 €" captures "12.99" in Group 2
    private val priceRegex = Regex("""(?:[¥$£€₹₽₩]\s*)(?<!\d)(\d{1,3}(?:[.,\s]+\d{3})*(?:[.,]\d{1,2})?|\d+)(?!\d)|(?<!\d)(\d{1,3}(?:[.,\s]+\d{3})*(?:[.,]\d{1,2})?|\d+)(?!\d)(?:\s*[¥$£€₹₽₩])""")

    private var lastAnalysisTime = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < 1000) {
            imageProxy.close() // Immediately close the frame to free up resources
            return           // Skip analysis
        }
        lastAnalysisTime = currentTime // Update the timestamp

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val detectedPrices = mutableListOf<DetectedPrice>()
                    
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val text = line.text
                            val matches = priceRegex.findAll(text)
                            
                            for (match in matches) {
                                // Extract the number whether it matched group 1 (symbol before) or group 2 (symbol after)
                                val numericString = match.groupValues[1].takeIf { it.isNotEmpty() } ?: match.groupValues[2]
                                
                                val value = parsePrice(numericString)
                                val bounds = line.boundingBox
                                
                                if (value != null && bounds != null) {
                                    detectedPrices.add(DetectedPrice(value, bounds))
                                }
                            }
                        }
                    }
                    onPricesDetected(detectedPrices)
                }
                .addOnFailureListener { e ->
                    Log.e("PriceAnalyzer", "Text recognition failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun parsePrice(raw: String): Double? {
        // Remove all spaces to normalize: "5, 000" -> "5,000"
        val s = raw.replace("\\s+".toRegex(), "")
        
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
