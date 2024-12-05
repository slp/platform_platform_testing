/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.platform.helpers.ui.UiAutomatorUtils.getUiDevice
import android.platform.systemui_tapl.utils.DeviceUtils.androidResSelector
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.stableBounds
import com.google.common.truth.Truth.assertThat

/**
 * System UI test automation object representing a media projection app selector. App selector is
 * launched when sharing a single app.
 */
class MediaProjectionAppSelector internal constructor() {

    init {
        CHOOSER_HEADER.assertVisible()
    }

    /** Dismiss app selector by pressing back. Make sure it is no longer visible */
    fun dismiss() {
        getUiDevice().pressBack()
        CHOOSER_HEADER.assertInvisible()
    }

    /** Make sure app selector is fully expanded */
    fun verifyIsExpanded() {
        // Assert full expansion when the top of the drawer touches the bottom of the status bar
        val headerTop = waitForObj(CHOOSER_HEADER).stableBounds.top
        val statusBarBottom = waitForObj(STATUS_BAR).stableBounds.bottom
        assertThat(headerTop).isEqualTo(statusBarBottom)
    }

    /** Width of the app selector drawer */
    val width: Int
        get() = waitForObj(CHOOSER_HEADER).stableBounds.width()

    companion object {
        private val CHOOSER_HEADER = androidResSelector("chooser_header")
        private val STATUS_BAR = sysuiResSelector("status_bar_container")
    }
}
