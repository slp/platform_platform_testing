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

import android.annotation.ColorInt
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.platform.uiautomator_helpers.DeviceHelpers.shell
import android.provider.Settings.System
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.Screenshot
import com.android.internal.app.SimpleIconFactory
import java.io.FileNotFoundException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import platform.test.screenshot.matchers.BitmapMatcher
import platform.test.screenshot.matchers.MSSIMMatcher
import platform.test.screenshot.matchers.MatchResult
import platform.test.screenshot.matchers.PixelPerfectMatcher
import platform.test.screenshot.parity.ParityStatsCollector
import platform.test.screenshot.proto.ScreenshotResultProto
import platform.test.screenshot.report.DiffResultExportStrategy

/**
 * Rule to be added to a test to facilitate screenshot testing.
 *
 * This rule records current test name and when instructed it will perform the given bitmap
 * comparison against the given golden. All the results (including result proto file) are stored
 * into the device to be retrieved later.
 *
 * @see Bitmap.assertAgainstGolden
 */
@SuppressLint("SyntheticAccessor")
open class ScreenshotTestRule
@VisibleForTesting
internal constructor(
    val goldenPathManager: GoldenPathManager,
    /** Strategy to report diffs to external systems. */
    private val diffEscrowStrategy: DiffResultExportStrategy,
    private val disableIconPool: Boolean = true
) : TestRule, BitmapDiffer, ScreenshotAsserterFactory {

    @JvmOverloads
    constructor(
        goldenPathManager: GoldenPathManager,
        disableIconPool: Boolean = true,
    ) : this(
        goldenPathManager,
        DiffResultExportStrategy.createDefaultStrategy(goldenPathManager),
        disableIconPool
    )

    private val doesCollectScreenshotParityStats =
        "yes".equals(
            java.lang.System.getProperty("screenshot.collectScreenshotParityStats"),
            ignoreCase = true)
    private lateinit var testIdentifier: String

    companion object {
        private val parityStatsCollector = ParityStatsCollector()
    }

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    testIdentifier = getTestIdentifier(description)
                    if (disableIconPool) {
                        // Disables pooling of SimpleIconFactory objects as it caches
                        // density, which when updating the screen configuration in runtime
                        // sometimes it does not get updated in the icon renderer.
                        SimpleIconFactory.setPoolEnabled(false)
                    }
                    base.evaluate()
                } finally {
                    if (disableIconPool) {
                        SimpleIconFactory.setPoolEnabled(true)
                    }
                }
            }
        }

    open fun getTestIdentifier(description: Description): String =
        "${description.className}_${description.methodName}"

    private fun fetchExpectedImage(goldenIdentifier: String): Bitmap? {
        val instrument = InstrumentationRegistry.getInstrumentation()
        return listOf(instrument.targetContext.applicationContext, instrument.context)
            .map { context ->
                try {
                    context.assets
                        .open(goldenPathManager.goldenImageIdentifierResolver(goldenIdentifier))
                        .use {
                            return@use BitmapFactory.decodeStream(it)
                        }
                } catch (e: FileNotFoundException) {
                    return@map null
                }
            }
            .filterNotNull()
            .firstOrNull()
    }

    /**
     * Asserts the given bitmap against the golden identified by the given name.
     *
     * Note: The golden identifier should be unique per your test module (unless you want multiple
     * tests to match the same golden). The name must not contain extension. You should also avoid
     * adding strings like "golden", "image" and instead describe what is the golder referring to.
     *
     * @param actual The bitmap captured during the test.
     * @param goldenIdentifier Name of the golden. Allowed characters: 'A-Za-z0-9_-'
     * @param matcher The algorithm to be used to perform the matching.
     * @throws IllegalArgumentException If the golden identifier contains forbidden characters or is
     *   empty.
     * @see MSSIMMatcher
     * @see PixelPerfectMatcher
     * @see Bitmap.assertAgainstGolden
     */
    @Deprecated("use BitmapDiffer or ScreenshotAsserterFactory interfaces")
    fun assertBitmapAgainstGolden(
        actual: Bitmap,
        goldenIdentifier: String,
        matcher: BitmapMatcher
    ) {
        try {
            assertBitmapAgainstGolden(
                actual = actual,
                goldenIdentifier = goldenIdentifier,
                matcher = matcher,
                regions = emptyList<Rect>()
            )
        } finally {
            actual.recycle()
        }
    }

    /**
     * Asserts the given bitmap against the golden identified by the given name.
     *
     * Note: The golden identifier should be unique per your test module (unless you want multiple
     * tests to match the same golden). The name must not contain extension. You should also avoid
     * adding strings like "golden", "image" and instead describe what is the golder referring to.
     *
     * @param actual The bitmap captured during the test.
     * @param goldenIdentifier Name of the golden. Allowed characters: 'A-Za-z0-9_-'
     * @param matcher The algorithm to be used to perform the matching.
     * @param regions An optional array of interesting regions for partial screenshot diff.
     * @throws IllegalArgumentException If the golden identifier contains forbidden characters or is
     *   empty.
     * @see MSSIMMatcher
     * @see PixelPerfectMatcher
     * @see Bitmap.assertAgainstGolden
     */
    @Deprecated("use BitmapDiffer or ScreenshotAsserterFactory interfaces")
    override fun assertBitmapAgainstGolden(
        actual: Bitmap,
        goldenIdentifier: String,
        matcher: BitmapMatcher,
        regions: List<Rect>
    ) {
        if (!goldenIdentifier.matches("^[A-Za-z0-9_-]+$".toRegex())) {
            throw IllegalArgumentException(
                "The given golden identifier '$goldenIdentifier' does not satisfy the naming " +
                    "requirement. Allowed characters are: '[A-Za-z0-9_-]'"
            )
        }

        val expected = fetchExpectedImage(goldenIdentifier)
        if (expected == null) {
            diffEscrowStrategy.reportResult(
                testIdentifier = testIdentifier,
                goldenIdentifier = goldenIdentifier,
                status = ScreenshotResultProto.DiffResult.Status.MISSING_REFERENCE,
                actual = actual
            )
            throw AssertionError(
                "Missing golden image " +
                    "'${goldenPathManager.goldenImageIdentifierResolver(goldenIdentifier)}'. " +
                    "Did you mean to check in a new image?"
            )
        }

        if (expected.sameAs(actual)) {
            if (doesCollectScreenshotParityStats) {
                val stats =
                    ScreenshotResultProto.DiffResult.ComparisonStatistics.newBuilder()
                        .setNumberPixelsCompared(actual.width * actual.height)
                        .setNumberPixelsIdentical(actual.width * actual.height)
                        .setNumberPixelsDifferent(0)
                        .setNumberPixelsIgnored(0)
                        .build()
                parityStatsCollector.collectTestStats(
                    testIdentifier,
                    MatchResult(matches = true, diff = null, comparisonStatistics = stats))
                parityStatsCollector.report()
            }
            expected.recycle()
            return
        }

        if (actual.width != expected.width || actual.height != expected.height) {
            val comparisonResult =
                matcher.compareBitmaps(
                    expected = expected.toIntArray(),
                    given = actual.toIntArray(),
                    expectedWidth = expected.width,
                    expectedHeight = expected.height,
                    actualWidth = actual.width,
                    actualHeight = actual.height
                )
            diffEscrowStrategy.reportResult(
                testIdentifier = testIdentifier,
                goldenIdentifier = goldenIdentifier,
                status = ScreenshotResultProto.DiffResult.Status.FAILED,
                actual = actual,
                comparisonStatistics = comparisonResult.comparisonStatistics,
                expected = expected,
                diff = comparisonResult.diff
            )
            if (doesCollectScreenshotParityStats) {
                parityStatsCollector.collectTestStats(testIdentifier, comparisonResult)
                parityStatsCollector.report()
            }

            val expectedWidth = expected.width
            val expectedHeight = expected.height
            expected.recycle()

            throw AssertionError(
                "Sizes are different! Expected: [$expectedWidth, $expectedHeight], Actual: [${
                    actual.width}, ${actual.height}]. " +
                    "Force aligned at (0, 0). Comparison stats: '${comparisonResult
                        .comparisonStatistics}'"
            )
        }

        val comparisonResult =
            matcher.compareBitmaps(
                expected = expected.toIntArray(),
                given = actual.toIntArray(),
                width = actual.width,
                height = actual.height,
                regions = regions
            )
        if (doesCollectScreenshotParityStats) {
            parityStatsCollector.collectTestStats(testIdentifier, comparisonResult)
            parityStatsCollector.report()
        }

        val status =
            if (comparisonResult.matches) {
                ScreenshotResultProto.DiffResult.Status.PASSED
            } else {
                ScreenshotResultProto.DiffResult.Status.FAILED
            }

        if (!comparisonResult.matches) {
            val expectedWithHighlight = highlightedBitmap(expected, regions)
            diffEscrowStrategy.reportResult(
                testIdentifier = testIdentifier,
                goldenIdentifier = goldenIdentifier,
                status = status,
                actual = actual,
                comparisonStatistics = comparisonResult.comparisonStatistics,
                expected = expectedWithHighlight,
                diff = comparisonResult.diff
            )

            expectedWithHighlight.recycle()
            expected.recycle()

            throw AssertionError(
                "Image mismatch! Comparison stats: '${comparisonResult.comparisonStatistics}'"
            )
        }

        expected.recycle()
    }

    override fun createScreenshotAsserter(config: ScreenshotAsserterConfig): ScreenshotAsserter {
        return ScreenshotRuleAsserter.Builder(this)
            .withMatcher(config.matcher)
            .setOnBeforeScreenshot(config.beforeScreenshot)
            .setOnAfterScreenshot(config.afterScreenshot)
            .setScreenshotProvider(config.captureStrategy)
            .build()
    }

    /** This will create a new Bitmap with the output (not modifying the [original] Bitmap */
    private fun highlightedBitmap(original: Bitmap, regions: List<Rect>): Bitmap {
        if (regions.isEmpty()) return original

        val outputBitmap = original.copy(original.config!!, true)
        val imageRect = Rect(0, 0, original.width, original.height)
        val regionLineWidth = 2
        for (region in regions) {
            val regionToDraw =
                Rect(region).apply {
                    inset(-regionLineWidth, -regionLineWidth)
                    intersect(imageRect)
                }

            repeat(regionLineWidth) {
                drawRectOnBitmap(outputBitmap, regionToDraw, Color.RED)
                regionToDraw.inset(1, 1)
                regionToDraw.intersect(imageRect)
            }
        }
        return outputBitmap
    }

    private fun drawRectOnBitmap(bitmap: Bitmap, rect: Rect, @ColorInt color: Int) {
        // Draw top and bottom edges
        for (x in rect.left until rect.right) {
            bitmap.setPixel(x, rect.top, color)
            bitmap.setPixel(x, rect.bottom - 1, color)
        }
        // Draw left and right edge
        for (y in rect.top until rect.bottom) {
            bitmap.setPixel(rect.left, y, color)
            bitmap.setPixel(rect.right - 1, y, color)
        }
    }
}

typealias BitmapSupplier = () -> Bitmap

/** Implements a screenshot asserter based on the ScreenshotRule */
class ScreenshotRuleAsserter private constructor(private val rule: ScreenshotTestRule) :
    ScreenshotAsserter {
    // use the most constraining matcher as default
    private var matcher: BitmapMatcher = PixelPerfectMatcher()
    private var beforeScreenshot: Runnable? = null
    private var afterScreenshot: Runnable? = null

    // use the instrumentation screenshot as default
    private var screenShotter: BitmapSupplier = { Screenshot.capture().bitmap }

    private var pointerLocationSetting: Int
        get() = shell("settings get system ${System.POINTER_LOCATION}").trim().toIntOrNull() ?: 0
        set(value) {
            shell("settings put system ${System.POINTER_LOCATION} $value")
        }

    private var showTouchesSetting
        get() = shell("settings get system ${System.SHOW_TOUCHES}").trim().toIntOrNull() ?: 0
        set(value) {
            shell("settings put system ${System.SHOW_TOUCHES} $value")
        }

    private var prevPointerLocationSetting: Int? = null
    private var prevShowTouchesSetting: Int? = null
    @Suppress("DEPRECATION")
    override fun assertGoldenImage(goldenId: String) {
        runBeforeScreenshot()
        var actual: Bitmap? = null
        try {
            actual = screenShotter()
            rule.assertBitmapAgainstGolden(actual, goldenId, matcher)
        } finally {
            actual?.recycle()
            runAfterScreenshot()
        }
    }

    @Suppress("DEPRECATION")
    override fun assertGoldenImage(goldenId: String, areas: List<Rect>) {
        runBeforeScreenshot()
        var actual: Bitmap? = null
        try {
            actual = screenShotter()
            rule.assertBitmapAgainstGolden(actual, goldenId, matcher, areas)
        } finally {
            actual?.recycle()
            runAfterScreenshot()
        }
    }

    private fun runBeforeScreenshot() {
        prevPointerLocationSetting = pointerLocationSetting
        prevShowTouchesSetting = showTouchesSetting

        if (prevPointerLocationSetting != 0) pointerLocationSetting = 0
        if (prevShowTouchesSetting != 0) showTouchesSetting = 0

        beforeScreenshot?.run()
    }

    private fun runAfterScreenshot() {
        afterScreenshot?.run()

        prevPointerLocationSetting?.let { pointerLocationSetting = it }
        prevShowTouchesSetting?.let { showTouchesSetting = it }
    }

    @Deprecated("Use ScreenshotAsserterFactory instead")
    class Builder(private val rule: ScreenshotTestRule) {
        private var asserter = ScreenshotRuleAsserter(rule)
        fun withMatcher(matcher: BitmapMatcher): Builder = apply { asserter.matcher = matcher }

        /**
         * The [Bitmap] produced by [screenshotProvider] will be recycled immediately after
         * assertions are completed. Therefore, do not retain references to created [Bitmap]s.
         */
        fun setScreenshotProvider(screenshotProvider: BitmapSupplier): Builder = apply {
            asserter.screenShotter = screenshotProvider
        }

        fun setOnBeforeScreenshot(run: Runnable): Builder = apply {
            asserter.beforeScreenshot = run
        }

        fun setOnAfterScreenshot(run: Runnable): Builder = apply { asserter.afterScreenshot = run }

        fun build(): ScreenshotAsserter = asserter.also { asserter = ScreenshotRuleAsserter(rule) }
    }
}

internal fun Bitmap.toIntArray(): IntArray {
    val bitmapArray = IntArray(width * height)
    getPixels(bitmapArray, 0, width, 0, 0, width, height)
    return bitmapArray
}

/**
 * Asserts this bitmap against the golden identified by the given name.
 *
 * Note: The golden identifier should be unique per your test module (unless you want multiple tests
 * to match the same golden). The name must not contain extension. You should also avoid adding
 * strings like "golden", "image" and instead describe what is the golder referring to.
 *
 * @param bitmapDiffer The screenshot test rule that provides the comparison and reporting.
 * @param goldenIdentifier Name of the golden. Allowed characters: 'A-Za-z0-9_-'
 * @param matcher The algorithm to be used to perform the matching. By default [MSSIMMatcher] is
 *   used.
 * @see MSSIMMatcher
 * @see PixelPerfectMatcher
 */
fun Bitmap.assertAgainstGolden(
    bitmapDiffer: BitmapDiffer,
    goldenIdentifier: String,
    matcher: BitmapMatcher = MSSIMMatcher(),
    regions: List<Rect> = emptyList()
) {
    bitmapDiffer.assertBitmapAgainstGolden(
        this,
        goldenIdentifier,
        matcher = matcher,
        regions = regions
    )
}
