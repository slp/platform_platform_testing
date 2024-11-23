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

@file:JvmName("MonitorUtils")
@file:OptIn(
    androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi::class,
    androidx.benchmark.perfetto.ExperimentalPerfettoTraceProcessorApi::class,
)

package android.tools.traces.monitors

import android.tools.ScenarioBuilder
import android.tools.Tag
import android.tools.io.Reader
import android.tools.io.TraceType
import android.tools.traces.SERVICE_TRACE_CONFIG
import android.tools.traces.TRACE_CONFIG_REQUIRE_CHANGES
import android.tools.traces.io.IResultData
import android.tools.traces.io.ResultReader
import android.tools.traces.io.ResultReaderWithLru
import android.tools.traces.io.ResultWriter
import android.tools.traces.monitors.wm.WindowManagerTraceMonitor
import android.tools.traces.parsers.perfetto.LayersTraceParser
import android.tools.traces.parsers.perfetto.TraceProcessorSession
import android.tools.traces.parsers.perfetto.TransactionsTraceParser
import android.tools.traces.parsers.perfetto.WindowManagerTraceParser
import android.tools.traces.surfaceflinger.LayersTrace
import android.tools.traces.surfaceflinger.TransactionsTrace
import android.tools.traces.wm.WindowManagerTrace
import java.io.File
import perfetto.protos.PerfettoConfig.SurfaceFlingerLayersConfig
import perfetto.protos.PerfettoConfig.WindowManagerConfig

private fun buildResultReader(resultData: IResultData): ResultReader =
    ResultReader(resultData, TRACE_CONFIG_REQUIRE_CHANGES)

/**
 * Reads the Perfetto file form the result reader and keep (or not) a copy
 *
 * @param debugFile File to keep a copy of the parsed trace, leave null to not keep any copies
 * @throws UnsupportedOperationException If tracing is already activated
 */
private fun readBytes(
    reader: ResultReader,
    debugFile: File?,
    traceType: TraceType = TraceType.PERFETTO,
    tag: String = Tag.ALL,
): ByteArray {
    val bytes = reader.readBytes(traceType, tag) ?: error("Missing trace $traceType")
    reader.artifact.deleteIfExists()
    debugFile?.writeBytes(bytes)
    return bytes
}

/**
 * Acquire the [WindowManagerTrace] with the device state changes that happen when executing the
 * commands defined in the [predicate].
 *
 * @param predicate Commands to execute
 * @throws UnsupportedOperationException If tracing is already activated
 */
fun withWMTracing(
    logFrequency: WindowManagerConfig.LogFrequency =
        WindowManagerConfig.LogFrequency.LOG_FREQUENCY_FRAME,
    debugFile: File? = null,
    predicate: Runnable,
): WindowManagerTrace {
    val reader =
        PerfettoTraceMonitor.newBuilder()
            .enableWindowManagerTrace(logFrequency)
            .build()
            .withTracing(resultReaderProvider = { buildResultReader(it) }, predicate)

    val bytes = readBytes(reader, debugFile)
    return TraceProcessorSession.loadPerfettoTrace(bytes) { session ->
        WindowManagerTraceParser().parse(session)
    }
}

/**
 * Acquire the [LayersTrace] with the device state changes that happen when executing the commands
 * defined in the [predicate].
 *
 * @param flags Flags to indicate tracing level
 * @param predicate Commands to execute
 * @param debugFile File to keep a copy of the parsed trace, leave null to not keep any copies
 * @throws UnsupportedOperationException If tracing is already activated
 */
@JvmOverloads
fun withSFTracing(
    flags: List<SurfaceFlingerLayersConfig.TraceFlag>? = null,
    debugFile: File? = null,
    predicate: Runnable,
): LayersTrace {
    val reader =
        PerfettoTraceMonitor.newBuilder()
            .enableLayersTrace(flags)
            .build()
            .withTracing(resultReaderProvider = { buildResultReader(it) }, predicate)

    val bytes = readBytes(reader, debugFile)
    return TraceProcessorSession.loadPerfettoTrace(bytes) { session ->
        LayersTraceParser().parse(session)
    }
}

/**
 * Acquire the [TransactionsTrace] with the device state changes that happen when executing the
 * commands defined in the [predicate].
 *
 * @param debugFile File to keep a copy of the parsed trace, leave null to not keep any copies
 * @param predicate Commands to execute
 * @throws UnsupportedOperationException If tracing is already activated
 */
fun withTransactionsTracing(debugFile: File? = null, predicate: Runnable): TransactionsTrace {
    val reader =
        PerfettoTraceMonitor.newBuilder()
            .enableTransactionsTrace()
            .build()
            .withTracing(resultReaderProvider = { buildResultReader(it) }, predicate)
    val bytes = readBytes(reader, debugFile)
    return TraceProcessorSession.loadPerfettoTrace(bytes) { session ->
        TransactionsTraceParser().parse(session)
    }
}

/**
 * Acquire the [WindowManagerTrace] and [LayersTrace] with the device state changes that happen when
 * executing the commands defined in the [predicate].
 *
 * @param traceMonitors List of monitors to start
 * @param debugFile File to keep a copy of the parsed trace, leave null to not keep any copies
 * @param predicate Commands to execute
 * @throws UnsupportedOperationException If tracing is already activated
 */
fun withTracing(
    traceMonitors: List<TraceMonitor> =
        mutableListOf<TraceMonitor>()
            .apply {
                if (!android.tracing.Flags.perfettoWmTracing()) {
                    this.add(WindowManagerTraceMonitor())
                }
            }
            .apply {
                val monitorBuilder =
                    PerfettoTraceMonitor.newBuilder().enableLayersTrace().enableTransactionsTrace()

                if (android.tracing.Flags.perfettoWmTracing()) {
                    monitorBuilder.enableWindowManagerTrace()
                }

                this.add(monitorBuilder.build())
            }
            .toList(),
    debugFile: File? = null,
    predicate: Runnable,
): Reader {
    val tmpFile = File.createTempFile("recordTraces", "")
    val writer =
        ResultWriter()
            .forScenario(ScenarioBuilder().forClass(tmpFile.name).build())
            .withOutputDir(tmpFile.parentFile)

    try {
        traceMonitors.forEach { it.start() }
        predicate.run()
    } finally {
        traceMonitors.forEach { it.stop(writer) }
    }
    val reader = ResultReaderWithLru(writer.write(), SERVICE_TRACE_CONFIG)
    debugFile?.writeBytes(reader.artifact.readBytes())
    return reader
}

/**
 * Acquire the [WindowManagerTrace] and [LayersTrace] with the device state changes that happen when
 * executing the commands defined in the [predicate].
 *
 * @param predicate Commands to execute
 * @return a pair containing the WM and SF traces
 * @throws UnsupportedOperationException If tracing is already activated
 */
fun recordTraces(predicate: Runnable): ResultReader {
    return PerfettoTraceMonitor.newBuilder()
        .enableLayersTrace()
        .enableWindowManagerTrace()
        .build()
        .withTracing(resultReaderProvider = { buildResultReader(it) }, predicate)
}
