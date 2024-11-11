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

import android.tools.flicker.ScenarioInstance
import android.tools.flicker.assertions.FlickerTest
import android.tools.flicker.assertors.ComponentTemplate
import android.tools.helpers.WindowUtils

/** Checks that [component] window aligns with only one of the display app corners at the end. */
class AppWindowAlignsWithOnlyOneDisplayCornerAtEnd(private val component: ComponentTemplate) :
    AssertionTemplateWithComponent(component) {
    /** {@inheritDoc} */
    override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
        flicker.assertWmEnd {
            val displayAppBounds = WindowUtils.getInsetDisplayBounds()
            val windowBounds = visibleRegion(component.build(scenarioInstance)).region.bounds

            val onRightSide = windowBounds.right == displayAppBounds.right
            val onLeftSide = windowBounds.left == displayAppBounds.left
            val onTopSide = windowBounds.top == displayAppBounds.top
            val onBottomSide = windowBounds.bottom == displayAppBounds.bottom
            val alignedOnCorners = onRightSide.xor(onLeftSide) and onTopSide.xor(onBottomSide)

            check { "window corner must meet display corner" }.that(alignedOnCorners).isEqual(true)
        }
    }
}
