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

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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
import platform.test.motion.compose.ComposeToolkit.Companion.TAG
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
import platform.test.screenshot.captureToBitmapAsync

/** Toolkit to support Compose-based [MotionTestRule] tests. */
class ComposeToolkit(val composeContentTestRule: ComposeContentTestRule, val testScope: TestScope) {
    internal companion object {
        const val TAG = "ComposeToolkit"
    }
}

/** Runs a motion test in the [ComposeToolkit.testScope] */
fun MotionTestRule<ComposeToolkit>.runTest(
    timeout: Duration = 20.seconds,
    testBody: suspend MotionTestRule<ComposeToolkit>.() -> Unit,
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
    deviceEmulationSpec: DeviceEmulationSpec = DeviceEmulationSpec(Displays.Phone),
): MotionTestRule<ComposeToolkit> {
    val deviceEmulationRule = DeviceEmulationRule(deviceEmulationSpec)
    val composeRule = createComposeRule(testScope.coroutineContext)

    return MotionTestRule(
        ComposeToolkit(composeRule, testScope),
        goldenPathManager,
        extraRules = RuleChain.outerRule(deviceEmulationRule).around(composeRule),
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
    val recording: MotionControlFn,
)

typealias MotionControlFn = suspend MotionControlScope.() -> Unit

interface MotionControlScope : SemanticsNodeInteractionsProvider {
    /** Waits until [check] returns true. Invoked on each frame. */
    suspend fun awaitCondition(check: () -> Boolean)

    /** Waits for [count] frames to be processed. */
    suspend fun awaitFrames(count: Int = 1)

    /** Waits for [duration] to pass. */
    suspend fun awaitDelay(duration: Duration)

    /**
     * Performs touch input, and waits for the completion thereof.
     *
     * NOTE: Do use this function instead of [SemanticsNodeInteraction.performTouchInput], since
     * `performTouchInput` will also advance the time of the compose clock, making it impossible to
     * record motion while performing gestures.
     */
    suspend fun performTouchInputAsync(
        onNode: SemanticsNodeInteraction,
        gestureControl: TouchInjectionScope.() -> Unit,
    )
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
                timeSeriesCapture,
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
            Log.i(TAG, "recordFrame($frameId)")
            frameIdCollector.add(frameId)
            recordingSpec.timeSeriesCapture.invoke(TimeSeriesCaptureScope(this, propertyCollector))

            val view = (onRoot().fetchSemanticsNode().root as ViewRootForTest).view
            val bitmap = view.captureToBitmapAsync().get(10, TimeUnit.SECONDS)
            screenshotCollector.add(bitmap.asImageBitmap())
        }

        var playbackStarted by mutableStateOf(false)

        mainClock.autoAdvance = false

        setContent { EnableMotionTestValueCollection { content(playbackStarted) } }
        Log.i(TAG, "recordMotion() created compose content")

        waitForIdle()

        val motionControl =
            MotionControlImpl(
                toolkit.composeContentTestRule,
                toolkit.testScope,
                recordingSpec.motionControl,
            )

        Log.i(TAG, "recordMotion() awaiting readyToPlay")

        // Wait for the test to allow readyToPlay
        while (!motionControl.readyToPlay) {
            motionControl.nextFrame()
        }

        if (recordingSpec.recordBefore) {
            recordFrame(SupplementalFrameId("before"))
        }
        Log.i(TAG, "recordMotion() awaiting recordingStarted")

        playbackStarted = true
        while (!motionControl.recordingStarted) {
            motionControl.nextFrame()
        }

        Log.i(TAG, "recordMotion() begin recording")

        val startFrameTime = mainClock.currentTime
        while (!motionControl.recordingEnded) {
            recordFrame(TimestampFrameId(mainClock.currentTime - startFrameTime))
            motionControl.nextFrame()
        }

        Log.i(TAG, "recordMotion() end recording")

        mainClock.autoAdvance = true
        waitForIdle()

        if (recordingSpec.recordAfter) {
            recordFrame(SupplementalFrameId("after"))
        }

        val timeSeries =
            TimeSeries(
                frameIdCollector.toList(),
                propertyCollector.entries.map { entry -> Feature(entry.key, entry.value) },
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
    val motionControl: MotionControl,
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

    override suspend fun performTouchInputAsync(
        onNode: SemanticsNodeInteraction,
        gestureControl: TouchInjectionScope.() -> Unit,
    ) {
        val node = onNode.fetchSemanticsNode()
        val density = node.layoutInfo.density
        val viewConfiguration = node.layoutInfo.viewConfiguration
        val visibleSize =
            with(node.boundsInRoot) { IntSize(width.roundToInt(), height.roundToInt()) }

        val touchEventRecorder = TouchEventRecorder(density, viewConfiguration, visibleSize)
        gestureControl(touchEventRecorder)

        val recordedEntries = touchEventRecorder.recordedEntries
        for (entry in recordedEntries) {
            when (entry) {
                is TouchEventRecorderEntry.AdvanceTime ->
                    awaitDelay(entry.durationMillis.milliseconds)
                is TouchEventRecorderEntry.Cancel ->
                    onNode.performTouchInput { cancel(delayMillis = 0) }
                is TouchEventRecorderEntry.Down ->
                    onNode.performTouchInput { down(entry.pointerId, entry.position) }
                is TouchEventRecorderEntry.Move ->
                    onNode.performTouchInput { move(delayMillis = 0) }
                is TouchEventRecorderEntry.Up -> onNode.performTouchInput { up(entry.pointerId) }
                is TouchEventRecorderEntry.UpdatePointerTo ->
                    onNode.performTouchInput { updatePointerTo(entry.pointerId, entry.position) }
            }
        }
    }

    private fun MotionControlFn.launch(): Job {
        val function = this
        return testScope.launch { function() }
    }
}

/** Records the invocations of the [TouchInjectionScope] methods. */
private sealed interface TouchEventRecorderEntry {

    class AdvanceTime(val durationMillis: Long) : TouchEventRecorderEntry

    object Cancel : TouchEventRecorderEntry

    class Down(val pointerId: Int, val position: Offset) : TouchEventRecorderEntry

    object Move : TouchEventRecorderEntry

    class Up(val pointerId: Int) : TouchEventRecorderEntry

    class UpdatePointerTo(val pointerId: Int, val position: Offset) : TouchEventRecorderEntry
}

private class TouchEventRecorder(
    density: Density,
    override val viewConfiguration: ViewConfiguration,
    override val visibleSize: IntSize,
) : TouchInjectionScope, Density by density {

    val lastPositions = mutableMapOf<Int, Offset>()
    val recordedEntries = mutableListOf<TouchEventRecorderEntry>()

    override fun advanceEventTime(durationMillis: Long) {
        if (durationMillis > 0) {
            recordedEntries.add(TouchEventRecorderEntry.AdvanceTime(durationMillis))
        }
    }

    override fun cancel(delayMillis: Long) {
        advanceEventTime(delayMillis)
        recordedEntries.add(TouchEventRecorderEntry.Cancel)
    }

    override fun currentPosition(pointerId: Int): Offset? {
        return lastPositions[pointerId]
    }

    override fun down(pointerId: Int, position: Offset) {
        recordedEntries.add(TouchEventRecorderEntry.Down(pointerId, position))
        lastPositions[pointerId] = position
    }

    override fun move(delayMillis: Long) {
        advanceEventTime(delayMillis)
        recordedEntries.add(TouchEventRecorderEntry.Move)
    }

    @ExperimentalTestApi
    override fun moveWithHistoryMultiPointer(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>,
        delayMillis: Long,
    ) {
        TODO("Not yet supported")
    }

    override fun up(pointerId: Int) {
        recordedEntries.add(TouchEventRecorderEntry.Up(pointerId))
        lastPositions.remove(pointerId)
    }

    override fun updatePointerTo(pointerId: Int, position: Offset) {
        recordedEntries.add(TouchEventRecorderEntry.UpdatePointerTo(pointerId, position))
        lastPositions[pointerId] = position
    }
}
