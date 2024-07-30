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

package android.tools.parsers.perfetto

import android.tools.Cache
import android.tools.testutils.CleanFlickerEnvironmentRule
import android.tools.testutils.readAsset
import android.tools.traces.monitors.PerfettoTraceMonitor
import android.tools.traces.parsers.perfetto.TraceProcessorSession
import android.tools.traces.parsers.perfetto.WindowManagerTraceParser
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth
import org.junit.Assume
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test

/** Tests for [WindowManagerTraceParser] */
class WindowManagerTraceParserTest {
    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun canParseAllEntriesFromStoredTrace() {
        val trace =
            TraceProcessorSession.loadPerfettoTrace(
                readAsset("wm_trace_openchrome.perfetto-trace")
            ) { session ->
                WindowManagerTraceParser().parse(session)
            }
        val firstEntry = trace.entries.first()
        Truth.assertThat(firstEntry.timestamp.elapsedNanos).isEqualTo(9213763541297L)
        Truth.assertThat(firstEntry.windowStates.size).isEqualTo(10)
        Truth.assertThat(firstEntry.visibleWindows.size).isEqualTo(5)
        Truth.assertThat(trace.entries.last().timestamp.elapsedNanos).isEqualTo(9216093628925L)
    }

    @Test
    fun canParseAllEntriesFromNewTrace() {
        Assume.assumeTrue(android.tracing.Flags.perfettoWmTracing())

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val monitor = PerfettoTraceMonitor.Builder().enableWindowManagerTrace().build()
        val data =
            monitor.withTracing {
                device.pressHome()
                device.pressRecentApps()
            }
        val trace =
            TraceProcessorSession.loadPerfettoTrace(data) { session ->
                WindowManagerTraceParser().parse(session)
            }
        Truth.assertThat(trace.entries).isNotEmpty()
    }

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRule()
    }
}
