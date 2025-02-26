/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.app.Activity
import android.app.Dialog
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import platform.test.screenshot.matchers.BitmapMatcher

/** A rule for View screenshot diff unit tests. */
open class ViewScreenshotTestRule(
    private val emulationSpec: DeviceEmulationSpec,
    pathManager: GoldenPathManager,
    private val matcher: BitmapMatcher = UnitTestBitmapMatcher,
    private val decorFitsSystemWindows: Boolean = false,
    protected val screenshotRule: ScreenshotTestRule = ScreenshotTestRule(pathManager),
) : TestRule, BitmapDiffer by screenshotRule, ScreenshotAsserterFactory by screenshotRule {
    private val colorsRule = MaterialYouColorsRule()
    private val fontsRule = FontsRule()
    private val timeZoneRule = TimeZoneRule()
    private val hardwareRenderingRule = HardwareRenderingRule()
    private val deviceEmulationRule = DeviceEmulationRule(emulationSpec)
    private val activityRule = ActivityScenarioRule(ScreenshotActivity::class.java)
    private val commonRule =
        RuleChain.outerRule(deviceEmulationRule).around(screenshotRule).around(activityRule)

    // As denoted in `MaterialYouColorsRule` and `FontsRule`, these two rules need to come first,
    // though their relative orders are not critical.
    private val deviceRule = RuleChain.outerRule(colorsRule).around(commonRule)
    private val roboRule =
        RuleChain.outerRule(colorsRule)
            .around(fontsRule)
            .around(timeZoneRule)
            .around(hardwareRenderingRule)
            .around(commonRule)
    private val isRobolectric = if (Build.FINGERPRINT.contains("robolectric")) true else false

    var frameLimit = 10

    override fun apply(base: Statement, description: Description): Statement {
        val ruleToApply = if (isRobolectric) roboRule else deviceRule
        return ruleToApply.apply(base, description)
    }

    protected fun takeScreenshot(
        mode: Mode = Mode.WrapContent,
        viewProvider: (ComponentActivity) -> View,
        checkView: (ComponentActivity, View) -> Boolean = { _, _ -> false },
        subviewId: Int? = null,
    ): Bitmap {
        activityRule.scenario.onActivity { activity ->
            // Make sure that the activity draws full screen and fits the whole display instead of
            // the system bars.
            val window = activity.window
            window.setDecorFitsSystemWindows(decorFitsSystemWindows)

            // Set the content.
            val inflatedView = viewProvider(activity)
            activity.setContentView(inflatedView, mode.layoutParams)

            // Elevation/shadows is not deterministic when doing hardware rendering, so we disable
            // it for any view in the hierarchy.
            window.decorView.removeElevationRecursively()

            activity.currentFocus?.clearFocus()
        }

        // We call onActivity again because it will make sure that our Activity is done measuring,
        // laying out and drawing its content (that we set in the previous onActivity lambda).
        var targetView: View? = null
        var waitForActivity = true
        var iterCount = 0
        while (waitForActivity && iterCount < frameLimit) {
            activityRule.scenario.onActivity { activity ->
                // Check that the content is what we expected.
                val content = activity.requireViewById<ViewGroup>(android.R.id.content)
                assertEquals(1, content.childCount)
                targetView =
                    fetchTargetView(content, subviewId).also {
                        waitForActivity = checkView(activity, it)
                    }
            }
            iterCount++
        }

        if (waitForActivity) {
            throw IllegalStateException(
                "checkView returned true but frameLimit was reached. Increase the frame limit if " +
                    "more frames are required before the screenshot is taken."
            )
        }

        return targetView?.captureToBitmapAsync()?.get(10, TimeUnit.SECONDS)
            ?: error("timeout while trying to capture view to bitmap")
    }

    private fun fetchTargetView(parent: ViewGroup, subviewId: Int?): View =
        if (subviewId != null) parent.requireViewById(subviewId) else parent.getChildAt(0)

    /**
     * Compare the content of the view provided by [viewProvider] with the golden image identified
     * by [goldenIdentifier] in the context of [emulationSpec].
     */
    fun screenshotTest(
        goldenIdentifier: String,
        mode: Mode = Mode.WrapContent,
        beforeScreenshot: (ComponentActivity) -> Unit = {},
        subviewId: Int? = null,
        viewProvider: (ComponentActivity) -> View,
    ) =
        screenshotTest(
            goldenIdentifier,
            mode,
            checkView = { activity, _ ->
                beforeScreenshot(activity)
                false
            },
            subviewId,
            viewProvider,
        )

    /**
     * Compare the content of the view provided by [viewProvider] with the golden image identified
     * by [goldenIdentifier] in the context of [emulationSpec].
     */
    fun screenshotTest(
        goldenIdentifier: String,
        mode: Mode = Mode.WrapContent,
        checkView: (ComponentActivity, View) -> Boolean,
        subviewId: Int? = null,
        viewProvider: (ComponentActivity) -> View,
    ) {
        val bitmap = takeScreenshot(mode, viewProvider, checkView, subviewId)
        screenshotRule.assertBitmapAgainstGolden(bitmap, goldenIdentifier, matcher)
    }

    /**
     * Compare the content of the dialog provided by [dialogProvider] with the golden image
     * identified by [goldenIdentifier] in the context of [emulationSpec].
     */
    fun dialogScreenshotTest(
        goldenIdentifier: String,
        waitForIdle: () -> Unit = {},
        waitingForDialog: (Dialog) -> Boolean = { _ -> false },
        dialogProvider: (Activity) -> Dialog,
    ) {
        dialogScreenshotTest(
            activityRule,
            screenshotRule,
            matcher,
            goldenIdentifier,
            waitForIdle,
            dialogProvider,
            waitingForDialog,
        )
    }

    enum class Mode(val layoutParams: LayoutParams) {
        WrapContent(LayoutParams(WRAP_CONTENT, WRAP_CONTENT)),
        MatchSize(LayoutParams(MATCH_PARENT, MATCH_PARENT)),
        MatchWidth(LayoutParams(MATCH_PARENT, WRAP_CONTENT)),
        MatchHeight(LayoutParams(WRAP_CONTENT, MATCH_PARENT)),
    }
}
