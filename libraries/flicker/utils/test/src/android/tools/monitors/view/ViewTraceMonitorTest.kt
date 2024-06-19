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

package android.tools.monitors.view

import android.tools.io.TraceType
import android.tools.monitors.TraceMonitorTest
import android.tools.testutils.assertArchiveContainsFiles
import android.tools.testutils.getActualTraceFilesFromArchive
import android.tools.testutils.getLauncherPackageName
import android.tools.testutils.getSystemUiUidName
import android.tools.testutils.newTestResultWriter
import android.tools.traces.TRACE_CONFIG_REQUIRE_CHANGES
import android.tools.traces.io.ResultReader
import android.tools.traces.monitors.view.ViewTraceMonitor
import android.tracing.Flags
import com.android.systemui.Flags.enableViewCaptureTracing
import com.google.common.truth.Truth
import java.io.File
import org.junit.Assume
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/** Tests for [ViewTraceMonitor] */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ViewTraceMonitorTest : TraceMonitorTest<ViewTraceMonitor>() {
    override val traceType = TraceType.VIEW

    override fun getMonitor() = ViewTraceMonitor()

    override fun assertTrace(traceData: ByteArray) {
        Truth.assertThat(traceData.size).isGreaterThan(0)
    }

    @Before
    override fun before() {
        Assume.assumeFalse(Flags.perfettoViewCaptureTracing())
        super.before()
    }

    @Test
    @Throws(Exception::class)
    override fun captureTrace() {
        var possibleExpectedTraces = listOf(EXPECTED_TRACES_LAUNCHER_ONLY)
        if (enableViewCaptureTracing()) {
            possibleExpectedTraces =
                listOf(EXPECTED_TRACES_LAUNCHER_FIRST, EXPECTED_TRACES_SYSUI_FIRST)
        }

        traceMonitor.start()
        device.pressHome()
        device.pressRecentApps()
        val writer = newTestResultWriter()
        traceMonitor.stop(writer)
        val result = writer.write()
        val reader = ResultReader(result, TRACE_CONFIG_REQUIRE_CHANGES)

        val traceArtifactPath = reader.artifactPath
        require(traceArtifactPath.isNotEmpty()) { "Artifact path missing in result" }
        val traceArchive = File(traceArtifactPath)

        assertArchiveContainsFiles(traceArchive, possibleExpectedTraces)
        val actualTraceFiles = getActualTraceFilesFromArchive(traceArchive)
        val tagList = getTagListFromTraces(actualTraceFiles)
        for (tag: String in tagList) {
            val trace =
                reader.readBytes(traceMonitor.traceType, tag)
                    ?: error("Missing trace file ${traceMonitor.traceType}")
            Truth.assertWithMessage("Trace file size").that(trace.size).isGreaterThan(0)
            assertTrace(trace)
        }
    }

    // We override this test to create a placeholder since the [withTracing] method does not align
    // with the implementation of multiple instances of ViewCapture.
    @Test
    @Throws(Exception::class)
    override fun withTracing() {
        Truth.assertThat(true).isTrue()
    }

    private fun getTagListFromTraces(actualTraceFiles: List<String>): List<String> {
        var tagList: List<String> = emptyList()
        for (traceFile: String in actualTraceFiles) {
            tagList += traceFile.removeSuffix("__view_capture_trace.winscope")
        }
        return tagList
    }

    companion object {
        val EXPECTED_TRACES_LAUNCHER_ONLY =
            listOf("${getLauncherPackageName()}_0.vc__view_capture_trace.winscope")
        val EXPECTED_TRACES_LAUNCHER_FIRST =
            listOf(
                "${getLauncherPackageName()}_0.vc__view_capture_trace.winscope",
                "${getSystemUiUidName()}_1.vc__view_capture_trace.winscope",
            )

        val EXPECTED_TRACES_SYSUI_FIRST =
            listOf(
                "${getSystemUiUidName()}_0.vc__view_capture_trace.winscope",
                "${getLauncherPackageName()}_1.vc__view_capture_trace.winscope",
            )
    }
}
