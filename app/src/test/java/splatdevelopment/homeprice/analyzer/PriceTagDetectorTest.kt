package splatdevelopment.homeprice.analyzer

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class PriceTagDetectorTest {

    /** Builds YOLOv8-style output: rows of cx, cy, w, h, score with one column per candidate. */
    private fun output(vararg candidates: FloatArray): FloatArray {
        val n = candidates.size
        return FloatArray(5 * n).also { out ->
            candidates.forEachIndexed { i, c -> for (row in 0 until 5) out[row * n + i] = c[row] }
        }
    }

    @Test
    fun `decodes center-size boxes into corners`() {
        val tags = PriceTagDetector.decodeDetections(output(floatArrayOf(100f, 200f, 40f, 20f, 0.9f)))

        assertEquals(listOf(Rect(80f, 190f, 120f, 210f)), tags)
    }

    @Test
    fun `drops low-confidence candidates`() {
        val tags = PriceTagDetector.decodeDetections(output(floatArrayOf(100f, 100f, 40f, 20f, 0.2f)))

        assertEquals(emptyList<Rect>(), tags)
    }

    @Test
    fun `keeps the most confident of overlapping boxes and separate tags`() {
        val tags = PriceTagDetector.decodeDetections(
            output(
                floatArrayOf(100f, 100f, 40f, 20f, 0.6f),
                floatArrayOf(102f, 101f, 40f, 20f, 0.9f), // same tag, more confident
                floatArrayOf(400f, 300f, 60f, 30f, 0.8f), // a different tag
            )
        )

        assertEquals(listOf(Rect(82f, 91f, 122f, 111f), Rect(370f, 285f, 430f, 315f)), tags)
    }
}
