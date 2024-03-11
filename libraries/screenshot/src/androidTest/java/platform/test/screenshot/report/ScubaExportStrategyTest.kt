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

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.PathConfig
import platform.test.screenshot.PathElementNoContext
import platform.test.screenshot.getDeviceOutputDirectory
import platform.test.screenshot.proto.ScreenshotResultProto.DiffResult
import platform.test.screenshot.report.OutputFileType.IMAGE_ACTUAL
import platform.test.screenshot.report.OutputFileType.IMAGE_DIFF
import platform.test.screenshot.report.OutputFileType.IMAGE_EXPECTED
import platform.test.screenshot.report.OutputFileType.RESULT_BIN_PROTO
import platform.test.screenshot.report.OutputFileType.RESULT_PROTO
import platform.test.screenshot.utils.createBitmap

@RunWith(AndroidJUnit4::class)
class ScubaExportStrategyTest {

    private val testId = "test-id"
    private val goldenId = "golden-id"

    // Helper to avoid repeating constant " testId, goldenId"
    private fun ExportToScubaStrategy.getPathOnDeviceFor(fileType: OutputFileType) =
        getPathOnDeviceFor(fileType, goldenId, testId)

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().context

    @After
    fun cleanupFiles() {
        // Make sure to clean-up the files, since all tests us the same testId/goldenId
        File(getDeviceOutputDirectory(context)).deleteRecursively()
    }

    @Test
    fun getPathOnDeviceFor_emptyPathConfig_filenameIncludesPathElement() {
        val subject = ExportToScubaStrategy(GoldenPathManager(context, pathConfig = PathConfig()))

        assertThat(subject.getPathOnDeviceFor(IMAGE_ACTUAL).name)
            .isEqualTo("test-id_actual_golden-id.png")
        assertThat(subject.getPathOnDeviceFor(IMAGE_EXPECTED).name)
            .isEqualTo("test-id_expected_golden-id.png")
        assertThat(subject.getPathOnDeviceFor(IMAGE_DIFF).name)
            .isEqualTo("test-id_diff_golden-id.png")
        assertThat(subject.getPathOnDeviceFor(RESULT_PROTO).name)
            .isEqualTo("test-id_golden-id_goldResult.textproto")
        assertThat(subject.getPathOnDeviceFor(RESULT_BIN_PROTO).name)
            .isEqualTo("test-id_golden-id_goldResult.pb")
    }

    @Test
    fun getPathOnDeviceFor_directoryElement_filenameIncludesPathElement() {
        val pathConfig = PathConfig(PathElementNoContext("foo", isDir = true) { "bar" })
        val subject = ExportToScubaStrategy(GoldenPathManager(context, pathConfig = pathConfig))

        assertThat(subject.getPathOnDeviceFor(IMAGE_ACTUAL).name)
            .isEqualTo("test-id_actual_bar_golden-id.png")
        assertThat(subject.getPathOnDeviceFor(IMAGE_EXPECTED).name)
            .isEqualTo("test-id_expected_bar_golden-id.png")
        assertThat(subject.getPathOnDeviceFor(IMAGE_DIFF).name)
            .isEqualTo("test-id_diff_bar_golden-id.png")
        assertThat(subject.getPathOnDeviceFor(RESULT_PROTO).name)
            .isEqualTo("test-id_bar_golden-id_goldResult.textproto")
        assertThat(subject.getPathOnDeviceFor(RESULT_BIN_PROTO).name)
            .isEqualTo("test-id_bar_golden-id_goldResult.pb")
    }

    @Test
    fun getPathOnDeviceFor_nonDirectoryElement_filenameIncludesPathElement() {
        val pathConfig = PathConfig(PathElementNoContext("foo", isDir = false) { "bar" })
        val subject = ExportToScubaStrategy(GoldenPathManager(context, pathConfig = pathConfig))

        assertThat(subject.getPathOnDeviceFor(IMAGE_ACTUAL).name)
            .isEqualTo("test-id_actual_bar_golden-id.png")
        assertThat(subject.getPathOnDeviceFor(IMAGE_EXPECTED).name)
            .isEqualTo("test-id_expected_bar_golden-id.png")
        assertThat(subject.getPathOnDeviceFor(IMAGE_DIFF).name)
            .isEqualTo("test-id_diff_bar_golden-id.png")
        assertThat(subject.getPathOnDeviceFor(RESULT_PROTO).name)
            .isEqualTo("test-id_bar_golden-id_goldResult.textproto")
        assertThat(subject.getPathOnDeviceFor(RESULT_BIN_PROTO).name)
            .isEqualTo("test-id_bar_golden-id_goldResult.pb")
    }

    @Test
    fun getPathOnDeviceFor_multiplePathConfigs_filenameIncludesPathElement() {
        val pathConfig =
            PathConfig(
                PathElementNoContext("foo", isDir = true) { "bar" },
                PathElementNoContext("one", isDir = false) { "two" }
            )
        val subject = ExportToScubaStrategy(GoldenPathManager(context, pathConfig = pathConfig))

        assertThat(subject.getPathOnDeviceFor(IMAGE_ACTUAL).name)
            .isEqualTo("test-id_actual_bar_two_golden-id.png")
        assertThat(subject.getPathOnDeviceFor(IMAGE_EXPECTED).name)
            .isEqualTo("test-id_expected_bar_two_golden-id.png")
        assertThat(subject.getPathOnDeviceFor(IMAGE_DIFF).name)
            .isEqualTo("test-id_diff_bar_two_golden-id.png")
        assertThat(subject.getPathOnDeviceFor(RESULT_PROTO).name)
            .isEqualTo("test-id_bar_two_golden-id_goldResult.textproto")
        assertThat(subject.getPathOnDeviceFor(RESULT_BIN_PROTO).name)
            .isEqualTo("test-id_bar_two_golden-id_goldResult.pb")
    }

    @Test
    fun reportResult_withoutOptionalArgs_writesActualAndProto() {
        val subject = ExportToScubaStrategy(GoldenPathManager(context))

        subject.reportResult(
            testId,
            goldenId,
            status = DiffResult.Status.PASSED,
            actual = createBitmap(Color.GREEN),
        )

        assertThat(subject.getPathOnDeviceFor(IMAGE_ACTUAL).exists()).isTrue()
        assertThat(subject.getPathOnDeviceFor(RESULT_BIN_PROTO).exists()).isTrue()
        assertThat(subject.getPathOnDeviceFor(RESULT_PROTO).exists()).isTrue()
        assertThat(subject.getPathOnDeviceFor(IMAGE_DIFF).exists()).isFalse()
        assertThat(subject.getPathOnDeviceFor(IMAGE_EXPECTED).exists()).isFalse()
    }

    @Test
    fun reportResult_withDiff_writesDiff() {
        val subject = ExportToScubaStrategy(GoldenPathManager(context))

        subject.reportResult(
            testId,
            goldenId,
            status = DiffResult.Status.PASSED,
            actual = createBitmap(Color.GREEN),
            diff = createBitmap(Color.YELLOW),
        )

        assertThat(subject.getPathOnDeviceFor(IMAGE_ACTUAL).exists()).isTrue()
        assertThat(subject.getPathOnDeviceFor(RESULT_BIN_PROTO).exists()).isTrue()
        assertThat(subject.getPathOnDeviceFor(RESULT_PROTO).exists()).isTrue()
        assertThat(subject.getPathOnDeviceFor(IMAGE_DIFF).exists()).isTrue()
        assertThat(subject.getPathOnDeviceFor(IMAGE_EXPECTED).exists()).isFalse()
    }

    @Test
    fun reportResult_withExpected_writesExpected() {
        val subject = ExportToScubaStrategy(GoldenPathManager(context))

        subject.reportResult(
            testId,
            goldenId,
            status = DiffResult.Status.PASSED,
            actual = createBitmap(Color.GREEN),
            expected = createBitmap(Color.RED),
        )

        assertThat(subject.getPathOnDeviceFor(IMAGE_ACTUAL).exists()).isTrue()
        assertThat(subject.getPathOnDeviceFor(RESULT_BIN_PROTO).exists()).isTrue()
        assertThat(subject.getPathOnDeviceFor(RESULT_PROTO).exists()).isTrue()
        assertThat(subject.getPathOnDeviceFor(IMAGE_DIFF).exists()).isFalse()
        assertThat(subject.getPathOnDeviceFor(IMAGE_EXPECTED).exists()).isTrue()
    }

    @Test
    fun reportResult_passed_writesStatus() {
        val subject = ExportToScubaStrategy(GoldenPathManager(context))

        subject.reportResult(
            testId,
            goldenId,
            status = DiffResult.Status.PASSED,
            actual = createBitmap(Color.GREEN),
        )

        assertThat(subject.getPathOnDeviceFor(RESULT_PROTO).readText()).contains("PASSED")
    }

    @Test
    fun reportResult_failed_writesStatus() {
        val subject = ExportToScubaStrategy(GoldenPathManager(context))

        subject.reportResult(
            testId,
            goldenId,
            status = DiffResult.Status.FAILED,
            actual = createBitmap(Color.GREEN),
        )

        assertThat(subject.getPathOnDeviceFor(RESULT_PROTO).readText()).contains("FAILED")
    }

    @Test
    fun reportResult_writesCorrectImageContents() {
        val subject = ExportToScubaStrategy(GoldenPathManager(context))

        subject.reportResult(
            testId,
            goldenId,
            status = DiffResult.Status.PASSED,
            actual = createBitmap(Color.RED),
            diff = createBitmap(Color.GREEN),
            expected = createBitmap(Color.BLUE),
        )

        fun extractColorSample(fileType: OutputFileType): Color {
            val path = subject.getPathOnDeviceFor(fileType).absolutePath
            return BitmapFactory.decodeFile(path).getColor(0, 0)
        }

        assertThat(extractColorSample(IMAGE_ACTUAL)).isEqualTo(Color.valueOf(Color.RED))
        assertThat(extractColorSample(IMAGE_DIFF)).isEqualTo(Color.valueOf(Color.GREEN))
        assertThat(extractColorSample(IMAGE_EXPECTED)).isEqualTo(Color.valueOf(Color.BLUE))
    }

    @Test
    fun reportResult_duplicateCall_doesNotOverwriteFile() {
        // Unsure why this is a desired behavior, but there was an explicit test for this before
        // the refactoring, hence carrying it forward.
        val subject = ExportToScubaStrategy(GoldenPathManager(context))

        subject.reportResult(
            testId,
            goldenId,
            status = DiffResult.Status.PASSED,
            actual = createBitmap(Color.RED),
            diff = createBitmap(Color.GREEN),
            expected = createBitmap(Color.BLUE),
        )

        subject.reportResult(
            testId,
            goldenId,
            status = DiffResult.Status.FAILED,
            actual = createBitmap(Color.BLACK),
            diff = createBitmap(Color.BLACK),
            expected = createBitmap(Color.BLACK),
        )

        fun extractColorSample(fileType: OutputFileType): Color {
            val path = subject.getPathOnDeviceFor(fileType).absolutePath
            return BitmapFactory.decodeFile(path).getColor(0, 0)
        }

        assertThat(subject.getPathOnDeviceFor(RESULT_PROTO).readText()).contains("PASSED")
        assertThat(extractColorSample(IMAGE_ACTUAL)).isEqualTo(Color.valueOf(Color.RED))
        assertThat(extractColorSample(IMAGE_DIFF)).isEqualTo(Color.valueOf(Color.GREEN))
        assertThat(extractColorSample(IMAGE_EXPECTED)).isEqualTo(Color.valueOf(Color.BLUE))
    }
}
