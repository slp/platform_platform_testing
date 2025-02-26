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

import android.platform.systemui_tapl.utils.DeviceUtils.launcherDescSelector
import android.platform.systemui_tapl.utils.DeviceUtils.launcherResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import com.google.common.truth.Truth.assertThat

/**
 * Provides an API for interacting with the expanded bubble bar within launcher in UI automation
 * tests.
 *
 * @see [BubbleBar]
 */
class ExpandedBubbleBar(val selectedBubble: BubbleBarItem) {

    init {
        assertThat(bubbles.size).isAtLeast(1)
    }

    /** @return all the bubbles in the bubble bar. */
    val bubbles: List<BubbleBarItem>
        get() = waitForObj(BUBBLE_BAR).children.map { BubbleBarItem(it) }

    /** @return expanded view for the current bubble. */
    val expandedBubble: ExpandedBubbleBarBubble
        get() = ExpandedBubbleBarBubble()

    /** Collapses the bubble bar by tapping on the selected bubble and returns [BubbleBar]. */
    fun collapse(): BubbleBar {
        selectedBubble.item.click()
        return BubbleBar()
    }

    /**
     * Selects the specified [bubble] by tapping on it and returns a new instance of
     * [ExpandedBubbleBar].
     */
    fun select(bubble: BubbleBarItem): ExpandedBubbleBar {
        assertThat(bubble).isNotEqualTo(selectedBubble)
        bubble.item.click()
        return ExpandedBubbleBar(bubble)
    }

    companion object {
        private val BUBBLE_BAR = launcherResSelector("taskbar_bubbles")
        private val OVERFLOW_BUBBLE = launcherDescSelector("Overflow")
    }
}
