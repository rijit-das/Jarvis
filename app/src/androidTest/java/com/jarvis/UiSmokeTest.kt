package com.jarvis

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented smoke test: the app launches and the home screen renders.
 * Requires an emulator/device — run via `./gradlew :app:connectedDebugAndroidTest`.
 */
class UiSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenRenders() {
        composeRule.onNodeWithText("JARVIS").assertIsDisplayed()
        composeRule.onNodeWithText("Talk to JARVIS").assertIsDisplayed()
        composeRule.onNodeWithText("Type a message").assertIsDisplayed()
    }

    @Test
    fun bottomNavigationIsVisible() {
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("Chat").assertIsDisplayed()
        composeRule.onNodeWithText("Voice").assertIsDisplayed()
        composeRule.onNodeWithText("Memory").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
