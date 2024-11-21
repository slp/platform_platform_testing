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

package android.tools.testutils

import android.content.Context
import android.tools.Scenario
import android.tools.ScenarioBuilder
import android.tools.ScenarioImpl
import android.tools.Timestamp
import android.tools.Timestamps
import android.tools.io.Reader
import android.tools.io.ResultArtifactDescriptor
import android.tools.io.RunStatus
import android.tools.rules.StopAllTracesRule
import android.tools.testrules.CacheCleanupRule
import android.tools.testrules.InitializeCrossPlatformRule
import android.tools.traces.io.ArtifactBuilder
import android.tools.traces.io.ResultWriter
import android.tools.traces.parsers.perfetto.LayersTraceParser
import android.tools.traces.parsers.perfetto.TraceProcessorSession
import android.tools.traces.parsers.perfetto.WindowManagerTraceParser
import android.tools.traces.parsers.wm.LegacyWindowManagerTraceParser
import android.tools.traces.parsers.wm.WindowManagerDumpParser
import android.tools.traces.wm.ConfigurationContainerImpl
import android.tools.traces.wm.RootWindowContainer
import android.tools.traces.wm.WindowContainerImpl
import android.tools.traces.wm.WindowManagerTrace
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.io.ByteStreams
import com.google.common.truth.Truth
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.ZipInputStream
import kotlin.io.path.createTempDirectory
import kotlin.io.path.name
import org.junit.rules.RuleChain

/** Factory function to create cleanup test rule */
fun CleanFlickerEnvironmentRule(): RuleChain =
    RuleChain.outerRule(InitializeCrossPlatformRule())
        .around(StopAllTracesRule())
        .around(CacheCleanupRule())

val TEST_SCENARIO = ScenarioBuilder().forClass("test").build() as ScenarioImpl

const val SYSTEMUI_PACKAGE = "com.android.systemui"

/**
 * Runs `r` and asserts that an exception with type `expectedThrowable` is thrown.
 *
 * @param r the [Runnable] which is run and expected to throw.
 * @throws AssertionError if `r` does not throw, or throws a runnable that is not an instance of
 *   `expectedThrowable`.
 */
inline fun <reified ExceptionType> assertThrows(r: () -> Unit): ExceptionType {
    try {
        r()
    } catch (t: Throwable) {
        when {
            ExceptionType::class.java.isInstance(t) -> return t as ExceptionType
            t is Exception ->
                throw AssertionError(
                    "Expected ${ExceptionType::class.java}, but got '${t.javaClass}'",
                    t,
                )
            // Re-throw Errors and other non-Exception throwables.
            else -> throw t
        }
    }
    error("Expected exception ${ExceptionType::class.java}, but nothing was thrown")
}

fun assertFail(expectedMessage: String, predicate: () -> Any) {
    val error = assertThrows<AssertionError> { predicate() }
    Truth.assertThat(error).hasMessageThat().contains(expectedMessage)
}

fun assertThatErrorContainsDebugInfo(error: Throwable) {
    Truth.assertThat(error).hasMessageThat().contains("What?")
    Truth.assertThat(error).hasMessageThat().contains("Where?")
}

/**
 * Method to check if the [archivePath] contains trace files from one of the expected files list as
 * given in the [possibleExpectedFiles].
 */
fun assertArchiveContainsFiles(archivePath: File, possibleExpectedFiles: List<List<String>>) {
    Truth.assertWithMessage("Expected trace archive `$archivePath` to exist")
        .that(archivePath.exists())
        .isTrue()

    val actualFiles = getActualTraceFilesFromArchive(archivePath)
    var isActualTraceAsExpected = false

    for (expectedFiles: List<String> in possibleExpectedFiles) {
        if (actualFiles.equalsIgnoreOrder(expectedFiles)) {
            isActualTraceAsExpected = true
            break
        }
    }

    val messageActualFiles = "[${actualFiles.joinToString(", ")}]"
    val messageExpectedFiles =
        "${possibleExpectedFiles.map { "[${it.joinToString(", ")}]"}.joinToString(", ")}"
    Truth.assertWithMessage(
            "Trace archive doesn't contain expected traces." +
                "\n Actual: $messageActualFiles" +
                "\n Expected: $messageExpectedFiles"
        )
        .that(isActualTraceAsExpected)
        .isTrue()
}

fun getActualTraceFilesFromArchive(archivePath: File): List<String> {
    val archiveStream = ZipInputStream(FileInputStream(archivePath))
    return generateSequence { archiveStream.nextEntry }.map { it.name }.toList()
}

fun <T> List<T>.equalsIgnoreOrder(other: List<T>) = this.toSet() == other.toSet()

fun getWmTraceReaderFromAsset(
    relativePathWithoutExtension: String,
    from: Long = Long.MIN_VALUE,
    to: Long = Long.MAX_VALUE,
    addInitialEntry: Boolean = true,
    legacyTrace: Boolean = false,
): Reader {
    fun parseTrace(): WindowManagerTrace {
        val traceData = readAsset("$relativePathWithoutExtension.perfetto-trace")
        return TraceProcessorSession.loadPerfettoTrace(traceData) { session ->
            WindowManagerTraceParser()
                .parse(
                    session,
                    Timestamps.from(elapsedNanos = from),
                    Timestamps.from(elapsedNanos = to),
                )
        }
    }

    fun parseLegacyTrace(): WindowManagerTrace {
        val traceData =
            runCatching { readAsset("$relativePathWithoutExtension.pb") }.getOrNull()
                ?: runCatching { readAsset("$relativePathWithoutExtension.winscope") }.getOrNull()
                ?: error("Can't find legacy trace file $relativePathWithoutExtension")

        return LegacyWindowManagerTraceParser(legacyTrace)
            .parse(
                traceData,
                Timestamps.from(elapsedNanos = from),
                Timestamps.from(elapsedNanos = to),
                addInitialEntry,
                clearCache = false,
            )
    }

    val trace =
        if (android.tracing.Flags.perfettoWmTracing()) {
            parseTrace()
        } else {
            parseLegacyTrace()
        }

    return ParsedTracesReader(
        artifact = TestArtifact(relativePathWithoutExtension),
        wmTrace = trace,
    )
}

fun getWmDumpReaderFromAsset(relativePathWithoutExtension: String): Reader {
    fun parseDump(): WindowManagerTrace {
        val traceData = readAsset("$relativePathWithoutExtension.perfetto-trace")
        return TraceProcessorSession.loadPerfettoTrace(traceData) { session ->
            WindowManagerTraceParser().parse(session)
        }
    }

    fun parseLegacyDump(): WindowManagerTrace {
        val traceData =
            runCatching { readAsset("$relativePathWithoutExtension.pb") }.getOrNull()
                ?: runCatching { readAsset("$relativePathWithoutExtension.winscope") }.getOrNull()
                ?: error("Can't find legacy trace file $relativePathWithoutExtension")
        return WindowManagerDumpParser().parse(traceData, clearCache = false)
    }

    val wmTrace =
        if (android.tracing.Flags.perfettoWmDump()) {
            parseDump()
        } else {
            parseLegacyDump()
        }
    return ParsedTracesReader(
        artifact = TestArtifact(relativePathWithoutExtension),
        wmTrace = wmTrace,
    )
}

fun getLayerTraceReaderFromAsset(
    relativePath: String,
    ignoreOrphanLayers: Boolean = true,
    from: Timestamp = Timestamps.min(),
    to: Timestamp = Timestamps.max(),
): Reader {
    val layersTrace =
        TraceProcessorSession.loadPerfettoTrace(readAsset(relativePath)) { session ->
            LayersTraceParser(
                    ignoreLayersStackMatchNoDisplay = false,
                    ignoreLayersInVirtualDisplay = false,
                ) {
                    ignoreOrphanLayers
                }
                .parse(session, from, to)
        }
    return ParsedTracesReader(artifact = TestArtifact(relativePath), layersTrace = layersTrace)
}

@Throws(Exception::class)
fun readAsset(relativePath: String): ByteArray {
    val context: Context = InstrumentationRegistry.getInstrumentation().context
    val inputStream = context.resources.assets.open("testdata/$relativePath")
    return ByteStreams.toByteArray(inputStream)
}

@Throws(IOException::class)
fun readAssetAsFile(relativePath: String): File {
    val context: Context = InstrumentationRegistry.getInstrumentation().context
    return File(context.cacheDir, relativePath).also {
        if (!it.exists()) {
            it.outputStream().use { cache ->
                context.assets.open("testdata/$relativePath").use { inputStream ->
                    inputStream.copyTo(cache)
                }
            }
        }
    }
}

fun newTestResultWriter(
    scenario: Scenario = ScenarioBuilder().forClass(kotlin.io.path.createTempFile().name).build()
) =
    ResultWriter()
        .forScenario(scenario)
        .withOutputDir(createTempDirectory().toFile())
        .setRunComplete()

fun assertExceptionMessage(error: Throwable?, expectedValue: String) {
    Truth.assertWithMessage("Expected exception")
        .that(error)
        .hasMessageThat()
        .contains(expectedValue)
}

fun outputFileName(status: RunStatus) =
    File("/sdcard/flicker/${status.prefix}__test_ROTATION_0_GESTURAL_NAV.zip")

fun createDefaultArtifactBuilder(
    status: RunStatus,
    outputDir: File = createTempDirectory().toFile(),
    files: Map<ResultArtifactDescriptor, File> = emptyMap(),
) =
    ArtifactBuilder()
        .withScenario(TEST_SCENARIO)
        .withOutputDir(outputDir)
        .withStatus(status)
        .withFiles(files)

fun getLauncherPackageName() =
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).launcherPackageName

fun getSystemUiUidName(): String {
    val packageManager = InstrumentationRegistry.getInstrumentation().context.getPackageManager()
    val uid = packageManager.getApplicationInfo(SYSTEMUI_PACKAGE, 0).uid
    return requireNotNull(packageManager.getNameForUid(uid))
}

fun newEmptyRootContainer(orientation: Int = 0, layerId: Int = 0) =
    RootWindowContainer(
        WindowContainerImpl(
            title = "root",
            token = "",
            orientation = orientation,
            layerId = layerId,
            _isVisible = true,
            _children = emptyList(),
            configurationContainer = ConfigurationContainerImpl.EMPTY,
            computedZ = 0,
        )
    )
