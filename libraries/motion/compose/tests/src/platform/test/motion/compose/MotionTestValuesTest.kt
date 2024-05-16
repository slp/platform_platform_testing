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
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.compose.values.EnableMotionTestValueCollection
import platform.test.motion.compose.values.MotionTestValueKey
import platform.test.motion.compose.values.motionTestValues
import platform.test.motion.golden.DataPoint
import platform.test.motion.golden.DataPointTypes
import platform.test.motion.golden.FeatureCapture
import platform.test.motion.golden.TimeSeries
import platform.test.motion.golden.TimeSeriesCaptureScope
import platform.test.motion.golden.asDataPoint
import platform.test.motion.testing.createGoldenPathManager

@RunWith(AndroidJUnit4::class)
class MotionTestValuesTest {
    private val pathManager =
        createGoldenPathManager("platform_testing/libraries/motion/compose/tests/goldens")

    @get:Rule val motionRule = createComposeMotionTestRule(pathManager)

    private val composeRule: ComposeContentTestRule
        get() = motionRule.toolkit.composeContentTestRule

    @Test
    fun motionTestValueOfNode_readsValueOfAnnotatedNode() {
        composeRule.setContent {
            EnableMotionTestValueCollection { Box(Modifier.motionTestValues { 0.5f exportAs foo }) }
        }

        assertThat(composeRule.motionTestValueOfNode(foo)).isEqualTo(0.5f)
    }

    @Test
    fun motionTestValueOfNode_multipleMatchingNodes_throws() {
        composeRule.setContent {
            EnableMotionTestValueCollection {
                Box(Modifier.motionTestValues { 0.5f exportAs foo })
                Box(Modifier.motionTestValues { 0.6f exportAs foo })
            }
        }
        Assert.assertThrows(AssertionError::class.java) { composeRule.motionTestValueOfNode(foo) }
    }

    @Test
    fun motionTestValueOfNode_valueNotExported_throws() {
        composeRule.setContent { EnableMotionTestValueCollection { Box(Modifier.testTag("foo")) } }
        Assert.assertThrows(IllegalStateException::class.java) {
            composeRule.motionTestValueOfNode(foo, hasTestTag("foo"))
        }
    }

    @Test
    fun motionTestValueOfNode_specifySelector() {
        composeRule.setContent {
            EnableMotionTestValueCollection {
                Box(Modifier.motionTestValues { 0.5f exportAs foo }.testTag("foo"))
                Box(Modifier.motionTestValues { 0.6f exportAs foo }.testTag("bar"))
            }
        }

        assertThat(composeRule.motionTestValueOfNode(foo, matcher = hasTestTag("foo")))
            .isEqualTo(0.5f)
    }

    @Test
    fun featureWithDataPointType_defaultMatcherAndName() =
        motionRule.runTest {
            val motion =
                recordMotion(
                    content = { Box(Modifier.size(10.dp).motionTestValues { .5f exportAs foo }) },
                    singleFrame { feature(foo, DataPointTypes.float) }
                )

            motion.timeSeries.assertSingleFeatureMatches("foo", .5f.asDataPoint())
        }

    @Test
    fun featureWithDataPointType_matchingViaTestTag() =
        motionRule.runTest {
            val motion =
                recordMotion(
                    content = {
                        Box(
                            Modifier.size(10.dp).testTag("foo").motionTestValues {
                                .5f exportAs foo
                            }
                        )
                    },
                    singleFrame { feature(foo, DataPointTypes.float, matcher = hasTestTag("foo")) }
                )

            motion.timeSeries.assertSingleFeatureMatches("foo", .5f.asDataPoint())
        }

    @Test
    fun featureWithDataPointType_unknownNode() =
        motionRule.runTest {
            val motion =
                recordMotion(
                    content = { Box(Modifier.size(10.dp).motionTestValues { .5f exportAs foo }) },
                    singleFrame {
                        feature(foo, DataPointTypes.float, matcher = hasTestTag("unknown"))
                    }
                )

            motion.timeSeries.assertSingleFeatureMatches("foo", DataPoint.notFound())
        }

    @Test
    fun featureWithDataPointType_withCustomName() =
        motionRule.runTest {
            val motion =
                recordMotion(
                    content = { Box(Modifier.size(10.dp).motionTestValues { .5f exportAs foo }) },
                    singleFrame { feature(foo, DataPointTypes.float, name = "bar") }
                )

            motion.timeSeries.assertSingleFeatureMatches("bar", .5f.asDataPoint())
        }

    @Test
    fun featureWithFeatureCapture_defaultMatcherAndName() =
        motionRule.runTest {
            val motion =
                recordMotion(
                    content = { Box(Modifier.size(10.dp).motionTestValues { .5f exportAs foo }) },
                    singleFrame { feature(foo, times3) }
                )

            motion.timeSeries.assertSingleFeatureMatches("foo_times3", 1.5f.asDataPoint())
        }

    @Test
    fun featureWithFeatureCapture_matchingViaTestTag() =
        motionRule.runTest {
            val motion =
                recordMotion(
                    content = {
                        Box(
                            Modifier.size(10.dp).testTag("foo").motionTestValues {
                                .5f exportAs foo
                            }
                        )
                    },
                    singleFrame { feature(foo, times3, matcher = hasTestTag("foo")) }
                )

            motion.timeSeries.assertSingleFeatureMatches("foo_times3", 1.5f.asDataPoint())
        }

    @Test
    fun featureWithFeatureCapture_unknownNode() =
        motionRule.runTest {
            val motion =
                recordMotion(
                    content = { Box(Modifier.size(10.dp).motionTestValues { .5f exportAs foo }) },
                    singleFrame { feature(foo, times3, matcher = hasTestTag("unknown")) }
                )

            motion.timeSeries.assertSingleFeatureMatches("foo_times3", DataPoint.notFound())
        }

    @Test
    fun featureWithFeatureCapture_withCustomName() =
        motionRule.runTest {
            val motion =
                recordMotion(
                    content = { Box(Modifier.size(10.dp).motionTestValues { .5f exportAs foo }) },
                    singleFrame { feature(foo, times3, name = "bar") }
                )

            motion.timeSeries.assertSingleFeatureMatches("bar", 1.5f.asDataPoint())
        }

    /**
     * Asserts the time series contains a single float feature named [featureName], matching the
     * expected data points.
     */
    private fun TimeSeries.assertSingleFeatureMatches(
        featureName: String,
        vararg expectedDataPoints: DataPoint<Float>
    ) {
        assertThat(features.keys).containsExactly(featureName)
        val feature = checkNotNull(features[featureName])
        assertThat(feature.dataPoints).containsExactly(*expectedDataPoints).inOrder()
    }

    private fun singleFrame(
        timeSeriesCapture: TimeSeriesCaptureScope<SemanticsNodeInteractionsProvider>.() -> Unit,
    ): ComposeRecordingSpec {
        return ComposeRecordingSpec(
            recording = { awaitFrames(1) },
            recordBefore = false,
            recordAfter = false,
            timeSeriesCapture
        )
    }

    companion object {
        val foo = MotionTestValueKey<Float>("foo")
        val times3 = FeatureCapture<Float, Float>("times3") { (it * 3).asDataPoint() }
    }
}
