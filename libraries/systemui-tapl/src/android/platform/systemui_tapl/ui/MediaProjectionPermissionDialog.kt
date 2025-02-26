/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.os.SystemClock
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.systemui_tapl.utils.SYSUI_PACKAGE
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisibility
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.click
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.WaitUtils.ensureThat
import android.platform.uiautomatorhelpers.scrollUntilFound
import androidx.test.uiautomator.By
import com.android.launcher3.tapl.LauncherInstrumentation
import com.android.systemui.shared.system.ActivityManagerWrapper
import java.time.Duration
import java.util.regex.Pattern

/**
 * System UI test automation object representing a media projection permission dialog for sharing
 * the user's screen to another app.
 *
 * https://screenshot.googleplex.com/9YU3VqEh2uiTQ34 (single app) and
 * https://screenshot.googleplex.com/AE5gqPJ3DE3jo7g (entire screen).
 */
class MediaProjectionPermissionDialog internal constructor() {

    init {
        assertSpinnerVisibility(visible = true)
    }

    fun startSingleTaskShare() {
        waitForObj(SPINNER_SELECTOR).click()

        // Make sure clicking one the pop-up option, not the spinner with the same text
        waitForObj(POP_UP).waitForObj(SINGLE_APP_SELECTOR) { "Single app option not found" }.click()
        waitForObj(DIALOG).scrollUntilFound(SINGLE_APP_START_SELECTOR)
            ?: error("Start button not found")

        // TODO(b/333510487): Sleep is temporarily needed after scroll, and should be remove later
        SystemClock.sleep(SHORT_TIMEOUT.toMillis())
        SINGLE_APP_START_SELECTOR.click()
        ensureThat("App selector is launched") { isAppSelectorRunning() }
    }

    private fun isAppSelectorRunning(): Boolean {
        val tasks = ActivityManagerWrapper.getInstance().getRunningTasks(false)
        return tasks.any { it.topActivity?.className?.contains(APP_SELECTOR_ACTIVITY) ?: false }
    }

    fun cancel() {
        waitForObj(CANCEL_SELECTOR) { "Cancel button not found" }.click()
    }

    /* Dismisses the dialog by the Back gesture */
    fun dismiss() {
        LauncherInstrumentation().pressBack()
        assertSpinnerVisibility(visible = false)
    }

    fun assertSingleAppOptionIsDefault() {
        // Single App option should be visible without clicking on the spinner
        SINGLE_APP_SELECTOR.assertVisible()
    }

    fun assertEntireScreenOptionIsDefault() {
        // Entire screen option should be visible without clicking on the spinner
        ENTIRE_SCREEN_SELECTOR.assertVisible()
    }

    fun assertSingleAppOptionDisabled() {
        waitForObj(SPINNER_SELECTOR).click()
        SINGLE_APP_DISABLED_SELECTOR.assertVisible()
        ensureThat("Single app is disabled") { !waitForObj(SINGLE_APP_SELECTOR).isEnabled }
    }

    fun assertSingleAppOptionEnabled() {
        waitForObj(SPINNER_SELECTOR).click()
        SINGLE_APP_DISABLED_SELECTOR.assertInvisible()
        ensureThat("Single app is enabled") { waitForObj(SINGLE_APP_SELECTOR).isEnabled }
    }

    fun assertSecondaryDisplayVisible() {
        waitForObj(SPINNER_SELECTOR).click()
        ENTIRE_SCREEN_RECORD_SECONDARY_SELECTOR.assertVisible()
    }

    companion object {
        private val DIALOG = sysuiResSelector("screen_share_permission_dialog")

        // Builds from 24Q3 and earlier will have screen_share_mode_spinner, while builds from
        // 24Q4 onwards will have screen_share_mode_options, so need to check both options here
        private val SPINNER_SELECTOR =
            By.res(Pattern.compile("$SYSUI_PACKAGE:id/screen_share_mode_(options|spinner)"))

        private val POP_UP = By.clazz("android.widget.ListView")
        private val SINGLE_APP_SELECTOR = By.text("Share one app")
        private val ENTIRE_SCREEN_SELECTOR = By.text("Share entire screen")
        private val ENTIRE_SCREEN_RECORD_SECONDARY_SELECTOR =
            By.text("Record entire screen: Overlay #1")
        private val SINGLE_APP_START_SELECTOR = By.text("Next")
        private val CANCEL_SELECTOR = By.text("Cancel")
        private val SINGLE_APP_DISABLED_SELECTOR = By.textEndsWith("has disabled this option")
        private const val APP_SELECTOR_ACTIVITY = "MediaProjectionAppSelectorActivity"
        private val SHORT_TIMEOUT = Duration.ofMillis(500)

        // Public as it is used in Root.java
        @JvmStatic
        fun assertSpinnerVisibility(visible: Boolean) {
            assertVisibility(SPINNER_SELECTOR, visible)
        }
    }
}
