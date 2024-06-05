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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.ExpectFailure
import com.google.common.truth.TruthFailureSubject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import platform.test.motion.GoldenNotFoundException
import platform.test.motion.MotionTestRule
import platform.test.motion.RecordedMotion
import platform.test.motion.TimeSeriesVerificationResult
import platform.test.motion.golden.TimeSeries
import platform.test.motion.golden.TimestampFrameId
import platform.test.screenshot.BitmapDiffer
import platform.test.screenshot.GoldenPathManager
import platform.test.screenshot.PathConfig

@RunWith(AndroidJUnit4::class)
class RecordedMotionSubjectTest {

    private val goldenPathManager =
        GoldenPathManager(
            InstrumentationRegistry.getInstrumentation().context,
            pathConfig = PathConfig()
        )
    private val bitmapDiffer: BitmapDiffer = mock()
    private val motionRule =
        spy(MotionTestRule(Unit, goldenPathManager, bitmapDiffer)) {
            doNothing().whenever(it).writeGeneratedTimeSeries(any(), any(), any())
        }

    @Test
    fun timeSeriesMatchesGolden_goldenNotFound() {
        val recordedMotion = fakeRecordedMotion()

        doAnswer { throw GoldenNotFoundException(it.getArgument(0)) }
            .whenever(motionRule)
            .readGoldenTimeSeries(any(), any())

        with(
            assertThrows { motionRule.assertThat(recordedMotion).timeSeriesMatchesGolden("foo") }
        ) {
            factKeys().contains("Golden [foo] not found")
        }

        verify(motionRule)
            .writeGeneratedTimeSeries(
                "foo",
                recordedMotion,
                TimeSeriesVerificationResult.MISSING_REFERENCE
            )
    }

    @Test
    fun timeSeriesMatchesGolden_matchesGolden() {
        val recordedMotion = fakeRecordedMotion()

        doReturn(recordedMotion.timeSeries)
            .whenever(motionRule)
            .readGoldenTimeSeries(anyString(), any())

        motionRule.assertThat(recordedMotion).timeSeriesMatchesGolden("foo")

        verify(motionRule)
            .writeGeneratedTimeSeries("foo", recordedMotion, TimeSeriesVerificationResult.PASSED)
    }

    @Test
    fun timeSeriesMatchesGolden_doesNotMatchGolden() {
        val recordedMotion = fakeRecordedMotion()
        doReturn(TimeSeries(listOf(TimestampFrameId(1L)), emptyList()))
            .whenever(motionRule)
            .readGoldenTimeSeries(any(), any())

        with(
            assertThrows { motionRule.assertThat(recordedMotion).timeSeriesMatchesGolden("foo") }
        ) {
            factValue("|  expected").isEqualTo("[1ms]")
            factValue("|  but got").isEqualTo("[0ms]")
        }

        verify(motionRule)
            .writeGeneratedTimeSeries("foo", recordedMotion, TimeSeriesVerificationResult.FAILED)
    }

    @Test
    fun filmstripMatchesGolden_matchesGolden() {
        val recordedMotion = fakeRecordedMotion()

        motionRule.assertThat(recordedMotion).filmstripMatchesGolden("foo")

        verify(motionRule, never()).readGoldenTimeSeries(any(), any())
        verify(motionRule, never()).writeGeneratedTimeSeries(any(), any(), any())
    }

    @Test
    fun filmstripMatchesGolden_doesNotMatchGolden() {
        val recordedMotion = fakeRecordedMotion()

        whenever(bitmapDiffer.assertBitmapAgainstGolden(any(), any(), any(), any()))
            .thenThrow(AssertionError("Image mismatch!"))

        Assert.assertThrows(AssertionError::class.java) {
            motionRule.assertThat(recordedMotion).filmstripMatchesGolden("foo")
        }

        verify(motionRule, never()).readGoldenTimeSeries(any(), any())
        verify(motionRule, never()).writeGeneratedTimeSeries(any(), any(), any())
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
        actual: TimeSeries = TimeSeries(listOf(TimestampFrameId(0L)), emptyList())
    ): RecordedMotion {
        val screenshots =
            List(actual.frameIds.size) { index ->
                Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).also {
                    Canvas(it).drawColor(if (index % 2 == 0) Color.RED else Color.GREEN)
                }
            }
        return RecordedMotion(
            testClassName = "MotionTest",
            testMethodName = "foo_test",
            actual,
            screenshots
        )
    }
}
