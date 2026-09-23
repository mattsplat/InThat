package splatdevelopment.homeprice.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import splatdevelopment.homeprice.domain.ConverterState
import splatdevelopment.homeprice.domain.DetectedPrice
import splatdevelopment.homeprice.domain.ScanMode

class ScannerOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sale = DetectedPrice(8.99, Rect(300f, 900f, 450f, 960f))
    private val regular = DetectedPrice(11.99, Rect(300f, 1200f, 450f, 1260f))
    private val autoState = ConverterState(
        fromCurrency = "USD",
        toCurrency = "CAD",
        rateAB = 2.0,
        scanMode = ScanMode.Auto,
        detectedPrices = listOf(sale, regular).associateBy { it.id },
    )

    private var selectedId: String? = null
    private var resumed = false

    private fun show(state: ConverterState) = composeTestRule.setContent {
        ScannerOverlay(
            state = state,
            onSelectPrice = { selectedId = it },
            onScanModeChange = {},
            onResumeScanning = { resumed = true },
        )
    }

    @Test
    fun autoMode_tappingACardPausesOnThatPrice() {
        show(autoState)

        composeTestRule.onNodeWithText(formatMoney(17.98, "CAD")).performClick()

        assertEquals(sale.id, selectedId)
    }

    @Test
    fun autoMode_pausedFrameShowsOnlyTheTappedPrice() {
        show(autoState.copy(selectedPriceId = sale.id))

        composeTestRule.onAllNodesWithText(formatMoney(17.98, "CAD")).assertCountEquals(1)
        composeTestRule.onAllNodesWithText(formatMoney(23.98, "CAD")).assertCountEquals(0)
    }

    @Test
    fun autoMode_tappingThePausedCardResumes() {
        show(autoState.copy(selectedPriceId = sale.id))

        composeTestRule.onNodeWithText(formatMoney(17.98, "CAD")).performClick()

        assertEquals(true, resumed)
    }
}
