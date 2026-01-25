/*
 *  Copyright 2025 Bammellab / James Andreas
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package com.bammellab.musicplayer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for the About dialog.
 *
 * This test verifies that:
 * 1. The About button is accessible from the main screen
 * 2. The About dialog displays correctly with expected content
 * 3. The About description dialog can be opened and displays correctly
 * 4. Dialogs can be dismissed properly
 */
@RunWith(AndroidJUnit4::class)
class AboutDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun aboutButton_isDisplayed() {
        // Verify the About button is visible in the top app bar
        composeTestRule
            .onNodeWithContentDescription("About")
            .assertIsDisplayed()
    }

    @Test
    fun aboutDialog_opensAndDisplaysContent() {
        // Click the About button
        composeTestRule
            .onNodeWithContentDescription("About")
            .performClick()

        // Verify the About dialog content is displayed
        composeTestRule
            .onNodeWithText("About this app…")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("This app in the Play Store…")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Source code")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Version")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Close")
            .assertIsDisplayed()
    }

    @Test
    fun aboutDialog_showsVersionNumber() {
        // Click the About button
        composeTestRule
            .onNodeWithContentDescription("About")
            .performClick()

        // Verify the version number is displayed (from BuildConfig)
        composeTestRule
            .onNodeWithText(BuildConfig.VERSION_NAME)
            .assertIsDisplayed()
    }

    @Test
    fun aboutDialog_canBeClosed() {
        // Click the About button
        composeTestRule
            .onNodeWithContentDescription("About")
            .performClick()

        // Verify dialog is open
        composeTestRule
            .onNodeWithText("About this app…")
            .assertIsDisplayed()

        // Click Close button
        composeTestRule
            .onNodeWithText("Close")
            .performClick()

        // Verify dialog is closed (About button should be visible again without dialog)
        composeTestRule
            .onNodeWithContentDescription("About")
            .assertIsDisplayed()

        // The dialog text should no longer be displayed
        composeTestRule
            .onNodeWithText("About this app…")
            .assertDoesNotExist()
    }

    @Test
    fun aboutDescriptionDialog_opensFromAboutDialog() {
        // Click the About button
        composeTestRule
            .onNodeWithContentDescription("About")
            .performClick()

        // Click "About this app..." to open description dialog
        composeTestRule
            .onNodeWithText("About this app…")
            .performClick()

        // Verify the description dialog content is displayed
        composeTestRule
            .onNodeWithText("Simple Music Player is a lightweight, open-source Android app for playing your local music collection.", substring = true)
            .assertIsDisplayed()

        // Verify Close button is present
        composeTestRule
            .onNodeWithText("Close")
            .assertIsDisplayed()
    }

    @Test
    fun aboutDescriptionDialog_canBeClosed() {
        // Click the About button
        composeTestRule
            .onNodeWithContentDescription("About")
            .performClick()

        // Click "About this app..." to open description dialog
        composeTestRule
            .onNodeWithText("About this app…")
            .performClick()

        // Verify description dialog is open
        composeTestRule
            .onNodeWithText("Simple Music Player is a lightweight", substring = true)
            .assertIsDisplayed()

        // Click Close button
        composeTestRule
            .onNodeWithText("Close")
            .performClick()

        // Verify description dialog is closed
        composeTestRule
            .onNodeWithText("Simple Music Player is a lightweight", substring = true)
            .assertDoesNotExist()
    }
}
