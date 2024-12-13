/*
 * Copyright (C) 2019 The Android Open Source Project
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
package android.platform.helpers;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.car.Car;
import android.car.CarOccupantZoneManager;
import android.car.SyncResultCallback;
import android.car.user.CarUserManager;
import android.car.user.CarUserManager.UserLifecycleListener;
import android.car.user.UserCreationRequest;
import android.car.user.UserCreationResult;
import android.car.user.UserStartResponse;
import android.car.user.UserStartRequest;
import android.car.user.UserSwitchResult;
import android.car.user.UserStopRequest;
import android.car.user.UserStopResponse;
import android.car.util.concurrent.AsyncFuture;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Build;
import android.os.SystemClock;
import android.os.UserManager;
import android.support.test.uiautomator.UiDevice;
import android.util.Log;

import androidx.test.InstrumentationRegistry;

import com.android.compatibility.common.util.SystemUtil;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Helper class that is used by integration test only. It is wrapping around exiting platform APIs
 * {@link CarUserManager}, {@link UserManager} to expose functions for user switch end-to-end tests.
 */
public class MultiUserHelper {
    private static final int CREATE_USER_TIMEOUT_MS = 20_000;
    private static final String LOG_TAG = MultiUserHelper.class.getSimpleName();

    /** For testing purpose we allow a wide range of switching time. */
    private static final int USER_SWITCH_TIMEOUT_SECOND = 300;

    private static final String CREATE_USER_COMMAND = "cmd car_service create-user ";

    private static MultiUserHelper sMultiUserHelper;
    private CarUserManager mCarUserManager;
    private UserManager mUserManager;
    private ActivityManager mActivityManager;
    private CarOccupantZoneManager mCarOccupantZoneManager;

    private MultiUserHelper() {
        Context context = InstrumentationRegistry.getTargetContext();
        mUserManager = UserManager.get(context);
        Car car = Car.createCar(context);
        mCarUserManager = (CarUserManager) car.getCarManager(Car.CAR_USER_SERVICE);
        mActivityManager = context.getSystemService(ActivityManager.class);
        mCarOccupantZoneManager = car.getCarManager(CarOccupantZoneManager.class);
    }

    /**
     * It will always be used as a singleton class
     *
     * @return MultiUserHelper instance
     */
    public static MultiUserHelper getInstance() {
        if (sMultiUserHelper == null) {
            sMultiUserHelper = new MultiUserHelper();
        }
        return sMultiUserHelper;
    }

    /**
     * Creates a user if it does not exist already.
     *
     * @param name the name of the user or guest
     * @param isGuestUser true if want to create a guest, otherwise create a regular user
     * @return User Id for newly created user or existing user if it already exists
     */
    public int createUserIfDoesNotExist(String name, boolean isGuestUser) throws Exception {
        UserInfo userInfo = getUserByName(name);
        if (userInfo != null) {
            return userInfo.id;
        }
        return createUser(name, isGuestUser);
    }

    /**
     * Creates a regular user or guest
     *
     * @param name the name of the user or guest
     * @param isGuestUser true if want to create a guest, otherwise create a regular user
     * @return User Id for newly created user
     */
    public int createUser(String name, boolean isGuestUser) throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Log.d(
                    LOG_TAG,
                    "An Android version earlier than UDC is detected. Using the shell for user "
                            + "creation");
            return createUserUsingShell(name, isGuestUser);
        }
        Log.d(
                LOG_TAG,
                "Creating new "
                        + (isGuestUser ? "guest" : "user")
                        + " with name '"
                        + name
                        + "' using CarUserManager");
        SyncResultCallback<UserCreationResult> userCreationResultCallback =
                new SyncResultCallback<>();
        if (isGuestUser) {
            // Create a new guest
            mCarUserManager.createUser(
                    new UserCreationRequest.Builder().setName(name).setGuest().build(),
                    Runnable::run,
                    userCreationResultCallback);
        } else {
            // Create a new user
            mCarUserManager.createUser(
                    new UserCreationRequest.Builder().setName(name).build(),
                    Runnable::run,
                    userCreationResultCallback);
        }
        UserCreationResult result =
                userCreationResultCallback.get(CREATE_USER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            throw new Exception(String.format("The user was not created successfully: %s", result));
        }
        return result.getUser().getIdentifier();
    }

    private int createUserUsingShell(String name, boolean isGuest) throws Exception {
        String retStr;
        Log.d(
                LOG_TAG,
                "Creating new " + (isGuest ? "guest" : "user") + " with name '" + name + "'");
        if (isGuest) {
            retStr = SystemUtil.runShellCommand(CREATE_USER_COMMAND + "--guest " + name);
        } else {
            retStr = SystemUtil.runShellCommand(CREATE_USER_COMMAND + name);
        }
        if (!retStr.contains("STATUS_SUCCESSFUL")) {
            throw new Exception(
                    "failed to create a new user: "
                            + name
                            + ". User creation shell command output: "
                            + retStr);
        }
        // Extract the user ID out and return it
        String userIdPattern = "id=";
        int newUserId = -1;
        if (retStr.contains(userIdPattern) && retStr.split(userIdPattern).length > 1) {
            newUserId = Integer.parseInt(retStr.split(userIdPattern)[1].trim());
        }
        return newUserId;
    }

    /**
     * Switches to the target user at API level. Always waits until user switch complete. Besides,
     * it waits for an additional amount of time for switched user to become idle (stable)
     *
     * @param id user id
     * @param timeoutMs the time to wait (in msec) after user switch complete
     */
    public void switchAndWaitForStable(int id, long timeoutMs) throws Exception {
        switchToUserId(id);
        SystemClock.sleep(timeoutMs);
    }

    /**
     * Switches to the target user at API level. Always wait until user switch complete.
     *
     * <p>User switch complete only means the user ready at API level. It doesn't mean the UI is
     * completely ready for the target user. It doesn't include unlocking user data and loading car
     * launcher page
     *
     * @param id Id of the user to switch to
     */
    public void switchToUserId(int id) throws Exception {
        Log.d(
                LOG_TAG,
                String.format(
                        "Switching from user %d to user %d",
                        getCurrentForegroundUserInfo().id, id));
        final CountDownLatch latch = new CountDownLatch(1);
        // A UserLifeCycleListener to wait for user switch event. It is equivalent to
        // UserSwitchObserver#onUserSwitchComplete callback
        // TODO(b/155434907): Should eventually wait for "user unlocked" event which is better
        UserLifecycleListener userSwitchListener =
                e -> {
                    if (e.getEventType() == CarUserManager.USER_LIFECYCLE_EVENT_TYPE_SWITCHING) {
                        latch.countDown();
                    }
                };
        mCarUserManager.addListener(Runnable::run, userSwitchListener);
        AsyncFuture<UserSwitchResult> future = mCarUserManager.switchUser(id);
        UserSwitchResult result = null;
        try {
            result = future.get(USER_SWITCH_TIMEOUT_SECOND, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new Exception(
                    String.format("Exception when switching to target user: %d", id), e);
        }

        if (!result.isSuccess()) {
            throw new Exception(String.format("User switch failed: %s", result));
        }
        // Wait for user switch complete event, which seems to happen later than UserSwitchResult.
        if (!latch.await(USER_SWITCH_TIMEOUT_SECOND, TimeUnit.SECONDS)) {
            throw new Exception(
                    String.format(
                            "Timeout while switching to user %d after %d seconds",
                            id, USER_SWITCH_TIMEOUT_SECOND));
        }
        mCarUserManager.removeListener(userSwitchListener);
    }

    /**
     * Starts a user on a display. Awaits {@link CarUserManager#USER_LIFECYCLE_EVENT_TYPE_STARTING} event.
     *
     * <p>User start complete only means the user ready at API level. It doesn't mean the UI is
     * completely ready for the target user. It doesn't include unlocking user data and loading car
     * launcher page
     *
     * @param id Id of the user to start
     * @param displayId Id of the display to start the user on
     */
    public void startUser(int id, int displayId) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        UserLifecycleListener userStartListener =
                e -> {
                    if (e.getEventType() == CarUserManager.USER_LIFECYCLE_EVENT_TYPE_STARTING) {
                        latch.countDown();
                    }
                };
        mCarUserManager.addListener(Runnable::run, userStartListener);
        UserStartRequest userStartRequest =
                new UserStartRequest.Builder(mUserManager.getUserInfo(id).getUserHandle())
                        .setDisplayId(displayId)
                        .build();
        Log.d(LOG_TAG, String.format("Starting a user with id %d on display %d", id, displayId));
        SyncResultCallback<UserStartResponse> userStartResponseCallback =
                new SyncResultCallback<>();
        mCarUserManager.startUser(userStartRequest, Runnable::run, userStartResponseCallback);
        UserStartResponse response =
                userStartResponseCallback.get(USER_SWITCH_TIMEOUT_SECOND, TimeUnit.SECONDS);
        if (!response.isSuccess()) {
            throw new Exception(
                    String.format(
                            "Failed to start user %d on display %d: %s", id, displayId, response));
        }
        if (!latch.await(USER_SWITCH_TIMEOUT_SECOND, TimeUnit.SECONDS)) {
            throw new Exception(
                    String.format(
                            "Timeout while starting user %d after %d seconds",
                            id, USER_SWITCH_TIMEOUT_SECOND));
        }
        mCarUserManager.removeListener(userStartListener);
    }

    /**
     * Stops a user.
     *
     * @param id Id of the user to stop
     */
    public void stopUser(int id) throws Exception {
        UserStopRequest userStopRequest =
                new UserStopRequest.Builder(mUserManager.getUserInfo(id).getUserHandle()).build();
        SyncResultCallback<UserStopResponse> userStopResponseCallback = new SyncResultCallback<>();
        mCarUserManager.stopUser(userStopRequest, Runnable::run, userStopResponseCallback);
        UserStopResponse response =
                userStopResponseCallback.get(USER_SWITCH_TIMEOUT_SECOND, TimeUnit.SECONDS);
        if (!response.isSuccess()) {
            throw new Exception(String.format("Failed to stop user %d: %s", id, response));
        }
    }

    /**
     * Removes the target user. For now it is a non-blocking call.
     *
     * @param userInfo info of the user to be removed
     * @return true if removed successfully
     */
    public boolean removeUser(UserInfo userInfo) {
        return removeUser(userInfo.id);
    }

    /**
     * Removes the target user. For now it is a non-blocking call.
     *
     * @param userId id of the user to be removed
     * @return true if removed successfully
     */
    public boolean removeUser(int userId) {
        return mUserManager.removeUser(userId);
    }

    public UserInfo getCurrentForegroundUserInfo() {
        return mUserManager.getUserInfo(ActivityManager.getCurrentUser());
    }

    /**
     * Get default initial user
     *
     * @return user ID of initial user
     */
    public int getInitialUser() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        try {
            String userId = device.executeShellCommand("cmd car_service get-initial-user").trim();
            return Integer.parseInt(userId);
        } catch (IOException e) {
            throw new RuntimeException("Failed to get initial user", e);
        }
    }

    /**
     * Tries to find an existing user with the given name
     *
     * @param name the name of the user
     * @return A {@link UserInfo} if the user is found, or {@code null} if not found
     */
    @Nullable
    public UserInfo getUserByName(String name) {
        return mUserManager.getUsers().stream()
                .filter(user -> user.name.equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the list of displays that are available for starting visible background users.
     *
     * @return The list of displays that are available for starting visible background users.
     */
    public int[] getDisplayIdsForStartingVisibleBackgroundUsers() {
        return mActivityManager.getDisplayIdsForStartingVisibleBackgroundUsers();
    }

    /**
     * Returns the user id for the given display id.
     *
     * @param displayId The display id.
     * @return The user id for the given display id.
     */
    public int getUserForDisplayId(int displayId) {
        return mCarOccupantZoneManager.getUserForDisplayId(displayId);
    }
}
