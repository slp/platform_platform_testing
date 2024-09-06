/*
 * Copyright 2022 The Android Open Source Project
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

package platform.test.screenshot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.platform.uiautomator_helpers.DeviceHelpers.shell
import android.provider.Settings.System
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.lang.AssertionError
import java.util.ArrayList
import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.screenshot.matchers.MSSIMMatcher
import platform.test.screenshot.matchers.PixelPerfectMatcher
import platform.test.screenshot.proto.ScreenshotResultProto.DiffResult
import platform.test.screenshot.report.DiffResultExportStrategy
import platform.test.screenshot.utils.loadBitmap

class CustomGoldenPathManager(appcontext: Context, assetsPath: String = "assets") :
    GoldenPathManager(appcontext, assetsPath) {
    override fun goldenIdentifierResolver(testName: String, extension: String): String =
        "$testName.$extension"
}

@RunWith(AndroidJUnit4::class)
@MediumTest
class ScreenshotTestRuleTest {

    private val fakeDiffEscrow = FakeDiffResultExport()

    @get:Rule
    val rule =
        ScreenshotTestRule(
            CustomGoldenPathManager(InstrumentationRegistry.getInstrumentation().context),
            diffEscrowStrategy = fakeDiffEscrow
        )

    @Test
    fun performDiff_sameBitmaps() {
        val goldenIdentifier = "round_rect_gray"
        val first = loadBitmap(goldenIdentifier)

        first.assertAgainstGolden(rule, goldenIdentifier, matcher = PixelPerfectMatcher())

        assertThat(fakeDiffEscrow.reports).isEmpty()
    }

    @Test
    fun performDiff_sameBitmaps_materialYouColors() {
        val goldenIdentifier = "defaultClock_largeClock_regionSampledColor"
        val first =
            bitmapWithMaterialYouColorsSimulation(
                loadBitmap("defaultClock_largeClock_regionSampledColor_original"),
                /* isDarkTheme= */ true
            )

        first.assertAgainstGolden(rule, goldenIdentifier, matcher = PixelPerfectMatcher())
        assertThat(fakeDiffEscrow.reports).isEmpty()
    }

    @Test
    fun performDiff_noPixelCompared() {
        val first = loadBitmap("round_rect_gray")
        val regions = ArrayList<Rect>()
        regions.add(Rect(/* left= */ 1, /* top= */ 1, /* right= */ 2, /* bottom=*/ 2))

        val goldenIdentifier = "round_rect_green"
        first.assertAgainstGolden(
            rule,
            goldenIdentifier,
            matcher = MSSIMMatcher(),
            regions = regions
        )

        assertThat(fakeDiffEscrow.reports).isEmpty()
    }

    @Test
    fun performDiff_sameRegion() {
        val first = loadBitmap("qmc-folder1")
        val startHeight = 18 * first.height / 20
        val endHeight = 37 * first.height / 40
        val startWidth = 10 * first.width / 20
        val endWidth = 11 * first.width / 20
        val matcher = MSSIMMatcher()
        val regions = ArrayList<Rect>()
        regions.add(Rect(startWidth, startHeight, endWidth, endHeight))
        regions.add(Rect())

        val goldenIdentifier = "qmc-folder2"
        first.assertAgainstGolden(rule, goldenIdentifier, matcher, regions)

        assertThat(fakeDiffEscrow.reports).isEmpty()
    }

    @Test
    fun performDiff_sameSizes_default_noMatch() {
        val imageExtension = ".png"
        val first = loadBitmap("round_rect_gray")
        val compStatistics =
            DiffResult.ComparisonStatistics.newBuilder()
                .setNumberPixelsCompared(1504)
                .setNumberPixelsDifferent(74)
                .setNumberPixelsIgnored(800)
                .setNumberPixelsSimilar(1430)
                .build()

        val goldenIdentifier = "round_rect_green"
        expectErrorMessage("Image mismatch! Comparison stats: '$compStatistics'") {
            first.assertAgainstGolden(rule, goldenIdentifier)
        }

        assertThat(fakeDiffEscrow.reports)
            .containsExactly(
                ReportedDiffResult(
                    goldenIdentifier,
                    DiffResult.Status.FAILED,
                    hasExpected = true,
                    hasDiff = true,
                    comparisonStatistics = compStatistics
                )
            )
    }

    @Test
    fun performDiff_sameSizes_pixelPerfect_noMatch() {
        val first = loadBitmap("round_rect_gray")
        val compStatistics =
            DiffResult.ComparisonStatistics.newBuilder()
                .setNumberPixelsCompared(2304)
                .setNumberPixelsDifferent(556)
                .setNumberPixelsIdentical(1748)
                .build()

        val goldenIdentifier = "round_rect_green"
        expectErrorMessage("Image mismatch! Comparison stats: '$compStatistics'") {
            first.assertAgainstGolden(rule, goldenIdentifier, matcher = PixelPerfectMatcher())
        }

        assertThat(fakeDiffEscrow.reports)
            .containsExactly(
                ReportedDiffResult(
                    goldenIdentifier,
                    DiffResult.Status.FAILED,
                    hasExpected = true,
                    hasDiff = true,
                    comparisonStatistics = compStatistics
                )
            )
    }

    @Test
    fun performDiff_differentSizes() {
        val first = loadBitmap("fullscreen_rect_gray")
        val goldenIdentifier = "round_rect_gray"
        val compStatistics =
            DiffResult.ComparisonStatistics.newBuilder()
                .setNumberPixelsCompared(2304)
                .setNumberPixelsDifferent(568)
                .setNumberPixelsIdentical(1736)
                .build()

        expectErrorMessage(
            "Sizes are different! Expected: [48, 48], Actual: [720, 1184]. Force aligned "
                + "at (0, 0). Comparison stats: '${compStatistics}'") {
            first.assertAgainstGolden(rule, goldenIdentifier)
        }

        assertThat(fakeDiffEscrow.reports)
            .containsExactly(
                ReportedDiffResult(
                    goldenIdentifier,
                    DiffResult.Status.FAILED,
                    hasExpected = true,
                    hasDiff = true,
                    comparisonStatistics = compStatistics
                )
            )
    }

    @Test(expected = IllegalArgumentException::class)
    fun performDiff_incorrectGoldenName() {
        val first = loadBitmap("fullscreen_rect_gray")

        first.assertAgainstGolden(rule, "round_rect_gray #")
    }

    @Test
    fun performDiff_missingGolden() {
        val first = loadBitmap("round_rect_gray")

        val goldenIdentifier = "does_not_exist"

        expectErrorMessage(
            "Missing golden image 'does_not_exist.png'. Did you mean to check in a new image?"
        ) {
            first.assertAgainstGolden(rule, goldenIdentifier)
        }

        assertThat(fakeDiffEscrow.reports)
            .containsExactly(
                ReportedDiffResult(
                    goldenIdentifier,
                    DiffResult.Status.MISSING_REFERENCE,
                    hasExpected = false,
                    hasDiff = false,
                    comparisonStatistics = null
                ),
            )
    }

    @Test
    fun screenshotAsserterHooks_successfulRun() {
        var preRan = false
        var postRan = false
        val bitmap = loadBitmap("round_rect_green")
        val asserter =
            ScreenshotRuleAsserter.Builder(rule)
                .setOnBeforeScreenshot { preRan = true }
                .setOnAfterScreenshot { postRan = true }
                .setScreenshotProvider { bitmap }
                .build()
        asserter.assertGoldenImage("round_rect_green")
        assertThat(preRan).isTrue()
        assertThat(postRan).isTrue()
    }

    @Test
    fun screenshotAsserterHooks_disablesVisibleDebugSettings() {
        // Turn visual debug settings on
        pointerLocationSetting = 1
        showTouchesSetting = 1

        var preRan = false
        val bitmap = loadBitmap("round_rect_green")
        val asserter =
            ScreenshotRuleAsserter.Builder(rule)
                .setOnBeforeScreenshot {
                    preRan = true
                    assertThat(pointerLocationSetting).isEqualTo(0)
                    assertThat(showTouchesSetting).isEqualTo(0)
                }
                .setScreenshotProvider { bitmap }
                .build()
        asserter.assertGoldenImage("round_rect_green")
        assertThat(preRan).isTrue()

        // Clear visual debug settings
        pointerLocationSetting = 0
        showTouchesSetting = 0
    }

    @Test
    fun screenshotAsserterHooks_whenVisibleDebugSettingsOn_revertsSettings() {
        // Turn visual debug settings on
        pointerLocationSetting = 1
        showTouchesSetting = 1

        val bitmap = loadBitmap("round_rect_green")
        val asserter = ScreenshotRuleAsserter.Builder(rule).setScreenshotProvider { bitmap }.build()
        asserter.assertGoldenImage("round_rect_green")
        assertThat(pointerLocationSetting).isEqualTo(1)
        assertThat(showTouchesSetting).isEqualTo(1)

        // Clear visual debug settings to pre-test values
        pointerLocationSetting = 0
        showTouchesSetting = 0
    }

    @Test
    fun screenshotAsserterHooks_whenVisibleDebugSettingsOff_retainsSettings() {
        // Turn visual debug settings off
        pointerLocationSetting = 0
        showTouchesSetting = 0

        val bitmap = loadBitmap("round_rect_green")
        val asserter = ScreenshotRuleAsserter.Builder(rule).setScreenshotProvider { bitmap }.build()
        asserter.assertGoldenImage("round_rect_green")
        assertThat(pointerLocationSetting).isEqualTo(0)
        assertThat(showTouchesSetting).isEqualTo(0)
    }

    @Test
    fun screenshotAsserterHooks_assertionException() {
        var preRan = false
        var postRan = false
        val bitmap = loadBitmap("round_rect_green")
        val asserter =
            ScreenshotRuleAsserter.Builder(rule)
                .setOnBeforeScreenshot { preRan = true }
                .setOnAfterScreenshot { postRan = true }
                .setScreenshotProvider {
                    throw RuntimeException()
                    bitmap
                }
                .build()
        try {
            asserter.assertGoldenImage("round_rect_green")
        } catch (e: RuntimeException) {}
        assertThat(preRan).isTrue()
        assertThat(postRan).isTrue()
    }

    @After
    fun after() {
        // Clear all files we generated so we don't have dependencies between tests
        File(rule.goldenPathManager.deviceLocalPath).deleteRecursively()
    }

    private fun expectErrorMessage(expectedErrorMessage: String, block: () -> Unit) {
        try {
            block()
        } catch (e: AssertionError) {
            val received = e.localizedMessage!!
            assertThat(received).isEqualTo(expectedErrorMessage.trim())
            return
        }

        throw AssertionError("No AssertionError thrown!")
    }

    data class ReportedDiffResult(
        val goldenIdentifier: String,
        val status: DiffResult.Status,
        val hasExpected: Boolean = false,
        val hasDiff: Boolean = false,
        val comparisonStatistics: DiffResult.ComparisonStatistics? = null,
    )

    class FakeDiffResultExport : DiffResultExportStrategy {
        val reports = mutableListOf<ReportedDiffResult>()
        override fun reportResult(
            testIdentifier: String,
            goldenIdentifier: String,
            actual: Bitmap,
            status: DiffResult.Status,
            comparisonStatistics: DiffResult.ComparisonStatistics?,
            expected: Bitmap?,
            diff: Bitmap?
        ) {
            reports.add(
                ReportedDiffResult(
                    goldenIdentifier,
                    status,
                    hasExpected = expected != null,
                    hasDiff = diff != null,
                    comparisonStatistics
                )
            )
        }
    }
    private companion object {
        var prevPointerLocationSetting: Int = 0
        var prevShowTouchesSetting: Int = 0

        private var pointerLocationSetting: Int
            get() =
                shell("settings get system ${System.POINTER_LOCATION}").trim().toIntOrNull() ?: 0
            set(value) {
                shell("settings put system ${System.POINTER_LOCATION} $value")
            }

        private var showTouchesSetting
            get() = shell("settings get system ${System.SHOW_TOUCHES}").trim().toIntOrNull() ?: 0
            set(value) {
                shell("settings put system ${System.SHOW_TOUCHES} $value")
            }

        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            prevPointerLocationSetting = pointerLocationSetting
            prevShowTouchesSetting = showTouchesSetting
        }

        @JvmStatic
        @AfterClass
        fun tearDownClass() {
            pointerLocationSetting = prevPointerLocationSetting
            showTouchesSetting = prevShowTouchesSetting
        }
    }
}
