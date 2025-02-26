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

package platform.test.motion

import android.util.Log
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.Volatile
import org.json.JSONObject
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement
import platform.test.motion.golden.DataPointType
import platform.test.motion.golden.JsonGoldenSerializer
import platform.test.motion.golden.TimeSeries
import platform.test.motion.truth.RecordedMotionSubject
import platform.test.screenshot.BitmapDiffer
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.report.ExportToScubaStrategy

/**
 * Test rule to verify correctness of animations and other time-based state.
 *
 * Capture a time-series of values, at specified intervals, during an animation. Additionally, a
 * screenshot is captured along each data frame, to simplify verification of the test setup as well
 * as debugging.
 *
 * To capture the animation, use the [Toolkit]-provided extension functions. See for example
 * `ComposeToolkit` and `ViewToolkit`.
 *
 * @param toolkit Environment specific implementation.
 * @param goldenPathManager Specifies how to locate the golden files.
 * @param bitmapDiffer A optional `ScreenshotTestRule` to enable support of `filmstripMatchesGolden`
 */
class MotionTestRule<Toolkit>(
    val toolkit: Toolkit,
    private val goldenPathManager: GoldenPathManager,
    internal val bitmapDiffer: BitmapDiffer? = null,
    extraRules: RuleChain = RuleChain.emptyRuleChain(),
) : TestRule {

    @Volatile internal var testClassName: String? = null
    @Volatile internal var testMethodName: String? = null
    private val motionTestWatcher =
        object : TestWatcher() {
            override fun starting(description: Description) {
                testClassName = description.testClass.simpleName
                testMethodName = description.methodName
            }

            override fun finished(description: Description?) {
                testClassName = null
                testMethodName = null
            }
        }

    private val rule = extraRules.around(motionTestWatcher)

    override fun apply(base: Statement?, description: Description?): Statement =
        rule.apply(base, description)

    private val scubaExportStrategy = ExportToScubaStrategy(goldenPathManager)

    /** Returns a Truth subject factory to be used with [Truth.assertAbout]. */
    fun motion(): Subject.Factory<RecordedMotionSubject, RecordedMotion> {
        return Subject.Factory { failureMetadata: FailureMetadata, subject: RecordedMotion? ->
            RecordedMotionSubject(failureMetadata, subject, this)
        }
    }

    /** Shortcut for `Truth.assertAbout(motion()).that(recordedMotion)`. */
    fun assertThat(recordedMotion: RecordedMotion): RecordedMotionSubject =
        assertAbout(motion()).that(recordedMotion)

    /**
     * Reads and parses the golden [TimeSeries].
     *
     * Golden data types not included in the `typeRegistry` will produce an [UnknownType].
     *
     * @param typeRegistry [DataPointType] implementations used to de-serialize structured JSON
     *   values to golden values. See [TimeSeries.dataPointTypes] for creating the registry based on
     *   the currently produced timeseries.
     * @throws GoldenNotFoundException if the golden does not exist.
     * @throws JSONException if the golden file fails to parse.
     */
    internal fun readGoldenTimeSeries(
        goldenIdentifier: String,
        typeRegistry: Map<String, DataPointType<*>>,
    ): TimeSeries {
        val path = goldenPathManager.goldenIdentifierResolver(goldenIdentifier, JSON_EXTENSION)
        try {
            return goldenPathManager.appContext.assets.open(path).bufferedReader().use {
                val jsonObject = JSONObject(it.readText())
                JsonGoldenSerializer.fromJson(jsonObject, typeRegistry)
            }
        } catch (e: FileNotFoundException) {
            throw GoldenNotFoundException(path)
        }
    }

    /** Writes generated, actual golden JSON data to the device, to be picked up by TF. */
    internal fun writeGeneratedTimeSeries(
        goldenIdentifier: String,
        recordedMotion: RecordedMotion,
        result: TimeSeriesVerificationResult,
    ) {
        requireValidGoldenIdentifier(goldenIdentifier)

        val relativeGoldenPath =
            goldenPathManager.goldenIdentifierResolver(goldenIdentifier, JSON_ACTUAL_EXTENSION)
        val deviceLocalPath = File(goldenPathManager.deviceLocalPath)
        val goldenFile =
            deviceLocalPath.resolve(recordedMotion.testClassName).resolve(relativeGoldenPath)

        val goldenFileDirectory = checkNotNull(goldenFile.parentFile)
        if (!goldenFileDirectory.exists()) {
            goldenFileDirectory.mkdirs()
        }

        val metadata = JSONObject()
        metadata.put(
            "goldenRepoPath",
            "${goldenPathManager.assetsPathRelativeToBuildRoot}/${relativeGoldenPath.replace(
                JSON_ACTUAL_EXTENSION, JSON_EXTENSION,)}",
        )
        metadata.put("goldenIdentifier", goldenIdentifier)
        metadata.put("testClassName", recordedMotion.testClassName)
        metadata.put("testMethodName", recordedMotion.testMethodName)
        metadata.put("deviceLocalPath", deviceLocalPath)
        metadata.put("result", result.name)

        recordedMotion.videoRenderer?.let { videoRenderer ->
            try {
                val videoFile =
                    goldenFile.resolveSibling("${goldenFile.nameWithoutExtension}.$VIDEO_EXTENSION")

                videoRenderer.renderToFile(videoFile.absolutePath)
                metadata.put("videoLocation", videoFile.relativeTo(deviceLocalPath))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to render motion test video", e)
            }
        }

        try {
            FileOutputStream(goldenFile).bufferedWriter().use {
                val jsonObject = JsonGoldenSerializer.toJson(recordedMotion.timeSeries)
                jsonObject.put("//metadata", metadata)
                it.write(jsonObject.toString(JSON_INDENTATION))
            }
        } catch (e: Exception) {
            throw IOException("Failed to write generated JSON (${goldenFile.absolutePath}). ", e)
        }
    }

    private fun requireValidGoldenIdentifier(goldenIdentifier: String) {
        require(goldenIdentifier.matches(GOLDEN_IDENTIFIER_REGEX)) {
            "Golden identifier '$goldenIdentifier' does not satisfy the naming " +
                "requirement. Allowed characters are: '[A-Za-z0-9_-]'"
        }
    }

    companion object {
        private const val JSON_EXTENSION = "json"
        private const val JSON_ACTUAL_EXTENSION = "actual.${JSON_EXTENSION}"
        private const val VIDEO_EXTENSION = "mp4"
        private const val JSON_INDENTATION = 2
        private val GOLDEN_IDENTIFIER_REGEX = "^[A-Za-z0-9_-]+$".toRegex()
        private const val TAG = "MotionTestRule"
    }
}

/**
 * Time-series golden verification result.
 *
 * Note that downstream golden-update tooling relies on the exact naming of these enum values.
 */
internal enum class TimeSeriesVerificationResult {
    PASSED,
    FAILED,
    MISSING_REFERENCE,
}

class GoldenNotFoundException(val missingGoldenFile: String) : Exception()
