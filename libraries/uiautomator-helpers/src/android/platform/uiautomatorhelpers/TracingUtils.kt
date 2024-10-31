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
package android.platform.uiautomatorhelpers

import android.os.Trace
import android.util.Log

/** Tracing utils until androidx tracing library is updated in the tree. */
@Deprecated("Use com.android.app.tracing utils instead.")
object TracingUtils {

    // from frameworks/base/core/java/android/os/Trace.java MAX_SECTION_NAME_LEN.
    private const val MAX_TRACE_NAME_LEN = 127
    private const val TAG = "TracingUtils"

    @Deprecated(
        "Use com.android.app.tracing.traceSection instead",
        replaceWith = ReplaceWith("com.android.app.tracing.traceSection(sectionName, block)")
    )
    inline fun <T> trace(sectionName: String, block: () -> T): T {
        Trace.beginSection(sectionName.shortenedIfNeeded())
        try {
            return block()
        } finally {
            Trace.endSection()
        }
    }

    /** Shortens the section name if it's too long. */
    fun beginSectionSafe(sectionName: String) {
        Trace.beginSection(sectionName.shortenedIfNeeded())
    }

    /** Shorten the length of a string to make it less than the limit for atraces. */
    fun String.shortenedIfNeeded(): String =
        if (length > MAX_TRACE_NAME_LEN) {
            Log.w(TAG, "Section name too long: \"$this\" (len=$length, max=$MAX_TRACE_NAME_LEN)")
            substring(0, MAX_TRACE_NAME_LEN)
        } else this
}
