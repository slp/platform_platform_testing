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

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.google.common.truth.IterableSubject
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import platform.test.motion.MotionTestRule
import platform.test.motion.compose.values.MotionTestValueKey
import platform.test.motion.compose.values.MotionTestValues
import platform.test.motion.compose.values.motionTestValues
import platform.test.motion.golden.DataPointTypes
import platform.test.motion.golden.NotFoundDataPoint
import platform.test.motion.golden.ValueDataPoint
import platform.test.motion.testing.createGoldenPathManager
import platform.test.screenshot.DeviceEmulationRule
import platform.test.screenshot.DeviceEmulationSpec
import platform.test.screenshot.Displays

class ComposeToolkitTest {
    private val pathManager =
        createGoldenPathManager("platform_testing/libraries/motion/compose/tests/goldens")
    private val deviceEmulationSpec = DeviceEmulationSpec(Displays.Phone)

    @get:Rule(order = 0) val deviceEmulationRule = DeviceEmulationRule(deviceEmulationSpec)
    @get:Rule(order = 1) val composeRule = createComposeRule()
    @get:Rule(order = 2) val motionRule = MotionTestRule(ComposeToolkit(composeRule), pathManager)

    @Test
    fun recordMotion_capturePosition() {
        var completed = false

        val motion =
            motionRule.recordMotion(
                content = { play ->
                    val offset by animateDpAsState(if (play) 90.dp else 0.dp) { completed = true }
                    Box(
                        modifier =
                            Modifier.offset(x = offset)
                                .testTag("foo")
                                .size(10.dp)
                                .background(Color.Red)
                    )
                },
                ComposeRecordingSpec.until({ completed }) {
                    feature(hasTestTag("foo"), ComposeFeatureCaptures.positionInRoot)
                }
            )

        motionRule.assertThat(motion).timeSeriesMatchesGolden()
    }

    @Test
    fun recordMotion_captureSize() {
        var completed = false

        val motion =
            motionRule.recordMotion(
                content = { play ->
                    Box(
                        modifier =
                            Modifier.testTag("foo")
                                .animateContentSize { _, _ -> completed = true }
                                .width(if (play) 90.dp else 10.dp)
                                .height(10.dp)
                                .background(Color.Red)
                    )
                },
                ComposeRecordingSpec.until({ completed }) {
                    feature(hasTestTag("foo"), ComposeFeatureCaptures.dpSize)
                }
            )

        motionRule.assertThat(motion).timeSeriesMatchesGolden()
    }

    @Test
    fun recordMotion_captureAlpha() {
        var completed = false

        val motion =
            motionRule.recordMotion(
                content = { play ->
                    val opacity by animateFloatAsState(if (play) 1f else 0f) { completed = true }
                    Box(
                        modifier =
                            Modifier.graphicsLayer { alpha = opacity }
                                .size(10.dp)
                                .fillMaxHeight()
                                .background(Color.Red)
                                .testTag("BoxOfInterest")
                                .motionTestValues { opacity exportAs MotionTestValues.alpha }
                    )
                },
                ComposeRecordingSpec.until({ completed }) {
                    feature(hasTestTag("BoxOfInterest"), ComposeFeatureCaptures.alpha)
                }
            )

        motionRule.assertThat(motion).timeSeriesMatchesGolden()
    }

    @Test
    fun recordMotion_captureCrossfade() {
        var completed = false

        val motion =
            motionRule.recordMotion(
                content = { play ->
                    val opacity by animateFloatAsState(if (play) 1f else 0f) { completed = true }

                    Box(
                        modifier =
                            Modifier.graphicsLayer { alpha = opacity }
                                .size(10.dp)
                                .fillMaxHeight()
                                .background(Color.Red)
                                .testTag("bar")
                                .motionTestValues { opacity exportAs MotionTestValues.alpha }
                    )
                    Box(
                        modifier =
                            Modifier.graphicsLayer { alpha = 1 - opacity }
                                .size(10.dp)
                                .fillMaxHeight()
                                .background(Color.Blue)
                                .testTag("foo")
                                .motionTestValues { (1f - opacity) exportAs MotionTestValues.alpha }
                    )
                },
                ComposeRecordingSpec.until({ completed }) {
                    feature(hasTestTag("bar"), ComposeFeatureCaptures.alpha, name = "bar_alpha")
                    feature(hasTestTag("foo"), ComposeFeatureCaptures.alpha, name = "foo_alpha")
                }
            )

        motionRule.assertThat(motion).timeSeriesMatchesGolden()
    }

    /**
     * Helper to assert the exact timing of the recording.
     *
     * Returns an [IterableSubject] identifying the frames *recorded*.
     *
     * This helper runs `recordMotion(testContent, motionControl)`. The testContent is Composable
     * that increases a frame counter on each animation frame. A time series of that frame count
     * value is what is captured and returned for assertion on the [IterableSubject].
     *
     * The frame count will start at 0 for the first composition, and increase up to 16, at which
     * point the `testContent` composable will be idle.
     *
     * The observed frame counts is offset by `100` as soon as the `play` flag is true, to make it
     * easy to assert the exact timing of the flipping. When [recordBefore] or [recordAfter] are set
     * to true, the first/last entry in the returned sequence denote the recorded before/after
     * frames.
     *
     * The actual captured frames reflect the internals of the motion test, as the
     * [MotionControlImpl] cycles through the [MotionControlState]s. The following chart illustrates
     * this.
     *
     * ```
     * Clock         :  0ms  16ms 32ms 48ms  ... 256ms
     * Frame count   :  0    1    2    103   ... 116
     * Animation Time:  -    0ms  16ms 32ms  ... 240ms
     * Events        :  │    │    │  │ │         └ testContent is idle (animation finished)
     *                  │    │    │  │ └  motionControl in `Recording` state
     *                  │    │    │  └ before state captured, flipping `play` to true
     *                  │    │    └  motionControl in `ReadyToPlay`
     *                  │    └ Animation first frame (value 0), motionControl in `Start` state
     *                  └ testContent enters composition, rule calls `waitForIdle`
     * ```
     */
    private fun assertThatFrameCountValues(
        recordBefore: Boolean,
        recordAfter: Boolean,
        motionControl: suspend MotionControlScope.() -> Unit,
    ) = assertThatFrameCountValuesImpl(recordBefore, recordAfter, motionControl)

    @Test
    fun recordMotion_motionControl_recordDurationOnly() {
        assertThatFrameCountValues(recordBefore = true, recordAfter = false) { awaitFrames(5) }
            // Minimum delays, play flag flipped after 2
            .containsExactly(/* before */ 2, 103, 104, 105, 106, 107)
            .inOrder()
    }

    @Test
    fun recordMotion_motionControl_recordDurationOnly_withoutBefore() {
        assertThatFrameCountValues(recordBefore = false, recordAfter = false) { awaitFrames(5) }
            // Same as above, just not recording before. Must not make a difference
            .containsExactly(103, 104, 105, 106, 107)
            .inOrder()
    }

    @Test
    fun recordMotion_motionControl_recordBeforeAndAfter() {
        assertThatFrameCountValues(recordBefore = true, recordAfter = true) { awaitFrames(1) }
            // after represents the state when the composable is idle, no matter how long the
            // recording took
            .containsExactly(/* before */ 2, 103, /* after */ 116)
            .inOrder()
    }

    @Test
    fun recordMotion_motionControl_delayStartRecording() {
        assertThatFrameCountValues(recordBefore = true, recordAfter = false) {
                // adjust the recording start by two frames.
                val onStartRecording = delayStartRecording()
                awaitFrames(2)
                onStartRecording()
                awaitFrames(5)
            }
            // Start recording is delayed, readyToPlay is still after frame 2 (before is captured
            // just before the flag is flipped)
            .containsExactly(/* before */ 2, 105, 106, 107, 108, 109)
            .inOrder()
    }

    @Test
    fun recordMotion_motionControl_delayStartRecording_zeroDelay_hasNoEffect() {
        assertThatFrameCountValues(recordBefore = true, recordAfter = false) {
                val onStartRecording = delayStartRecording()
                onStartRecording()
                awaitFrames(5)
            }
            // Capturing the delayStartRecording() but immediately releasing it is a no-op
            // must result in the same as recordMotion_motionControl_recordDurationOnly
            .containsExactly(/* before */ 2, 103, 104, 105, 106, 107)
            .inOrder()
    }

    @Test
    fun recordMotion_motionControl_delayReadyToPlay() {
        assertThatFrameCountValues(recordBefore = true, recordAfter = false) {
                val onReadyToPlay = delayReadyToPlay()
                awaitFrames(2)
                onReadyToPlay()
                awaitFrames(5)
            }
            // delaying readyToPlay pushes back the before recording
            .containsExactly(/* before */ 4, 105, 106, 107, 108, 109)
            .inOrder()
    }

    @Test
    fun recordMotion_motionControl_delayReadyToPlay_zeroDelay_hasNoEffect() {
        assertThatFrameCountValues(recordBefore = true, recordAfter = false) {
                val onReadyToPlay = delayReadyToPlay()
                onReadyToPlay()
                awaitFrames(5)
            }
            // Capturing the delayReadyToPlay() but immediately releasing it is a no-op
            // must result in the same as recordMotion_motionControl_recordDurationOnly
            .containsExactly(/* before */ 2, 103, 104, 105, 106, 107)
            .inOrder()
    }

    @Test
    fun recordMotion_motionControl_delayPlayAndRecording() {
        assertThatFrameCountValues(recordBefore = true, recordAfter = false) {
                val onReadyToPlay = delayReadyToPlay()
                val onStartRecording = delayStartRecording()
                awaitFrames(2)

                onReadyToPlay()
                awaitFrames(3)

                onStartRecording()
                awaitFrames(5)
            }
            .containsExactly(/* before */ 4, 108, 109, 110, 111, 112)
            .inOrder()
    }

    @Test
    fun recordMotion_motionControl_awaitDelay_10ms_skipsOneFrame() {
        assertThatFrameCountValues(recordBefore = false, recordAfter = false) {
                awaitDelay(10.milliseconds)
            }
            .hasSize(1)
    }

    @Test
    fun recordMotion_motionControl_awaitDelay_16ms_skipsOneFrame() {
        assertThatFrameCountValues(recordBefore = false, recordAfter = false) {
                awaitDelay(16.milliseconds)
            }
            .hasSize(1)
    }

    @Test
    fun recordMotion_motionControl_awaitDelay_17ms_skipsTwoFrames() {
        assertThatFrameCountValues(recordBefore = false, recordAfter = false) {
                awaitDelay(17.milliseconds)
            }
            .hasSize(2)
    }

    @Test
    fun recordMotion_motionControl_awaitDelay_delayStartsAfterImmediately() {
        assertThatFrameCountValues(recordBefore = false, recordAfter = false) {
                // 20ms
                awaitDelay(10.milliseconds)
                awaitDelay(10.milliseconds)
            }
            .hasSize(2)
    }

    @Test
    fun recordMotion_motionControl_awaitDelay_roundsUpToFullDelay() {
        assertThatFrameCountValues(recordBefore = false, recordAfter = false) {
                awaitDelay(10.milliseconds)
            }
            .hasSize(1)
    }

    @Test
    fun recordMotion_motionControl_awaitCondition() {
        val checkConditionInvocationFrames = mutableListOf<Int>()

        assertThatFrameCountValues(recordBefore = true, recordAfter = false) {
                awaitCondition {
                    val currentFrameCount = motionTestValueOfNode(frameCountKey)
                    checkConditionInvocationFrames.add(currentFrameCount)
                    currentFrameCount == 105
                }
            }
            // Must not record the frame where the condition returned true
            .containsExactly(/* before */ 2, 103, 104)
            .inOrder()

        assertThat(checkConditionInvocationFrames).containsExactly(103, 104, 105).inOrder()
    }

    @Test
    fun recordMotion_motionControl_awaitConditionOnSignals() {
        val awaitReadyToPlayInvocationFrames = mutableListOf<Int>()
        val awaitStartRecordingInvocationFrames = mutableListOf<Int>()
        val awaitAnimationEndInvocationFrames = mutableListOf<Int>()

        assertThatFrameCountValues(recordBefore = true, recordAfter = false) {
                val onReadyToPlay = delayReadyToPlay()
                val onStartRecording = delayStartRecording()

                awaitCondition {
                    motionTestValueOfNode(frameCountKey)
                        .also(awaitReadyToPlayInvocationFrames::add) == 5
                }

                onReadyToPlay()

                awaitCondition {
                    motionTestValueOfNode(frameCountKey)
                        .also(awaitStartRecordingInvocationFrames::add) == 107
                }

                onStartRecording()

                awaitCondition {
                    motionTestValueOfNode(frameCountKey)
                        .also(awaitAnimationEndInvocationFrames::add) == 110
                }
            }
            .containsExactly(/* before */ 6, 108, 109)
            .inOrder()

        assertThat(awaitReadyToPlayInvocationFrames).containsExactly(1, 2, 3, 4, 5).inOrder()
        // 6 (and not 106) as this method is invoked immediately after readyToPlay was flipped, and
        // no recomposition happened yet.
        assertThat(awaitStartRecordingInvocationFrames).containsExactly(6, 107).inOrder()
        assertThat(awaitAnimationEndInvocationFrames).containsExactly(108, 109, 110).inOrder()
    }

    /** @see assertThatFrameCountValues */
    private fun assertThatFrameCountValuesImpl(
        recordBefore: Boolean,
        recordAfter: Boolean,
        motionControl: suspend MotionControlScope.() -> Unit,
    ): IterableSubject {
        val motion =
            motionRule.recordMotion(
                content = { play ->
                    var frameCount by remember { mutableStateOf(0) }
                    LaunchedEffect(Unit) {
                        val animatable = Animatable(0f)
                        launch {
                            // frameCount must end at 16 when the animation ends (some tests
                            // capture this)
                            val testMaxFrameCount = 16

                            // Compute the animation duration that ensures the callback will be
                            // invoked exactly testMaxFrameCount:  Compose runs test animations at
                            // exactly 16ms/frame, and the first animation frame is for animation
                            // value 0.
                            val animationDurationMillis = (testMaxFrameCount - 1) * 16
                            animatable.animateTo(1f, tween(animationDurationMillis)) {
                                frameCount++
                            }
                        }
                    }
                    val exportedFrameCount = if (play) frameCount + 100 else frameCount
                    Text(
                        text = "$exportedFrameCount",
                        modifier =
                            Modifier.motionTestValues { exportedFrameCount exportAs frameCountKey }
                    )
                },
                ComposeRecordingSpec(motionControl, recordBefore, recordAfter) {
                    feature(frameCountKey, DataPointTypes.int)
                }
            )

        val frameCountValues =
            checkNotNull(motion.timeSeries.features["frameCount"]).dataPoints.map { dataPoint ->
                when (dataPoint) {
                    is ValueDataPoint -> dataPoint.value
                    is NotFoundDataPoint -> null
                    else -> throw AssertionError()
                }
            }

        return assertThat(frameCountValues)
    }

    companion object {
        val frameCountKey = MotionTestValueKey<Int>("frameCount")
    }
}
