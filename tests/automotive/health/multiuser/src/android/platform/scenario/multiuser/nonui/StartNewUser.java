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

package android.platform.scenario.multiuser;

import android.app.UiAutomation;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.platform.helpers.MultiUserHelper;
import android.platform.test.scenario.annotation.Scenario;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This test will create a new user and start it on the display under test.
 * Additionally, it can create and start additional users on other displays.
 *
 * <p>It should be running under user 0, otherwise instrumentation may be killed after user
 * switched.
 */
@Scenario
@RunWith(JUnit4.class)
public class StartNewUser {
    private static final String LOG_TAG = StartNewUser.class.getSimpleName();

    private static final String DISPLAY_UNDER_TEST = "display-under-test";
    private static final String START_USERS_ON_ADDITIONAL_DISPLAYS =
            "start-users-on-additional-displays";
    private static final String TARGET_USER_GUEST = "target-user-guest";
    private static final int WAIT_FOR_USER_START_TIME_MS = 10000;

    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private UiAutomation mUiAutomation = null;
    private static final String CREATE_USERS_PERMISSION = "android.permission.CREATE_USERS";
    private int mDisplayUnderTest;
    private boolean mTargetUserGuest;

    private int mStartUsersOnAdditionalDisplays;
    private Map<Integer, Integer> mDisplayToUserIdMap = new HashMap<>();

    static List<Integer> usersToDelete = new ArrayList<>();

    @Before
    public void setup() throws Exception {
        mUiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        mUiAutomation.adoptShellPermissionIdentity(CREATE_USERS_PERMISSION);

        final Bundle arguments = InstrumentationRegistry.getArguments();
        mDisplayUnderTest = Integer.parseInt(arguments.getString(DISPLAY_UNDER_TEST, "1"));
        mStartUsersOnAdditionalDisplays =
                Integer.parseInt(arguments.getString(START_USERS_ON_ADDITIONAL_DISPLAYS, "0"));
        mTargetUserGuest = Boolean.valueOf(arguments.getString(TARGET_USER_GUEST, "false"));
        Log.d(
                LOG_TAG,
                String.format(
                        Locale.US,
                        "Testing user start latency on %d display with %d additional users",
                        mDisplayUnderTest,
                        mStartUsersOnAdditionalDisplays));

        int[] displays = mMultiUserHelper.getDisplayIdsForStartingVisibleBackgroundUsers();
        // Device must have enough displays to start users on additional displays
        // and the display under test
        Assume.assumeTrue(
                String.format(
                        Locale.US,
                        "Maximum additional visible background users allowed is %d (# of displays -"
                                + " 1), but %d were requested",
                        displays.length - 1,
                        mStartUsersOnAdditionalDisplays),
                displays.length > mStartUsersOnAdditionalDisplays);

        // Target User must be created first so the userId will be 11
        mDisplayToUserIdMap.put(
                mDisplayUnderTest,
                mMultiUserHelper.createUserIfDoesNotExist(
                        "user" + mDisplayUnderTest, mTargetUserGuest));

        mDisplayToUserIdMap.putAll(createAndStartAdditionalUsers(displays));
        SystemClock.sleep(MultiUserConstants.WAIT_FOR_IDLE_TIME_MS);

        StartNewUser.usersToDelete = new ArrayList<>(mDisplayToUserIdMap.values());
    }

    @Test
    public void testStartNewUser() throws Exception {
        int userId = mDisplayToUserIdMap.get(mDisplayUnderTest);
        mMultiUserHelper.startUser(userId, mDisplayUnderTest);
        // TODO(ivankozlov): instead of sleep, wait for an event(carlauncher drawn or smth else)
        SystemClock.sleep(WAIT_FOR_USER_START_TIME_MS);
        int actualUserId = mMultiUserHelper.getUserForDisplayId(mDisplayUnderTest);
        Assert.assertEquals(
            String.format(Locale.US, "User %d is not started on display %d", userId, mDisplayUnderTest),
            userId,
            actualUserId
        );
    }

    @After
    public void dropShellPermissionIdentity() throws Exception {
        for (int userId : mDisplayToUserIdMap.values()) {
            mMultiUserHelper.stopUser(userId);
        }
        mUiAutomation.dropShellPermissionIdentity();
    }

    @AfterClass
    public static void deleteUsers() throws Exception {
        for (int userId : usersToDelete) {
            MultiUserHelper.getInstance().removeUser(userId);
        }
    }

    private Map<Integer, Integer> createAndStartAdditionalUsers(int[] displays) throws Exception {
        Map<Integer, Integer> displayToUserIdMap = new HashMap<>();
        for (int display : displays) {
            if (display == mDisplayUnderTest) {
                continue;
            }
            boolean reachedMaxAdditionalUsers =
                    displayToUserIdMap.size() >= mStartUsersOnAdditionalDisplays;
            if (reachedMaxAdditionalUsers) {
                break;
            }
            int userId = mMultiUserHelper.createUserIfDoesNotExist("user" + display, false);
            Log.d(
                    LOG_TAG,
                    String.format(Locale.US, "Mapping user id %d for display %d", userId, display));
            mMultiUserHelper.startUser(userId, display);
            displayToUserIdMap.put(display, userId);
        }
        return displayToUserIdMap;
    }
}
