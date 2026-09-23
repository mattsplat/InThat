package splatdevelopment.homeprice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class ExampleInstrumentedTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun converterScreen_showsTitleAndAmountField() {
        composeTestRule.onNodeWithText("HomePrice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Amount").assertIsDisplayed()
    }
}
