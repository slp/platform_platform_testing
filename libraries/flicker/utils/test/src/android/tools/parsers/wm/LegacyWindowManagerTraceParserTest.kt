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

package android.tools.parsers.wm

import android.tools.Cache
import android.tools.testutils.CleanFlickerEnvironmentRule
import android.tools.testutils.readAsset
import android.tools.traces.SERVICE_TRACE_CONFIG
import android.tools.traces.io.ResultReader
import android.tools.traces.monitors.wm.WindowManagerTraceMonitor
import android.tools.traces.parsers.wm.LegacyWindowManagerTraceParser
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth
import org.junit.Assume
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test

/** Tests for [LegacyWindowManagerTraceParser] */
class LegacyWindowManagerTraceParserTest {
    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun canParseAllEntriesFromStoredTrace() {
        val trace =
            LegacyWindowManagerTraceParser(legacyTrace = true)
                .parse(readAsset("wm_trace_openchrome.pb"), clearCache = false)
        val firstEntry = trace.entries.first()
        Truth.assertThat(firstEntry.timestamp.elapsedNanos).isEqualTo(9213763541297L)
        Truth.assertThat(firstEntry.windowStates.size).isEqualTo(10)
        Truth.assertThat(firstEntry.visibleWindows.size).isEqualTo(5)
        Truth.assertThat(trace.entries.last().timestamp.elapsedNanos).isEqualTo(9216093628925L)
    }

    @Test
    fun canParseAllEntriesFromNewTrace() {
        Assume.assumeFalse(android.tracing.Flags.perfettoWmTracing())

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val reader =
            WindowManagerTraceMonitor().withTracing(
                resultReaderProvider = { ResultReader(it, SERVICE_TRACE_CONFIG) }
            ) {
                device.pressHome()
                device.pressRecentApps()
            }
        val trace = reader.readWmTrace()
        Truth.assertThat(trace?.entries).isNotEmpty()
    }

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRule()
    }
}
