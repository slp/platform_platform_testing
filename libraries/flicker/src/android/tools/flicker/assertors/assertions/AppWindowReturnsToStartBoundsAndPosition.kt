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
import kotlin.math.abs

class AppWindowReturnsToStartBoundsAndPosition(private val component: ComponentTemplate) :
    AssertionTemplateWithComponent(component) {
    override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
        val matcher = component.get(scenarioInstance)
        flicker.assertLayers {
            val startRegion = first().visibleRegion(matcher)
            val endRegion = last().visibleRegion(matcher)

            val heightDiff = startRegion.region.bounds.height() - endRegion.region.bounds.height()
            val widthDiff = startRegion.region.bounds.width() - endRegion.region.bounds.width()
            val xDiff = startRegion.region.bounds.centerX() - endRegion.region.bounds.centerX()

            check { "height" }.that(abs(heightDiff)).isLowerOrEqual(1)
            check { "width" }.that(abs(widthDiff)).isLowerOrEqual(1)
            check { "horizontal position" }.that(abs(xDiff)).isGreaterOrEqual(2)
        }
    }
}
