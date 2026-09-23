package splatdevelopment.homeprice.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversionLabelTest {

    private val screen = IntSize(1000, 2000)
    private val label = IntSize(300, 120)

    @Test
    fun `label sits centered above the price`() {
        val position = labelPosition(Rect(400f, 900f, 600f, 960f), label, screen, margin = 30, gap = 10)

        assertEquals(IntOffset(350, 770), position)
    }

    @Test
    fun `label drops below the price when there is no room above`() {
        val position = labelPosition(Rect(400f, 50f, 600f, 110f), label, screen, margin = 30, gap = 10)

        assertEquals(IntOffset(350, 120), position)
    }

    @Test
    fun `label is pushed back on screen near the edges`() {
        val right = labelPosition(Rect(900f, 900f, 990f, 960f), label, screen, margin = 30, gap = 10)
        val left = labelPosition(Rect(0f, 900f, 60f, 960f), label, screen, margin = 30, gap = 10)

        assertEquals(670, right.x)
        assertEquals(30, left.x)
    }
}
