/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.integration

import android.app.Instrumentation
import android.tools.device.apphelpers.ClockAppHelper
import android.tools.flicker.FlickerServiceTracesCollector
import android.tools.flicker.isShellTransitionsEnabled
import android.tools.flicker.rules.ArtifactSaverRule
import android.tools.io.TraceType
import android.tools.testutils.CleanFlickerEnvironmentRule
import android.tools.testutils.TEST_SCENARIO
import android.tools.testutils.assertArchiveContainsFiles
import android.tools.testutils.getLauncherPackageName
import android.tools.testutils.getSystemUiUidName
import android.tools.traces.parsers.WindowManagerStateHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.android.systemui.Flags.enableViewCaptureTracing
import com.google.common.truth.Truth
import java.io.File
import org.junit.Assume
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [FlickerServiceTracesCollector] tests. To run this test: `atest
 * FlickerLibTestE2e:FlickerServiceTracesCollectorTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class FlickerServiceTracesCollectorTest {
    val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    private val testApp = ClockAppHelper(instrumentation)
    @get:Rule val cleanUp = CleanFlickerEnvironmentRule()
    @get:Rule val artifactSaver = ArtifactSaverRule()

    @Before
    fun before() {
        Assume.assumeTrue(isShellTransitionsEnabled)
    }

    @Test
    fun canCollectTraces() {
        val wmHelper = WindowManagerStateHelper(instrumentation)
        val collector = FlickerServiceTracesCollector()
        collector.start(TEST_SCENARIO)
        testApp.launchViaIntent(wmHelper)
        testApp.exit(wmHelper)
        val reader = collector.stop()
        Truth.assertThat(reader.readWmTrace()?.entries).isNotEmpty()
        Truth.assertThat(reader.readLayersTrace()?.entries).isNotEmpty()
        Truth.assertThat(reader.readTransitionsTrace()?.entries).isNotEmpty()
    }

    @Test
    fun reportsTraceFile() {
        val wmHelper = WindowManagerStateHelper(instrumentation)
        val collector = FlickerServiceTracesCollector()
        collector.start(TEST_SCENARIO)
        testApp.launchViaIntent(wmHelper)
        testApp.exit(wmHelper)
        val reader = collector.stop()
        val tracePath = reader.artifactPath

        require(tracePath.isNotEmpty()) { "Artifact path missing in result" }
        val traceFile = File(tracePath)
        Truth.assertThat(traceFile.exists()).isTrue()
    }

    @Test
    fun reportedTraceFileContainsAllTraces() {
        var possibleExpectedTraces = listOf(EXPECTED_TRACES_LAUNCHER_ONLY)
        if (enableViewCaptureTracing()) {
            possibleExpectedTraces =
                listOf(EXPECTED_TRACES_LAUNCHER_FIRST, EXPECTED_TRACES_SYSUI_FIRST)
        }

        val wmHelper = WindowManagerStateHelper(instrumentation)
        val collector = FlickerServiceTracesCollector()
        collector.start(TEST_SCENARIO)
        testApp.launchViaIntent(wmHelper)
        testApp.exit(wmHelper)
        val reader = collector.stop()
        val tracePath = reader.artifactPath

        require(tracePath.isNotEmpty()) { "Artifact path missing in result" }
        val traceFile = File(tracePath)
        assertArchiveContainsFiles(traceFile, possibleExpectedTraces)
    }

    @Test
    fun supportHavingNoTransitions() {
        val collector = FlickerServiceTracesCollector()
        collector.start(TEST_SCENARIO)
        val reader = collector.stop()
        val transitionTrace = reader.readTransitionsTrace() ?: error("Expected a transition trace")
        Truth.assertThat(transitionTrace.entries).isEmpty()
    }

    companion object {
        val EXPECTED_TRACES_LAUNCHER_ONLY =
            mutableListOf(TraceType.EVENT_LOG.fileName, TraceType.PERFETTO.fileName)
                .also {
                    if (!android.tracing.Flags.perfettoProtologTracing()) {
                        it.add(TraceType.PROTOLOG.fileName)
                    }

                    if (!android.tracing.Flags.perfettoTransitionTracing()) {
                        it.add(TraceType.LEGACY_WM_TRANSITION.fileName)
                        it.add(TraceType.LEGACY_SHELL_TRANSITION.fileName)
                    }

                    if (!android.tracing.Flags.perfettoViewCaptureTracing()) {
                        it.add("${getLauncherPackageName()}_0.vc__view_capture_trace.winscope")
                    }

                    if (!android.tracing.Flags.perfettoWmTracing()) {
                        it.add(TraceType.WM.fileName)
                    }
                }
                .toList()

        val EXPECTED_TRACES_LAUNCHER_FIRST =
            mutableListOf(TraceType.EVENT_LOG.fileName, TraceType.PERFETTO.fileName)
                .also {
                    if (!android.tracing.Flags.perfettoProtologTracing()) {
                        it.add(TraceType.PROTOLOG.fileName)
                    }

                    if (!android.tracing.Flags.perfettoTransitionTracing()) {
                        it.add(TraceType.LEGACY_WM_TRANSITION.fileName)
                        it.add(TraceType.LEGACY_SHELL_TRANSITION.fileName)
                    }

                    if (!android.tracing.Flags.perfettoViewCaptureTracing()) {
                        it.add("${getLauncherPackageName()}_0.vc__view_capture_trace.winscope")
                        it.add("${getSystemUiUidName()}_1.vc__view_capture_trace.winscope")
                    }

                    if (!android.tracing.Flags.perfettoWmTracing()) {
                        it.add(TraceType.WM.fileName)
                    }
                }
                .toList()

        val EXPECTED_TRACES_SYSUI_FIRST =
            mutableListOf(TraceType.EVENT_LOG.fileName, TraceType.PERFETTO.fileName).also {
                if (!android.tracing.Flags.perfettoProtologTracing()) {
                    it.add(TraceType.PROTOLOG.fileName)
                }

                if (!android.tracing.Flags.perfettoTransitionTracing()) {
                    it.add(TraceType.LEGACY_WM_TRANSITION.fileName)
                    it.add(TraceType.LEGACY_SHELL_TRANSITION.fileName)
                }

                if (!android.tracing.Flags.perfettoViewCaptureTracing()) {
                    it.add("${getSystemUiUidName()}_0.vc__view_capture_trace.winscope")
                    it.add("${getLauncherPackageName()}_1.vc__view_capture_trace.winscope")
                }

                if (!android.tracing.Flags.perfettoWmTracing()) {
                    it.add(TraceType.WM.fileName)
                }
            }
    }
}
