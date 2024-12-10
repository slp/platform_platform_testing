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

import android.graphics.PointF
import android.platform.systemui_tapl.utils.DeviceUtils.LONG_WAIT
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.BetterSwipe.from
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisibility
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.PRECISE_GESTURE_INTERPOLATOR
import androidx.test.uiautomator.UiObject2
import com.android.systemui.Flags
import com.google.common.truth.Truth.assertThat
import java.time.Duration

/** System UI test automation object representing the quick settings' brightness slider. */
class BrightnessSlider internal constructor() {
    private val slider: UiObject2

    init {
        val selector = sysuiResSelector(UI_BRIGHTNESS_SLIDER_ID)
        slider =
            waitForObj(selector, LONG_WAIT) { "$selector not found" }
                .waitForObj(sysuiResSelector(UI_TOGGLE_SEEKBAR_ID))
    }

    /** Slides from left to right */
    fun swipeFromLeftToRight() {
        val sliderBounds = slider.visibleBounds
        val pointFrom =
            PointF(
                (sliderBounds.centerX() - sliderBounds.width() / 3).toFloat(),
                sliderBounds.centerY().toFloat(),
            )
        val pointTo =
            PointF(
                (sliderBounds.centerX() + sliderBounds.width() / 3).toFloat(),
                sliderBounds.centerY().toFloat(),
            )
        val swipe =
            from(pointFrom).to(pointTo, Duration.ofMillis(500), PRECISE_GESTURE_INTERPOLATOR)
        if (Flags.qsUiRefactorComposeFragment()) {
            // In this case, the slider is moved to an overlay, then we verify:
            // The notification shade is not visible, but
            assertVisibility(sysuiResSelector(UI_NOTIFICATION_SHADE_ID), visible = false)
            // The actual slider is visible, and
            assertVisibility(sysuiResSelector(UI_BRIGHTNESS_SLIDER_ID), visible = true)
            // The bounds haven't changed.
            assertThat(slider.visibleBounds).isEqualTo(sliderBounds)
            swipe.release()
        } else {
            val mirrorBounds = brightnessSliderMirror.visibleBounds
            assertThat(sliderBounds).isEqualTo(mirrorBounds)
            swipe.release()
            assertThat(mirrorBounds).isEqualTo(slider.visibleBounds)
        }
    }

    // The Mirror slider has the same id as the original one, so we get it from the container
    private val brightnessSliderMirror: UiObject2
        get() {
            // The Mirror slider has the same id as the original one, so we get it from the
            // container
            return waitForObj(sysuiResSelector(UI_BRIGHTNESS_MIRROR_CONTAINER_ID))
                .waitForObj(sysuiResSelector(UI_TOGGLE_SEEKBAR_ID))
        }

    private companion object {
        const val UI_TOGGLE_SEEKBAR_ID = "slider"
        const val UI_BRIGHTNESS_SLIDER_ID = "brightness_slider"
        const val UI_BRIGHTNESS_MIRROR_CONTAINER_ID = "brightness_mirror_container"
        const val UI_NOTIFICATION_SHADE_ID = "notification_shade"
    }
}
