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

import android.os.SystemClock
import android.platform.helpers.CommonUtils
import android.platform.systemui_tapl.utils.DeviceUtils.LONG_WAIT
import android.platform.systemui_tapl.utils.DeviceUtils.settingsResSelector
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.test.scenario.tapl_common.Gestures
import android.platform.test.scenario.tapl_common.TaplUiDevice
import android.platform.test.scenario.tapl_common.TaplUiObject
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.betterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.context
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.FLING_GESTURE_INTERPOLATOR
import android.platform.uiautomatorhelpers.TracingUtils.trace
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.android.launcher3.tapl.LauncherInstrumentation
import com.android.systemui.Flags
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.floor

/** System UI test automation object representing the notification shade. */
class NotificationShade internal constructor() {
    init {
        if (CommonUtils.isSplitShade()) {
            val qsBounds = quickSettingsContainer.visibleBounds
            val notificationBounds = notificationShadeScrollContainer.visibleBounds
            assertWithMessage(
                    "Quick settings container is not positioned left to notification stack scroller"
                )
                .that(qsBounds.right <= notificationBounds.left)
                .isTrue()
        }
    }

    /* fromLockscreen= */
    /** Returns the shade's notification stack. */
    val notificationStack: NotificationStack
        get() = NotificationStack(/* fromLockscreen= */ false)

    /**
     * Returns a SystemUI test object representing the Quick Quick Settings element in the
     * Notification Shade.
     */
    val quickQuickSettings: QuickQuickSettings
        get() = QuickQuickSettings()

    /** Check whether QuickSettings are expanded in the NotificationShade. */
    fun assertQuickSettingsExpanded() {
        assertWithMessage("QuickQuickSettings is visible")
            .waitUntilGone(QuickQuickSettings.UI_QUICK_QUICK_SETTINGS_CONTAINER_SELECTOR)
    }

    /** Check whether QuickSettings are collapsed in the NotificationShade. */
    fun assertQuickSettingsCollapsed() {
        assertWithMessage("QuickQuickSettings not visible, shade is not collapsed")
            .waitUntilVisible(QuickQuickSettings.UI_QUICK_QUICK_SETTINGS_CONTAINER_SELECTOR)
    }

    fun verifyIsEmpty() {
        assertWithMessage("Notification shade is not empty")
            .waitUntilVisible(sysuiResSelector(UI_EMPTY_SHADE_VIEW_ID))
    }

    fun verifyIsNotEmpty() {
        assertWithMessage("Notification shade is empty")
            .waitUntilGone(sysuiResSelector(UI_EMPTY_SHADE_VIEW_ID))
    }

    fun verifyIsShowingFooter() {
        assertWithMessage("Notification footer is invisible")
            .waitUntilVisible(sysuiResSelector(UI_SETTINGS_BUTTON_ID))
    }

    fun verifyIsNotShowingFooter() {
        assertWithMessage("Notification footer is visible")
            .waitUntilGone(sysuiResSelector(UI_SETTINGS_BUTTON_ID))
    }

    /** Click Manage button to open notification settings page. */
    fun openNotificationSettingsFromButton() {
        val manageBtn =
            if (Flags.notificationsRedesignFooterView())
                scrollAndFindButton("Notification settings")
            else scrollAndFindButton("Manage")
        assertThat(manageBtn).isNotNull()
        Gestures.click(manageBtn!!, "Settings button")

        settingsResSelector("app_bar").assertVisible()
    }

    private fun scrollAndFindButton(desc: String): UiObject2? {
        var btn: UiObject2? = null
        for (i in 0 until SCROLL_TIMES) {
            btn = uiDevice.wait(Until.findObject(By.desc(desc)), LONG_WAIT.toMillis())
            if (btn != null) {
                break
            }
            flingDown()
        }
        return btn
    }

    /** Presses Clear All button. */
    fun clearAllNotifications() {
        scrollToBottom()
        val device = uiDevice
        sysuiResSelector(UI_EMPTY_SHADE_VIEW_ID).assertInvisible {
            "Shade is empty; cannot clear all"
        }
        TaplUiDevice.waitForObject(
                sysuiResSelector(UI_CLEAR_ALL_BUTTON_ID),
                objectName = "Clear All button",
            )
            .click()
        waitForShadeToClose()
        Root.get().goHomeViaKeycode()
    }

    /**
     * Performs a fling gesture from the bottom towards the top of the shade, thereby scrolling it
     * down (or closing it where appropriate).
     */
    fun flingUp() {
        fling(Direction.UP)
    }

    /**
     * Performs a fling gesture from the top towards the bottom of the shade, thereby scrolling it
     * up (or opening the quick settings where appropriate).
     */
    fun flingDown() {
        fling(Direction.DOWN)
    }

    private fun fling(direction: Direction) {
        val notificationObject = notificationsStack ?: error("Notification stack is not visible")
        val notificationList = TaplUiObject(notificationObject, "Notification stack")
        val notificationListY: Int = notificationObject.visibleBounds.height()
        notificationList.setGestureMargin(floor(notificationListY * 0.2).toInt())
        notificationList.fling(direction, 1.0f)
        uiDevice.waitForIdle()
    }

    /** Scrolls the shade to the bottom. */
    fun scrollToBottom() {
        for (retries in 0 until Notification.MAX_FIND_BOTTOM_ATTEMPTS) {
            // Checks the notification list has scrolled to the bottom or not
            if (isShowingBottomOfShade) {
                notificationStack.assertShelfVisibility(/* visible= */ false)
                return
            }
            NotificationStack.scrollNotificationListOnce(Direction.DOWN)
        }
        throw AssertionError("Failed to find the bottom of the notification shade")
    }

    /** Closes the shade. */
    fun close() {
        val device = uiDevice
        // Swipe in first quarter to avoid desktop windowing app handle interactions.
        val swipeXCoordinate = device.displayWidth / 4
        device.betterSwipe(
            startX = swipeXCoordinate,
            startY = screenBottom,
            endX = swipeXCoordinate,
            endY = 0,
            interpolator = FLING_GESTURE_INTERPOLATOR,
        )
        waitForShadeToClose()
    }

    /** Closes the shade with the back button. */
    fun closeWithBackButton() {
        LauncherInstrumentation().pressBack()
        waitForShadeToClose()
    }

    // UiDevice#getDisplayHeight() excludes insets.
    private val screenBottom: Int
        get() {
            val mWindowMetrics: WindowMetrics =
                context
                    .getSystemService<WindowManager>(WindowManager::class.java)!!
                    .getMaximumWindowMetrics()

            // UiDevice#getDisplayHeight() excludes insets.
            return mWindowMetrics.getBounds().height() - 1
        }

    /** Scrolls the shade down. */
    fun scrollDown() {
        NotificationStack.scrollNotificationListOnce(Direction.DOWN)
    }

    /** Scrolls the shade up. */
    fun scrollUp() {
        NotificationStack.scrollNotificationListOnce(Direction.UP)
    }

    /**
     * Returns the type of the shade.
     *
     * This depends by the device characteristics (e.g. currently large screens in landscape has a
     * split shade, composed by two columns)
     */
    val type: NotificationShadeType
        get() {
            val stackBounds = notificationsStack!!.visibleBounds
            val stackWidth = stackBounds.width()
            val displayWidth = uiDevice.displayWidth
            return if (stackWidth <= displayWidth / 2) NotificationShadeType.SPLIT
            else NotificationShadeType.NORMAL
        }

    /** Opens quick settings via swipe. */
    fun openQuickSettings(): QuickSettings {
        val device = uiDevice
        // Swipe in first quarter to avoid desktop windowing app handle interactions.
        val swipeXCoordinate = device.displayWidth / 4
        device.betterSwipe(
            startX = swipeXCoordinate,
            startY = 0,
            endX = swipeXCoordinate,
            endY = device.displayHeight,
        )
        SystemClock.sleep(SHORT_TIMEOUT.toLong())
        return QuickSettings()
    }

    /** Returns Quick Settings (aka expanded Quick Settings) or fails if it's not visible. */
    val quickSettings: QuickSettings
        get() = QuickSettings()

    /**
     * Returns the visible UMO, or fails if it's not visible.
     *
     * **See:** [HSV](https://hsv.googleplex.com/5715413598994432?node=44)
     */
    val universalMediaObject: UniversalMediaObject
        get() = UniversalMediaObject()

    /**
     * Returns the QS header. Experimental.
     *
     * It provided both from here and from [QuickSettings] because there are slightly different
     * layouts when QS are expanded and collapsed.
     */
    val header: QSHeader
        get() = QSHeader()

    companion object {
        private val QS_HEADER_SELECTOR = sysuiResSelector("split_shade_status_bar")
        private const val WAIT_TIME = 10_000L
        private const val UI_EMPTY_SHADE_VIEW_ID = "no_notifications"
        private val UI_SETTINGS_BUTTON_ID =
            if (Flags.notificationsRedesignFooterView()) "settings_button" else "manage_text"
        private const val UI_QS_CONTAINER_ID = "quick_settings_container"
        private const val UI_RESPONSE_TIMEOUT_MSECS: Long = 3000
        private const val UI_CLEAR_ALL_BUTTON_ID = "dismiss_text"
        private const val SHORT_TRANSITION_WAIT: Long = 1500
        private const val UI_NOTIFICATION_LIST_ID = "notification_stack_scroller"
        private const val SCROLL_TIMES = 3
        private const val SHORT_TIMEOUT = 500
        const val NOTIFICATION_MAX_HIERARCHY_DEPTH = 4
        const val EXPANDABLE_NOTIFICATION_ROW = "expandableNotificationRow"
        const val SHELF_ID = "notificationShelf"
        const val UI_SCROLLABLE_ELEMENT_ID = "notification_stack_scroller"
        const val HEADER_EXPAND_BUTTON = "expand_button"
        val notificationsStack: UiObject2?
            get() =
                uiDevice.wait(
                    Until.findObject(sysuiResSelector(UI_NOTIFICATION_LIST_ID)),
                    SHORT_TRANSITION_WAIT,
                )

        val isShowingBottomOfShade: Boolean
            get() = isShowingEmptyShade || isShowingFooter

        private val isShowingEmptyShade: Boolean
            get() = uiDevice.hasObject(sysuiResSelector(UI_EMPTY_SHADE_VIEW_ID))

        private val isShowingFooter: Boolean
            get() = uiDevice.hasObject(sysuiResSelector(UI_SETTINGS_BUTTON_ID))

        private val quickSettingsContainer: UiObject2
            get() =
                uiDevice.wait(
                    Until.findObject(sysuiResSelector(UI_QS_CONTAINER_ID)),
                    UI_RESPONSE_TIMEOUT_MSECS,
                ) ?: error("Can't find qs container.")

        private val notificationShadeScrollContainer: UiObject2
            get() =
                uiDevice.wait(
                    Until.findObject(sysuiResSelector(UI_SCROLLABLE_ELEMENT_ID)),
                    UI_RESPONSE_TIMEOUT_MSECS,
                ) ?: error("Can't find notification shade scroll container.")

        @JvmStatic
        fun waitForShadeToClose() {
            trace("waitForShadeToClose") {
                // QS header view used in all configurations of Notification shade.
                QS_HEADER_SELECTOR.assertInvisible { "Notification shade didn't close" }
                // Asserts on new QS resId.
                sysuiResSelector("shade_header_root").assertInvisible {
                    "Notification shade didn't close"
                }
            }
        }
    }
}

private fun StandardSubjectBuilder.waitUntilGone(selector: BySelector) {
    that(uiDevice.wait(Until.gone(selector), LONG_WAIT.toMillis())).isTrue()
}

private fun StandardSubjectBuilder.waitUntilVisible(selector: BySelector) {
    that(uiDevice.wait(Until.hasObject(selector), LONG_WAIT.toMillis())).isTrue()
}
