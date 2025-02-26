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

package android.platform.systemui_tapl.ui

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.RemoteException
import android.os.SystemClock
import android.platform.systemui_tapl.controller.LockscreenController
import android.platform.systemui_tapl.controller.NotificationIdentity
import android.platform.systemui_tapl.ui.ExpandedBubbleStack.Companion.BUBBLE_EXPANDED_VIEW
import android.platform.systemui_tapl.utils.DeviceUtils.LONG_WAIT
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.BetterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.betterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.FLING_GESTURE_INTERPOLATOR
import android.platform.uiautomatorhelpers.TracingUtils.trace
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.android.launcher3.tapl.LauncherInstrumentation
import com.android.launcher3.tapl.Workspace
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import org.junit.Assert

/**
 * The root class for System UI test automation objects. All System UI test automation objects are
 * produced by this class or other System UI test automation objects.
 */
class Root private constructor() {

    /**
     * Opens the notification shade. Use this if there is no need to assert the way of opening it.
     *
     * Uses AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS to open the shade, because it turned
     * out to be more reliable than swipe gestures. Note that GLOBAL_ACTION_NOTIFICATIONS won't open
     * notifications shade if the lockscreen screen is shown.
     */
    fun openNotificationShade(): NotificationShade {
        return openNotificationShadeViaGlobalAction()
    }

    /** Opens the notification shade via AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS. */
    fun openNotificationShadeViaGlobalAction(): NotificationShade {
        trace("Opening notification shade via global action") {
            uiDevice.openNotification()
            waitForShadeToOpen()
            return NotificationShade()
        }
    }

    /** Opens the notification shade via two fingers wipe. */
    fun openNotificationShadeViaTwoFingersSwipe(): NotificationShade {
        return openNotificationShadeViaTwoFingersSwipe(Duration.ofMillis(300))
    }

    /** Opens the notification shade via slow swipe. */
    fun openNotificationShadeViaSlowSwipe(): NotificationShade {
        return openNotificationShadeViaSwipe(Duration.ofMillis(3000))
    }

    /**
     * Opens the notification shade via swipe with a default speed of 500ms and default start point
     * of 10% of the display height. NOTE: with b/277063189, the default start point of a quarter of
     * the way down the screen can overlap a widget and the shade won't open.
     *
     * @param swipeDuration amount of time the swipe will last from start to finish
     * @param heightFraction fraction of the height of the display to start from.
     */
    @JvmOverloads
    fun openNotificationShadeViaSwipe(
        swipeDuration: Duration = Duration.ofMillis(500),
        heightFraction: Float = 0.1F,
    ): NotificationShade {
        trace("Opening notification shade via swipe") {
            val device = uiDevice
            val width = device.displayWidth.toFloat()
            val height = device.displayHeight.toFloat()
            BetterSwipe.from(PointF(width / 2, height * heightFraction))
                .to(PointF(width / 2, height), swipeDuration, FLING_GESTURE_INTERPOLATOR)
                .release()
            waitForShadeToOpen()
            return NotificationShade()
        }
    }

    /**
     * Opens the notification shade via swipe from top of screen. Needed for opening shade while in
     * an app.
     */
    fun openNotificationShadeViaSwipeFromTop(): NotificationShade {
        val device = uiDevice
        // Swipe in first quarter to avoid desktop windowing app handle interactions.
        val swipeXCoordinate = (device.displayWidth / 4).toFloat()
        val height = device.displayHeight.toFloat()
        BetterSwipe.from(PointF(swipeXCoordinate, 0f))
            .to(
                PointF(swipeXCoordinate, height),
                Duration.ofMillis(500),
                FLING_GESTURE_INTERPOLATOR,
            )
            .release()
        waitForShadeToOpen()
        return NotificationShade()
    }

    /** Opens the notification shade via swipe. */
    private fun openNotificationShadeViaTwoFingersSwipe(
        swipeDuration: Duration
    ): NotificationShade {
        val device = uiDevice
        val width = device.displayWidth
        val distance = device.displayHeight / 3 * 2
        // Steps are injected about 5 milliseconds apart
        val steps = swipeDuration.toMillisPart() / 5
        val resId = "com.google.android.apps.nexuslauncher:id/workspace"
        // Wait is only available for UiObject2
        DeviceHelpers.waitForObj(By.res(resId))
        val obj = device.findObject(UiSelector().resourceId(resId))
        obj.performTwoPointerGesture(
            Point(width / 3, 0),
            Point(width / 3 * 2, 0),
            Point(width / 3, distance),
            Point(width / 3 * 2, distance),
            steps,
        )
        waitForShadeToOpen()
        return NotificationShade()
    }

    /**
     * Finds a HUN by its identity. Fails if the notification can't be found.
     *
     * @param identity The NotificationIdentity used to find the HUN
     * @param assertIsHunState When it's true, findHeadsUpNotification would fail if the
     *   notification is not at the HUN state (eg. showing in the Shade), or its HUN state cannot be
     *   verified. An action button is necessary for the verification. Consider posting the HUN with
     *   NotificationController#postBigTextHeadsUpNotification if you need to assert the HUN state.
     *   Expanded HUN state cannot be asserted.
     */
    @JvmOverloads
    fun findHeadsUpNotification(
        identity: NotificationIdentity,
        assertIsHunState: Boolean = true,
    ): Notification {
        return NotificationStack.findHeadsUpNotification(
            identity = identity,
            assertIsHunState = assertIsHunState,
        )
    }

    /**
     * Ensures there is not a HUN with this identity. Fails if the HUN is found, or the identity
     * doesn't have an action button.
     *
     * @param identity The NotificationIdentity used to find the HUN, an action button is necessary
     */
    // TODO(b/295209746): More robust (and more performant) assertion for "HUN does not appear"
    fun ensureNoHeadsUpNotification(identity: NotificationIdentity) {
        Assert.assertTrue(
            "HUN state Assertion usage error: Notification: ${identity.title} " +
                "| You can only assert the HUN State of a notification that has an action " +
                "button.",
            identity.hasAction,
        )
        Assert.assertThrows(IllegalStateException::class.java) {
            findHeadsUpNotification(identity, assertIsHunState = false)
        }
    }

    /** Opens the quick settings via AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS. */
    fun openQuickSettingsViaGlobalAction(): QuickSettings {
        val device = uiDevice
        device.openQuickSettings()
        // Quick Settings isn't always open when this is complete. Explicitly wait for the Quick
        // Settings footer to make sure that the buttons are accessible when the bar is open and
        // this call is complete.
        FOOTER_SELECTOR.assertVisible()
        // Wait an extra bit for the animation to complete. If we return to early, future callers
        // that are trying to find the location of the footer will get incorrect coordinates
        device.waitForIdle(LONG_TIMEOUT.toLong())
        return QuickSettings()
    }

    /** Gets status bar. */
    val statusBar: StatusBar
        get() = StatusBar()

    /** Gets an alert dialog. */
    val alertDialog: AlertDialog
        get() = AlertDialog()

    /** Gets a media projection permission dialog. */
    val mediaProjectionPermissionDialog: MediaProjectionPermissionDialog
        get() = MediaProjectionPermissionDialog()

    /** Gets a media projection app selector. */
    val mediaProjectionAppSelector: MediaProjectionAppSelector
        get() = MediaProjectionAppSelector()

    /** Asserts that the media projection permission dialog is not visible. */
    fun assertMediaProjectionPermissionDialogNotVisible() {
        MediaProjectionPermissionDialog.assertSpinnerVisibility(false)
    }

    /** Gets lock screen. Fails if lock screen is not visible. */
    val lockScreen: LockScreen
        get() = LockScreen()

    /** Gets primary bouncer. Fails if the primary bouncer is not visible. */
    val primaryBouncer: Bouncer
        get() = Bouncer(null)

    /** Gets Aod. Fails if Aod is not visible. */
    val aod: Aod
        get() = Aod()

    /** Gets ChooseScreenLock. Fails if ChooseScreenLock is not visible. */
    val chooseScreenLock: ChooseScreenLock
        get() = ChooseScreenLock()

    /** Gets the bubble. Fails if there is no bubble. */
    val bubble: Bubble
        get() {
            val bubbleViews = Bubble.bubbleViews
            return Bubble(bubbleViews[0])
        }

    /**
     * Returns the selected bubble.
     *
     * Bubbles in the collapsed stack are reversed. The selected bubble is the last bubble in the
     * view hierarchy.
     */
    val selectedBubble: Bubble
        get() {
            val bubbleViews = Bubble.bubbleViews
            return Bubble(bubbleViews.last())
        }

    /** Gets the expanded bubble stack. Fails if no stack or if the stack is not expanded. */
    val expandedBubbleStack: ExpandedBubbleStack
        get() = ExpandedBubbleStack()

    /** Gets the collapsed bubble bar in launcher. */
    val bubbleBar: BubbleBar
        get() = BubbleBar()

    /** Verifies that the bubble bar is hidden. */
    fun verifyBubbleBarIsHidden() {
        BubbleBar.BUBBLE_BAR_VIEW.assertInvisible(LONG_WAIT)
    }

    /** Verifies that no bubbles or an expanded bubble stack are visible. */
    fun verifyNoBubbleIsVisible() {
        Bubble.BUBBLE_VIEW.assertInvisible(timeout = Bubble.FIND_OBJECT_TIMEOUT)
        verifyNoExpandedBubbleStackIsVisible()
    }

    /** Verifies that expanded bubble stack is not visible. */
    fun verifyNoExpandedBubbleStackIsVisible() {
        BUBBLE_EXPANDED_VIEW.assertInvisible(timeout = Bubble.FIND_OBJECT_TIMEOUT)
    }

    /** Verifies that status bar is hidden by checking StatusBar's clock icon whether it exists. */
    fun verifyStatusBarIsHidden() {
        assertThat(
                uiDevice.wait(
                    Until.gone(sysuiResSelector(StatusBar.CLOCK_ID)),
                    SHORT_TIMEOUT.toLong(),
                )
            )
            .isTrue()
    }

    /** Takes a screenshot and returns the actions panel that appears. */
    fun screenshot(): ScreenshotActions {
        val device = uiDevice
        device.pressKeyCode(KeyEvent.KEYCODE_SYSRQ)
        check(
            device.wait(Until.hasObject(GLOBAL_SCREENSHOT_SELECTOR), SCREENSHOT_POST_TIMEOUT_MSEC)
        ) {
            "Can't find screenshot image"
        }
        return ScreenshotActions()
    }

    /** Gets the power panel. Fails if there is no power panel visible. */
    val powerPanel: PowerPanel
        get() = PowerPanel()

    /**
     * Goes to Launcher workspace by sending KeyEvent.KEYCODE_HOME. This method is not
     * representative of real user's actions, but it's more stable than
     * LauncherInstrumentation.goHome because LauncherInstrumentation.goHome expects all prior
     * animations to settle before it's used, which is true for Launcher tests that use it, but not
     * necessarily true for SysUI tests.
     *
     * @return the Workspace object.
     */
    fun goHomeViaKeycode(): Workspace {
        uiDevice.pressHome()
        // getWorkspace will check `expectedRotation` and fail if it doesn't match the one from
        // the device. However, if the test has an Orientation annotation, the orientation won't
        // be fixed back until after this is run, possibly failing the test.
        val instrumentation = LauncherInstrumentation()
        instrumentation.setExpectedRotation(uiDevice.displayRotation)
        return instrumentation.getWorkspace()
    }

    private fun wakeUp() {
        try {
            uiDevice.wakeUp()
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    /** Returns the volume dialog or fails if it's invisible. */
    val volumeDialog: VolumeDialog
        get() = VolumeDialog()

    /** Asserts that the volume dialog is not visible. */
    fun assertVolumeDialogNotVisible() {
        VolumeDialog.PAGE_TITLE_SELECTOR.assertInvisible()
    }

    /** Asserts that lock screen is invisible. */
    fun assertLockScreenNotVisible() {
        LockScreen.LOCKSCREEN_SELECTOR.assertInvisible()
    }

    // TODO (b/277105514): Determine whether this is an idiomatic method of determing visibility.
    /** Asserts that launcher is visible. */
    fun assertLauncherVisible() {
        By.pkg("com.google.android.apps.nexuslauncher").assertVisible()
    }

    val keyboardBacklightIndicatorDialog: KeyboardBacklightIndicatorDialog
        get() = KeyboardBacklightIndicatorDialog()

    fun assertKeyboardBacklightIndicatorDialogNotVisible() {
        KeyboardBacklightIndicatorDialog.CONTAINER_SELECTOR.assertInvisible()
    }

    private fun injectEventSync(event: InputEvent): Boolean {
        return InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .injectInputEvent(event, true)
    }

    private fun sendKey(keyCode: Int, metaState: Int, eventTime: Long): Boolean {
        val downEvent =
            KeyEvent(
                eventTime,
                eventTime,
                KeyEvent.ACTION_DOWN,
                keyCode,
                0,
                metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                0,
                InputDevice.SOURCE_KEYBOARD,
            )
        if (injectEventSync(downEvent)) {
            val upEvent =
                KeyEvent(
                    eventTime,
                    eventTime,
                    KeyEvent.ACTION_UP,
                    keyCode,
                    0,
                    metaState,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    0,
                    InputDevice.SOURCE_KEYBOARD,
                )
            if (injectEventSync(upEvent)) {
                return true
            }
        }
        return false
    }

    private fun pressKeyCode(keyCode: Int, eventTime: Long) {
        sendKey(keyCode, /* metaState= */ 0, eventTime)
    }

    /** Double-taps the power button. Can be used to bring up the camera app. */
    fun doubleTapPowerButton() {
        val eventTime = SystemClock.uptimeMillis()
        pressKeyCode(KeyEvent.KEYCODE_POWER, eventTime)
        pressKeyCode(KeyEvent.KEYCODE_POWER, eventTime + 1)
    }

    /** Opens the tutorial by swiping. */
    fun openTutorialViaSwipe(): OneHandModeTutorial {
        NotificationShade.waitForShadeToClose()
        val windowMetrics: WindowMetrics =
            DeviceHelpers.context
                .getSystemService(WindowManager::class.java)!!
                .getCurrentWindowMetrics()
        val insets: WindowInsets = windowMetrics.getWindowInsets()
        val displayBounds: Rect = windowMetrics.getBounds()
        val bottomMandatoryGestureHeight: Int =
            insets
                .getInsetsIgnoringVisibility(
                    WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout()
                )
                .bottom
        NotificationShade.waitForShadeToClose()
        uiDevice.betterSwipe(
            displayBounds.width() / 2,
            displayBounds.height() - Math.round(bottomMandatoryGestureHeight * 2.5f),
            displayBounds.width() / 2,
            displayBounds.height(),
        )
        NotificationShade.waitForShadeToClose()
        return OneHandModeTutorial()
    }

    /**
     * Turn the device off and on, and check for the lockScreen. This should be used instead of
     * LockscreenUtils.goToLockScreen() because LockscreenController validates that the screen is
     * off or on, rather than just sleeping and waking up the device. "return lockScreen" calls the
     * LockScreen constructor, which ensures that the lockscreen clock is visible
     *
     * TODO: replace LockscreenUtils.goToLockscreen() with this once it's submitted: b/322870306
     */
    fun goToLockscreen(): LockScreen {
        LockscreenController.get().turnScreenOff()
        LockscreenController.get().turnScreenOn()
        return lockScreen
    }

    companion object {
        private val QS_HEADER_SELECTOR =
            if (com.android.systemui.Flags.sceneContainer()) {
                sysuiResSelector("shade_header_root")
            } else {
                sysuiResSelector("split_shade_status_bar")
            }
        private val NOTIFICATION_SHADE_OPEN_TIMEOUT = Duration.ofSeconds(20)
        private const val LONG_TIMEOUT = 2000
        private const val SHORT_TIMEOUT = 500
        private val FOOTER_SELECTOR = sysuiResSelector("qs_footer_actions")
        private const val SCREENSHOT_POST_TIMEOUT_MSEC: Long = 20000
        private val GLOBAL_SCREENSHOT_SELECTOR = sysuiResSelector("screenshot_actions")

        /** Returns an instance of Root. */
        @JvmStatic
        fun get(): Root {
            return Root()
        }

        private fun waitForShadeToOpen() {
            // Note that this duplicates the tracing done by assertVisible, but with a better name.
            trace("waitForShadeToOpen") {
                QS_HEADER_SELECTOR.assertVisible(
                    timeout = NOTIFICATION_SHADE_OPEN_TIMEOUT,
                    errorProvider = { "Notification shade didn't open" },
                )
            }
        }
    }
}
