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

package android.platform.systemui_tapl.ui

import android.graphics.Rect
import android.platform.systemui_tapl.controller.NotificationIdentity
import android.platform.systemui_tapl.controller.NotificationIdentity.Type.BIG_PICTURE
import android.platform.systemui_tapl.controller.NotificationIdentity.Type.BIG_TEXT
import android.platform.systemui_tapl.controller.NotificationIdentity.Type.BY_TEXT
import android.platform.systemui_tapl.controller.NotificationIdentity.Type.BY_TITLE
import android.platform.systemui_tapl.controller.NotificationIdentity.Type.CALL
import android.platform.systemui_tapl.controller.NotificationIdentity.Type.CONVERSATION
import android.platform.systemui_tapl.controller.NotificationIdentity.Type.CUSTOM
import android.platform.systemui_tapl.controller.NotificationIdentity.Type.GROUP
import android.platform.systemui_tapl.controller.NotificationIdentity.Type.GROUP_AUTO_GENERATED
import android.platform.systemui_tapl.controller.NotificationIdentity.Type.GROUP_MINIMIZED
import android.platform.systemui_tapl.controller.NotificationIdentity.Type.INBOX
import android.platform.systemui_tapl.controller.NotificationIdentity.Type.MEDIA
import android.platform.systemui_tapl.controller.NotificationIdentity.Type.MESSAGING_STYLE
import android.platform.systemui_tapl.ui.NotificationShade.Companion.SHELF_ID
import android.platform.systemui_tapl.utils.DeviceUtils.LONG_WAIT
import android.platform.systemui_tapl.utils.DeviceUtils.androidResSelector
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.test.scenario.tapl_common.TaplUiObject
import android.platform.test.util.HealthTestingUtils.waitForValueToSettle
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisibility
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForNullableObj
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForNullableObjects
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.WaitUtils.ensureThat
import android.platform.uiautomatorhelpers.WaitUtils.retryIfStale
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.time.Duration
import java.util.regex.Pattern
import kotlin.math.floor
import org.junit.Assert.assertTrue

/**
 * Represents the stack of notifications, terminating with the optional [NotificationShelf].
 *
 * This might be shown on several places:
 * - Lockscreen ([LockscreenNotificationShade])
 * - Notification shade ([NotificationShade]), pulled-down from the top of the screen.
 */
open class NotificationStack internal constructor(val fromLockscreen: Boolean) {

    init {
        NOTIFICATION_STACK_SCROLLER.assertVisible { "Notification stack scroller didn't appear" }
    }

    /** Fails when shelf visibility doesn't match [visible] within a timeout. */
    fun assertShelfVisibility(visible: Boolean) {
        uiDevice.assertVisibility(NOTIFICATION_SHELF_SELECTOR, visible) {
            "Notification shelf is ${if (visible) { "invisible" } else {"visible"}}"
        }
    }

    /** Fails when [notifications] size doesn't become [expected] within a timeout. */
    fun assertVisibleNotificationCount(expected: Int) {
        val errorMessage = {
            "Notification count didn't match expectations. " +
                "Count=${notifications.size}, expected=$expected"
        }
        ensureThat("Visible notifications count match", errorProvider = errorMessage) {
            notifications.size == expected
        }
    }

    /** Fails when [notifications] count doesn't become at least [expected] within a timeout. */
    fun assertVisibleNotificationCountAtLeast(expected: Int) {
        val errorMessage = {
            "Notification count didn't match expectations. " +
                "Count=${notifications.size}, expected>=$expected"
        }
        ensureThat("Visible notifications count at least $expected", errorProvider = errorMessage) {
            notifications.size >= expected
        }
    }

    /** Returns visible notifications. */
    val notifications: List<Notification>
        get() =
            waitForValueToSettle(/* errorMessage= */ { "visibleNotifications didn't settle." }) {
                visibleNotifications.map { uiObject ->
                    Notification(uiObject, fromLockscreen, isHeadsUpNotification = false)
                }
            }

    /** Returns visible notifications if able, otherwise [null]. */
    fun tryGetNotifications(): List<Notification>? {
        return retryIfStale(description = "tryGetNotifications", times = 3) {
            return@retryIfStale notifications
        }
    }

    /**
     * Waits for visible notifications to settle (the same number of notifications for several
     * seconds)
     */
    fun waitForNotificationsToSettle() {
        // Accessing visible notifications waits for them to settle
        notifications
    }

    /** @return the bounds of the notification shade. */
    fun getShadeBounds(): Rect {
        val stack = waitForObj(NOTIFICATION_STACK_SCROLLER)
        return Rect().apply { stack.children.forEach { child -> union(child.visibleBounds) } }
    }

    fun getShelfBounds(): Rect {
        return waitForObj(NOTIFICATION_SHELF_SELECTOR).visibleBounds
    }

    /** Returns the [NotificationShelf] if visible, otherwise [null]. */
    fun tryGetNotificationShelf(): NotificationShelf? {
        return retryIfStale(description = "tryGetNotificationShelf", times = 3) {
            return@retryIfStale waitForValueToSettle(
                /* errorMessage= */ { "Notification shelf didn't settle." }) {
                notificationShelfObject?.let { NotificationShelf(rect = it.visibleBounds) }
            }
        }
    }

    /**
     * Finds a notification by its identity. Fails if the notification can't be found.
     *
     * @param identity description of the notification tyoe and properties
     * @param waitTimeout duration to wait for notification to appear.
     * @return Notification (throws assertion if not found)
     */
    @JvmOverloads
    fun findNotification(
        identity: NotificationIdentity,
        waitTimeout: Duration = LONG_WAIT,
    ): Notification =
        findNotificationInternal(
            identity,
            fromLockscreen,
            isHeadsUpNotification = false,
            scroll = false,
            waitTimeout = waitTimeout,
        )

    /**
     * Scrolls to a notification defined by its identity. Fails if the notification can't be found
     * in the shade.
     *
     * @param identity description of the notification tyoe and properties
     * @param waitTimeout duration to wait for notification to appear.
     * @return Notification (throws assertion if not found)
     */
    fun scrollToNotification(
        identity: NotificationIdentity,
        waitTimeout: Duration = LONG_WAIT,
    ): Notification =
        findNotificationInternal(
            identity,
            fromLockscreen,
            isHeadsUpNotification = false,
            scroll = true,
            waitTimeout = waitTimeout,
        )

    companion object {

        /**
         * Finds a HUN by its identity. Fails if the notification can't be found.
         *
         * @param identity The NotificationIdentity used to find the HUN
         * @param assertIsHunState When it's true, findHeadsUpNotification would fail if the
         *   notification is not at the HUN state (eg. showing in the Shade), or its HUN state
         *   cannot be verified. An action button is necessary for the verification. Consider
         *   posting the HUN with NotificationController#postBigTextHeadsUpNotification if you need
         *   to assert the HUN state. Expanded HUN state cannot be asserted.
         * @param waitTimeout duration to wait for the notification to appear.
         * @return Notification (throws assertion if not found)
         */
        @JvmOverloads
        @JvmStatic
        internal fun findHeadsUpNotification(
            identity: NotificationIdentity,
            assertIsHunState: Boolean = true,
            waitTimeout: Duration = LONG_WAIT,
        ): Notification {
            if (!assertIsHunState) {
                return findNotificationInternal(
                    identity = identity,
                    fromLockscreen = false,
                    isHeadsUpNotification = true,
                    scroll = false,
                    waitTimeout = waitTimeout,
                )
            }

            assertTrue(
                "HUN state Assertion usage error: Notification: ${identity.title} " +
                    "| You can only assert the HUN State of a notification that has an action " +
                    "button. Add an action button to the notification or set assertHeadsUpState " +
                    "to false.",
                identity.hasAction,
            )

            val notification =
                findNotificationInternal(
                    identity,
                    fromLockscreen = false,
                    isHeadsUpNotification = true,
                    scroll = false,
                    waitTimeout = waitTimeout,
                )
            notification.verifyIsHunState()
            return notification
        }

        /**
         * Finds a notification by its identity. Fails is the notification can't be found.
         *
         * @param identity description of the notification tyoe and properties.
         * @param fromLockscreen flag set in the returned Notification object.
         * @param isHeadsUpNotification flag set in the returned Notification object.
         * @param scroll allow scrolling to find the notification in the notification stak.
         * @param waitTimeout duration to wait for notification to appear.
         * @return Notification (throws assertion if not found)
         */
        @JvmStatic
        private fun findNotificationInternal(
            identity: NotificationIdentity,
            fromLockscreen: Boolean,
            isHeadsUpNotification: Boolean,
            scroll: Boolean,
            waitTimeout: Duration,
        ): Notification {

            // Generate the selector for the expanded notification.
            val selectorWhenExpanded: BySelector? =
                when (identity.type) {
                    GROUP,
                    GROUP_AUTO_GENERATED,
                    GROUP_MINIMIZED -> null
                    BIG_TEXT -> By.text(identity.textWhenExpanded!!)
                    BIG_PICTURE -> BIG_PICTURE_SELECTOR
                    CUSTOM -> CUSTOM_NOTIFICATION_SELECTOR
                    CALL,
                    MEDIA,
                    INBOX,
                    BY_TEXT -> By.text(identity.text!!)
                    MESSAGING_STYLE,
                    CONVERSATION -> MESSAGE_ICON_CONTAINER_SELECTOR
                    BY_TITLE -> notificationByTitleSelector(identity.title!!)
                }

            // Generate the selector for the notification
            val selector =
                when (identity.type) {
                    GROUP -> groupBySummarySelector(identity.summary!!)
                    GROUP_MINIMIZED -> minimizedGroupBySummarySelector(identity.title!!)
                    GROUP_AUTO_GENERATED -> autoGeneratedGroupByAppNameSelector(identity.summary!!)
                    CALL,
                    MEDIA,
                    INBOX,
                    MESSAGING_STYLE,
                    CONVERSATION,
                    BY_TEXT -> notificationByTextSelector(identity.text!!)
                    BIG_TEXT -> notificationByTitleSelector(identity.title!!)
                    CUSTOM -> CUSTOM_NOTIFICATION_SELECTOR
                    BY_TITLE -> notificationByTitleSelector(identity.title!!)
                    else -> notificationByTitleSelector(identity.text!!)
                }

            // If scrolling is enabled, scroll to the notification using the selector,
            // otherwise, just wait for it.
            val notification =
                if (scroll) {
                    scrollToNotificationBySelector(selector)
                } else {
                    waitForObj(selector, waitTimeout)
                }

            // Notification groups should have at least 2 children
            if (identity.type == GROUP || identity.type == GROUP_AUTO_GENERATED) {
                val childCount = notification.findObjects(NOTIFICATION_ROW_SELECTOR).size
                assertTrue(
                    "Wanted at least 2 children, but found only $childCount.",
                    childCount >= 2,
                )
            }
            return if (identity.type == GROUP) {
                Notification(
                    notification = notification,
                    groupNotificationIdentity = identity,
                    selectorWhenExpanded = selectorWhenExpanded,
                    contentIsVisibleInCollapsedState = false,
                    isBigText = true,
                    pkg = identity.pkg,
                    fromLockscreen = fromLockscreen,
                    isHeadsUpNotification = isHeadsUpNotification,
                )
            } else {
                Notification(
                    notification = notification,
                    selectorWhenExpanded = selectorWhenExpanded,
                    contentIsVisibleInCollapsedState = identity.contentIsVisibleInCollapsedState,
                    isBigText = identity.type == BIG_TEXT,
                    pkg = identity.pkg,
                    fromLockscreen = fromLockscreen,
                    isHeadsUpNotification = isHeadsUpNotification,
                )
            }
        }

        private fun groupBySummarySelector(summary: String): BySelector {
            return By.copy(NOTIFICATION_ROW_SELECTOR)
                .hasDescendant(androidResSelector("header_text").text(summary))
                .hasDescendant(NOTIFICATION_ROW_SELECTOR)
        }

        private fun minimizedGroupBySummarySelector(summary: String): BySelector {
            return By.copy(NOTIFICATION_ROW_SELECTOR)
                .hasDescendant(androidResSelector("header_text").text(summary))
                .hasDescendant(NOTIFICATION_HEADER_EXPAND_BUTTON_SELECTOR)
        }

        private fun autoGeneratedGroupByAppNameSelector(appName: String): BySelector {
            return By.copy(NOTIFICATION_ROW_SELECTOR)
                .hasDescendant(androidResSelector("app_name_text").text(appName))
                .hasDescendant(NOTIFICATION_ROW_SELECTOR)
        }

        private fun notificationByTitleSelector(title: String) =
            By.copy(NOTIFICATION_ROW_SELECTOR)
                .hasDescendant(androidResSelector("title").text(title))

        internal fun notificationByTextSelector(text: String) =
            By.copy(NOTIFICATION_ROW_SELECTOR).hasDescendant(By.text(text))

        internal fun getNotificationCountByIdentityText(identity: NotificationIdentity): Int {
            val notifications: Collection<UiObject2> =
                uiDevice.wait(
                    Until.findObjects(notificationByTextSelector(identity.text!!)),
                    LONG_WAIT.toMillis(),
                ) ?: throw AssertionError("Cannot find notifications with text '${identity.text}'")
            return notifications.size
        }

        private fun scrollToNotificationBySelector(selector: BySelector): UiObject2 {
            // fail if the device doesn't become idle
            uiDevice.waitForIdle(LONG_WAIT.toMillis())
            // wait for the first element longer, maybe our notifications are not posted yet
            var found = waitForNullableObj(selector, LONG_WAIT)
            var scrolledToBottom = NotificationShade.isShowingBottomOfShade
            var retries = 0
            while (
                retries++ < MAX_FIND_NOTIFICATION_ATTEMPTS && !scrolledToBottom && found == null
            ) {
                scrollNotificationListOnce(Direction.DOWN)
                scrolledToBottom = NotificationShade.isShowingBottomOfShade

                found =
                    waitForNullableObj(selector)?.takeIf {
                        // only take this object, if it is entirely scrolled above the shelf
                        scrolledToBottom || isAboveShelf(it)
                    }
            }

            return checkNotNull(found) { "Did not find notification matching $selector" }
        }

        private fun isAboveShelf(notification: UiObject2): Boolean {
            val stack: UiObject2 =
                NotificationShade.notificationsStack ?: error("Notification stack is not visible")
            val shelf = stack.findObject(NOTIFICATION_SHELF_SELECTOR) ?: return true
            return notification.visibleBounds.bottom < shelf.visibleBounds.top
        }

        /** Performs one swipe to scroll notification list. */
        internal fun scrollNotificationListOnce(direction: Direction) {
            val notificationListObject2: UiObject2 =
                NotificationShade.notificationsStack ?: error("Notification stack is not visible")
            val notificationList = TaplUiObject(notificationListObject2, "Notification stack")
            val notificationListY: Int = notificationListObject2.visibleBounds.height()
            notificationList.setGestureMargin(floor(notificationListY * 0.2).toInt())
            notificationList.scroll(direction, 1.0f)
        }

        private val notificationShelfObject: UiObject2?
            get() = uiDevice.waitForNullableObj(NOTIFICATION_SHELF_SELECTOR)

        private val visibleNotifications: List<UiObject2>
            get() = uiDevice.waitForNullableObjects(NOTIFICATION_ROW_SELECTOR) ?: emptyList()

        private const val MAX_FIND_NOTIFICATION_ATTEMPTS = 15
        private val NOTIFICATION_SHELF_SELECTOR =
            sysuiResSelector(SHELF_ID).maxDepth(NotificationShade.NOTIFICATION_MAX_HIERARCHY_DEPTH)
        private val NOTIFICATION_STACK_SCROLLER = sysuiResSelector("notification_stack_scroller")
        private val BIG_PICTURE_SELECTOR = androidResSelector("big_picture")
        private val MESSAGE_ICON_CONTAINER_SELECTOR = androidResSelector("message_icon_container")
        private val CUSTOM_NOTIFICATION_SELECTOR =
            By.text(Pattern.compile("Example text|Example Text"))

        internal val NOTIFICATION_ROW_SELECTOR =
            sysuiResSelector(NotificationShade.EXPANDABLE_NOTIFICATION_ROW)

        internal val NOTIFICATION_HEADER_EXPAND_BUTTON_SELECTOR =
            androidResSelector(NotificationShade.HEADER_EXPAND_BUTTON)
    }
}
