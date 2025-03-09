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

package platform.test.motion.filmstrip

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import platform.test.motion.golden.SupplementalFrameId
import platform.test.motion.golden.TimestampFrameId
import platform.test.motion.testing.createGoldenPathManager
import platform.test.screenshot.ScreenshotAsserterConfig
import platform.test.screenshot.ScreenshotTestRule

@RunWith(AndroidJUnit4::class)
class FilmstripTest {

    private val goldenPathManager =
        createGoldenPathManager("platform_testing/libraries/motion/tests/assets")

    @get:Rule val screenshotTestRule = ScreenshotTestRule(goldenPathManager)
    @get:Rule val testName = TestName()

    private fun assertFilmstripRendering(filmstrip: Filmstrip) {
        screenshotTestRule
            .createScreenshotAsserter(
                ScreenshotAsserterConfig(captureStrategy = { filmstrip.renderFilmstrip() })
            )
            .assertGoldenImage(testName.methodName)
    }

    @Test
    fun horizontalSingleFrame() {
        assertFilmstripRendering(
            Filmstrip(listOf(MotionScreenshot(TimestampFrameId(10), mockScreenshot(Color.RED))))
                .apply { orientation = FilmstripOrientation.HORIZONTAL }
        )
    }

    @Test
    fun verticalSingleFrame() {
        assertFilmstripRendering(
            Filmstrip(listOf(MotionScreenshot(TimestampFrameId(10), mockScreenshot(Color.RED))))
                .apply { orientation = FilmstripOrientation.VERTICAL }
        )
    }

    @Test
    fun horizontalFilmstrip() {
        val w = 100
        val h = 200

        val screenshots =
            listOf(
                MotionScreenshot(TimestampFrameId(10), mockScreenshot(Color.RED, w, h)),
                MotionScreenshot(TimestampFrameId(20), mockScreenshot(Color.GREEN, w, h)),
                MotionScreenshot(SupplementalFrameId("after"), mockScreenshot(Color.BLUE, w, h)),
            )

        assertFilmstripRendering(
            Filmstrip(screenshots).apply { orientation = FilmstripOrientation.HORIZONTAL }
        )
    }

    @Test
    fun horizontalFilmstrip_labelsWiderThanScreenshot() {
        val w = 100
        val h = 20

        val screenshots =
            listOf(
                MotionScreenshot(
                    SupplementalFrameId("wide_before"),
                    mockScreenshot(Color.RED, w, h),
                ),
                MotionScreenshot(
                    SupplementalFrameId("wide_after"),
                    mockScreenshot(Color.BLUE, w, h),
                ),
            )

        assertFilmstripRendering(
            Filmstrip(screenshots).apply { orientation = FilmstripOrientation.HORIZONTAL }
        )
    }

    @Test
    fun horizontalFilmstrip_variableSize_tileMatchesLargestDimensions() {
        val screenshots =
            listOf(
                MotionScreenshot(TimestampFrameId(1), mockScreenshot(Color.RED, 100, 200)),
                MotionScreenshot(TimestampFrameId(2), mockScreenshot(Color.GREEN, 150, 75)),
                MotionScreenshot(TimestampFrameId(3), mockScreenshot(Color.BLUE, 50, 50)),
            )

        assertFilmstripRendering(
            Filmstrip(screenshots).apply { orientation = FilmstripOrientation.HORIZONTAL }
        )
    }

    @Test
    fun verticalFilmstrip() {
        val w = 200
        val h = 100

        val screenshots =
            listOf(
                MotionScreenshot(TimestampFrameId(10), mockScreenshot(Color.RED, w, h)),
                MotionScreenshot(TimestampFrameId(20), mockScreenshot(Color.GREEN, w, h)),
                MotionScreenshot(SupplementalFrameId("after"), mockScreenshot(Color.BLUE, w, h)),
            )

        assertFilmstripRendering(
            Filmstrip(screenshots).apply { orientation = FilmstripOrientation.VERTICAL }
        )
    }

    @Test
    fun verticalFilmstrip_labelsTallerThanScreenshot() {
        val w = 100
        val h = 5

        val screenshots =
            listOf(
                MotionScreenshot(SupplementalFrameId("before"), mockScreenshot(Color.RED, w, h)),
                MotionScreenshot(SupplementalFrameId("after"), mockScreenshot(Color.BLUE, w, h)),
            )

        assertFilmstripRendering(
            Filmstrip(screenshots).apply { orientation = FilmstripOrientation.VERTICAL }
        )
    }

    @Test
    fun verticalFilmstrip_variableSize_tileMatchesLargestDimensions() {
        val screenshots =
            listOf(
                MotionScreenshot(TimestampFrameId(1), mockScreenshot(Color.RED, 100, 200)),
                MotionScreenshot(TimestampFrameId(2), mockScreenshot(Color.GREEN, 150, 75)),
                MotionScreenshot(TimestampFrameId(3), mockScreenshot(Color.BLUE, 50, 50)),
            )

        assertFilmstripRendering(
            Filmstrip(screenshots).apply { orientation = FilmstripOrientation.VERTICAL }
        )
    }

    @Test
    fun automaticOrientation_tallScreenshots_filmstripIsHorizontal() {
        val w = 100
        val h = 200

        val screenshots =
            listOf(
                MotionScreenshot(TimestampFrameId(10), mockScreenshot(Color.RED, w, h)),
                MotionScreenshot(TimestampFrameId(20), mockScreenshot(Color.GREEN, w, h)),
                MotionScreenshot(SupplementalFrameId("after"), mockScreenshot(Color.BLUE, w, h)),
            )

        val bitmap =
            Filmstrip(screenshots)
                .apply { orientation = FilmstripOrientation.AUTOMATIC }
                .renderFilmstrip()

        assertThat(bitmap.width).isEqualTo(w * 3)
        assertThat(bitmap.height).isLessThan(h * 3)
    }

    @Test
    fun automaticOrientation_wideScreenshots_filmstripIsVertical() {
        val w = 200
        val h = 100

        val screenshots =
            listOf(
                MotionScreenshot(TimestampFrameId(10), mockScreenshot(Color.RED, w, h)),
                MotionScreenshot(TimestampFrameId(20), mockScreenshot(Color.GREEN, w, h)),
                MotionScreenshot(SupplementalFrameId("after"), mockScreenshot(Color.BLUE, w, h)),
            )

        val bitmap =
            Filmstrip(screenshots)
                .apply { orientation = FilmstripOrientation.AUTOMATIC }
                .renderFilmstrip()

        assertThat(bitmap.width).isLessThan(w * 3)
        assertThat(bitmap.height).isEqualTo(h * 3)
    }

    @Test
    fun limitLongestSide_scalesBasedOnLongerHeight() {
        val screenshots =
            listOf(
                MotionScreenshot(TimestampFrameId(1), mockScreenshot(Color.RED, 100, 200)),
                MotionScreenshot(TimestampFrameId(2), mockScreenshot(Color.GREEN, 150, 75)),
                MotionScreenshot(TimestampFrameId(3), mockScreenshot(Color.BLUE, 50, 50)),
            )

        assertFilmstripRendering(
            Filmstrip(screenshots).apply {
                limitLongestSide(100)
                orientation = FilmstripOrientation.HORIZONTAL
            }
        )
    }

    @Test
    fun limitLongestSide_scalesBasedOnLongerWidth() {
        val screenshots =
            listOf(
                MotionScreenshot(TimestampFrameId(1), mockScreenshot(Color.RED, 200, 100)),
                MotionScreenshot(TimestampFrameId(2), mockScreenshot(Color.GREEN, 150, 75)),
                MotionScreenshot(TimestampFrameId(3), mockScreenshot(Color.BLUE, 50, 50)),
            )

        assertFilmstripRendering(
            Filmstrip(screenshots).apply {
                limitLongestSide(100)
                orientation = FilmstripOrientation.HORIZONTAL
            }
        )
    }

    @Test
    fun limitLongestSide_doesNotScaleUp() {
        val screenshots =
            listOf(
                MotionScreenshot(TimestampFrameId(1), mockScreenshot(Color.RED, 200, 100)),
                MotionScreenshot(TimestampFrameId(2), mockScreenshot(Color.GREEN, 150, 75)),
                MotionScreenshot(TimestampFrameId(3), mockScreenshot(Color.BLUE, 50, 50)),
            )

        assertFilmstripRendering(
            Filmstrip(screenshots).apply {
                limitLongestSide(300)
                orientation = FilmstripOrientation.HORIZONTAL
            }
        )
    }

    private fun mockScreenshot(
        color: Int,
        width: Int = 400,
        height: Int = 200,
        bitmapConfig: Bitmap.Config = Bitmap.Config.ARGB_8888,
    ) = Bitmap.createBitmap(width, height, bitmapConfig).also { Canvas(it).drawColor(color) }
}
