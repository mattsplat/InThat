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
        // Read the app's own strings so the test passes in any device language
        val activity = composeTestRule.activity
        composeTestRule.onNodeWithText(activity.getString(R.string.app_name)).assertIsDisplayed()
        composeTestRule.onNodeWithText(activity.getString(R.string.converter_amount)).assertIsDisplayed()
    }
}
