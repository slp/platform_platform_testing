/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.tools.device.apphelpers

import android.app.Instrumentation
import android.graphics.Rect
import android.graphics.Region
import android.tools.datatypes.coversMoreThan
import android.tools.helpers.FIND_TIMEOUT
import android.tools.helpers.GestureHelper
import android.tools.helpers.SYSTEMUI_PACKAGE
import android.tools.traces.ConditionsFactory
import android.tools.traces.component.IComponentNameMatcher
import android.tools.traces.parsers.WindowManagerStateHelper
import android.util.Log
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

abstract class BasePipAppHelper(
    instrumentation: Instrumentation,
    appName: String,
    componentMatcher: IComponentNameMatcher,
) : StandardAppHelper(instrumentation, appName, componentMatcher), PipApp {
    private val gestureHelper: GestureHelper = GestureHelper(instrumentation)

    open fun clickObject(resId: String) {
        val selector = By.res(packageName, resId)
        val obj = uiDevice.findObject(selector) ?: error("Could not find `$resId` object")

        obj.click()
    }

    override fun waitForPip(wmHelper: WindowManagerStateHelper) {
        wmHelper
            .StateSyncBuilder()
            .withWindowSurfaceAppeared(this)
            .withPipShown()
            .waitForAndVerify()
    }

    /** Drags the PIP window to the provided final coordinates without releasing the pointer. */
    override fun dragPipWindowAwayFromEdgeWithoutRelease(
        wmHelper: WindowManagerStateHelper,
        steps: Int,
    ) {
        val initWindowRect = Rect(getWindowRect(wmHelper))

        // initial pointer at the center of the window
        val initialCoord =
            GestureHelper.Tuple(
                initWindowRect.centerX().toFloat(),
                initWindowRect.centerY().toFloat(),
            )

        // the offset to the right (or left) of the window center to drag the window to
        val offset = 50

        // the actual final x coordinate with the offset included;
        // if the pip window is closer to the right edge of the display the offset is negative
        // otherwise the offset is positive
        val endX =
            initWindowRect.centerX() + offset * (if (isCloserToRightEdge(wmHelper)) -1 else 1)
        val finalCoord = GestureHelper.Tuple(endX.toFloat(), initWindowRect.centerY().toFloat())

        // drag to the final coordinate
        gestureHelper.dragWithoutRelease(initialCoord, finalCoord, steps)
    }

    /**
     * Releases the primary pointer.
     *
     * Injects the release of the primary pointer if the primary pointer info was cached after
     * another gesture was injected without pointer release.
     */
    override fun releasePipAfterDragging() {
        gestureHelper.releasePrimaryPointer()
    }

    /**
     * Drags the PIP window away from the screen edge while not crossing the display center.
     *
     * @throws IllegalStateException if default display bounds are not available
     */
    override fun dragPipWindowAwayFromEdge(wmHelper: WindowManagerStateHelper, steps: Int) {
        val initWindowRect = Rect(getWindowRect(wmHelper))

        // initial pointer at the center of the window
        val startX = initWindowRect.centerX()
        val y = initWindowRect.centerY()

        val displayRect =
            wmHelper.currentState.wmState.getDefaultDisplay()?.displayRect
                ?: throw IllegalStateException("Default display is null")

        // the offset to the right (or left) of the display center to drag the window to
        val offset = 20

        // the actual final x coordinate with the offset included;
        // if the pip window is closer to the right edge of the display the offset is positive
        // otherwise the offset is negative
        val endX = displayRect.centerX() + offset * (if (isCloserToRightEdge(wmHelper)) 1 else -1)

        // drag the window to the left but not beyond the center of the display
        uiDevice.drag(startX, y, endX, y, steps)
    }

    /**
     * Returns true if PIP window is closer to the right edge of the display than left.
     *
     * @throws IllegalStateException if default display bounds are not available
     */
    override fun isCloserToRightEdge(wmHelper: WindowManagerStateHelper): Boolean {
        val windowRect = getWindowRect(wmHelper)

        val displayRect =
            wmHelper.currentState.wmState.getDefaultDisplay()?.displayRect
                ?: throw IllegalStateException("Default display is null")

        return windowRect.centerX() > displayRect.centerX()
    }

    /**
     * Expands the PIP window by using the pinch out gesture.
     *
     * @param percent The percentage by which to increase the pip window size.
     * @throws IllegalArgumentException if percentage isn't between 0.0f and 1.0f
     */
    override fun pinchOpenPipWindow(
        wmHelper: WindowManagerStateHelper,
        percent: Float,
        steps: Int,
    ) {
        // the percentage must be between 0.0f and 1.0f
        if (percent <= 0.0f || percent > 1.0f) {
            throw IllegalArgumentException("Percent must be between 0.0f and 1.0f")
        }

        val windowRect = getWindowRect(wmHelper)

        // first pointer's initial x coordinate is halfway between the left edge and the center
        val initLeftX = (windowRect.centerX() - windowRect.width() / 4).toFloat()
        // second pointer's initial x coordinate is halfway between the right edge and the center
        val initRightX = (windowRect.centerX() + windowRect.width() / 4).toFloat()

        // horizontal distance the window should increase by
        val distIncrease = windowRect.width() * percent

        // final x-coordinates
        val finalLeftX = initLeftX - (distIncrease / 2)
        val finalRightX = initRightX + (distIncrease / 2)

        // y-coordinate is the same throughout this animation
        val yCoord = windowRect.centerY().toFloat()

        var adjustedSteps = MIN_STEPS_TO_ANIMATE

        // if distance per step is at least 1, then we can use the number of steps requested
        if (distIncrease.toInt() / (steps * 2) >= 1) {
            adjustedSteps = steps
        }

        // if the distance per step is less than 1, carry out the animation in two steps
        gestureHelper.pinch(
            GestureHelper.Tuple(initLeftX, yCoord),
            GestureHelper.Tuple(initRightX, yCoord),
            GestureHelper.Tuple(finalLeftX, yCoord),
            GestureHelper.Tuple(finalRightX, yCoord),
            adjustedSteps,
        )

        waitForPipWindowToExpandFrom(wmHelper, Region(windowRect))
    }

    /**
     * Minimizes the PIP window by using the pinch in gesture.
     *
     * @param percent The percentage by which to decrease the pip window size.
     * @throws IllegalArgumentException if percentage isn't between 0.0f and 1.0f
     */
    override fun pinchInPipWindow(wmHelper: WindowManagerStateHelper, percent: Float, steps: Int) {
        // the percentage must be between 0.0f and 1.0f
        if (percent <= 0.0f || percent > 1.0f) {
            throw IllegalArgumentException("Percent must be between 0.0f and 1.0f")
        }

        val windowRect = getWindowRect(wmHelper)

        // first pointer's initial x coordinate is halfway between the left edge and the center
        val initLeftX = (windowRect.centerX() - windowRect.width() / 4).toFloat()
        // second pointer's initial x coordinate is halfway between the right edge and the center
        val initRightX = (windowRect.centerX() + windowRect.width() / 4).toFloat()

        // decrease by the distance specified through the percentage
        val distDecrease = windowRect.width() * percent

        // get the final x-coordinates and make sure they are not passing the center of the window
        val finalLeftX = Math.min(initLeftX + (distDecrease / 2), windowRect.centerX().toFloat())
        val finalRightX = Math.max(initRightX - (distDecrease / 2), windowRect.centerX().toFloat())

        // y-coordinate is the same throughout this animation
        val yCoord = windowRect.centerY().toFloat()

        var adjustedSteps = MIN_STEPS_TO_ANIMATE

        // if distance per step is at least 1, then we can use the number of steps requested
        if (distDecrease.toInt() / (steps * 2) >= 1) {
            adjustedSteps = steps
        }

        // if the distance per step is less than 1, carry out the animation in two steps
        gestureHelper.pinch(
            GestureHelper.Tuple(initLeftX, yCoord),
            GestureHelper.Tuple(initRightX, yCoord),
            GestureHelper.Tuple(finalLeftX, yCoord),
            GestureHelper.Tuple(finalRightX, yCoord),
            adjustedSteps,
        )

        waitForPipWindowToMinimizeFrom(wmHelper, Region(windowRect))
    }

    /** Returns the pip window bounds. */
    override fun getWindowRect(wmHelper: WindowManagerStateHelper): Rect {
        val windowRegion = wmHelper.getWindowRegion(this)
        require(!windowRegion.isEmpty) { "Unable to find a PIP window in the current state" }
        return windowRegion.bounds
    }

    /** Taps the pip window and dismisses it by clicking on the X button. */
    open fun closePipWindow(wmHelper: WindowManagerStateHelper) {
        val windowRect = getWindowRect(wmHelper)
        uiDevice.click(windowRect.centerX(), windowRect.centerY())
        // search and interact with the dismiss button
        val dismissSelector = By.res(SYSTEMUI_PACKAGE, "dismiss")
        uiDevice.wait(Until.hasObject(dismissSelector), FIND_TIMEOUT)
        val dismissPipObject =
            uiDevice.findObject(dismissSelector) ?: error("PIP window dismiss button not found")
        val dismissButtonBounds = dismissPipObject.visibleBounds
        uiDevice.click(dismissButtonBounds.centerX(), dismissButtonBounds.centerY())

        // Wait for animation to complete.
        wmHelper.StateSyncBuilder().withPipGone().withHomeActivityVisible().waitForAndVerify()
    }

    open fun tapPipToShowMenu(wmHelper: WindowManagerStateHelper) {
        val windowRect = getWindowRect(wmHelper)
        uiDevice.click(windowRect.centerX(), windowRect.centerY())
        // search and interact with the dismiss button
        val dismissSelector = By.res(SYSTEMUI_PACKAGE, "dismiss")
        uiDevice.wait(Until.hasObject(dismissSelector), FIND_TIMEOUT)
    }

    /** Close the pip window by pressing the expand button */
    fun expandPipWindowToApp(wmHelper: WindowManagerStateHelper) {
        val windowRect = getWindowRect(wmHelper)
        uiDevice.click(windowRect.centerX(), windowRect.centerY())
        // search and interact with the expand button
        val expandSelector = By.res(SYSTEMUI_PACKAGE, "expand_button")
        uiDevice.wait(Until.hasObject(expandSelector), FIND_TIMEOUT)
        val expandPipObject =
            uiDevice.findObject(expandSelector) ?: error("PIP window expand button not found")
        val expandButtonBounds = expandPipObject.visibleBounds
        uiDevice.click(expandButtonBounds.centerX(), expandButtonBounds.centerY())
        wmHelper.StateSyncBuilder().withPipGone().withFullScreenApp(this).waitForAndVerify()
    }

    /** Double click on the PIP window to expand it */
    override fun doubleClickPipWindow(wmHelper: WindowManagerStateHelper) {
        val windowRect = getWindowRect(wmHelper)
        Log.d(TAG, "First click")
        uiDevice.click(windowRect.centerX(), windowRect.centerY())
        Log.d(TAG, "Second click")
        uiDevice.click(windowRect.centerX(), windowRect.centerY())
        Log.d(TAG, "Wait for app transition to end")
        wmHelper.StateSyncBuilder().withAppTransitionIdle().waitForAndVerify()
        waitForPipWindowToExpandFrom(wmHelper, Region(windowRect))
    }

    private fun waitForPipWindowToExpandFrom(
        wmHelper: WindowManagerStateHelper,
        windowRect: Region,
    ) {
        wmHelper
            .StateSyncBuilder()
            .add("pipWindowExpanded") {
                val pipAppWindow =
                    it.wmState.visibleWindows.firstOrNull { window ->
                        this.windowMatchesAnyOf(window)
                    } ?: return@add false
                val pipRegion = pipAppWindow.frameRegion
                return@add pipRegion.coversMoreThan(windowRect)
            }
            .waitForAndVerify()
    }

    private fun waitForPipWindowToMinimizeFrom(
        wmHelper: WindowManagerStateHelper,
        windowRect: Region,
    ) {
        wmHelper
            .StateSyncBuilder()
            .add("pipWindowMinimized") {
                val pipAppWindow =
                    it.wmState.visibleWindows.firstOrNull { window ->
                        this.windowMatchesAnyOf(window)
                    }
                Log.d(TAG, "window $pipAppWindow")
                if (pipAppWindow == null) return@add false
                val pipRegion = pipAppWindow.frameRegion
                Log.d(
                    TAG,
                    "region " + pipRegion + " covers " + windowRect.coversMoreThan(pipRegion),
                )
                return@add windowRect.coversMoreThan(pipRegion)
            }
            .waitForAndVerify()
    }

    /**
     * Waits until the PIP window snaps horizontally to the provided bounds.
     *
     * @param finalBounds the bounds to wait for PIP window to snap to
     */
    override fun waitForPipToSnapTo(wmHelper: WindowManagerStateHelper, finalBounds: Rect) {
        wmHelper
            .StateSyncBuilder()
            .add("pipWindowSnapped") {
                val pipAppWindow =
                    it.wmState.visibleWindows.firstOrNull { window ->
                        this.windowMatchesAnyOf(window)
                    } ?: return@add false
                val pipRegionBounds = pipAppWindow.frameRegion.bounds
                return@add pipRegionBounds.left == finalBounds.left &&
                    pipRegionBounds.right == finalBounds.right
            }
            .add(ConditionsFactory.isWMStateComplete())
            .waitForAndVerify()
    }

    companion object {
        private const val TAG = "BasePipAppHelper"
        // minimum number of steps to take, when animating gestures, needs to be 2
        // so that there is at least a single intermediate layer that flicker tests can check
        private const val MIN_STEPS_TO_ANIMATE = 2
    }
}
