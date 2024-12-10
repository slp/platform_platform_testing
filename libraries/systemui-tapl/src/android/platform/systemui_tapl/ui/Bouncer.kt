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
import android.graphics.Rect
import android.os.SystemClock.sleep
import android.platform.helpers.Constants
import android.platform.helpers.LockscreenUtils
import android.platform.helpers.LockscreenUtils.LockscreenType
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.systemui_tapl.utils.SYSUI_PACKAGE
import android.platform.uiautomatorhelpers.BetterSwipe.from
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisibility
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.click
import android.platform.uiautomatorhelpers.DeviceHelpers.doubleTapAt
import android.platform.uiautomatorhelpers.DeviceHelpers.shell
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForFirstObj
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.assertOnTheLeftSide
import android.platform.uiautomatorhelpers.assertOnTheRightSide
import android.platform.uiautomatorhelpers.stableBounds
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import com.google.common.truth.Truth
import java.time.Duration
import java.util.regex.Pattern

/** System UI test automation object representing the lockscreen bouncer. */
class Bouncer internal constructor(private val notification: Notification?) {
    private val uiObject: UiObject2 = waitForFirstObj(*BOUNCER_SELECTORS, timeout = LONG_WAIT).first

    private fun enterCodeOnBouncer(lockscreenType: LockscreenType, lockCode: String) {
        LOCKSCREEN_TEXT_BOX_SELECTOR.assertVisible { "Lockscreen text box is not visible" }
        LOCKSCREEN_TEXT_BOX_SELECTOR.click()
        LockscreenUtils.enterCodeOnLockscreen(lockscreenType, lockCode)
    }

    /**
     * Enters pattern based on a string which contains digits between 1-9 to represent a 3x3 grid.
     *
     * The constraint here is that the pattern must start with 5 (the center) as we use the center
     * of the lock pattern view as a reference on where to swipe.
     */
    fun enterPattern(pattern: String) {
        Truth.assertWithMessage("#enterPattern argument does not start with 5")
            .that(pattern.isNotEmpty() && pattern[0] == '5')
            .isTrue()
        val lockPatternView = waitForObj(PATTERN_SELECTOR)
        val visibleCenter = lockPatternView.visibleCenter
        val visibleBounds = lockPatternView.visibleBounds
        val points = mutableListOf<Point>()
        val centerPoint = Point(visibleCenter.x, visibleCenter.y)
        for (c in pattern.substring(1).toCharArray()) {
            points.add(centerPoint)
            points.add(
                when (c) {
                    '1' -> visibleBounds.left to visibleBounds.top
                    '2' -> visibleCenter.x to visibleBounds.top
                    '3' -> visibleBounds.right to visibleBounds.top
                    '4' -> visibleBounds.left to visibleCenter.y
                    '5' -> visibleCenter.x to visibleCenter.y
                    '6' -> visibleBounds.right to visibleCenter.y
                    '7' -> visibleBounds.left to visibleBounds.bottom
                    '8' -> visibleCenter.x to visibleBounds.bottom
                    '9' -> visibleBounds.right to visibleBounds.bottom
                    else -> error("Entering invalid digit: $c")
                }.toPoint()
            )
        }
        val swipe = from(centerPoint)
        points.forEach { swipe.to(it) }
        swipe.release()
    }

    private fun Pair<Int, Int>.toPoint() = Point(first, second)

    /**
     * Enter the Lockscreen code in the enter lockscreen text box.
     *
     * @param lockscreenType type of lockscreen set
     * @param lockCode code to unlock the lockscreen
     */
    fun unlockViaCode(lockscreenType: LockscreenType, lockCode: String) {
        enterCodeOnBouncer(lockscreenType, lockCode)
        LockscreenUtils.checkDeviceLock(false)
        By.res(PAGE_TITLE_SELECTOR_PATTERN).assertInvisible()
        notification?.verifyStartedApp()
    }

    /**
     * Enter invalid Lockscreen code in the enter lockscreen text box and fail to unlock.
     *
     * @param lockscreenType type of lockscreen set
     * @param invalidCode invalid code to unlock the lockscreen
     */
    private fun failedUnlockViaCode(lockscreenType: LockscreenType, invalidCode: String) {
        enterCodeOnBouncer(lockscreenType, invalidCode)

        // Making sure device is still locked. The action happens really fast. Making sure
        // previous action got completed
        sleep((Constants.SHORT_WAIT_TIME_IN_SECONDS * 1000).toLong())
        LockscreenUtils.checkDeviceLock(true)
    }

    /**
     * Enter invalid Lockscreen pin in the enter lockscreen text box and fail to unlock.
     *
     * @param invalidPin invalid pin to unlock the lockscreen
     */
    fun failedUnlockViaPin(invalidPin: String) {
        failedUnlockViaCode(LockscreenType.PIN, invalidPin)
    }

    /**
     * Enter invalid Lockscreen password in the enter lockscreen text box and fail to unlock.
     *
     * @param invalidPassword invalid password to unlock the lockscreen
     */
    fun failedUnlockViaPassword(invalidPassword: String) {
        failedUnlockViaCode(LockscreenType.PASSWORD, invalidPassword)
    }

    /** Check bouncer input UI is on the left side of the screen */
    fun assertOnTheLeftSide(lockscreenType: LockscreenType) {
        getInputUI(lockscreenType).assertOnTheLeftSide()
    }

    /** Check bouncer is on the right side of the screen */
    fun assertOnTheRightSide(lockscreenType: LockscreenType) {
        getInputUI(lockscreenType).assertOnTheRightSide()
    }

    private fun getInputUI(lockscreenType: LockscreenType): UiObject2 {
        return when (lockscreenType) {
            LockscreenType.PIN -> waitForObj(KEYPAD_SELECTOR)
            LockscreenType.PATTERN -> waitForObj(PATTERN_SELECTOR)
            LockscreenType.PASSWORD,
            LockscreenType.SWIPE,
            LockscreenType.NONE -> throw NotImplementedError("Not supported for these auth types")
        }
    }

    /** Double-taps on the left side of the screen. */
    fun doubleTapOnTheLeftSide() {
        doubleTapAtXPosition(uiDevice.displayWidth / 4)
    }

    /** Double-taps on the right side of the screen. */
    fun doubleTapOnTheRightSide() {
        doubleTapAtXPosition(uiDevice.displayWidth * 3 / 4)
    }

    private fun doubleTapAtXPosition(touchX: Int) {
        val touchY = uiDevice.displayHeight / 2
        uiDevice.doubleTapAt(touchX, touchY)
    }

    /** https://hsv.googleplex.com/5840630509993984?node=26 */
    val pinContainerRect: Rect?
        get() {
            return waitForFirstObj(*PIN_CONTAINER_SELECTOR).first.visibleBounds
        }

    /** https://hsv.googleplex.com/5550967647895552?node=25 */
    val pinBouncerContainerRect: Rect
        get() {
            return waitForObj(sysuiResSelector("keyguard_pin_view")).stableBounds
        }

    /** https://hsv.googleplex.com/6358737448075264?node=25 */
    val patternBouncerContainerRect: Rect
        get() {
            return waitForObj(sysuiResSelector("keyguard_pattern_view")).stableBounds
        }

    /** https://hsv.googleplex.com/4951362564521984?node=25 */
    val passwordBouncerContainerRect: Rect
        get() {
            return waitForObj(sysuiResSelector("keyguard_password_view")).stableBounds
        }

    /** Checks whether the delete button exists or not. */
    fun assertDeleteButtonVisibility(visible: Boolean) {
        assertVisibility(PIN_BOUNCER_DELETE_BUTTON, visible)
    }

    /** Checks whether the enter button exists or not. */
    fun assertEnterButtonVisibility(visible: Boolean) {
        assertVisibility(PIN_BOUNCER_ENTER_BUTTON, visible)
    }

    /** Inputs key on the bouncer. */
    fun inputKey(key: String) {
        shell("input keyboard text $key")
    }

    companion object {
        // Default wait used by waitForObj. waitForFirstObj uses a shorter wait.
        private val LONG_WAIT = Duration.ofSeconds(10)

        private val IS_COMPOSE_BOUNCER_ENABLED =
            com.android.systemui.Flags.composeBouncer() ||
                com.android.systemui.Flags.sceneContainer()
        /**
         * Possible selectors for container holding security view like pin, bouncer etc HSV:
         * https://hsv.googleplex.com/5452172222267392?node=22
         *
         * It can be one of these three selectors depending on the flags that are active.
         */
        private val BOUNCER_SELECTORS =
            arrayOf(
                sysuiResSelector("bouncer_root"),
                By.res("element:BouncerContent"),
                sysuiResSelector("view_flipper"),
            )

        private val LOCKSCREEN_TEXT_BOX_SELECTOR =
            if (IS_COMPOSE_BOUNCER_ENABLED) {
                sysuiResSelector("bouncer_text_entry")
            } else {
                By.res(Pattern.compile(SYSUI_PACKAGE + ":id/(pinEntry|passwordEntry)"))
                    .focused(true)
            }

        /** The compose bouncer_text_entry isn't the same as pin_container, but close enough */
        private val PIN_CONTAINER_SELECTOR =
            arrayOf(
                sysuiResSelector("bouncer_text_entry"),
                sysuiResSelector("pin_container"),
            )
        /** https://hsv.googleplex.com/5225465733185536?node=54 */
        private val PIN_BOUNCER_DELETE_BUTTON = sysuiResSelector("delete_button")
        /** https://hsv.googleplex.com/5554629610831872?node=52 */
        private val PIN_BOUNCER_ENTER_BUTTON = sysuiResSelector("key_enter")

        // https://hsv.googleplex.com/5130837462876160?node=117
        private val PAGE_TITLE_SELECTOR_PATTERN =
            Pattern.compile(String.format("%s:id/%s", SYSUI_PACKAGE, "keyguard_clock_container"))

        public val PATTERN_SELECTOR =
            if (IS_COMPOSE_BOUNCER_ENABLED) {
                sysuiResSelector("bouncer_pattern_root")
            } else {
                sysuiResSelector("lockPatternView")
            }

        public val PASSWORD_SELECTOR =
            if (IS_COMPOSE_BOUNCER_ENABLED) {
                sysuiResSelector("bouncer_text_entry")
            } else {
                sysuiResSelector("passwordEntry")
            }

        public val KEYPAD_SELECTOR =
            if (IS_COMPOSE_BOUNCER_ENABLED) {
                sysuiResSelector("pin_pad_grid")
            } else {
                sysuiResSelector("flow1")
            }

        const val VALID_PIN = "1234"

        const val VALID_PASSWORD = "abcd"

        const val DEFAULT_PATTERN = "5624"

        public val USER_ICON_SELECTOR = sysuiResSelector("user_icon")
    }
}
