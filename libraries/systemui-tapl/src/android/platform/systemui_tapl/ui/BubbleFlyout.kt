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
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import java.time.Duration

/**
 * System UI test automation object representing a notification bubble flyout popup that appears
 * after posting or visually updating a bubble notification.
 */
class BubbleFlyout internal constructor() {
    init {
        BUBBLE_FLYOUT_TEXT_CONTAIER_VIEW.assertVisible(timeout = TIMEOUT)
    }

    /** Fails if the flyout doesn't auto-close */
    fun verifyAutoClosing() {
        BUBBLE_FLYOUT_TEXT_CONTAIER_VIEW.assertInvisible(timeout = TIMEOUT) {
            "Flyout didn't auto close"
        }
    }

    private companion object {
        val BUBBLE_FLYOUT_TEXT_CONTAIER_VIEW = sysuiResSelector("bubble_flyout_text_container")
        val TIMEOUT = Duration.ofSeconds(20)
    }
}
