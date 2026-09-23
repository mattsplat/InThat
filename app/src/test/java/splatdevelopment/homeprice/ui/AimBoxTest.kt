package splatdevelopment.homeprice.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AimBoxTest {

    @Test
    fun `aim box is centered and wider than tall on a portrait screen`() {
        val box = aimBoxRect(Size(1000f, 2000f))

        assertEquals(Offset(500f, 1000f), box.center)
        assertEquals(850f, box.width, 0.01f)
        assertEquals(467.5f, box.height, 0.01f)
    }

    @Test
    fun `aim box height is capped on a landscape screen`() {
        val box = aimBoxRect(Size(2000f, 1000f))

        assertEquals(400f, box.height, 0.01f)
        assertTrue(box.top >= 0f && box.bottom <= 1000f)
    }

    @Test
    fun `numbers near the screen edges fall outside the aim box`() {
        val box = aimBoxRect(Size(1000f, 2000f))

        assertTrue(box.contains(Offset(500f, 1000f)))
        assertFalse(box.contains(Offset(500f, 200f)))
        assertFalse(box.contains(Offset(20f, 1000f)))
    }
}
