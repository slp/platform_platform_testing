/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.platform.systemui_tapl.ui

import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.systemui_tapl.utils.SETTINGS_PACKAGE
import android.platform.test.scenario.tapl_common.Gestures
import android.platform.test.scenario.tapl_common.Gestures.click
import android.platform.test.scenario.tapl_common.TaplUiDevice
import android.platform.test.util.HealthTestingUtils.waitForValueCatchingStaleObjectExceptions
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.WaitResult
import android.platform.uiautomatorhelpers.WaitUtils.ensureThat
import android.platform.uiautomatorhelpers.WaitUtils.waitToBecomeTrue
import android.platform.uiautomatorhelpers.scrollUntilFound
import android.util.Log
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import com.android.systemui.Flags
import com.google.common.truth.Truth.assertWithMessage

/** Base class for tiles in Quick Settings and Quick Quick Settings */
sealed class QuickSettingsTileBase {
    protected abstract val tile: UiObject2

    /**
     * Obtain the label of this tile. Will fail if the label is not found or it doesn't have text.
     */
    val tileLabel: String
        get() {
            val label =
                if (Flags.qsUiRefactorComposeFragment()) {
                    tile.contentDescription
                } else {
                    tile.waitForObj(QuickQuickSettings.UI_TILE_LABEL_SELECTOR).text
                }
            assertWithMessage("Tile label should not be null").that(label).isNotNull()
            return label
        }

    /** Returns whether the tile is checked. */
    val isChecked: Boolean
        get() {
            ensureThat("tile is clickable") {
                waitForValueCatchingStaleObjectExceptions({
                    "Failed to get clickable state for tile."
                }) {
                    tile.isClickable
                }
            }
            return waitForValueCatchingStaleObjectExceptions({
                "Failed to get checked state for tile"
            }) {
                tile.isChecked
            }
        }

    /** Clicks a non-Internet tile and verifies that its checked state changes. */
    fun click() {
        val wasChecked = isChecked
        click(tile, "Tile")
        assertCheckedStatus(!wasChecked)
    }

    /**
     * Clicks a non-Internet tile without any assertions about resulting tile state. Please don't
     * use unless you have a very good reason to omit assertions.
     */
    fun clickWithoutAssertions() {
        click(tile, "Tile")
    }

    fun assertCheckedStatus(checked: Boolean) {
        val expectedState = if (checked) "checked" else "unchecked"
        ensureThat("tile is $expectedState") { tile.isChecked == checked }
    }

    /** Clicks the Internet tile and presses Done button. */
    fun clickInternetTile() {
        click(tile, "Tile")
        val scrollView =
            TaplUiDevice.waitForObject(
                    sysuiResSelector(DIALOG_RES_ID),
                    objectName = "Internet connectivity dialog",
                )
                .waitForChildObject(
                    childResourceId = SCROLL_VIEW_RES_ID,
                    childObjectName = "Scroll view",
                )
                .uiObject
        val doneButton = scrollView.scrollUntilFound(DONE_BTN) ?: error("Done button not found")
        doneButton.click()
        if (waitToBecomeTrue { !uiDevice.hasObject(DONE_BTN) }.result !is WaitResult.WaitSuccess) {
            Log.d("QuickSettingsTileBase", "Retrying click due to b/339676505")
            doneButton.click()
        }
        DONE_BTN.assertInvisible(errorProvider = { "Internet dialog is dismissed" })
    }

    /** Clicks the Bluetooth tile and presses Done button. */
    fun clickBluetoothTile() {
        click(tile, "Tile")
        val scrollView =
            TaplUiDevice.waitForObject(
                    sysuiResSelector(BLUETOOTH_TILE_DIALOG_RES_ID),
                    objectName = "Bluetooth tile dialog",
                )
                .waitForChildObject(
                    childResourceId = SCROLL_VIEW_RES_ID,
                    childObjectName = "Scroll view",
                )
                .uiObject
        scrollView.scrollUntilFound(DONE_BTN)?.click() ?: error("Done button not found")
        DONE_BTN.assertInvisible(errorProvider = { "Bluetooth tile dialog is dismissed" })
    }

    fun clickDnDIntoDialog(): AlertDialog {
        click(tile, "Tile")
        return AlertDialog()
    }

    fun clickModesTile(): ModesDialog {
        click(tile, "Tile")
        return ModesDialog()
    }

    /** Long-clicks the tile and verifies that Settings app appears, unless otherwise specified */
    fun longClick(expectedSettingsPackage: String? = null) {
        val longClick = Gestures.longClickDown(tile, "Quick settings tile")
        try {
            val packageName = expectedSettingsPackage ?: SETTINGS_PACKAGE
            By.pkg(packageName).assertVisible { "$packageName didn't appear" }
        } finally {
            longClick.up()
        }
    }

    private companion object {
        val DONE_BTN = sysuiResSelector("done_button")
        const val DIALOG_RES_ID = "internet_connectivity_dialog"
        const val SCROLL_VIEW_RES_ID = "scroll_view"
        const val BLUETOOTH_TILE_DIALOG_RES_ID = "root"
    }
}

/**
 * System UI test automation object representing a Quick Settings tile.
 *
 * It keeps track of the tile by its selector, instead of the UiObject2 in case it moves in the
 * middle of the test.
 */
class QuickSettingsTile internal constructor(private val selector: BySelector) :
    QuickSettingsTileBase() {

    override val tile: UiObject2
        get() = waitForObj(selector)
}

/**
 * System UI test automation object representing a Quick Quick Settings tile.
 *
 * As these are retrieved by the position, they are associated with the actual object.
 */
class QuickQuickSettingsTile internal constructor(override val tile: UiObject2) :
    QuickSettingsTileBase()
