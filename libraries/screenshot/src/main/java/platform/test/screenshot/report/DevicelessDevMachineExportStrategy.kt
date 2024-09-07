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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.outputStream
import kotlin.io.path.writeText
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.proto.ScreenshotResultProto

/**
 * Writes the bitmap diff results on the local development machine.
 *
 * Useful during development of deviceless tests.
 *
 * TODO(b/322324387) Cleanup code - this is copied without modification from
 *   http://shortn/_nZmmj8v5zv for reviewability.
 */
internal class DevicelessDevMachineExportStrategy(
    private val goldenPathManager: GoldenPathManager,
) : DiffResultExportStrategy {
    private val imageExtension = ".png"

    override fun reportResult(
        testIdentifier: String,
        goldenIdentifier: String,
        actual: Bitmap,
        status: ScreenshotResultProto.DiffResult.Status,
        comparisonStatistics: ScreenshotResultProto.DiffResult.ComparisonStatistics?,
        expected: Bitmap?,
        diff: Bitmap?
    ) {
        val localDir = Paths.get("/tmp/screenshots")
        val actualDir = localDir.resolve("actual")
        val expectedDir = localDir.resolve("expected")
        val diffDir = localDir.resolve("diff")
        val reportDir = localDir.resolve("report")

        val androidBuildTopDir = System.getenv("ANDROID_BUILD_TOP")
        val androidBuildTop =
            if (androidBuildTopDir != null) {
                Paths.get(androidBuildTopDir)
            } else {
                null
            }
        val assetsDir = androidBuildTop?.resolve(goldenPathManager.assetsPathRelativeToBuildRoot)
        val imagePath = goldenPathManager.goldenImageIdentifierResolver(goldenIdentifier)
        val actualImagePath = actualDir.resolve(imagePath)
        val expectedImagePath = expectedDir.resolve(imagePath)
        val diffImagePath = diffDir.resolve(imagePath)

        actual.writeTo(actualImagePath)
        if (assetsDir != null) {
            actual.writeTo(assetsDir.resolve(imagePath))
        }
        expected?.writeTo(expectedImagePath)
        diff?.writeTo(diffImagePath)

        check(imagePath.endsWith(imageExtension))

        val reportPath =
            reportDir.resolve(
                imagePath.substring(0, imagePath.length - imageExtension.length) + ".html"
            )

        println("file://$reportPath")
        Files.createDirectories(reportPath.parent)

        fun html(bitmap: Bitmap?, image: Path, name: String, alt: String): String {
            return if (bitmap == null) {
                ""
            } else {
                """
                    <p>
                        <h2><a href="file://$image">$name</a></h2>
                        <img src="$image" alt="$alt"/>
                    </p>
                """
                    .trimIndent()
            }
        }

        reportPath.writeText(
            """
                <!DOCTYPE html>
                <meta charset="utf-8">
                <title>$imagePath</title>
                <p><h1>$testIdentifier</h1></p>
                ${html(expected, expectedImagePath, "Expected", "Golden")}
                ${html(actual, actualImagePath, "Actual", "Actual")}
                ${html(diff, diffImagePath, "Diff", "Diff")}
            """
                .trimIndent()
        )
    }

    private fun Bitmap.writeTo(path: Path) {
        // Make sure we either create a new file or overwrite an existing one.
        check(!Files.exists(path) || Files.isRegularFile(path))

        // Make sure the parent directory exists.
        Files.createDirectories(path.parent)

        // Write the Bitmap to the given file.
        path.outputStream().use { stream ->
            this@writeTo.compress(Bitmap.CompressFormat.PNG, 0, stream)
        }
    }
}
