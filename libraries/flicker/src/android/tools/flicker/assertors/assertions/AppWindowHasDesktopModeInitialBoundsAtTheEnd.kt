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

package android.tools.flicker.assertors.assertions

import android.graphics.Rect
import android.os.SystemProperties
import android.tools.PlatformConsts.DESKTOP_MODE_INITIAL_WINDOW_HEIGHT_PROPORTION
import android.tools.flicker.ScenarioInstance
import android.tools.flicker.assertions.FlickerTest
import android.tools.flicker.assertors.ComponentTemplate
import android.tools.helpers.WindowUtils

class AppWindowHasDesktopModeInitialBoundsAtTheEnd(private val component: ComponentTemplate) :
    AssertionTemplateWithComponent(component) {

    /** {@inheritDoc} */
    override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
        flicker.assertLayersEnd {
            val displayBounds =
                entry.physicalDisplayBounds ?: error("Missing physical display bounds")
            val stableBounds = WindowUtils.getInsetDisplayBounds()
            val desktopModeInitialBoundsScale =
                SystemProperties.getInt("persist.wm.debug.desktop_mode_initial_bounds_scale", 75) /
                    100f

            val desiredWidth = displayBounds.width().times(desktopModeInitialBoundsScale)
            val desiredHeight = displayBounds.height().times(desktopModeInitialBoundsScale)

            val outBounds = Rect(0, 0, desiredWidth.toInt(), desiredHeight.toInt())
            val xOffset = ((stableBounds.width() - desiredWidth) / 2).toInt()
            val yOffset =
                ((stableBounds.height() - desiredHeight) *
                        DESKTOP_MODE_INITIAL_WINDOW_HEIGHT_PROPORTION + stableBounds.top)
                    .toInt()
            // Position the task in screen bounds
            outBounds.offset(xOffset, yOffset)

            visibleRegion(component.build(scenarioInstance)).coversExactly(outBounds)
        }
    }
}
