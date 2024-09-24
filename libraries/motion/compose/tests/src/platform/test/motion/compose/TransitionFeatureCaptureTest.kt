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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Up
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.compose.DataPointTypes.intSize
import platform.test.motion.compose.TransitionFeatureCapture.animatedVisibilityValues
import platform.test.motion.compose.values.MotionTestValueKey
import platform.test.motion.compose.values.motionTestValues
import platform.test.motion.golden.DataPoint
import platform.test.motion.testing.DataPointTypeSubject.Companion.assertThat
import platform.test.motion.testing.createGoldenPathManager

@RunWith(AndroidJUnit4::class)
class TransitionFeatureCaptureTest {

    private val pathManager =
        createGoldenPathManager("platform_testing/libraries/motion/compose/tests/goldens")
    @get:Rule val motionRule = createComposeMotionTestRule(pathManager)

    @Test
    fun recordMotion_AnimatedContent_capturesDefaultTransition() =
        transitionGoldenTest(
            content = { transition ->
                transition.AnimatedContent(modifier = Modifier.subject(transition)) { targetState ->
                    Text(text = targetState)
                }
            },
            ComposeRecordingSpec.duringTransition(transitionKey) {
                animatedVisibilityFeatures(transitionKey)
            },
        )

    @Test
    fun recordMotion_AnimatedContent_capturesFade() =
        transitionGoldenTest(
            content = { transition ->
                transition.AnimatedContent(
                    modifier = Modifier.subject(transition),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                ) { targetState ->
                    Text(text = targetState)
                }
            },
            ComposeRecordingSpec.duringTransition(transitionKey) {
                animatedVisibilityFeatures(transitionKey)
            },
        )

    @Test
    fun recordMotion_AnimatedContent_capturesSlide() =
        transitionGoldenTest(
            content = { transition ->
                transition.AnimatedContent(
                    modifier = Modifier.subject(transition),
                    transitionSpec = { slideIntoContainer(Up) togetherWith slideOutOfContainer(Up) },
                ) { targetState ->
                    Text(text = targetState)
                }
            },
            ComposeRecordingSpec.duringTransition(transitionKey) {
                animatedVisibilityFeatures(transitionKey)
            },
        )

    @Test
    fun recordMotion_twoAnimatedContents_allowsNamingTransitions() =
        transitionGoldenTest(
            content = { transition ->
                Column(modifier = Modifier.motionTestValues { transition exportAs transitionKey }) {
                    transition.AnimatedContent(
                        modifier = Modifier.size(50.dp),
                        transitionSpec = {
                            fadeIn(tween(durationMillis = 200, delayMillis = 200)) togetherWith
                                fadeOut(tween(durationMillis = 100))
                        },
                    ) { targetState ->
                        Text(text = targetState)
                    }

                    transition.AnimatedContent(
                        modifier = Modifier.size(50.dp),
                        transitionSpec = {
                            fadeIn(tween(durationMillis = 200, delayMillis = 300)) togetherWith
                                fadeOut(tween(durationMillis = 100))
                        },
                    ) { targetState ->
                        Text(text = targetState)
                    }
                }
            },
            ComposeRecordingSpec.duringTransition(transitionKey) {
                animatedVisibilityFeatures(transitionKey) {
                    when (it) {
                        0 -> "one"
                        1 -> "two"
                        else -> throw IllegalArgumentException()
                    }
                }
            },
        )

    @Test
    fun animatedVisibilityValues_jsonConversion_notFoundDoesNotWriteJson() {
        assertThat(animatedVisibilityValues)
            .convertsJsonObject(
                AnimatedVisibilityValues(
                    DataPoint.notFound(),
                    DataPoint.notFound(),
                    DataPoint.notFound(),
                    DataPoint.notFound(),
                ),
                """{}""",
            )

        assertThat(intSize).invalidJsonReturnsUnknownDataPoint(JSONObject(), 1)
    }

    @Test
    fun animatedVisibilityValues_jsonConversion_nullDataPointWritesNull() {
        assertThat(animatedVisibilityValues)
            .convertsJsonObject(
                AnimatedVisibilityValues(
                    DataPoint.nullValue(),
                    DataPoint.nullValue(),
                    DataPoint.nullValue(),
                    DataPoint.nullValue(),
                ),
                """{"alpha":null,"slide":null,"scale":null,"size":null}""",
            )

        assertThat(intSize).invalidJsonReturnsUnknownDataPoint(JSONObject(), 1)
    }

    @Test
    fun animatedVisibilityValues_jsonConversion_actial() {
        assertThat(animatedVisibilityValues)
            .convertsJsonObject(
                AnimatedVisibilityValues(
                    DataPoint.nullValue(),
                    DataPoint.nullValue(),
                    DataPoint.nullValue(),
                    DataPoint.nullValue(),
                ),
                """{"alpha":null,"slide":null,"scale":null,"size":null}""",
            )

        assertThat(intSize).invalidJsonReturnsUnknownDataPoint(JSONObject(), 1)
    }

    @Test
    fun animatedVisibilityValues_invalid_returnsUnknown() {
        assertThat(animatedVisibilityValues).invalidJsonReturnsUnknownDataPoint(JSONArray(), 1)
    }

    private fun Modifier.subject(transition: Transition<String>) =
        this then Modifier.size(50.dp).motionTestValues { transition exportAs transitionKey }

    private fun transitionGoldenTest(
        content: @Composable (transition: Transition<String>) -> Unit,
        recordingSpec: ComposeRecordingSpec,
    ) {

        motionRule.runTest {
            val motion =
                recordMotion(
                    content = { play ->
                        val transition =
                            updateTransition(
                                targetState = if (play) "B" else "A",
                                label = "subject",
                            )
                        content(transition)
                    },
                    recordingSpec = recordingSpec,
                )

            assertThat(motion).timeSeriesMatchesGolden()
        }
    }

    companion object {
        val transitionKey = MotionTestValueKey<Transition<String>>("test_transition")
    }
}
