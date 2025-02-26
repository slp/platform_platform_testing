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

package android.tools.monitors

import android.tools.device.apphelpers.BrowserAppHelper
import android.tools.io.TraceType
import android.tools.testutils.CleanFlickerEnvironmentRule
import android.tools.traces.SERVICE_TRACE_CONFIG
import android.tools.traces.io.IResultData
import android.tools.traces.io.ResultReader
import android.tools.traces.monitors.PerfettoTraceMonitor
import android.tools.traces.monitors.withSFTracing
import android.tools.traces.monitors.withTransactionsTracing
import android.tools.traces.parsers.WindowManagerStateHelper
import android.tools.traces.parsers.perfetto.LayersTraceParser
import android.tools.traces.parsers.perfetto.TraceProcessorSession
import android.tools.traces.parsers.perfetto.TransitionsTraceParser
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.helpers.ImeAppHelper
import com.google.common.truth.Truth
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [PerfettoTraceMonitor] tests. To run this test: `atest
 * FlickerLibTest:PerfettoTraceMonitorTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PerfettoTraceMonitorTest : TraceMonitorTest<PerfettoTraceMonitor>() {
    override val traceType = TraceType.PERFETTO

    override fun getMonitor() =
        PerfettoTraceMonitor.newBuilder().enableLayersTrace().enableTransactionsTrace().build()

    override fun assertTrace(traceData: ByteArray) {
        Truth.assertThat(traceData.size).isGreaterThan(0)
    }

    @Test
    fun withSFTracingTest() {
        val debugFile = getDebugFile("uiTrace-PerfettoTraceMonitorTest-withSFTracingTest")
        val trace =
            withSFTracing(debugFile = debugFile) {
                device.pressHome()
                device.pressRecentApps()
            }

        Truth.assertWithMessage("Could not obtain layers trace").that(trace.entries).isNotEmpty()
    }

    @Test
    fun withTransactionsTracingTest() {
        val debugFile = getDebugFile("uiTrace-PerfettoTraceMonitorTest-withTransactionsTracingTest")
        val trace =
            withTransactionsTracing(debugFile) {
                device.pressHome()
                device.pressRecentApps()
            }

        Truth.assertWithMessage("Could not obtain transactions trace")
            .that(trace.entries)
            .isNotEmpty()
    }

    @Test
    fun layersDump() {
        val reader =
            PerfettoTraceMonitor.newBuilder().enableLayersDump().build().withTracing(
                resultReaderProvider = { buildResultReader(it) }
            ) {}
        val debugFile = getDebugFile("uiTrace-PerfettoTraceMonitorTest-layersDump")
        debugFile.writeBytes(reader.artifact.readBytes())
        val traceData = reader.readBytes(TraceType.PERFETTO) ?: ByteArray(0)
        assertTrace(traceData)

        val trace =
            TraceProcessorSession.loadPerfettoTrace(traceData) { session ->
                LayersTraceParser().parse(session)
            }
        Truth.assertWithMessage("Could not obtain layers dump")
            .that(trace.entries.size)
            .isEqualTo(1)
    }

    @Test
    fun withTransitionTracingTest() {
        assumeTrue(
            "PerfettoTransitionTracing flag should be enabled",
            android.tracing.Flags.perfettoTransitionTracing(),
        )

        val traceMonitor = PerfettoTraceMonitor.newBuilder().enableTransitionsTrace().build()
        val reader =
            traceMonitor.withTracing(resultReaderProvider = { buildResultReader(it) }) {
                BrowserAppHelper().launchViaIntent()
                device.pressHome()
                device.pressRecentApps()
            }
        val debugFile = getDebugFile("uiTrace-PerfettoTraceMonitorTest-withTransitionTracingTest")
        debugFile.writeBytes(reader.artifact.readBytes())
        val traceData = reader.readBytes(TraceType.PERFETTO) ?: ByteArray(0)
        assertTrace(traceData)

        val trace =
            TraceProcessorSession.loadPerfettoTrace(traceData) { session ->
                TransitionsTraceParser().parse(session)
            }
        Truth.assertWithMessage("Could not obtain transition trace")
            .that(trace.entries)
            .isNotEmpty()
    }

    @Test
    fun imeTracingTest() {
        assumeTrue("PerfettoIme flag should be enabled", android.tracing.Flags.perfettoIme())

        val traceMonitor = PerfettoTraceMonitor.newBuilder().enableImeTrace().build()
        val reader =
            traceMonitor.withTracing(resultReaderProvider = { buildResultReader(it) }) {
                val wmHelper = WindowManagerStateHelper()
                val imeApp = ImeAppHelper(instrumentation)
                imeApp.launchViaIntent(wmHelper)
                imeApp.openIME(wmHelper)
            }
        val debugFile = getDebugFile("uiTrace-PerfettoTraceMonitorTest-imeTracingTest")
        debugFile.writeBytes(reader.artifact.readBytes())
        val traceData = reader.readBytes(TraceType.PERFETTO) ?: ByteArray(0)
        assertTrace(traceData)

        val queryRowsCount = { session: TraceProcessorSession, tableName: String ->
            val sql =
                "INCLUDE PERFETTO MODULE android.winscope.inputmethod;" +
                    "SELECT COUNT(*) FROM $tableName;"
            session.query(sql) { rows ->
                require(rows.size == 1)
                rows[0]["COUNT(*)"] as Long
            }
        }

        val (countRowsClients, countRowsManagerService, countRowsService) =
            TraceProcessorSession.loadPerfettoTrace(traceData) { session ->
                Triple(
                    queryRowsCount(session, "android_inputmethod_clients"),
                    queryRowsCount(session, "android_inputmethod_manager_service"),
                    queryRowsCount(session, "android_inputmethod_service"),
                )
            }

        Truth.assertWithMessage("TP doesn't contain IME client rows")
            .that(countRowsClients)
            .isGreaterThan(0L)
        Truth.assertWithMessage("TP doesn't contain IME manager service rows")
            .that(countRowsManagerService)
            .isGreaterThan(0L)
        Truth.assertWithMessage("TP doesn't contain IME service rows")
            .that(countRowsService)
            .isGreaterThan(0L)
    }

    @Test
    fun viewCaptureTracingTest() {
        assumeTrue(
            "PerfettoViewCaptureTracing flag should be enabled",
            android.tracing.Flags.perfettoViewCaptureTracing(),
        )

        val traceMonitor = PerfettoTraceMonitor.newBuilder().enableViewCaptureTrace().build()
        val reader =
            traceMonitor.withTracing(resultReaderProvider = { buildResultReader(it) }) {
                BrowserAppHelper().launchViaIntent()
                device.pressHome()
                device.pressRecentApps()
            }
        val debugFile = getDebugFile("uiTrace-PerfettoTraceMonitorTest-viewCaptureTracingTest")
        debugFile.writeBytes(reader.artifact.readBytes())
        val traceData = reader.readBytes(TraceType.PERFETTO) ?: ByteArray(0)
        assertTrace(traceData)

        val countRows =
            TraceProcessorSession.loadPerfettoTrace(traceData) { session ->
                val sql =
                    "INCLUDE PERFETTO MODULE android.winscope.viewcapture;" +
                        "SELECT COUNT(*) FROM android_viewcapture;"
                session.query(sql) { rows ->
                    require(rows.size == 1)
                    rows[0]["COUNT(*)"] as Long
                }
            }

        Truth.assertWithMessage("TP doesn't contain ViewCapture rows")
            .that(countRows)
            .isGreaterThan(0L)
    }

    @Test
    fun windowManagerTracingTest() {
        assumeTrue(
            "PerfettoWmTracing flag should be enabled",
            android.tracing.Flags.perfettoWmTracing(),
        )

        val traceMonitor = PerfettoTraceMonitor.newBuilder().enableWindowManagerTrace().build()
        val reader =
            traceMonitor.withTracing(resultReaderProvider = { buildResultReader(it) }) {
                BrowserAppHelper().launchViaIntent()
                device.pressHome()
                device.pressRecentApps()
            }
        val debugFile = getDebugFile("uiTrace-PerfettoTraceMonitorTest-windowManagerTracingTest")
        debugFile.writeBytes(reader.artifact.readBytes())
        val traceData = reader.readBytes(TraceType.PERFETTO) ?: ByteArray(0)
        assertTrace(traceData)

        val countRows =
            TraceProcessorSession.loadPerfettoTrace(traceData) { session ->
                val sql =
                    "INCLUDE PERFETTO MODULE android.winscope.windowmanager;" +
                        "SELECT COUNT(*) FROM android_windowmanager;"
                session.query(sql) { rows ->
                    require(rows.size == 1)
                    rows[0]["COUNT(*)"] as Long
                }
            }

        Truth.assertWithMessage("TP doesn't contain WindowManager rows")
            .that(countRows)
            .isGreaterThan(0L)
    }

    @Test
    fun windowManagerDumpTest() {
        assumeTrue(
            "PerfettoWmTracing flag should be enabled",
            android.tracing.Flags.perfettoWmTracing(),
        )

        val reader =
            PerfettoTraceMonitor.newBuilder().enableWindowManagerDump().build().withTracing(
                resultReaderProvider = { buildResultReader(it) }
            ) {}
        val debugFile = getDebugFile("uiTrace-PerfettoTraceMonitorTest-windowManagerDumpTest")
        debugFile.writeBytes(reader.artifact.readBytes())
        val traceData = reader.readBytes(TraceType.PERFETTO) ?: ByteArray(0)
        assertTrace(traceData)

        val countRows =
            TraceProcessorSession.loadPerfettoTrace(traceData) { session ->
                val sql =
                    "INCLUDE PERFETTO MODULE android.winscope.windowmanager;" +
                        "SELECT COUNT(*) FROM android_windowmanager;"
                session.query(sql) { rows ->
                    require(rows.size == 1)
                    rows[0]["COUNT(*)"] as Long
                }
            }

        Truth.assertWithMessage("TP doesn't contain WindowManager dump rows")
            .that(countRows)
            .isEqualTo(1L)
    }

    private fun getDebugFile(testName: String): File =
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .filesDir
            .resolve("$testName.winscope")

    private fun buildResultReader(resultData: IResultData): ResultReader =
        ResultReader(resultData, SERVICE_TRACE_CONFIG)

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRule()
    }
}
