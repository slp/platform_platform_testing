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

package platform.test.motion.view

import android.platform.uiautomator_helpers.DeviceHelpers.context
import android.view.LayoutInflater
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.Sampling.Companion.evenlySampled
import platform.test.motion.testing.SampleScene
import platform.test.motion.tests.R
import platform.test.screenshot.DeviceEmulationRule
import platform.test.screenshot.DeviceEmulationSpec
import platform.test.screenshot.DisplaySpec
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.PathConfig
import platform.test.screenshot.ScreenshotActivity
import platform.test.screenshot.ScreenshotTestRule

@SmallTest
@RunWith(AndroidJUnit4::class)
class ViewMotionTestRuleTest {

    private val pathManager = GoldenPathManager(context, ASSETS_PATH, pathConfig = PathConfig())

    @get:Rule(order = 0) val deviceEmulationRule = DeviceEmulationRule(emulationSpec)
    @get:Rule(order = 1) val screenshotRule = ScreenshotTestRule(pathManager)
    @get:Rule(order = 2) val activityRule = ActivityScenarioRule(ScreenshotActivity::class.java)
    @get:Rule(order = 3)
    val motionRule =
        ViewMotionTestRule<ScreenshotActivity>(
            pathManager,
            { activityRule.scenario },
            bitmapDiffer = screenshotRule,
        )

    @Test
    fun record_timeseries_withAnimator() {
        val sceneRoot = createSampleScene()
        val animator = sceneRoot.createSlideLeftAnimator()

        val recordedMotion =
            motionRule.checkThat(animator).record(sceneRoot, evenlySampled(10)) {
                onViewWithId(R.id.test_box) { capture(ViewFeatureCaptures.x, "box_x") }
            }

        motionRule.assertThat(recordedMotion).timeSeriesMatchesGolden("timeseries_simple_scene_box")
    }

    @Test
    fun record_timeseries_noVisualCapture() {
        val sceneRoot = createSampleScene()
        val animator = sceneRoot.createSlideLeftAnimator()

        val recordedMotion =
            motionRule.checkThat(animator).record(
                sceneRoot,
                evenlySampled(10),
                visualCapture = null
            ) {
                onViewWithId(R.id.test_box) { capture(ViewFeatureCaptures.x, "box_x") }
            }

        motionRule.assertThat(recordedMotion).timeSeriesMatchesGolden("timeseries_simple_scene_box")
    }

    @Test
    fun recordedMotion_filmstripMatchesGolden() {
        val sceneRoot = createSampleScene()
        val animator = sceneRoot.createSlideLeftAnimator()

        val recordedMotion =
            motionRule.checkThat(animator).record(sceneRoot, evenlySampled(10)) {
                onViewWithId(R.id.test_box) { capture(ViewFeatureCaptures.x, "box_x") }
            }

        motionRule.assertThat(recordedMotion).filmstripMatchesGolden()
    }

    private fun createSampleScene(): SampleScene {
        lateinit var sceneRoot: SampleScene
        activityRule.scenario.onActivity { activity ->
            sceneRoot =
                LayoutInflater.from(activity).inflate(R.layout.test_box_scene, null) as SampleScene
            activity.setContentView(sceneRoot)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return sceneRoot
    }

    companion object {
        private const val ASSETS_PATH = "platform_testing/libraries/motion/androidTest/assets"

        private val emulationSpec =
            DeviceEmulationSpec(
                DisplaySpec(
                    "phone",
                    width = 320,
                    height = 690,
                    densityDpi = 160,
                )
            )
    }
}
