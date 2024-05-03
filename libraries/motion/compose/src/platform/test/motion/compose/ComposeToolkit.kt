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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
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

/** Toolkit class to use for View-based [MotionTestRule] tests. */
class ComposeToolkit(
    internal val composeContentTestRule: ComposeContentTestRule,
)

/**
 * Defines the sampling of features during a test run.
 *
 * @param motionControl defines the timing for the recording. As soon as this function finished, the
 *   recording is ended. See [MotionControlScope] for more information.
 * @param recordBefore Records the frame just before the animation is started (immediately before
 *   flipping the `play` parameter of the [recordMotion]'s content composable)
 * @param recordAfter Records the frame after the recording has ended (runs after awaiting idleness,
 *   after all animations have finished and no more recomposition is pending).
 * @param timeSeriesCapture produces the time-series, invoked on each animation frame.
 */
data class ComposeRecordingSpec(
    val motionControl: suspend MotionControlScope.() -> Unit,
    val recordBefore: Boolean = true,
    val recordAfter: Boolean = true,
    val timeSeriesCapture: TimeSeriesCaptureScope<SemanticsNodeInteractionsProvider>.() -> Unit,
) {
    companion object {
        /** Record a time-series until [checkDone] returns true. */
        fun until(
            checkDone: SemanticsNodeInteractionsProvider.() -> Boolean,
            recordBefore: Boolean = true,
            recordAfter: Boolean = true,
            timeSeriesCapture: TimeSeriesCaptureScope<SemanticsNodeInteractionsProvider>.() -> Unit,
        ): ComposeRecordingSpec {
            return ComposeRecordingSpec(
                motionControl = { awaitCondition { checkDone() } },
                recordBefore,
                recordAfter,
                timeSeriesCapture
            )
        }
    }
}

/**
 * Controls the duration (and start) of the motion recording.
 *
 * The recording ends as soon as the [ComposeRecordingSpec.motionControl] coroutine function ends.
 *
 * The `await*()` functions in this scope allow for event- and time-based control of the recording.
 *
 * The recording starts immediately after calling [recordMotion]. This can be fine-tuned using the
 * `delay*()` functions in this scope:
 * - upon `readyToPlay`, the `play` parameter of the [recordMotion]'s content composable is flipped
 *   to true. This is considered the animation start.
 * - upon `startRecording`, the first frame is recorded. This marks the 0ms of the resulting time
 *   series.
 */
interface MotionControlScope : SemanticsNodeInteractionsProvider {

    /**
     * Returns a signal to indicate the timing of when the animation should start.
     *
     * This controls the timing of entering the `readyToPlay` state.
     *
     * When invoking this method, the animation starts as soon as the returned [ReadySignal] is
     * invoked. Obtaining the [ReadySignal] (ie invoking this method) must happen at the start of
     * the [ComposeRecordingSpec.motionControl] implementation, before awaiting anything.
     *
     * NOTE: Only call this method if the test needs to delay the animation start. Without obtaining
     * the signal, the animation is started immediately.
     */
    fun delayReadyToPlay(): ReadySignal

    /**
     * Returns a signal to indicate when the recording should start.
     *
     * This controls the timing of recording the first frame after the `play` parameter of the
     * [recordMotion]'s content is flipped to `true`.
     *
     * Obtaining the [ReadySignal] (ie invoking this method) must happen at the start of the
     * [ComposeRecordingSpec.motionControl] implementation, before awaiting anything.
     *
     * NOTE: Only call this method if the test needs to delay starting the recording. Without
     * obtaining the signal, the recording starts on the frame following the animation start.
     */
    fun delayStartRecording(): ReadySignal

    /** Waits for [count] frames to be processed. */
    suspend fun awaitFrames(count: Int = 1)
    /** Waits for [duration] to pass. */
    suspend fun awaitDelay(duration: Duration)
    /** Waits until [check] returns true. Invoked on each frame. */
    suspend fun awaitCondition(check: () -> Boolean)
}

typealias ReadySignal = () -> Unit

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

        lateinit var coroutineScope: CoroutineScope
        setContent {
            coroutineScope = rememberCoroutineScope()
            EnableMotionTestValueCollection { content(playbackStarted) }
        }

        waitForIdle()

        val motionControl =
            MotionControlImpl(
                toolkit.composeContentTestRule,
                coroutineScope,
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

private class MotionControlImpl(
    val composeTestRule: ComposeTestRule,
    coroutineScope: CoroutineScope,
    motionControl: suspend MotionControlScope.() -> Unit
) : MotionControlScope, SemanticsNodeInteractionsProvider by composeTestRule {

    private var state = MotionControlState.Start
    private var readyToPlaySignal: Boolean? = null
    private var startRecordingSignal: Boolean? = null
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

    private val motionControlJob = coroutineScope.launch { motionControl() }

    fun nextFrame() {
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()

        when (state) {
            MotionControlState.Start -> {
                state = MotionControlState.WaitingToPlay
            }
            MotionControlState.WaitingToPlay -> {
                val readyToPlay =
                    if (readyToPlaySignal == null) {
                        // the test did not call delayReadyToPlay()
                        true
                    } else {
                        readyToPlaySignal!!
                    }

                if (readyToPlay) {
                    state = MotionControlState.WaitingToRecord
                }
            }
            MotionControlState.WaitingToRecord -> {
                val readyToRecord =
                    when (startRecordingSignal) {
                        null -> true // the test did not call startRecordingSignal()
                        else -> startRecordingSignal!!
                    }

                if (readyToRecord) {
                    state = MotionControlState.Recording
                }
            }
            MotionControlState.Recording -> {
                if (motionControlJob.isCompleted) {
                    state = MotionControlState.Ended
                }
            }
            MotionControlState.Ended -> {}
        }

        frameEmitter.tryEmit(composeTestRule.mainClock.currentTime)

        composeTestRule.waitForIdle()

        if (state == MotionControlState.Recording && motionControlJob.isCompleted) {
            state = MotionControlState.Ended
        }
    }

    override fun delayReadyToPlay(): ReadySignal {
        require(state == MotionControlState.Start) {
            "delayReadyToPlay() can't be obtained after motion control started"
        }

        require(readyToPlaySignal == null) { "delayReadyToPlay() must be obtained at most once" }

        readyToPlaySignal = false
        return { readyToPlaySignal = true }
    }

    override fun delayStartRecording(): ReadySignal {
        require(state == MotionControlState.Start) {
            "delayStartRecording() can't be obtained after motion control started"
        }

        require(startRecordingSignal == null) {
            "delayStartRecording() must be obtained at most once"
        }

        startRecordingSignal = false
        return { startRecordingSignal = true }
    }

    override suspend fun awaitFrames(count: Int) {
        skipUncontrolledFrames()
        // Since this is a state-flow, the current frame is counted too. This condition must wait
        // for an additional frame to fulfill the contract
        onFrame.take(count + 1).collect {}
    }

    override suspend fun awaitDelay(duration: Duration) {
        skipUncontrolledFrames()
        val endTime = onFrame.value + duration.inWholeMilliseconds
        onFrame.takeWhile { it < endTime }.collect {}
    }

    override suspend fun awaitCondition(check: () -> Boolean) {
        skipUncontrolledFrames()
        onFrame.takeWhile { !check() }.collect {}
    }

    /** Skips frames that the test did not intend to control. */
    private suspend fun skipUncontrolledFrames() {
        onFrame
            .takeWhile {
                val result =
                    when (state) {
                        MotionControlState.Start -> true
                        MotionControlState.WaitingToPlay -> readyToPlaySignal != false
                        MotionControlState.WaitingToRecord -> startRecordingSignal != false
                        else -> false
                    }

                result
            }
            .collect {}
    }
}
