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

package platform.test.motion.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.compose.values.EnableMotionTestValueCollection
import platform.test.motion.compose.values.MotionTestValues
import platform.test.motion.compose.values.motionTestValues
import platform.test.motion.testing.DataPointSubject.Companion.assertThat
import platform.test.screenshot.DeviceEmulationRule
import platform.test.screenshot.DeviceEmulationSpec
import platform.test.screenshot.DisplaySpec

@RunWith(AndroidJUnit4::class)
class ComposeFeatureCapturesTest {

    // set dp/pixel ratio to 1:2
    private val emulationSpec =
        DeviceEmulationSpec(DisplaySpec("phone", width = 400, height = 800, densityDpi = 320))
    @get:Rule(order = 0) val deviceEmulationRule = DeviceEmulationRule(emulationSpec)

    @get:Rule(order = 1) val composeRule = createComposeRule()

    @Test
    fun dpSize_capturesDataPoint() {
        composeRule.setContent { Box(Modifier.testTag("box").size(width = 10.dp, height = 20.dp)) }

        val semanticsNode = composeRule.onNode(hasTestTag("box")).fetchSemanticsNode()
        assertThat(ComposeFeatureCaptures.dpSize.capture(semanticsNode))
            .hasNativeValue(DpSize(10.dp, 20.dp))
    }

    @Test
    fun size_capturesDataPoint() {
        composeRule.setContent { Box(Modifier.testTag("box").size(width = 10.dp, height = 20.dp)) }

        val semanticsNode = composeRule.onNode(hasTestTag("box")).fetchSemanticsNode()
        assertThat(ComposeFeatureCaptures.size.capture(semanticsNode))
            .hasNativeValue(IntSize(20, 40))
    }

    @Test
    fun positionInRoot_capturesDataPoint() {
        composeRule.setContent {
            Box(Modifier.offset(x = 10.dp, y = 20.dp)) {
                Box(Modifier.offset(x = 1.dp, y = 2.dp).testTag("box"))
            }
        }

        val semanticsNode = composeRule.onNode(hasTestTag("box")).fetchSemanticsNode()
        assertThat(ComposeFeatureCaptures.positionInRoot.capture(semanticsNode))
            .hasNativeValue(DpOffset(11.dp, 22.dp))
    }

    @Test
    fun alpha_capturesDataPoint() {
        composeRule.setContent {
            EnableMotionTestValueCollection {
                val opacity = 0.5f
                Box(
                    Modifier.graphicsLayer { alpha = opacity }
                        .motionTestValues { opacity exportAs MotionTestValues.alpha }
                        .testTag("box")
                )
            }
        }

        val semanticsNode = composeRule.onNode(hasTestTag("box")).fetchSemanticsNode()
        assertThat(ComposeFeatureCaptures.alpha.capture(semanticsNode)).hasNativeValue(0.5f)
    }

    @Test
    fun alpha_withoutExport_throws() {
        composeRule.setContent {
            val opacity = 0.5f
            Box(Modifier.graphicsLayer { alpha = opacity }.testTag("box"))
        }

        val semanticsNode = composeRule.onNode(hasTestTag("box")).fetchSemanticsNode()

        assertThrows(IllegalStateException::class.java) {
            ComposeFeatureCaptures.alpha.capture(semanticsNode)
        }
    }
}
