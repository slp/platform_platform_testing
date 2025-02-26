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

package android.platform.systemui_tapl.controller

import android.R
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.platform.systemui_tapl.ui.Notification.Companion.NOTIFICATION_BIG_TEXT
import android.platform.uiautomator_helpers.DeviceHelpers

object BigTextNotificationController {
    /** Id of the high importance channel created by the controller. */
    private const val NOTIFICATION_CHANNEL_HIGH_IMPORTANCE_ID = "test_channel_id_high_importance"
    private const val NOTIFICATION_CHANNEL_DEFAULT_IMPORTANCE_ID =
        "test_channel_id_default_importance"
    private const val NOTIFICATION_CONTENT_TEXT = "Test notification content"
    private const val NOTIFICATION_TITLE_TEXT = "TEST NOTIFICATION"

    /**
     * Posts a notification using [Notification.BigTextStyle].
     *
     * @param pkg Default contentIntent launches this App to when clicking on notification.
     * @param highImportance Whether to post the notification with high importance.
     * @param collapsedText Text content when the notification is collapsed.
     * @param expandedText Text content when the notification is expanded.
     * @param title Title of the notification.
     * @param contentIntent The contentIntent when the notification is clicked.
     * @param addActionButton When true, add a default action button to the notification.
     * @param actions The list of actions attached to the notification.
     * @return The [NotificationIdentity] that represents the posted notification.
     */
    @JvmOverloads
    @JvmStatic
    fun postBigTextNotification(
        pkg: String?,
        highImportance: Boolean = false,
        collapsedText: String = NOTIFICATION_CONTENT_TEXT,
        expandedText: String = NOTIFICATION_BIG_TEXT,
        title: String = NOTIFICATION_TITLE_TEXT,
        contentIntent: PendingIntent? = null,
        publicVersion: Notification? = null,
        addActionButton: Boolean = false,
        allowAutoGroup: Boolean = false,
        vararg actions: Notification.Action,
    ): NotificationIdentity {
        val context = DeviceHelpers.context
        val channelId =
            if (highImportance) NOTIFICATION_CHANNEL_HIGH_IMPORTANCE_ID
            else NOTIFICATION_CHANNEL_DEFAULT_IMPORTANCE_ID
        val notificationController = NotificationController.get()

        val builder =
            Notification.Builder(context, channelId)
                .setStyle(Notification.BigTextStyle().bigText(expandedText))
                .setSmallIcon(R.drawable.stat_notify_chat)
                .setContentText(collapsedText)
                .setContentTitle(title)
                .setCategory(Notification.CATEGORY_SYSTEM)
                .setGroupSummary(false)
                .setActions(*actions)

        if (addActionButton) {
            builder.addAction(notificationController.defaultActionBuilder.build())
        }

        publicVersion?.let(builder::setPublicVersion)
        val pendingIntent =
            if (pkg != null && contentIntent == null)
                PendingIntent.getActivity(
                    /* context= */ context,
                    /* requestCode= */ 0,
                    /* intent= */ context.packageManager.getLaunchIntentForPackage(pkg),
                    /* flags= */ Intent.FLAG_ACTIVITY_NEW_TASK or PendingIntent.FLAG_IMMUTABLE,
                )
            else contentIntent
        pendingIntent?.let { builder.setContentIntent(it) }

        if (allowAutoGroup) {
            notificationController.postNotificationNoGroup(builder)
        } else {
            notificationController.postNotification(builder)
        }

        return NotificationIdentity(
            type = NotificationIdentity.Type.BIG_TEXT,
            title = title,
            text = collapsedText,
            summary = null,
            textWhenExpanded = expandedText,
            contentIsVisibleInCollapsedState = true,
            pkg = pkg,
            hasAction = actions.isNotEmpty() or addActionButton,
        )
    }
}
