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

package platform.test.screenshot.report

import android.graphics.Bitmap
import android.os.Build
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.proto.ScreenshotResultProto.DiffResult

/**
 * Strategy to "export" the results of a bitmap diff to an external system, such as Scuba.
 *
 * This allows to implement a strategy to pass the results to the external system which is
 * appropriate for the environment the Android test is running in.
 */
interface DiffResultExportStrategy {
    /**
     * TODO(b/322324387) Simplify this interface. Right now, this is just factoring out the existing
     *   call.
     */
    fun reportResult(
        testIdentifier: String,
        goldenIdentifier: String,
        actual: Bitmap,
        status: DiffResult.Status,
        comparisonStatistics: DiffResult.ComparisonStatistics? = null,
        expected: Bitmap? = null,
        diff: Bitmap? = null
    )

    companion object {
        /** Creates the export strategy to be used in Android tests. */
        fun createDefaultStrategy(goldenPathManager: GoldenPathManager): DiffResultExportStrategy {
            val exportStrategy = ExportToScubaStrategy(goldenPathManager)

            val isRobolectric = Build.FINGERPRINT.contains("robolectric")
            val doesWriteScreenshotToLocal =
                "yes".equals(
                    System.getProperty("screenshot.writeScreenshotToLocal"), ignoreCase = true)

            return if (isRobolectric && doesWriteScreenshotToLocal) {
                MultiplexedStrategy(
                    listOf(exportStrategy, DevicelessDevMachineExportStrategy(goldenPathManager))
                )
            } else {
                exportStrategy
            }
        }
    }
}

/** Delegates [reportResult] calls to all [strategies]. */
private class MultiplexedStrategy(private val strategies: List<DiffResultExportStrategy>) :
    DiffResultExportStrategy {
    override fun reportResult(
        testIdentifier: String,
        goldenIdentifier: String,
        actual: Bitmap,
        status: DiffResult.Status,
        comparisonStatistics: DiffResult.ComparisonStatistics?,
        expected: Bitmap?,
        diff: Bitmap?
    ) {
        strategies.forEach {
            it.reportResult(
                testIdentifier,
                goldenIdentifier,
                actual,
                status,
                comparisonStatistics,
                expected,
                diff
            )
        }
    }
}
