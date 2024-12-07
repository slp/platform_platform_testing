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

import static android.platform.helpers.CommonUtils.executeShellCommand;
import static android.platform.test.util.HealthTestingUtils.waitForCondition;
import static android.platform.uiautomator_helpers.DeviceHelpers.getContext;
import static android.platform.uiautomator_helpers.DeviceHelpers.getUiDevice;
import static android.platform.uiautomator_helpers.DeviceHelpers.shell;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.app.ActivityManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.NewUserRequest;
import android.os.NewUserResponse;
import android.os.UserManager;
import android.platform.helpers.CommonUtils;
import android.platform.systemui_tapl.utils.UserUtils;
import android.system.helpers.UserHelper;

import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/** Controller for manipulating users. */
public class UserController {

    // TODO(b/264023316): Reduce once fixed.
    private static final int VERIFICATION_TIME_IN_SECONDS = 90;
    private static final String USER_SWITCHER_VISIBLE_FLAG_CMD = "cmd statusbar flag 204";

    /** Returns an instance of UserController. */
    public static UserController get() {
        return new UserController();
    }

    private UserController() {}

    /** Switch to given user directly, not via any switcher UI. */
    public void switchToUser(int userId) {
        // TODO(b/192001071): Consider just returning getActivityManager().switchUser(user.id);
        //                    instead. Avoids shell, but then you lose the advantages of "-w".
        if (getCurrentUserId() == userId) return;
        // Go to user, waiting until action is completed.
        UserUtils.runThenWaitUntilSwitchCompleted(
                () ->
                        executeShellCommand(
                                String.format(Locale.US, "am switch-user -w %d", userId)));
        waitForCondition(
                () -> "Current user didn't become " + userId,
                () -> CommonUtils.getCurrentUserId() == userId);
    }

    /** Returns current user ID. UserID = 0 is for System User. */
    public int getCurrentUserId() {
        return CommonUtils.getCurrentUserId();
    }

    /**
     * Returns the main user ID. NOTE: For headless system it is NOT 0. Returns 0 by default, if
     * there is no main user.
     */
    public int getMainUserId() {
        return CommonUtils.getMainUserId();
    }

    private static UserManager getUserManager() {
        return UserHelper.getInstance().getUserManager();
    }

    /** Creates a user with specified name and returns its id. */
    public int createSecondaryUser(String userName) {
        Optional<Integer> userId = tryCreatingSecondaryUser(userName);
        assertWithMessage("Failed user creation").that(userId.isPresent()).isTrue();
        return userId.get();
    }

    /**
     * Create a secondary user with specific icon color.
     *
     * @see #createSecondaryUser(String)
     */
    public int createSecondaryUser(String userName, int iconColor) {
        Optional<Integer> userId =
                tryCreatingSecondaryUserImpl(userName, defaultUserIcon(iconColor));
        assertWithMessage("Failed to create user with icon color.")
                .that(userId.isPresent())
                .isTrue();
        return userId.get();
    }

    /**
     * Try to create a secondary user. This function is mainly for testing corner cases, which
     * expects the user creation will fail.
     */
    public Optional<Integer> tryCreatingSecondaryUser(String userName) {
        return tryCreatingSecondaryUserImpl(
                userName,
                /* userIcon= */
                null);
    }

    private Optional<Integer> tryCreatingSecondaryUserImpl(
            String userName, @Nullable Bitmap userIcon) {
        NewUserRequest request =
                new NewUserRequest.Builder().setName(userName).setUserIcon(userIcon).build();
        final NewUserResponse resp = getUserManager().createUser(request);
        if (!resp.isSuccessful()) {
            return Optional.empty();
        }
        return Optional.of(resp.getUser().getIdentifier());
    }

    private Bitmap defaultUserIcon(int color) {
        Resources resources = getContext().getResources();
        int iconSize = resources.getDimensionPixelSize(com.android.internal.R.dimen.user_icon_size);
        Drawable iconDrawable =
                getContext().getDrawable(com.android.internal.R.drawable.ic_account_circle);
        iconDrawable.setColorFilter(color, Mode.MULTIPLY);
        Bitmap iconBitmap = Bitmap.createBitmap(iconSize, iconSize, Config.ARGB_8888);
        Canvas canvas = new Canvas(iconBitmap);
        iconDrawable.setBounds(new Rect(0, 0, iconSize, iconSize));
        iconDrawable.draw(canvas);
        return iconBitmap;
    }

    /** Returns all users except for the primary and the system user. */
    public Collection<Integer> getSecondaryUsers() {
        final HashSet<Integer> secondaryUsers = new HashSet<>();
        List<UserInfo> userInfoList = getUserManager().getAliveUsers();
        for (UserInfo userInfo : userInfoList) {
            // Remove all users except for the primary / system user
            if (!userInfo.isPrimary()) {
                secondaryUsers.add(userInfo.id);
            }
        }
        return secondaryUsers;
    }

    private static boolean hasGuest() {
        return getUserManager().findCurrentGuestUser() != null;
    }

    /** Creates a guest user. */
    public void createGuest() {
        try {
            assertThat(hasGuest()).isFalse();
            CountDownLatch countDownLatch = new CountDownLatch(1);
            new Thread(
                            () -> {
                                getUserManager().createGuest(getContext());
                                countDownLatch.countDown();
                            })
                    .start();
            countDownLatch.await();
        } catch (InterruptedException exception) {
            throw new RuntimeException("Create guest failed.", exception);
        }
    }

    private static ActivityManager getActivityManager() {
        return getContext().getSystemService(ActivityManager.class);
    }

    /** Stop the provided user. */
    public void stopUser(int userId) {
        executeShellCommand(String.format(Locale.US, "am stop-user -w %d", userId));

        assertWithMessage("Failed to stop userId=" + userId)
                .that(getActivityManager().isUserRunning(userId))
                .isFalse();
    }

    /** Returns whether the given user is actively running. */
    public boolean isUserRunning(int userId) {
        return getActivityManager().isUserRunning(userId);
    }

    /** Returns whether the project supports user switcher chip. */
    public boolean isUserSwitcherChipSupported() {
        return shell(getUiDevice(), USER_SWITCHER_VISIBLE_FLAG_CMD).trim().endsWith("true");
    }

    /** Return the maximum number of users that are allowed in the current device. */
    public int getMaxUsers() {
        return getUserManager().getMaxSupportedUsers();
    }
}
