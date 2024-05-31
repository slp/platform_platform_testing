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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.rules.RuleChain
import platform.test.motion.MotionTestRule
import platform.test.motion.RecordedMotion
import platform.test.motion.RecordedMotion.Companion.create
import platform.test.motion.compose.values.EnableMotionTestValueCollection
import platform.test.motion.golden.DataPoint
import platform.test.motion.golden.Feature
import platform.test.motion.golden.FrameId
import platform.test.motion.golden.SupplementalFrameId
import platform.test.motion.golden.TimeSeries
import platform.test.motion.golden.TimeSeriesCaptureScope
import platform.test.motion.golden.TimestampFrameId
import platform.test.screenshot.DeviceEmulationRule
import platform.test.screenshot.DeviceEmulationSpec
import platform.test.screenshot.Displays
import platform.test.screenshot.GoldenPathManager

/** Toolkit class to use for View-based [MotionTestRule] tests. */
class ComposeToolkit(
    val composeContentTestRule: ComposeContentTestRule,
    val testScope: TestScope,
)

/** Runs a motion test in the [ComposeToolkit.testScope] */
fun MotionTestRule<ComposeToolkit>.runTest(
    timeout: Duration = 20.seconds,
    testBody: suspend MotionTestRule<ComposeToolkit>.() -> Unit
) {
    val motionTestRule = this
    toolkit.testScope.runTest(timeout) { testBody.invoke(motionTestRule) }
}

/**
 * Convenience to create a [MotionTestRule], including the required setup.
 *
 * In addition to the [MotionTestRule], this function also creates a [DeviceEmulationRule] and
 * [ComposeContentTestRule], and ensures these are run as part of the [MotionTestRule].
 */
@OptIn(ExperimentalTestApi::class)
fun createComposeMotionTestRule(
    goldenPathManager: GoldenPathManager,
    testScope: TestScope = TestScope(),
    deviceEmulationSpec: DeviceEmulationSpec = DeviceEmulationSpec(Displays.Phone)
): MotionTestRule<ComposeToolkit> {
    val deviceEmulationRule = DeviceEmulationRule(deviceEmulationSpec)
    val composeRule = createComposeRule(testScope.coroutineContext)

    return MotionTestRule(
        ComposeToolkit(composeRule, testScope),
        goldenPathManager,
        extraRules = RuleChain.outerRule(deviceEmulationRule).around(composeRule)
    )
}

/**
 * Controls the timing of the motion recording.
 *
 * The time series is recorded while the [recording] function is running.
 *
 * @param delayReadyToPlay allows delaying flipping the `play` parameter of the [recordMotion]'s
 *   content composable to true.
 * @param delayRecording allows delaying the first recorded frame, after the animation started.
 */
class MotionControl(
    val delayReadyToPlay: MotionControlFn = {},
    val delayRecording: MotionControlFn = {},
    val recording: MotionControlFn
)

typealias MotionControlFn = suspend MotionControlScope.() -> Unit

interface MotionControlScope : SemanticsNodeInteractionsProvider {
    /** Waits until [check] returns true. Invoked on each frame. */
    suspend fun awaitCondition(check: () -> Boolean)
    /** Waits for [count] frames to be processed. */
    suspend fun awaitFrames(count: Int = 1)
    /** Waits for [duration] to pass. */
    suspend fun awaitDelay(duration: Duration)
}

/**
 * Defines the sampling of features during a test run.
 *
 * @param motionControl defines the timing for the recording.
 * @param recordBefore Records the frame just before the animation is started (immediately before
 *   flipping the `play` parameter of the [recordMotion]'s content composable)
 * @param recordAfter Records the frame after the recording has ended (runs after awaiting idleness,
 *   after all animations have finished and no more recomposition is pending).
 * @param timeSeriesCapture produces the time-series, invoked on each animation frame.
 */
data class ComposeRecordingSpec(
    val motionControl: MotionControl,
    val recordBefore: Boolean = true,
    val recordAfter: Boolean = true,
    val timeSeriesCapture: TimeSeriesCaptureScope<SemanticsNodeInteractionsProvider>.() -> Unit,
) {
    constructor(
        recording: MotionControlFn,
        recordBefore: Boolean = true,
        recordAfter: Boolean = true,
        timeSeriesCapture: TimeSeriesCaptureScope<SemanticsNodeInteractionsProvider>.() -> Unit,
    ) : this(MotionControl(recording = recording), recordBefore, recordAfter, timeSeriesCapture)

    companion object {
        /** Record a time-series until [checkDone] returns true. */
        fun until(
            checkDone: SemanticsNodeInteractionsProvider.() -> Boolean,
            recordBefore: Boolean = true,
            recordAfter: Boolean = true,
            timeSeriesCapture: TimeSeriesCaptureScope<SemanticsNodeInteractionsProvider>.() -> Unit,
        ): ComposeRecordingSpec {
            return ComposeRecordingSpec(
                motionControl = MotionControl { awaitCondition { checkDone() } },
                recordBefore,
                recordAfter,
                timeSeriesCapture
            )
        }
    }
}

/**
 * Composes [content] and records the time-series of the features specified in [recordingSpec].
 *
 * The animation is recorded between flipping [content]'s `play` parameter to `true`, until the
 * [ComposeRecordingSpec.motionControl] finishes.
 */
fun MotionTestRule<ComposeToolkit>.recordMotion(
    content: @Composable (play: Boolean) -> Unit,
    recordingSpec: ComposeRecordingSpec,
): RecordedMotion {
    with(toolkit.composeContentTestRule) {
        val frameIdCollector = mutableListOf<FrameId>()
        val propertyCollector = mutableMapOf<String, MutableList<DataPoint<*>>>()
        val screenshotCollector = mutableListOf<ImageBitmap>()

        fun recordFrame(frameId: FrameId) {
            frameIdCollector.add(frameId)
            recordingSpec.timeSeriesCapture.invoke(TimeSeriesCaptureScope(this, propertyCollector))
            screenshotCollector.add(onRoot().captureToImage())
        }

        var playbackStarted by mutableStateOf(false)

        mainClock.autoAdvance = false

        setContent { EnableMotionTestValueCollection { content(playbackStarted) } }

        waitForIdle()

        val motionControl =
            MotionControlImpl(
                toolkit.composeContentTestRule,
                toolkit.testScope,
                recordingSpec.motionControl
            )

        // Wait for the test to allow readyToPlay
        while (!motionControl.readyToPlay) {
            motionControl.nextFrame()
        }

        if (recordingSpec.recordBefore) {
            recordFrame(SupplementalFrameId("before"))
        }

        playbackStarted = true
        while (!motionControl.recordingStarted) {
            motionControl.nextFrame()
        }

        val startFrameTime = mainClock.currentTime
        while (!motionControl.recordingEnded) {
            recordFrame(TimestampFrameId(mainClock.currentTime - startFrameTime))
            motionControl.nextFrame()
        }

        mainClock.autoAdvance = true
        waitForIdle()

        if (recordingSpec.recordAfter) {
            recordFrame(SupplementalFrameId("after"))
        }

        val timeSeries =
            TimeSeries(
                frameIdCollector.toList(),
                propertyCollector.entries.map { entry -> Feature(entry.key, entry.value) }
            )

        return create(timeSeries, screenshotCollector.map { it.asAndroidBitmap() })
    }
}

enum class MotionControlState {
    Start,
    WaitingToPlay,
    WaitingToRecord,
    Recording,
    Ended,
}

@OptIn(ExperimentalCoroutinesApi::class)
private class MotionControlImpl(
    val composeTestRule: ComposeTestRule,
    val testScope: TestScope,
    val motionControl: MotionControl
) : MotionControlScope, SemanticsNodeInteractionsProvider by composeTestRule {

    private var state = MotionControlState.Start
    private lateinit var delayReadyToPlayJob: Job
    private lateinit var delayRecordingJob: Job
    private lateinit var recordingJob: Job

    private val frameEmitter = MutableStateFlow<Long>(0)
    private val onFrame = frameEmitter.asStateFlow()

    val readyToPlay: Boolean
        get() =
            when (state) {
                MotionControlState.Start,
                MotionControlState.WaitingToPlay -> false
                else -> true
            }
    val recordingStarted: Boolean
        get() =
            when (state) {
                MotionControlState.Recording,
                MotionControlState.Ended -> true
                else -> false
            }

    val recordingEnded: Boolean
        get() =
            when (state) {
                MotionControlState.Ended -> true
                else -> false
            }

    fun nextFrame() {
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()

        when (state) {
            MotionControlState.Start -> {
                delayReadyToPlayJob = motionControl.delayReadyToPlay.launch()
                state = MotionControlState.WaitingToPlay
            }
            MotionControlState.WaitingToPlay -> {
                if (delayReadyToPlayJob.isCompleted) {
                    delayRecordingJob = motionControl.delayRecording.launch()
                    state = MotionControlState.WaitingToRecord
                }
            }
            MotionControlState.WaitingToRecord -> {
                if (delayRecordingJob.isCompleted) {
                    recordingJob = motionControl.recording.launch()
                    state = MotionControlState.Recording
                }
            }
            MotionControlState.Recording -> {
                if (recordingJob.isCompleted) {
                    state = MotionControlState.Ended
                }
            }
            MotionControlState.Ended -> {}
        }

        frameEmitter.tryEmit(composeTestRule.mainClock.currentTime)
        testScope.runCurrent()

        composeTestRule.waitForIdle()

        if (state == MotionControlState.Recording && recordingJob.isCompleted) {
            state = MotionControlState.Ended
        }
    }

    override suspend fun awaitFrames(count: Int) {
        // Since this is a state-flow, the current frame is counted too. This condition must wait
        // for an additional frame to fulfill the contract
        onFrame.take(count + 1).collect {}
    }

    override suspend fun awaitDelay(duration: Duration) {
        val endTime = onFrame.value + duration.inWholeMilliseconds
        onFrame.takeWhile { it < endTime }.collect {}
    }

    override suspend fun awaitCondition(check: () -> Boolean) {
        onFrame.takeWhile { !check() }.collect {}
    }

    private fun MotionControlFn.launch(): Job {
        val function = this
        return testScope.launch { function() }
    }
}
