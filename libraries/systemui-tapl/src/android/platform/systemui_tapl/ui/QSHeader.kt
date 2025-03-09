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

import android.platform.systemui_tapl.utils.DeviceUtils.LONG_WAIT
import android.platform.systemui_tapl.utils.DeviceUtils.SHORT_WAIT
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForFirstObj
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import androidx.test.uiautomator.By
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * Space above quick settings, similar to the status bar. Visible only when the shade is open. It
 * what's visible inside might differ when:
 * - in QQS state
 * - in QS state
 * - Large screen device https://hsv.googleplex.com/4864533182021632?node=16
 */
class QSHeader internal constructor() {
    private val uiObject =
        waitForFirstObj(
                sysuiResSelector("shade_header_root"),
                sysuiResSelector("split_shade_status_bar"),
                timeout = LONG_WAIT,
            )
            .first

    /** Returns the value of the battery level on QS header. Experimental. */
    fun getBatteryLevel(): String {
        return uiDevice
            .waitForObj(sysuiResSelector(StatusBar.BATTERY_LEVEL_TEXT_ID)) {
                "Battery percentage not found on QS header."
            }
            .text
    }

    /** Verifies the clock is visible. Throws otherwise. Experimental. */
    fun verifyTimeVisible() {
        // Matches 12h or 24h time format
        val timePattern = Pattern.compile("^(?:[01]?\\d|2[0-3]):[0-5]\\d")
        By.pkg("com.android.systemui").text(timePattern).assertVisible(SHORT_WAIT)
    }

    /** Verifies the date is visible. Throws otherwise. Experimental. */
    fun verifyDateVisible() {
        val date = Date(System.currentTimeMillis())
        val dateText = SimpleDateFormat("EEE, MMM d", Locale.ENGLISH).format(date)
        By.pkg("com.android.systemui").text(dateText).assertVisible(SHORT_WAIT)
    }

    /** Verifies status icons are visible. Throws otherwise. Experimental. */
    fun verifyStatusIconsVisible() {
        // See {@link EnsureAtLeastOneStatusBarIconVisibleRule}
        verifySilentIconIsVisible()
    }

    /** Verifies the battery percentage is visible. Throws otherwise. Experimental. */
    fun verifyBatteryMeterVisible() {
        "battery_percentage_view".assertVisible()
    }

    /** Verifies that dock defend icon is visible. */
    fun verifyDockDefendIconVisible() {
        uiObject.hasObject(By.descContains(StatusBar.DOCK_DEFEND_ICON_SUFFIX_STRING))
    }

    /** Verifies that DND icon is visible. Experimental. */
    fun verifyDndIconIsVisible() {
        By.pkg("com.android.systemui")
            .clazz("android.widget.ImageView")
            .desc(StatusBar.DND_ICON_DESC)
            .assertVisible(SHORT_WAIT)
    }

    /** Verifies that silent icon is visible. */
    fun verifySilentIconIsVisible() {
        By.pkg("com.android.systemui")
            .clazz("android.widget.ImageView")
            .desc("Ringer silent.")
            .assertVisible(SHORT_WAIT)
    }

    private fun String.assertVisible() = sysuiResSelector(this).assertVisible(timeout = SHORT_WAIT)
}
