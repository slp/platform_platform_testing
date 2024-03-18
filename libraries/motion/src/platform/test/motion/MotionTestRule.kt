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

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.Volatile
import org.json.JSONObject
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import platform.test.motion.golden.DataPointType
import platform.test.motion.golden.DataPointTypes
import platform.test.motion.golden.JsonGoldenSerializer
import platform.test.motion.golden.TimeSeries
import platform.test.motion.truth.RecordedMotionSubject
import platform.test.screenshot.BitmapDiffer
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.proto.ScreenshotResultProto
import platform.test.screenshot.report.ExportToScubaStrategy

/**
 * Test rule to verify correctness of animations.
 *
 * Capture a time-series of values, at specified intervals, during an animation. Additionally, a
 * screenshot is captured along each data frame, to simplify verification of the test setup as well
 * as debugging.
 *
 * To capture the animation, use the environment-specific extension functions.
 */
open class MotionTestRule(
    private val goldenPathManager: GoldenPathManager,
    goldenDataPointTypes: List<DataPointType<*>> = DataPointTypes.allTypes,
    internal val bitmapDiffer: BitmapDiffer? = null,
) : TestWatcher() {
    private val goldenSerializer = JsonGoldenSerializer(goldenDataPointTypes)
    private val scubaExportStrategy = ExportToScubaStrategy(goldenPathManager)

    @Volatile protected var testClassName: String? = null
    @Volatile protected var testMethodName: String? = null

    /** Returns a Truth subject factory to be used with [Truth.assertAbout]. */
    fun motion(): Subject.Factory<RecordedMotionSubject, RecordedMotion> {
        return Subject.Factory { failureMetadata: FailureMetadata, subject: RecordedMotion? ->
            RecordedMotionSubject(failureMetadata, subject, this)
        }
    }

    /** Shortcut for `Truth.assertAbout(motion()).that(recordedMotion)`. */
    fun assertThat(recordedMotion: RecordedMotion): RecordedMotionSubject =
        assertAbout(motion()).that(recordedMotion)

    override fun starting(description: Description) {
        testClassName = description.className
        testMethodName = description.methodName
    }

    override fun finished(description: Description?) {
        testClassName = null
        testMethodName = null
    }

    /**
     * Reads and parses the golden [TimeSeries].
     *
     * This method is only `open` to be overridden for tests.
     *
     * @throws GoldenNotFoundException if the golden does not exist.
     * @throws JSONException if the golden file fails to parse.
     */
    internal open fun readGoldenTimeSeries(goldenIdentifier: String): TimeSeries {
        val path = goldenPathManager.goldenIdentifierResolver(goldenIdentifier, JSON_EXTENSION)
        val instrument = InstrumentationRegistry.getInstrumentation()
        return listOf(instrument.targetContext.applicationContext, instrument.context)
            .firstNotNullOfOrNull { context ->
                try {
                    context.assets.open(path).bufferedReader().use {
                        val jsonObject = JSONObject(it.readText())
                        goldenSerializer.fromJson(jsonObject)
                    }
                } catch (e: FileNotFoundException) {
                    null
                }
            }
            ?: throw GoldenNotFoundException(path)
    }

    /**
     * Writes generated, actual golden JSON data to the device, to be picked up by TF.
     *
     * This method is only `open` to be overridden for tests.
     */
    internal open fun writeGeneratedTimeSeries(goldenIdentifier: String, timeSeries: TimeSeries) {
        requireValidGoldenIdentifier(goldenIdentifier)

        val relativeGoldenPath =
            goldenPathManager.goldenIdentifierResolver(goldenIdentifier, JSON_EXTENSION)
        val goldenFile = File(goldenPathManager.deviceLocalPath).resolve(relativeGoldenPath)

        val goldenFileDirectory = checkNotNull(goldenFile.parentFile)
        if (!goldenFileDirectory.exists()) {
            goldenFileDirectory.mkdirs()
        }
        try {
            FileOutputStream(goldenFile).bufferedWriter().use {
                val jsonObject = goldenSerializer.toJson(timeSeries)
                it.write(jsonObject.toString(JSON_INDENTATION))
            }
        } catch (e: Exception) {
            throw IOException("Failed to write generated JSON (${goldenFile.absolutePath}). ", e)
        }
    }

    internal open fun writeDebugFilmstrip(
        recordedMotion: RecordedMotion,
        goldenIdentifier: String,
        matches: Boolean
    ) {
        if (recordedMotion.filmstrip == null) {
            return
        }
        requireValidGoldenIdentifier(goldenIdentifier)
        val filmstrip = recordedMotion.filmstrip.renderFilmstrip()
        val testIdentifier = "${recordedMotion.testClassName}_${recordedMotion.testMethodName}"
        scubaExportStrategy.reportResult(
            testIdentifier,
            goldenIdentifier,
            actual = filmstrip,
            status =
                if (matches) ScreenshotResultProto.DiffResult.Status.PASSED
                else ScreenshotResultProto.DiffResult.Status.FAILED,
            comparisonStatistics =
                ScreenshotResultProto.DiffResult.ComparisonStatistics.newBuilder()
                    .setNumberPixelsCompared(filmstrip.width * filmstrip.height)
                    .setNumberPixelsIgnored(filmstrip.width * filmstrip.height)
                    .build()
        )
    }

    private fun requireValidGoldenIdentifier(goldenIdentifier: String) {
        require(goldenIdentifier.matches(GOLDEN_IDENTIFIER_REGEX)) {
            "Golden identifier '$goldenIdentifier' does not satisfy the naming " +
                "requirement. Allowed characters are: '[A-Za-z0-9_-]'"
        }
    }

    companion object {
        private const val JSON_EXTENSION = "json"
        private const val JSON_INDENTATION = 2
        private val GOLDEN_IDENTIFIER_REGEX = "^[A-Za-z0-9_-]+$".toRegex()
    }
}

class GoldenNotFoundException(val missingGoldenFile: String) : Exception()
