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

import android.view.LayoutInflater
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.MotionTestRule
import platform.test.motion.testing.SampleScene
import platform.test.motion.testing.createGoldenPathManager
import platform.test.motion.tests.R
import platform.test.motion.view.AnimationSampling.Companion.evenlySampled
import platform.test.motion.view.ViewRecordingSpec.Companion.capture
import platform.test.motion.view.ViewRecordingSpec.Companion.captureWithoutScreenshot
import platform.test.screenshot.DeviceEmulationRule
import platform.test.screenshot.DeviceEmulationSpec
import platform.test.screenshot.DisplaySpec
import platform.test.screenshot.ScreenshotActivity
import platform.test.screenshot.ScreenshotTestRule

@SmallTest
@RunWith(AndroidJUnit4::class)
class ViewToolkitTest {

    private val goldenPathManager =
        createGoldenPathManager("platform_testing/libraries/motion/tests/assets")

    @get:Rule(order = 0) val deviceEmulationRule = DeviceEmulationRule(emulationSpec)
    @get:Rule(order = 1) val screenshotRule = ScreenshotTestRule(goldenPathManager)
    @get:Rule(order = 2) val activityRule = ActivityScenarioRule(ScreenshotActivity::class.java)
    @get:Rule(order = 3)
    val motionRule =
        MotionTestRule(
            ViewToolkit { activityRule.scenario },
            goldenPathManager,
            bitmapDiffer = screenshotRule,
        )

    @Test
    fun record_timeseries_withAnimator() {
        val sceneRoot = createSampleScene()
        val animator = sceneRoot.createSlideLeftAnimator()

        val recordedMotion =
            motionRule.record(
                animator,
                sceneRoot.capture(evenlySampled(10)) {
                    onViewWithId(R.id.test_box) { feature(ViewFeatureCaptures.x, "box_x") }
                }
            )

        motionRule.assertThat(recordedMotion).timeSeriesMatchesGolden("timeseries_simple_scene_box")
    }

    @Test
    fun record_timeseries_noVisualCapture() {
        val sceneRoot = createSampleScene()
        val animator = sceneRoot.createSlideLeftAnimator()

        val recordedMotion =
            motionRule.record(
                animator,
                sceneRoot.captureWithoutScreenshot(evenlySampled(10)) {
                    onViewWithId(R.id.test_box) { feature(ViewFeatureCaptures.x, "box_x") }
                }
            )

        motionRule.assertThat(recordedMotion).timeSeriesMatchesGolden("timeseries_simple_scene_box")
    }

    @Test
    @Ignore("Generated image size is not yet consistent")
    fun recordedMotion_filmstripMatchesGolden() {
        val sceneRoot = createSampleScene()
        val animator = sceneRoot.createSlideLeftAnimator()

        val recordedMotion = motionRule.record(animator, sceneRoot.capture(evenlySampled(10)) {})

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
