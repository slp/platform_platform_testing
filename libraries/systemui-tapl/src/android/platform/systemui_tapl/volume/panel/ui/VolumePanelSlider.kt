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

package android.platform.systemui_tapl.volume.panel.ui

import android.platform.systemui_tapl.utils.DeviceUtils
import android.platform.uiautomator_helpers.DeviceHelpers.waitForObj
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2

/**
 * An access point for the Volume Panel slider.
 *
 * @see VolumePanel.slider
 */
interface VolumePanelSlider {

    /** Scrolls until the [targetProgress] is met. */
    fun scrollToProgress(targetProgress: Float)
}

internal class VolumePanelSliderImpl(container: UiObject2, @SliderStream streamType: Int) :
    VolumePanelSlider {

    private val sliderObject: UiObject2 =
        container.waitForObj(DeviceUtils.sysuiResSelector(SliderStream.getResourceId(streamType))) {
            "Can't find slider=$streamType"
        }

    override fun scrollToProgress(targetProgress: Float) {
        val currentProgress = sliderObject.getProgress()
        val direction =
            when {
                targetProgress > currentProgress -> Direction.LEFT
                targetProgress < currentProgress -> Direction.RIGHT
                else -> return
            }
        sliderObject.scrollUntil(direction) { obj -> obj.getProgress() == targetProgress }
    }

    // TODO: b/349817024 - Remove this method in favour of the new API
    private fun UiObject2.getProgress(): Float =
        with(javaClass.getDeclaredMethod("getAccessibilityNodeInfo")) {
            val originalIsAccessible = isAccessible
            return try {
                isAccessible = true
                val rangeInfo =
                    (invoke(this@getProgress) as AccessibilityNodeInfo).rangeInfo
                        ?: throw RuntimeException(
                            "No RangeInfo found for the object=[$contentDescription, $resourceName]"
                        )
                rangeInfo.current
            } finally {
                isAccessible = originalIsAccessible
            }
        }
}
