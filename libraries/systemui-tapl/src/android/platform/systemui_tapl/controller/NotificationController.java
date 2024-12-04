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

package android.platform.systemui_tapl.controller;

import static android.app.Notification.CATEGORY_SYSTEM;
import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_MIN;
import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.platform.systemui_tapl.ui.Notification.NOTIFICATION_BIG_TEXT;
import static android.platform.test.util.HealthTestingUtils.waitForCondition;
import static android.platform.uiautomator_helpers.DeviceHelpers.getContext;
import static android.platform.uiautomator_helpers.DeviceHelpers.getUiDevice;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.R;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.Notification.MessagingStyle;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Person;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Controller for manipulating notifications. */
public class NotificationController {
    private static final String LOG_TAG = "NotificationController";

    private static final String NOTIFICATION_TITLE_TEXT = "TEST NOTIFICATION";

    private static final String INCOMING_CALL_TEXT = "Incoming call";

    private static final String NOTIFICATION_GROUP = "Test group";
    private static final String CUSTOM_TEXT = "Example text";
    private static final String NOTIFICATION_CONTENT_TEXT_FORMAT = "Test notification %d";

    /** Id of the high importance channel created by the controller. */
    public static final String NOTIFICATION_CHANNEL_HIGH_IMPORTANCE_ID =
            "test_channel_id_high_importance";

    public static final String NOTIFICATION_CHANNEL_DEFAULT_IMPORTANCE_ID =
            "test_channel_id_default_importance";

    public static final String NOTIFICATION_CHANNEL_LOW_IMPORTANCE_ID =
            "test_channel_id_low_importance";

    public static final String NOTIFICATION_CHANNEL_MIN_IMPORTANCE_ID =
            "test_channel_id_min_importance";

    private static final String NOTIFICATION_CONTENT_TEXT = "Test notification content";
    private static final String NOTIFICATION_CHANNEL_HIGH_IMPORTANCE_NAME =
            "Test Channel HIGH_IMPORTANCE";
    private static final String NOTIFICATION_CHANNEL_DEFAULT_IMPORTANCE_NAME =
            "Test Channel DEFAULT_IMPORTANCE";

    private static final String NOTIFICATION_CHANNEL_LOW_IMPORTANCE_NAME =
            "Test Channel LOW_IMPORTANCE";
    private static final String NOTIFICATION_CHANNEL_MIN_IMPORTANCE_NAME =
            "Test Channel MIN_IMPORTANCE";
    private static final String NOTIFICATION_GROUP_KEY_FORMAT = "Test group %d";
    private static final String PACKAGE_NAME =
            getInstrumentation().getTargetContext().getPackageName();
    private static final String EXTRA_NAME_MESSAGE = "message";
    private static final String DEFAULT_TEST_SHORTCUT_ID = "test_shortcut_id";

    private static final android.app.NotificationManager NOTIFICATION_MANAGER =
            getInstrumentation().getTargetContext().getSystemService(NotificationManager.class);

    private static int nextNotificationId = 0;

    private static final String DEFAULT_ACTION_TEXT = "action";

    static {
        NOTIFICATION_MANAGER.createNotificationChannel(
                new NotificationChannel(
                        NOTIFICATION_CHANNEL_HIGH_IMPORTANCE_ID,
                        NOTIFICATION_CHANNEL_HIGH_IMPORTANCE_NAME,
                        IMPORTANCE_HIGH));

        NOTIFICATION_MANAGER.createNotificationChannel(
                new NotificationChannel(
                        NOTIFICATION_CHANNEL_DEFAULT_IMPORTANCE_ID,
                        NOTIFICATION_CHANNEL_DEFAULT_IMPORTANCE_NAME,
                        IMPORTANCE_DEFAULT));

        NOTIFICATION_MANAGER.createNotificationChannel(
                new NotificationChannel(
                        NOTIFICATION_CHANNEL_LOW_IMPORTANCE_ID,
                        NOTIFICATION_CHANNEL_LOW_IMPORTANCE_NAME,
                        IMPORTANCE_LOW));

        NOTIFICATION_MANAGER.createNotificationChannel(
                new NotificationChannel(
                        NOTIFICATION_CHANNEL_MIN_IMPORTANCE_ID,
                        NOTIFICATION_CHANNEL_MIN_IMPORTANCE_NAME,
                        IMPORTANCE_MIN));
    }

    /** Returns an instance of NotificationController. */
    public static NotificationController get() {
        return new NotificationController();
    }

    private NotificationController() {}

    private static int getNextNotificationId() {
        return nextNotificationId++;
    }

    /**
     * Posts notification.
     *
     * @param builder Builder for notification to post.
     */
    public void postNotification(Builder builder) {
        postNotificationSync(getNextNotificationId(), builder);
    }

    /**
     * Posts notification without setting group ID.
     *
     * @param builder Builder for notification to post.
     */
    public void postNotificationNoGroup(Builder builder) {
        postNotificationSync(/* id= */ getNextNotificationId(), builder, /* groupKey= */ null);
    }

    /**
     * Posts notification.
     *
     * @param id notification id.
     * @param builder Builder for notification to post.
     */
    public void postNotification(int id, Builder builder) {
        Notification notification = builder.setGroup(getGroupKey(id)).build();
        NOTIFICATION_MANAGER.notify(id, notification);
        waitUntilNotificationPosted(id);
    }

    /**
     * Cancels a notification.
     *
     * @param id notification id.
     */
    public void cancelNotification(int id) {
        NOTIFICATION_MANAGER.cancel(id);
        waitUntilNotificationCancelled(id);
    }

    /**
     * Checks the notification has the LIFETIME_EXTENDED_BY_DIRECT_REPLY flag, then sends a
     * cancellation for given notification, and checks that the cancellation is refused because of
     * the flag, which prevents non-user originated cancellations from occurring.
     *
     * @param id notification id.
     */
    public void cancelNotificationLifetimeExtended(int id) {
        // Checks the notification has the lifetime extended flag.
        waitUntilNotificationUpdatedWithFlag(
                id, Notification.FLAG_LIFETIME_EXTENDED_BY_DIRECT_REPLY);
        // Sends the cancelation signal.
        NOTIFICATION_MANAGER.cancel(id);
        // The cancelation should be refused.
        waitForCondition(
                () -> "Notification is gone when cancelation should have been prevented",
                () -> hasNotification(id));
    }

    /**
     * Checks that a notification has been cancelled.
     *
     * @param id notification id.
     */
    public void notificationCancelled(int id) {
        waitUntilNotificationCancelled(id);
    }

    /**
     * Sends a cancellation signal; does not confirm the notification is canceled.
     *
     * @param id notification id
     */
    public void sendCancellation(int id) {
        NOTIFICATION_MANAGER.cancel(id);
    }

    /**
     * Posts a number of notifications to the device with a package to launch. Successive calls to
     * this should post new notifications in addition to those previously posted. Note that this may
     * fail if the helper has surpassed the system-defined limit for per-package notifications.
     *
     * @param count The number of notifications to post.
     * @param isMessaging If notification should be a messagingstyle notification
     */
    public void postNotifications(int count, boolean isMessaging) {
        postNotifications(count, null, isMessaging);
    }

    /**
     * Posts a number of notifications to the device. Successive calls to this should post new
     * notifications to those previously posted. Note that this may fail if the helper has surpassed
     * the system-defined limit for per-package notifications.
     *
     * @param count The number of notifications to post.
     */
    public NotificationIdentity postNotifications(int count) {
        return postNotifications(count, /* pkg */ null);
    }

    /**
     * Setup Expectations: None
     *
     * <p>Posts a number of notifications to the device with a package to launch. Successive calls
     * to this should post new notifications in addition to those previously posted. Note that this
     * may fail if the helper has surpassed the system-defined limit for per-package notifications.
     *
     * @param count The number of notifications to post.
     * @param pkg The application that will be launched by notifications.
     */
    public NotificationIdentity postNotifications(int count, String pkg) {
        postNotifications(count, pkg, /* isMessaging= */ false);
        return new NotificationIdentity(
                NotificationIdentity.Type.BY_TITLE,
                NOTIFICATION_TITLE_TEXT,
                null,
                null,
                null,
                false,
                pkg);
    }

    /**
     * Posts a notification using {@link android.app.Notification.CallStyle}.
     *
     * @param pkg App to launch, when clicking on notification.
     */
    @NonNull
    public NotificationIdentity postCallStyleNotification(@Nullable String pkg) {
        Person namedPerson = new Person.Builder().setName("Named Person").build();
        postNotificationSync(
                getNextNotificationId(),
                getBuilder(pkg)
                        .setStyle(
                                Notification.CallStyle.forOngoingCall(
                                        namedPerson, getLaunchIntent(pkg)))
                        .setFullScreenIntent(getLaunchIntent(pkg), true)
                        .setContentText(INCOMING_CALL_TEXT));
        return new NotificationIdentity(
                NotificationIdentity.Type.CALL, null, INCOMING_CALL_TEXT, null, null, true, null);
    }

    /**
     * Posts a notification using {@link android.app.Notification.InboxStyle}.
     *
     * @param pkg App to launch, when clicking on notification.
     */
    @NonNull
    public NotificationIdentity postInboxStyleNotification(
            @Nullable String pkg, @Nullable String rowText) {
        postNotificationSync(
                getNextNotificationId(),
                getBuilder(pkg)
                        .setStyle(new Notification.InboxStyle().addLine(rowText))
                        .setContentText(NOTIFICATION_CONTENT_TEXT));
        return new NotificationIdentity(
                NotificationIdentity.Type.INBOX,
                null,
                NOTIFICATION_TITLE_TEXT,
                null,
                null,
                true,
                null);
    }

    /**
     * Posts a notification using {@link android.app.Notification.MediaStyle}.
     *
     * @param pkg App to launch, when clicking on notification.
     */
    @NonNull
    public NotificationIdentity postMediaStyleNotification(
            @Nullable String pkg, boolean decorated) {
        postNotificationSync(
                getNextNotificationId(),
                getBuilder(pkg)
                        .setStyle(
                                decorated
                                        ? new Notification.DecoratedMediaCustomViewStyle()
                                        : new Notification.MediaStyle())
                        .setContentText(NOTIFICATION_CONTENT_TEXT));
        return new NotificationIdentity(
                NotificationIdentity.Type.MEDIA,
                null,
                NOTIFICATION_CONTENT_TEXT,
                null,
                null,
                true,
                null);
    }

    /**
     * Posts a notification with a custom layout.
     *
     * @param pkg App to launch, when clicking on notification.
     * @param decorated whether the custom notification should have the standard view wrapper
     */
    @NonNull
    public NotificationIdentity postCustomNotification(@Nullable String pkg, boolean decorated) {
        postNotificationSync(
                getNextNotificationId(),
                getBuilder(pkg)
                        .setCustomContentView(makeCustomContent())
                        .setStyle(decorated ? new Notification.DecoratedCustomViewStyle() : null)
                        .setContentText(CUSTOM_TEXT));
        return new NotificationIdentity(
                NotificationIdentity.Type.CUSTOM, null, CUSTOM_TEXT, null, null, true, null);
    }

    protected RemoteViews makeCustomContent() {
        RemoteViews customContent =
                new RemoteViews(PACKAGE_NAME, android.R.layout.simple_list_item_1);
        int textId = android.R.id.text1;
        customContent.setTextViewText(textId, "Example Text");
        return customContent;
    }

    /**
     * Posts a notification using {@link android.app.Notification.BigPictureStyle}.
     *
     * @param pkg App to launch, when clicking on notification.
     */
    @NonNull
    public NotificationIdentity postBigPictureNotification(@Nullable String pkg) {
        Bitmap bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        new Canvas(bitmap).drawColor(Color.BLUE);
        postNotificationSync(
                getNextNotificationId(),
                getBuilder(pkg)
                        .setStyle(new android.app.Notification.BigPictureStyle().bigPicture(bitmap))
                        .setContentText(NOTIFICATION_CONTENT_TEXT));
        return new NotificationIdentity(
                /* type= */ NotificationIdentity.Type.BIG_PICTURE,
                /* title= */ null,
                /* text= */ NOTIFICATION_TITLE_TEXT,
                /* summary= */ null,
                /* textWhenExpanded= */ null,
                /* contentIsVisibleInCollapsedState= */ true,
                /* pkg= */ null);
    }

    /**
     * Posts a notification using {@link android.app.Notification.BigPictureStyle}.
     *
     * @param pkg App to launch, when clicking on notification.
     * @param picture The picture to include as the content of the BigPicture Notification.
     */
    @NonNull
    public NotificationIdentity postBigPictureNotification(
            @Nullable String pkg, String title, @NonNull Icon picture, boolean lowImportance) {
        postNotificationSync(
                getNextNotificationId(),
                getBuilder(pkg, lowImportance ? Importance.LOW : Importance.DEFAULT)
                        .setContentTitle(title)
                        .setStyle(
                                new android.app.Notification.BigPictureStyle().bigPicture(picture))
                        .setContentText(NOTIFICATION_CONTENT_TEXT));
        return new NotificationIdentity(
                /* type= */ NotificationIdentity.Type.BIG_PICTURE,
                /* title= */ null,
                /* text= */ title,
                /* summary= */ null,
                /* textWhenExpanded= */ null,
                /* contentIsVisibleInCollapsedState= */ true,
                "Scenario");
    }

    /**
     * Posts a number of notifications while the shade is closed. Successive calls to this should
     * post new notifications in addition to those previously posted. Note that this may fail if the
     * helper has surpassed the system-defined limit for per-package notifications.
     *
     * @param count The number of notifications to post.
     * @param pkg The application that will be launched by notifications.
     * @param summary Summary text for this group notification
     */
    @NonNull
    public GroupNotificationIdentities postGroupNotifications(
            int count, @Nullable String pkg, @NonNull String summary) {
        return postGroupNotifications(count, pkg, summary, /* highImportance= */ false);
    }

    /**
     * Posts a number of notifications while the shade is closed. Successive calls to this should
     * post new notifications in addition to those previously posted. Note that this may fail if the
     * helper has surpassed the system-defined limit for per-package notifications.
     *
     * @param count The number of notifications to post.
     * @param pkg The application that will be launched by notifications.
     * @param summary Summary text for this group notification
     * @param highImportance Whether to post the notification with high importance
     */
    @NonNull
    public GroupNotificationIdentities postGroupNotifications(
            int count, @Nullable String pkg, @NonNull String summary, boolean highImportance) {
        return postGroupNotificationsImpl(count, pkg, summary, highImportance);
    }

    /**
     * Posts a number of notifications while the shade is closed with custom prioruty. Successive
     * calls to this should post new notifications in addition to those previously posted. Note that
     * this may fail if the helper has surpassed the system-defined limit for per-package
     * notifications.
     *
     * @param count The number of notifications to post.
     * @param pkg The application that will be launched by notifications.
     * @param summary Summary text for this group notification
     * @param priority The priority of the group notification
     */
    @NonNull
    public GroupNotificationIdentities postGroupNotifications(
            int count, @Nullable String pkg, @NonNull String summary, Importance priority) {
        return postGroupNotificationsImpl(count, pkg, summary, priority);
    }

    /***
     * Posts a number of MessagingStyle Notifications and group them. Note that this may fail if the
     * helper has surpassed the system-defined limit for per-package notifications.
     * @param pkg The application that will be launched by notifications.
     * @param count The number of notifications to post.
     * @param summary Summary text for this group notification
     * @param personName Name of the person
     * @param messages Message Content to be posted for each MessingStyle Notification
     */
    public NotificationIdentity postGroupNotificationWithMessagingStyle(
            String pkg,
            String summary,
            int count,
            String groupName,
            String personName,
            List<MessagingStyle.Message> messages) {
        Builder builder =
                getBuilder(pkg)
                        .setGroupAlertBehavior(android.app.Notification.GROUP_ALERT_SUMMARY)
                        .setGroup(groupName);

        for (int i = 0; i < count; i++) {
            final Person person = new Person.Builder().setName(personName + "_" + i).build();
            final MessagingStyle messagingStyle =
                    new MessagingStyle(person).setConversationTitle(NOTIFICATION_TITLE_TEXT);
            for (MessagingStyle.Message message : messages) {
                messagingStyle.addMessage(message);
            }
            builder.setStyle(messagingStyle);
            postNotificationSync(getNextNotificationId(), builder, groupName);
        }
        builder.setStyle(new android.app.Notification.InboxStyle().setSummaryText(summary))
                .setGroupSummary(true);
        postNotificationSync(getNextNotificationId(), builder, groupName);
        return new NotificationIdentity(
                NotificationIdentity.Type.GROUP,
                null,
                NOTIFICATION_TITLE_TEXT,
                summary,
                null,
                true,
                null);
    }

    /***
     * Posts a number of ConversationStyle Notifications and group them.
     * Note that this may fail if the helper has surpassed the system-defined limit
     * for per-package notifications.
     *
     * @param pkg The application that will be launched by notifications.
     * @param count The number of notifications to post.
     * @param summary Summary text for this group notification
     * @param personName Name of the person
     * @param messages Message Content to be posted for each MessingStyle Notification
     */
    public NotificationIdentity postGroupNotificationWithConversationStyle(
            String pkg,
            String summary,
            int count,
            String groupName,
            String personName,
            List<MessagingStyle.Message> messages) {
        Context context = getInstrumentation().getTargetContext();
        Bitmap bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        new Canvas(bitmap).drawColor(Color.BLUE);
        Intent intent = new android.content.Intent(Intent.ACTION_VIEW);

        Builder builder =
                getBuilder(pkg)
                        .setGroupAlertBehavior(android.app.Notification.GROUP_ALERT_SUMMARY)
                        .setGroup(groupName);
        final ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);

        for (int i = 0; i < count; i++) {
            final Person person = new Person.Builder().setName(personName + "_" + i).build();

            String shortCutId = "short_cut" + i;
            ShortcutInfo shortcutInfo =
                    new ShortcutInfo.Builder(context, shortCutId)
                            .setShortLabel(personName)
                            .setLongLabel(personName)
                            .setIntent(intent)
                            .setIcon(Icon.createWithAdaptiveBitmap(bitmap))
                            .setPerson(person)
                            .setLongLived(true)
                            .build();
            shortcutManager.pushDynamicShortcut(shortcutInfo);
            final MessagingStyle messagingStyle =
                    new MessagingStyle(person).setConversationTitle(NOTIFICATION_TITLE_TEXT);
            for (MessagingStyle.Message message : messages) {
                messagingStyle.addMessage(message);
            }
            builder.setStyle(messagingStyle).setShortcutId(shortCutId);
            postNotificationSync(getNextNotificationId(), builder, groupName);
        }
        builder.setStyle(new android.app.Notification.InboxStyle().setSummaryText(summary))
                .setGroupSummary(true);
        postNotificationSync(getNextNotificationId(), builder, groupName);
        return new NotificationIdentity(
                /* type= */ NotificationIdentity.Type.GROUP,
                /* title= */ null,
                /* text= */ NOTIFICATION_TITLE_TEXT,
                /* summary= */ summary,
                /* textWhenExpanded= */ null,
                /* contentIsVisibleInCollapsedState= */ true,
                /* pkg= */ null);
    }

    /***
     * Posts a number of BigTextStyle Notifications and group them. Note that this may fail if the
     * helper has surpassed the system-defined limit for per-package notifications.
     * @param pkg The application that will be launched by notifications.
     * @param summary Summary text for this group notification
     * @param groupName Name of the group
     * @param bigTextContents List of Text to be mapped BigTextStyle Notifications
     */
    public NotificationIdentity postGroupNotificationWithBigTextStyle(
            String pkg, String summary, String groupName, List<String> bigTextContents) {
        Builder builder =
                getBuilder(pkg).setGroupAlertBehavior(android.app.Notification.GROUP_ALERT_SUMMARY);

        final Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        for (String bigText : bigTextContents) {
            bigTextStyle.setBigContentTitle(bigText);
            builder.setStyle(bigTextStyle).setGroup(groupName);
            postNotificationSync(getNextNotificationId(), builder, groupName);
        }
        builder.setStyle(new android.app.Notification.InboxStyle().setSummaryText(summary))
                .setGroupSummary(true);
        postNotificationSync(getNextNotificationId(), builder, groupName);
        return new NotificationIdentity(
                NotificationIdentity.Type.GROUP,
                null,
                NOTIFICATION_TITLE_TEXT,
                summary,
                null,
                true,
                null);
    }

    /**
     * Posts a notification using {@link android.app.Notification.BigTextStyle}.
     *
     * @param pkg App to launch, when clicking on notification.
     */
    @NonNull
    public NotificationIdentity postBigTextNotification(@Nullable String pkg) {
        return postBigTextNotification(pkg, false);
    }

    /**
     * Posts a notification using {@link android.app.Notification.BigTextStyle}.
     *
     * @param pkg App to launch, when clicking on notification.
     * @param highImportance Whether to post the notification with high importance.
     */
    @NonNull
    public NotificationIdentity postBigTextNotification(
            @Nullable String pkg, boolean highImportance) {
        return BigTextNotificationController.postBigTextNotification(
                /* pkg= */ pkg, /* highImportance= */ highImportance);
    }

    /**
     * Posts a notification using {@link android.app.Notification.BigTextStyle}.
     *
     * @param pkg App to launch, when clicking on notification.
     * @param title title of a notification
     * @param collapsedText collapsed text of a notification
     * @param expandedText expanded text of a notification
     */
    @NonNull
    public NotificationIdentity postBigTextNotification(
            @Nullable String pkg,
            boolean highImportance,
            String title,
            String collapsedText,
            String expandedText) {
        return BigTextNotificationController.postBigTextNotification(
                /* pkg= */ pkg,
                /* highImportance= */ highImportance,
                /* collapsedText= */ collapsedText,
                /* expandedText= */ expandedText,
                /* title= */ title);
    }

    /**
     * Posts a heads-up notification using {@link android.app.Notification.BigTextStyle} with a
     * default action button. The action button is useful to distinguish if the notification is in
     * the HUN form (We can tell a notification is in the HUN form if its expand button is at the
     * "expand" state, and an action button is showing).
     *
     * @param pkg App to launch, when clicking on notification.
     */
    public NotificationIdentity postBigTextHeadsUpNotification(@Nullable String pkg) {
        return BigTextNotificationController.postBigTextNotification(
                /* pkg= */ pkg,
                /* highImportance= */ true,
                /* actions= */ getDefaultActionBuilder().build());
    }

    public Notification.Action.Builder getDefaultActionBuilder() {
        return new Notification.Action.Builder(
                Icon.createWithResource("", R.drawable.btn_star),
                DEFAULT_ACTION_TEXT,
                PendingIntent.getActivity(
                        getContext(),
                        0,
                        new android.content.Intent(Intent.ACTION_VIEW),
                        PendingIntent.FLAG_IMMUTABLE));
    }

    /**
     * Posts a Full Screen Intent Notification.
     *
     * @param pkg App to launch, when clicking on notification.
     * @param fsiPendingIntent Full Screen Intent
     * @param actions actions Action to be shown in the Notification
     */
    public NotificationIdentity postFullScreenIntentNotification(
            @Nullable final String pkg,
            final PendingIntent fsiPendingIntent,
            final Notification.Action... actions) {
        postNotificationSync(
                getNextNotificationId(),
                getBuilder(pkg, Importance.HIGH)
                        .setSmallIcon(android.R.drawable.stat_notify_chat)
                        .setContentText(NOTIFICATION_CONTENT_TEXT)
                        .setFullScreenIntent(fsiPendingIntent, true)
                        .setActions(actions));
        return new NotificationIdentity(
                /* title= */ NotificationIdentity.Type.BY_TITLE,
                /* type= */ NOTIFICATION_TITLE_TEXT,
                /* text= */ null,
                /* summary= */ null,
                /* textWhenExpanded= */ null,
                /* contentIsVisibleInCollapsedState= */ true,
                /* pkg= */ pkg);
    }

    /**
     * Posts an ongoing Notification.
     *
     * @param pkg App to launch, when clicking on notification.
     */
    public NotificationIdentity postOngoingNotification(@Nullable final String pkg) {
        postNotificationSync(
                getNextNotificationId(),
                getBuilder(pkg, Importance.HIGH)
                        .setContentText(NOTIFICATION_CONTENT_TEXT)
                        .setOngoing(true));
        return new NotificationIdentity(
                /* title= */ NotificationIdentity.Type.BY_TITLE,
                /* type= */ NOTIFICATION_TITLE_TEXT,
                /* text= */ null,
                /* summary= */ null,
                /* textWhenExpanded= */ null,
                /* contentIsVisibleInCollapsedState= */ true,
                /* pkg= */ pkg);
    }

    private static GroupNotificationIdentities postGroupNotificationsImpl(
            int count, @Nullable String pkg, @NonNull String summary, boolean highImportance) {
        return postGroupNotificationsImpl(
                count, pkg, summary, highImportance ? Importance.HIGH : Importance.DEFAULT);
    }

    private static GroupNotificationIdentities postGroupNotificationsImpl(
            int count, @Nullable String pkg, @NonNull String summary, Importance priority) {
        GroupNotificationIdentities identities = new GroupNotificationIdentities();
        Builder builder =
                getBuilder(pkg, priority)
                        .setGroupAlertBehavior(android.app.Notification.GROUP_ALERT_SUMMARY);

        for (int i = 0; i < count; i++) {
            final String childText = String.format(Locale.US, NOTIFICATION_CONTENT_TEXT_FORMAT, i);
            builder.setContentText(childText);
            postNotificationSync(getNextNotificationId(), builder, NOTIFICATION_GROUP);
            identities.children.add(
                    new NotificationIdentity(
                            /* type= */ NotificationIdentity.Type.BY_TEXT,
                            /* title= */ null,
                            /* text= */ childText,
                            /* summary= */ null,
                            /* textWhenExpanded= */ null,
                            /* contentIsVisibleInCollapsedState= */ true,
                            /* pkg= */ pkg));
        }

        builder.setStyle(new android.app.Notification.InboxStyle().setSummaryText(summary))
                .setGroupSummary(true);
        postNotificationSync(getNextNotificationId(), builder, NOTIFICATION_GROUP);
        identities.summary =
                new NotificationIdentity(
                        /* type= */ priority == Importance.MIN
                                ? NotificationIdentity.Type.GROUP_MINIMIZED
                                : NotificationIdentity.Type.GROUP,
                        /* title= */ NOTIFICATION_TITLE_TEXT,
                        /* text= */ NOTIFICATION_TITLE_TEXT,
                        /* summary= */ summary,
                        /* textWhenExpanded= */ null,
                        /* contentIsVisibleInCollapsedState= */ true,
                        /* pkg= */ pkg);

        return identities;
    }

    /**
     * Posts Standard Silent Notification
     *
     * @param pkg
     */
    public NotificationIdentity postStandardSilentNotification(String pkg) {
        postNotificationSync(
                getNextNotificationId(),
                getBuilder(pkg, Importance.LOW).setContentText(NOTIFICATION_CONTENT_TEXT));
        return new NotificationIdentity(
                /* title= */ NotificationIdentity.Type.BY_TITLE,
                /* type= */ NOTIFICATION_TITLE_TEXT,
                /* text= */ null,
                /* summary= */ null,
                /* textWhenExpanded= */ null,
                /* contentIsVisibleInCollapsedState= */ true,
                /* pkg= */ pkg);
    }

    /**
     * Posts a Standard Notification.
     *
     * @param pkg App to launch, when clicking on notification.
     */
    public NotificationIdentity postStandardStyleNotification(String pkg) {
        postNotificationSync(getNextNotificationId(), getBuilder(pkg));

        return new NotificationIdentity(
                /* type= */ NotificationIdentity.Type.BY_TITLE,
                /* title= */ NOTIFICATION_TITLE_TEXT,
                /* text= */ null,
                /* summary= */ null,
                /* textWhenExpanded= */ null,
                /* contentIsVisibleInCollapsedState= */ false,
                /* pkg= */ pkg);
    }

    /**
     * Posts a Standard Notification.
     *
     * @param pkg App to launch, when clicking on notification.
     * @param title title of the notification
     * @param content content of the notification
     */
    public NotificationIdentity postStandardStyleNotification(
            String pkg, String title, String content) {
        postNotificationSync(
                getNextNotificationId(),
                getBuilder(pkg).setContentTitle(title).setContentText(content));

        return new NotificationIdentity(
                /* type= */ NotificationIdentity.Type.BY_TITLE,
                /* title= */ title,
                /* text= */ null,
                /* summary= */ null,
                /* textWhenExpanded= */ null,
                /* contentIsVisibleInCollapsedState= */ false,
                /* pkg= */ pkg);
    }

    /**
     * Posts a notification using {@link android.app.Notification.MessagingStyle}.
     *
     * @param pkg App to launch, when clicking on notification.
     */
    public NotificationIdentity postMessagingStyleNotification(String pkg) {
        String personName = "Person Name";
        android.app.Person person = new android.app.Person.Builder().setName(personName).build();
        postNotificationSync(
                getNextNotificationId(),
                getBuilder(pkg)
                        .setStyle(
                                new android.app.Notification.MessagingStyle(person)
                                        .setConversationTitle(NOTIFICATION_TITLE_TEXT)
                                        .addMessage(
                                                new android.app.Notification.MessagingStyle.Message(
                                                        "Message 4",
                                                        SystemClock.currentThreadTimeMillis(),
                                                        person))
                                        .addMessage(
                                                new android.app.Notification.MessagingStyle.Message(
                                                        "Message 3",
                                                        SystemClock.currentThreadTimeMillis(),
                                                        person))
                                        .addMessage(
                                                new android.app.Notification.MessagingStyle.Message(
                                                        "Message 2",
                                                        SystemClock.currentThreadTimeMillis(),
                                                        person))
                                        .addMessage(
                                                new android.app.Notification.MessagingStyle.Message(
                                                        "Message 1",
                                                        SystemClock.currentThreadTimeMillis(),
                                                        person))));
        return new NotificationIdentity(
                NotificationIdentity.Type.MESSAGING_STYLE,
                null,
                personName,
                null,
                null,
                false,
                null);
    }

    /**
     * Posts a notification using {@link android.app.Notification.MessagingStyle}.
     *
     * @param pkg App to launch, when clicking on notification.
     * @param personName name of the person who sends message
     * @param messages message list.
     */
    public NotificationIdentity postMessagingStyleNotification(
            String pkg, String personName, List<MessagingStyle.Message> messages) {
        final Person person = new Person.Builder().setName(personName).build();
        final MessagingStyle messagingStyle =
                new MessagingStyle(person).setConversationTitle(NOTIFICATION_TITLE_TEXT);
        for (MessagingStyle.Message message : messages) {
            messagingStyle.addMessage(message);
        }
        postNotificationSync(getNextNotificationId(), getBuilder(pkg).setStyle(messagingStyle));
        return new NotificationIdentity(
                /* type= */ NotificationIdentity.Type.MESSAGING_STYLE,
                /* title= */ null,
                /* text= */ personName,
                /* summary= */ null,
                /* textWhenExpanded= */ null,
                /* contentIsVisibleInCollapsedState= */ false,
                /* pkg= */ null);
    }

    /**
     * Posts a conversation notification. This notification is associated with a conversation
     * shortcut and in {@link android.app.Notification.MessagingStyle}.
     *
     * @param pkg App to launch, when clicking on notification.
     */
    public NotificationIdentity postConversationNotification(String pkg) {
        return postConversationNotification(pkg, "test_shortcut_id", "Person Name");
    }

    /**
     * Posts a sensitive big text style notification.
     *
     * @param pkg App to launch, when clicking on notification.
     */
    public NotificationIdentity postBigTextNotificationWithPublicVersion(String pkg) {
        return BigTextNotificationController.postBigTextNotification(
                /* pkg= */ pkg,
                /* highImportance= */ false,
                /* collapsedText= */ NOTIFICATION_CONTENT_TEXT,
                /* expandedText= */ NOTIFICATION_BIG_TEXT,
                /* title: String = */ NOTIFICATION_TITLE_TEXT,
                /* contentIntent= */ null,
                /* publicVersion= */ getBuilder(pkg).build());
    }

    /**
     * Posts a conversation notification. This notification is associated with a conversation
     * shortcut and in {@link android.app.Notification.MessagingStyle}.
     *
     * @param pkg App to launch, when clicking on notification.
     * @param shortcutId The shortcut ID of the associated conversation.
     * @param personName The name of the person of the associated conversation.
     */
    public NotificationIdentity postConversationNotification(
            String pkg, String shortcutId, String personName) {
        Context context = getInstrumentation().getTargetContext();
        Person person = new Person.Builder().setName(personName).build();
        long currentTimeMillis = SystemClock.currentThreadTimeMillis();
        Bitmap bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        new Canvas(bitmap).drawColor(Color.BLUE);
        Intent intent = new android.content.Intent(Intent.ACTION_VIEW);

        ShortcutInfo shortcutInfo =
                new ShortcutInfo.Builder(context, shortcutId)
                        .setShortLabel(personName)
                        .setLongLabel(personName)
                        .setIntent(intent)
                        .setIcon(Icon.createWithAdaptiveBitmap(bitmap))
                        .setPerson(person)
                        .setLongLived(true)
                        .build();
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        shortcutManager.pushDynamicShortcut(shortcutInfo);

        Builder builder =
                getBuilder(pkg)
                        .setStyle(
                                new MessagingStyle(person)
                                        .addMessage(
                                                new MessagingStyle.Message(
                                                        "Message " + personName,
                                                        currentTimeMillis,
                                                        person)))
                        .setShortcutId(shortcutId);

        postNotificationSync(getNextNotificationId(), builder);

        return new NotificationIdentity(
                NotificationIdentity.Type.CONVERSATION, null, personName, null, null, false, null);
    }

    /**
     * Posts a conversation notification. This notification is associated with a conversation
     * shortcut and in {@link android.app.Notification.MessagingStyle}.
     *
     * @param pkg App to launch, when clicking on notification.
     * @param shortcutId The shortcut ID of the associated conversation.
     * @param personName The name of the person of the associated conversation.
     * @param messages messages of the conversation
     */
    public NotificationIdentity postConversationNotification(
            String pkg,
            String shortcutId,
            String personName,
            List<MessagingStyle.Message> messages) {
        final Context context = getInstrumentation().getTargetContext();
        final Person person = new Person.Builder().setName(personName).build();
        final Bitmap bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        new Canvas(bitmap).drawColor(Color.BLUE);
        final Intent intent = new android.content.Intent(Intent.ACTION_VIEW);

        final ShortcutInfo shortcutInfo =
                new ShortcutInfo.Builder(context, shortcutId)
                        .setShortLabel(personName)
                        .setLongLabel(personName)
                        .setIntent(intent)
                        .setIcon(Icon.createWithAdaptiveBitmap(bitmap))
                        .setPerson(person)
                        .setLongLived(true)
                        .build();
        final ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        shortcutManager.pushDynamicShortcut(shortcutInfo);

        final MessagingStyle messagingStyle = new MessagingStyle(person);
        for (MessagingStyle.Message message : messages) {
            messagingStyle.addMessage(message);
        }

        final Builder builder = getBuilder(pkg).setStyle(messagingStyle).setShortcutId(shortcutId);

        postNotificationSync(getNextNotificationId(), builder);

        return new NotificationIdentity(
                /* type= */ NotificationIdentity.Type.CONVERSATION,
                /* title= */ null,
                /* text= */ personName,
                /* summary= */ null,
                /* textWhenExpanded= */ null,
                /* contentIsVisibleInCollapsedState= */ false,
                /* pkg= */ null);
    }

    private Builder createBubbleNotificationPostBuilder(
            String senderName, String text, String shortcutId, String messageToActivity) {
        final String pkg = getInstrumentation().getTargetContext().getPackageName();

        Context context = getInstrumentation().getTargetContext();
        Person person = new Person.Builder().setName(senderName).build();
        long currentTimeMillis = SystemClock.currentThreadTimeMillis();
        Bitmap bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        new Canvas(bitmap).drawColor(Color.BLUE);
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        if (messageToActivity != null) {
            intent.putExtra(EXTRA_NAME_MESSAGE, messageToActivity);
        }

        ShortcutInfo shortcutInfo =
                new ShortcutInfo.Builder(context, shortcutId)
                        .setShortLabel(senderName)
                        .setLongLabel(senderName)
                        .setIntent(intent)
                        .setIcon(Icon.createWithAdaptiveBitmap(bitmap))
                        .setPerson(person)
                        .setLongLived(true)
                        .build();
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        shortcutManager.pushDynamicShortcut(shortcutInfo);

        Notification.BubbleMetadata bubbleMetadata =
                new Notification.BubbleMetadata.Builder(shortcutInfo.getId())
                        .setAutoExpandBubble(false /* autoExpand */)
                        .setSuppressNotification(false /* suppressNotif */)
                        .build();

        return getBuilder(pkg)
                .setStyle(
                        new MessagingStyle(person)
                                .addMessage(
                                        new MessagingStyle.Message(
                                                text, currentTimeMillis, person)))
                .setShortcutId(shortcutId)
                .setBubbleMetadata(bubbleMetadata);
    }

    /**
     * Posts multiple bubble notifications.
     *
     * @param senderName Name of notification sender.
     * @param count How many bubble notifications to send.
     */
    @NonNull
    public NotificationIdentity postBubbleNotifications(String senderName, int count) {
        final Builder builder =
                createBubbleNotificationPostBuilder(
                        senderName, "Bubble message", DEFAULT_TEST_SHORTCUT_ID, null);

        for (int i = 0; i < count; i++) {
            postNotificationSync(getNextNotificationId(), builder);
        }

        return new NotificationIdentity(
                NotificationIdentity.Type.CONVERSATION,
                null,
                "Bubble message",
                null,
                null,
                false,
                null);
    }

    /**
     * Posts a bubble notification.
     *
     * @param id An identifier of the notification to be posted.
     * @param senderName Name of notification sender.
     * @param text Notification message content.
     * @param shortcutId id of the shortcut used in the notification.
     * @param messageToActivity message to send to bubble test activity.
     */
    public void postBubbleNotification(
            int id, String senderName, String text, String shortcutId, String messageToActivity) {
        Builder builder =
                createBubbleNotificationPostBuilder(
                        senderName, text, shortcutId, messageToActivity);

        postNotificationSync(id, builder);
    }

    /**
     * Posts a bubble notification.
     *
     * @param id An identifier of the notification to be posted.
     * @param senderName Name of notification sender.
     * @param text Notification message content.
     */
    public void postBubbleNotification(int id, String senderName, String text) {
        postBubbleNotification(
                id, senderName, text, DEFAULT_TEST_SHORTCUT_ID, /* messageToActivity= */ null);
    }

    /**
     * Updates an existing bubble notification.
     *
     * @param id An identifier of the notification to be updated.
     * @param senderName Name of notification sender.
     * @param text Update message content.
     */
    public void updateBubbleNotification(int id, String senderName, String text) {
        Person person = new Person.Builder().setName(senderName).build();
        long currentTimeMillis = SystemClock.currentThreadTimeMillis();

        Notification.BubbleMetadata bubbleMetadata =
                new Notification.BubbleMetadata.Builder(DEFAULT_TEST_SHORTCUT_ID)
                        .setAutoExpandBubble(false /* autoExpand */)
                        .setSuppressNotification(false /* suppressNotif */)
                        .build();

        Builder builder =
                getBuilder(PACKAGE_NAME)
                        .setStyle(
                                new Notification.MessagingStyle(person)
                                        .addMessage(
                                                new MessagingStyle.Message(
                                                        text, currentTimeMillis, person)))
                        .setShortcutId(DEFAULT_TEST_SHORTCUT_ID)
                        .setBubbleMetadata(bubbleMetadata);

        NOTIFICATION_MANAGER.notify(id, builder.build());
    }

    private static void postNotificationSync(int id, Builder builder) {
        // By default, we add a group key with the same id as the notification so that it is not
        // grouped with other notifications, making sure that the notification count is incremented
        // only by 1 when we already posted another notifications, and not by 2 which will happen
        // if a new group is formed (as a group also counts as 1 notification). This avoids race
        // conditions when adding a lot of consecutive notifications.
        postNotificationSync(id, builder, getGroupKey(id));
    }

    private static String getGroupKey(int id) {
        return String.format(NOTIFICATION_GROUP_KEY_FORMAT, id);
    }

    private static void postNotificationSync(int id, Builder builder, String groupKey) {
        final int initialCount = getNotificationCount();
        final Notification notification = builder.setGroup(groupKey).build();
        NOTIFICATION_MANAGER.notify(id, notification);
        waitUntilPostedNotificationsCountMatches(initialCount + 1);
    }

    /**
     * Returns the current total number of posted notifications. If you've just posted a
     * notification via NOTIFICATION_MANAGER.notify, this count isn't guaranteed to be correct
     * unless you've waited for it to arrive. If the notification is posted by postNotificationSync,
     * the count will be correct after posting. Use only postNotificationSync to post notifications.
     */
    private static int getNotificationCount() {
        return NOTIFICATION_MANAGER.getActiveNotifications().length;
    }

    private static boolean hasNotification(int id) {
        StatusBarNotification[] activeNotifications = NOTIFICATION_MANAGER.getActiveNotifications();
        for (StatusBarNotification notification : activeNotifications) {
            if (notification.getId() == id) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNotificationWithFlag(int id, int flag) {
        StatusBarNotification[] activeNotifications = NOTIFICATION_MANAGER.getActiveNotifications();
        for (StatusBarNotification notification : activeNotifications) {
            if (notification.getId() == id && (notification.getNotification().flags & flag) != 0) {
                return true;
            }
        }
        return false;
    }

    private static void waitUntilNotificationPosted(int id) {
        waitForCondition(() -> "Notification was not posted.", () -> hasNotification(id));
    }

    private static void waitUntilNotificationCancelled(int id) {
        waitForCondition(() -> "Notification is still present.", () -> !hasNotification(id));
    }

    private static void waitUntilNotificationUpdatedWithFlag(int id, int flag) {
        waitForCondition(
                () -> "Notification was not posted with flag.",
                () -> (hasNotificationWithFlag(id, flag)));
    }

    private static void waitUntilPostedNotificationsCountMatches(int count) {
        waitForCondition(
                () ->
                        "Notification count didn't become "
                                + count
                                + ". It is currently equal to "
                                + getNotificationCount(),
                () -> getNotificationCount() == count);
    }

    private static Builder getBuilder(String pkg) {
        return getBuilder(pkg, Importance.DEFAULT);
    }

    private static Builder getBuilder(String pkg, Importance importance) {
        Context context = getInstrumentation().getTargetContext();

        final String channelId =
                switch (importance) {
                    case HIGH -> NOTIFICATION_CHANNEL_HIGH_IMPORTANCE_ID;
                    case DEFAULT -> NOTIFICATION_CHANNEL_DEFAULT_IMPORTANCE_ID;
                    case LOW -> NOTIFICATION_CHANNEL_LOW_IMPORTANCE_ID;
                    case MIN -> NOTIFICATION_CHANNEL_MIN_IMPORTANCE_ID;
                };
        Builder builder =
                new Builder(context, channelId)
                        .setContentTitle(NOTIFICATION_TITLE_TEXT)
                        .setCategory(CATEGORY_SYSTEM)
                        .setGroupSummary(false)
                        .setContentText(NOTIFICATION_CONTENT_TEXT)
                        .setShowWhen(false)
                        .setSmallIcon(android.R.drawable.stat_notify_chat);
        if (pkg != null) {
            builder.setContentIntent(getLaunchIntent(pkg));
        }
        return builder;
    }

    private static PendingIntent getLaunchIntent(String pkg) {
        Context context = getInstrumentation().getTargetContext();
        return PendingIntent.getActivity(
                context,
                0,
                context.getPackageManager().getLaunchIntentForPackage(pkg),
                Intent.FLAG_ACTIVITY_NEW_TASK | FLAG_IMMUTABLE);
    }

    private static void postNotifications(int count, String pkg, boolean isMessaging) {
        Builder builder = getBuilder(pkg);
        if (isMessaging) {
            Person person = new Person.Builder().setName("Marvelous user").build();
            builder.setStyle(
                    new MessagingStyle(person)
                            .addMessage(
                                    new MessagingStyle.Message(
                                            "Hello",
                                            SystemClock.currentThreadTimeMillis(),
                                            person)));
        }

        for (int i = (count - 1); i >= 0; i--) {
            builder.setContentText(String.format(NOTIFICATION_CONTENT_TEXT_FORMAT, i));
            postNotificationSync(getNextNotificationId(), builder);
        }
    }

    /** Cancels all notifications posted by this object. */
    public void cancelNotifications() {
        NOTIFICATION_MANAGER.cancelAll();
        waitUntilPostedNotificationsCountMatches(0);
    }

    /**
     * Set or reset avalanche suppression.
     *
     * @param disabledForTest If true, disable; otherwise reset to default.
     */
    public void setCooldownSettingDisabled(boolean disabledForTest) {
        StringBuilder sb = new StringBuilder();
        StringBuilder command = new StringBuilder("");
        if (disabledForTest) {
            command.append("settings put system notification_cooldown_enabled 0");
        } else {
            command.append("settings reset system notification_cooldown_enabled");
        }
        runCommandAndCollectResult("set avalanche suppression", command.toString(), sb);
        Log.d(LOG_TAG, sb.toString());
    }

    private static String runCommandAndCollectResult(
            String description, String cmd, StringBuilder sb) {
        if (cmd == null || sb == null) {
            return null;
        }
        String result = null;
        try {
            Log.d(LOG_TAG, "Before command: " + cmd);
            result = getUiDevice().executeShellCommand(cmd);
            String msg = String.format("%s command: %s\nResult: %s\n", description, cmd, result);
            Log.d(LOG_TAG, msg);
            sb.append(msg);
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Failed to run command: " + cmd, ioe);
        }
        return result;
    }

    /**
     * Set up or clear the debug filter; restricting notifications to the provided packages, or
     * resetting if none are provided.
     *
     * @param allowedPackages package names allowed to show notifications
     */
    private void setDebugNotificationFilter(@Nullable List<String> allowedPackages) {
        StringBuilder sb = new StringBuilder();
        StringBuilder command = new StringBuilder("cmd statusbar notif-filter ");
        if (allowedPackages == null || allowedPackages.isEmpty()) {
            command.append("reset");
        } else {
            command.append("allowed-pkgs");
            for (String pkg : allowedPackages) {
                command.append(" ");
                command.append(pkg);
            }
        }
        runCommandAndCollectResult("set debug filter", command.toString(), sb);
        Log.d(LOG_TAG, sb.toString());
    }

    /**
     * Set up or clear the debug filter; restricting notifications to the test package, or resetting
     * if false is provided.
     *
     * @param enabled whether to enable the debug filter
     */
    public void setDebugNotificationFilter(boolean enabled) {
        setDebugNotificationFilter(enabled ? List.of(PACKAGE_NAME) : null);
    }

    /**
     * Holds the identities of a group summary and children as posted by {@link
     * NotificationController#postGroupNotifications(int, String, String, boolean)}.
     */
    public static class GroupNotificationIdentities {
        public NotificationIdentity summary = null;
        public List<NotificationIdentity> children = new ArrayList<NotificationIdentity>();
    }

    /**
     * The importance of the Notification to be posted.
     *
     * @see NotificationChannel#setImportance(int)
     */
    public enum Importance {
        HIGH,
        DEFAULT,
        LOW,
        MIN
    }
}
