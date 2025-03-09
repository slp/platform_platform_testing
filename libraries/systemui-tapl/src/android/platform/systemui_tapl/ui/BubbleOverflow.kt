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
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import com.google.common.truth.Truth.assertThat
import java.time.Duration

/** System UI test automation object representing a bubble overflow panel. */
class BubbleOverflow internal constructor() {
    init {
        BUBBLE_OVERFLOW_CONTAINER.assertVisible(timeout = FIND_OBJECT_TIMEOUT)
    }

    /** Asserts that the panel is empty. */
    fun verifyIsEmpty() {
        BUBBLE_OVERFLOW_EMPTY_STATE_ID.assertVisible(timeout = FIND_OBJECT_TIMEOUT)
    }

    /** Asserts that the panel has at least one bubble. */
    fun verifyHasBubbles() {
        val recycler = waitForObj(BUBBLE_OVERFLOW_RECYCLER_ID, timeout = FIND_OBJECT_TIMEOUT)
        val overflowBubbleViews = recycler.findObjects(BUBBLE_OVERFLOW_VIEW)
        assertThat(overflowBubbleViews.size).isAtLeast(1)
    }

    private companion object {
        val FIND_OBJECT_TIMEOUT = Duration.ofSeconds(20)
        val BUBBLE_OVERFLOW_RECYCLER_ID = sysuiResSelector("bubble_overflow_recycler")
        val BUBBLE_OVERFLOW_EMPTY_STATE_ID = sysuiResSelector("bubble_overflow_empty_state")
        val BUBBLE_OVERFLOW_VIEW = sysuiResSelector("bubble_overflow_view")
        val BUBBLE_OVERFLOW_CONTAINER = sysuiResSelector("bubble_overflow_container")
    }
}
