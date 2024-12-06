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

import static android.content.Context.STATUS_BAR_SERVICE;
import static android.platform.uiautomator_helpers.DeviceHelpers.getContext;

import android.app.INotificationManager;
import android.app.NotificationManager;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.platform.systemui_tapl.ui.Root;
import android.system.Os;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

/** Controller for manipulating bubble settings. */
public class BubbleSettingsController {
    private static final int UID = Os.getuid();
    private static final String PACKAGE_NAME = getContext().getPackageName();
    private static final String TAG = "BubbleSettingsController";

    private final INotificationManager mINotificationManager =
            INotificationManager.Stub.asInterface(
                    ServiceManager.getService(Context.NOTIFICATION_SERVICE));

    private BubbleSettingsController() {}

    /** Returns an instance of BubbleSettingsController. */
    public static BubbleSettingsController get() {
        return new BubbleSettingsController();
    }

    private void setBubblesAllowed(int bubblePreference) {
        Log.d(TAG, "setBubblesAllowed(" + bubblePreference + ") for UID " + UID);
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .adoptShellPermissionIdentity(STATUS_BAR_SERVICE);
        try {
            mINotificationManager.setBubblesAllowed(PACKAGE_NAME, UID, bubblePreference);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }
    }

    /** Allows all bubbles. */
    public void enableBubbles() {
        setBubblesAllowed(NotificationManager.BUBBLE_PREFERENCE_ALL);
    }

    /** Disallows all bubbles. */
    public void disableBubbles() {
        setBubblesAllowed(NotificationManager.BUBBLE_PREFERENCE_NONE);
        // Ensure any existing bubble is removed
        Root.get().verifyNoBubbleIsVisible();
    }
}
