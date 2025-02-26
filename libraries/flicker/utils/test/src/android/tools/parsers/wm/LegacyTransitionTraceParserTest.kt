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
import android.tools.traces.parsers.perfetto.TraceProcessorSession
import android.tools.traces.parsers.perfetto.TransitionsTraceParser
import android.tools.traces.parsers.wm.LegacyTransitionTraceParser
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test

class LegacyTransitionTraceParserTest {
    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun canParseAllEntriesFromLegacyStoredTrace() {
        val trace =
            LegacyTransitionTraceParser()
                .parse(
                    wmSideTraceData = readAsset("wm_transition_trace.winscope"),
                    shellSideTraceData = readAsset("shell_transition_trace.winscope"),
                )
        val firstEntry = trace.entries.first()
        Truth.assertThat(firstEntry.timestamp.elapsedNanos).isEqualTo(760760231809L)
        Truth.assertThat(firstEntry.id).isEqualTo(9)

        val lastEntry = trace.entries.last()
        Truth.assertThat(lastEntry.timestamp.elapsedNanos).isEqualTo(2770105426934L)
    }

    @Test
    fun canParseAllEntriesFromStoredTrace() {
        val trace =
            TraceProcessorSession.loadPerfettoTrace(readAsset("transitions.perfetto-trace")) {
                session ->
                TransitionsTraceParser().parse(session)
            }
        Truth.assertWithMessage("Trace").that(trace.entries).isNotEmpty()
        Truth.assertWithMessage("Trace contains entry")
            .that(trace.entries.map { it.id })
            .contains(35)
    }

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRule()
    }
}
