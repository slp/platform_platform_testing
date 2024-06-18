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
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.proto.ScreenshotResultProto

/**
 * Writes bitmap diff results to the local test device, for Trade Federation to pick them up and
 * upload to Scuba.
 *
 * TODO(b/322324387) Cleanup code - this is copied with only minor modifications (`testIdentifier`
 *   is now an argument rather than a member) from http://shortn/_7AMZiumx0f for reviewability.
 */
class ExportToScubaStrategy(
    private val goldenPathManager: GoldenPathManager,
) : DiffResultExportStrategy {
    private val imageExtension = ".png"
    private val resultBinaryProtoFileSuffix = "goldResult.pb"

    // This is used in CI to identify the files.
    private val resultProtoFileSuffix = "goldResult.textproto"

    // Magic number for an in-progress status report
    private val bundleStatusInProgress = 2
    private val bundleKeyPrefix = "platform_screenshots_"

    override fun reportResult(
        testIdentifier: String,
        goldenIdentifier: String,
        actual: Bitmap,
        status: ScreenshotResultProto.DiffResult.Status,
        comparisonStatistics: ScreenshotResultProto.DiffResult.ComparisonStatistics?,
        expected: Bitmap?,
        diff: Bitmap?
    ) {
        val resultProto =
            ScreenshotResultProto.DiffResult.newBuilder()
                .setResultType(status)
                .addMetadata(
                    ScreenshotResultProto.Metadata.newBuilder()
                        .setKey("repoRootPath")
                        .setValue(goldenPathManager.deviceLocalPath)
                )

        if (comparisonStatistics != null) {
            resultProto.comparisonStatistics = comparisonStatistics
        }

        val pathRelativeToAssets = goldenPathManager.goldenImageIdentifierResolver(goldenIdentifier)
        resultProto.imageLocationGolden =
            "${goldenPathManager.assetsPathRelativeToBuildRoot}/$pathRelativeToAssets"

        val report = Bundle()

        actual.writeToDevice(OutputFileType.IMAGE_ACTUAL, goldenIdentifier, testIdentifier).also {
            resultProto.imageLocationTest = it.name
            report.putString(bundleKeyPrefix + OutputFileType.IMAGE_ACTUAL, it.absolutePath)
        }
        diff?.run {
            writeToDevice(OutputFileType.IMAGE_DIFF, goldenIdentifier, testIdentifier).also {
                resultProto.imageLocationDiff = it.name
                report.putString(bundleKeyPrefix + OutputFileType.IMAGE_DIFF, it.absolutePath)
            }
        }
        expected?.run {
            writeToDevice(OutputFileType.IMAGE_EXPECTED, goldenIdentifier, testIdentifier).also {
                resultProto.imageLocationReference = it.name
                report.putString(bundleKeyPrefix + OutputFileType.IMAGE_EXPECTED, it.absolutePath)
            }
        }

        writeToDevice(OutputFileType.RESULT_PROTO, goldenIdentifier, testIdentifier) {
                it.write(resultProto.build().toString().toByteArray())
            }
            .also {
                report.putString(bundleKeyPrefix + OutputFileType.RESULT_PROTO, it.absolutePath)
            }

        writeToDevice(OutputFileType.RESULT_BIN_PROTO, goldenIdentifier, testIdentifier) {
                it.write(resultProto.build().toByteArray())
            }
            .also {
                report.putString(bundleKeyPrefix + OutputFileType.RESULT_BIN_PROTO, it.absolutePath)
            }

        InstrumentationRegistry.getInstrumentation().sendStatus(bundleStatusInProgress, report)
    }

    internal fun getPathOnDeviceFor(
        fileType: OutputFileType,
        goldenIdentifier: String,
        testIdentifier: String,
    ): File {
        val imageSuffix = getOnDeviceImageSuffix(goldenIdentifier)
        val protoSuffix = getOnDeviceArtifactsSuffix(goldenIdentifier, resultProtoFileSuffix)
        val binProtoSuffix =
            getOnDeviceArtifactsSuffix(goldenIdentifier, resultBinaryProtoFileSuffix)
        val succinctTestIdentifier = getSuccinctTestIdentifier(testIdentifier)
        val fileName =
            when (fileType) {
                OutputFileType.IMAGE_ACTUAL -> "${succinctTestIdentifier}_actual_$imageSuffix"
                OutputFileType.IMAGE_EXPECTED -> "${succinctTestIdentifier}_expected_$imageSuffix"
                OutputFileType.IMAGE_DIFF -> "${succinctTestIdentifier}_diff_$imageSuffix"
                OutputFileType.RESULT_PROTO -> "${succinctTestIdentifier}_$protoSuffix"
                OutputFileType.RESULT_BIN_PROTO -> "${succinctTestIdentifier}_$binProtoSuffix"
            }
        return File(goldenPathManager.deviceLocalPath, fileName)
    }

    private fun getOnDeviceImageSuffix(goldenIdentifier: String): String {
        val resolvedGoldenIdentifier =
            goldenPathManager
                .goldenImageIdentifierResolver(goldenIdentifier)
                .replace('/', '_')
                .replace(imageExtension, "")
        return "$resolvedGoldenIdentifier$imageExtension"
    }

    private fun getOnDeviceArtifactsSuffix(goldenIdentifier: String, suffix: String): String {
        val resolvedGoldenIdentifier =
            goldenPathManager
                .goldenImageIdentifierResolver(goldenIdentifier)
                .replace('/', '_')
                .replace(imageExtension, "")
        return "${resolvedGoldenIdentifier}_$suffix"
    }

    private fun getSuccinctTestIdentifier(identifier: String): String {
        val pattern = Regex("\\[([A-Za-z0-9_]+)\\]")
        return pattern.replace(identifier, "")
    }

    private fun Bitmap.writeToDevice(
        fileType: OutputFileType,
        goldenIdentifier: String,
        testIdentifier: String,
    ): File {
        return writeToDevice(fileType, goldenIdentifier, testIdentifier) {
            compress(Bitmap.CompressFormat.PNG, 0 /*ignored for png*/, it)
        }
    }

    private fun writeToDevice(
        fileType: OutputFileType,
        goldenIdentifier: String,
        testIdentifier: String,
        writeAction: (FileOutputStream) -> Unit
    ): File {
        val fileGolden = File(goldenPathManager.deviceLocalPath)
        if (!fileGolden.exists() && !fileGolden.mkdirs()) {
            throw IOException("Could not create folder $fileGolden.")
        }

        val file = getPathOnDeviceFor(fileType, goldenIdentifier, testIdentifier)
        if (!file.exists()) {
            // file typically exists when in one test, the same golden image was repeatedly
            // compared with. In this scenario, multiple actual/expected/diff images with same
            // names will be attempted to write to the device.
            try {
                FileOutputStream(file).use { writeAction(it) }
            } catch (e: Exception) {
                throw IOException(
                    "Could not write file to storage (path: ${file.absolutePath}). ",
                    e
                )
            }
        }

        return file
    }
}

/** Type of file that can be produced by the [ExportToScubaStrategy]. */
internal enum class OutputFileType {
    IMAGE_ACTUAL,
    IMAGE_EXPECTED,
    IMAGE_DIFF,
    RESULT_PROTO,
    RESULT_BIN_PROTO
}
