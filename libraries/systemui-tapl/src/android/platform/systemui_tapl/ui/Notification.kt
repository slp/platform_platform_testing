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

import android.graphics.PointF
import android.graphics.Rect
import android.platform.helpers.ui.UiAutomatorUtils.getUiDevice
import android.platform.systemui_tapl.controller.NotificationIdentity
import android.platform.systemui_tapl.ui.NotificationStack.Companion.NOTIFICATION_ROW_SELECTOR
import android.platform.systemui_tapl.ui.NotificationStack.Companion.getNotificationCountByIdentityText
import android.platform.systemui_tapl.ui.NotificationStack.Companion.notificationByTextSelector
import android.platform.systemui_tapl.utils.DeviceUtils.LONG_WAIT
import android.platform.systemui_tapl.utils.DeviceUtils.SHORT_WAIT
import android.platform.systemui_tapl.utils.DeviceUtils.androidResSelector
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.test.scenario.tapl_common.Gestures
import android.platform.test.scenario.tapl_common.TaplUiDevice
import android.platform.uiautomatorhelpers.BetterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisibility
import android.platform.uiautomatorhelpers.DeviceHelpers.betterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForNullableObj
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.FLING_GESTURE_INTERPOLATOR
import android.platform.uiautomatorhelpers.WaitUtils.ensureThat
import android.platform.uiautomatorhelpers.WaitUtils.retryIfStale
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.time.Duration
import org.junit.Assert.assertNull

/** System UI test automation object representing a notification in the notification shade. */
class Notification
internal constructor(
    private val notification: UiObject2,
    private val fromLockscreen: Boolean,
    private val isHeadsUpNotification: Boolean,
    private val groupNotificationIdentity: NotificationIdentity? = null,
    // Selector of a view visible only in the expanded state.
    private val contentIsVisibleInCollapsedState: Boolean = false,
    private val isBigText: Boolean? = null,
    private val pkg: String? = null,
    selectorWhenExpanded: BySelector? = null,
) : Sized(notification.visibleBounds) {

    private val selectorWhenExpanded: BySelector = selectorWhenExpanded ?: COLLAPSE_SELECTOR

    /**
     * Verifies that the notification is in collapsed or expanded state.
     *
     * @param expectedExpanded whether the expected state is "expanded".
     */
    fun verifyExpanded(expectedExpanded: Boolean) {
        notification.assertVisibility(selector = selectorWhenExpanded, visible = expectedExpanded)
    }

    /**
     * Taps the chevron or swipes the specified notification to expand it from the collapsed state.
     *
     * @param dragging By swiping when `true`, by tapping the chevron otherwise.
     */
    fun expand(dragging: Boolean) {
        if (groupNotificationIdentity != null) {
            expandGroup(dragging)
        } else {
            verifyExpanded(false)
            toggleNonGroup(dragging, wasExpanded = false)
            verifyExpanded(true)
        }
    }

    /**
     * Taps the chevron or swipes the specified notification to collapse it from the expanded state.
     *
     * @param dragging By swiping when `true`, by tapping the chevron otherwise.
     */
    fun collapse(dragging: Boolean) {
        assertNull("Collapsing groups is not supported", groupNotificationIdentity)
        verifyExpanded(true)
        toggleNonGroup(dragging, wasExpanded = true)
        verifyExpanded(false)
    }

    /** Dismisses the notification via swipe. */
    fun dismiss() {
        val rowCountBeforeSwipe = expandableNotificationRows.size
        swipeRightOnNotification()

        // Since one group notification was swiped away, the new size shall be smaller
        ensureThat("Number of notifications decreases after swipe") {
            expandableNotificationRows.size < rowCountBeforeSwipe
        }
        // group notification shall not been found again
        groupNotificationIdentity?.let {
            notificationByTextSelector(it.summary!!).assertInvisible()
        }
    }

    fun waitUntilGone() {
        notification.assertVisibility(TITLE_SELECTOR, false)
    }

    /**
     * Verifies that the notification is in HUN state. HUN State: A notification that has the expand
     * button (chevron) at the “expand” status, and has at least an action that is currently
     * showing. We only allow assertion of HUN state for notifications that have action buttons.
     * Fails if the notification is not at the HUN state defined above.
     */
    fun verifyIsHunState() {
        notification.assertVisibility(
            selector = androidResSelector(EXPAND_BUTTON_ID).desc("Expand"),
            visible = true,
            errorProvider = {
                "HUN state assertion error: The notification is found, but not " +
                    "in the HUN status, because didn't find the expand_button at the Expand status."
            },
        )
        notification.assertVisibility(
            selector = ACTION_BUTTON_SELECTOR,
            visible = true,
            errorProvider = {
                "HUN state assertion error: The notification is found, but not " +
                    "in the HUN status, because didn't find an action button."
            },
        )
    }

    /** Swipes on the notification but not able to dismiss the notification. */
    fun swipeButNotDismiss() {
        val rowCountBeforeSwipe = expandableNotificationRows.size
        swipeRightOnNotification()

        // Since one group notification was swiped away, the new size shall be smaller
        ensureThat("Number of notifications keeping the same after swipe") {
            expandableNotificationRows.size == rowCountBeforeSwipe
        }
    }

    /**
     * Swipes vertically on the specified notification. When the notification is a HUN (heads up
     * notification), this expands the shade.
     */
    fun expandShadeFromHun(): NotificationShade {
        assertWithMessage("Not a heads-up notification").that(isHeadsUpNotification).isTrue()
        // drag straight downward by 1/4 of the screen size
        val center = notification.visibleCenter
        uiDevice.betterSwipe(
            startX = center.x,
            startY = center.y,
            endX = center.x,
            endY = center.y + uiDevice.displayHeight / 2,
            interpolator = FLING_GESTURE_INTERPOLATOR,
        )

        val shade = NotificationShade()
        // swipe to show full list. Throws if we aren't in the shade
        shade.scrollToBottom()
        shade.verifyIsShowingFooter()
        return shade
    }

    /** Returns this notification object's visible bounds. */
    fun getBounds(): Rect {
        return notification.visibleBounds
    }

    private fun toggleNonGroup(dragging: Boolean, wasExpanded: Boolean) {
        check(isBigText != null) { "It is needed to know isBigText to use toggle notification" }
        expandNotification(dragging)

        InstrumentationRegistry.getInstrumentation().uiAutomation.clearCache()

        // Expansion indicator be visible on the expanded state, and hidden on the collapsed one.
        if (wasExpanded) {
            assertThat(notification.wait(Until.gone(selectorWhenExpanded), TIMEOUT_MS)).isTrue()

            notification.assertVisibility(By.text(APP_NAME), false)
            notification.assertVisibility(
                By.text(NOTIFICATION_CONTENT_TEXT),
                visible = contentIsVisibleInCollapsedState,
            )
            notification.assertVisibility(By.text(NOTIFICATION_BIG_TEXT), false)
        } else {
            assertThat(notification.wait(Until.hasObject(selectorWhenExpanded), TIMEOUT_MS))
                .isTrue()

            // Expanded state must contain app name.
            notification.assertVisibility(By.text(APP_NAME), true)
            if (isBigText) {
                notification.assertVisibility(By.text(NOTIFICATION_BIG_TEXT), true)
            } else {
                notification.assertVisibility(By.text(NOTIFICATION_CONTENT_TEXT), true)
            }
        }
        notification.assertVisibility(TITLE_SELECTOR, true)
        notification.assertVisibility(androidResSelector(APP_ICON_ID), true)
        notification.assertVisibility(androidResSelector(EXPAND_BUTTON_ID), true)
    }

    private fun expandNotification(dragging: Boolean) {
        val height: Int = notification.visibleBounds.height()
        if (dragging) {
            val center = notification.visibleCenter
            uiDevice.betterSwipe(
                startX = center.x,
                startY = center.y,
                endX = center.x,
                endY = center.y + 300,
                interpolator = FLING_GESTURE_INTERPOLATOR,
            )
        } else {
            tapExpandButton()
        }

        // There isn't an explicit contract for notification expansion, so let's assert
        // that the content height changed, which is likely.
        ensureThat("Notification height changed") { notification.visibleBounds.height() != height }
    }

    fun tapExpandButton() {
        val chevron = notification.waitForObj(androidResSelector(EXPAND_BUTTON_ID))
        Gestures.click(chevron, "Chevron")
    }

    private fun expandGroup(dragging: Boolean) {
        check(dragging) { "Only expanding by dragging is supported for group notifications" }
        val collapsedNotificationsCount =
            getNotificationCountByIdentityText(groupNotificationIdentity!!)

        // drag group notification to bottom to expand group
        val center = notification.visibleCenter
        uiDevice.betterSwipe(
            startX = center.x,
            startY = center.y,
            endX = uiDevice.displayWidth / 2,
            endY = uiDevice.displayHeight,
            interpolator = FLING_GESTURE_INTERPOLATOR,
        )

        // swipe to show full list
        NotificationShade().scrollToBottom()

        // make sure the group notification expanded
        ensureThat("Notification count increases") {
            val expandNotificationsCount =
                getNotificationCountByIdentityText(groupNotificationIdentity)
            collapsedNotificationsCount < expandNotificationsCount
        }
    }

    /** Returns number of messages in the notification. */
    val messageCount: Int
        get() = notification.waitForObj(MESSAGE_SELECTOR).children.size

    /** Long press on notification to show its hidden menu (a.k.a. guts) */
    fun showGuts(): NotificationGuts {
        val longClick = Gestures.longClickDown(notification, "Notification")
        val guts = notification.waitForObj(GUTS_SELECTOR, UI_RESPONSE_TIMEOUT)
        guts.assertVisibility(By.text(APP_NAME), true)
        guts.assertVisibility(By.text(NOTIFICATION_CHANNEL_NAME), true)

        // Confirmation/Settings buttons
        guts.assertVisibility(GUTS_SETTINGS_SELECTOR, true)
        guts.assertVisibility(GUTS_CLOSE_SELECTOR, true)
        longClick.up()
        return NotificationGuts(notification)
    }

    /** Clicks the notification and verifies that the expected app opens. */
    fun clickToApp() {
        Gestures.click(notification, "Notification")
        verifyStartedApp()
    }

    /** Clicks the notification to open the bouncer. */
    fun clickToBouncer(): Bouncer {
        assertWithMessage("The notification should be a lockscreen one")
            .that(fromLockscreen)
            .isTrue()
        Gestures.click(notification, "Notification")
        return Bouncer(/* notification= */ this)
    }

    fun verifyStartedApp() {
        check(
            uiDevice.wait(Until.hasObject(By.pkg(pkg!!).depth(0)), LAUNCH_APP_TIMEOUT.toMillis())
        ) {
            "Did not find application, ${pkg}, in foreground"
        }
    }

    /** Clicks "show bubble" button to show a bubble. */
    fun showBubble() {
        // Create bubble from the notification
        TaplUiDevice.waitForObject(BUBBLE_BUTTON_SELECTOR, "Show bubble button").click()

        // Verify that a bubble is visible
        Root.get().bubble
    }

    /** Taps the snooze button on the notification */
    fun snooze(): Notification = also {
        ensureThat { notification.isLongClickable }

        val snoozeButton = notification.waitForObj(SNOOZE_BUTTON_SELECTOR)

        Gestures.click(snoozeButton, "Snooze button")

        notification.assertVisibility(UNDO_BUTTON_SELECTOR, true)

        ensureThat { !notification.isLongClickable }
    }

    /** Taps undo button on the snoozed notification */
    fun unsnooze(): Notification = also {
        ensureThat { !notification.isLongClickable }

        val undoButton = notification.waitForObj(UNDO_BUTTON_SELECTOR)

        Gestures.click(undoButton, "Undo Snooze button")

        notification.assertVisibility(SNOOZE_BUTTON_SELECTOR, true)

        ensureThat { notification.isLongClickable }
    }

    /** Verifies that the given notification action is enabled/disabled */
    fun verifyActionIsEnabled(actionSelectorText: String, expectedEnabledState: Boolean) {
        val actionButton =
            notification.wait(
                Until.findObject(By.text(actionSelectorText)),
                UI_RESPONSE_TIMEOUT.toMillis(),
            )

        assertThat(actionButton).isNotNull()

        ensureThat { actionButton.isEnabled == expectedEnabledState }
    }

    fun verifyTitleEquals(expected: String) {
        waitForObj(
            By.copy(TITLE_SELECTOR).text(expected),
            errorProvider = { "Couldn't find title with text \"$expected\"" },
        )
    }

    fun verifyBigTextEquals(expected: String) {
        waitForObj(
            By.copy(BIG_TEXT_SELECTOR).text(expected),
            errorProvider = { "Couldn't find big text with text \"$expected\"" },
        )
    }

    fun clickButton(label: String) {
        notification.waitForObj(By.text(label)).click()
    }

    /**
     * Press the reply button, enter [text] to reply with and send.
     *
     * NOTE: Prefer using shorter strings here, as longer ones tend to have a significant effect on
     * performance on slower test devices.
     */
    fun replyWithText(text: String) {
        // This sometimes has issues where it can't find the reply button due to a
        // StaleObjectException, although the button is there. So we attempt each interaciton
        // three times, separately.
        retryIfStale(description = "find reply button", times = 3) {
            notification.waitForObj(REPLY_BUTTON_SELECTOR, SHORT_WAIT).click()
        }

        var remoteInputSelector: UiObject2? = null
        retryIfStale(description = "add reply text \"$text\"", times = 3) {
            remoteInputSelector = notification.waitForObj(REMOTE_INPUT_TEXT_SELECTOR, LONG_WAIT)
        }
        if (remoteInputSelector == null) {
            // If the screen is too small, it might be hidden by IME.
            // Dismiss the IME and try again.
            getUiDevice().pressBack()
            retryIfStale(description = "add reply text \"$text\"", times = 3) {
                remoteInputSelector = notification.waitForObj(REMOTE_INPUT_TEXT_SELECTOR, LONG_WAIT)
            }
        }
        remoteInputSelector?.text = text

        var sendSelector: UiObject2? = null
        retryIfStale(description = "find send selector input", times = 3) {
            sendSelector = notification.waitForObj(REMOTE_INPUT_SEND_SELECTOR, SHORT_WAIT)
        }
        if (sendSelector == null) {
            // If the screen is too small, it might be hidden by IME.
            // Dismiss the IME and try again.
            getUiDevice().pressBack()
            retryIfStale(description = "find send selector input", times = 3) {
                sendSelector = notification.waitForObj(REMOTE_INPUT_SEND_SELECTOR, SHORT_WAIT)
            }
        }
        sendSelector?.click()
    }

    fun assertReplyHistoryContains(reply: String) {
        ensureThat("Reply history should contain \"$reply\"") { replyHistoryContains(reply) }
    }

    fun getUiObject(): UiObject2 = notification

    private fun replyHistoryContains(reply: String): Boolean {
        // Fail if we cannot find the container
        val container = notification.waitForObj(REPLY_HISTORY_CONTAINER, LONG_WAIT)
        return (1..3).any { i ->
            val replyObject =
                container.waitForNullableObj(getReplyHistorySelector(i), SHORT_WAIT)
                    ?: return false // We don't expect more replies
            replyObject.text == reply
        }
    }

    private fun swipeRightOnNotification() {
        val bounds = notification.visibleBounds
        val centerY = (bounds.top + bounds.bottom) / 2f
        BetterSwipe.from(PointF(bounds.left.toFloat(), centerY))
            .to(PointF(bounds.right.toFloat(), centerY), interpolator = FLING_GESTURE_INTERPOLATOR)
            .release()
    }

    companion object {
        private const val APP_NAME = "Scenario"
        private val UI_RESPONSE_TIMEOUT = Duration.ofSeconds(3)
        private val LAUNCH_APP_TIMEOUT = Duration.ofSeconds(10)
        private val SHORT_TRANSITION_WAIT = Duration.ofMillis(1500)
        private val TIMEOUT_MS = LONG_WAIT.toMillis()

        private val TITLE_SELECTOR = androidResSelector("title")
        private val MESSAGE_SELECTOR = androidResSelector("group_message_container")
        private val COLLAPSE_SELECTOR = By.descContains("Collapse")
        private val GUTS_SELECTOR = sysuiResSelector("notification_guts").maxDepth(1)
        private const val NOTIFICATION_CHANNEL_NAME = "Test Channel DEFAULT_IMPORTANCE"
        private val GUTS_SETTINGS_SELECTOR = sysuiResSelector("info")
        private val GUTS_CLOSE_SELECTOR = sysuiResSelector("done")
        private val BUBBLE_BUTTON_SELECTOR = By.res("android:id/bubble_button")
        private val SNOOZE_BUTTON_SELECTOR = androidResSelector("snooze_button")
        private val UNDO_BUTTON_SELECTOR = By.text("Undo")
        private val ACTION_BUTTON_SELECTOR = androidResSelector("action0")
        private val BIG_TEXT_SELECTOR = androidResSelector("big_text")

        // RemoteInput selectors
        private val REPLY_BUTTON_SELECTOR = androidResSelector("action0").descContains("Reply")
        private val REMOTE_INPUT_TEXT_SELECTOR = sysuiResSelector("remote_input_text")
        private val REMOTE_INPUT_SEND_SELECTOR = sysuiResSelector("remote_input_send")
        private val REPLY_HISTORY_CONTAINER =
            androidResSelector("notification_material_reply_container")

        private fun getReplyHistorySelector(index: Int) =
            androidResSelector("notification_material_reply_text_$index")

        const val MAX_FIND_BOTTOM_ATTEMPTS = 15

        const val NOTIFICATION_TITLE_TEXT = "TEST NOTIFICATION"

        @JvmField
        val NOTIFICATION_BIG_TEXT =
            """
            lorem ipsum dolor sit amet
            lorem ipsum dolor sit amet
            lorem ipsum dolor sit amet
            lorem ipsum dolor sit amet
            """
                .trimIndent()
        private const val EXPAND_BUTTON_ID = "expand_button"
        private const val APP_ICON_ID = "icon"
        private const val NOTIFICATION_CONTENT_TEXT = "Test notification content"

        private val expandableNotificationRows: List<UiObject2>
            get() {
                return uiDevice.wait(
                    Until.findObjects(NOTIFICATION_ROW_SELECTOR),
                    SHORT_TRANSITION_WAIT.toMillis(),
                ) ?: emptyList()
            }
    }
}
