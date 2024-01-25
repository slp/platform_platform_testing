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

package android.tools.device.flicker

import android.tools.common.Scenario
import android.tools.common.io.Reader
import android.tools.device.traces.TRACE_CONFIG_REQUIRE_CHANGES
import android.tools.device.traces.io.ResultReaderWithLru
import android.tools.device.traces.io.ResultWriter
import android.tools.device.traces.monitors.PerfettoTraceMonitor
import android.tools.device.traces.monitors.ScreenRecorder
import android.tools.device.traces.monitors.TraceMonitor
import android.tools.device.traces.monitors.events.EventLogMonitor
import android.tools.device.traces.monitors.view.ViewTraceMonitor
import android.tools.device.traces.monitors.wm.LegacyShellTransitionTraceMonitor
import android.tools.device.traces.monitors.wm.LegacyWmTransitionTraceMonitor
import android.tools.device.traces.monitors.wm.WindowManagerTraceMonitor
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.io.path.createTempDirectory

object Utils {
    val ALL_MONITORS: List<TraceMonitor> =
        mutableListOf<TraceMonitor>(
                WindowManagerTraceMonitor(),
                EventLogMonitor(),
                ViewTraceMonitor(),
                ScreenRecorder(InstrumentationRegistry.getInstrumentation().targetContext)
            )
            .apply {
                if (android.tracing.Flags.perfettoTransitionTracing()) {
                    this.add(
                        PerfettoTraceMonitor.newBuilder()
                            .enableLayersTrace()
                            .enableTransactionsTrace()
                            .enableTransitionsTrace()
                            .build(),
                    )
                } else {
                    this.add(LegacyWmTransitionTraceMonitor())
                    this.add(LegacyShellTransitionTraceMonitor())
                    this.add(
                        PerfettoTraceMonitor.newBuilder()
                            .enableLayersTrace()
                            .enableTransactionsTrace()
                            .build(),
                    )
                }
            }

    fun captureTrace(
        scenario: Scenario,
        outputDir: File = createTempDirectory().toFile(),
        monitors: List<TraceMonitor> = ALL_MONITORS,
        actions: (writer: ResultWriter) -> Unit
    ): Reader {
        val writer = ResultWriter().forScenario(scenario).withOutputDir(outputDir).setRunComplete()
        monitors.fold({ actions.invoke(writer) }) { action, monitor ->
            { monitor.withTracing(writer) { action() } }
        }()
        val result = writer.write()

        return ResultReaderWithLru(result, TRACE_CONFIG_REQUIRE_CHANGES)
    }
}
