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

package android.tools.traces.parsers.perfetto

import android.tools.parsers.AbstractTraceParser
import android.tools.traces.wm.WindowManagerState
import android.tools.traces.wm.WindowManagerTrace

/** Parser for [WindowManagerTrace] objects containing traces */
class WindowManagerTraceParser :
    AbstractTraceParser<
        TraceProcessorSession,
        WindowManagerState,
        WindowManagerState,
        WindowManagerTrace,
    >() {
    override val traceName: String = "WM Trace"

    override fun doDecodeByteArray(bytes: ByteArray): TraceProcessorSession {
        error("This parser can only read from perfetto trace processor")
    }

    override fun createTrace(entries: Collection<WindowManagerState>): WindowManagerTrace =
        WindowManagerTrace(entries)

    override fun getEntries(input: TraceProcessorSession): List<WindowManagerState> {
        return input.query("INCLUDE PERFETTO MODULE android.winscope.windowmanager;") {
            val realToElapsedTimeOffsetNs = queryRealToElapsedTimeOffsetNs(input, TABLE_NAME)

            val traceEntries = mutableListOf<WindowManagerState>()
            val entryIds = queryEntryIds(input)

            for (entryId in entryIds) {
                val entry =
                    input.query(getSqlQueryEntry(entryId)) { rows ->
                        val args = Args.build(rows)
                        WindowManagerStateBuilder(args, realToElapsedTimeOffsetNs).build()
                    }
                traceEntries.add(entry)
            }

            traceEntries
        }
    }

    override fun getTimestamp(entry: WindowManagerState) = entry.timestamp

    override fun doParseEntry(entry: WindowManagerState) = entry

    companion object {
        val TABLE_NAME = "android_windowmanager"

        private fun queryEntryIds(input: TraceProcessorSession): List<Long> {
            val sql =
                """
                SELECT id FROM $TABLE_NAME ORDER BY ts;
            """
                    .trimIndent()
            return input.query(sql) { rows ->
                val ids = rows.map { it["id"] as Long }
                ids
            }
        }

        private fun getSqlQueryEntry(entryId: Long): String {
            return """
                SELECT
                    args.key as key,
                    args.display_value as value,
                    args.value_type
                FROM
                    $TABLE_NAME as wm
                INNER JOIN args ON wm.arg_set_id = args.arg_set_id
                WHERE wm.id = $entryId;
            """
                .trimIndent()
        }
    }
}
