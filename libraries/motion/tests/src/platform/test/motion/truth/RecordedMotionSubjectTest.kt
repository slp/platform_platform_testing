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

package platform.test.motion.truth

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.ExpectFailure
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.truth.TruthFailureSubject
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.GoldenNotFoundException
import platform.test.motion.MotionTestRule
import platform.test.motion.RecordedMotion
import platform.test.motion.TimeSeriesVerificationResult
import platform.test.motion.golden.DataPointType
import platform.test.motion.golden.TimeSeries
import platform.test.motion.golden.TimestampFrameId
import platform.test.screenshot.BitmapDiffer
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.PathConfig
import platform.test.screenshot.matchers.BitmapMatcher

@RunWith(AndroidJUnit4::class)
class RecordedMotionSubjectTest {

    @Test
    fun timeSeriesMatchesGolden_goldenNotFound() {
        val (rule, recordedMotion) =
            fakeRecordedMotion(
                actual = TimeSeries(listOf(TimestampFrameId(0L)), emptyList()),
                golden = null
            )

        with(
            assertThrows {
                assertAbout(rule.motion()).that(recordedMotion).timeSeriesMatchesGolden("foo")
            }
        ) {
            factKeys().contains("Golden [foo] not found")
        }

        assertWithMessage("exports actual data")
            .that(rule.writeGeneratedTimeInvocations)
            .containsExactly(
                Triple("foo", recordedMotion, TimeSeriesVerificationResult.MISSING_REFERENCE)
            )

        assertWithMessage("exports debug filmstrip as failed")
            .that(rule.writeDebugFilmstripInvocations)
            .containsExactly("foo" to false)
    }

    @Test
    fun timeSeriesMatchesGolden_matchesGolden() {
        val (rule, recordedMotion) =
            fakeRecordedMotion(
                actual = TimeSeries(listOf(TimestampFrameId(0L)), emptyList()),
                golden = TimeSeries(listOf(TimestampFrameId(0L)), emptyList())
            )

        assertAbout(rule.motion()).that(recordedMotion).timeSeriesMatchesGolden("foo")

        assertWithMessage("exports actual data")
            .that(rule.writeGeneratedTimeInvocations)
            .containsExactly(Triple("foo", recordedMotion, TimeSeriesVerificationResult.PASSED))

        assertWithMessage("exports debug filmstrip as failed")
            .that(rule.writeDebugFilmstripInvocations)
            .containsExactly("foo" to true)
    }

    @Test
    fun timeSeriesMatchesGolden_doesNotMatchGolden() {
        val (rule, recordedMotion) =
            fakeRecordedMotion(
                actual = TimeSeries(listOf(TimestampFrameId(1L)), emptyList()),
                golden = TimeSeries(listOf(TimestampFrameId(0L)), emptyList())
            )

        with(
            assertThrows {
                assertAbout(rule.motion()).that(recordedMotion).timeSeriesMatchesGolden("foo")
            }
        ) {
            factValue("|  expected").isEqualTo("[0ms]")
            factValue("|  but got").isEqualTo("[1ms]")
        }

        assertWithMessage("exports actual data")
            .that(rule.writeGeneratedTimeInvocations)
            .containsExactly(Triple("foo", recordedMotion, TimeSeriesVerificationResult.FAILED))

        assertWithMessage("exports debug filmstrip as failed")
            .that(rule.writeDebugFilmstripInvocations)
            .containsExactly("foo" to false)
    }

    @Test
    fun filmstripMatchesGolden_matchesGolden() {
        val (rule, recordedMotion) =
            fakeRecordedMotion(
                actual = TimeSeries(listOf(TimestampFrameId(0L)), emptyList()),
                golden = null,
                matchesBitmap = true
            )

        assertAbout(rule.motion()).that(recordedMotion).filmstripMatchesGolden("foo")

        assertThat(rule.readGoldenTimeSeriesInvocations).isEmpty()
        assertThat(rule.writeGeneratedTimeInvocations).isEmpty()
        assertThat(rule.writeDebugFilmstripInvocations).isEmpty()
    }

    @Test
    fun filmstripMatchesGolden_doesNotMatchGolden() {
        val (rule, recordedMotion) =
            fakeRecordedMotion(
                actual = TimeSeries(listOf(TimestampFrameId(0L)), emptyList()),
                golden = null,
                matchesBitmap = false
            )

        with(
            assertThrows {
                assertAbout(rule.motion()).that(recordedMotion).filmstripMatchesGolden("foo")
            }
        ) {
            factKeys().contains("expected to be true")
        }
        assertThat(rule.readGoldenTimeSeriesInvocations).isEmpty()
        assertThat(rule.writeGeneratedTimeInvocations).isEmpty()
        assertThat(rule.writeDebugFilmstripInvocations).isEmpty()
    }

    private inline fun assertThrows(body: () -> Unit): TruthFailureSubject {
        try {
            body()
        } catch (e: Throwable) {
            if (e is AssertionError) {
                return ExpectFailure.assertThat(e)
            }
            throw e
        }
        throw AssertionError("Body completed successfully. Expected AssertionError")
    }

    private fun fakeRecordedMotion(
        actual: TimeSeries,
        golden: TimeSeries?,
        matchesBitmap: Boolean = true
    ): Pair<FakeMotionTestRule, RecordedMotion> {
        val screenshots =
            List(actual.frameIds.size) { index ->
                Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).also {
                    Canvas(it).drawColor(if (index % 2 == 0) Color.RED else Color.GREEN)
                }
            }

        val recordedMotion =
            RecordedMotion(
                testClassName = "MotionTest",
                testMethodName = "foo_test",
                actual,
                screenshots
            )

        val fakeBitmapDiffer =
            object : BitmapDiffer {
                override fun assertBitmapAgainstGolden(
                    actual: Bitmap,
                    goldenIdentifier: String,
                    matcher: BitmapMatcher,
                    regions: List<Rect>
                ) {
                    assertWithMessage("fake bitmap differ").that(matchesBitmap).isTrue()
                }
            }

        val fakeRule = FakeMotionTestRule(golden, fakeBitmapDiffer)
        return fakeRule to recordedMotion
    }

    private class FakeMotionTestRule(private val golden: TimeSeries?, bitmapDiffer: BitmapDiffer?) :
        MotionTestRule(
            goldenPathManager =
                GoldenPathManager(
                    InstrumentationRegistry.getInstrumentation().context,
                    pathConfig = PathConfig()
                ),
            bitmapDiffer = bitmapDiffer
        ) {
        var readGoldenTimeSeriesInvocations = mutableListOf<String>()
        var writeGeneratedTimeInvocations =
            mutableListOf<Triple<String, RecordedMotion, TimeSeriesVerificationResult>>()
        var writeDebugFilmstripInvocations = mutableListOf<Pair<String, Boolean>>()

        override fun readGoldenTimeSeries(
            goldenIdentifier: String,
            typeRegistry: Map<String, DataPointType<*>>
        ): TimeSeries {
            readGoldenTimeSeriesInvocations.add(goldenIdentifier)
            return golden ?: throw GoldenNotFoundException(goldenIdentifier)
        }

        override fun writeGeneratedTimeSeries(
            goldenIdentifier: String,
            recordedMotion: RecordedMotion,
            result: TimeSeriesVerificationResult
        ) {
            writeGeneratedTimeInvocations.add(Triple(goldenIdentifier, recordedMotion, result))
        }

        override fun writeDebugFilmstrip(
            recordedMotion: RecordedMotion,
            goldenIdentifier: String,
            matches: Boolean
        ) {
            writeDebugFilmstripInvocations.add(goldenIdentifier to matches)
        }
    }
}
